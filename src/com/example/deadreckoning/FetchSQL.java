/*
 * THIS CLASS WILL BE SCHEDULED TO RUN IN THE BACKGOURND EVERY 3 SENCONDS 
 * IT WILL QUERY THE DATABSE AND TAKE IN MAP NODES IN 20M AROUND THE CURRENT POINT. EACH POINT WILL BE 0.5M APART 
 * THE LIST IS UDATED EVERY 3S 
 */
package com.example.deadreckoning;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
/*
import com.javadocmd.simplelatlng.LatLng;
import com.javadocmd.simplelatlng.util.LengthUnit;
import com.javadocmd.simplelatlng.window.RectangularWindow;
*/
import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;

class FetchSQL extends AsyncTask<Void,Void,String> {
	private static String DEBUG = null;
	static double segmentizedistance = 0.5;
	static double rangeQueryRadius = 20;

	private static Location rawDRFix;

	
	//public static double lonMin, lonMax, latMin, latMax;

	@Override
	protected String doInBackground(Void... params) {
		if(rawDRFix!=null){
			getMapNodesAndSTMatch();
		}
		return DEBUG;
	}

	public void getMapNodesAndSTMatch(){
		Log.d("SQL", "Fetch SQL Process Started");
		ResultSet rsTrajectories = null;

		try {
			Class.forName("org.postgresql.Driver");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		//Establishes a connection the PSQL database
		String DB_connection = "jdbc:postgresql://" + SQLSettingsActivity.getSQLSettings().getIPAddress()+"/"+ SQLSettingsActivity.getSQLSettings().getDBName(); 
		String DB_user = SQLSettingsActivity.getSQLSettings().getUsername();
		String DB_password = SQLSettingsActivity.getSQLSettings().getPassword();
		Connection conn;
		try { 
			DriverManager.setLoginTimeout(5);
			conn = DriverManager.getConnection(DB_connection, DB_user, DB_password);
			Statement stStartNodes = conn.createStatement();

			String sqlStartNodes;
			/*
			//Creates a rectangular window used for the range query (for selecting the candidate line strings and nodes)
			RectangularWindow rectangularWindow = new RectangularWindow(new LatLng(rawDRFix.getLatitude()
					, rawDRFix.getLongitude()), rangeQueryRadius, rangeQueryRadius, LengthUnit.METER);
			latMin = rectangularWindow.getMinLatitude();     
			latMax =  rectangularWindow.getMaxLatitude();
			lonMin =  rectangularWindow.getLeftLongitude();
			lonMax =  rectangularWindow.getRightLongitude();
			*/
			sqlStartNodes ="SELECT " +	
							        "id," +
							        "sector," +
							        "description," +
							        "StartLon," +
							        "StartLat," +
							        "EndLon," +
							        "EndLat," +
							        "StrictFix," +  
							        "azimuth" +
							"FROM " +
									"i3_building.floordata " +
							"WHERE("+
									"ST_DWithin("+
											"ST_Transform(ST_GeomFromText('POINT(" + FetchSQL.rawDRFix.getLongitude() + " " + FetchSQL.rawDRFix.getLatitude() + ")',4326),3414)," +
											"ST_Transform(geom,3414)," +
											FetchSQL.rangeQueryRadius + ")) " +
							"ORDER BY " + 
									"id;";
			Log.d(DEBUG, "SQL StartNodes Command: " + sqlStartNodes);

			//Execution of the query and the ResultSet returned
			rsTrajectories = stStartNodes.executeQuery(sqlStartNodes);

			UpdateTrajectoriesList(rsTrajectories);

			rsTrajectories.close();
			stStartNodes.close();

			conn.close();
		} catch (SQLException e) {
			System.err.println("Connection Failed, Check console");
			e.printStackTrace();
		}
	}

	public static void UpdateTrajectoriesList(ResultSet rsTrajectories){
		try {
			//Clear mapNodesList Array to add new array
			MapFixing.trajectoriesList.clear();
			while(rsTrajectories.next()) {

				String sector = rsTrajectories.getString(2);
				String description = rsTrajectories.getString(3);

				//read out the StartNodes Latitudes and Longitudes from the ResultSet
				Double StartLongitude = Double.parseDouble(rsTrajectories.getString(4));
				Double StartLatitude = Double.parseDouble(rsTrajectories.getString(5));
				Double EndLongitude = Double.parseDouble(rsTrajectories.getString(6));
				Double EndLatitude = Double.parseDouble(rsTrajectories.getString(7));
				Double Azimuth = Double.parseDouble(rsTrajectories.getString(9));
				Boolean StrictFix;
				if (rsTrajectories.getString(8) == "TRUE")
					StrictFix = true;
				else 
					StrictFix = false;
				
				Trajectory newTrajectory= new Trajectory(StartLatitude, StartLongitude, EndLatitude, EndLongitude, sector, description, StrictFix, Azimuth);
				MapFixing.trajectoriesList.add(newTrajectory);
				/*
				if(StartLongitude >(lonMin) && StartLongitude<(lonMax)&&
						StartLatitude>(latMin) && StartLatitude<(latMax)){
					MapFixing.mapNodesList.add(newStartCandidate);
				}
				*/
			}
		} catch (NumberFormatException e1) {
			e1.printStackTrace();
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
		Log.d(DEBUG,"Close nodes: " + new Integer(MapFixing.trajectoriesList.size()).toString());
	}
	
	@Override
	protected void onPostExecute(String value) {
	}

	public static void setDRFixData(Location newFix){
		rawDRFix = newFix;
		//if(newFix.hasAccuracy())
		//	rangeQueryRadius = 1.5*newFix.getAccuracy();
	}

	public static void setRangeQueryRadius(int rangeRadius){
		rangeQueryRadius = rangeRadius;
	}
}