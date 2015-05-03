/*
 * THIS CLASS ADD PREFERENCE FROM THE PREFERENCE.XML FILE 
 */
package com.example.deadreckoning;

//import android.app.AlertDialog;
//import android.content.DialogInterface;
//import android.preference.Preference;
//import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.os.Bundle;

public class Preferences extends PreferenceActivity {
 
    @SuppressWarnings("deprecation")
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }
} 