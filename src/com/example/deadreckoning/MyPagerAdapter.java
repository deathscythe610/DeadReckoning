package com.example.deadreckoning;

import java.util.HashMap;

import android.os.Bundle;
import android.content.Context;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

public class MyPagerAdapter extends FragmentPagerAdapter {
		private static final String TAG = "TM_PageAdapter";
		private static int numItems = 3;
		
		public MyPagerAdapter(FragmentManager fragmentManager){
			super(fragmentManager);
		}
	
        public int getCount() {
            return numItems;
        }
        
        @Override
		public Fragment getItem(int position) {
        	switch(position){
        		case 0:
        			return MapFragment.newInstance(0, "Map Information");
        		case 1:
        			return DRFragment.newInstance(1, "DR Information");
        		case 2: 
        			return SensorFragment.newInstance(2, "Sensor Information");
        		default: 
        			return null;
        	}
		}
        
        @Override
		public CharSequence getPageTitle(int position) {
			switch(position){
				case 0:
					return "Map Information";
				case 1:
					return "DR Information";
				case 2: 
					return "Sensor Information";
				default:
					return null;
			}
        }
        
         
        @Override
        public boolean isViewFromObject(View arg0, Object arg1) {
            return arg0 == ((View) arg1);
 
        }
        
        @Override
        public Parcelable saveState() {
        	return null;
        }
 

		
        
}