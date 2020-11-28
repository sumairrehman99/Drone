   
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
import com.mapbox.turf.TurfJoins;
import com.mapbox.turf.TurfMisc;
import com.mapbox.turf.models.LineIntersectsResult;
import com.mapbox.turf.models.LineIntersectsResult.Builder;





public class App 
{
	
    public static void main( String[] args ) throws IOException, InterruptedException 
    {
       
    	var day = args[0];
        var month = args[1];
        var year = args[2];
        var starting_latitude = Double.parseDouble(args[3]);
        var starting_longitude = Double.parseDouble(args[4]);
        
        Point starting_point = Point.fromLngLat(starting_longitude, starting_latitude);
        
        
        var line_points = new ArrayList<Point>();
        line_points.add(Point.fromLngLat(-3.192473, 55.946233));
        line_points.add(Point.fromLngLat(-3.184319, 55.946233));
        line_points.add(Point.fromLngLat(-3.184319, 55.942617));
        line_points.add(Point.fromLngLat(-3.192473, 55.942617));
        line_points.add(Point.fromLngLat(-3.192473, 55.946233));
        
        
        var lines_list = new ArrayList<List<Point>>();
        
        lines_list.add(line_points);
        
        
        
        Data data = new Data(day, month, year);
        
        
        Path path = new Path(starting_point, data);

        
        path.buildPath();
           
        var no_flys = data.getNoFly();
        var no_fly_lines = new ArrayList<LineString>();
        
        
        
        for (int i = 0; i < no_flys.size(); i++) {
        	no_fly_lines.add(no_flys.get(i).outer());
        }

        System.out.println(path.getMoves());
 
        var feature_list = path.getFeatures();
        
//        for (int i = 0; i < no_fly_lines.size(); i++) {
//        	feature_list.add(Feature.fromGeometry((Geometry)no_fly_lines.get(i)));
//        }
        
        
        
        LineString boundary = LineString.fromLngLats(line_points);
        
        feature_list.add(Feature.fromGeometry((Geometry) boundary));
        
        
//        //converting the geometries into features and adding them to feature_list
//        for (int y = 0; y < geometry_list.size(); y++) {
//        	feature_list.add(Feature.fromGeometry(geometry_list.get(y)));
//        }
        

        FeatureCollection collection = FeatureCollection.fromFeatures(feature_list);
        
        path.writeFlightPath(collection);
        //path.writeLogFile();
        
    }
// end of the main method


}


