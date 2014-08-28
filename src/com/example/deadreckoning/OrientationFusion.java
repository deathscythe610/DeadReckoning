/*
 * THIS CLASS:
 * 1. REGISTER GYRO AND MAGNETOMETER 
 * 2. COLLECT DATA FROM 2 SENSORS 
 * 3. COMBINE BOTH RESULTS TO GET FUSED RESULTS (USING COMPLEMENTARY FILTER)
 */
package com.example.deadreckoning;

import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.SharedPreferences;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.FloatMath;

public class OrientationFusion {
	private static final String TAG = "TM_OrientationFusion";
	
	
	private float timestamp;
	private boolean initState = true;
	public static final float EPSILON = 0.000000001f;
    private static final float NS2S = 1.0f / 1000000000.0f;
    public static final int TIME_CONSTANT = 30;
	public float filterCoefficient = 0.95f;
	private Timer fuseTimer = new Timer();
//	private float orientationOffset = 0;
	private double  DIRECTION_DIV = 2*Math.PI/8;
	private short orientationSource = 2;
	
    private float[] gyroMatrix = {1.0f, 0.0f, 0.0f,   // rotation matrix from gyro data
    								0.0f, 1.0f, 0.0f,
    								0.0f, 0.0f, 1.0f};
    private float[] gyroscopeOrientation = new float[3]; // orientation angles from gyro matrix
    private float[] gyroscopeOriginalOrientation = new float[3];
	
 
    private float[] magneticField = new float[3]; // magnetic field vector
    private float[] accelerometer = new float[3]; // accelerometer vector
 
    
    private float[] compassOrientation = new float[3]; // orientation angles from accel and magnet
    private float[] fusedOrientation = new float[3]; // final orientation angles from sensor fusion
 
    private float[] rotationMatrix = new float[9]; // accelerometer and magnetometer based rotation matrix
    
    private static final int gyroscopeCalibrationLength = 60;// [s]
	private float[] gyroscopeOffset = {0,0,0};
	
	public OrientationFusion() {
        fuseTimer.scheduleAtFixedRate(new calculateFusedOrientationTask(), 1000, TIME_CONSTANT);
	}
	
	public void setAccelerometer(float[] in) {
		this.accelerometer=in;
		this.calculateCompassOrientation();
	}
	
	// calculates orientation angles from accelerometer and magnetometer output
	public void calculateCompassOrientation() {
	    if(SensorManager.getRotationMatrix(rotationMatrix, null, accelerometer, magneticField)) {
	        SensorManager.getOrientation(rotationMatrix, compassOrientation);
	    }
	}
	
	public void setMagneticField(float[] in) {
		this.magneticField=in;
	}
	
	// This function is borrowed from the Android reference
	// at http://developer.android.com/reference/android/hardware/SensorEvent.html#values
	// It calculates a rotation vector from the gyroscope angular speed values.
    private void getGyroscopeRotationVector(float[] gyroValues, float[] deltaRotationVector, float dt) {
		float[] normValues = new float[3];
		
		// Calculate the angular speed of the sample
		float omegaMagnitude = FloatMath.sqrt(gyroValues[0] * gyroValues[0] +
				gyroValues[1] * gyroValues[1] + gyroValues[2] * gyroValues[2]);
		
		// Normalize the rotation vector if it's big enough to get the axis
		if(omegaMagnitude > EPSILON) {
			normValues[0] = gyroValues[0] / omegaMagnitude;
			normValues[1] = gyroValues[1] / omegaMagnitude;
			normValues[2] = gyroValues[2] / omegaMagnitude;
		}
		
		// Integrate around this axis with the angular speed by the timestep
		// in order to get a delta rotation from this sample over the timestep
		// We will convert this axis-angle representation of the delta rotation
		// into a quaternion before turning it into the rotation matrix.
		float thetaOverTwo = omegaMagnitude * dt  / 2.0f;
		float sinThetaOverTwo = FloatMath.sin(thetaOverTwo);
		float cosThetaOverTwo = FloatMath.cos(thetaOverTwo);
		deltaRotationVector[0] = sinThetaOverTwo * normValues[0];
		deltaRotationVector[1] = sinThetaOverTwo * normValues[1];
		deltaRotationVector[2] = sinThetaOverTwo * normValues[2];
		deltaRotationVector[3] = cosThetaOverTwo;
	}
	    
