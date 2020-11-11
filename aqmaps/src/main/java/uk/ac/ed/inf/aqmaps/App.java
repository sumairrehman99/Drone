  
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
import java.util.List;

import com.google.gson.Gson;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;





public class App 
{

	private static final HttpClient client = HttpClient.newHttpClient();
	private static ArrayList<Point> visitedSensors = new ArrayList<Point>();
	private static ArrayList<Point> path = new ArrayList<Point>();
	private static ArrayList<Geometry> geometry_list = new ArrayList<>();
    private static ArrayList<Feature> featureList = new ArrayList<>();
	
    public static void main( String[] args ) throws IOException, InterruptedException
    {
       
    	var day = args[0];
        var month = args[1];
        var year = args[2];
        var starting_latitude = Double.parseDouble(args[3]);
        var starting_longitude = Double.parseDouble(args[4]);
       

       
           

        
        var line_points = new ArrayList<Point>();
        line_points.add(Point.fromLngLat(-3.192473, 55.946233));
        line_points.add(Point.fromLngLat(-3.184319, 55.946233));
        line_points.add(Point.fromLngLat(-3.184319, 55.942617));
        line_points.add(Point.fromLngLat(-3.192473, 55.942617));
        line_points.add(Point.fromLngLat(-3.192473, 55.946233));
        
        
        var lines_list = new ArrayList<List<Point>>();
        
        lines_list.add(line_points);
        
        Polygon b = Polygon.fromLngLats(lines_list);
 
        
        
        Data data = new Data(day, month, year);
        
        var sensorDetails = data.getSensors();        
        
        var readings = data.getSensorReadings(sensorDetails);
        
        var splitLocations = new ArrayList<String[]>();
        
        
        
        
        
        // splitting the locations at "."
        for (int i = 0; i < sensorDetails.size(); i++) {
      	  splitLocations.add(sensorDetails.get(i).getLocation().split("\\."));
        }
    
        
//    this list stores the responses to the word request from the web server
      var detailsList = new ArrayList<HttpResponse<String>>();
      
      
      // pulling the details from the web server
      for (int i = 0; i < splitLocations.size(); i++) {
    	  var detailsRequest = HttpRequest.newBuilder().uri(URI.create("http://localhost/words/" + splitLocations.get(i)[0] + "/" + splitLocations.get(i)[1] + "/" + splitLocations.get(i)[2] + "/details.json")).build();
    	  detailsList.add(client.send(detailsRequest, BodyHandlers.ofString()));
      }
      	
        
        
        // plotting the sensors on the map
        
        var coordinates_list = new ArrayList<Coordinates>();	// stores the coordinates of all the sensors 
        var sensor_positions = new ArrayList<Point>();			
        
        
        
       
        
        
        for (int i = 0; i < detailsList.size(); i++){
        	var words = new Gson().fromJson(detailsList.get(i).body(), Words.class);
        	coordinates_list.add(new Coordinates(words.coordinates.lng, words.coordinates.lat));
        }
        
        for (int p = 0; p < coordinates_list.size(); p++) {
        	sensor_positions.add(Point.fromLngLat(coordinates_list.get(p).getLongitude(), coordinates_list.get(p).getLatitude()));
        }
        
        
        for (int x = 0; x < sensor_positions.size(); x++) {
        	geometry_list.add((Geometry) sensor_positions.get(x));
        }
        
       
        
       
        path.add(Point.fromLngLat(starting_longitude, starting_latitude));
        
        
        new Path(sensor_positions, path.get(path.size() -1), visitedSensors, path);
        
        
        
        Path.buildPath();
       
        
        
        
       	
        System.out.println(Path.getMoves());
        System.out.println(Path.getSensors());
        
        
        LineString flight_path = LineString.fromLngLats(path);
        LineString boundary = LineString.fromLngLats(line_points);
        geometry_list.add((Geometry) flight_path);
        geometry_list.add((Geometry) boundary);
        
        
        for (int y = 0; y < geometry_list.size(); y++) {
        	featureList.add(Feature.fromGeometry(geometry_list.get(y)));
        }
        
        
        data.drawSensors(readings, sensorDetails, featureList);
        
    
      
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


}




