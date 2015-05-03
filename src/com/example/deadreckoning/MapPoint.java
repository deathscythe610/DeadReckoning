package com.example.deadreckoning;


public class MapPoint {
	public double Lat;
	public double Lon;
	public String label;
	public int id;
	
	public MapPoint(double lat, double lon, String label, int id) {
		this.Lat=lat;
		this.Lon=lon;
		this.label=label;
		this.id=id;
	}
	
	public MapPoint(double lat, double lon, String label) {
		this.Lat=lat;
		this.Lon=lon;
		this.label=label;
	}
	
	public double getLat() {
		return this.Lat;
	}
	
	public double getLon() {
		return this.Lon;
	}
	
	public String getLabel() {
		return this.label;
	}
	
	@Override
	public String toString() {
		return this.label+" ("+this.Lat+", "+this.Lon+")";
	}
}