	 // This function performs the integration of the gyroscope data.
	    // It writes the gyroscope based orientation into gyroOrientation.
	    @SuppressLint("NewApi")
		public void gyroFunction(SensorEvent event) {
	        // don't start until first accelerometer/magnetometer orientation has been acquired
	    	if (initState && compassOrientation[0] == 0)
	            return;
	     
	        // Initialization of the gyroscope based rotation matrix
	        if(initState) {
	        	gyroMatrix = getRotationMatrixFromOrientation(compassOrientation);//initMatrix
	            initState = false;
	        }
	     
	        // copy the new gyro values into the gyro array
	        // convert the raw gyro data into a rotation vector
	        float[] deltaVector = new float[4];
	        if(timestamp != 0) {
	            final float dT = (event.timestamp - timestamp) * NS2S;
	        	getGyroscopeRotationVector(event.values, deltaVector, dT);
	        }
	        timestamp = event.timestamp; // measurement done, save current time for next interval
	     
	        // convert rotation vector into rotation matrix
	        float[] deltaMatrix = new float[9];
	        SensorManager.getRotationMatrixFromVector(deltaMatrix, deltaVector);
	     
	        // apply the new rotation interval on the gyroscope based rotation matrix
	        gyroMatrix = MatrixHelper.arrayMultiply33(gyroMatrix, deltaMatrix);
	     
	        // get the gyroscope based orientation from the rotation matrix
	        SensorManager.getOrientation(gyroMatrix, gyroscopeOrientation);
	        gyroscopeOriginalOrientation=gyroscopeOrientation.clone();
	    }                              
	    
	    private float[] getRotationMatrixFromOrientation(float[] o) {
	        float[] xM = new float[9];
	        float[] yM = new float[9];
	        float[] zM = new float[9];
	     
	        float sinX = FloatMath.sin(o[1]);
	        float cosX = FloatMath.cos(o[1]);
	        float sinY = FloatMath.sin(o[2]);
	        float cosY = FloatMath.cos(o[2]);
	        float sinZ = FloatMath.sin(o[0]);
	        float cosZ = FloatMath.cos(o[0]);
	     
	        // rotation about x-axis (pitch)
	        xM[0] = 1.0f; xM[1] = 0.0f; xM[2] = 0.0f;
	        xM[3] = 0.0f; xM[4] = cosX; xM[5] = sinX;
	        xM[6] = 0.0f; xM[7] = -sinX; xM[8] = cosX;
	     
	        // rotation about y-axis (roll)
	        yM[0] = cosY; yM[1] = 0.0f; yM[2] = sinY;
	        yM[3] = 0.0f; yM[4] = 1.0f; yM[5] = 0.0f;
	        yM[6] = -sinY; yM[7] = 0.0f; yM[8] = cosY;
	     
	        // rotation about z-axis (azimuth)
	        zM[0] = cosZ; zM[1] = sinZ; zM[2] = 0.0f;
	        zM[3] = -sinZ; zM[4] = cosZ; zM[5] = 0.0f;
	        zM[6] = 0.0f; zM[7] = 0.0f; zM[8] = 1.0f;
	     
	        // rotation order is y, x, z (roll, pitch, azimuth)
	        float[] resultMatrix = MatrixHelper.arrayMultiply33(xM, yM);
	        resultMatrix = MatrixHelper.arrayMultiply33(zM, resultMatrix);
	        return resultMatrix;
	    }
	    
