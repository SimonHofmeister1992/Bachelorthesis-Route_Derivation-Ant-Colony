package bins.management.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.util.JSONPObject;

import bins.management.Config;
import bins.management.entity.DistanceMatrixEntry;

public class GoogleDistanceApiConsumer implements IDistanceMatrixApiConsumer {

	private Config config;
	private String apiKey;
	private final String FIX_PART_GOOGLE_DISTANCE_API="https://maps.googleapis.com/maps/api/distancematrix/json?";
	
	public GoogleDistanceApiConsumer() {
		config = new Config();
		setApiKey(config.getProperty("googleApiKey"));
	}
	
	
	public DistanceMatrixEntry getDistanceByDistanceMatrixEntry(DistanceMatrixEntry dme) {
		
		String distanceAsString;
		Long distance=Long.MAX_VALUE;
		
		RestTemplate rt = new RestTemplate();
		
		String url = FIX_PART_GOOGLE_DISTANCE_API 
				+ "origins=" + dme.getLongLat1OrAddress1().replace(" ", "+") 
				+ "&destinations=" + dme.getLongLat2OrAddress2().replace(" ", "+")
				+ "&key=" + this.getApiKey();
		
		String responseEntity = rt.getForObject(url, String.class);
		
		String[] split = responseEntity.split("\n");
		
		for(int i=0; i<split.length;i++) {
//			System.out.println(i+": " + split[i]);
			if(split[i].contains("distance")) {
				i=i+2;
				distanceAsString = split[i].replace(" ", "").replace("\"", "").replace("value:", "");
				distance=Long.valueOf(distanceAsString);
				break;
			}
		}
		
		dme.setDistance(distance);
		return dme;
	}
	
	private String getApiKey() {
		return apiKey;
	}

	private void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}
	
}
