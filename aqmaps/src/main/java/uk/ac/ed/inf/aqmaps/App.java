  
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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;
import com.mapbox.turf.TurfJoins;




public class App 
{

	private static final HttpClient client = HttpClient.newHttpClient();
	private static int moveCounter = 0;
	private static ArrayList<Point> visitedSensors = new ArrayList<Point>();
	private static ArrayList<Point> path = new ArrayList<Point>();
	private static HashMap<String, Polygon> noFlyMap = new HashMap<>();
	
	
    public static void main( String[] args ) throws IOException, InterruptedException
    {
       
    	var day = args[0];
        var month = args[1];
        var year = args[2];
        var starting_latitude = Double.parseDouble(args[3]);
        var starting_longitude = Double.parseDouble(args[4]);
        var seed = args[5];
        var port = args[6];
        

        var maps_request = HttpRequest.newBuilder().uri(URI.create("http://localhost/maps/" + year.toString() + "/" + month.toString() + "/" + day.toString() + "/air-quality-data.json")).build();
        var maps_response = client.send(maps_request, BodyHandlers.ofString());
           
        
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

        
        
        noFlyMap.put("appleton", no_fly_polygons.get(0));
	    noFlyMap.put("dht", no_fly_polygons.get(1));
	    noFlyMap.put("library", no_fly_polygons.get(2));
	    noFlyMap.put("forum", no_fly_polygons.get(3));
       
        System.out.println(noFlyMap.get("library"));
        
        
        
        var line_points = new ArrayList<Point>();
        line_points.add(Point.fromLngLat(-3.192473, 55.946233));
        line_points.add(Point.fromLngLat(-3.184319, 55.946233));
        line_points.add(Point.fromLngLat(-3.184319, 55.942617));
        line_points.add(Point.fromLngLat(-3.192473, 55.942617));
        line_points.add(Point.fromLngLat(-3.192473, 55.946233));
        
        
        var lines_list = new ArrayList<List<Point>>();
        
        lines_list.add(line_points);
        
        Polygon b = Polygon.fromLngLats(lines_list);
        
        
   
        
        
        Type mapsType = new TypeToken<ArrayList<Maps>>() {}.getType();
        
        // this list stores the location, reading and battery percentage of each sensor
        ArrayList<Maps> map_list = new Gson().fromJson(maps_response.body(), mapsType);
          
        
        // this list stores the locations as arrays split at "."
        var splitLocations = new ArrayList<String[]>();
        

        
        var sensorDetails = new ArrayList<Sensor>();
        
        
        for (int i = 0; i < map_list.size(); i++) {
        	sensorDetails.add(new Sensor(map_list.get(i).location, map_list.get(i).reading, map_list.get(i).battery));
        }
        
        
        
        
        
        var readings = new ArrayList<Double>();
        
        // Parsing the strings from readingsList as doubles and storing them in readings
    	for (int i = 0; i < sensorDetails.size(); i++) {
    		try {
    			readings.add(Double.parseDouble(sensorDetails.get(i).getReading()));
    		}
    		catch (NumberFormatException e) {
    			readings.add((double) -1);
    		}
    	}
       
        
        
        // splitting the locations at "."
        for (int i = 0; i < sensorDetails.size(); i++) {
        	splitLocations.add(sensorDetails.get(i).getLocation().split("\\."));
        }
        
        
        // this list stores the responses to the word request from the web server
        var words_response_list = new ArrayList<HttpResponse<String>>();
        
        for (int i = 0; i < splitLocations.size(); i++) {
        	var words_request = HttpRequest.newBuilder().uri(URI.create("http://localhost/words/" + splitLocations.get(i)[0] + "/" + splitLocations.get(i)[1] + "/" + splitLocations.get(i)[2] + "/details.json")).build();
        	words_response_list.add(client.send(words_request, BodyHandlers.ofString()));
        }
       
        
        
        
       
        // plotting the sensors on the map
        
        var positions_list = new ArrayList<Position>();		// stores the positions of all the sensors 
        var sensor_positions = new ArrayList<Point>();
        var geometry_list = new ArrayList<Geometry>();
        var featureList = new ArrayList<Feature>();
        
        
       
        
        
        for (int i = 0; i < words_response_list.size(); i++){
        	var words = new Gson().fromJson(words_response_list.get(i).body(), Words.class);
        	positions_list.add(new Position(words.coordinates.lng, words.coordinates.lat));
        }
        
        for (int p = 0; p < positions_list.size(); p++) {
        	sensor_positions.add(Point.fromLngLat(positions_list.get(p).getLongitude(), positions_list.get(p).getLatitude()));
        }
        
        
        for (int x = 0; x < sensor_positions.size(); x++) {
        	geometry_list.add((Geometry) sensor_positions.get(x));
        }
        
       
        
       
        path.add(Point.fromLngLat(starting_longitude, starting_latitude));
        
        
        Path route = new Path(sensor_positions, path.get(path.size() - 1), visitedSensors, path, moveCounter);
        
        
        
        route.buildPath();
       
        
       	
        
        
        
       	
        System.out.println(route.getMoves());
        System.out.println(visitedSensors.size());
        
        
        LineString flight_path = LineString.fromLngLats(path);
        LineString boundary = LineString.fromLngLats(line_points);
        geometry_list.add((Geometry) flight_path);
        geometry_list.add((Geometry) boundary);
        
        
        for (int y = 0; y < geometry_list.size(); y++) {
        	featureList.add(Feature.fromGeometry(geometry_list.get(y)));
        }
        
        
     // drawing the sensors on the map
        for (int i = 0; i < 33; i++) {
        	featureList.get(i).addStringProperty("location", sensorDetails.get(i).getLocation());
        	drawSensors(readings.get(i), sensorDetails.get(i).getBattery(), featureList.get(i));
        }
        

        

      
        FeatureCollection collection = FeatureCollection.fromFeatures(featureList);
        
        
        String geojson_file = collection.toJson();

        
        
        
        
        
        // Writing the readings to the file 
         try {
 			FileWriter fw = new FileWriter("readings-" + day + "-" + month + "-" + year + ".geojson");
 			PrintWriter pw = new PrintWriter(fw);
 			
 			pw.println(geojson_file);
 			pw.close();	
 		
 	}
 		catch (IOException e) {
 			System.out.println("error");
 		}
        
         
        // Writing the flightpath-DD-MM-YYYY.txt file 
//        try {
//        	FileWriter fw = new FileWriter("flightpath-" + day + "-" + month + "-" + year + ".txt");
//        	PrintWriter pw = new PrintWriter(fw);
//        	
//        	for (int i = 1; i <= move_counter; i++) {
//        		pw.println(i + "," + path_list.get(i - 1).longitude() + "," + path_list.get(i - 1).latitude() + "," + angles.get(i -1) + "," + path_list.get(i).longitude() + "," + path_list.get(i).latitude() + "," + sensor_names.get(i - 1));       		
//        	}
//        	pw.close();
//        }
//        catch (IOException e) {
//        	System.out.println("error");
//        }
        
        
    }
// end of the main method



