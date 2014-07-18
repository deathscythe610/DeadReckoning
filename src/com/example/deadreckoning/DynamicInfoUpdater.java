/*
 * THIS CLASS IS USED TO UPDATE THE DYNAMIC INFORMATION 
 * UPDATE THE INFOCLASSMAP WHICH INCLUDE: SENSOR_INFO, DEAD_RECKONING, MAP_INFO
 */
package com.example.deadreckoning;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import android.os.Handler;
import android.util.Log;
import android.widget.TextView;
import android.support.v4.app.Fragment;

public class DynamicInfoUpdater {
	private static final String TAG = "TM_DynamicInfoUpdater";
	
	private Handler diuHandler=null;
	private int uiUpdateRate;
	private Map<Integer,FragmentControl> fragmentClassMap = new HashMap<Integer,FragmentControl>();
	private Map<Integer,Info> infoClassMap = new HashMap<Integer,Info>();
	
	public DynamicInfoUpdater(Map<Integer,FragmentControl>fragment, Map<Integer,Info>info) {
		this.infoClassMap=info;
		this.fragmentClassMap=fragment;
		//THIS ONLY RUN ONCE
		this.initInfo();
		this.updateFragment();
	}
	
	public void restart(int uiUpdateRate) {
		this.uiUpdateRate=uiUpdateRate;
		this.restart();
	}
	
	protected void restart() {
		if(diuHandler!=null) {
        	diuHandler.removeCallbacks(diuRunner);
        	diuRunner.run();
        }
	}
	
	private void updateFragment() {
    	diuHandler = new Handler();
        diuRunner.run();
	}
	
	protected void initInfo(){
		Iterator<Entry<Integer, Info>> iter = this.infoClassMap.entrySet().iterator();
		while (iter.hasNext()){
			Entry<Integer, Info> pair = iter.next();
			Info tempClass = pair.getValue();
			tempClass.init();
		}
	}
	
	//THIS RUNS EVERY uiUpdateRate time
	protected void updateUI() {
    	Iterator<Entry<Integer, FragmentControl>> iter = this.fragmentClassMap.entrySet().iterator();
    	while (iter.hasNext()) {
    		Entry<Integer, FragmentControl> pair = iter.next();
    		FragmentControl tempClass = pair.getValue();
    		tempClass.updateUI();
    		Iterator<Entry<String, TextView>> it = tempClass.uiMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, TextView> pairs = (Map.Entry<String, TextView>)it.next();
//                Log.d(TAG,pairs.toString());
                if(tempClass.valuesMap.containsKey(pairs.getKey())) {
                	TextView temp = pairs.getValue();
                	if(temp!=null) {
                		temp.setText(tempClass.valuesMap.get(pairs.getKey()));
                	} else {
                		Log.d(TAG,pairs.toString());
                	}
                }
            }
    	}
    	
    }
    
    Runnable diuRunner = new Runnable()
    {
		public void run() {
              updateUI();
              diuHandler.postDelayed(diuRunner, uiUpdateRate);
         }
    };
}