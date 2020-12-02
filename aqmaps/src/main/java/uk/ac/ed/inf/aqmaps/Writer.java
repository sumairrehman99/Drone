package uk.ac.ed.inf.aqmaps;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import com.mapbox.geojson.FeatureCollection;

public class Writer {

	private Path path;
	
	
	public Writer(Path path) {
		this.path = path;
		
	}
	
	// this method writes the flight path to a GeoJSON file
		public void writeReadings(FeatureCollection collection) {

			
			Data data = path.getFlightData();

			try {
				FileWriter fw = new FileWriter("readings-" + data.getDay() + "-" + data.getMonth() + "-" + data.getYear() + ".geojson");
				PrintWriter pw = new PrintWriter(fw);

				// converting the FeatureCollection to a JSON file
				pw.println(collection.toJson());
				pw.close();

			}

			catch (IOException e) {
				System.out.println("error");
			}
		}

		// this method writes the flighpath-DD-MM-YYYY.txt file
		public void writeFlightPath() {
			
			Data data = path.getFlightData();
			var angles = path.getAngles();
			var calculated_points = path.getCalculatedPoints();
			var sensor_names = path.getConnectedSensors();

			try {
				FileWriter fw = new FileWriter("flightpath-" + data.getDay() + "-" + data.getMonth() + "-" + data.getYear() + ".txt");
				PrintWriter pw = new PrintWriter(fw);

				for (int i = 1; i <= path.getMoves(); i++) {
					pw.println(i + "," + calculated_points.get(i - 1).longitude() + "," + calculated_points.get(i - 1).latitude()
							+ "," + angles.get(i - 1) + "," + calculated_points.get(i).longitude() + ","
							+ calculated_points.get(i).latitude() + "," + sensor_names.get(i - 1));
				}
				pw.close();
			} catch (IOException e) {
				System.out.println("error");
			}
		}
	
}
