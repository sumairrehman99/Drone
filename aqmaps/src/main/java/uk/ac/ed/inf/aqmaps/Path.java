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
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;

public class Path {

	private static  List<Point> sensor_positions;
	private static int move_counter = 0;
	private static List<Point> path; 
	private static HashMap<String, Polygon> noFlyMap;
	private static List<Polygon> no_fly_polygons;
	private static final HttpClient client = HttpClient.newHttpClient();
	
	
    
    
    


	
	public Path(List<Point> sensor_positions, List<Point> path) {
		Path.sensor_positions = sensor_positions;
		Path.path = path;	
	}
	
	
	
	
	
	public static LineString buildPath(List<Point> visitedSensors) throws IOException, InterruptedException {
		
		var no_flys = getNoFly();
		
		
		// flying to the sensors
		while (visitedSensors.size() < 33 || move_counter < 150) {
			Point next_sensor = closestSensor(sensor_positions, dronePosition(path), visitedSensors);
			if (inRange(next_sensor, dronePosition(path))) {
				visitedSensors.add(next_sensor);
				continue;
			}
			double angle = nearestTen(findAngle(dronePosition(path), next_sensor, sensorDirection(dronePosition(path), next_sensor)));
			Point new_point = Point.fromLngLat(dronePosition(path).longitude() + lngDifference(angle), dronePosition(path).latitude() + latDifference(angle));
			new_point = checkBoundary(new_point, dronePosition(path));
			
			
			if (new_point.latitude() > 55.946233) {
				System.out.println(flightDirection(path, new_point));
			}
			
			path.add(new_point);

			if (inRange(dronePosition(path), next_sensor)) {
				visitedSensors.add(next_sensor);
				move_counter++;
			}
			else {
				move_counter++;
			}
			
			if (move_counter == 150 || visitedSensors.size() == 33) {
				break;
			}
		}
		
		// returning to the starting position
		while (move_counter < 150) {
	    	   Point start = path.get(0);
	    	   double angle = findAngle(dronePosition(path), start, sensorDirection(dronePosition(path), start));
	    	   Point new_point = Point.fromLngLat(dronePosition(path).longitude() + lngDifference(angle), dronePosition(path).latitude() + latDifference(angle));

	    	   if (getDistance(start, dronePosition(path)) < 0.0003) {
	    		   break;
	    	   }
	    	   
	    	   path.add(new_point);
	    	   move_counter++;
	       }
		
		
		
		
		LineString flight_path = LineString.fromLngLats(path);
		

		
		return flight_path;
	}
	
	
	
	
	
	
	public static void testPath(List<Point> path) throws IOException, InterruptedException {
		var no_flys = getNoFly();
		for (Polygon p : no_flys) {
			for (Point x : path) {
				if (TurfJoins.inside(x, p)) {
					System.out.println(true);
				}
			}
	}	
	}
	
	
	private static ArrayList<Point> checkNoFly(LineString line) throws IOException, InterruptedException {
		ArrayList<Polygon> no_flys =  getNoFly();
		var trespass = new ArrayList<Point>();
		var allCoords = TurfMeta.coordAll(line);
		
		for (Point p : allCoords) {
			for (Polygon pol : no_flys) {
				if (TurfJoins.inside(p, pol)) {
					trespass.add(p);
				}
			}
		}
		return trespass;
	}
	
	
	
	
	
	
	
	private static ArrayList<Polygon> getNoFly() throws IOException, InterruptedException {
		
		var no_fly_request = HttpRequest.newBuilder().uri(URI.create("http://localhost/buildings/no-fly-zones.geojson")).build();
        var no_fly_response = client.send(no_fly_request, BodyHandlers.ofString());
        
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
	
	
	// returns the euclidean distance between two points
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
	private static Point dronePosition(List<Point> path) {
    	return path.get(path.size() - 1);
    }

	// rounds the number to the nearest ten
	private static double nearestTen(double number) {
    	return ((Math.round(number + 5) / 10) * 10);
    }

	// returns the longitude difference when drone is flying at angle
    private static double lngDifference(double angle) {
    	double inRad = Math.toRadians(angle);
    	
    	double difference = Math.cos(inRad) * 0.0003;
    	
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
	   if (new_point.longitude() >= -3.184319) {
		  new_point = Point.fromLngLat(drone.longitude() + lngDifference(90), drone.latitude() + latDifference(90));
	   }
	   
	   // if the drone is about to cross the bottom boundary
	   if (new_point.latitude() <= 55.942617) {
		   new_point = Point.fromLngLat(drone.longitude() + lngDifference(0), drone.latitude() + latDifference(0));
	   }
	   
	   // if the drone is about the top boundary
	   if (new_point.latitude() >= 55.946233 && drone.longitude() >= new_point.longitude()) {
  		  new_point = Point.fromLngLat(drone.longitude() + lngDifference(180), drone.latitude() + latDifference(180));
	   }
	   
	   
	   if (new_point.latitude() >= 55.946233 && drone.longitude() == new_point.longitude()) {
	  		  new_point = Point.fromLngLat(drone.longitude() + lngDifference(180), drone.latitude() + latDifference(180));
	   }
	   
	   if (new_point.latitude() >= 55.946233 && drone.longitude() <= new_point.longitude()) {
		   new_point = Point.fromLngLat(drone.longitude() + lngDifference(0), drone.latitude() + latDifference(0));
	   }
	   
	   
	   // if the drone is about to cross the left boundary
	   if (new_point.longitude() <= -3.192473) {
		   new_point = Point.fromLngLat(drone.longitude() + lngDifference(90), drone.latitude() + latDifference(90));
	   }
	   
	   return new_point;
	   
   }
    
    
    // gives the direction the drone is flying
    private static String flightDirection(List<Point> path, Point new_point) {
    	String direction = "";
    	
    	if (dronePosition(path).latitude() >= new_point.latitude() && dronePosition(path).longitude() >= new_point.longitude()) {
    		direction = "south-west";
    	}
    	if (dronePosition(path).latitude() >= new_point.latitude() && dronePosition(path).longitude() <= new_point.longitude()) {
    		direction = "south-east";
    	}
    	if (dronePosition(path).latitude() <= new_point.latitude() && dronePosition(path).longitude() >= new_point.longitude()) {
    		direction = "north-west";
    	}
    	if (dronePosition(path).latitude() <= new_point.latitude() && dronePosition(path).longitude() <= new_point.longitude()) {
    		direction = "north-east";
    	}
    	if (dronePosition(path).latitude() == new_point.latitude() && dronePosition(path).longitude() < new_point.longitude()) {
    		direction = "west";
    	}
    	if (dronePosition(path).latitude() == new_point.latitude() && dronePosition(path).longitude() > new_point.longitude()) {
    		direction = "east";
    	}
    	if (dronePosition(path).latitude() < new_point.latitude() && dronePosition(path).longitude() == new_point.longitude()) {
    		direction = "north";
    	}
    	if (dronePosition(path).latitude() > new_point.latitude() && dronePosition(path).longitude() == new_point.longitude()) {
    		direction = "south";
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
	

}
