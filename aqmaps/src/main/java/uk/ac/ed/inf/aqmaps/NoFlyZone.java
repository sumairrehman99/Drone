package uk.ac.ed.inf.aqmaps;

import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;


public class NoFlyZone {

	private Polygon p;
	
	
	public NoFlyZone(Polygon p) {
		this.p = p;
	}
	
	
	public Polygon getZone() {
		return p;
	}
	
	
	
	
	
}
