package com.example.deadreckoning;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.example.deadreckoning.DataLog;
import com.example.deadreckoning.DataLogManager;
import com.example.deadreckoning.MainActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

public class DataLogManager {
	private static final String TAG = "TM_DataLogManager";
	private static Map<String,DataLog> dataLogs = new HashMap<String, DataLog>();
	public static Boolean globalLogging = false;
	private static List<String> allowList = new ArrayList<String>();
	
	public static void resumeLogging(String dataLogName) {
		if(DataLogManager.dataLogs.containsKey(dataLogName)) {
			DataLog temp = dataLogs.get(dataLogName);
			temp.resumeLogging();
		} else {
			Log.d(TAG,"No DataLog with name: "+dataLogName);
		}
	}
	
	public static void restartLogging(String dataLogName) {
		if(DataLogManager.dataLogs.containsKey(dataLogName)) {
			DataLog temp = dataLogs.get(dataLogName);
			temp.restartLogging();
		} else {
			Log.d(TAG,"No DataLog with name: "+dataLogName);
		}
	}
	
	public static void stopLogging(String dataLogName) {
		if(DataLogManager.dataLogs.containsKey(dataLogName)) {
			DataLog temp = dataLogs.get(dataLogName);
			temp.stopLogging();
		} else {
			Log.d(TAG,"No DataLog with name: "+dataLogName);
		}
	}
	
	public static void saveLog(String dataLogName) {
		synchronized (DataLogManager.dataLogs) {
			if(DataLogManager.dataLogs.containsKey(dataLogName)) {
				DataLog temp = dataLogs.get(dataLogName);
				temp.write();
			} else {
				Log.d(TAG,"No DataLog with name: "+dataLogName);
				Misc.toast("No DataLog with name: "+dataLogName);
			}
		}
	}
	
	public static void addLine(String dataLogName, String line) {
		DataLogManager.addLine(dataLogName, line,true);
	}
	
	public static void addLine(String dataLogName, String line, Boolean addTimeStamp) {
//		synchronized (DataLogManager.dataLogs) {
			if(DataLogManager.isAllowed(dataLogName)) {
				if(!DataLogManager.dataLogs.containsKey(dataLogName)) {
					DataLogManager.initLog(dataLogName, null);
					Log.d(TAG,"initlog");
				} 
				DataLog temp = dataLogs.get(dataLogName);
				temp.addLine(line,addTimeStamp);
			}
//		}
	}
	
	/**
	 * creates dataLog on internal storage
	 * @param dataLogName
	 * @param dli
	 * @return dataLog name on storage
	 */
	public static String initLog(String dataLogName,DataLogInterface dli) {
		synchronized (DataLogManager.dataLogs) {
			if(DataLogManager.isAllowed(dataLogName)) {
				DataLog temp = new DataLog(dataLogName, dli);
//				temp.stopLogging();
				DataLogManager.dataLogs.put(dataLogName, temp);
				Misc.toast("Init log: "+dataLogName);
				return temp.getFileName();
			}
		}
		return null;
	}
	
	public static String getInfo() {
		Iterator<Entry<String, DataLog>> it = DataLogManager.dataLogs.entrySet().iterator();
		String data = "";
		while(it.hasNext()) {
			Map.Entry<String, DataLog> pairs = (Map.Entry<String, DataLog>)it.next();
			DataLog temp = pairs.getValue();
			data += pairs.getKey() + ": " +  temp.debugInfo();
		}
		return data;
	}
	
	
	public static void resetAll() {
		Iterator<Entry<String, DataLog>> it = DataLogManager.dataLogs.entrySet().iterator();
		while(it.hasNext()) {
			Map.Entry<String, DataLog> pairs = (Map.Entry<String, DataLog>)it.next();
			DataLog temp = pairs.getValue();
			temp.restartLogging();
		}
	}
	
	public static void saveAll() {
		Iterator<Entry<String, DataLog>> it = DataLogManager.dataLogs.entrySet().iterator();
		while(it.hasNext()) {
			Map.Entry<String, DataLog> pairs = (Map.Entry<String, DataLog>)it.next();
			DataLog temp = pairs.getValue();
			temp.write();
		}
	}
	
	public static void refreshLogs() {
		MainActivity.getInstance().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://"
                + Environment.getExternalStorageDirectory()+"/Logs"))); 
//		File f = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/Logs");
//		new SingleMediaScanner(m, Environment
//		          .getExternalStorageDirectory().getAbsolutePath()+"/Logs");
//	     File[] files = f.listFiles();
//	     for (File file : files){
//	    	 Log.d(TAG,file.getAbsolutePath());
//	    	 new SingleMediaScanner(m, file.getAbsolutePath());
//	     }
	}
	
	/**
	 * is the specified log name allowed to keep logs?
	 * @param logName logname to check
	 * @return Boolean is the specified log name allowed to keep logs?
	 */
	public static Boolean isAllowed(String logName) {
		return (DataLogManager.globalLogging && DataLogManager.allowList.contains(logName));
	}
	
	/**
	 * allow the logging of the specified log
	 * @param logName logname to allow
	 */
	public static void allow(String logName) {
		DataLogManager.allowList.add(logName);
	}
}
