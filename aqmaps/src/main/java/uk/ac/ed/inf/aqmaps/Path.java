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

public class Path extends App {

	private static int move_counter = 0;
	private static ArrayList<Point> current_path; 			// stores all the points the drone visits on its journey
	private static ArrayList<Integer> angles;				// stores the angles that the drone moves
	private static ArrayList<String> sensor_names;			// stores the names of the sensors in the order they are visited
	private static Data data;
	private static final HttpClient CLIENT = HttpClient.newHttpClient();
	private static final int MOVE_LIMIT = 150;
	private static final int TOTAL_SENSORS = 33;
	private static final double MAX_LONGITUDE = -3.184319;
	private static final double MIN_LONGITUDE = -3.192473;
	private static final double MAX_LATITUDE = 55.946233;
	private static final double MIN_LATITUDE = 55.942617;
    
    


	
	public Path(ArrayList<Point> current_path, ArrayList<Integer> angles, Data data) {
		Path.current_path = current_path;	
		Path.angles = angles;
		Path.data = data;
		
		sensor_names = data.getSensorNames();
	}
	
	
	
	
	
	public static LineString buildPath(List<Point> visited_sensors) throws IOException, InterruptedException {
		// make sure to remove the parameter from this method when submitting
		
       //var visited_sensors = new ArrayList<Point>();     
       var sensor_positions = data.getSensorCoordinates();			
       var names = data.getSensorNames();       					// this list stores all the names of the sensors in the order that they are in sensor_positions
       var no_flys = getNoFly();	
       var no_fly_lines = new ArrayList<LineString>();
       final Point starting_point = current_path.get(0);
       
       for (Polygon pol : no_flys) {
    	   no_fly_lines.add(pol.outer());
       }
		
       // this list needs to be cleared since it already contains all the names of the sensors in order. 
       sensor_names.clear();
		
		// flying to the sensors
		while (visited_sensors.size() < TOTAL_SENSORS || move_counter < MOVE_LIMIT) {
			Point next_sensor = closestSensor(sensor_positions, currentPosition(current_path), visited_sensors);
			
			// if the first sensor is in range of the starting position of the drone
			if (inRange(next_sensor, currentPosition(current_path)) && move_counter == 0) {
				
				// move to a arbitrary location 90 degrees north. This is counted as a move.
				angles.add(90);
				Point new_point = Point.fromLngLat(starting_point.longitude() + lngDifference(90), starting_point.latitude() + latDifference(90));
				current_path.add(new_point);
				sensor_names.add(null);
				move_counter++;
				
				// return to the starting location. The drone is now in range to take a reading, after having made a move.
				double new_angle = nearestTen(findAngle(currentPosition(current_path), starting_point, sensorDirection(currentPosition(current_path), starting_point)));
				angles.add((int)new_angle);
				current_path.add(starting_point);
				sensor_names.add(names.get(sensor_positions.indexOf(next_sensor)));
				visited_sensors.add(next_sensor);
				move_counter++;
				continue;
			}
			
			
			// if the first sensor isn't in range of the drone's starting point
			
			double angle = nearestTen(findAngle(currentPosition(current_path), next_sensor, sensorDirection(currentPosition(current_path), next_sensor)));
			angles.add((int) angle);
			Point new_point = Point.fromLngLat(currentPosition(current_path).longitude() + lngDifference(angle), currentPosition(current_path).latitude() + latDifference(angle));
			new_point = checkBoundary(new_point, currentPosition(current_path));
			
			

			
			current_path.add(new_point);

			// if the sensor is in range of the drone, add it to visited_sensors and add the name to sensor_names
			if (inRange(currentPosition(current_path), next_sensor)) {
				sensor_names.add(names.get(sensor_positions.indexOf(next_sensor)));
				visited_sensors.add(next_sensor);
				move_counter++;
			}
			else {
				sensor_names.add(null);
				move_counter++;
			}
			
			
			// this is added in order to terminate the code in case the drone goes in an infinite loop
			if (move_counter == MOVE_LIMIT || visited_sensors.size() == TOTAL_SENSORS) {
				break;
			}
		}
		
		
		
		
		// returning to the starting position
		while (move_counter < MOVE_LIMIT) {
	    	   double angle = findAngle(currentPosition(current_path), starting_point, sensorDirection(currentPosition(current_path), starting_point));
	    	   angles.add((int) angle);
	    	   Point new_point = Point.fromLngLat(currentPosition(current_path).longitude() + lngDifference(angle), currentPosition(current_path).latitude() + latDifference(angle));	    	   
	    	   
	    	   // once the drone is close enough to the starting position, stop
	    	   if (getDistance(starting_point, currentPosition(current_path)) < 0.0003) {
	    		   break;
	    	   }
	    	   
	    	   current_path.add(new_point);
	    	   sensor_names.add(null);
	    	   move_counter++;
	       }

		
		LineString flight_path = LineString.fromLngLats(current_path);
		
		return flight_path;
	}
	
	

		
	public static ArrayList<Integer> getAngles() {
    	return angles;
    }
	
	
	public static ArrayList<Polygon> getNoFly() throws IOException, InterruptedException {
		
		var no_fly_request = HttpRequest.newBuilder().uri(URI.create("http://localhost/buildings/no-fly-zones.geojson")).build();
        var no_fly_response = CLIENT.send(no_fly_request, BodyHandlers.ofString());
        
        FeatureCollection no_fly_collection = FeatureCollection.fromJson(no_fly_response.body());        
        List<Feature> no_fly_list = no_fly_collection.features();
        List<Geometry> no_fly_geometrys = new ArrayList<>();
        
        for (Feature f : no_fly_list) {
        	no_fly_geometrys.add(f.geometry());
        }
        
        
        var no_fly_polygons = new ArrayList<Polygon>();
        
        for (Geometry g : no_fly_geometrys) {
        	if (g instanceof Polygon) {
        		no_fly_polygons.add((Polygon)g);
        	}
        }
		
		return no_fly_polygons;
		
	}
	
	
	
	
	
	
	// returns the Euclidean distance between two points
	private static double getDistance(Point p1, Point p2) {
		
		double sum = Math.pow((p1.longitude() - p2.longitude()), 2) + Math.pow((p1.latitude() - p2.latitude()), 2);
		
		double distance = Math.sqrt(sum);
		
		return distance;
    }
	
