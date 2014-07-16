package com.example.deadreckoning;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Debug;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;


public class MainActivity extends FragmentActivity{
	private static final String TAG = "TM_MainActivity";
	private static MainActivity instance=null;
	private MapFragment mapFragment;
	private DRFragment drFragment;
	private SensorFragment sensorFragment;
	
	private DynamicInfoUpdater diu;
	private Map<Integer,Fragment> fragmentClassMap = new HashMap<Integer,Fragment>();
	protected SensorInfo sensorInfo;
	protected DeadReckoning deadReckoning;
	protected MapInfo mapInfo;

	public Runnable toastRunnable(final String text){

	    Runnable aRunnable = new Runnable(){
	        public void run(){
	        	Toast.makeText(MainActivity.getInstance(), text,Toast.LENGTH_SHORT).show();
	        }
	    };

	    return aRunnable;

	}
	ViewPager mPager;
	private MyPagerAdapter pagerAdapter;
	private Timer deadReckoningTimer;
	private Timer mapFixingTimer;
	public static Boolean wifiLocationFixing = true;
	public static Boolean mapLocationFixing = true;
	protected WifiManager wifiManager;
	private BroadcastReceiver broadcastReceiver=null;
	
	public static MainActivity getInstance() {
		if(MainActivity.instance==null) {
			Log.e(TAG,"MainActivity singleton not initialized!");
		}
		return MainActivity.instance;
	}
	
