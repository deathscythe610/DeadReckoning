/*
 * THIS CLASS HOLDS ALL THE INFORMATION ABOUT SENSORS:
 * 1. SENSOR USED: ACCLELEROMETER, GYROSCOPE, MAGNETOMETER
 * 2. LOG SENSOR DATA
 * 3. CREATE UI MAP FOR THE SENSOR INFO PAGE 
 * 4.UPDATE WORLD ACCELERATION USING ORIENTATION FUSION RESULT AND ACCELERATION RAW RESULT
 */
package com.example.deadreckoning;

import java.util.Timer;
import java.util.TimerTask;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;


public class SensorInfo extends Info  implements SensorEventListener{
	private static final String TAG = "TM_SensorInfo";
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
	
	
	public SensorInfo(){
		super();
		this.linearAccelerometerValues=new float[4];
		this.linearAccelerometerValues[3]=0;
		this.sensorManager = (SensorManager)MainActivity.getInstance().getSystemService(Context.SENSOR_SERVICE);
		this.orientationFusion=new OrientationFusion();
	}
	
	public void stopLogging() {
		if(this.logTimer!=null)
			this.logTimer.cancel();
	}
	
	class logOrientationTask extends TimerTask {
		public void run() {
		}
	}
		
    public void logData() {
    	float[] oFused = orientationFusion.getFusedOrientation();
    	float[] orgGyro = orientationFusion.getOriginalGyroscopeOrientation();
    	float[] oCompass = orientationFusion.getCompassOrientation();
    	float[] rm = orientationFusion.getRotationMatrix();
    	String line = oFused[0]+","+oFused[1]+","+oFused[2]+","
    			+ oCompass[0]+","+oCompass[1]+","+oCompass[2]+","
    			+ worldAccelerationX + "," + worldAccelerationY + "," + worldAccelerationZ + ","
    			+ magneticFieldValues[0] + "," + magneticFieldValues[1]+ ","+magneticFieldValues[2] + ","
    			+ MainActivity.getInstance().deadReckoning.getSteps() + ","
    			+gyroscopeValues[0]+","+gyroscopeValues[1]+","+gyroscopeValues[2]+","
    			+accelerometerValues[0]+","+accelerometerValues[1]+","+accelerometerValues[2]+","
    			+rm[0]+","+rm[1]+","+rm[2]+","
    			+rm[3]+","+rm[4]+","+rm[5]+","
    			+rm[6]+","+rm[7]+","+rm[8]+","
    			+orgGyro[0]+","+orgGyro[1]+","+orgGyro[2]
    					;
    	DataLogManager.addLine("datalog",line);
    }

        

	@Override
	void init() {
		registerSensors();
		DataLogManager.addLine("datalog", "% time | 3x orientation fused | 3x orientation compass | " +
				"3x accelerometer world | 3x magnetic field | steps | distance | x | y | " +
				"3x gyroscope raw | 3x acceleration | 9x rotation matrix | 3x gyroscope original",false);
		DataLogManager.addLine("datalog", "% orientation source: "+this.orientationFusion.getOrientationSource(),false);
		DataLogManager.addLine("datalog", "%%% K="+MainActivity.getInstance().deadReckoning.getK()+";",false);
		DataLogManager.addLine("datalog", "%%% orientationOffset="+MainActivity.getInstance().mapFragment.getCurMap().getOrientationOffsetRadians()+";",false);
		DataLogManager.addLine("datalog", "%%% filterCoefficient="+this.orientationFusion.getFilterCoefficient()+";",false);
	}
	
	@Override
	void update() {
	}
	

	
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
	
	public void onSensorChanged(SensorEvent event) {
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
