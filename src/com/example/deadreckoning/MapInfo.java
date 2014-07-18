package com.example.deadreckoning;

import android.graphics.PointF;
import android.location.Location;
import android.os.SystemClock;
import android.widget.Toast;


public class MapInfo extends Info{
	 /**
     * Note that this may be null if the Google Play services APK is not available.
     */
	public static Location mapFixLocation = new Location("dummyprovider");
    private static final String TAG = "TM_MapInfo"; 
    private final double EarthRadius = 6371000;
    private static final int wifiFixRadius = 6;
    private int steps = 0;
    private long steptime = 0;
    protected boolean readyupdate = false;
    //define public variables for map Fragment to access
    public PointF mapPoint = new PointF(0,0);
    public float orientation = 0;
    public float distance = 0;
    public MapInfo(){
    	super();
    }
    @Override
    protected void init() {
    	
    }
    
    
    @Override
	void update() {
    	//update only when map has been set up and step number change
    	if ((MainActivity.getInstance().mapFragment.mMap!=null) && (this.readyupdate==false)){
    		this.orientation = MainActivity.getInstance().sensorInfo.orientationFusion.getFusedZOrientation()+ MainActivity.getInstance().mapFragment.curMap.getRotationRadians();
    		this.distance = MainActivity.getInstance().deadReckoning.getDistance();
    		if(this.steps<MainActivity.getInstance().deadReckoning.getSteps()){
				steps = MainActivity.getInstance().deadReckoning.getSteps();
				steptime = MainActivity.getInstance().deadReckoning.getStepTime();
				updateCoodinate(distance, orientation);
				//MapFix called if map fix option is enabled
				if (MainActivity.mapLocationFixing){
					this.mapPoint = MapFix(this.mapPoint, orientation,this.steptime);
				}
    		this.readyupdate = true;
    		}
    	}
	}
    
	public void updateCoodinate(float distance, float orientation){
		double newLat,newLon;
		if ((mapPoint.x==0) && (mapPoint.y==0)){
			newLat = MainActivity.getInstance().mapFragment.curMap.getStartLat();
			newLon =  MainActivity.getInstance().mapFragment.curMap.getStartLon();
		}
		else
		{
			double orgLat = Math.toRadians(mapPoint.x);
			double orgLon = Math.toRadians(mapPoint.y);
			newLat = Math.asin(Math.sin(orgLat)*Math.cos(distance/this.EarthRadius) +
										Math.cos(orgLat)*Math.sin(distance/this.EarthRadius)*Math.cos(orientation));
			newLon = orgLon + Math.atan2(Math.sin(orientation)*Math.sin(distance/this.EarthRadius)*Math.cos(orgLat), 
														Math.cos(distance/this.EarthRadius)-Math.sin(orgLat)*Math.sin(newLat));
		}
		if ((!Double.isNaN(newLat)) && (!Double.isNaN(newLon))){
			mapPoint.x = (float) Math.toDegrees(newLat);
			mapPoint.y = (float) Math.toDegrees(newLon);
		}
	}

	
	public Boolean wifiLocationFix(String bssid) {
		if(MainActivity.getInstance().mapFragment.curMap.hasWifiAP(bssid)) {
			WiFiAP temp = MainActivity.getInstance().mapFragment.curMap.getWifiAP(bssid);
			float orgLat = this.getLat();
			float orgLon = this.getLon();
			float dLat = orgLat - temp.Lat;
			float dLon = orgLon - temp.Lon;
			double Bx = Math.cos(temp.Lat) * Math.cos(dLon);
	        double By = Math.cos(temp.Lat) * Math.sin(dLon);
	        double dist = Math.sin(orgLat) * Math.sin(temp.Lat) + Math.cos(orgLat) * Math.cos(temp.Lat) * Math.cos(dLon);
			if(dist>wifiFixRadius) { //fix location
				double lat3 = Math.atan2(Math.sin(orgLat)+Math.sin(temp.Lat),Math.sqrt( (Math.cos(orgLat)+Bx)*(Math.cos(orgLat)+Bx) + By*By) );
		        double lon3 = orgLon + Math.atan2(By, Math.cos(orgLat) + Bx);
				MainActivity.getInstance().deadReckoning.reset();
				MainActivity.getInstance().mapFragment.curMap.setPosition((float) lat3, (float) lon3);
				Misc.toast("wifi location fixed");
				DataLogManager.addLine("wififix", orgLat+", "+orgLon+", " + MainActivity.getInstance().mapFragment.curMap.getStartLat()+", "+MainActivity.getInstance().mapFragment.curMap.getStartLon());
				return true;
			}
		}
		return false;
	}
	
	
	public PointF MapFix(PointF DRestimate, float brearing, long timestamp){
		Toast.makeText(MainActivity.getInstance(), "Doing Mapmatching", Toast.LENGTH_SHORT).show();
		Location location = setLocation(DRestimate);
		FetchSQL.setDRFixData(location);
		//Only fix map point if node list is loaded
		if (MapFixing.mapNodesList.size()>0){
			location = MapFixing.STMatching(location,brearing,timestamp);
		}
		DRestimate.x = (float) location.getLatitude();
		DRestimate.y = (float) location.getLongitude();
		return DRestimate;
	}
	
	
	public Location setLocation(PointF mark){
		Location location = new Location("dummyprovider");
		location.setLatitude(mark.x);
		location.setLongitude(mark.y);
		location.setTime(SystemClock.uptimeMillis());
		return location;
	}
	
	private float getLat(){
		return this.mapPoint.x;
	}
	
	private float getLon(){
		return this.mapPoint.y;
	}
}

