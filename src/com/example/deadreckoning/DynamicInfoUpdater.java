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
	private Map<Integer,Fragment> fragmentClassMap = new HashMap<Integer,Fragment>();
	private Map<Integer,Info> infoClassMap = new HashMap<Integer,Info>();
	
	public DynamicInfoUpdater(Map<Integer,Fragment> fragment, Map<Integer,Info> info) {
		this.infoClassMap=info;
		this.fragmentClassMap=fragment;
		this.init();
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
	
	private void init() {
    	diuHandler = new Handler();
        diuRunner.run();
	}
	
	protected void update() {
    	Iterator<Entry<Integer, Fragment>> iter = this.fragmentClassMap.entrySet().iterator();
    	while (iter.hasNext()) {
    		Entry<Integer, Fragment> pair = iter.next();
    		Fragment tempClass = pair.getValue();
        	tempClass.update();
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
	
	protected void initUI(int pos) {
		if(this.infoClassMap.containsKey(pos)) {
			Info tempClass = this.infoClassMap.get(pos);
			tempClass.init();
			tempClass.createUiMap();
		}
	}
    
//    class UpdateDynamicInfoTask extends TimerTask {
//	    public void run() {
//	    	update();
//	    }
//    }
    
    Runnable diuRunner = new Runnable()
    {
		public void run() {
              update();
              diuHandler.postDelayed(diuRunner, uiUpdateRate);
         }
    };
}