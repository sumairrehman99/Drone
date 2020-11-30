package uk.ac.ed.inf.aqmaps;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import com.mapbox.turf.TurfJoins;
import com.mapbox.turf.TurfMeta;
import com.mapbox.turf.TurfMisc;
import com.mapbox.turf.models.LineIntersectsResult;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;

public class Path {

	private static final int MOVE_LIMIT = 150;
	private static final int TOTAL_SENSORS = 33;
	private static final double MAX_LONGITUDE = -3.184319;
	private static final double MIN_LONGITUDE = -3.192473;
	private static final double MAX_LATITUDE = 55.946233;
	private static final double MIN_LATITUDE = 55.942617;

	private static int move_counter = 0; // keeps track of the number of moves made by the drone
	private ArrayList<Point> current_path = new ArrayList<>(MOVE_LIMIT); // consists of all the points that make up the
																			// flight path
	private static ArrayList<String> sensor_names = new ArrayList<>(TOTAL_SENSORS); // the names of the visited sensors
	private static ArrayList<Integer> angles = new ArrayList<>(MOVE_LIMIT); // stores the angles that the drone moves on
	private Data flight_data;
	private Point starting_point; // the starting point of the drone
	private ArrayList<Geometry> sensor_geometries = new ArrayList<>(TOTAL_SENSORS);
	private static ArrayList<Feature> features_list = new ArrayList<>(TOTAL_SENSORS);

	public Path(Point starting_point, Data flight_data) throws IOException, InterruptedException {
		this.starting_point = starting_point;
		this.flight_data = flight_data;
	}

