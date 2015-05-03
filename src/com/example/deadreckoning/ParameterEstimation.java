/*
 * THIS CLASS IS USED TO:
 * 1.RECORD ACCELERATION METHOD
 * 2.PROMT CALIBRATION DIALOG
 * 3.FIND THRESHOLD AND K IN CALIBRATION, SAVE FOUND PARAMETERS
 */
package com.example.deadreckoning;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;

public class ParameterEstimation {
	protected static final String TAG = "TM_ParameterEstimation";
	
	private float kStep=0.01f;
	private float kStart=0.01f;
	private float kEnd=1.5f;
	private float thresholdStep=0.01f;
	
	private int maxThresholdPasses = 0;
	private int minThresholdPasses = 0;

	private float distanceError;
	
	private float minThreshold;
	private float maxThreshold;
	private float K;
	
	private int stepsReported = 0;
	private float distanceReported = 0;
	
	private LinkedHashMap<Long, Float> azData = new LinkedHashMap<Long, Float>(); // timestamp => acceleration
	private DRFragment dr;
	
	public ParameterEstimation(DRFragment dr) {
		this.dr=dr;
	}
	
	public void recordAcceleration(float val) {
		this.azData.put(Misc.getTime(), val);
	}
	
	public void findParameters() {
		if(this.findThresholdValues()) {
			this.findKValue();
			this.saveParameters();
			Misc.toast("Calibration successful!");
		} else {
			Misc.toast("Calibration error, please try again.");
		}
		this.dr.endCalibration();
	}
	
	private void findKValue() {
		float bestDistanceError=Float.MAX_VALUE;
		float bestK=0;
		for(float K = this.kStart;K<this.kEnd;K+=this.kStep) {
			this.testThreshold(this.minThreshold, this.maxThreshold,K,false);
			if(this.distanceError<bestDistanceError) {
				bestK=K;
				bestDistanceError=this.distanceError;
			}
		}
		this.K=bestK;
	}
	
	private Boolean findThresholdValues() {
		float[] minMax = this.findMinMax();
		
		float minThreshold = minMax[0];
		float maxThreshold = minMax[1];
		float maxThresholdOut=0f;
		float minThresholdOut=0f;
		
		boolean foundMaxThreshold = false;
		int loopMax=2000;
		int i = 0;
		DataLogManager.addLine("piLog", "% steps reported: "+this.stepsReported,false);
		while(!foundMaxThreshold && maxThreshold>0f && i<loopMax) {
			this.testThreshold(minThreshold/2, maxThreshold,0,false);
			DataLogManager.addLine("piLog", "% max/minTh: "+maxThreshold+"/"+minThreshold/2,false);
			DataLogManager.addLine("piLog", "% maxPasses: "+this.maxThresholdPasses,false);
			DataLogManager.addLine("piLog", "% -----------",false);
			if(this.maxThresholdPasses==this.stepsReported && maxThresholdOut==0f) {
				maxThresholdOut=maxThreshold;
//				DataLogManager.addLine("PE", "first thresholdMax found: "+maxThreshold);
			} else if (this.maxThresholdPasses>this.stepsReported && maxThresholdOut!=0f) {
				foundMaxThreshold=true;
				maxThresholdOut+=maxThreshold;
				maxThresholdOut=maxThresholdOut/2;
//				DataLogManager.addLine("PE", "second thresholdMax found: "+maxThreshold);
//				DataLogManager.addLine("PE", "calculated thresholdMax: "+maxThresholdOut);
			}
			
			i++;
			maxThreshold-=this.thresholdStep;
		}
		this.testThreshold(minThreshold/2, maxThresholdOut,0,true);
		
		boolean foundMinThreshold = false;
		i=0;
		while(!foundMinThreshold && minThreshold<0f && i<loopMax) {
			this.testThreshold(minThreshold, maxThresholdOut,0,false);
			DataLogManager.addLine("piLog", "% max/minTh: "+maxThresholdOut+"/"+minThreshold,false);
			DataLogManager.addLine("piLog", "% minPasses: "+this.minThresholdPasses,false);
			DataLogManager.addLine("piLog", "% -----------",false);
			if(this.minThresholdPasses==this.stepsReported && minThresholdOut==0f) {
				minThresholdOut=minThreshold/2;
				foundMinThreshold=true;
//				DataLogManager.addLine("PE", "thresholdMin found: "+minThresholdOut);
			}
			
			i++;
			minThreshold+=this.thresholdStep;
		}
//		DataLogManager.saveLog("PE");
		this.logRecordedAcceleration();
		DataLogManager.saveLog("piLog");
		if(foundMaxThreshold && foundMinThreshold) {
			this.minThreshold=minThresholdOut;
			this.maxThreshold=maxThresholdOut;
			return true;
			
		} else {
			return false;
		}
	}
	
