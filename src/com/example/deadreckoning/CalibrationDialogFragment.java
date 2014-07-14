/*
 * THIS CLASS IS USED TO INITIATE CALIBRATION FRAGMENT MESSAGE VIEW ON PHONE WHEN CALIBRATION IS SELECTED 
 */
package com.example.deadreckoning;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

public class CalibrationDialogFragment extends DialogFragment {
	private OnClickListener calibrationDoneHandler=null;
	
	static CalibrationDialogFragment newInstance() {
		CalibrationDialogFragment cdf = new CalibrationDialogFragment();
		return cdf;
	}
	
	
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
	}
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.calibration_layout, container, false);
        
        Button doneBtn = (Button) v.findViewById(R.id.calibrationDone);
		doneBtn.setOnClickListener(this.calibrationDoneHandler);
		
        return v;
    }
	
	public void setDoneHandler(OnClickListener l) {
		this.calibrationDoneHandler=l;
	}
	
//	public View getView() {
//		return this.getLayoutInflater(null).inflate(R.layout.calibration_layout, null, false);
//	}
}
