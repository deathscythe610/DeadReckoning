package com.example.deadreckoning;

import java.util.Date;

import android.widget.Toast;

public class Misc {
	
	
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