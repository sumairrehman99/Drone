package uk.ac.ed.inf.aqmaps;

import uk.ac.ed.inf.aqmaps.Words.Square.Northeast;
import uk.ac.ed.inf.aqmaps.Words.Square.Southwest;

public class Words {
	private String country;
	private String nearestPlace;
	private String words;
	private String language;
	private String map;
	
	
	public Words(String country, String nearestPlace, String words, String language, String map) {
		this.country = country;
		this.nearestPlace = nearestPlace;
		this.words = words;
		this.language = language;
		this.map = map;
	}
	
	public static class Square{
		public static class Southwest{
			double lng;
			double lat;
		}
		
		public static class Northeast{
			double lng;
			double lat;
		}
	}
	
	public static class Coordinates{
		double lng;
		double lat;
	}
	
	Square square;
	Southwest southwest;
	Northeast northeast;
	Coordinates coordinates;

	public String getCountry() {
		return country;
	}
	
	public String nearestPlace() {
		return nearestPlace;
	}
	
	public String words() {
		return words;
	}
	
	public String getLanguage() {
		return language;
	}
	
	public String getMap() {
		return map;
	}
	
}