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
	protected MapFragment mapFragment;
	protected DRFragment drFragment;
	protected SensorFragment sensorFragment;
	
	private Map<Integer,FragmentControl> fragmentClassMap = new HashMap<Integer,FragmentControl>();
		
	public Runnable toastRunnable(final String text){

	    Runnable aRunnable = new Runnable(){
	        public void run(){
	        	Toast.makeText(MainActivity.getInstance(), text,Toast.LENGTH_SHORT).show();
	        }
	    };

	    return aRunnable;

	}
	protected ViewPager vpPager;
	private MyPagerAdapter pagerAdapter;
	public static Boolean wifiLocationFixing = true;
	public static Boolean mapLocationFixing = true;
	protected WifiManager wifiManager;
	private BroadcastReceiver broadcastReceiver=null;
	public static int uiUpdateRate = 500; 
	public static int startupScreen = 1;
	
	
	public static MainActivity getInstance() {
		if(MainActivity.instance==null) {
			Log.e(TAG,"MainActivity singleton not initialized!");
		}
		return MainActivity.instance;
	}
	

	public void init() {
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
  
        //Initialize pager
        vpPager = (ViewPager)findViewById(R.id.mypager);
        //Set number off pages to be load
        vpPager.setOffscreenPageLimit(2);
        pagerAdapter = new MyPagerAdapter(getSupportFragmentManager());
        vpPager.setAdapter(pagerAdapter);
        //Set default start up screen
        Log.i(MainActivity.TAG,"Startup Screen: " + startupScreen);
  		vpPager.setCurrentItem(startupScreen);
  		
		vpPager.setOnPageChangeListener(new OnPageChangeListener() {

            @Override
            public void onPageSelected(int index) {
                switch(index){
                	case 0:
                		MainActivity.getInstance().drFragment = (DRFragment)MainActivity.getInstance().findFragmentByPosition(0);
                		MainActivity.getInstance().fragmentClassMap.put(0, MainActivity.getInstance().drFragment);
                	case 1:
                		MainActivity.getInstance().mapFragment = (MapFragment)MainActivity.getInstance().findFragmentByPosition(1);
                		MainActivity.getInstance().fragmentClassMap.put(1, MainActivity.getInstance().mapFragment);
                		
                	case 2: 
                		MainActivity.getInstance().sensorFragment = (SensorFragment)MainActivity.getInstance().findFragmentByPosition(2);
                		MainActivity.getInstance().fragmentClassMap.put(2, MainActivity.getInstance().sensorFragment);
                		
                		
                }
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
            	DRFragment.getInstance().startCalibrationLogging();
				showDrCalibrationDialog();
				return true;
			case R.id.gyroscopeCalibration:
				SensorFragment.getInstance().triggerGyroscopeCalibration();
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
		this.init();
	}

	@Override
	protected void onPause() {
		Log.d(TAG,"onPause()");
		super.onPause();
		this.unregisterWifiReceiver();
	}
	
	@Override
	protected void onStop() {
		Log.d(TAG,"onStop()");
		DataLogManager.saveAll();
		this.unregisterWifiReceiver();
		super.onStop();
	};

	
	protected void reloadSettings() {
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        int uiUpdateRate = Integer.valueOf(sharedPrefs.getString("ui_refresh_speed","500"));
        DataLogManager.globalLogging = sharedPrefs.getBoolean("globalLogging", false);
        if ( DRFragment.getInstance()!=null)
        	DRFragment.getInstance().setParameters(Float.parseFloat(sharedPrefs.getString("drThresholdMax", "1.0"))
        			,Float.parseFloat(sharedPrefs.getString("drThresholdMin", "-0.9"))
        			,Float.parseFloat(sharedPrefs.getString("drK", "0.7"))
        			);
        MainActivity.startupScreen = Integer.parseInt(sharedPrefs.getString("startup_screen", "1"));
        MainActivity.uiUpdateRate = uiUpdateRate;
        if (SensorFragment.getInstance()!=null)
	        SensorFragment.getInstance().reloadSettings(Integer.valueOf(sharedPrefs.getString("sensor_refresh_speed","3")),
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
	
    private void showDrCalibrationDialog() {
    	new AlertDialog.Builder(MainActivity.getInstance())
		.setTitle(R.string.drCalibrationTitle)
		.setMessage(R.string.drCalibrationMsg)
		.setIcon(android.R.drawable.ic_dialog_alert)
		.setPositiveButton(R.string.done, new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int whichButton) {
		    	DRFragment.getInstance().startCalibrationCalculations();
		    }
	    }).setNegativeButton(android.R.string.cancel,  new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int whichButton) {
		    	DRFragment.getInstance().endCalibration();
		    }
	    }).show();
    }
    
    protected Fragment	findFragmentByPosition(int position) {
    	int pagerid = this.vpPager.getId();
    	return getSupportFragmentManager().findFragmentByTag("android:switcher:" + pagerid + ":" + position);
    }
    
    protected View findViewForPositionInPager(int position) {
    	switch(position){
    		case 0: 
    			return this.drFragment.getLayout();
    		case 1:
    			return this.mapFragment.getLayout();
    		case 2:
    			return this.sensorFragment.getLayout();
    		default:
    			return null;
    	}
    }
    
    class FetchMapDataTask extends TimerTask {
    	public void run() {
    		new FetchSQL().execute();
    	}
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
}
