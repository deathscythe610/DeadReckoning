package com.example.deadreckoning;


import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;


public class SensorFragment extends FragmentControl implements SensorEventListener{
	private static final String TAG = "Sensor_Fragment";
	public static SensorFragment instance = null;
	protected int sensorDelay = SensorManager.SENSOR_DELAY_NORMAL;
	protected SensorManager sensorManager;
	private Timer logTimer;

	//define public variables to display in sensor fragment
	public float[] magneticFieldValues={0,0,0};
	public float[] accelerometerValues={0,0,0};
	public float[] linearAccelerometerValues={0,0,0};
	public double displacementX=0.0;
	public double displacementY=0.0;
	public double displacementZ=0.0;
	public double worldAccelerationX=0.0;
	public double worldAccelerationY=0.0;
	public double worldAccelerationZ=0.0;
	public OrientationFusion orientationFusion;
	public float[] gyroscopeValues={0,0,0};
	private Timer sensorinfoTimer;
	public View layout;
	private int mCurrentPage;
	private String pageTitle;

//*******************************************************************************************************************
//											FUNCTION INITIALIZATION
//*******************************************************************************************************************
	public static SensorFragment getInstance() {
		if(SensorFragment.instance==null) {
			Log.e(TAG,"Sensor Fragment is not loaded");
		}
		return SensorFragment.instance;
	}

	public SensorFragment(){
		super();
		this.linearAccelerometerValues=new float[4];
		this.linearAccelerometerValues[3]=0;
		this.sensorManager = (SensorManager)MainActivity.getInstance().getSystemService(Context.SENSOR_SERVICE);
		this.orientationFusion=new OrientationFusion();
	}

