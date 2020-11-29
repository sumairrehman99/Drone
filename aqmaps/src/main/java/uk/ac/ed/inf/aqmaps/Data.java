package uk.ac.ed.inf.aqmaps;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;

public class Data {

	private static final HttpClient CLIENT = HttpClient.newHttpClient();
	private ArrayList<Maps> data_list; // this list stores the location, reading and battery percentage of each sensor
	private ArrayList<Sensor> sensors_list; // this list stores the sensors to be visited for the day
	private String day;
	private String month;
	private String year;

	public Data(String day, String month, String year) throws IOException, InterruptedException {
		this.day = day;
		this.month = month;
		this.year = year;

		// Building a request to get the data for the given day, month and year
		var data_request = HttpRequest.newBuilder()
				.uri(URI.create("http://localhost/maps/" + year + "/" + month + "/" + day + "/air-quality-data.json"))
				.build();

		var data_response = CLIENT.send(data_request, BodyHandlers.ofString());

		Type mapsType = new TypeToken<ArrayList<Maps>>() {}.getType();

		data_list = new Gson().fromJson(data_response.body(), mapsType);

	}

	public String getDay() {
		return day;
	}

	public String getMonth() {
		return month;
	}

	public String getYear() {
		return year;
	}

	
	// returns the list of sensors to be visited on the given day
	public ArrayList<Sensor> getSensors() {
		sensors_list = new ArrayList<Sensor>();
		
		// creating a Sensor object using the entries in data_list and storing them in sensors_list
		for (int i = 0; i < data_list.size(); i++) {
			sensors_list.add(new Sensor(data_list.get(i).getLocation(), data_list.get(i).getReading(),
					data_list.get(i).getBattery()));
		}
		return sensors_list;
	}

	// returns the no fly zones as a list of Polygons
	public ArrayList<Polygon> getNoFly() throws IOException, InterruptedException {

		ArrayList<Polygon> no_fly_zones = new ArrayList<>(4); // this list stores the no_fly zones
		
		// building an HttpRequest to fetch the no fly zones
		var no_fly_request = HttpRequest.newBuilder().uri(URI.create("http://localhost/buildings/no-fly-zones.geojson"))
				.build();
		var no_fly_response = CLIENT.send(no_fly_request, BodyHandlers.ofString());

		FeatureCollection no_fly_collection = FeatureCollection.fromJson(no_fly_response.body());
		List<Feature> no_fly_features = no_fly_collection.features();
		List<Geometry> no_fly_geometrys = new ArrayList<>();

		for (Feature f : no_fly_features) {
			no_fly_geometrys.add(f.geometry());
		}
		
		// if the geometry is an instance of a Polygon, add it to the list
		for (Geometry g : no_fly_geometrys) {
			if (g instanceof Polygon) {
				no_fly_zones.add((Polygon) g);
			}
		}

		return no_fly_zones;

	}

//	 // this method parses each sensor reading as a double and returns them in a list
//	private ArrayList<Double> parseSensorReadings(){
//		ArrayList<Double> readings  = new ArrayList<>();
//		for (int i = 0; i < sensor_details.size(); i++) {
//        	readings.add(sensor_details.get(i).parseReadings(sensor_details.get(i).getReading()));
//        }
//		return readings;
//	}

	// this method returns the details of a What3Words location
//    public ArrayList<HttpResponse<String>> getDetails() throws IOException, InterruptedException{
//    	var split_locations = new ArrayList<String[]>();
//    	
//    	// splitting the locations at "."
//        for (int i = 0; i < sensor_details.size(); i++) {
//      	  split_locations.add(sensor_details.get(i).getLocation().split("\\."));
//        }
//        
//        
//        // this list stores the responses to the word request from the web server
//        var details_list = new ArrayList<HttpResponse<String>>();
//      
//      
//      // pulling the details from the web server
//      for (int i = 0; i < split_locations.size(); i++) {
//    	  var details_request = HttpRequest.newBuilder().uri(URI.create("http://localhost/words/" + split_locations.get(i)[0] + "/" + split_locations.get(i)[1] + "/" + split_locations.get(i)[2] + "/details.json")).build();
//    	  details_list.add(CLIENT.send(details_request, BodyHandlers.ofString()));
//      }
//      return details_list;
//    }

	// this method gets the What3Words names of all the sensors
//    public ArrayList<String> getSensorNames(){
//    	var names = new ArrayList<String>();
//    	
//    	for (int i = 0; i < sensor_details.size(); i++) {
//    		names.add(sensor_details.get(i).getLocation());
//    	}
//    	return names;
//    	
//    }

	// this method returns the coordinates of the each sensor as a (lng,lat) point
//    public ArrayList<Point> getSensorCoordinates () throws IOException, InterruptedException{
//    	 var details_list = getDetails();
//    	 var coordinates_list = new ArrayList<Coordinates>();	// stores the coordinates of all the sensors as Coordinates
//         var sensor_positions = new ArrayList<Point>();			// stores the coordinates of all the sensors as Points	
//          
//         
//         // storing the coordinates of all the sensors to be visited
//         for (int i = 0; i < details_list.size(); i++){
//         	var words = new Gson().fromJson(details_list.get(i).body(), Words.class);
//         	coordinates_list.add(new Coordinates(words.coordinates.lng, words.coordinates.lat));
//         }
//         
//         // converting the Coordinates objects to Points and storing them in sensor_positions
//         for (int p = 0; p < coordinates_list.size(); p++) {
//         	sensor_positions.add(Point.fromLngLat(coordinates_list.get(p).getLongitude(), coordinates_list.get(p).getLatitude()));
//         }
//         
//         return sensor_positions;
//         
//    }

}
	