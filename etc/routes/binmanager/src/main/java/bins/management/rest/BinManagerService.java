package bins.management.rest;

import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import bins.management.db.BinSelektor;
import bins.management.db.BinUpdateOrInserter;
import bins.management.entity.Bin;


@RestController
public class BinManagerService {
			
		// if someone uses one of the webservices directly via browser replace this by the csrf-token-concept
		@Bean
	    public WebMvcConfigurer corsConfigurer() {
	        return new WebMvcConfigurer() {
	            @Override
	            public void addCorsMappings(CorsRegistry registry) {
	                registry.addMapping("/**").allowedOrigins("*");
	            }
	        };
	    }
		
		/*
		 * purpose: inserting new bins or updating their information. Deleting information is not possible here (Null-Values are ignored)
		 * input: list of bins, and information about the bin-group as the organizationid, fractionid and region
		 * 		  dates: // month January=0, December=11 !!!
		 * output:	map containing the bins and the information if the insert/update progress worked
		 */
	    @GetMapping("/bin-manager/insert-or-update")
	    public ResponseEntity<HashMap<Long, Boolean>> insertOrUpdateBins(@RequestParam(value="orgId") long orgId,
	    		@RequestParam(value="fractionId") long fractionId,
	    		@RequestParam(value="regionId") long regionId,
	    		@RequestParam(value="bins") String b) {
	    	
	    	final BinUpdateOrInserter buoi;
	    	HashMap<Long, Boolean> updatingSuccessfulMap = new HashMap<Long, Boolean>();
	    	List<Bin> bins = convertStringToBinList(b);
	    	
			try {
				buoi = new BinUpdateOrInserter(orgId, fractionId, regionId);
		    	bins.forEach(new Consumer<Bin>(){
					@Override
					public void accept(Bin bin) {
						boolean isSuccessful = buoi.updateOrInsert(bin);
						if(!isSuccessful) {
							updatingSuccessfulMap.put(bin.getBinid(), Boolean.FALSE);
						}
						else {
							updatingSuccessfulMap.put(bin.getBinid(), Boolean.TRUE);
						}
					}
		    	});  
			} catch (SQLException e) {
			}
 	
	    	return new ResponseEntity<HashMap<Long, Boolean>>(updatingSuccessfulMap,HttpStatus.OK);
	    }

	    // month start count with 0!
	    private List<Bin> convertStringToBinList(String b) {
	    	List<Bin> binList = new ArrayList<Bin>();
	    	Bin bin;
	    	b = b.replace("]]","]");
	    	b= b.replace("[[", "[");
	    	String[] bns = b.split(";");
	    	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss");
	    	for(String s : bns) {
	    		bin = new Bin();
	    		s = s.replace("[", "").replace("]", "");
	    		if(s.equals("")) break;
	    		String[] map = s.split(",");
	    		for(String entry : map) {
	    			String[] submap = entry.split(":");
	    			String key=submap[0];
	    			String value=submap[1];
	    			
	    			if(key.equals("binid")) bin.setBinid(Long.valueOf(value.replace("\"", "")));
	    			else if(key.equals("filllevel")) bin.setFillLevel(Integer.valueOf(value.replace("\"", "")));
	    			else if(key.equals("lastvalidfilllevel")) bin.setLastValidFillLevel(Integer.valueOf(value.replace("\"", "")));
	    			else if(key.equals("filldate"))
						try {
							String[] splittedDate = value.split("-");
							int month = Integer.valueOf(splittedDate[1])+1;
							String temp = "";
							for(int i = 0; i < splittedDate.length; i++) {
								if(i != 0) temp += "-";
								if(i != 1) temp += splittedDate[i];
								else temp += month;
							}
							value = temp;							
							bin.setFillDate(sdf.parse(value));
						} catch (ParseException e) {
						}
	    			else if(key.equals("lastvalidfilldate"))
						try {
							String[] splittedDate = value.split("-");
							int month = Integer.valueOf(splittedDate[1])+1;
							String temp = "";
							for(int i = 0; i < splittedDate.length; i++) {
								if(i != 0) temp += "-";
								if(i != 1) temp += splittedDate[i];
								else temp += month;
							}
							value = temp;								
							bin.setLastValidFillDate(sdf.parse(value));
						} catch (ParseException e) {
						}
	    			else if(key.equals("latitude")) bin.setLatitude(value.replace("\"", ""));
	    			else if(key.equals("longitude")) bin.setLongitude(value.replace("\"", ""));
	    			else if(key.equals("geoid")) bin.setGeoId(value.replace("\"", ""));
	    			else if(key.equals("street")) bin.setStreet(value.replace("\"", ""));
	    			else if(key.equals("housenr")) bin.setHouseNr(value.replace("\"", ""));
	    			else if(key.equals("postcode")) bin.setPostCode(value.replace("\"", ""));
	    			else if(key.equals("city")) bin.setCity(value.replace("\"", ""));
	    			else if(key.equals("country")) bin.setCountryName(value.replace("\"", ""));
	    			
	    		}
	    		binList.add(bin);
	    	}
	    	
			return binList;
		}

