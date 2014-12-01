package com.example.deadreckoning;

import java.util.Date;
import android.location.Location;
import android.widget.Toast;
import android.util.Log;
public class Misc {
	private static double EarthRadius = 6371000;
	
	public static double roundToDecimals(double d, int c) {
		int temp=(int)((d*Math.pow(10,c)));
		return (((double)temp)/Math.pow(10,c));
	}
	
	public static float roundToDecimals(float d, int c) {
		int temp=(int)((d*Math.pow(10,c)));
		return (float) (temp/Math.pow(10,c));
	}
	
	public static long getTime() {
		Date date = new Date();
		return date.getTime();
	}
	
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
	
	
	public static Location findPoint(Location StartPoint, double bearingInRadian, double distance){
		Location tempLoc = new Location("");
		
		double orgLatInRad = Math.toRadians(StartPoint.getLatitude());
		double orgLonInRad = Math.toRadians(StartPoint.getLongitude());
		double newLatInRad = Math.asin(Math.sin(orgLatInRad)*Math.cos(distance/EarthRadius) +
									Math.cos(orgLatInRad)*Math.sin(distance/EarthRadius)*Math.cos(bearingInRadian));
		double newLonInRad = orgLonInRad + Math.atan2(Math.sin(bearingInRadian)*Math.sin(distance/EarthRadius)*Math.cos(orgLatInRad), 
													Math.cos(distance/EarthRadius)-Math.sin(orgLatInRad)*Math.sin(tempLoc.getLatitude()));
		if ((!Double.isNaN(newLatInRad)) && (!Double.isNaN(newLonInRad))){
			tempLoc.setLatitude(Math.toDegrees(newLatInRad));
			tempLoc.setLongitude(Math.toDegrees(newLonInRad));
		}
		else 
			Log.e("ERROR", "Error Calculating Point");
		return tempLoc;
	}
	
	public static Location findLocationOnTrack(Trajectory track, Location DREstimate, double StrideLength, boolean Foward){
		Location tempLoc = new Location("");
		double height = track.getDistancetoPoint(DREstimate);
		double a = DREstimate.distanceTo(track.getstartPoint());
		double abased = Math.sqrt(a*a - height*height);
		double x = Math.sqrt(StrideLength*StrideLength-height*height);
		double distancetoLocation;
		if (Foward)
			distancetoLocation = abased + x;
		else 
			distancetoLocation = abased - x;
		tempLoc = Misc.findPoint(track.getstartPoint(), track.getAzimuthinRadian(), distancetoLocation);
		return tempLoc;
	}
	
	
	public static boolean OrienatationTrack(double TrajectoryAzimuth, float StepAzimuth, boolean StricFix){
		boolean doNotAdd = false;
		
		if (StepAzimuth<0) StepAzimuth = StepAzimuth +360;
		
		if((StricFix) && (StepAzimuth<180))
		{
			if (Math.abs(TrajectoryAzimuth-StepAzimuth)>30)
				doNotAdd = true;
		}
		else if((StricFix) && (StepAzimuth>180))
		{
			if (Math.abs(TrajectoryAzimuth-StepAzimuth+180)>30)
				doNotAdd = true;
		}
		else if ((!StricFix) && (StepAzimuth<180))
		{
			if (Math.abs(TrajectoryAzimuth-StepAzimuth)>30)
				doNotAdd = true;
		}
		else 
		{
			if (Math.abs(TrajectoryAzimuth-StepAzimuth+180)>60)
				doNotAdd = true;
		}
		return doNotAdd;
	}
	
	
	public static void toast(final String text) {
		MainActivity.getInstance().runOnUiThread(new Runnable() {
		    public void run() {
		    	Toast.makeText(MainActivity.getInstance(), text,Toast.LENGTH_SHORT).show();
		    }
		});

//		MainActivity.getInstance().toastRunnable(text).run();		
	}
	
	public static void toast(int text) {
		Misc.toast(MainActivity.getInstance().getString(text));
	}
}