package uk.ac.ed.inf.aqmaps;


public class Maps {
	private String location;
	private double battery;
	private String reading;
	
	public Maps(String location, double battery, String reading) {
		location = this.location;
		battery = this.battery;
		reading = this.reading;
	}
	
	
	public String getLocation() {
		return location;
	}
	
	public double getBattery() {
		return battery;
	}
	
	public String getReading() {
		return reading;
	}
}
