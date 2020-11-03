package uk.ac.ed.inf.aqmaps;

public class Position {

	private double longitude;
	private double latitude;
	
	
	public Position(double longitude, double latitude) {
		this.longitude = longitude;
		this.latitude = latitude;
	}

	
	public double getLongitude() {
		return longitude;
	}
	
	public double getLatitude() {
		return latitude;
	}
	
	

}
