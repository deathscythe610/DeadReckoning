/*
 * THIS CLASS CREATE A WIFIAP TYPE WHICH INCLUDES: 
 * 1. BSSID 
 * 2. LATTITUDE OF WIFIAP 
 * 3. LONGITUDE OF WIFIAP 
 */
package com.example.deadreckoning;

public class WiFiAP {
	public String bssid;
	public double Lat;
	public double Lon;
	
	public WiFiAP(String bssid, double Lat, double Lon) {
		this.bssid=bssid;
		this.Lat=Lat;
		this.Lon=Lon;
	}
}