		@GetMapping("/bin-manager/request-bins")
	    public ResponseEntity<List<Bin>> getBins(@RequestParam(value="orgId") long orgId,
	    		@RequestParam(value="fractionId") long fractionId,
	    		@RequestParam(value="regionId") long regionId,
	    		@RequestParam(value="infoType", defaultValue=IInfoType.BIN_NUM) String infoType) {
	    	
	    	List<Bin> bins = null;
	    	List<String> validInfoTypes = new ArrayList<String>();
	    	validInfoTypes.add(IInfoType.BIN_NUM);
	    	validInfoTypes.add(IInfoType.BINS);
	    	validInfoTypes.add(IInfoType.BINS_WITH_LOCS);
	    	validInfoTypes.add(IInfoType.FULL);
	    	validInfoTypes.add(IInfoType.LOCATIONS_ONLY);
	    	
	    	for(int i = 0; i < validInfoTypes.size(); i++) {
	    		
	    		if(validInfoTypes.get(i).equals(infoType)) {
	    			bins = requestBins(orgId, fractionId, regionId, infoType);
	    			return new ResponseEntity<List<Bin>>(bins, HttpStatus.OK);
	    		}	    		
	    	}	   
	    	return new ResponseEntity<List<Bin>>(HttpStatus.BAD_REQUEST);
	    }
    	
	    @GetMapping("/bin-manager/empty-bins")
	    public boolean emptyBins(@RequestParam(value="orgId") long orgId,
	    		@RequestParam(value="fractionId") long fractionId,
	    		@RequestParam(value="regionId") long regionId){
	    	BinUpdateOrInserter buoi;
			try {
				buoi = new BinUpdateOrInserter(orgId, fractionId, regionId);
				buoi.emptyBins();
				return true;
			} catch (SQLException e) {
				e.printStackTrace();
			}
	    	return false;
	    }
	    
	    public List<Bin> requestBins(long orgId, long fractionId, long regionId, String infoType) {
	    	BinSelektor bs = new BinSelektor(orgId, fractionId, regionId);

	    	List<Bin> bins = null;
	    	
	    	if(infoType.equals(IInfoType.FULL)) {
	    		bins = bs.getBinsWithFullInformation();
	    	}
	    	else if(infoType.equals(IInfoType.BINS_WITH_LOCS)) {
	    		bins = bs.getBinsWithLocationInformation();
	    	}	    	
	    	else if(infoType.equals(IInfoType.BINS)) {
	    		bins = bs.getBinsWithBasicInformation();
	    	}

	    	else if(infoType.equals(IInfoType.BIN_NUM)) {
	    		bins = bs.getBinsWithoutInformation();
	    	}
	    	
	    	else if(infoType.equals(IInfoType.LOCATIONS_ONLY)) {
	    		bins = bs.getOnlyLocations();
	    	}
	    	
	    	return bins;
	    }
}
