package com.example.deadreckoning;

import android.location.Location;
import android.util.Log;

public class Trajectory {
	String sector, description;

	double startLat, startLon, endLat, endLon, azimuth;
	Location startPoint, endPoint, DRestimation;
	Boolean StrictFix;
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
	}
	
	//Update additional information for relevant node list and previous node list
	public void updateTrajectoryInfo(Location DRestimation, long timestamp){
		this.DRestimation = DRestimation;
		this.timestamp = timestamp;
		this.distancetoDRestimate = Misc.distancefromPointtoTrajectory(DRestimation, startPoint, endPoint);
	}
	

	public Location getstartPoint(){
		return this.startPoint;
	}
	
	public Double getAzimuth(){
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
	
	public Location getDRestimation(){
		return this.DRestimation;
	}
	
	public long getTimestamp(){
		return this.timestamp;
	}
	
	public double getDistancetoPoint(Location Point){
		return Misc.distancefromPointtoTrajectory(Point, startPoint, endPoint);
	}
}