	// generating a flight path
	public void buildPath() throws IOException, InterruptedException {

		var list_of_sensors = flight_data.getSensors(); // the sensors that are to be read
		var visited_sensors = new ArrayList<Point>(); // the sensors that are already visited
		var sensor_points = new ArrayList<Point>(); // the (lng,lat) coordinates of all the sensors
		var names = new HashMap<Point, Sensor>(); // stores the the coordinates and their corresponding Sensor
		@SuppressWarnings("unused")
		var no_flys = flight_data.getNoFly(); // Fetching the no fly zones from the web server
		current_path.add(starting_point); // the starting position of the drone

		for (int i = 0; i < list_of_sensors.size(); i++) {
			names.put(list_of_sensors.get(i).getPoint(), list_of_sensors.get(i));
		}

		for (int i = 0; i < list_of_sensors.size(); i++) {
			sensor_points.add(list_of_sensors.get(i).getPoint());
		}

		for (int i = 0; i < sensor_points.size(); i++) {
			sensor_geometries.add((Geometry) sensor_points.get(i));
		}

		// adding all the sensors to features_list
		for (int i = 0; i < sensor_geometries.size(); i++) {
			features_list.add(Feature.fromGeometry(sensor_geometries.get(i)));
		}

		// flying to the sensors
		while (visited_sensors.size() < TOTAL_SENSORS || move_counter < MOVE_LIMIT) {

			// the "target" sensor. This is the sensor the drone will fly to.
			Point target_sensor = closestSensor(sensor_points, currentPosition(current_path), visited_sensors);

			// if the first sensor is in range of the starting position of the drone
			if (inRange(target_sensor, currentPosition(current_path)) && move_counter == 0) {

				// move to a arbitrary location 90 degrees north. This is counted as a move.
				angles.add(90);
				Point new_point = Point.fromLngLat(starting_point.longitude() + lngDifference(90),
						starting_point.latitude() + latDifference(90));
				current_path.add(new_point);
				sensor_names.add(null);
				move_counter++;

				// return to the starting location. The drone is now in range to take a reading
				// after having made a move.
				double new_angle = nearestTen(findAngle(currentPosition(current_path), starting_point,
						flightDirection(currentPosition(current_path), starting_point)));
				angles.add((int) new_angle);
				current_path.add(starting_point);
				visited_sensors.add(target_sensor);
				Sensor visited = names.get(target_sensor); // the sensor that was just visited

				sensor_names.add(visited.getLocation()); // getting the visited sensor's name

				Feature sensor_feature = Feature.fromGeometry((Geometry) target_sensor); // converting it to a Feature

				features_list.set(features_list.indexOf(sensor_feature), drawSensor(visited)); // replacing the visited
																								// sensor
																								// in features_list with
																								// a marked version
				move_counter++;
				continue;
			}

			// if the first sensor isn't in range of the drone's starting point

			// calculating the angle at which the drone will move on its next move
			double angle = nearestTen(findAngle(currentPosition(current_path), target_sensor,
					flightDirection(currentPosition(current_path), target_sensor)));

			Point new_point = Point.fromLngLat(currentPosition(current_path).longitude() + lngDifference(angle),
					currentPosition(current_path).latitude() + latDifference(angle));

			/*
			 * checking if the new calculated point lies within the confinement boundary if
			 * not, alter the angle the drone will fly on
			 */
			if (outOfBounds(new_point)) {
				angle = checkBoundary(currentPosition(current_path), new_point);
				new_point = Point.fromLngLat(currentPosition(current_path).longitude() + lngDifference(angle),
						currentPosition(current_path).latitude() + latDifference(angle));
			}
			angles.add((int) angle);
			current_path.add(new_point);

			/*
			 * if the drone is in range add it to visited sensors, mark it add the name to
			 * sensor_names
			 */
			if (inRange(currentPosition(current_path), target_sensor)) {
				visited_sensors.add(target_sensor);
				Sensor visited = names.get(target_sensor); /*
																		 * getting the sensor that was just visited
																		 */
				sensor_names.add(visited.getLocation()); // getting the visited sensor's name

				Feature sensor_feature = Feature.fromGeometry((Geometry) target_sensor); // converting it to a Feature

				features_list.set(features_list.indexOf(sensor_feature),
						drawSensor(visited)); /*
												 * replacing the sensor in features_list with a marked version
												 */
				move_counter++;
			} else {
				sensor_names.add(null);
				move_counter++;
			}

			// this is added in order to terminate the code in case the drone goes in an
			// infinite loop
			if (move_counter == MOVE_LIMIT || visited_sensors.size() == TOTAL_SENSORS) {
				break;
			}
		}

		// returning to the starting position
		while (move_counter < MOVE_LIMIT) {
			double angle = findAngle(currentPosition(current_path), starting_point,
					flightDirection(currentPosition(current_path), starting_point));
			Point new_point = Point.fromLngLat(currentPosition(current_path).longitude() + lngDifference(angle),
					currentPosition(current_path).latitude() + latDifference(angle));

			// if the drone is close enough to the starting position, stop
			if (getDistance(starting_point, currentPosition(current_path)) < 0.0002) {
				break;
			}
			angles.add((int) angle);
			current_path.add(new_point);
			sensor_names.add(null);
			move_counter++;
		}

		// making the flight path of the drone from the Points in current_path
		LineString flight_path = LineString.fromLngLats(current_path);

		features_list.add(Feature.fromGeometry((Geometry) flight_path));

	}

	public ArrayList<Feature> getFeatures() {

		return features_list;
	}

