package com.example.deadreckoning;

import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Map.Entry;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

public class DRFragment extends FragmentControl{
	
	public enum States {
		IDLE, MAX_DETECT, AFTER_MAX_DETECT, MIDSTEP_DELAY, MIN_DETECT, AFTER_MIN_DETECT;
	}
	private static DRFragment instance=null;
	private static final String TAG = "DR_Fragment";
	private double[] azHistory;
	private int azHistorySize=2;
	
	private int maxThresholdPasses=0;
	private int minThresholdPasses=0;
	private float lastMaximum = 0;
	private float lastMinimum = 0;
	private float StrideLengthConstant = 0.31f;
	private long lastStepTime=0;
	private long midStepTime = 0;
	private States state = States.IDLE;
	private ParameterEstimation paramEst = null;
	private boolean calibrationLogging = false;
	public boolean stateLogging=false;
	
	//define public information for drFragment
	public int steps = 0;
	public float distance=0;
	public String stateLog = "";
	
	
	//define separate constant for zhangyu step detection algorithm 
	private boolean IsGoUp = true;
	private long zhangyu_stepDelay = 250;
	private float zhangyu_thresholdMax = 2.3f;
	private float zhangyu_thresholdMin = -2.3f;
	private float min_dif = 2.5f;
	private long zhangyu_minstep_delay = 1;
	private long zhangyu_maxstep_delay = 1000;
	
	//define a slot for previous sampling time stamp
	private long AccPreviousTimeStamp = 0;
	
	//Map<String, String> valuesMap = new HashMap<String, String>(); // name -> value
	//Map<String, TextView> uiMap = new HashMap<String, TextView>(); // name -> TextView
	
	//Define a Timer for scheduled task
	Timer deadReckoningTimer;
	private ScrollView layout;
	
	
//*******************************************************************************************************************
//	FUNCTION INITIALIZATION
//*******************************************************************************************************************	
	
	public static DRFragment getInstance() {
		if(DRFragment.instance==null) {
			Log.e(TAG,"DR Fragment is not loaded");
		}
		return DRFragment.instance;
	}
	public DRFragment() {
		super();
		this.azHistory=new double[this.azHistorySize];
		for(int i=0;i<this.azHistorySize;i++) {
			this.azHistory[i]=0;
		}
	}
	
	public static DRFragment newInstance(int position, String title){
    	DRFragment drFragment = new DRFragment();
    	Bundle args = new Bundle();
    	args.putInt("current_page", 0);
    	args.putString("page_tile", "DR Information");
    	drFragment.setArguments(args);
    	return drFragment;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		DRFragment.instance=this;
		
		//Initialise different threshold for different sampling rate set in Main Activity
		if (MainActivity.MSsensorSamplingRate==20){
			zhangyu_thresholdMax = 3f;
			zhangyu_thresholdMin = -2.8f;
			StrideLengthConstant = 0.301f;		
		}
		else if (MainActivity.MSsensorSamplingRate==30)
		{
			zhangyu_thresholdMax = 3f;
			zhangyu_thresholdMin = -2.2f; 
			StrideLengthConstant = 0.31f;
		}
		else if (MainActivity.MSsensorSamplingRate==40)
		{
			zhangyu_thresholdMax = 2.2f;
			zhangyu_thresholdMin = -2f;
			StrideLengthConstant = 0.32f;
		}
		else 
		{	
			zhangyu_thresholdMax = 2.2f;
			zhangyu_thresholdMin = -1.6f;
			StrideLengthConstant = 0.337f;
		}
	}



	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		this.layout = (ScrollView)inflater.inflate(R.layout.dead_reckoning, container, false);
		if(layout==null) {
			Log.d(TAG,"layout null");
			return null;
		}
		uiMap.put("steps", (TextView) layout.findViewById(R.id.stepsValue));
		uiMap.put("statesLog", (TextView) layout.findViewById(R.id.statesLog));
		uiMap.put("distance", (TextView) layout.findViewById(R.id.distanceValue));
		return layout;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		deadReckoningTimer = new Timer();
		deadReckoningTimer.scheduleAtFixedRate(new deadReckoningTask(), 0, MainActivity.MSsensorSamplingRate);
		deadReckoningTimer.scheduleAtFixedRate(new updateUITask(), 50, MainActivity.uiUpdateRate);
	}
	
	@Override
	public void onPause() {
		if (this.deadReckoningTimer!=null)	
			this.deadReckoningTimer.cancel();
		super.onPause();
	}
	
	//*******************************************************************************************************************
