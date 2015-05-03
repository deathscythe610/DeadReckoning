/*
 * THIS CLASS IS USED TO HOLD UTILITY FUNCTION 
 * ALL FUNCITON ARE IN STATIC STATE
 */
package com.example.deadreckoning;

import java.util.Date;

import android.location.Location;
import android.widget.Toast;
import android.util.Log;
public class Misc {
	private static double EarthRadius = 6371000;
	private long lastTime = 0;
	
	/*
	 * Round the number d (double) with c number of decimal point 
	 * Precondition: none
	 * Postcondition: function return new number which is the formated version of d
	 */
	public static double roundToDecimals(double d, int c) {
		int temp=(int)((d*Math.pow(10,c)));
		return (((double)temp)/Math.pow(10,c));
	}
	
	/*
	 * find the distance between 2 point manually
	 * Precondition: the location in l1 and l2 must be valid
	 * Postcondition: return the distance between l1 and l2
	 */
	public static double distanceTo(Location l1, Location l2){
		double lat1=l1.getLatitude();
	    double lon1=l1.getLongitude();
	    double lat2=l2.getLatitude();
	    double lon2=l2.getLongitude();
	    double R = 6371; // km
	    double dLat = (lat2-lat1)*Math.PI/180;
	    double dLon = (lon2-lon1)*Math.PI/180;
	    lat1 = lat1*Math.PI/180;
	    lat2 = lat2*Math.PI/180;

	    double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
	            Math.sin(dLon/2) * Math.sin(dLon/2) * Math.cos(lat1) * Math.cos(lat2);
	    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
	    double d = R * c * 1000;
	    return d;
	}
	
	/*
	 * Round the number d (float) with c number of decimal point 
	 * Precondition: none
	 * Postcondition: function return new number which is the formated version of d
	 */
	public static float roundToDecimals(float d, int c) {
		int temp=(int)((d*Math.pow(10,c)));
		return (float) (temp/Math.pow(10,c));
	}
	
	/*
	 * get current time
	 * Precondition: none
	 * Postcondition: function return current time in format yyyymmddhhmm
	 */
	public static long getTime() {
		Date date = new Date();
		return date.getTime();
	}
	
	/*
	 * get the nearest distance from a point to the trajectory
	 * Precondition: none
	 * Postcondition: return the nearest distance from the point to the trajectory
	 */
	public static double distancefromPointtoTrajectory(Location DRestimate, Location startPoint, Location endPoint){
		Float a = DRestimate.distanceTo(startPoint);
		Float b = DRestimate.distanceTo(endPoint);
		Float c = startPoint.distanceTo(endPoint);
		//Use Heron formula of area triangle to find height
		Float s = (a+b+c)/2;
		Double area = Math.sqrt(s*(s-a)*(s-b)*(s-c));
		Double distance = 2*area/c;
		return distance;
	}
	
	/*
	 * Find coordinate of new point with known start point, bearing (trueNorth) and distance from start point
	 * Precondition: distance must in m, bearing must in radiant
	 * Postcondition: return the Location of the new point
	 */
	public static Location findPoint(Location StartPoint, double bearingInRadian, double distance){
		Location tempLoc = new Location("");
		
		double orgLatInRad = Math.toRadians(StartPoint.getLatitude());
		double orgLonInRad = Math.toRadians(StartPoint.getLongitude());
		double newLatInRad = Math.asin(Math.sin(orgLatInRad)*Math.cos(distance/EarthRadius) +
									Math.cos(orgLatInRad)*Math.sin(distance/EarthRadius)*Math.cos(bearingInRadian));
		double newLonInRad = orgLonInRad + Math.atan2(Math.sin(bearingInRadian)*Math.sin(distance/EarthRadius)*Math.cos(orgLatInRad), 
													Math.cos(distance/EarthRadius)-Math.sin(orgLatInRad)*Math.sin(newLatInRad));
		if ((!Double.isNaN(newLatInRad)) && (!Double.isNaN(newLonInRad))){
			tempLoc.setLatitude(Math.toDegrees(newLatInRad));
			tempLoc.setLongitude(Math.toDegrees(newLonInRad));
		}
		else 
			Log.e("ERROR", "Error Calculating Point");
		return tempLoc;
	}
	
