package com.example.deadreckoning;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.os.Environment;
import android.util.Log;

public class DataLog {
	private static final String TAG = "TM_DataLog";
	private static final String SEP = ",";
	private static final int LINE_LIMIT = 200;
	private String data = "";
	private long lastTime = 0;
	private int lineCount=0;
	private String fileName;
	private File filePath;
	private String prefix;
	private Boolean log = true;
	private DataLogInterface dli=null;
	
	public DataLog() {
		this.prefix="dl";
		this.restartLogging();
		this.log=false;
	}
	
	public DataLog(String prefix) {
		this.prefix=prefix;
		this.restartLogging();
	}
	
	public DataLog(String prefix, DataLogInterface dli) {
		this.prefix=prefix;
		this.restartLogging();
		this.dli = dli;
	}
	
	public void createFileName() {
		this.fileName = this.prefix+this.getDateTime()+".csv";
		//this.filePath = "Logs/"+this.fileName;
	}
	
	public void restartLogging() {
		this.log=true;
		this.data = "";
		this.lineCount=0;
		this.createFileName();
		this.lastTime = 0;
	}
	
	private void continueLogging() {
		this.log=true;
		this.data = "";
		this.lineCount=0;
//		this.createFileName();
	}
	
	public void resumeLogging() {
		this.log=true;
	}
	
	public void stopLogging() {
		this.log=false;
	}
	
	
	public void addLine(String line) {
		this.addLine(line,true);
	}
	
	public void addLine(String line, Boolean addTimeStamp) {
		String time="";
		if(addTimeStamp) {
			time = getTimeDiff();
		}
		synchronized(this) {
			if(!this.log) {
				Log.d(TAG,"no logging ("+this.prefix+")");
				return;
			}
			this.lineCount++;
			if(addTimeStamp) {
				this.data += time + SEP + line + "\n";
			} else {
				this.data += line + "\n";
			}
			
			if(LINE_LIMIT>0 && this.lineCount>=LINE_LIMIT) {
				this.write();
				if(this.dli!=null) {
					dli.stopLogging();
				}
//				Misc.toast("Datalog overflow, saved log "+this.fileName);
			}
		}
	}
	
	private String getTimeDiff() {
		Date date = new Date();
		long currTime=date.getTime();
		if(this.lastTime>0) {
			long diff = currTime - this.lastTime;
//			this.lastTime = currTime;
			return String.valueOf(diff);
		} else {
			this.lastTime=currTime;
			return "0";
		}
	}
	
	public synchronized void write() {
		if(this.data=="") {
			Misc.toast("Nothing to save (DL:"+this.prefix+")");
			return;
		}
    	filePath = new File(Environment.getExternalStorageDirectory()
				+ File.separator + "Log_MainActivity");
    	if (!filePath.exists())
		{
    		System.out.println("Create new directory :" + filePath.toString());
			filePath.mkdirs();
		}
    	File timeFile = new File(filePath, this.fileName);	
        try{
    		OutputStream timeOS = new FileOutputStream(timeFile,true);
    		OutputStreamWriter timeOSW = new OutputStreamWriter(timeOS);
    		timeOSW.write(this.data);
    		timeOSW.close();
		}catch(Exception e){
			Log.e(TAG, "Exception: "+e.toString());
		}
        
    	DataLogManager.refreshLogs();
    	this.continueLogging();
	}
	
	private String getDateTime() {
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
        Date date = new Date();
        return dateFormat.format(date);
    }
	
	public String debugInfo() {
		String data = "";
		data += "Name: "+ this.filePath +"\n";
		data += "LineCount: "+this.lineCount+"\n";
		data += "Log: "+this.log.toString()+"\n";
		data += "\n";
		return data;
	}
	
	public String getFileName() {
		return this.fileName;
	}
}