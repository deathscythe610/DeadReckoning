package com.example.deadreckoning;

import android.location.Location;



public class CandidateNode {

	String sector, description;

	double nodeLatitude, nodeLongitude;
	Location nodeLocation, DRestimation;
	long timestamp;
	boolean startCandidate = false, endCandidate = false, connected = false, bestMatch = false;
	
	float distancetoDRestimate;
	
	double observationProbability;
	
	Double transmissionProbabilities = 0.0;
	//The spatial analysis function results (in regard to one or more previously obtained candidate nodes) are saved here
	Double spatialAnalysisFunctionResults = 0.0;
	
	
	//Input basic information when fetch from database which include lat, lon, sector and description
	public CandidateNode(double latitude, double longitude, String sector, String description){

		Location locationOfThePoint = new Location("");
		locationOfThePoint.setLatitude(latitude);
		locationOfThePoint.setLongitude(longitude);

		this.nodeLocation = locationOfThePoint;
		this.nodeLatitude = latitude;
		this.nodeLongitude = longitude;
		this.sector = sector;
		this.description = description;
	}
	
	//Update additional information for revelent node list and previous node list
	public void updateNodeInfo(Location DRestimation, long timestamp){
		this.DRestimation = DRestimation;
		this.timestamp = timestamp;
		this.distancetoDRestimate = this.nodeLocation.distanceTo(DRestimation);
	}
	public boolean equals(CandidateNode NodeToCompare){
		if(this.nodeLatitude == NodeToCompare.getLatitude())
		{
			if(this.nodeLongitude == NodeToCompare.getLongitude())
			{
				if (this.DRestimation == NodeToCompare.getDRestimation())
					return true;
			}
		}
		return false;
	}

	public double getLatitude(){
		return this.nodeLatitude;
	}

	public double getLongitude(){
		return this.nodeLongitude;
	}
	
	public double getDistanceToLocation(Location DRestimate){
		double distanceTorespondingDRFix = DRestimate.distanceTo(this.nodeLocation);
		return distanceTorespondingDRFix;
	}
	
	
	public float getDistanceToDR(){
		return this.distancetoDRestimate;
	}
	
	public void setStartOrEndNode(String input){
		if(input.equals("start"))
			startCandidate = true;
		else endCandidate = true;
	}

	
	public double getObservationProbability(){
		return this.observationProbability;
	}
	
	public void setLocation(Location myLocation){
		this.nodeLocation = myLocation;
	}
	
	public Location getLocation(){
		return this.nodeLocation;
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
	
	public void setObservationProbability(double probability){
		this.observationProbability = probability;
	}
	
	public void setTransmissionProbability(CandidateNode pastCandidate, Double transmissionProbability){
		this.transmissionProbabilities = transmissionProbability;
		this.spatialAnalysisFunctionResults = transmissionProbability*observationProbability;
	}
}
