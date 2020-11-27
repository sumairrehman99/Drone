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
	private ArrayList<Point> current_path;
	private static ArrayList<String> sensor_names = new ArrayList<>(TOTAL_SENSORS); // stores the names of the sensors
																					// in the order they are visited
	private static ArrayList<Integer> angles = new ArrayList<>(MOVE_LIMIT); // stores the angles that the drone moves
	private Data data;
	private ArrayList<Geometry> geometry_list = new ArrayList<>(TOTAL_SENSORS);
	private static ArrayList<Feature> features_list = new ArrayList<>(TOTAL_SENSORS);

	public Path(ArrayList<Point> current_path, Data data) throws IOException, InterruptedException {
		this.current_path = current_path;
		this.data = data;
	}

	// generating a flight path
	public void buildPath() throws IOException, InterruptedException {

		var list_of_sensors = data.getSensors();					// the sensors that are to be read
		var visited_sensors = new ArrayList<Point>();				// the sensors that are already visited
		var sensor_coordinates = new ArrayList<Point>();			// the coordinates of all the sensors
		@SuppressWarnings("unused")
		var no_flys = data.getNoFly();								// Fetching the no fly zones from the web server
		final Point starting_point = current_path.get(0);

		for (int i = 0; i < list_of_sensors.size(); i++) {
			sensor_coordinates.add(list_of_sensors.get(i).getCoordinates());
		}

		for (int i = 0; i < sensor_coordinates.size(); i++) {
			geometry_list.add((Geometry) sensor_coordinates.get(i));
		}

		// this will plot all the sensors on the map
		for (int i = 0; i < geometry_list.size(); i++) {
			features_list.add(Feature.fromGeometry(geometry_list.get(i)));
		}

		// flying to the sensors
		while (visited_sensors.size() < TOTAL_SENSORS || move_counter < MOVE_LIMIT) {
			Point next_sensor = closestSensor(sensor_coordinates, currentPosition(current_path), visited_sensors);

			// if the first sensor is in range of the starting position of the drone
			if (inRange(next_sensor, currentPosition(current_path)) && move_counter == 0) {

				// move to a arbitrary location 90 degrees north. This is counted as a move.
				angles.add(90);
				Point new_point = Point.fromLngLat(starting_point.longitude() + lngDifference(90),
						starting_point.latitude() + latDifference(90));
				current_path.add(new_point);
				sensor_names.add(null);
				move_counter++;

				// return to the starting location. The drone is now in range to take a reading,
				// after having made a move.
				double new_angle = nearestTen(findAngle(currentPosition(current_path), starting_point,
						sensorDirection(currentPosition(current_path), starting_point)));
				angles.add((int) new_angle);
				current_path.add(starting_point);
				visited_sensors.add(next_sensor);
				Sensor visited = list_of_sensors.get(sensor_coordinates.indexOf(next_sensor)); // getting the sensor
																								// that was visited
				sensor_names.add(visited.getLocation()); // getting the visited sensor's name
				Feature sensor_feature = Feature.fromGeometry((Geometry) next_sensor); // converting it to a Feature
				features_list.set(features_list.indexOf(sensor_feature), drawSensor(visited)); // replacing the sensor
																								// in features_list with
																								// a marked version
				move_counter++;
				continue;
			}

			// if the first sensor isn't in range of the drone's starting point

			double angle = nearestTen(findAngle(currentPosition(current_path), next_sensor,
					sensorDirection(currentPosition(current_path), next_sensor)));
			angles.add((int) angle);
			Point new_point = Point.fromLngLat(currentPosition(current_path).longitude() + lngDifference(angle),
					currentPosition(current_path).latitude() + latDifference(angle));
			new_point = checkBoundary(new_point, currentPosition(current_path));

			current_path.add(new_point);

			// if the sensor is in range of the drone, add it to visited_sensors and add the
			// name to sensor_names
			if (inRange(currentPosition(current_path), next_sensor)) {
				visited_sensors.add(next_sensor);
				Sensor visited = list_of_sensors.get(sensor_coordinates.indexOf(next_sensor)); // getting the sensor
																								// that was visited
				sensor_names.add(visited.getLocation()); // getting the visited sensor's name
				Feature sensor_feature = Feature.fromGeometry((Geometry) next_sensor); // converting it to a Feature
				features_list.set(features_list.indexOf(sensor_feature), drawSensor(visited)); // replacing the sensor
																								// in features_list with
																								// a marked version
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
					sensorDirection(currentPosition(current_path), starting_point));
			angles.add((int) angle);
			Point new_point = Point.fromLngLat(currentPosition(current_path).longitude() + lngDifference(angle),
					currentPosition(current_path).latitude() + latDifference(angle));

			// once the drone is close enough to the starting position, stop
			if (getDistance(starting_point, currentPosition(current_path)) < 0.0003) {
				break;
			}

			current_path.add(new_point);
			sensor_names.add(null);
			move_counter++;
		}

		LineString flight_path = LineString.fromLngLats(current_path);
		features_list.add(Feature.fromGeometry((Geometry) flight_path));

	}

	public ArrayList<Feature> getFeatures() {

		return features_list;
	}

	// this method writes the flight path to a GeoJSON file
	public void writeFlightPath(FeatureCollection collection) {

		String day = data.getDay();
		String month = data.getMonth();
		String year = data.getYear();

		try {
			FileWriter fw = new FileWriter("readings-" + day + "-" + month + "-" + year + ".geojson");
			PrintWriter pw = new PrintWriter(fw);

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
		String day = data.getDay();
		String month = data.getMonth();
		String year = data.getYear();

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

	// this method takes a sensor and returns a marked Feature version of it
	// according to it's reading, battery and location
	private static Feature drawSensor(Sensor sensor) throws IOException, InterruptedException {
		Feature sensor_feature = Feature.fromGeometry((Geometry) sensor.getCoordinates());

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
	private static Point closestSensor(List<Point> sensors, Point drone, List<Point> visitedSensors) {

		Point closest = Point.fromLngLat(MAX_LONGITUDE + 100, MAX_LATITUDE + 100);

		for (Point sensor : sensors) {
			if (getDistance(drone, sensor) <= getDistance(drone, closest) && !visitedSensors.contains(sensor)) {
				closest = sensor;
			}

		}

		return closest;
	}

	// returns the drone's current position
	private static Point currentPosition(List<Point> path) {
		return path.get(path.size() - 1);
	}

	// rounds the number to the nearest ten. 48 is rounded up to 50. 43 is rounded
	// down to 40.
	private static double nearestTen(double number) {
		return ((Math.round(number + 5) / 10) * 10);
	}

	// returns the longitude difference when drone is flying at angle
	private static double lngDifference(double angle) {

		double difference = Math.cos(Math.toRadians(angle)) * 0.0003;

		return difference;
	}

	// returns the latitude difference when drone is flying at angle
	private static double latDifference(double angle) {
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

	// gives the direction of the sensor with respect to the drone (north-west,
	// south-east,.... etc.)
	private static String sensorDirection(Point drone, Point sensor) {
		String direction = "";
		if (sensor.latitude() > drone.latitude() && sensor.longitude() > drone.longitude()) {
			direction = "north-east";
		} else if (sensor.latitude() > drone.latitude() && sensor.longitude() < drone.longitude()) {
			direction = "north-west";
		} else if (sensor.latitude() < drone.latitude() && sensor.longitude() > drone.longitude()) {
			direction = "south-east";
		} else if (sensor.latitude() < drone.latitude() && sensor.longitude() < drone.longitude()) {
			direction = "south-west";
		} else if (sensor.latitude() == drone.latitude() && sensor.longitude() < drone.longitude()) {
			direction = "west";
		} else if (sensor.latitude() == drone.latitude() && sensor.longitude() > drone.longitude()) {
			direction = "east";
		} else if (sensor.latitude() < drone.latitude() && sensor.longitude() == drone.longitude()) {
			direction = "south";
		} else if (sensor.latitude() > drone.latitude() && sensor.longitude() == drone.longitude()) {
			direction = "north";
		}
		return direction;
	}

	// calculates the angle the drone will have to fly to reach the sensor
	private static double findAngle(Point drone, Point sensor, String direction) {
		double lng_difference = Math.abs(sensor.longitude() - drone.longitude());
		double lat_difference = Math.abs(sensor.latitude() - drone.latitude());
		double basic_angle = 0;
		double angle = 0;

		basic_angle = nearestTen(Math.toDegrees(Math.atan(lat_difference / lng_difference)));

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

		if (angle == 360) {
			angle = 0;
		}

		return angle;

	}

	// checks if the drone is within the boundary. If it is crossing it, it alters
	// the path
	private static Point checkBoundary(Point new_point, Point drone) {
		// if the drone is about to cross the right boundary
		if (new_point.longitude() >= MAX_LONGITUDE) {
			new_point = Point.fromLngLat(drone.longitude() + lngDifference(90), drone.latitude() + latDifference(90));
		}

		// if the drone is about to cross the bottom boundary and is flying in a
		// south-east direction
		if (new_point.latitude() <= MIN_LATITUDE && drone.longitude() <= new_point.longitude()) {
			new_point = Point.fromLngLat(drone.longitude() + lngDifference(0), drone.latitude() + latDifference(0));
		}

		// if the drone is about to cross the bottom boundary and is flying in a
		// south-west direction
		if (new_point.latitude() <= MIN_LATITUDE && drone.longitude() >= new_point.longitude()) {
			new_point = Point.fromLngLat(drone.longitude() + lngDifference(0), drone.latitude() + latDifference(0));
		}

		// if the drone is about the top boundary and is flying in a north-east
		// direction
		if (new_point.latitude() >= MAX_LATITUDE && drone.longitude() >= new_point.longitude()) {
			new_point = Point.fromLngLat(drone.longitude() + lngDifference(180), drone.latitude() + latDifference(180));
		}

		// if the drone is about the top boundary and is flying in a north-west
		// direction
		if (new_point.latitude() >= MAX_LATITUDE && drone.longitude() < new_point.longitude()) {
			new_point = Point.fromLngLat(drone.longitude() + lngDifference(0), drone.latitude() + latDifference(0));
		}

		// if the drone is about to cross the left boundary
		if (new_point.longitude() <= MIN_LONGITUDE) {
			new_point = Point.fromLngLat(drone.longitude() + lngDifference(90), drone.latitude() + latDifference(90));
		}

		return new_point;

	}

	// this method returns the number of moves made by the drone on its journey. It
	// is merely for testing purposes.
	public int getMoves() {
		return move_counter;
	}

}