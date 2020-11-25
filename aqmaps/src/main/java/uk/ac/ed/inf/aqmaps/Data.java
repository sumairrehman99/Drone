package uk.ac.ed.inf.aqmaps;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.Point;

public class Data {

	private static final HttpClient CLIENT = HttpClient.newHttpClient();
	private ArrayList<Maps> data_list;			 // this list stores the location, reading and battery percentage of each sensor
	private ArrayList<Sensor> sensor_details;	 // this list stores the sensors to be visited for the day
	
	public Data(String day, String month, String year) throws IOException, InterruptedException {
		
		// Building a request to get the data for the given day, month and year
		var data_request = HttpRequest.newBuilder().uri(URI.create("http://localhost/maps/" + year + "/" + month + "/" + day + "/air-quality-data.json")).build();
        
		var data_response = CLIENT.send(data_request, BodyHandlers.ofString());
        
        Type mapsType = new TypeToken<ArrayList<Maps>>() {}.getType();
        
       
        data_list = new Gson().fromJson(data_response.body(), mapsType);
        
        sensor_details = new ArrayList<Sensor>();
        
        for (int i = 0; i < data_list.size(); i++) {
        	sensor_details.add(new Sensor(data_list.get(i).getLocation(), data_list.get(i).getReading(), data_list.get(i).getBattery()));
        }
        
        
	}
	
	
	
	 // this method parses each sensor reading as a double and returns them in a list
	private ArrayList<Double> parseSensorReadings(){
		ArrayList<Double> readings  = new ArrayList<>();
		for (int i = 0; i < sensor_details.size(); i++) {
        	readings.add(sensor_details.get(i).parseReadings(sensor_details.get(i).getReading()));
        }
		return readings;
	}
	
	
	// drawing the sensors on the map
    public void drawSensors(ArrayList<Feature> feature_list) {
    	// this list contains the readings of each sensor in sensor_details parsed as doubles
    	var readings_list = parseSensorReadings();
    	
    	for (int i = 0; i < sensor_details.size(); i++) {
    		Sensor.draw(readings_list.get(i), sensor_details.get(i).getBattery(), sensor_details.get(i).getLocation(), feature_list.get(i));
    	}
    }
	
    
    // this method returns the details of a What3Words location
    public ArrayList<HttpResponse<String>> getDetails() throws IOException, InterruptedException{
    	var split_locations = new ArrayList<String[]>();
    	
    	// splitting the locations at "."
        for (int i = 0; i < sensor_details.size(); i++) {
      	  split_locations.add(sensor_details.get(i).getLocation().split("\\."));
        }
        
        
        // this list stores the responses to the word request from the web server
        var details_list = new ArrayList<HttpResponse<String>>();
      
      
      // pulling the details from the web server
      for (int i = 0; i < split_locations.size(); i++) {
    	  var details_request = HttpRequest.newBuilder().uri(URI.create("http://localhost/words/" + split_locations.get(i)[0] + "/" + split_locations.get(i)[1] + "/" + split_locations.get(i)[2] + "/details.json")).build();
    	  details_list.add(CLIENT.send(details_request, BodyHandlers.ofString()));
      }
      return details_list;
    }
	
    
    // this method gets the What3Words names of all the sensors
    public ArrayList<String> getSensorNames(){
    	var names = new ArrayList<String>();
    	
    	for (int i = 0; i < sensor_details.size(); i++) {
    		names.add(sensor_details.get(i).getLocation());
    	}
    	return names;
    	
    }
    
    
    // this method returns the coordinates of the each sensor as a (lng,lat) point
    public ArrayList<Point> getSensorCoordinates () throws IOException, InterruptedException{
    	 var details_list = getDetails();
    	 var coordinates_list = new ArrayList<Coordinates>();	// stores the coordinates of all the sensors as Coordinates
         var sensor_positions = new ArrayList<Point>();			// stores the coordinates of all the sensors as Points	
          
         
         // storing the coordinates of all the sensors to be visited
         for (int i = 0; i < details_list.size(); i++){
         	var words = new Gson().fromJson(details_list.get(i).body(), Words.class);
         	coordinates_list.add(new Coordinates(words.coordinates.lng, words.coordinates.lat));
         }
         
         // converting the Coordinates objects to Points and storing them in sensor_positions
         for (int p = 0; p < coordinates_list.size(); p++) {
         	sensor_positions.add(Point.fromLngLat(coordinates_list.get(p).getLongitude(), coordinates_list.get(p).getLatitude()));
         }
         
         return sensor_positions;
         
    }
    
 
    
}

	
	
	

