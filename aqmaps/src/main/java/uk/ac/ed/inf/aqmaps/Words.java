package uk.ac.ed.inf.aqmaps;

import uk.ac.ed.inf.aqmaps.Words.Square.Northeast;
import uk.ac.ed.inf.aqmaps.Words.Square.Southwest;

public class Words {
	
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