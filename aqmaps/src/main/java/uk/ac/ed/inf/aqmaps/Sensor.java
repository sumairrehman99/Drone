package uk.ac.ed.inf.aqmaps;

import java.util.ArrayList;

import com.mapbox.geojson.Feature;

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
	public double parseReadings(String reading){
		try {
			return Double.parseDouble(reading);
		}
		catch (NumberFormatException e) {			// the only way an exception will be returned is if the reading is "null" or "NaN" since it isn't a number
			return -1;								// -1 in this case means null or NaN
		}
	}
	

	
	public static void drawSensors(double reading, double battery, String location, Feature f) {
    	if (battery < 10) {
    		f.addStringProperty("location", location);
    		f.addStringProperty("rgb-string", "#000000");
	  		f.addStringProperty("marker-color", "#000000");
	  		f.addStringProperty("marker-symbol", "cross");
    	}
		if (reading == -1) {
			f.addStringProperty("location", location);
	  		f.addStringProperty("rgb-string", "#000000");
	  		f.addStringProperty("marker-color", "#000000");
	  		f.addStringProperty("marker-symbol", "cross");
		}
		
		if (reading >= 0 && reading < 32) {
			f.addStringProperty("location", location);
			f.addStringProperty("rgb-string", "#00ff00");
    		f.addStringProperty("marker-color", "#00ff00");
    		f.addStringProperty("marker-symbol", "lighthouse");
		}
		
		if (reading >= 32 && reading < 64) {
			f.addStringProperty("location", location);
			f.addStringProperty("rgb-string", "#40ff00");
    		f.addStringProperty("marker-color", "#40ff00");
    		f.addStringProperty("marker-symbol", "lighthouse");
		}
		
		if (reading >= 64 && reading < 96) {
			f.addStringProperty("location", location);
			f.addStringProperty("rgb-string", "#80ff00");
    		f.addStringProperty("marker-color", "#80ff00");
    		f.addStringProperty("marker-symbol", "lighthouse");
		}
		
		if (reading >= 96 && reading < 128) {
			f.addStringProperty("location", location);
			f.addStringProperty("rgb-string", "#c0ff00");
    		f.addStringProperty("marker-color", "#c0ff00");
    		f.addStringProperty("marker-symbol", "lighthouse");
		}

		if (reading >= 128 && reading < 160) {
			f.addStringProperty("location", location);
    		f.addStringProperty("rgb-string", "#ffc000");
    		f.addStringProperty("marker-color", "#ffc000");
    		f.addStringProperty("marker-symbol", "danger");
    	}
		
		if (reading >= 160 && reading < 192) {
			f.addStringProperty("location", location);
    		f.addStringProperty("rgb-string", "#ff8000");
    		f.addStringProperty("marker-color", "#ff8000");
    		f.addStringProperty("marker-symbol", "danger");
		}
		
		if (reading >= 192 && reading < 224) {
			f.addStringProperty("location", location);
    		f.addStringProperty("rgb-string", "#ff4000");
    		f.addStringProperty("marker-color", "#ff4000");
    		f.addStringProperty("marker-symbol", "danger");
    	}
    	if (reading >= 224 && reading < 256) {
    		f.addStringProperty("location", location);
    		f.addStringProperty("rgb-string", "#ff0000");
    		f.addStringProperty("marker-color", "#ff0000");
    		f.addStringProperty("marker-symbol", "danger");
    	}

    }   

}