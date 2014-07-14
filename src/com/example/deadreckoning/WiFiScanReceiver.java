/*
 * THIS CLASS INCLUDES METHODS WHICH:
 * 1.SCAN THE AREA FOR AVAILABLE BSSID 
 * 2.CHOOSE BSSIDs WHICH ARE ABOVE THE SIGNAL LEVEL THRESHOLD 
 * 3.CALL MAPINFO CLASS TO FIX THE LOCATION USING BSSID SENT 
 */
package com.example.deadreckoning;

import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.util.Log;

public class WiFiScanReceiver extends BroadcastReceiver {
  private static final String TAG = "TM_WiFiScanReceiver";
  private static final int lockLevelTreshold = -40;
  private List<ScanResult> lastResults;

  public WiFiScanReceiver() {
	  MainActivity.getInstance().wifiManager.startScan();
  }
  

  @Override
  public void onReceive(Context c, Intent intent) {
	Log.d(TAG,"onReceive()");
    this.lastResults = MainActivity.getInstance().wifiManager.getScanResults();
    this.filterBssid();
    this.getLocationLock();
    MainActivity.getInstance().wifiManager.startScan();
  }
  
  /**
   * iterate through this.lastResults
   * cut off last octet from each bssid
   */
  private void filterBssid() {
	  for (ScanResult result : this.lastResults) {
			result.BSSID = result.BSSID.substring(0,14);
	  }
  }
  
  /**
   * get ScanResult by providing BSSID
   * @param bssid
   * @return ScanResult
   */
  public ScanResult getScanResultByBssid(String bssid) {
	for (ScanResult result : this.lastResults) {
		if(result.BSSID.equals(bssid)) {
			return result;
		}
	}
	return null;
  }
  
  /**
   * find AP with best position lock
   * AP must have RSS higher than lockLevelThreshold 
   * @return ScanResult with best RSS
   */
  	public ScanResult getBestLocationLock() {
		ScanResult bestSignal = null;
	    for (ScanResult result : this.lastResults) {
	      if (bestSignal == null || WifiManager.compareSignalLevel(bestSignal.level, result.level) < 0)
	        bestSignal = result;
	    }
	    if (bestSignal.level>=lockLevelTreshold)
	    	return bestSignal;
	    else
	    	return null;
  }
  	
  	/**
  	 * call MapInfo::wifiLocationFix for each ScanResult with RSS higher than lockLevelThreshold
  	 */
  	public void getLocationLock() {
	    for (ScanResult result : this.lastResults) {
	      if (result.level>=lockLevelTreshold) {
//	    	  Log.d(TAG,"got lock on: "+result.BSSID+"@"+result.level+"db");
	    	  if(MainActivity.getInstance().mapInfo.wifiLocationFix(result.BSSID)) {
	    		  return;
	    	  }
	      }
	    }
  }

}
