package com.example.deadreckoning;

import java.util.HashMap;
import java.util.Map;


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
	
	Map<String, String> valuesMap = new HashMap<String, String>(); // name -> value
	Map<String, TextView> uiMap = new HashMap<String, TextView>(); // name -> TextView
	
	private static final String TAG = "DR_Fragment";
	private int mCurrentPage;
	private String pageTitle;
	private ScrollView layout;
	
	public static DRFragment newInstance(int position, String title){
    	DRFragment drFragment = new DRFragment();
    	Bundle args = new Bundle();
    	args.putInt("current_page", 1);
    	args.putString("page_tile", "DR Information");
    	drFragment.setArguments(args);
    	return drFragment;
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
		this.layout = (ScrollView)inflater.inflate(R.layout.dead_reckoning, container, false);
		if(layout==null) {
			Log.d(TAG,"layout null");
			return null;
		}
		uiMap.put("steps", (TextView) layout.findViewById(R.id.stepsValue));
		uiMap.put("statesLog", (TextView) layout.findViewById(R.id.statesLog));
		uiMap.put("distance", (TextView) layout.findViewById(R.id.distanceValue));
		return super.onCreateView(inflater, container, savedInstanceState);
	}

	@Override
	public void updateUI(){
		valuesMap.put("steps", MainActivity.getInstance().deadReckoning.steps+"");
		valuesMap.put("statesLog", MainActivity.getInstance().deadReckoning.stateLog);
		valuesMap.put("distance", MainActivity.getInstance().deadReckoning.distance+"");
	}
		

	public View getLayout(){
		return this.layout;
	}

	
}
