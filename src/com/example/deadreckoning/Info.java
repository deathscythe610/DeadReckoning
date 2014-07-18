/*
 * THIS CLASS IS USED TO INITIALIZE THE VALUESMAP AND UIMAP HAS HASHMAP TYPE
 */
package com.example.deadreckoning;

import java.util.HashMap;
import java.util.Map;


public abstract class Info {
	Map<String, String> valuesMap = new HashMap<String, String>(); 
	
	abstract void init();
	abstract void update();
	
	public Info() {
	}
}