	public static SensorFragment newInstance(int position, String title){
    	SensorFragment sensorFragment = new SensorFragment();
    	Bundle args = new Bundle();
    	args.putInt("current_page", 2);
    	args.putString("page_tile", "Sensor Information");
    	sensorFragment.setArguments(args);
    	return sensorFragment;
    }

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mCurrentPage = getArguments().getInt("current_page", 2);
		pageTitle = getArguments().getString("page_title");
		SensorFragment.instance=this;
		this.init();
	}



	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		this.layout = (ScrollView)inflater.inflate(R.layout.dynamic_info, container, false);
		if(layout==null) {
			Log.d(TAG,"layout null");
			return null;
		}
		uiMap.put("accelerometerXSensorValue", (TextView) layout.findViewById(R.id.accelerometerXSensorValue));
		uiMap.put("accelerometerYSensorValue", (TextView) layout.findViewById(R.id.accelerometerYSensorValue));
		uiMap.put("accelerometerZSensorValue", (TextView) layout.findViewById(R.id.accelerometerZSensorValue));
		uiMap.put("gravityXSensorValue", (TextView) layout.findViewById(R.id.gravityXSensorValue));
		uiMap.put("gravityYSensorValue", (TextView) layout.findViewById(R.id.gravityYSensorValue));
		uiMap.put("gravityZSensorValue", (TextView) layout.findViewById(R.id.gravityZSensorValue));
		uiMap.put("gyroscopeXSensorValue", (TextView) layout.findViewById(R.id.gyroscopeXSensorValue));
		uiMap.put("gyroscopeYSensorValue", (TextView) layout.findViewById(R.id.gyroscopeYSensorValue));
		uiMap.put("gyroscopeZSensorValue", (TextView) layout.findViewById(R.id.gyroscopeZSensorValue));
		uiMap.put("linearAccelerationXSensorValue", (TextView) layout.findViewById(R.id.linearAccelerationXSensorValue));
		uiMap.put("linearAccelerationYSensorValue", (TextView) layout.findViewById(R.id.linearAccelerationYSensorValue));
		uiMap.put("linearAccelerationZSensorValue", (TextView) layout.findViewById(R.id.linearAccelerationZSensorValue));
		uiMap.put("magneticFieldXSensorValue", (TextView) layout.findViewById(R.id.magneticFieldXSensorValue));
		uiMap.put("magneticFieldYSensorValue", (TextView) layout.findViewById(R.id.magneticFieldYSensorValue));
		uiMap.put("magneticFieldZSensorValue", (TextView) layout.findViewById(R.id.magneticFieldZSensorValue));
		uiMap.put("rotationVectorXSensorValue", (TextView) layout.findViewById(R.id.rotationVectorXSensorValue));
		uiMap.put("rotationVectorYSensorValue", (TextView) layout.findViewById(R.id.rotationVectorYSensorValue));
		uiMap.put("rotationVectorZSensorValue", (TextView) layout.findViewById(R.id.rotationVectorZSensorValue));
		uiMap.put("orientationXSensorValue", (TextView) layout.findViewById(R.id.orientationXSensorValue));
		uiMap.put("orientationYSensorValue", (TextView) layout.findViewById(R.id.orientationYSensorValue));
		uiMap.put("orientationZSensorValue", (TextView) layout.findViewById(R.id.orientationZSensorValue));
		uiMap.put("worldAccelerationXSensorValue", (TextView) layout.findViewById(R.id.worldAccelerationXSensorValue));
		uiMap.put("worldAccelerationYSensorValue", (TextView) layout.findViewById(R.id.worldAccelerationYSensorValue));
		uiMap.put("worldAccelerationZSensorValue", (TextView) layout.findViewById(R.id.worldAccelerationZSensorValue));
		uiMap.put("displacementXSensorValue", (TextView) layout.findViewById(R.id.displacementXSensorValue));
		uiMap.put("displacementYSensorValue", (TextView) layout.findViewById(R.id.displacementYSensorValue));
		uiMap.put("displacementZSensorValue", (TextView) layout.findViewById(R.id.displacementZSensorValue));
		uiMap.put("logInfo", (TextView) layout.findViewById(R.id.logInfo));
		return layout;
	}

	@Override
	public void onResume() {
		super.onResume();
		sensorinfoTimer = new Timer();
		sensorinfoTimer.scheduleAtFixedRate(new updateUITask(), 50, MainActivity.uiUpdateRate);
	}

	@Override
	public void onPause() {
		super.onPause();
		this.sensorManager.unregisterListener(this);
		this.stopLogging();
		if (sensorinfoTimer!=null)
			this.sensorinfoTimer.cancel();
	}

	public void init(){
		registerSensors();
	}


	public void registerSensors() {
    	Sensor accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    	if(accelSensor!=null) {
    		sensorManager.registerListener(this,accelSensor,sensorDelay);
    	} else {
    		valuesMap.put("accelerometerXSensorValue","No accelerometer sensor available");
			valuesMap.put("accelerometerYSensorValue","-");
			valuesMap.put("accelerometerZSensorValue","-");
    	}
    	

    	Sensor gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
    	if(gyroscopeSensor!=null) {
    		sensorManager.registerListener(this,gyroscopeSensor,sensorDelay);
    	} else {
    		valuesMap.put("gyroscopeXSensorValue","No gyroscope sensor available");
			valuesMap.put("gyroscopeYSensorValue","-");
			valuesMap.put("gyroscopeZSensorValue","-");
    	}
    	
    	Sensor linearAccelerationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
    	if(linearAccelerationSensor!=null) {
    		sensorManager.registerListener(this,linearAccelerationSensor,sensorDelay);
    	} else {
    		valuesMap.put("linearAccelerationXSensorValue","No linear acceleration sensor available");
			valuesMap.put("linearAccelerationYSensorValue","-");
			valuesMap.put("linearAccelerationZSensorValue","-");
    	}
    	
    	Sensor magneticFieldSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    	if(magneticFieldSensor!=null) {
    		sensorManager.registerListener(this,magneticFieldSensor,sensorDelay);
    	} else {
    		valuesMap.put("magneticFieldXSensorValue","No magnetic field sensor available");
			valuesMap.put("magneticFieldYSensorValue","-");
			valuesMap.put("magneticFieldZSensorValue","-");
    	}
    }


	public void stopLogging() {
		if(this.logTimer!=null)
			this.logTimer.cancel();
	}