	// this method writes the flight path to a GeoJSON file
	public void writeFlightPath(FeatureCollection collection) {

		String day = flight_data.getDay();
		String month = flight_data.getMonth();
		String year = flight_data.getYear();

		try {
			FileWriter fw = new FileWriter("readings-" + day + "-" + month + "-" + year + ".geojson");
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
	public void writeLogFile() {

		var angles = getAngles();
		String day = flight_data.getDay();
		String month = flight_data.getMonth();
		String year = flight_data.getYear();

		try {
			FileWriter fw = new FileWriter("flightpath-" + day + "-" + month + "-" + year + ".txt");
			PrintWriter pw = new PrintWriter(fw);

			for (int i = 1; i <= move_counter; i++) {
				pw.println(i + "," + current_path.get(i - 1).longitude() + "," + current_path.get(i - 1).latitude()
						+ "," + angles.get(i - 1) + "," + current_path.get(i).longitude() + ","
						+ current_path.get(i).latitude() + "," + sensor_names.get(i - 1));
			}
			pw.close();
		} catch (IOException e) {
			System.out.println("error");
		}
	}

	/*
	 * this method takes a sensor and returns a marked Feature version of it
	 * according to it's reading, battery and location
	 */
	private static Feature drawSensor(Sensor sensor) throws IOException, InterruptedException {
		// converting the Sensor object to a Feature
		Feature sensor_feature = Feature.fromGeometry((Geometry) sensor.getPoint());

		// drawing the sensor
		Sensor.draw(sensor.parseReading(), sensor.getBattery(), sensor.getLocation(), sensor_feature);

		return sensor_feature;
	}

	private static ArrayList<Integer> getAngles() {
		return angles;
	}

	// returns the Euclidean distance between two points
	private static double getDistance(Point p1, Point p2) {

		double sum = Math.pow((p1.longitude() - p2.longitude()), 2) + Math.pow((p1.latitude() - p2.latitude()), 2);

		double distance = Math.sqrt(sum);

		return distance;
	}

	// returns the closest sensor to the drone that hasn't been visited yet
	private static Point closestSensor(List<Point> sensors, Point drone, List<Point> visited_sensors) {

		// An arbitrary Point. This will always be outside the drone confinement area.
		Point closest = Point.fromLngLat(MAX_LONGITUDE + 100, MAX_LATITUDE + 100);

		for (Point sensor : sensors) {
			// finding the closest sensor that isn't in visited_sensors
			if (getDistance(drone, sensor) <= getDistance(drone, closest) && !visited_sensors.contains(sensor)) {
				closest = sensor;
			}

		}

		return closest;
	}

	/*
	 * returns the drone's current position This is basically the last added Point
	 * in the current_path list
	 */
	private static Point currentPosition(List<Point> path) {
		return path.get(path.size() - 1);
	}

	/*
	 * rounds the number to the nearest ten. 48 is rounded up to 50. 43 is rounded
	 * down to 40.
	 */
	private static double nearestTen(double number) {
		return ((Math.round(number + 5) / 10) * 10);
	}

	// returns the longitude difference when drone is flying at angle
	private static double lngDifference(double angle) {

		/*
		 * this is the difference in longitude that will be caused by the drone moving
		 * at the given angle
		 */
		double difference = Math.cos(Math.toRadians(angle)) * 0.0003;

		return difference;
	}

	// returns the latitude difference when drone is flying at angle
	private static double latDifference(double angle) {

		/*
		 * this is the difference in latitude that will be caused by the drone moving at
		 * the given angle
		 */
		double difference = Math.sin(Math.toRadians(angle)) * 0.0003;

		return difference;
	}

	// checks if drone is within 0.0002 degrees of the sensor
	private static boolean inRange(Point drone, Point sensor) {
		if (getDistance(drone, sensor) < 0.0002) {
			return true;
		} else {
			return false;
		}
	}

	// this method return the direction the drone is flying
	// (north-east, south-west, ..., etc.)
	private static String flightDirection(Point from, Point to) {
		String direction = "";
		if (to.latitude() > from.latitude() && to.longitude() > from.longitude()) {
			direction = "north-east";
		} else if (to.latitude() > from.latitude() && to.longitude() < from.longitude()) {
			direction = "north-west";
		} else if (to.latitude() < from.latitude() && to.longitude() > from.longitude()) {
			direction = "south-east";
		} else if (to.latitude() < from.latitude() && to.longitude() < from.longitude()) {
			direction = "south-west";
		} else if (to.latitude() == from.latitude() && to.longitude() < from.longitude()) {
			direction = "west";
		} else if (to.latitude() == from.latitude() && to.longitude() > from.longitude()) {
			direction = "east";
		} else if (to.latitude() < from.latitude() && to.longitude() == from.longitude()) {
			direction = "south";
		} else if (to.latitude() > from.latitude() && to.longitude() == from.longitude()) {
			direction = "north";
		}
		return direction;
	}

	// calculates the angle the drone will have to fly to reach the target point
	private static double findAngle(Point drone, Point target_point, String direction) {
		double lng_difference = Math.abs(target_point.longitude() - drone.longitude()); // the longtiude difference b/w
																						// the drone and the target
		double lat_difference = Math.abs(target_point.latitude() - drone.latitude()); // the latitude difference b/w the
																						// drone and the target

		double basic_angle = 0; /*
								 * the angle the horizantal makes with the hypotenuse in a right-angle triangle
								 * where the hypotenuse is 0.0003 i.e the length of each move the drone makes
								 */

		double angle = 0;

		basic_angle = nearestTen(Math.toDegrees(Math.atan(lat_difference / lng_difference)));

		// the angle is calculated using the 360 degree ASTC graph
		switch (direction) {
		case "north-east":
			angle = basic_angle;
			break;
		case "north-west":
			angle = basic_angle + 90;
			break;
		case "north":
			angle = 90;
			break;
		case "west":
			angle = 180;
			break;
		case "south":
			angle = 270;
			break;
		case "east":
			angle = 0;
			break;
		case "south-west":
			angle = basic_angle + 180;
			break;
		case "south-east":
			angle = basic_angle + 270;
			break;
		}

		/*
		 * Since the drone moves at angles between 0 and 350 moving at 360 is the same
		 * as moving at 0
		 */
		if (angle == 360) {
			angle = 0;
		}

		return angle;

	}

	// this method checks if the drone is outside the confinement area
	private boolean outOfBounds(Point new_point) {
		boolean out = false;
		if (new_point.longitude() >= MAX_LONGITUDE || new_point.longitude() <= MIN_LONGITUDE) {
			out = true;
		}
		if (new_point.latitude() >= MAX_LATITUDE || new_point.latitude() <= MIN_LATITUDE) {
			out = true;
		}
		return out;
	}

	/*
	 * this method alters the angle the drone is travelling on based on the
	 * direction in which it crosses the boundary
	 */
	private double checkBoundary(Point drone, Point new_point) {
		double new_angle = 0;

		// if the drone is about to cross the right boundary and is flying south-east
		if (new_point.longitude() >= MAX_LONGITUDE && flightDirection(drone, new_point) == "south-east") {
			new_angle = 270;
		}
		// if the drone is about to cross the right boundary and is flying north-east
		if (new_point.longitude() >= MAX_LONGITUDE && flightDirection(drone, new_point) == "north-east") {
			new_angle = 90;
		}

		// if the drone is about to cross the top boundary and is flying
		// north/north-west
		if (new_point.latitude() >= MAX_LATITUDE && ((flightDirection(drone, new_point) == "north"
				|| flightDirection(drone, new_point) == "north-west"))) {
			new_angle = 180;
		}
		// if the drone is about to cross top boundary and is flying north-east
		if (new_point.latitude() >= MAX_LATITUDE && flightDirection(drone, new_point) == "north-east") {
			new_angle = 0;
		}
		// if the drone is about to cross the left boundary and is flying south-west
		if (new_point.longitude() <= MIN_LONGITUDE && flightDirection(drone, new_point) == "south-west") {
			new_angle = 270;
		}
		// if the drone is about to cross the left boundary and is flying north-west
		if (new_point.longitude() <= MIN_LONGITUDE && flightDirection(drone, new_point) == "north-west") {
			new_angle = 90;
		}
		// if the drone is about to cross the bottom boundary and is flying south-east
		if (new_point.latitude() <= MIN_LATITUDE && flightDirection(drone, new_point) == "south-east") {
			new_angle = 0;
		}
		// if the drone is about to cross the bottom boundary and is flying south-east
		if (new_point.latitude() <= MIN_LATITUDE && flightDirection(drone, new_point) == "south-west") {
			new_angle = 180;
		}
		return new_angle;
	}

	public int getMoves() {
		return move_counter;
	}

}