	    class calculateFusedOrientationTask extends TimerTask {
	        public void run() {
	            float oneMinusCoeff = 1.0f - filterCoefficient;
	            
	            /*
	             * Fix for 179° <--> -179° transition problem:
	             * Check whether one of the two orientation angles (gyro or accMag) is negative while the other one is positive.
	             * If so, add 360° (2 * math.PI) to the negative value, perform the sensor fusion, and remove the 360° from the result
	             * if it is greater than 180°. This stabilizes the output in positive-to-negative-transition cases.
	             */
	            
	            // azimuth
	            if (gyroscopeOrientation[0] < -0.5 * Math.PI && compassOrientation[0] > 0.0) {
	            	fusedOrientation[0] = (float) (filterCoefficient * (gyroscopeOrientation[0] + 2.0 * Math.PI) + oneMinusCoeff * compassOrientation[0]);
	        		fusedOrientation[0] -= (fusedOrientation[0] > Math.PI) ? 2.0 * Math.PI : 0;
	            }
	            else if (compassOrientation[0] < -0.5 * Math.PI && gyroscopeOrientation[0] > 0.0) {
	            	fusedOrientation[0] = (float) (filterCoefficient * gyroscopeOrientation[0] + oneMinusCoeff * (compassOrientation[0] + 2.0 * Math.PI));
	            	fusedOrientation[0] -= (fusedOrientation[0] > Math.PI)? 2.0 * Math.PI : 0;
	            }
	            else {
	            	fusedOrientation[0] = filterCoefficient * gyroscopeOrientation[0] + oneMinusCoeff * compassOrientation[0];
	            }
	            
	            // pitch
	            if (gyroscopeOrientation[1] < -0.5 * Math.PI && compassOrientation[1] > 0.0) {
	            	fusedOrientation[1] = (float) (filterCoefficient * (gyroscopeOrientation[1] + 2.0 * Math.PI) + oneMinusCoeff * compassOrientation[1]);
	        		fusedOrientation[1] -= (fusedOrientation[1] > Math.PI) ? 2.0 * Math.PI : 0;
	            }
	            else if (compassOrientation[1] < -0.5 * Math.PI && gyroscopeOrientation[1] > 0.0) {
	            	fusedOrientation[1] = (float) (filterCoefficient * gyroscopeOrientation[1] + oneMinusCoeff * (compassOrientation[1] + 2.0 * Math.PI));
	            	fusedOrientation[1] -= (fusedOrientation[1] > Math.PI)? 2.0 * Math.PI : 0;
	            }
	            else {
	            	fusedOrientation[1] = filterCoefficient * gyroscopeOrientation[1] + oneMinusCoeff * compassOrientation[1];
	            }
	            
	            // roll
	            if (gyroscopeOrientation[2] < -0.5 * Math.PI && compassOrientation[2] > 0.0) {
	            	fusedOrientation[2] = (float) (filterCoefficient * (gyroscopeOrientation[2] + 2.0 * Math.PI) + oneMinusCoeff * compassOrientation[2]);
	        		fusedOrientation[2] -= (fusedOrientation[2] > Math.PI) ? 2.0 * Math.PI : 0;
	            }
	            else if (compassOrientation[2] < -0.5 * Math.PI && gyroscopeOrientation[2] > 0.0) {
	            	fusedOrientation[2] = (float) (filterCoefficient * gyroscopeOrientation[2] + oneMinusCoeff * (compassOrientation[2] + 2.0 * Math.PI));
	            	fusedOrientation[2] -= (fusedOrientation[2] > Math.PI)? 2.0 * Math.PI : 0;
	            }
	            else {
	            	fusedOrientation[2] = filterCoefficient * gyroscopeOrientation[2] + oneMinusCoeff * compassOrientation[2];
	            }
	     
	            // overwrite gyro matrix and orientation with fused orientation
	            // to comensate gyro drift
	            gyroMatrix = getRotationMatrixFromOrientation(fusedOrientation);
	            System.arraycopy(fusedOrientation, 0, gyroscopeOrientation, 0, 3);
	        }
	    }
	
    public float[] getGyroscopeOrientation() {
		return this.gyroscopeOrientation;
	}
    
    public double getGyroscopeZOrientation() {
		return this.gyroscopeOrientation[0]+ MapFragment.getInstance().getCurMap().getOrientationOffsetRadians();
	}
    
