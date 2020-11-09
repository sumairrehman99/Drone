package uk.ac.ed.inf.aqmaps;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class Data {

	private static final HttpClient client = HttpClient.newHttpClient();
	private String day;
	private String month;
	private String year;
	private ArrayList<Maps> data_list;			 // this list stores the location, reading and battery percentage of each sensor

	public Data(String day, String month, String year) throws IOException, InterruptedException {
		this.day = day;
		this.month = month;
		this.year = year;
		
		var data_request = HttpRequest.newBuilder().uri(URI.create("http://localhost/maps/" + year + "/" + month + "/" + day + "/air-quality-data.json")).build();
        var data_response = client.send(data_request, BodyHandlers.ofString());
        
        Type mapsType = new TypeToken<ArrayList<Maps>>() {}.getType();
        
       
        data_list = new Gson().fromJson(data_response.body(), mapsType);
	}
	
	
	public ArrayList<Maps> getDataList(){
		return data_list;
	}
	
	
	
	
	
	
	
	
	
	
	
}
