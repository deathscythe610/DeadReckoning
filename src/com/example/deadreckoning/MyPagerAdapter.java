package com.example.deadreckoning;

import java.util.HashMap;

import android.content.Context;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

public class MyPagerAdapter extends PagerAdapter {
		private static final String TAG = "TM_PageAdapter";
		private HashMap<Integer, Object> views = new HashMap<Integer, Object>();
		private Integer[] pages = {R.layout.map,R.layout.dead_reckoning,R.layout.dynamic_info};
		private ExtendedViewPager myPager;
		
		public MyPagerAdapter(MainActivity activity) {
			myPager = (ExtendedViewPager) activity.findViewById(R.id.mypager);
	        myPager.setAdapter(this);
		}
	
        public int getCount() {
            return 3;
        }
        
        public void setCurrentItem(int pos){
        	this.myPager.setCurrentItem(pos);
        }

 
        public Object instantiateItem(View collection, int position) {
            LayoutInflater inflater = (LayoutInflater) collection.getContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
 
            View view = inflater.inflate(pages[position], null);
            ((ViewPager) collection).addView(view, 0);
            Log.d("TM_Pager",position+"");
            views.put(position, view);
            switch (position) {
            case 1:
                break;
            case 2:
                break;
	        }
            MainActivity.getInstance().initUI(position);
            
            return view;
        }
        
        protected View findViewForPosition(int position) {
        	return (View) views.get(position);
    	}

 
        @Override
        public void destroyItem(View arg0, int position, Object arg2) {
            ((ViewPager) arg0).removeView((View) arg2);
            views.remove(position);
        }
 
        @Override
        public boolean isViewFromObject(View arg0, Object arg1) {
            return arg0 == ((View) arg1);
 
        }
        
        @Override
        public Parcelable saveState() {
        	return null;
        }
 
		public void setMapFix(boolean fix) {
			this.myPager.setPagingEnabled(fix);
		}
        
}