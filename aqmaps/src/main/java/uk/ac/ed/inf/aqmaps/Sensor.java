package uk.ac.ed.inf.aqmaps;


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
	
	
	
	
}
