package com.example.deadreckoning;

import android.location.Location;

public class Trajectory {
	String sector, description;

	double startLat, startLon, endLat, endLon, azimuth;
	Location startPoint = new Location("");
	Location endPoint = new Location("");
	Boolean StrictFix;
	int forward;
	long timestamp;
	
	Double distancetoDRestimate;
	
	//Input basic information when fetch from database which include lat, lon, sector and description
	public Trajectory(double startLat, double startLon, double endLat, double endLon, String sector, String description, Boolean StrictFix, Double Azimuth){

		this.startPoint.setLatitude(startLat);
		this.startPoint.setLongitude(startLon);
		this.endPoint.setLatitude(endLat);
		this.endPoint.setLongitude(endLon);
		this.startLat = startLat;
		this.startLon = startLon;
		this.endLat = endLat;
		this.endLon = endLon;
		this.sector = sector;
		this.description = description;
		this.StrictFix = StrictFix;
		this.azimuth = Azimuth;
		this.forward = 0;
	}
	
	public void setForward(int forward){
		this.forward = forward;
	}
	public Location getstartPoint(){
		return this.startPoint;
	}
	
	public double getAzimuth(){
		return this.azimuth;
	}
	
	public Double getAzimuthinRadian(){
		return this.azimuth*3.14/180;
	}

	public Location getendPoint(){
		return this.endPoint;
	}
	
	public Double getDistanceToDR(){
		return this.distancetoDRestimate;
	}
	
	
	public String getSectorName(){
		return this.sector;
	}
	
	public String getDescription(){
		return this.description;
	}
	
	
	public long getTimestamp(){
		return this.timestamp;
	}
	
	public int getForward(){;
		return this.forward;
	}	
	
	public double getDistancetoPoint(Location Point){
		return Misc.distancefromPointtoTrajectory(Point, startPoint, endPoint);
	}
	
}
