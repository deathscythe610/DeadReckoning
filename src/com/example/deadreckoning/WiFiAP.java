/*
 * THIS CLASS CREATE A WIFIAP TYPE WHICH INCLUDES: 
 * 1. BSSID 
 * 2. LATTITUDE OF WIFIAP 
 * 3. LONGITUDE OF WIFIAP 
 */
package com.example.deadreckoning;

import android.location.Location;

public class WiFiAP {
	private String bssid;
	private String ssid;
	private double Lat;
	private double Lon;
	
	public WiFiAP(String bssid, String ssid, double Lat, double Lon) {
		this.bssid=bssid;
		this.Lat=Lat;
		this.Lon=Lon;
		this.ssid = ssid;
	}
	
	public double getLat(){
		return this.Lat;
	}
	
	public double getLon(){
		return this.Lon;
	}
	
	public String getBSSID(){
		return bssid;
	}
	
	public String getSSID(){
		return ssid;
	}
	
	public Location getAPLocation(){
		Location temp = new Location("");
		temp.setLatitude(this.Lat);
		temp.setLongitude(this.Lon);
		return temp;
	}
};
