/*
 * THIS CLASS IS USED TO INITIALIZE THE VALUESMAP AND UIMAP HAS HASHMAP TYPE
 */
package com.example.deadreckoning;

import java.util.HashMap;
import java.util.Map;

import android.widget.TextView;

public abstract class Info {
	Map<String, String> valuesMap = new HashMap<String, String>(); // name -> value
	Map<String, TextView> uiMap = new HashMap<String, TextView>(); // name -> TextView
	
	abstract void createUiMap();
	abstract void init();
	abstract void update();
	
	public Info() {
	}
}