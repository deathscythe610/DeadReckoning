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
	private float K = 0.7f;
	private long lastStepTime=0;
	private States state = States.IDLE;
	private ParameterEstimation paramEst = null;
	private boolean calibrationLogging = false;
	public boolean stateLogging=false;
	
	//define public information for drFragment
	public int steps = 0;
	public float distance=0;
	public String stateLog = "";
	
	
	//define separate constant for zhanhy step detection algorithm 
	private boolean IsGoUp = true;
	private int halfsteps = 0;
	private long zhanhy_stepDelay = 100;
	private float zhanhy_thresholdMax = 1f;
	private float zhanhy_thresholdMin = -0.9f;
	private float min_dif = 2.5f;
	private long zhanhy_minstep_delay = 80;
	private long zhanhy_maxstep_delay = 1500;
	
	//define separate variable for X axis filter 
	private double axHistory[];
	private int axHistorySize = 4; 
	private double ax_thresholdMin = -0.5;
	private double ax_thresholdMax = 0.5;
	
	//Map<String, String> valuesMap = new HashMap<String, String>(); // name -> value
	//Map<String, TextView> uiMap = new HashMap<String, TextView>(); // name -> TextView
	
	//Define a Timer for scheduled task
	Timer deadReckoningTimer;
	private int mCurrentPage;
	private String pageTitle;
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
		this.axHistory=new double[this.axHistorySize];
		for(int i=0;i<this.axHistorySize;i++) {
			this.axHistory[i]=0;
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
		mCurrentPage = getArguments().getInt("current_page", 0);
		pageTitle = getArguments().getString("page_title");
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
		deadReckoningTimer.scheduleAtFixedRate(new deadReckoningTask(), 0, 10);
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
	/**
	 * This is an added function for zhanhy detection algorithm
	 * This function count the occurrence of half steps (a single max or min peak)
	 * @param orientation
	 * @param triggerTime
	 */
	void halfstepDetected(double orientation, long triggerTime){
		this.halfsteps++;
		this.lastStepTime = triggerTime; 
	}
	
	void stepDetected(double orientation, long triggerTime) {
		//Log.d(DRFragment.TAG, "Step detected");
		this.steps++;
		this.lastStepTime=triggerTime;
		float stepDistance = (float)(this.K * Math.pow(this.lastMaximum-this.lastMinimum,0.25));
		this.distance = stepDistance;
		System.out.println("Distance:" + this.distance + " Step Distance" + stepDistance);
		this.distance = Misc.roundToDecimals(this.distance,2);
		
	}
	
	private void addLine(String line) {
		this.stateLog = line + " ("+this.steps+")\n" + this.stateLog;
	}
	
	/** 
	 * Zhanhy method use a sampling rate of 20 Hz which is much slower than Thomas (100Hz) --> Change to 25Hz 
	 * At peak and valley point, the data tends to go down immediately and do not stay for a long time
	 * Therefore, aftermax and aftermin state can safely be put away
	 * Zhanhy method only care if there is any pair of peak and valley but not the order of occurrence    
	 * @param az
	 * @param orientation
	 * @param triggerTime
	 */
	protected void trigger_zhanhy(float az, float ax, double orientation) {
		this.axPush(ax);
		this.trigger_zhanhy(az, ax, orientation, Misc.getTime());
	}
	
	protected void trigger_zhanhy(float az, float ax, double orientation, long triggerTime){
		if(this.paramEst!=null && this.calibrationLogging) {
			this.paramEst.recordAcceleration(az);
		}
		if(az>this.lastMaximum)
			this.lastMaximum=az;
		if(az<this.lastMinimum)
			this.lastMinimum=az;
		switch (this.state){
			case IDLE:
				this.lastMaximum=0;
				this.lastMinimum=0;
				if(triggerTime-this.lastStepTime>this.zhanhy_stepDelay  && az>this.zhanhy_thresholdMax ) {
					this.state=States.MAX_DETECT;
				}
				else if (triggerTime-this.lastStepTime>this.zhanhy_stepDelay  && az<this.zhanhy_thresholdMin ) {
					this.state=States.MIN_DETECT;
				}
				break;
			case MAX_DETECT:
				if(az==this.lastMaximum){
					//Not yet reach peak, wait for peak
				}
				else{								//Reach peak.
					this.maxThresholdPasses++;
					this.lastStepTime = triggerTime;
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
					this.lastStepTime = triggerTime;
					this.IsGoUp = true;
					//this.addLine("Last Minimum: " + this.lastMinimum);
					this.state=States.MIDSTEP_DELAY;	
				}
				break;
				
			case MIDSTEP_DELAY:
				if((IsGoUp = false) && (az<this.zhanhy_thresholdMin) && ((triggerTime-lastStepTime)>zhanhy_minstep_delay)){
					this.state = States.AFTER_MIN_DETECT;
					//this.addLine("Change to AFTER_MIN_DETECT");
				}
				else if((IsGoUp = true) && (az>this.zhanhy_thresholdMax) && ((triggerTime-lastStepTime)>zhanhy_minstep_delay)){
					this.state = States.AFTER_MAX_DETECT;
					//this.addLine("Change to AFTER_MAX_DETECT");
				}
				else if ((triggerTime - lastStepTime)>zhanhy_maxstep_delay){
					this.state = States.IDLE;
					this.addLine("MIDSTEP TO IDLE, Half Step Detected");
					halfstepDetected(orientation, triggerTime);
				}
				break;
			case AFTER_MIN_DETECT:
				if (az==this.lastMinimum){
					//Not yet reach valley, wait for valley
				}
				else if( ((this.lastMaximum-this.lastMinimum)>min_dif) && (Checkax())){
					//Valid step
					this.state = States.IDLE;
					stepDetected(orientation, triggerTime);
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
				else if ( ((this.lastMaximum-this.lastMinimum)>min_dif) && (Checkax()) ){
					//Valid step
					this.state = States.IDLE;
					stepDetected(orientation, triggerTime);
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
		this.azPush(az);
		
		
	}

	protected boolean Checkax(){
		for(int i=0; i<axHistorySize; i++){
			if ((axHistory[i]<this.ax_thresholdMin) || (axHistory[i]>this.ax_thresholdMax)){
				//this.addLine("Pass Threshold X Value: " + axHistory[i]);
				return true;
			}
		}
		//this.addLine("X value not valid");
		return false; 
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
				DRFragment.getInstance().trigger_zhanhy(SensorFragment.getInstance().getWorldAccelerationZ(),SensorFragment.getInstance().getWorldAccelerationX(), SensorFragment.getInstance().orientationFusion.getOrientation());
			}
		}
	}
	
//*******************************************************************************************************************
//												SUPPORT FUNCTION
//*******************************************************************************************************************	
	
	protected void setParameters(float tMax, float tMin, float K) {
		this.zhanhy_thresholdMax=tMax;
		this.zhanhy_thresholdMin=tMin;
		this.K=K;
	}
	
	private void azPush(double val) {
		for(int i=this.azHistorySize-1;i>0;i--) {
			this.azHistory[i]=this.azHistory[i-1];
		}
		this.azHistory[0]=val;
	}
	
	private void axPush(double val) {
		for(int i=this.axHistorySize-1;i>0;i--) {
			this.axHistory[i]=this.axHistory[i-1];
		}
		this.axHistory[0]=val;
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
			this.K = 0.415f * height;
		else
			this.K = 0.413f * height;
		return this.K;
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
		return this.K;
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
