package com.example.deadreckoning;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.graphics.PointF;
import android.location.Location;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

public class Map {
	private static final String TAG = "TM_Map";
	public String name;
	public int map;
	public int width;
	public int height;
	private HashMap<Integer,MapPoint> locations = new HashMap<Integer,MapPoint>();
	private HashMap<String,WiFiAP> wifiAPs = new HashMap<String,WiFiAP>();
	private MapPoint currentMapPoint;
	private int currentLocation=-1;
	private int rotation=0; //in degrees
	private int orientationOffset=0; //in degrees
	public int invertX=1;
	public int invertY=1;
	private double Lat=0;
	private double Lon=0;
	
	public Map() {
	}
	
	public Map(String name, int map,int w, int h, int rot, int orOff) {
		this.sharedConstructor(name, map, w, h, rot, orOff, 1, 1);
	}
	
	public Map(String name, int map,int w, int h, int rot, int orOff, int iX, int iY) {
		this.sharedConstructor(name, map, w, h, rot, orOff, iX, iY);
	}
	
	private void sharedConstructor(String name, int map,int w, int h, int rot, int orOff, int iX, int iY) {
		this.name=name;
		this.map=map;
		this.width=w;
		this.height=h;
		this.rotation=rot;
		this.setOrientationOffset(orOff);
		this.invertX=iX;
		this.invertY=iY;
	}
	
	/**
	 * set orientation offset (degrees!)
	 * @param off orientation offset (degrees!)
	 */
	public void setOrientationOffset(int off) {
		Display display = ((WindowManager) MainActivity.getInstance().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
		this.orientationOffset=off+display.getRotation()*90;
	}
	
	/**
	 * returns orientation offset (degrees)
	 * @return
	 */
	public int getOrientationOffsetDegrees() {
		return this.orientationOffset;
	}
	
	/**
	 * 
	 * @return
	 */
	public double getOrientationOffsetRadians() {
		return this.orientationOffset*3.14f/180;
	}
	
	/**
	 * get rotation in radians 
	 * @return rotation in radians
	 */
	public double getRotationRadians() {
		return this.rotation*3.14f/180;
	}
	
	/**
	 * get rotation in degrees
	 * @return rotation in degrees
	 */
	public double getRotationDegrees() {
		return this.rotation;
	}
	
	/**
	 * set rotation (degrees!)
	 * @param rot rotation in degrees
	 */
	public void setRotation(int rot) {
		this.rotation=rot;
	}
	
	/**
	 * adds a new map starting point
	 * @param lat coordinate
	 * @param lon coordinate
	 * @param label label/name
	 * @return id of starting point
	 */
	public int addMapPoint(double lat, double lon, String label) {
		int id = this.locations.size();
		this.locations.put(id ,new MapPoint(lat, lon, label, id));
		return id;
	}
	
	/**
	 * set starting position based on start point id
	 * @param id start point id
	 */
	public void setPosition(int id) {
		if(this.locations.containsKey(id)) {
			this.currentLocation = id;
			this.currentMapPoint = this.locations.get(this.currentLocation);
			this.Lat=this.currentMapPoint.Lat;
			this.Lon=this.currentMapPoint.Lon;
		}
	}
	
	/**
	 * changes current start point based on label name given
	 * @param find label of start point to set
	 * @return Boolean success?
	 */
	public Boolean setPosition(String find) {
		for (MapPoint tempMP : this.locations.values()) {
	    	if(find.equals(tempMP.label)) {
	    		this.setPosition(tempMP.id);
	    		return true;
	    	}
	    }
		return false;
	}
	
	/**
	 * set starting position based on unscaled map coordinates
	 * @param lat coordinate
	 * @param lon coordinate
	 */
	public void setPosition(double lat, double lon) {
		this.currentLocation=-1;
		this.currentMapPoint=null;
		this.Lat=lat;
		this.Lon=lon;
	}
	
	/**
	 * returns the start point x coordinate
	 * @return 
	 */
	public double getStartLat() {
		return this.Lat;
	}
	
	/**
	 * returns the start point y coordinate
	 * @return
	 */
	public double getStartLon() {
		return this.Lon;
	}
	
	/**
	 * returns the start point as a PointF object
	 * @return
	 */
	public Location getStartPoint() {
		Location newLoc = new Location("dummyprovider");
		newLoc.setLatitude(this.getStartLat());
		newLoc.setLongitude(this.getStartLon());
		return newLoc;
	}
	
	/**
	 * returns list of start points associated with this map
	 * @return list of start points
	 */
	public List<String> getMapPointList() {
		List<String> list = new ArrayList<String>();
	    for (MapPoint value : this.locations.values()) {
	    	list.add(value.label);
	    }
	    return list;
	}
	
	/**
	 * basic bssid verification
	 * checks whether last octet was discarded
	 * @param bssid bssid to verify
	 * @return verification successful
	 */
	public Boolean verifyBssid(String bssid) {
		Boolean ret = bssid.length()==14;
		if(!ret) {
			Log.w(TAG,"verifyBssid() incorrect bssid length! ("+bssid+")");
		}
		return ret;
	}

	/**
	 * add wifi AP
	 * used to fix location based on wifi signal RSS
	 * @param bssid the bssid of the wifi AP without the last octet
	 * @param lat position on unscaled map
	 * @param lon position on unscaled map
	 */
	public void addWifiAP(String bssid, double Lat, double Lon) {
		if(this.verifyBssid(bssid))
			this.wifiAPs.put(bssid, new WiFiAP(bssid, Lat, Lon));
	}
	
	/**
	 * checks whether this map contains a specified bssid
	 * @param bssid
	 * @return
	 */
	public Boolean hasWifiAP(String bssid) {
		return this.wifiAPs.containsKey(bssid);
	}
	
	/**
	 * returns WifiAP with specified bssid
	 * @param bssid the bssid of the wifi AP without the last octet
	 * @return WifiAP
	 */
	public WiFiAP getWifiAP(String bssid) {
		if(this.hasWifiAP(bssid))
			return this.wifiAPs.get(bssid);
		else
			return null;
	}
}