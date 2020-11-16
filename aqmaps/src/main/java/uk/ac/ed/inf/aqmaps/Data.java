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

	private static final HttpClient client = HttpClient.newHttpClient();
	private ArrayList<Maps> data_list;			 // this list stores the location, reading and battery percentage of each sensor
	private ArrayList<Sensor> sensor_details;	 // this list stores the sensors as objects
	
	public Data(String day, String month, String year) throws IOException, InterruptedException {
		
		var data_request = HttpRequest.newBuilder().uri(URI.create("http://localhost/maps/" + year + "/" + month + "/" + day + "/air-quality-data.json")).build();
        var data_response = client.send(data_request, BodyHandlers.ofString());
        
        Type mapsType = new TypeToken<ArrayList<Maps>>() {}.getType();
        
       
        data_list = new Gson().fromJson(data_response.body(), mapsType);
        
        sensor_details = new ArrayList<Sensor>();
        
        for (int i = 0; i < data_list.size(); i++) {
        	sensor_details.add(new Sensor(data_list.get(i).location, data_list.get(i).reading, data_list.get(i).battery));
        }
        
        
	}
	
	
	
	// this method returns an arraylist of sensors which contains their locations, readings and battery percentages
	public ArrayList<Sensor> getSensorDetails(){
		return sensor_details;
	}
	
	public ArrayList<Double> getSensorReadings(ArrayList<Sensor> sensors){
		ArrayList<Double> readings  = new ArrayList<>();
		for (int i = 0; i < sensors.size(); i++) {
        	readings.add(sensors.get(i).parseReadings(sensors.get(i).getReading()));
        }
		return readings;
	}
	
	
	// drawing the sensors on the map
    public void drawSensors(ArrayList<Double> readings, ArrayList<Sensor> sensors, ArrayList<Feature> feature_list) {
    	for (int i = 0; i < sensors.size(); i++) {
    		sensors.get(i);
    		Sensor.drawSensors(readings.get(i), sensor_details.get(i).getBattery(), sensor_details.get(i).getLocation(), feature_list.get(i));
    	}
    }
	
    
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
    	  details_list.add(client.send(details_request, BodyHandlers.ofString()));
      }
      return details_list;
    }
	
    
    public ArrayList<Point> getSensorCoordinates (ArrayList<HttpResponse<String>> detailsList){
    	
    	 var coordinates_list = new ArrayList<Coordinates>();	// stores the coordinates of all the sensors 
         var sensor_positions = new ArrayList<Point>();			
          
         
         for (int i = 0; i < detailsList.size(); i++){
         	var words = new Gson().fromJson(detailsList.get(i).body(), Words.class);
         	coordinates_list.add(new Coordinates(words.coordinates.lng, words.coordinates.lat));
         }
         
         for (int p = 0; p < coordinates_list.size(); p++) {
         	sensor_positions.add(Point.fromLngLat(coordinates_list.get(p).getLongitude(), coordinates_list.get(p).getLatitude()));
         }
         
         return sensor_positions;
         
    }
    
}

	
	
	