//*******************************************************************************************************************
//											FUNCTION CALCULATION AND UPDATE
//*******************************************************************************************************************	

	private void updateWorldAcceleration() {
		float[] rotationMatrix = this.orientationFusion.getRotationMatrix();
		if(rotationMatrix!=null && this.linearAccelerometerValues!=null) {
			double[][] result = MatrixHelper.matrixMultiply(rotationMatrix, 3, 3, linearAccelerometerValues, 3, 1);

			this.worldAccelerationX=result[0][0];
			this.worldAccelerationY=result[1][0];
			this.worldAccelerationZ=result[2][0];
			this.logData();
		}
	}

	public void onSensorChanged(SensorEvent event) {
		//Log.d("DR_SensorChanged","Log Sensor Change");
		int sensorType = event.sensor.getType();
		if(sensorType==Sensor.TYPE_ACCELEROMETER){
			accelerometerValues=event.values.clone();
			this.orientationFusion.setAccelerometer(accelerometerValues);

			valuesMap.put("accelerometerXSensorValue",String.valueOf(event.values[0]));
			valuesMap.put("accelerometerYSensorValue",String.valueOf(event.values[1]));
			valuesMap.put("accelerometerZSensorValue",String.valueOf(event.values[2]));
//			   this.updateDisplacement();
		}
		if(sensorType==Sensor.TYPE_GYROSCOPE){
			this.orientationFusion.gyroFunction(event);
			float[] temp = this.orientationFusion.getFusedOrientation();
			gyroscopeValues=event.values.clone();
			valuesMap.put("gyroscopeXSensorValue",Misc.roundToDecimals(event.values[0],4)+" / "+Misc.roundToDecimals(temp[0]*180/3.14,2));
			valuesMap.put("gyroscopeYSensorValue",Misc.roundToDecimals(event.values[1],4)+" / "+Misc.roundToDecimals(temp[1]*180/3.14,2));
			valuesMap.put("gyroscopeZSensorValue",Misc.roundToDecimals(event.values[2],4)+" / "+Misc.roundToDecimals(temp[2]*180/3.14,2));
		}
		if(sensorType==Sensor.TYPE_LINEAR_ACCELERATION){  
			valuesMap.put("linearAccelerationXSensorValue",String.valueOf(event.values[0]));
			valuesMap.put("linearAccelerationYSensorValue",String.valueOf(event.values[1]));
			valuesMap.put("linearAccelerationZSensorValue",String.valueOf(event.values[2]));
			float[] temp = event.values.clone();
			this.linearAccelerometerValues[0]=temp[0];
			this.linearAccelerometerValues[1]=temp[1];
			this.linearAccelerometerValues[2]=temp[2];
			this.updateWorldAcceleration();
		}
		if(sensorType==Sensor.TYPE_MAGNETIC_FIELD){
			magneticFieldValues=event.values.clone();
			this.orientationFusion.setMagneticField(magneticFieldValues);

			valuesMap.put("magneticFieldXSensorValue",String.valueOf(event.values[0]));
			valuesMap.put("magneticFieldYSensorValue",String.valueOf(event.values[1]));
			valuesMap.put("magneticFieldZSensorValue",String.valueOf(event.values[2]));
		}
	}

	public void onAccuracyChanged(Sensor arg0, int arg1) {
	    // TODO Auto-generated method stub
	}

	class updateUITask extends TimerTask {
		public void run() {
			MainActivity.getInstance().runOnUiThread(new Thread(new Runnable() {
				public void run() {
					//Log.d("Sensor_UI", "running updateUITask_Sensor");
					valuesMap.put("logInfo", DataLogManager.getInfo());
					double oFused = SensorFragment.getInstance().orientationFusion.getFusedZOrientation();
					double oGyro = SensorFragment.getInstance().orientationFusion.getGyroscopeZOrientation();
			    	double oCompass = SensorFragment.getInstance().orientationFusion.getCompassZOrientation();
			    	valuesMap.put("orientationXSensorValue",Misc.roundToDecimals(oCompass*180/3.14,2) + " / "+ Misc.roundToDecimals(oFused*180/3.14,2) + "/" + Misc.roundToDecimals(oGyro*180/3.14,2) + " / " + Misc.roundToDecimals(SensorFragment.getInstance().orientationFusion.getOrientationDiscrete()*180/3.14,2));
					valuesMap.put("worldAccelerationXSensorValue", SensorFragment.getInstance().worldAccelerationX + "");
					valuesMap.put("worldAccelerationYSensorValue", SensorFragment.getInstance().worldAccelerationY + "");
					valuesMap.put("worldAccelerationZSensorValue", SensorFragment.getInstance().worldAccelerationZ + "");
					valuesMap.put("displacementXSensorValue", SensorFragment.getInstance().displacementX + "");
					valuesMap.put("displacementYSensorValue", SensorFragment.getInstance().displacementY + "");
					valuesMap.put("displacementZSensorValue", SensorFragment.getInstance().displacementZ + "");
					Iterator<Entry<String, TextView>> it = SensorFragment.getInstance().uiMap.entrySet().iterator();
					while (it.hasNext()) {
						java.util.Map.Entry<String, TextView> pairs = (java.util.Map.Entry<String, TextView>)it.next();
						 if(SensorFragment.getInstance().valuesMap.containsKey(pairs.getKey())) {
			                TextView temp = pairs.getValue();
			                if(temp!=null) {
			                	temp.setText(SensorFragment.getInstance().valuesMap.get(pairs.getKey()));
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

//*******************************************************************************************************************
//												SUPPORT FUNCTIONS 
//*******************************************************************************************************************	

	public View getLayout(){
		return SensorFragment.getInstance().layout;
	}

	public void triggerGyroscopeCalibration() {
		new AlertDialog.Builder(MainActivity.getInstance())
			.setTitle(R.string.gyroscopeCalibrationTitle)
			.setMessage(R.string.gyroscopeCalibrationMsg)
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog, int whichButton) {
			    	orientationFusion.startGyroscopeCalibration();
			    }
		    }).setNegativeButton(android.R.string.cancel,  new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog, int whichButton) {

			    }
		    }).show();
	}


	private void logData() {
		int steps = 0;
    	if (DRFragment.getInstance()!=null) steps = DRFragment.getInstance().getSteps();
    	float[] oFused = orientationFusion.getFusedOrientation();
    	float[] orgGyro = orientationFusion.getOriginalGyroscopeOrientation();
    	float[] oCompass = orientationFusion.getCompassOrientation();
    	float[] rm = orientationFusion.getRotationMatrix();
    	String line = oFused[0]+","+oFused[1]+","+oFused[2]+","
    			+ oCompass[0]+","+oCompass[1]+","+oCompass[2]+","
    			+ worldAccelerationX + "," + worldAccelerationY + "," + worldAccelerationZ + ","
    			+ magneticFieldValues[0] + "," + magneticFieldValues[1]+ ","+magneticFieldValues[2] + ","
    			+ steps + ","
    			+gyroscopeValues[0]+","+gyroscopeValues[1]+","+gyroscopeValues[2]+","
    			+accelerometerValues[0]+","+accelerometerValues[1]+","+accelerometerValues[2]+","
    			+rm[0]+","+rm[1]+","+rm[2]+","
    			+rm[3]+","+rm[4]+","+rm[5]+","
    			+rm[6]+","+rm[7]+","+rm[8]+","
    			+orgGyro[0]+","+orgGyro[1]+","+orgGyro[2]
    					;
    	DataLogManager.addLine("datalog",line);
}


	
	public void reloadSettings(int sensorDelay, float gyroscopeXOffset, float gyroscopeYOffset, float gyroscopeZOffset, short orientationSource, float filterCoefficient) {
		this.sensorDelay=sensorDelay;
		this.orientationFusion.reloadSettings(gyroscopeXOffset,gyroscopeYOffset,gyroscopeZOffset,orientationSource,filterCoefficient);
	}

	public float getWorldAccelerationZ() {
		return (float)this.worldAccelerationZ;
	}

	public float getWorldAccelerationX() {
		return (float)this.worldAccelerationX;
	}

}