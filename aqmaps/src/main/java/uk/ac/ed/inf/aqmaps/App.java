
package uk.ac.ed.inf.aqmaps;

import java.io.IOException;
import java.net.http.HttpClient;

import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;


public class App {
	
	private static final HttpClient CLIENT = HttpClient.newHttpClient();	// one HttpClient is shared between classes

	private static void setUp(String day, String month, String year, String port, Point starting_point)
			throws IOException, InterruptedException {

		Data data = new Data(day, month, year, port);

		Path path = new Path(starting_point, data);

		path.buildPath();

		var feature_list = path.getFeatures();
		
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
		var port = args[6];

		// defining the starting point using the starting longitude and starting latitude
		Point starting_point = Point.fromLngLat(starting_longitude, starting_latitude);

		setUp(day, month, year, port, starting_point);

	}

}