	/**
	 * called after each resume
	 */
	public void init() {
		deadReckoningTimer = new Timer();
		deadReckoningTimer.scheduleAtFixedRate(new deadReckoningTask(), 0, 10);
		if(MainActivity.mapLocationFixing){
			mapFixingTimer = new Timer();
			mapFixingTimer.scheduleAtFixedRate(new FetchMapDataTask(), 0, 3000);
		}
		if(MainActivity.wifiLocationFixing) {
			this.wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
			if(!this.wifiManager.isWifiEnabled()){
	          this.wifiManager.setWifiEnabled(true);
	        }
			
			this.broadcastReceiver = new WiFiScanReceiver();
			this.registerReceiver(this.broadcastReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
		}
	}
		
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	this.getSupportFragmentManager();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        MainActivity.instance=this;
        ViewPager vpPager = (ViewPager)findViewById(R.id.mypager);
        pagerAdapter = new MyPagerAdapter(getSupportFragmentManager());
        vpPager.setAdapter(pagerAdapter);
        mapFragment = (MapFragment)pagerAdapter.getItem(0);
        drFragment = (DRFragment)pagerAdapter.getItem(1);
        sensorFragment = (SensorFragment)pagerAdapter.getItem(2);
        
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
		int startupScreen = Integer.valueOf(sharedPrefs.getString("startup_screen","0"));
		vpPager.setCurrentItem(startupScreen);
		
		vpPager.setOnPageChangeListener(new OnPageChangeListener() {

            @Override
            public void onPageSelected(int index) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onPageScrolled(int arg0, float arg1, int arg2) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onPageScrollStateChanged(int arg0) {
                // TODO Auto-generated method stub
            }
        });
		fragmentClassMap.put(0, this.mapFragment);
		fragmentClassMap.put(1, this.drFragment);
		fragmentClassMap.put(2, this.sensorFragment);
        this.diu = new DynamicInfoUpdater(fragmentClassMap);
        
        //init default SQL configuration
        SQLSettingsActivity.initSQLConfig();
        this.reloadSettings();
        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://"
                + Environment.getExternalStorageDirectory())));
    }
    

		

	@Override
    public void onDestroy() {
    	Log.d(TAG,"onDestroy()");
    	super.onDestroy();
    }
    
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.static_info, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.menu_settings:
            	startActivity(new Intent(this, Preferences.class));
                return true;
            case R.id.exit:
            	finish();
            	return true;
            case R.id.startDataLogMain:
            	DataLogManager.allow("datalog");
            	return true;
            case R.id.startDataLogMapPath:
            	DataLogManager.allow("wififix");
            	String wififixLogName = DataLogManager.initLog("wififix", null);
            	DataLogManager.allow("mapPath");
            	DataLogManager.addLine("mapPath", "%%% wififixfile='"+wififixLogName+"';",false);            	
            	return true;
            case R.id.drCalibration:
            	this.deadReckoning.startCalibrationLogging();
				showDrCalibrationDialog();
				return true;
			case R.id.gyroscopeCalibration:
				this.sensorFragment.triggerGyroscopeCalibration();
				return true;
			case R.id.change_psql_settings:
				startActivity(new Intent(this, SQLSettingsActivity.class));
				return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    @Override
    public void onBackPressed() {
    	finish();
    }
    
    @Override
	protected void onResume() {
		super.onResume();
		reloadSettings();
		DataLogManager.resetAll();
		sensorInfo.init();
		this.mapInfo.init();
		this.init();
	}

	@Override
	protected void onPause() {
		Log.d(TAG,"onPause()");
		super.onPause();
		sensorInfo.sensorManager.unregisterListener(sensorInfo);
		this.sensorInfo.stopLogging();
		if(this.deadReckoningTimer!=null)
			this.deadReckoningTimer.cancel();
		this.unregisterWifiReceiver();
	}
	
	@Override
	protected void onStop() {
		Log.d(TAG,"onStop()");
		DataLogManager.saveAll();
		this.sensorInfo.stopLogging();
		if(this.deadReckoningTimer!=null)
			this.deadReckoningTimer.cancel();
		this.mapInfo.removeallMarker();
		this.unregisterWifiReceiver();
		super.onStop();
	};
	
	protected void reloadSettings() {
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        int uiUpdateRate = Integer.valueOf(sharedPrefs.getString("ui_refresh_speed","500"));
        DataLogManager.globalLogging = sharedPrefs.getBoolean("globalLogging", false);
        this.deadReckoning.setParameters(Float.parseFloat(sharedPrefs.getString("drThresholdMax", "1.0"))
        		,Float.parseFloat(sharedPrefs.getString("drThresholdMin", "-0.9"))
        		,Float.parseFloat(sharedPrefs.getString("drK", "0.7"))
        		);
        this.diu.restart(uiUpdateRate);
        this.sensorInfo.reloadSettings(Integer.valueOf(sharedPrefs.getString("sensor_refresh_speed","3")),
    		Float.valueOf(sharedPrefs.getString("gyroscopeXOffset","0.0")),
			Float.valueOf(sharedPrefs.getString("gyroscopeYOffset","0.0")),
			Float.valueOf(sharedPrefs.getString("gyroscopeZOffset","0.0")),
			Short.parseShort(sharedPrefs.getString("dr_orientation_source", "2")),
			Float.valueOf(sharedPrefs.getString("fuse_coefficient","0.95"))
		);
        //this.mapInfo.reloadSettings(sharedPrefs.getBoolean("mapFullRotation", true));
        MainActivity.wifiLocationFixing=sharedPrefs.getBoolean("wifiLocationFixing", true);
        MainActivity.mapLocationFixing=sharedPrefs.getBoolean("mapLocationFixing", true);
	}
	
	protected void initUI(int pos) {
		this.diu.initUI(pos);
	}
	
    private void showDrCalibrationDialog() {
    	new AlertDialog.Builder(MainActivity.getInstance())
		.setTitle(R.string.drCalibrationTitle)
		.setMessage(R.string.drCalibrationMsg)
		.setIcon(android.R.drawable.ic_dialog_alert)
		.setPositiveButton(R.string.done, new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int whichButton) {
		    	deadReckoning.startCalibrationCalculations();
		    }
	    }).setNegativeButton(android.R.string.cancel,  new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int whichButton) {
		    	deadReckoning.endCalibration();
		    }
	    }).show();
    }
    
    protected View findViewForPositionInPager(int position) {
    	return this.pagerAdapter.findViewForPosition(position);
    }
    
    class deadReckoningTask extends TimerTask {
		public void run() {
			drFragment.trigger_zhanhy(sensorInfo.getWorldAccelerationZ(),sensorInfo.getWorldAccelerationX(), sensorInfo.orientationFusion.getOrientation());
		}
	}
    
    class FetchMapDataTask extends TimerTask {
    	public void run() {
    		new FetchSQL().execute();
    	}
    }
    
    private void setTitleBarMemoryUsage() {
    	int usedMegs = (int)(Debug.getNativeHeapAllocatedSize() / 1048576L);
		String usedMegsString = String.format(" - Memory Used: %d MB", usedMegs);
		getWindow().setTitle(usedMegsString);
    }
    
    /**
     * unregister broadcastReceiver and catch IllegalArgumentException
     * exception happens when we try to unregister the receiver more than once
     */
    private void unregisterWifiReceiver() {
    	Log.d(TAG,"unregisterWifiReceiver()");
    	try{
    		this.unregisterReceiver(this.broadcastReceiver);
    	} catch(IllegalArgumentException ex) {
    		Log.d(TAG,"caught exception from unregistering");
    	}
    }
    protected void setMapFix(boolean fix) {
    	Log.d(TAG,"mapviewlocked: ");
    	this.pagerAdapter.setMapFix(fix);
    	this.mapInfo.setMapFix(fix);
    }
}
