package com.example.deadreckoning;


public class MapPoint {
	public float Lat;
	public float Lon;
	public String label;
	public int id;
	
	public MapPoint(float lat, float lon, String label, int id) {
		this.Lat=lat;
		this.Lon=lon;
		this.label=label;
		this.id=id;
	}
	
	public MapPoint(float lat, float lon, String label) {
		this.Lat=lat;
		this.Lon=lon;
		this.label=label;
	}
	
	public float getLat() {
		return this.Lat;
	}
	
	public float getLon() {
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
