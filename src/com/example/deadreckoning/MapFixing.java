/*
 * THIS IS THE MAP FIXING CLASS WHICH WILL RUN EVERYTIME A DRESTIMATE IS GENERATED
 * USING THE DATABASE INFORMATION TAKEN FROM POSTGRESQL DATABASE
 */
package com.example.deadreckoning;

import java.util.ArrayList;

import android.location.Location;
import android.util.Log;



public class MapFixing {
	
	public static ArrayList<CandidateNode> mapNodesList = new ArrayList<CandidateNode>();
	public static ArrayList<CandidateNode> relevantNodesList = new ArrayList<CandidateNode>();
	private static String DEBUG = null;

	static CandidateNode previousBestNode;
	static double lastBearing = 0, newBearing = 0, outlierBearing = 0, bearingDifference = 0;
	static boolean outlierDetected = false;
	static Boolean initPoint = true;

	//Mean and standard deviation in meters
	static double mu = 0.3;
	static double sigma = 1;
	static double range = 1;
	static String debugbearing = "Bearing";
	
	
	public static Location STMatching(Location DRestimation, float orientation, long timestamp){
		//Clear relevant node list to load new node list
		Log.d("Map Fixing", "Map Fixing Process Started");
		Location Bestmatch = new Location("dummyprovider");
		MapFixing.relevantNodesList.clear();
		for(int i=0; i< mapNodesList.size(); i++){
			boolean doNotAdd = false;
			if(mapNodesList.get(i).getDistanceToLocation(DRestimation)>MapFixing.range){
				doNotAdd = true;
			}
			else{
				for(int j=0; j< mapNodesList.size(); j++){
					if(mapNodesList.get(i).getSectorName().equals(mapNodesList.get(j).getSectorName()) && mapNodesList.get(i).getDistanceToLocation(DRestimation)>mapNodesList.get(j).getDistanceToLocation(DRestimation)){
						doNotAdd = true;
						break;
					}
				}
			}
			if(!doNotAdd){
				mapNodesList.get(i).updateNodeInfo(DRestimation, timestamp);
				relevantNodesList.add(mapNodesList.get(i));
				Log.d(DEBUG, "New close point: Distance: "+ " Latitude "+mapNodesList.get(i).getLatitude() + " Longitude " + 
						mapNodesList.get(i).getLongitude() + " Street " + mapNodesList.get(i).getSectorName() + " Way hash: " + mapNodesList.get(i).getDescription().hashCode()); 
			}
			
		}

		if(relevantNodesList.size()>0){
			MapFixing.assignObservationProbability();
			MapFixing.assignTransmissionProbability();
			for(CandidateNode e:relevantNodesList){
				if (e.bestMatch){
					previousBestNode = e;
					Bestmatch.setLatitude(e.getLatitude());
					Bestmatch.setLongitude(e.getLongitude());
					Bestmatch.setTime(e.timestamp);
					return Bestmatch;
				}
			}
		}
		return DRestimation;
		//OverlayMapViewer.setCandidatePoints(closeNodesList);
	}
	
	
	
