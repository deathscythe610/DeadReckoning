package com.example.deadreckoning;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
import android.support.v4.app.Fragment;
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
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class MapFragment extends Fragment  implements OnSeekBarChangeListener{
	
	private static final int TRANSPARENCY_MAX = 100;
	public static Location mapFixLocation = new Location("dummyprovider");
    private static final LatLng NEWARK = new LatLng(1.292412, 103.775672);
    private final List<BitmapDescriptor> mImages = new ArrayList<BitmapDescriptor>();
    
    private static final String TAG = "TM_MapInfo";
    private GoogleMap mMap;
    private GroundOverlay mGroundOverlay;
    private SeekBar mTransparencyBar;
    private View layout;
    private Map curMap;
    private FragmentManager fm;
    private int mCurrentEntry = 0;
    private boolean skipFirst;
    private PointF mapPoint = new PointF(0,0);
    private List<Marker> markerList;
    private final double EarthRadius = 6371000;
    private Marker marker;
    private static final int wifiFixRadius = 6;
    private HashMap<String,Map> mapList = new HashMap<String,Map>();
    private PointF defaultStartPoint = new PointF(1.292214f, 103.776072f);
    private int steps = 0;
    private long steptime = 0;
    private boolean mapFix = false;
	private int mCurrentPage;
	private String pageTitle;
	
	
    public MapFragment(){
    	super();
    	this.loadMaps();
    }
    
    public static MapFragment newInstance(int position, String title){
    	MapFragment mapFragment = new MapFragment();
    	Bundle args = new Bundle();
    	args.putInt("current_page", 0);
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
						tempMap.addWifiAP(xpp.getAttributeValue(null, "bssid"),xpp.getAttributeFloatValue(null, "Lat", 0.0f),xpp.getAttributeFloatValue(null, "Lon",0.0f));
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
		mCurrentPage = getArguments().getInt("current_page", 0);
		pageTitle = getArguments().getString("page_title");
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		layout = (View)inflater.inflate(R.layout.map, container, false);
    	if (layout==null){
    		Log.d(TAG,"layout null");
			return null;
    	}
    	CheckBox mapFix = (CheckBox)layout.findViewById(R.id.mapFix);
    	mapFix.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				MainActivity.getInstance().setMapFix(isChecked);
			}
		});;
        mTransparencyBar = (SeekBar)layout.findViewById(R.id.transparencySeekBar);
        mTransparencyBar.setMax(TRANSPARENCY_MAX);
        mTransparencyBar.setProgress(0);
        markerList = new ArrayList<Marker>();
        createUiMap();
		
		return layout;
	}
	
	void createUiMap() {
		  // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            fm = MainActivity.getInstance().getSupportFragmentManager();
        	mMap = ((SupportMapFragment) fm.findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
                populateMapStartPointsSpinner(layout);
                setStartPointSpinnerListener(layout);
                this.setUpMarker(this.defaultStartPoint.x, this.defaultStartPoint.y);
                FetchSQL.setDRFixData(this.setLocation(defaultStartPoint));
            }
        }
	}
       
    void update() {
    	//update only when map has been set up and step number change
    	if (this.mMap!=null){
    		float orientation = MainActivity.getInstance().sensorInfo.orientationFusion.getFusedZOrientation()+this.curMap.getRotationRadians();
    		float distance = MainActivity.getInstance().deadReckoning.getDistance();
    		marker.setRotation((float)Math.toDegrees(orientation));
    		if(this.steps<MainActivity.getInstance().deadReckoning.getSteps()){
				steps = MainActivity.getInstance().deadReckoning.getSteps();
				steptime = MainActivity.getInstance().deadReckoning.getStepTime();
				updateCoodinate(distance, orientation);
				//MapFix called if map fix option is enabled
				if (MainActivity.mapLocationFixing){
					this.mapPoint = MapFix(this.mapPoint, orientation,this.steptime);
				}
				updateMarker(marker, new LatLng(this.mapPoint.x, this.mapPoint.y), orientation, false); 		
				mMap.animateCamera(CameraUpdateFactory.newLatLng(marker.getPosition()));
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
        
/*
 * Set up a starting point marker at the beginning of the program and when spinner item is selected 
 */
    private void setUpMarker(float Lat, float Lon){
    	this.mapPoint.x = Lat;
    	this.mapPoint.y = Lon;
    	LatLng Position = new LatLng(Lat, Lon);
        marker = mMap.addMarker(new MarkerOptions()
		.position(Position).title("Marker")
		.title("current position")
		.icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_arrow))
		.anchor(0.5f, 0.73f)
		.rotation(0)	
        .flat(true));
        if (mMap!=null){
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
    
	public void updateCoodinate(float distance, float orientation){
		double newLat,newLon;
		if ((mapPoint.x==0) && (mapPoint.y==0)){
			newLat = this.curMap.getStartLat();
			newLon = this.curMap.getStartLon();
		}
		else
		{
			double orgLat = Math.toRadians(mapPoint.x);
			double orgLon = Math.toRadians(mapPoint.y);
			newLat = Math.asin(Math.sin(orgLat)*Math.cos(distance/this.EarthRadius) +
										Math.cos(orgLat)*Math.sin(distance/this.EarthRadius)*Math.cos(orientation));
			newLon = orgLon + Math.atan2(Math.sin(orientation)*Math.sin(distance/this.EarthRadius)*Math.cos(orgLat), 
														Math.cos(distance/this.EarthRadius)-Math.sin(orgLat)*Math.sin(newLat));
		}
		if ((!Double.isNaN(newLat)) && (!Double.isNaN(newLon))){
			mapPoint.x = (float) Math.toDegrees(newLat);
			mapPoint.y = (float) Math.toDegrees(newLon);
		}
	}
	
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		   if (mGroundOverlay != null) {
	            mGroundOverlay.setTransparency((float) progress / (float) TRANSPARENCY_MAX);
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
				MainActivity.getInstance().deadReckoning.reset();
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
	
	public void updateMarker(final Marker marker, final LatLng toPosition, final float orientation,
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
	
	public Boolean wifiLocationFix(String bssid) {
		if(this.curMap.hasWifiAP(bssid)) {
			WiFiAP temp = this.curMap.getWifiAP(bssid);
			float orgLat = this.getLat();
			float orgLon = this.getLon();
			float dLat = orgLat - temp.Lat;
			float dLon = orgLon - temp.Lon;
			double Bx = Math.cos(temp.Lat) * Math.cos(dLon);
	        double By = Math.cos(temp.Lat) * Math.sin(dLon);
	        double dist = Math.sin(orgLat) * Math.sin(temp.Lat) + Math.cos(orgLat) * Math.cos(temp.Lat) * Math.cos(dLon);
			if(dist>wifiFixRadius) { //fix location
				double lat3 = Math.atan2(Math.sin(orgLat)+Math.sin(temp.Lat),Math.sqrt( (Math.cos(orgLat)+Bx)*(Math.cos(orgLat)+Bx) + By*By) );
		        double lon3 = orgLon + Math.atan2(By, Math.cos(orgLat) + Bx);
				MainActivity.getInstance().deadReckoning.reset();
				this.curMap.setPosition((float) lat3, (float) lon3);
				Misc.toast("wifi location fixed");
				DataLogManager.addLine("wififix", orgLat+", "+orgLon+", " + this.curMap.getStartLat()+", "+this.curMap.getStartLon());
				return true;
			}
		}
		return false;
	}
	
	public PointF MapFix(PointF DRestimate, float brearing, long timestamp){
		Toast.makeText(MainActivity.getInstance(), "Doing Mapmatching", Toast.LENGTH_SHORT).show();
		Location location = setLocation(DRestimate);
		FetchSQL.setDRFixData(location);
		//Only fix map point if node list is loaded
		if (MapFixing.mapNodesList.size()>0){
			location = MapFixing.STMatching(location,brearing,timestamp);
		}
		DRestimate.x = (float) location.getLatitude();
		DRestimate.y = (float) location.getLongitude();
		return DRestimate;
	}
	
	public void setMapFix(boolean mapFix){
		this.mapFix=mapFix;
	}
	
	public View getView(){
		return this.layout;
	}
	
	public Location setLocation(PointF mark){
		Location location = new Location("dummyprovider");
		location.setLatitude(mark.x);
		location.setLongitude(mark.y);
		location.setTime(SystemClock.uptimeMillis());
		return location;
	}
	private float getLat(){
		return this.mapPoint.x;
	}
	
	private float getLon(){
		return this.mapPoint.y;
	}
	
	public Map getCurMap() {
		return this.curMap;
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub
	}
}