	/*
	 * Match the location of a point in the map onto trajectory, knowing the previous location on track as and the step distance
	 * Precondition: used in map matching 
	 * Postcondition: the new location on the track is returned
	 */
	public static Location findLocationOnTrack(Trajectory track, Location DREstimate, double StrideLength, int Foward){
		Location tempLoc = new Location("");
		double height = track.getDistancetoPoint(DREstimate);
		double a = DREstimate.distanceTo(track.getstartPoint());
		double b = DREstimate.distanceTo(track.getendPoint());
		if (a<StrideLength){
			return track.getstartPoint();
		}
		else if (b<StrideLength){
			return track.getendPoint();
		}
		else{
			double abased = Math.sqrt(a*a - height*height);
			double x = Math.sqrt(StrideLength*StrideLength-height*height);
			double distancetoLocation;
			if (Foward==1)
				distancetoLocation = abased + x;
			else 
				distancetoLocation = abased - x;
			tempLoc = Misc.findPoint(track.getstartPoint(), track.getAzimuthinRadian(), distancetoLocation);
			return tempLoc;
		}
	}
	
	/*
	 * Get time different from current time and the previous getTimeDif() function
	 * Precondition: None
	 * Postcondition: Return the time different in ms between current and previous getTimeDif()
	 */
	public String getTimeDif(){
		Date date = new Date();
		long currTime=date.getTime();
		if(this.lastTime>0) {
			long diff = currTime - this.lastTime;
			return String.valueOf(diff);
		} else {
			this.lastTime=currTime;
			return "0";
		}
	}
	
	/*
	 * Check difference between step direction and trajectory direction
	 * The threshold for the angle difference can be changed in the MainActivity
	 * Precondition: Azimuth in degree, used in map matching
	 * Postcondition: return 0 if angle difference larger than threshold, 1 if the moving forward, 2 if moving backward
	 */
	public static int OrienatationTrack(double TrajectoryAzimuth, float StepAzimuth, boolean StricFix){
		int forward;
		
		if (StepAzimuth<0) StepAzimuth = StepAzimuth +360;
		
		if((StricFix) && (StepAzimuth<180))
		{
			if (Math.abs(TrajectoryAzimuth-StepAzimuth) > MainActivity.strictFixAngle)
				forward = 0;
			else 
				forward = 1;
		}
		else if((StricFix) && (StepAzimuth>180))
		{
			if (Math.abs(TrajectoryAzimuth-StepAzimuth+180) > MainActivity.strictFixAngle)
				forward = 0;
			else 
				forward = 2; 
		}
		else if ((!StricFix) && (StepAzimuth<180))
		{
			if (Math.abs(TrajectoryAzimuth-StepAzimuth) > MainActivity.looseFixAngle)
				forward = 0;
			else 
				forward = 1;
		}
		else 
		{
			if (Math.abs(TrajectoryAzimuth-StepAzimuth+180) > MainActivity.looseFixAngle)
				forward = 0;
			else 
				forward = 2;
		}
		return forward;
	}
	
	/*
	 * Find the location of a point on the track knowing start point, end point and distance from start
	 * Precondition: none 
	 * Postcondition: return new location on the track 
	 */
	public static Location findPointOnTrajectory(Location startPoint, Location endPoint, double distanceFromStart){
		Location tempLoc = new Location("");
		float bearing = startPoint.bearingTo(endPoint);
		tempLoc = findPoint(startPoint, Math.toRadians(bearing), distanceFromStart);
		return tempLoc;
	}
	
	/*
	 * Inner function of public static void toast(int text)
	 */
	public static void toast(final String text) {
		MainActivity.getInstance().runOnUiThread(new Runnable() {
		    public void run() {
		    	Toast.makeText(MainActivity.getInstance(), text,Toast.LENGTH_SHORT).show();
		    }
		});
	}
	
	/*
	 * Print a message to the device screen
	 * precondition: none
	 * postcondition: message is printed on the screen
	 */
	public static void toast(int text) {
		Misc.toast(MainActivity.getInstance().getString(text));
	}
}