	//Likelihood that the raw fix should be mapped to the candidate in question, without considering the neighboring points
	public static void assignObservationProbability(){
		for(CandidateNode e:relevantNodesList){
			double probability = (1/(Math.sqrt(6.28)*sigma)) * Math.exp (-((Math.pow((e.getDistanceToDR()-mu), 2))/(2*Math.pow(sigma,2))));
			e.setObservationProbability(probability);
		}

		CandidateNode observationBestMatch = null, observationSecondBestMatch;

		observationBestMatch = relevantNodesList.get(0);
		observationSecondBestMatch = relevantNodesList.get(0);

		for(CandidateNode e:relevantNodesList){
			if(e.getObservationProbability()>observationBestMatch.getObservationProbability()){
				observationSecondBestMatch = observationBestMatch;
				observationBestMatch = e;
			}
			else if(!e.equals(observationBestMatch) && (observationBestMatch.equals(observationSecondBestMatch) || e.getObservationProbability()>observationSecondBestMatch.getObservationProbability()))
				observationSecondBestMatch = e;
		}

		pointWeighting(observationBestMatch, observationSecondBestMatch);
	}
	
	
	public static void assignTransmissionProbability(){
		double highestSpatialResult = 0;
		CandidateNode highestSpatialNode = null;
		if(previousBestNode!=null){
			//If previous best node match exist, compute the transmission probability based on the the current relevant 
			//nodes and the previous node
			for(CandidateNode e:relevantNodesList){
			//Compute the transmission probability between relevant nodes and previous best match node
			//the new candidates for current observation
				//Distance between the GPS fixes
				double distanceBetweenRawPoints = e.getDRestimation().distanceTo(previousBestNode.getDRestimation());
				Location locationOfThePreviousPoint = new Location("");
				locationOfThePreviousPoint.setLatitude(previousBestNode.getLatitude());
				locationOfThePreviousPoint.setLongitude(previousBestNode.getLongitude());

				Location locationOfTheNextPoint = new Location("");
				locationOfTheNextPoint.setLatitude(e.getLatitude());
				locationOfTheNextPoint.setLongitude(e.getLongitude());

				//Distance and approximate speed between the two candidate nodes
				double distanceBetweenTheCandidateNodes = locationOfThePreviousPoint.distanceTo(locationOfTheNextPoint);
				//Computing transmission probability
				double transmissionProbability = (distanceBetweenRawPoints/distanceBetweenTheCandidateNodes);
				e.setTransmissionProbability(transmissionProbability);
			}
		}
		else{
			//If not set transmission of all relevant nodes = 1 and spatial result = observation probability
			for(CandidateNode e:relevantNodesList){
				e.setTransmissionProbability(1.0);
			}
		}
		//Determine the candidate node with the overall highest spatial/temporal function score	
		for (CandidateNode e:relevantNodesList){
			if (e.spatialAnalysisFunctionResults>highestSpatialResult){
				highestSpatialResult = e.spatialAnalysisFunctionResults;
				highestSpatialNode = e;
				Log.i("DEBUG", "HIGHEST NODE REASSIGN");
				}
		}	
		for (CandidateNode e:relevantNodesList){
			if(e==highestSpatialNode){
				e.bestMatch=true;
				Log.i("DEBUG", "FOUND BEST MATCH");
			}
		}
}
	
	public static void pointWeighting(CandidateNode observationBestMatch, CandidateNode observationSecondBestMatch){
		if(observationBestMatch.getSectorName().equals(observationSecondBestMatch.getSectorName()))
		{
			Location locationBestMatch = observationBestMatch.getLocation();
			Location locationSecondMatch = observationSecondBestMatch.getLocation();

			double distanceBetween = locationSecondMatch.distanceTo(locationBestMatch);

			double ratio = (observationSecondBestMatch.getObservationProbability()/observationBestMatch.getObservationProbability())/2.0;
			double theTrueDistance = distanceBetween*ratio;

			double dist = (theTrueDistance/1000.0)/6371.0;
			double lat1 = Math.toRadians(observationBestMatch.getLatitude());
			double lon1 = Math.toRadians(observationBestMatch.getLongitude());
			double bearing = Math.toRadians(locationBestMatch.bearingTo(locationSecondMatch));

			double lat2 = Math.asin( Math.sin(lat1)*Math.cos(dist) + Math.cos(lat1)*Math.sin(dist)*Math.cos(bearing) );
			double a = Math.atan2(Math.sin(bearing)*Math.sin(dist)*Math.cos(lat1), Math.cos(dist)-Math.sin(lat1)*Math.sin(lat2));
			System.out.println("a = " +  a);
			double lon2 = lon1 + a;

			lon2 = (lon2+ 3*Math.PI) % (2*Math.PI) - Math.PI;

			Log.d(DEBUG, "Latitude = "+Math.toDegrees(lat2)+"\nLongitude = "+Math.toDegrees(lon2));
			double matchedLatitude = Math.toDegrees(lat2);
			double matchedLongitude = Math.toDegrees(lon2);

			CandidateNode geoMatchedCandidate= new CandidateNode(matchedLatitude, matchedLongitude, 
					observationBestMatch.getSectorName(), observationBestMatch.getDescription());
			geoMatchedCandidate.updateNodeInfo(observationBestMatch.getDRestimation(), observationBestMatch.getTimestamp());
			assignObservationProbability(geoMatchedCandidate);
			observationBestMatch = geoMatchedCandidate;
		}
	}
	
	public static void assignObservationProbability(CandidateNode node){
		double probability = (1/(Math.sqrt(6.28)*sigma)) * Math.exp (-((Math.pow((node.getDistanceToDR()-mu), 2))/(2*Math.pow(sigma,2))));
		node.setObservationProbability(probability);
	}	

	public ArrayList<CandidateNode> returnNodesList(String parameter){
		if(parameter.equals("close"))
			return mapNodesList;
		else return relevantNodesList;
	}
}
