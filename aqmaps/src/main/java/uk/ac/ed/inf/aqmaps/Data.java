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

public class Data {

	private static final HttpClient client = HttpClient.newHttpClient();
	private ArrayList<Maps> data_list;			 // this list stores the location, reading and battery percentage of each sensor
	private ArrayList<Sensor> sensorDetails;	 // this list stores the sensors as objects
	private ArrayList<HttpResponse<String>> detailsList;
	private ArrayList<String[]> splitLocations;
	
	public Data(String day, String month, String year) throws IOException, InterruptedException {
		
		var data_request = HttpRequest.newBuilder().uri(URI.create("http://localhost/maps/" + year + "/" + month + "/" + day + "/air-quality-data.json")).build();
        var data_response = client.send(data_request, BodyHandlers.ofString());
        
        Type mapsType = new TypeToken<ArrayList<Maps>>() {}.getType();
        
       
        data_list = new Gson().fromJson(data_response.body(), mapsType);
        
        sensorDetails = new ArrayList<Sensor>();
        
        for (int i = 0; i < data_list.size(); i++) {
        	sensorDetails.add(new Sensor(data_list.get(i).location, data_list.get(i).reading, data_list.get(i).battery));
        }
        
        
	}
	
	
	
	public ArrayList<Sensor> getSensors(){
		return sensorDetails;
	}
	
	public ArrayList<Double> getSensorReadings(ArrayList<Sensor> sensors){
		ArrayList<Double> readings  = new ArrayList<>();
		for (int i = 0; i < sensors.size(); i++) {
        	readings.add(sensors.get(i).parseReadings(sensors.get(i).getReading()));
        }
		return readings;
	}
	
	
	// drawing the sensors on the map
    public void drawSensors(ArrayList<Double> readings, ArrayList<Sensor> sensors, ArrayList<Feature> f) {
    	for (int i = 0; i < sensors.size(); i++) {
    		sensors.get(i);
    		Sensor.drawSensors(readings.get(i), sensorDetails.get(i).getBattery(), sensorDetails.get(i).getLocation(), f.get(i));
    	}
    }
	
	
	
}
	
	
	