//								STEP DETECTION AND STRIDE LENGHT ESTIMTION
//*******************************************************************************************************************

	void stepDetected(double orientation, long triggerTime) {
		//Log.d(DRFragment.TAG, "Step detected");
		this.steps++;
		this.lastStepTime=triggerTime;
		float stepDistance = (float)(this.StrideLengthConstant * Math.pow(this.lastMaximum-this.lastMinimum,0.25));
		this.distance = stepDistance;
		System.out.println("Distance:" + this.distance + " Step Distance" + stepDistance);
		this.distance = Misc.roundToDecimals(this.distance,2);
		
	}
	
	private void addLine(String line) {
		this.stateLog = line + " ("+this.steps+")\n" + this.stateLog;
	}
	
	/** 
	 * Zhang yu method use a sampling rate of 20 Hz which is much slower than Thomas (100Hz) --> Change to 25Hz 
	 * At peak and valley point, the data tends to go down immediately and do not stay for a long time
	 * Therefore, aftermax and aftermin state can safely be put away
	 * Zhang yu method only care if there is any pair of peak and valley but not the order of occurrence    
	 * @param az
	 * @param orientation
	 * @param triggerTime
	 */
	protected void trigger_zhangyu(float az, float ax, double orientation) {
		this.trigger_zhangyu(az, ax, orientation, Misc.getTime());
	}
	
	protected void trigger_zhangyu(float az, float ax, double orientation, long triggerTime){
		if(this.paramEst!=null && this.calibrationLogging) {
			this.paramEst.recordAcceleration(az);
		}
		if(az>this.lastMaximum)
			this.lastMaximum=az;
		if(az<this.lastMinimum)
			this.lastMinimum=az;
		switch (this.state){
			case IDLE:
				if(triggerTime-this.lastStepTime>this.zhangyu_stepDelay  && az>this.zhangyu_thresholdMax ) {
					this.state=States.MAX_DETECT;
				}
				else if (triggerTime-this.lastStepTime>this.zhangyu_stepDelay  && az<this.zhangyu_thresholdMin ) {
					this.state=States.MIN_DETECT;
				}
				else 
				{
					this.lastMaximum=0;
					this.lastMinimum=0;
				}
				break;
			case MAX_DETECT:
				if(az==this.lastMaximum){
					//Not yet reach peak, wait for peak
				}
				else{								//Reach peak.
					this.maxThresholdPasses++;
					this.midStepTime = this.AccPreviousTimeStamp;
					this.IsGoUp = false;
					//this.addLine("Last Maximum: " + this.lastMaximum);
					this.state=States.MIDSTEP_DELAY;
				}
				break;
			case MIN_DETECT:
				if(az==this.lastMinimum){
					//Not yet reach peak, wait for peak
				}
				else{								//Reach peak.
					this.minThresholdPasses++;
					this.midStepTime = this.AccPreviousTimeStamp;
					this.IsGoUp = true;
					//this.addLine("Last Minimum: " + this.lastMinimum);
					this.state=States.MIDSTEP_DELAY;	
				}
				break;
				
			case MIDSTEP_DELAY:
				if (IsGoUp == false)
				{
					if ((az>this.azHistory[0]) && (this.azHistory[0]<this.zhangyu_thresholdMin))
					{	//No mid point between peak and valley, step already occurred
						this.state = States.IDLE;
						stepDetected(orientation, triggerTime);
						this.addLine("step detected: "+this.steps);
						this.minThresholdPasses++;
					}
					else if ((az<this.zhangyu_thresholdMin) && ((triggerTime-midStepTime)>zhangyu_minstep_delay))
					{	
						this.state = States.AFTER_MIN_DETECT;
					}
				}
				else if (IsGoUp) 
				{
					if ((az<this.azHistory[0]) && (this.azHistory[0]>this.zhangyu_thresholdMax))
					{	//No mid point between peak and valley, step already occurred
						this.state = States.IDLE;
						stepDetected(orientation, triggerTime);
						this.addLine("step detected: "+this.steps);
						this.maxThresholdPasses++;
					}
					else if ((az>this.zhangyu_thresholdMax) && ((triggerTime-midStepTime)>zhangyu_minstep_delay))
					{
						this.state = States.AFTER_MAX_DETECT;
					}
				}
				else if ((triggerTime - midStepTime)>zhangyu_maxstep_delay){
					this.state = States.IDLE;
					this.addLine("MIDSTEP TO IDLE, Half Step Detected");
				}
				break;
			case AFTER_MIN_DETECT:
				if (az==this.lastMinimum){
					//Not yet reach valley, wait for valley
				}
				else if ((this.lastMaximum-this.lastMinimum)>min_dif){
					//Valid step
					this.state = States.IDLE;
					stepDetected(orientation, this.AccPreviousTimeStamp);
					this.addLine("step detected: "+this.steps);
					this.minThresholdPasses++;
				}
				else{
					float difference = this.lastMaximum - this.lastMinimum;
					this.addLine("Difference too small: " + difference);
					this.state = States.IDLE;
				}
				break;
			case AFTER_MAX_DETECT:
				if (az==this.lastMaximum){
					//Not yet reach valley, wait for valley
				}
				else if ((this.lastMaximum-this.lastMinimum)>min_dif){
					//Valid step
					this.state = States.IDLE;
					stepDetected(orientation, this.AccPreviousTimeStamp);
					this.addLine("step detected: "+this.steps);
					this.maxThresholdPasses++;
				}
				else{
					float difference = this.lastMaximum - this.lastMinimum;
					this.addLine("Difference too small: " + difference);
					this.state = States.IDLE;
				}
				break;
		}
		if(this.stateLogging)
			DataLogManager.addLine("DR", triggerTime+","+this.state.ordinal()+","+this.steps+","+az+","+this.azAvg());
		this.AccPreviousTimeStamp = triggerTime;
		this.azPush(az);
		
		
	}

	
	
	class updateUITask extends TimerTask {
		public void run() {
			MainActivity.getInstance().runOnUiThread(new Thread(new Runnable(){
				public void run(){
					
					//Log.d("DR_UI", "running updateUITask_DR");
					valuesMap.put("steps", DRFragment.getInstance().steps+"");
					valuesMap.put("statesLog", DRFragment.getInstance().stateLog);
					valuesMap.put("distance", DRFragment.getInstance().distance+"");
					Iterator<Entry<String, TextView>> it = DRFragment.getInstance().uiMap.entrySet().iterator();
					while (it.hasNext()) {
						java.util.Map.Entry<String, TextView> pairs = (java.util.Map.Entry<String, TextView>)it.next();
						 if(DRFragment.getInstance().valuesMap.containsKey(pairs.getKey())) {
			                TextView temp = pairs.getValue();
			                if(temp!=null) {
			                	temp.setText(DRFragment.getInstance().valuesMap.get(pairs.getKey()));
			                }
			                else {
			                	Log.d(TAG,pairs.toString());
			                }
						 }
					}
					
				}
			}));
		}
	}
	
	class deadReckoningTask extends TimerTask {
		public void run() {
			//Log.d("DR_Task", "running deadReckingTask_DR");
			if (SensorFragment.getInstance()!=null){
				DRFragment.getInstance().trigger_zhangyu(SensorFragment.getInstance().getWorldAccelerationZ(),SensorFragment.getInstance().getWorldAccelerationX(), SensorFragment.getInstance().orientationFusion.getOrientation());
			}
		}
	}
	
