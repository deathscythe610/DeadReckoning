/*
 * THIS CLASS INCLUDES METHODS WHICH:
 * 1.SCAN THE AREA FOR AVAILABLE BSSID 
 * 2.CHOOSE BSSIDs WHICH ARE ABOVE THE SIGNAL LEVEL THRESHOLD 
 * 3.CALL MAPINFO CLASS TO FIX THE LOCATION USING BSSID SENT 
 */
package com.example.deadreckoning;

import java.util.Iterator;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.util.Log;

public class WiFiScanReceiver extends BroadcastReceiver {
  private static final String TAG = "TM_WiFiScanReceiver";
  private static int aboveLevelThreshold = -35;
  private static int belowLevelThreshold = -52;
  private static final int[] distanceToRSS = {4, 8, 12, 16};
  private List<ScanResult> lastResults;
  private ScanResult bestResult;
  private static final String relevantNetwork = "NUS";
  private long endScanTime; 
  
  
  public WiFiScanReceiver() {
	  MainActivity.getInstance().wifiManager.startScan();
  }
  

  @Override
  public void onReceive(Context c, Intent intent) {
	Log.d(TAG,"onReceive()");
	this.endScanTime = System.currentTimeMillis();
    this.lastResults = MainActivity.getInstance().wifiManager.getScanResults();
    this.processResult();
    this.bestResult = this.getBestLocationLock();
    if (bestResult != null)
    	this.fixLocation();
    MainActivity.getInstance().wifiManager.startScan();
  }
  
  
  /**
   * 	Iterate through this.lastResults to delete all the signal not from NUS, cut off octet from bssid 
   * 	Precondition: The list is not empty, not null
   * 	Postcondition: All network not from NUS will be removed, remaning will have BSSID truncate 
   */
  private void processResult(){
	  for (Iterator<ScanResult> iter = this.lastResults.iterator(); iter.hasNext();){
		 ScanResult current = iter.next();
		 if ( (!current.SSID.equals(relevantNetwork)) || (current.level < belowLevelThreshold) ){
			 iter.remove();
		 }
		 else {
			 current.BSSID = current.BSSID.substring(0,14);
		 }
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
	    if ( (bestSignal != null) && (bestSignal.level>WiFiScanReceiver.aboveLevelThreshold)){
	    	WiFiScanReceiver.aboveLevelThreshold = bestSignal.level;
	    }
	    return bestSignal;
  	}
  	
  	/**
  	 * Use signal level to estimate the distance of phone to the nearest beacon
  	 * Precondition: All the threshold and the distanceToRSS are properly updated
  	 * Postcondition: Return the estimated distance from the phone to the beacon
  	 */
  	 public int getEstimatedDistance(int RSS){
  		 int distance;
  		 if (RSS >= (WiFiScanReceiver.aboveLevelThreshold-distanceToRSS[0])){
  			 distance = 1;
  		 }
  		 else if (RSS >= (WiFiScanReceiver.aboveLevelThreshold-distanceToRSS[1])){
  			 distance = 2;
  		 }
  		 else if ((RSS >= (WiFiScanReceiver.aboveLevelThreshold-distanceToRSS[2]))){
  			 distance = 3;
  		 }
  		else if ((RSS >= (WiFiScanReceiver.aboveLevelThreshold-distanceToRSS[3]))){
 			 distance = 4;
 		 }
  		else {
 			 distance = 5;
 		 }
  		 return distance;
  	 }
  	 
  	
  	/**
  	 * call MapInfo::wifiLocationFix for each ScanResult with RSS higher than lockLevelThreshold
  	 */
  	public void fixLocation() {
			if ( ( MapFragment.getInstance() != null ) && ( bestResult != null ) )
				MapFragment.getInstance().wifiLocationFix(bestResult.BSSID, MapFragment.getInstance().getmapPoint(), this.getEstimatedDistance(bestResult.level), this.endScanTime, bestResult.level);
  	}
  	
  	public long getEndTime(){
  		return this.endScanTime;
  	}
  	
}
