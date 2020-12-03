package uk.ac.ed.inf.aqmaps;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;


import com.google.gson.Gson;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;

public class Sensor {
	private String location;
	private String reading;
	private double battery;
	
	public Sensor(String location, String reading, double battery) {
		this.location = location;
		this.reading = reading;
		this.battery = battery;

	}

	public String getLocation() {
		return location;
	}

	public String getReading() {
		return reading;
	}

	public double getBattery() {
		return battery;
	}

	// parses the sensor reading as a double
	public double parseReading() {
		var reading = getReading();

		try {
			return Double.parseDouble(reading);
		} 
		catch (NumberFormatException e) {  // An exception will be returned is if the reading is "null" or "NaN" since
										   // those aren't numbers
			return -1; // -1 in this case means null or NaN
		}
	}

	// returns a Point using the sensor's What3Words address
	public Point getPoint() throws IOException, InterruptedException {
		String location = getLocation();
		
		// splitting the sensor location at the "."
		String[] split_location = location.split("\\.");

		var server_request = HttpRequest.newBuilder().uri(URI.create("http://localhost:" + Data.getPort() + "/words/" + split_location[0] + "/"
				+ split_location[1] + "/" + split_location[2] + "/details.json")).build();
		var server_response = App.getClient().send(server_request, BodyHandlers.ofString());

		var words = new Gson().fromJson(server_response.body(), Words.class);

		Point sensor_point = Point.fromLngLat(words.coordinates.lng, words.coordinates.lat);
		return sensor_point;

	}

	// this method draws a sensor according to the reading and battery
	public static Feature draw(double reading, double battery, String location, Feature sensor_feature) {

		if (battery < 10) {
			sensor_feature.addStringProperty("location", location);
			sensor_feature.addStringProperty("rgb-string", "#000000");
			sensor_feature.addStringProperty("marker-color", "#000000");
			sensor_feature.addStringProperty("marker-symbol", "cross");
		}
		// if the reading is "NaN" or null
		if (reading == -1) {
			sensor_feature.addStringProperty("location", location);
			sensor_feature.addStringProperty("rgb-string", "#000000");
			sensor_feature.addStringProperty("marker-color", "#000000");
			sensor_feature.addStringProperty("marker-symbol", "cross");
		}

		if (reading >= 0 && reading < 32) {
			sensor_feature.addStringProperty("location", location);
			sensor_feature.addStringProperty("rgb-string", "#00ff00");
			sensor_feature.addStringProperty("marker-color", "#00ff00");
			sensor_feature.addStringProperty("marker-symbol", "lighthouse");
		}

		if (reading >= 32 && reading < 64) {
			sensor_feature.addStringProperty("location", location);
			sensor_feature.addStringProperty("rgb-string", "#40ff00");
			sensor_feature.addStringProperty("marker-color", "#40ff00");
			sensor_feature.addStringProperty("marker-symbol", "lighthouse");
		}

		if (reading >= 64 && reading < 96) {
			sensor_feature.addStringProperty("location", location);
			sensor_feature.addStringProperty("rgb-string", "#80ff00");
			sensor_feature.addStringProperty("marker-color", "#80ff00");
			sensor_feature.addStringProperty("marker-symbol", "lighthouse");
		}

		if (reading >= 96 && reading < 128) {
			sensor_feature.addStringProperty("location", location);
			sensor_feature.addStringProperty("rgb-string", "#c0ff00");
			sensor_feature.addStringProperty("marker-color", "#c0ff00");
			sensor_feature.addStringProperty("marker-symbol", "lighthouse");
		}

		if (reading >= 128 && reading < 160) {
			sensor_feature.addStringProperty("location", location);
			sensor_feature.addStringProperty("rgb-string", "#ffc000");
			sensor_feature.addStringProperty("marker-color", "#ffc000");
			sensor_feature.addStringProperty("marker-symbol", "danger");
		}

		if (reading >= 160 && reading < 192) {
			sensor_feature.addStringProperty("location", location);
			sensor_feature.addStringProperty("rgb-string", "#ff8000");
			sensor_feature.addStringProperty("marker-color", "#ff8000");
			sensor_feature.addStringProperty("marker-symbol", "danger");
		}

		if (reading >= 192 && reading < 224) {
			sensor_feature.addStringProperty("location", location);
			sensor_feature.addStringProperty("rgb-string", "#ff4000");
			sensor_feature.addStringProperty("marker-color", "#ff4000");
			sensor_feature.addStringProperty("marker-symbol", "danger");
		}
		if (reading >= 224 && reading < 256) {
			sensor_feature.addStringProperty("location", location);
			sensor_feature.addStringProperty("rgb-string", "#ff0000");
			sensor_feature.addStringProperty("marker-color", "#ff0000");
			sensor_feature.addStringProperty("marker-symbol", "danger");
		}
		return sensor_feature;
	}

}