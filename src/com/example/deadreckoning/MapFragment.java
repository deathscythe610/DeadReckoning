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

public class MapFragment extends FragmentControl  implements OnSeekBarChangeListener{
	
	private static final int TRANSPARENCY_MAX = 100;
	public static Location mapFixLocation = new Location("dummyprovider");
    private static final LatLng NEWARK = new LatLng(1.292412, 103.775672);
    private final List<BitmapDescriptor> mImages = new ArrayList<BitmapDescriptor>();
    
    private static final String TAG = "TM_MapInfo";
    protected GoogleMap mMap;
    private SupportMapFragment supportfragment;
    private GroundOverlay mGroundOverlay;
    private SeekBar mTransparencyBar;
    private View layout;  
    private FragmentManager fm;
    private int mCurrentEntry = 0;
    private boolean skipFirst;
    private List<Marker> markerList;
    private Marker marker;
    private HashMap<String,Map> mapList = new HashMap<String,Map>();
    private PointF defaultStartPoint = new PointF(1.292214f, 103.776072f);
    private static boolean moveCamera;
	private int mCurrentPage;
	private String pageTitle;
	public Map curMap;
	
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
 
    
//*****************************************************************************************************************************
//   												MAP INITIALIZATION   
//*****************************************************************************************************************************
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
    	CheckBox camera = (CheckBox)layout.findViewById(R.id.cameraMove);
    	camera.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				MapFragment.moveCamera = isChecked;
			}
		});;
        mTransparencyBar = (SeekBar)layout.findViewById(R.id.transparencySeekBar);
        mTransparencyBar.setMax(TRANSPARENCY_MAX);
        mTransparencyBar.setProgress(0);
        markerList = new ArrayList<Marker>();
        createUiMap();
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
		}
		createUiMap();
	}


	void createUiMap() {
		  // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
        	mMap = supportfragment.getMap();
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
    	MainActivity.getInstance().mapInfo.mapPoint.x = Lat;
    	MainActivity.getInstance().mapInfo.mapPoint.y = Lon;
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
	
	
//*****************************************************************************************************************************
//													MAP DSIPLAY UPDATE   
//*****************************************************************************************************************************	
	 
	@Override
    public void updateUI() {
    	//update only when map has been set up and step number change
    	if (MainActivity.getInstance().mapInfo!=null){
    		marker.setRotation((float)Math.toDegrees(MainActivity.getInstance().mapInfo.orientation));
			if (MainActivity.getInstance().mapInfo.readyupdate){
				updateMarker(marker, new LatLng(MainActivity.getInstance().mapInfo.mapPoint.x, MainActivity.getInstance().mapInfo.mapPoint.y), MainActivity.getInstance().mapInfo.orientation, false); 		
				mMap.animateCamera(CameraUpdateFactory.newLatLng(marker.getPosition()));
				MainActivity.getInstance().mapInfo.readyupdate = false;
			}
    	}
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
	
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		   if (mGroundOverlay != null) {
	            mGroundOverlay.setTransparency((float) progress / (float) TRANSPARENCY_MAX);
	        }
	}
	
//*****************************************************************************************************************************
//														SUPPORT FUNCTION  
//*****************************************************************************************************************************	

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

	public Map getCurMap() {
		return this.curMap;
	}
	
	public View getLayout(){
		return this.layout;
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
