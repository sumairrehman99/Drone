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
	private ArrayList<HttpResponse<String>> words_response_list;
	
	public Details(String word_one, String word_two, String word_three) throws IOException, InterruptedException {
		this.word_one = word_one;
		this.word_two = word_two;
		this.word_three = word_three;
		var words_request = HttpRequest.newBuilder().uri(URI.create("http://localhost/words/" + word_one + "/" + word_two + "/" + word_three + "/details.json")).build();
    	words_response_list.add(client.send(words_request, BodyHandlers.ofString()));
	}
	
	
	public ArrayList<HttpResponse<String>> getDetailsList(){
		return words_response_list;
	}
	
}
