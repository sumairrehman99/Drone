  
package uk.ac.ed.inf.aqmaps;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;





public class App 
{
	private static ArrayList<Point> visited_sensors = new ArrayList<>();
	private static ArrayList<Point> current_path = new ArrayList<>();	
	private static ArrayList<Integer> angles = new ArrayList<>();
	private static ArrayList<Geometry> geometry_list = new ArrayList<>();		// stores the geometries of the sensors and the flight path
    private static ArrayList<Feature> feature_list = new ArrayList<>();
	
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
        
        var sensor_details = data.getSensorDetails();        
        
        var readings = data.getSensorReadings(sensor_details);
        
        var names = data.getSensorNames();

    
        // this list stores the What3Words details
        ArrayList<HttpResponse<String>> details_list =  data.getDetails();       
        
        
        // plotting the sensors on the map
       var sensor_positions = data.getSensorCoordinates(details_list);
        
        for (int x = 0; x < sensor_positions.size(); x++) {
        	geometry_list.add((Geometry) sensor_positions.get(x));
        }
        

       
        current_path.add(Point.fromLngLat(starting_longitude, starting_latitude));
        
        
        new Path(sensor_positions, current_path, angles, names);

        
        LineString flight_path = Path.buildPath(visited_sensors, day, month, year);
        
       	
        
        
        
        System.out.println(Path.getMoves());
        System.out.println(Path.getSensors(visited_sensors));
        
        LineString boundary = LineString.fromLngLats(line_points);
        geometry_list.add((Geometry) flight_path);
        geometry_list.add((Geometry) boundary);
        
        
        for (int y = 0; y < geometry_list.size(); y++) {
        	feature_list.add(Feature.fromGeometry(geometry_list.get(y)));
        }
        
        data.drawSensors(readings, sensor_details, feature_list);

        FeatureCollection collection = FeatureCollection.fromFeatures(feature_list);
        
        Path.writeReadings(collection, day, month, year);
        //Path.writeFlightPath(day, month, year, angles);
        
    }
// end of the main method


}




