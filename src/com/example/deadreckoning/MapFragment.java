package com.example.deadreckoning;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Point;
import android.graphics.PointF;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class MapFragment extends FragmentControl implements OnSeekBarChangeListener{
	
	private static final String TAG = "Map_Fragment";
	private static final int TRANSPARENCY_MAX = 100;
	public static Location mapFixLocation = new Location("dummyprovider");
    private static final LatLng NEWARK = new LatLng(1.292412, 103.775672);
    private final List<BitmapDescriptor> mImages = new ArrayList<BitmapDescriptor>();
    private int steps = 0;
    private long steptime = 0;
    
    private Location mapPoint = new Location("mappoint");
    private Location estimatedDRPoint = new Location("estimatedDRPoint");
    private double orientation = 0;
    private double distance = 0;
    
    
    protected GoogleMap mMap;
    private SupportMapFragment supportfragment;
    private GroundOverlay mGroundOverlay;
    private SeekBar mTransparencyBar;
    private View layout;  
    private int mCurrentEntry = 0;
    private boolean skipFirst;
    private List<Marker> markerList;
    private Marker marker;
    private HashMap<String,Map> mapList = new HashMap<String,Map>();
    private Location defaultStartPoint = new Location("deaultStartPoint");
    private static boolean moveCamera = true;
	public Map curMap;
	public static MapFragment instance;
    private Timer mapTimer;
    private boolean requireupdate = false;
    private boolean MapFixChange = false;
    private boolean WiFiFixChange = false;
    private long wifiFixTime = 0;
    
    
//*****************************************************************************************************************************
//   												MAP INITIALIZATION   
//*****************************************************************************************************************************
	public MapFragment(){
    	super();
    	this.defaultStartPoint.setLatitude(1.292214);
    	this.defaultStartPoint.setLongitude(103.776072);
    	this.loadMaps();
    }
	
	public static MapFragment getInstance() {
		if(MapFragment.instance==null) {
			Log.e(TAG,"Map Fragment is not loaded");
		}
		return MapFragment.instance;
	}
	
	
    public static MapFragment newInstance(int position, String title){
    	MapFragment mapFragment = new MapFragment();
    	Bundle args = new Bundle();
    	args.putInt("current_page", 1);
    	args.putString("page_tile", "Map Information");
    	mapFragment.setArguments(args);
    	return mapFragment;
    }
 
	private void loadMaps() {
		try {
			Resources res = MainActivity.getInstance().getResources();
			XmlResourceParser xpp = res.getXml(R.xml.map);
			xpp.next();
			int eventType = xpp.getEventType();
			String tagName;
			Map tempMap = new Map();
			Boolean defaultMap=false;
			while (eventType != XmlPullParser.END_DOCUMENT) {
				tagName = xpp.getName();
				if(eventType == XmlPullParser.START_TAG) {
					if(tagName.equals("map")) {
						tempMap = new Map();
						tempMap.name = xpp.getAttributeValue(null, "name");
						tempMap.map = xpp.getAttributeResourceValue(null, "src", -1);
						tempMap.width = xpp.getAttributeIntValue(null, "width", 0);
						tempMap.height = xpp.getAttributeIntValue(null, "height", 0);
						tempMap.setRotation(xpp.getAttributeIntValue(null, "rotation", 0));
						tempMap.setOrientationOffset(xpp.getAttributeIntValue(null, "orientationOffset", 0));
						tempMap.invertX = xpp.getAttributeIntValue(null, "invertx", 1);
						tempMap.invertY = xpp.getAttributeIntValue(null, "inverty", 1);
						defaultMap=xpp.getAttributeBooleanValue(null, "default", false);
					} else if (tagName.equals("map_point")) {
						int tempId = tempMap.addMapPoint(xpp.getAttributeFloatValue(null, "Lat", 0.0f),xpp.getAttributeFloatValue(null, "Lon",0.0f),xpp.getAttributeValue(null, "name"));
						if(xpp.getAttributeBooleanValue(null, "default", false)) {
							tempMap.setPosition(tempId);
						}
					} else if (tagName.equals("wifi_ap")) {
						tempMap.addWifiAP(xpp.getAttributeValue(null, "bssid"), xpp.getAttributeValue(null, "label"), xpp.getAttributeFloatValue(null, "Lat", 0.0f),xpp.getAttributeFloatValue(null, "Lon",0.0f));
					}
				} else if (eventType == XmlPullParser.END_TAG) {
					if(tagName.equals("map")) {
						this.mapList.put(tempMap.name, tempMap);
						if (defaultMap==true) {
							this.curMap=tempMap;
						}
					}
				}
			    eventType = xpp.next();
			}
		} catch (IOException ex) {
			Log.e(TAG, ex.toString());
		} catch (XmlPullParserException ex) {
			Log.e(TAG, ex.toString());
		}
	}
    
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		MapFragment.instance=this;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		layout = inflater.inflate(R.layout.map, container, false);
    	
		if (layout==null){
    		Log.d(TAG,"layout null");
			return null;
    	}
    	CheckBox camera = (CheckBox)layout.findViewById(R.id.cameraMove);
    	camera.setChecked(MapFragment.moveCamera);
    	camera.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				MapFragment.moveCamera = isChecked;
			}
		});;
        mTransparencyBar = (SeekBar)layout.findViewById(R.id.transparencySeekBar);
        mTransparencyBar.setMax(TRANSPARENCY_MAX);
        mTransparencyBar.setProgress(0);
        markerList = new ArrayList<Marker>();
		return layout;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		FragmentManager fm = getChildFragmentManager();
		supportfragment = (SupportMapFragment) fm.findFragmentById(R.id.googlemap);
		if (supportfragment == null) {
		    supportfragment = SupportMapFragment.newInstance();
		    fm.beginTransaction().replace(R.id.googlemap, supportfragment).commit();
		    fm.executePendingTransactions();
		}
	}
	
	@Override
	public void onResume() {
		super.onResume();
		this.createUiMap();
		mapTimer = new Timer();
		mapTimer.scheduleAtFixedRate(new FetchMapDataTask(), 0, 3000);
		mapTimer.scheduleAtFixedRate(new updateCalculationTask(), 25, 100);
		mapTimer.scheduleAtFixedRate(new updateUITask(), 50, MainActivity.uiUpdateRate);	
	}
	

	@Override
	public void onPause() {
		if (mapTimer!=null)
			this.mapTimer.cancel();
		super.onPause();
	}

	@Override
	public void onDestroy() {
		this.removeallMarker();
		super.onDestroy();
	}

	private void createUiMap() {
		 if (mMap == null) {
			// Try to obtain the map from the SupportMapFragment.
	        mMap = supportfragment.getMap();
	        // Check if we were successful in obtaining the map.
	        if (mMap != null) {
	            setUpMap();
	            populateMapStartPointsSpinner(layout);
	            setStartPointSpinnerListener(layout);
	            this.setUpMarker(this.defaultStartPoint.getLatitude(), this.defaultStartPoint.getLongitude());
	            FetchSQL.setDRFixData(defaultStartPoint);
	         }
		}
	}
	
    private void setUpMap() {
        	mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(NEWARK, 20));
        	mImages.add(BitmapDescriptorFactory.fromResource(R.drawable.i3f2));
        	mCurrentEntry = 0;
        	LatLngBounds newarkBounds = new LatLngBounds(
        	        new LatLng(1.291926, 103.775114),       // South west corner
        	        new LatLng(1.292833, 103.776310));      // North east corner
        	mGroundOverlay = mMap.addGroundOverlay(new GroundOverlayOptions()
                     .image(mImages.get(mCurrentEntry))
                     .positionFromBounds(newarkBounds));
        	mTransparencyBar.setOnSeekBarChangeListener(this);
        }
        

 // Set up a starting point marker at the beginning of the program and when spinner item is selected 

    private void setUpMarker(double Lat, double Lon){
    	this.mapPoint.setLatitude(Lat);
    	this.mapPoint.setLongitude(Lon);
    	LatLng Position = new LatLng(Lat, Lon);
        marker = mMap.addMarker(new MarkerOptions()
		.position(Position).title("Marker")
		.title("current position")
		.icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_arrow))
		.anchor(0.5f, 0.73f)
		.rotation(0)	
        .flat(true));
        if ((mMap!=null) && (MapFragment.moveCamera=true)){
        mMap.animateCamera(CameraUpdateFactory.newLatLng(marker.getPosition()));
        }
        markerList.add(marker);
    }
    
    public void removeallMarker(){
    	if ((marker!=null) && (markerList!=null)){
    		marker.remove();
    		markerList.clear();
    	}
    }
    
    
	private void populateMapStartPointsSpinner(View layout) {
		Spinner sp = (Spinner)layout.findViewById(R.id.mapStartPointSpinner);
		ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(MainActivity.getInstance(),android.R.layout.simple_spinner_dropdown_item,this.curMap.getMapPointList());
		sp.setAdapter(dataAdapter);
	}
	
	
	private void setStartPointSpinnerListener(View layout) {
		this.skipFirst = true;
		Spinner sp = (Spinner)layout.findViewById(R.id.mapStartPointSpinner);
		sp.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View view, int pos,long id) {
				if (skipFirst==true){
					skipFirst=false;
					return;
				}
				DRFragment.getInstance().reset();
				removeallMarker();
				Boolean success = curMap.setPosition(parent.getItemAtPosition(pos).toString());
				setUpMarker(curMap.getStartLat(), curMap.getStartLon());
				if(success)
					Misc.toast(R.string.mapStartPointResetSuccess);
				else
					Misc.toast(R.string.mapStartPointResetFailed);
			  }
			public void onNothingSelected(AdapterView<?> arg0) {
				Misc.toast(R.string.mapStartPointResetFailed);
			}
		});
	}
	
