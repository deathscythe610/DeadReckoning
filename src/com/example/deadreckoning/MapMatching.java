/*
 * THIS IS THE MAP FIXING CLASS WHICH WILL RUN EVERYTIME A DRESTIMATE IS GENERATED
 * USING THE DATABASE INFORMATION TAKEN FROM POSTGRESQL DATABASE
 * ALGORITHM: 
 * 1. INPUT new DREsitmation with step distance
 * 2. Compare DREstimate distance to Trajectories with step distance, bigger distance is left out
 * 3. Check if the previous estimation is on trajectory, if yes, compare bearing of the step with azimuth of trajectories:
 * -If StrictFix = true --> left out if difference>30 degree
 * -If StrictFix = false --> left out if difference>60 degree 
 * 4. Find point on remaining trajectories with distance to previous estimation = step distance 
 * 5. If more than 1 points are found, use transmission probability to select best match
 */
package com.example.deadreckoning;

import java.util.ArrayList;

import android.location.Location;
import android.util.Log;



public class MapMatching {
	public static ArrayList<Trajectory> trajectoriesList = new ArrayList<Trajectory>();
	public static ArrayList<Trajectory> relevantTrajectoriesList = new ArrayList<Trajectory>();
	private static String DEBUG = null;
	public static boolean fixStatus = false;
	static Location previousMatch = new Location("");
	static double lastBearing = 0, newBearing = 0, outlierBearing = 0, bearingDifference = 0;
	static boolean outlierDetected = false;
	static Boolean initPoint = true;

	//Mean and standard deviation in meters
	static double mu = 0.3;
	static double sigma = 1;
	static double range = 0.5;
	static String debugbearing = "Bearing";
	
	
	public static Location STMatching(Location DRestimation, double orientation, long timestamp, double stepdistance){
		//Clear relevant node list to load new node list
		Log.d("Map Fixing", "Map Fixing Process Started");
		Location Bestmatch = new Location("BestMatch");
		Boolean mapFixed = false;
		
		//Clear the relevant node and trajectory list for new input 
		MapMatching.relevantTrajectoriesList.clear();
		
		for(int i=0; i< trajectoriesList.size(); i++){
			int forward;
			Trajectory temp = trajectoriesList.get(i);
			//Check orientation condition
			if((temp.getDistancetoPoint(DRestimation)>stepdistance) || (temp.getDistancetoPoint(DRestimation)>DRestimation.distanceTo(temp.getstartPoint())) || (temp.getDistancetoPoint(DRestimation)>DRestimation.distanceTo(temp.getendPoint()))) {
				forward = 0;
			}
			else {
				float bearing = previousMatch.bearingTo(DRestimation);
				forward = Misc.OrienatationTrack(temp.getAzimuth(), bearing, temp.StrictFix);
			}
			if (forward != 0){
				temp.setForward(forward);
				MapMatching.relevantTrajectoriesList.add(temp);
			}
		}
		Trajectory bestPath = null;
		if (MapMatching.relevantTrajectoriesList.size() == 0){		//No relevant trajectory available 
			return DRestimation;
		}
		else if (MapMatching.relevantTrajectoriesList.size() > 1){
			//Choose the most possible trajectory
			double minDist = 100;
			for (Trajectory temp: MapMatching.relevantTrajectoriesList){
				double tempDist = Misc.distancefromPointtoTrajectory(previousMatch, temp.getstartPoint(), temp.getendPoint());
				if (tempDist < minDist){
					bestPath = temp;
					minDist = tempDist;
				}
			}
		}
		else{
			bestPath = MapMatching.relevantTrajectoriesList.get(0);
		}
			
		Bestmatch =  Misc.findLocationOnTrack(bestPath, previousMatch, stepdistance, bestPath.getForward());
		
		Misc.toast("Map Fixed");
		mapFixed = true;
		previousMatch = Bestmatch;
		return Bestmatch;
	}
}
