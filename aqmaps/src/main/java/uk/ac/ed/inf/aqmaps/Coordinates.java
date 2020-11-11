package uk.ac.ed.inf.aqmaps;

import java.util.ArrayList;

import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.Point;

public class Coordinates {

	private double longitude;
	private double latitude;
	private ArrayList<Geometry> geometry_list;
	
	
	public Coordinates(double longitude, double latitude) {
		this.longitude = longitude;
		this.latitude = latitude;
	}

	
	public double getLongitude() {
		return longitude;
	}
	
	public double getLatitude() {
		return latitude;
	}
	
	public void plotSensors(ArrayList<Coordinates> coordinates, ArrayList<Point> sensors) {
		
	}
	

}