//*****************************************************************************************************************************
//													MAP UPDATE   
//*****************************************************************************************************************************	
	
	class updateCalculationTask extends TimerTask{
    	//update only when map has been set up and step number change
		@Override
		public void run() {
			//Log.d("DEBUG", "UPDATE CALCULATION");
			if ((MapFragment.getInstance().mMap!=null) && (SensorFragment.getInstance()!=null) && (DRFragment.getInstance()!=null)){
	    		MapFragment.getInstance().orientation = SensorFragment.getInstance().orientationFusion.getFusedZOrientation()+ MapFragment.getInstance().curMap.getRotationRadians();
	    		MapFragment.getInstance().distance = DRFragment.getInstance().getDistance();
	    		if(MapFragment.getInstance().steps < DRFragment.getInstance().getSteps()){
					steps = DRFragment.getInstance().getSteps();
					steptime = DRFragment.getInstance().getStepTime();
					updateCoodinate(distance, orientation);
					//MapFix called if map fix option is enabled
					if (MainActivity.mapLocationMatching){
						Location tempMapFix = MapMatch(MapFragment.getInstance().mapPoint, orientation,MapFragment.getInstance().steptime, MapFragment.getInstance().distance);
						if ((MapFragment.getInstance().mapPoint.getLatitude()!=tempMapFix.getLatitude() || MapFragment.getInstance().mapPoint.getLongitude()!=tempMapFix.getLongitude())){
							MapFragment.getInstance().mapPoint = tempMapFix;
							MapFragment.getInstance().MapFixChange = true;
							MapFragment.getInstance().mapLog("MapMatching");
						}
						else{
							MapFragment.getInstance().mapLog("DR");
						}
					}
					else {
						MapFragment.getInstance().mapLog("DR");
					}
					MapFragment.getInstance().requireupdate=true;
					
	    		}
	    	}
		}
	}
    
	public void updateCoodinate(double distance, double orientation){
		Location tempLoc = new Location("");	
		if ((mapPoint.getLatitude()==0) && (mapPoint.getLongitude()==0)){
			tempLoc.setLatitude(this.curMap.getStartLat());
			tempLoc.setLongitude(this.curMap.getStartLon());
		}
		else
		{
			tempLoc = Misc.findPoint(mapPoint, orientation, distance);
		}
		if ((!Double.isNaN(tempLoc.getLatitude())) && (!Double.isNaN(tempLoc.getLongitude()))){
			mapPoint.setLatitude(tempLoc.getLatitude());
			mapPoint.setLongitude(tempLoc.getLongitude());
			//remember result in EstimatedDRPoint for purpose of logging
			this.estimatedDRPoint = this.mapPoint;
		}
	}

	/*
	 * WiFiLocationFix works only if the wifiScan complete time is within 3s from a detected step
	 */
	public boolean wifiLocationFix(String bssid, Location DREstimate, double distanceFromAP, long scanTime, int RSS) {
		if ( ((scanTime-steptime) < MainActivity.wiFiFixToStep) && ((scanTime-wifiFixTime) > MainActivity.wiFiFixToPrev) ){
			if (this.curMap.hasWifiAP(bssid)){
				WiFiAP temp = this.curMap.getWifiAP(bssid);
				Location WiFiLoc = new Location("");
				WiFiLoc.setLatitude(temp.getLat());
				WiFiLoc.setLongitude(temp.getLon());
				Misc.toast("AP" +  temp.getSSID() + " with " + RSS + "dBm is " + String.format("%.4f", distance) + "m away");
				double estimatedDis = Misc.distanceTo(DREstimate, WiFiLoc);
				if (distanceFromAP<estimatedDis){
					this.mapPoint = Misc.findPointOnTrajectory(DREstimate, WiFiLoc, (estimatedDis-distanceFromAP));
				}
				else {
					this.mapPoint = Misc.findPointOnTrajectory(WiFiLoc, DREstimate, distanceFromAP);
				}
				this.wifiFixTime = System.currentTimeMillis();
				this.WiFiFixChange = true;
				this.requireupdate = true;
				this.mapLog("WiFiFixing");
			return true;
			}
		} 
		return false;
	}
	
	
	public Location MapMatch(Location DRestimate, double brearing, long timestamp, double distance){
		FetchSQL.setDRFixData(DRestimate);
		Location tempLocation = new Location("dummyprovider");
		//Only fix map point if node list is loaded
		if (MapMatching.trajectoriesList.size()>0){
			tempLocation = MapMatching.STMatching(DRestimate, brearing,timestamp,distance);
		}
		return tempLocation;
	}
	


    class updateUITask extends TimerTask {
		public void run() {
			MainActivity.getInstance().runOnUiThread(new Thread(new Runnable(){
				public void run() {
					//Log.d("Map_UI", "running updateUITask_Map");
					//update only when map has been set up and step number change
			    	if (MapFragment.getInstance().mMap!=null){
			    		marker.setRotation((float)Math.toDegrees(MapFragment.getInstance().orientation));
						if (MapFragment.getInstance().requireupdate){
							updateMarker(marker, new LatLng(MapFragment.getInstance().mapPoint.getLatitude(), MapFragment.getInstance().mapPoint.getLongitude()), MapFragment.getInstance().orientation, false); 		
							if (MapFragment.moveCamera)
								mMap.animateCamera(CameraUpdateFactory.newLatLng(marker.getPosition()));
							MapFragment.getInstance().requireupdate = false;
						}
					}
		    	}
			}));
		}
    }
 
	public void updateMarker(final Marker marker, final LatLng toPosition, final double orientation,
            final boolean hideMarker) {
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        Projection proj = mMap.getProjection();
        Point startPoint = proj.toScreenLocation(marker.getPosition());
        final LatLng startLatLng = proj.fromScreenLocation(startPoint);
        final long duration = 500;

        final Interpolator interpolator = new LinearInterpolator();

        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = interpolator.getInterpolation((float) elapsed
                        / duration);
                double lng = t * toPosition.longitude + (1 - t)
                        * startLatLng.longitude;
                double lat = t * toPosition.latitude + (1 - t)
                        * startLatLng.latitude;
                marker.setPosition(new LatLng(lat, lng));
                if (t < 1.0) {
                    // Post again 16ms later.
                    handler.postDelayed(this, 16);
                } else {
                    if (hideMarker) {
                        marker.setVisible(false);
                    } else {
                        marker.setVisible(true);
                    }
                }
            }
        });
        marker.setRotation((float)Math.toDegrees(orientation));
    }

	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		   if (mGroundOverlay != null) {
	            mGroundOverlay.setTransparency((float) progress / (float) TRANSPARENCY_MAX);
	        }
	}
	
	 class FetchMapDataTask extends TimerTask {
	    	public void run() {
	    		new FetchSQL().execute();
	    	}
	    }
	
	