//*******************************************************************************************************************
//												SUPPORT FUNCTION
//*******************************************************************************************************************	
	
	protected void setParameters(float tMax, float tMin, float StrideLengthConstant) {
		this.zhangyu_thresholdMax=tMax;
		this.zhangyu_thresholdMin=tMin;
		this.StrideLengthConstant=StrideLengthConstant;
	}
	
	private void azPush(double val) {
		for(int i=this.azHistorySize-1;i>0;i--) {
			this.azHistory[i]=this.azHistory[i-1];
		}
		this.azHistory[0]=val;
	}
	
	
	private double azAvg() {
		double sum=0;
		for(int i=0;i<this.azHistorySize;i++) {
			sum+=this.azHistory[i];
		}
		return sum/this.azHistorySize;
	}
	
	protected double setKFromHeight(boolean isMale, float height) {
		if(isMale)
			this.StrideLengthConstant = 0.415f * height;
		else
			this.StrideLengthConstant = 0.413f * height;
		return this.StrideLengthConstant;
	}
	
	public int getSteps() {
		return this.steps;
	}
	
	public int getMinThresholdPasses() {
		return this.minThresholdPasses;
	}
	
	public int getMaxThresholdPasses() {
		return this.maxThresholdPasses;
	}
	
	public float getDistance() {
		return this.distance;
	}
	
	public long getStepTime(){
		return this.lastStepTime;
	}
	
public float getK() {
		return this.StrideLengthConstant;
	}
	
	public String getLog() {
		return this.stateLog;
	}
	
	public void reset() {
		this.distance=0;
	}

	public void startCalibrationLogging() {
		this.paramEst=new ParameterEstimation(this);
		this.calibrationLogging=true;
	}
	
	public void startCalibrationCalculations() {
		this.calibrationLogging=false;
		this.paramEst.calibrationPromptDialog();
	}
	
	public void endCalibration() {
		this.paramEst=null;
	}

	public View getLayout(){
		return this.layout;
	}
	
}
