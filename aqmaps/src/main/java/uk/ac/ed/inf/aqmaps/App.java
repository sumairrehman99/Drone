
package uk.ac.ed.inf.aqmaps;

import java.io.IOException;
import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.List;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;


public class App {
	
	private static final HttpClient CLIENT = HttpClient.newHttpClient();	// one HttpClient is shared between classes

	private static void setUp(String day, String month, String year, Point starting_point)
			throws IOException, InterruptedException {

		Data data = new Data(day, month, year);

		Path path = new Path(starting_point, data);

		path.buildPath();

		var feature_list = path.getFeatures();

		
		  var line_points = new ArrayList<Point>();
		  line_points.add(Point.fromLngLat(-3.192473, 55.946233));
		  line_points.add(Point.fromLngLat(-3.184319, 55.946233));
		  line_points.add(Point.fromLngLat(-3.184319, 55.942617));
		  line_points.add(Point.fromLngLat(-3.192473, 55.942617));
		  line_points.add(Point.fromLngLat(-3.192473, 55.946233));
		  
		  
		  var lines_list = new ArrayList<List<Point>>();
		  
		  lines_list.add(line_points);
		  
		  LineString boundary = LineString.fromLngLats(line_points);
		  
		  feature_list.add(Feature.fromGeometry((Geometry) boundary));
		 
		FeatureCollection collection = FeatureCollection.fromFeatures(feature_list);

		Writer path_writer = new Writer(path);
		
		path_writer.writeReadings(collection);
		path_writer.writeFlightPath();

		System.out.println(path.getMoves());

	}
	
	public static HttpClient getClient() {
		return CLIENT;
	}

	public static void main(String[] args) throws IOException, InterruptedException {

		var day = args[0];
		var month = args[1];
		var year = args[2];
		var starting_latitude = Double.parseDouble(args[3]);
		var starting_longitude = Double.parseDouble(args[4]);

		// defining the starting point using the starting longitude and starting latitude
		Point starting_point = Point.fromLngLat(starting_longitude, starting_latitude);

		setUp(day, month, year, starting_point);

	}

}
