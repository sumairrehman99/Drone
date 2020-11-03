package uk.ac.ed.inf.aqmaps;

import uk.ac.ed.inf.aqmaps.Words.Square.Northeast;
import uk.ac.ed.inf.aqmaps.Words.Square.Southwest;

public class Words {
	public String country;
	public String nearestPlace;
	public String words;
	public String language;
	public String map;
	
	
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

}