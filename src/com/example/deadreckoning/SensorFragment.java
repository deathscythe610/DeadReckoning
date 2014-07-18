package com.example.deadreckoning;


import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.os.Bundle;



public class SensorFragment extends FragmentControl{
	
	private static final String TAG = "Sensor_Fragment";
	public View layout;
	private int mCurrentPage;
	private String pageTitle;
	public SensorFragment(){
		super();
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
		mCurrentPage = getArguments().getInt("current_page", 0);
		pageTitle = getArguments().getString("page_title");
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
	public void updateUI() {//update is also done asynchronously onSensorChanged()
		valuesMap.put("logInfo", DataLogManager.getInfo());
		float oFused = MainActivity.getInstance().sensorInfo.orientationFusion.getFusedZOrientation();
		float oGyro = MainActivity.getInstance().sensorInfo.orientationFusion.getGyroscopeZOrientation();
    	float oCompass = MainActivity.getInstance().sensorInfo.orientationFusion.getCompassZOrientation();
    	valuesMap.put("orientationXSensorValue",Misc.roundToDecimals(oCompass*180/3.14,2) + " / "+ Misc.roundToDecimals(oFused*180/3.14,2) + "/" + Misc.roundToDecimals(oGyro*180/3.14,2) + " / " + Misc.roundToDecimals(MainActivity.getInstance().sensorInfo.orientationFusion.getOrientationDiscrete()*180/3.14,2));
		valuesMap.put("worldAccelerationXSensorValue", MainActivity.getInstance().sensorInfo.worldAccelerationX + "");
		valuesMap.put("worldAccelerationYSensorValue", MainActivity.getInstance().sensorInfo.worldAccelerationY + "");
		valuesMap.put("worldAccelerationZSensorValue", MainActivity.getInstance().sensorInfo.worldAccelerationZ + "");
		valuesMap.put("displacementXSensorValue", MainActivity.getInstance().sensorInfo.displacementX + "");
		valuesMap.put("displacementYSensorValue", MainActivity.getInstance().sensorInfo.displacementY + "");
		valuesMap.put("displacementZSensorValue", MainActivity.getInstance().sensorInfo.displacementZ + "");
	}
	
	public View getLayout(){
		return this.layout;
	}
	

}
