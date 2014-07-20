package com.example.deadreckoning;

import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.View;

public class MyPagerAdapter extends FragmentPagerAdapter {
		private static final String TAG = "TM_PageAdapter";
		private static int numItems = 3;
		public int fragmentcreated = 0;
		
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
        			this.fragmentcreated++;
        			return MapFragment.newInstance(0, "Map Information");
        		case 1:
        			this.fragmentcreated++;
        			return DRFragment.newInstance(1, "DR Information");
        		case 2: 
        			this.fragmentcreated++;
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
        
        public static String makeFragmentName(int viewId, int index) {
            return "android:switcher:" + viewId + ":" + index;
       }
    
        
        @Override
        public Parcelable saveState() {
        	return null;
        }
 

		
        
}