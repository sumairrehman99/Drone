package uk.ac.ed.inf.aqmaps;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;

public class Details {
	private static final HttpClient client = HttpClient.newHttpClient();
	private String word_one;
	private String word_two;
	private String word_three;
	private static ArrayList<HttpResponse<String>> detailsList;
	
	public Details (String word_one, String word_two, String word_three) throws IOException, InterruptedException {
		var detailsRequest = HttpRequest.newBuilder().uri(URI.create("http://localhost/words/" + word_one + "/" + word_two+ "/" + word_three + "/details.json")).build();
      	detailsList.add(client.send(detailsRequest, BodyHandlers.ofString()));
	}
	
	public static ArrayList<HttpResponse<String>> getDetails(){
		return detailsList;
	}


}