//*****************************************************************************************************************************
//														SUPPORT FUNCTION  
//*****************************************************************************************************************************	
	public Location setLocation(PointF mark){
		Location location = new Location("dummyprovider");
		location.setLatitude(mark.x);
		location.setLongitude(mark.y);
		location.setTime(SystemClock.uptimeMillis());
		return location;
	}
	
	/*Log map when there is mapMatching or WifiFixing
	 * Precondition: TAG needs to be "WiFiFixing" or "MapMatching"
	 */
	public void mapLog(String TAG){
		Double DREstimateLat = this.estimatedDRPoint.getLatitude();
		Double DREstimateLon = this.estimatedDRPoint.getLongitude();
		Double FixedLat = this.mapPoint.getLatitude();
		Double FixedLon = this.mapPoint.getLongitude();
		String line = "";
		if (TAG == "DR"){
			line = DREstimateLat + "," + DREstimateLon + "," + MainActivity.mapLocationMatching + "," + MainActivity.wifiLocationFixing + 
					",FALSE,FALSE,N/A,N/A,N/A,N/A";
		}
		else if (TAG == "MapMatching"){
			line = DREstimateLat + "," + DREstimateLon + "," + MainActivity.mapLocationMatching + "," + MainActivity.wifiLocationFixing + 
						"," + this.MapFixChange + "," + this.WiFiFixChange+ "," + FixedLat + "," + FixedLon + ",N/A,N/A";
			this.MapFixChange = false;
		}
		else if (TAG == "WiFiFixing"){
			line = DREstimateLat + "," + DREstimateLon + "," + MainActivity.mapLocationMatching+ "," + MainActivity.wifiLocationFixing +
					"," + this.MapFixChange + "," + this.WiFiFixChange + ",N/A,N/A," + FixedLat + "," + FixedLon;
			this.WiFiFixChange = false;
		}
		DataLogManager.addLine("mapPath", line);
		//Return MapFixChange to false for next point checking
		
	}
	
	public Location getmapPoint(){
		return this.mapPoint;
	}
	
	public double getLat(){
		return this.mapPoint.getLatitude();
	}
	
	public double getLon(){
		return this.mapPoint.getLongitude();
	}
	
	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub
	}
	

	public Map getCurMap() {
		return this.curMap;
	}
	
	public View getLayout(){
		return this.layout;
	}
}