	// returns the closest sensor to the drone that hasn't been visited yet
	private static Point closestSensor(List<Point> sensors, Point drone, List<Point> visitedSensors) {
    	
    	Point closest = Point.fromLngLat(60, -4);
    	
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

	// rounds the number to the nearest ten. 48 is rounded up to 50. 43 is rounded down to 40.
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
    	}
    	else {
    		return false;
    	}
    }

    // gives the direction of the sensor with respect to the drone (north-west, south-east,.... etc.)
    private static String sensorDirection(Point drone, Point sensor) {
    	String direction = "";
    	if (sensor.latitude() > drone.latitude() && sensor.longitude() > drone.longitude()) {
    		direction = "north-east";
    	}
    	else if (sensor.latitude() > drone.latitude() && sensor.longitude() < drone.longitude()) {
    		direction = "north-west";
    	}
    	else if (sensor.latitude() < drone.latitude() && sensor.longitude() > drone.longitude()) {
    		direction = "south-east";
    	}
    	else if (sensor.latitude() < drone.latitude() && sensor.longitude() < drone.longitude()) {
    		direction = "south-west";
    	}
    	else if (sensor.latitude() == drone.latitude() && sensor.longitude() < drone.longitude()) {
    		direction = "west";
    	}
    	else if (sensor.latitude() == drone.latitude() && sensor.longitude() > drone.longitude()) {
    		direction = "east";
    	}
    	else if (sensor.latitude() < drone.latitude() && sensor.longitude() == drone.longitude()) {
    		direction = "south";
    	}
    	else if (sensor.latitude() > drone.latitude() && sensor.longitude() == drone.longitude()) {
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
    	
    	
    	
    	
    	switch(direction) {
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
    	
    		
    	return angle;
    	
    }
    
    // checks if the drone is within the boundary. If it is crossing it, it alters the path
    private static Point checkBoundary(Point new_point, Point drone) {
	   // if the drone is about to cross the right boundary
	   if (new_point.longitude() >= MAX_LONGITUDE) {
		  new_point = Point.fromLngLat(drone.longitude() + lngDifference(90), drone.latitude() + latDifference(90));
	   }
	   
	   // if the drone is about to cross the bottom boundary
	   if (new_point.latitude() <= MIN_LATITUDE) {
		   new_point = Point.fromLngLat(drone.longitude() + lngDifference(0), drone.latitude() + latDifference(0));
	   }
	   
	   // if the drone is about the top boundary and is flying in a north-east direction
	   if (new_point.latitude() >= MAX_LATITUDE && drone.longitude() >= new_point.longitude()) {
  		  new_point = Point.fromLngLat(drone.longitude() + lngDifference(180), drone.latitude() + latDifference(180));
	   }
	   
	   // if the drone is about the top boundary and is flying in a north-west direction
	   if (new_point.latitude() >= MAX_LATITUDE && drone.longitude() < new_point.longitude()) {
		   new_point = Point.fromLngLat(drone.longitude() + lngDifference(0), drone.latitude() + latDifference(0));
	   }
	   
	   
	   // if the drone is about to cross the left boundary
	   if (new_point.longitude() <= MIN_LONGITUDE) {
		   new_point = Point.fromLngLat(drone.longitude() + lngDifference(90), drone.latitude() + latDifference(90));
	   }
	   
	   return new_point;
	   
   }
    
    
    private static String pointDirection(Point p1, Point p2) {
    	String direction = "";
    	
    	if (p1.latitude() <= p2.latitude() && p1.longitude() <= p2.longitude()) {
    		direction = "north-east";
    	}
    	if (p1.latitude() <= p2.latitude() && p1.longitude() >= p2.longitude()) {
    		direction = "north-west";
    	}
    	if (p1.latitude() >= p2.latitude() && p1.longitude() <= p2.longitude()) {
    		direction = "south-east";
    	}
    	if (p1.latitude() >= p2.latitude() && p1.longitude() >= p2.longitude()) {
    		direction = "south-west";
    	}
    	return direction;
    }
    
    
    
    public static int getMoves() {
    	return move_counter;
    }
    
    public static int getSensors(List<Point> visitedSensors) {
    	return visitedSensors.size();
    }
    
    // rounds to 6 decimal places
    private static double sixdp(double x) {
    	return (Math.round(x * 1000000.0)/1000000.0);
    }
    
   

    // this method writes a feature collection to a geojson file
	public static void writeReadings(FeatureCollection collection, String day, String month, String year) {
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
	public static void writeFlightPath(String day, String month, String year, ArrayList<Integer> angles) {
		try {
			FileWriter fw = new FileWriter("flightpath-" + day + "-" + month + "-" + year + ".txt");
			PrintWriter pw = new PrintWriter(fw);
					
			for (int i = 1; i <= move_counter; i++) {
				pw.println(i + "," + current_path.get(i-1).longitude() + "," + current_path.get(i-1).latitude() + "," + angles.get(i-1) + "," + current_path.get(i).longitude() + "," + current_path.get(i).latitude() + "," + sensor_names.get(i-1));
			}
			pw.close();
		}
		catch (IOException e) {
			System.out.println("error");
		}
	}
	

}
