package com.example.deadreckoning;

import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;


public class MyPagerAdapter extends FragmentPagerAdapter {
		
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
        			return DRFragment.newInstance(0, "DR Information");
        		case 1:
        			return MapFragment.newInstance(1, "Map Information");
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
					return "DR Info";
				case 1:
					return "Map Info";
				case 2: 
					return "Sensor Info";
				default:
					return null;
			}
        }
        
        public static String makeFragmentName(int viewId, int index) {
            return "android:switcher:" + viewId + ":" + index;
       }
    
        
        @Override
        public Parcelable saveState() {
        	return null;
        }
 

		
        
}