    private static void drawSensors(double reading, double battery, Feature f) {
    	if (battery < 10) {
    		f.addStringProperty("rgb-string", "#000000");
	  		f.addStringProperty("marker-color", "#000000");
	  		f.addStringProperty("marker-symbol", "cross");
    	}
		if (reading == -1) {
	  		f.addStringProperty("rgb-string", "#000000");
	  		f.addStringProperty("marker-color", "#000000");
	  		f.addStringProperty("marker-symbol", "cross");
		}
		
		if (reading >= 0 && reading < 32) {
			f.addStringProperty("rgb-string", "#00ff00");
    		f.addStringProperty("marker-color", "#00ff00");
    		f.addStringProperty("marker-symbol", "lighthouse");
		}
		
		if (reading >= 32 && reading < 64) {
			f.addStringProperty("rgb-string", "#40ff00");
    		f.addStringProperty("marker-color", "#40ff00");
    		f.addStringProperty("marker-symbol", "lighthouse");
		}
		
		if (reading >= 64 && reading < 96) {
			f.addStringProperty("rgb-string", "#80ff00");
    		f.addStringProperty("marker-color", "#80ff00");
    		f.addStringProperty("marker-symbol", "lighthouse");
		}
		
		if (reading >= 96 && reading < 128) {
			f.addStringProperty("rgb-string", "#c0ff00");
    		f.addStringProperty("marker-color", "#c0ff00");
    		f.addStringProperty("marker-symbol", "lighthouse");
		}

		if (reading >= 128 && reading < 160) {
    		f.addStringProperty("rgb-string", "#ffc000");
    		f.addStringProperty("marker-color", "#ffc000");
    		f.addStringProperty("marker-symbol", "danger");
    	}
		
		if (reading >= 160 && reading < 192) {
    		f.addStringProperty("rgb-string", "#ff8000");
    		f.addStringProperty("marker-color", "#ff8000");
    		f.addStringProperty("marker-symbol", "danger");
		}
		
		if (reading >= 192 && reading < 224) {
    		f.addStringProperty("rgb-string", "#ff4000");
    		f.addStringProperty("marker-color", "#ff4000");
    		f.addStringProperty("marker-symbol", "danger");
    	}
    	if (reading >= 224 && reading < 256) {
    		f.addStringProperty("rgb-string", "#ff0000");
    		f.addStringProperty("marker-color", "#ff0000");
    		f.addStringProperty("marker-symbol", "danger");
    	}

    }   
    

   
    
    
}