    public float[] getCompassOrientation() {
		return this.compassOrientation;
	}
    
    public double getCompassZOrientation() {
		return this.compassOrientation[0]+ MapFragment.getInstance().getCurMap().getOrientationOffsetRadians();
	}
	
	public void startGyroscopeCalibration() {
		for(int i=0;i<3;i++) {
			this.gyroscopeOffset[i]=0;
		}
		final Handler h = new Handler();
		Runnable runner = new Runnable()
	    {
			float dx=-gyroscopeOriginalOrientation[0];
			float dy=-gyroscopeOriginalOrientation[1];
			float dz=-gyroscopeOriginalOrientation[2];
			ProgressDialog pd=null;
			
			public void run() {
				if(pd!=null) {
					pd.dismiss();
				}
				
				this.dx+=gyroscopeOriginalOrientation[0];
				this.dy+=gyroscopeOriginalOrientation[1];
				this.dz+=gyroscopeOriginalOrientation[2];
				
				SharedPreferences preferences = PreferenceManager
		                .getDefaultSharedPreferences(MainActivity.getInstance());
				SharedPreferences.Editor editor = preferences.edit();
				editor.putString("gyroscopeXOffset",String.valueOf(this.dx));
				editor.putString("gyroscopeYOffset",String.valueOf(this.dy));
				editor.putString("gyroscopeZOffset",String.valueOf(this.dz));
				editor.commit();
				MainActivity.getInstance().reloadSettings();
				Misc.toast(R.string.calibrationSuccess);
	         }
			
			{
				final Runnable self = this;
				pd = ProgressDialog.show(MainActivity.getInstance(), "", MainActivity.getInstance().getString(R.string.gyroscopeCalibrationProgress), true, true);
				pd.setOnCancelListener(new OnCancelListener() {
					
					public void onCancel(DialogInterface dialog) {
						Misc.toast(R.string.calibrationCancelled);
						h.removeCallbacks(self);
					}
				});
			}
	    };
	    h.postDelayed(runner, gyroscopeCalibrationLength*1000);
	}

	public void reloadSettings(float gyroscopeXOffset, float gyroscopeYOffset, float gyroscopeZOffset, short orientationSource, float filterCoefficient) {
		this.gyroscopeOffset[0]=gyroscopeXOffset/(float)gyroscopeCalibrationLength;
		this.gyroscopeOffset[1]=gyroscopeYOffset/(float)gyroscopeCalibrationLength;
		this.gyroscopeOffset[2]=gyroscopeZOffset/(float)gyroscopeCalibrationLength;
		this.orientationSource=orientationSource;
		this.filterCoefficient=filterCoefficient;
	}
	
	protected double getOrientationDiscrete() {
		double o = this.getFusedOrientation()[0]+ MapFragment.getInstance().getCurMap().getOrientationOffsetRadians();
		o= Math.round(o/this.DIRECTION_DIV)*this.DIRECTION_DIV;
		return o;
	}

	public float[] getFusedOrientation() {
		return this.fusedOrientation;
	}
	
	public double getFusedZOrientation() {
		return this.fusedOrientation[0]+ MapFragment.getInstance().getCurMap().getOrientationOffsetRadians();
	}

	
	public float[] getOriginalGyroscopeOrientation() {
		return this.gyroscopeOriginalOrientation;
	}
	
	public float[] getRotationMatrix() {
		return this.rotationMatrix;
	}

	public String getOrientationSource() {
		switch(this.orientationSource) {
			case 0:
				return "Compass";
			case 1:
				return "Gyroscope";
			case 3:
				return "Discrete (fused)";
			case 2:
			default:
				return "Fused";
		}
	}
	
	public double getOrientation() {
		switch(this.orientationSource) {
			case 0:
				return getCompassZOrientation();
			case 1:
				return getGyroscopeZOrientation();
			case 3:
				return getOrientationDiscrete();
			case 2:
			default:
				return getFusedZOrientation();
		}
	}
	
	public float getFilterCoefficient() {
		return this.filterCoefficient;
	}
	
}