	private void logRecordedAcceleration() {
		Iterator<Entry<Long, Float>> it = this.azData.entrySet().iterator();
        while (it.hasNext()) {
        	Map.Entry<Long, Float> pairs = (Map.Entry<Long, Float>)it.next();
        	long timestamp = pairs.getKey();
        	float az = pairs.getValue();
        	DataLogManager.addLine("piLog", timestamp+", "+az);
        }
	}
	
	private void saveParameters() {
		SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(MainActivity.getInstance());
		SharedPreferences.Editor editor = preferences.edit();
		editor.putString("drThresholdMin",String.valueOf(this.minThreshold));
		editor.putString("drThresholdMax",String.valueOf(this.maxThreshold));
		editor.putString("drK",String.valueOf(this.K));
		editor.commit();
	}
	
	private void testThreshold(float minThreshold, float maxThreshold, float K, boolean stateLogging) {
		DRFragment dr = new DRFragment();
		dr.stateLogging=stateLogging;
		dr.setParameters(maxThreshold,minThreshold, K);
		
		Iterator<Entry<Long, Float>> it = this.azData.entrySet().iterator();
        while (it.hasNext()) {
        	Map.Entry<Long, Float> pairs = (Map.Entry<Long, Float>)it.next();
        	long timestamp = pairs.getKey();
        	float az = pairs.getValue();
        	dr.trigger_zhangyu(az, 0, timestamp);
        }
//        this.stepsCounted = dr.getSteps();
        this.maxThresholdPasses = dr.getMaxThresholdPasses();
        this.minThresholdPasses = dr.getMinThresholdPasses();
        this.distanceError=Math.abs(this.distanceReported-dr.getDistance());
//        DataLogManager.addLine("PE", "For parameters "+minThreshold+"/"+maxThreshold+"; found steps "+this.stepsCounted+"("+this.minThresholdPasses+"/"+this.maxThresholdPasses+")");
        if(stateLogging) {
//        	DataLogManager.saveLog("DR");
        }
	}
	
	private float[] findMinMax() {
		float max=0;
		float min=0;
		float val;
		Iterator<Entry<Long, Float>> it = this.azData.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long,Float> pairs = it.next();
            val = pairs.getValue();
            if(val>max) {
            	max=val;
            }
            if(val<min) {
            	min=val;
            }
        }
        float[] ret = new float[2];
        ret[0]=min;
        ret[1]=max;
        return ret;
	}
	
	public void calibrationPromptDialog() {
		FragmentTransaction ft = MainActivity.getInstance().getSupportFragmentManager().beginTransaction();
		
	    final CalibrationDialogFragment newFragment = CalibrationDialogFragment.newInstance();
	    newFragment.setDoneHandler(new OnClickListener() {
			
			public void onClick(View v) {
				String temp;
				EditText stepsReportedText = (EditText)newFragment.getView().findViewById(R.id.stepsReported);
				temp = stepsReportedText.getText().toString().trim();
				if(!temp.equals("")) {
					stepsReported = Integer.valueOf(temp);
					
					EditText distanceReportedText = (EditText)newFragment.getView().findViewById(R.id.distanceReported);
					temp = distanceReportedText.getText().toString().trim();
					if(!temp.equals("")) {
						distanceReported = Float.valueOf(temp);
						
						newFragment.dismiss();
						findParameters();
					} else {
						Misc.toast("Please fill in distance walked!");
					}
				} else {
					Misc.toast("Please fill in number of steps walked!");
				}
			}
		});
	    newFragment.show(ft, "calibrationDialog");
	}
}