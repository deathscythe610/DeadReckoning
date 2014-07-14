/*
 * THIS CLASS CREATE A WIFIAP TYPE WHICH INCLUDES: 
 * 1. BSSID 
 * 2. LATTITUDE OF WIFIAP 
 * 3. LONGITUDE OF WIFIAP 
 */
package com.example.deadreckoning;

public class WiFiAP {
	public String bssid;
	public float Lat;
	public float Lon;
	
	public WiFiAP(String bssid, float Lat, float Lon) {
		this.bssid=bssid;
		this.Lat=Lat;
		this.Lon=Lon;
	}
}
