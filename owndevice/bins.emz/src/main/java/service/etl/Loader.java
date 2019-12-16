package service.etl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;

import org.springframework.web.client.RestTemplate;

import service.Bin;
import service.Config;

public class Loader {

	private Config config;
	private String emptyBinUrl;
	private String insertOrUpdateBinUrl;
	private List<Bin> goodBeans;
	private List<Bin> badBeans=new ArrayList();
	
	
	// consume BinManagerService - bins.management
	/*
	 * public HashMap<Long, Boolean> insertOrUpdateBins(@RequestParam(value="orgId") long orgId,
	    		@RequestParam(value="fraktionId") long fraktionId,
	    		@RequestParam(value="regionId") Long regionId,
	    		@RequestParam(value="bins") List<Bin> bins) 
	 * 
	 */
	
	public Loader(ITransformer t, Long orgId, Long fractionId, Long regionId) {
		config = new Config();
		String url = config.getProperty("pathToBinManagerService");
		String serviceInsertOrUpdateBin = config.getProperty("insertOrUpdateBinMethod");
		String serviceEmptyBin = config.getProperty("emptyAllBinsMethod");
		
		
		emptyBinUrl = url + "/" + serviceEmptyBin + "?orgId=" + orgId + "&fractionId=" + fractionId + "&regionId=" + regionId;
		
		// emptyBinUrl absenden
		
		insertOrUpdateBinUrl = url + "/" + serviceInsertOrUpdateBin + "?orgId=" + orgId 
				+ "&fractionId=" + fractionId 
				+ "&regionId=" + regionId 
				+ "&bins=" + "["
				;
		setGoodBeans(t.getGoodBeans());
	}
	
	
	public List<Long> load() {
		ArrayList<Long> badBins = new ArrayList<Long>();
		String url;
		RestTemplate rt = new RestTemplate();
		rt.getForObject(emptyBinUrl, String.class); // returns void
		HashMap<String,Boolean> beans=new HashMap<String, Boolean>();
		
		StringBuffer sb;
		Calendar gc1 = GregorianCalendar.getInstance();
		Calendar gc2 = GregorianCalendar.getInstance();
		boolean hasFillDate;
		boolean hasLVFillDate;
		for(Bin b : getGoodBeans()) {
			hasFillDate=false;
			hasLVFillDate=false;
			if(b.getFillDate()!=null) {
				gc1.setTime(b.getFillDate());
				hasFillDate=true;
			}
			if(b.getLastValidFillDate()!=null) {
				gc2.setTime(b.getLastValidFillDate());
				hasLVFillDate=true;
			}
			
			sb = new StringBuffer(insertOrUpdateBinUrl);
			
			sb.append("[");
			if(b.getBinid()>=0) sb.append("binid:" + b.getBinid() +",");
			if(b.getFillLevel()>=0) sb.append("filllevel:" + b.getFillLevel() +",");
			if(b.getLastValidFillLevel()>=0) sb.append("lastvalidfilllevel:" + b.getLastValidFillLevel() +",");
			if(hasFillDate) sb.append("filldate:" + gc1.get(GregorianCalendar.YEAR)+"-"+gc1.get(GregorianCalendar.MONTH)+"-"+gc1.get(GregorianCalendar.DAY_OF_MONTH)+"-"+gc1.get(GregorianCalendar.HOUR)+"-"+gc1.get(GregorianCalendar.MINUTE)+"-"+gc1.get(GregorianCalendar.SECOND) +",");
			if(hasLVFillDate) sb.append("lastvalidfilldate:" + gc2.get(GregorianCalendar.YEAR)+"-"+gc2.get(GregorianCalendar.MONTH)+"-"+gc2.get(GregorianCalendar.DAY_OF_MONTH)+"-"+gc2.get(GregorianCalendar.HOUR)+"-"+gc2.get(GregorianCalendar.MINUTE)+"-"+gc2.get(GregorianCalendar.SECOND) +",");
			if(b.getLatitude()!=null) sb.append("latitude:" + b.getLatitude() +",");
			if(b.getLongitude()!=null) sb.append("longitude:" + b.getLongitude() +",");
			if(b.getGeoId()!=null) sb.append("geoid:" + b.getGeoId() +",");
			if(b.getStreet()!=null) sb.append("street:" + b.getStreet() +",");
			if(b.getHouseNr()!=null) sb.append("housenr:" + b.getHouseNr() +",");
			if(b.getPostCode()!=null) sb.append("postcode:" + b.getPostCode() +",");
			if(b.getCity()!=null) sb.append("city:" + b.getCity() +",");
			if(b.getCountry()!=null) sb.append("country:" + b.getCountry());
			sb.append("]");
			sb.append(";");
			
			// request header would be too large, so update each bin alone
			
			sb.append("]");
			url = sb.toString();
			url=url.replace(",]", "]");
			url=url.replace(",];]", "]]");
					
			HashMap<String, Boolean> tmpBean = rt.getForObject(url, HashMap.class);
			for(String k : tmpBean.keySet()) {
				beans.put(k, tmpBean.get(k));
			}
			
			

		}
		setGoodBeans(new ArrayList<Bin>());
		if(beans != null) {
			Boolean value;
			Bin bin;
			for(String k : beans.keySet()) {
				value = beans.get(k);
				bin = new Bin();
				bin.setBinid(Long.valueOf(k));
				if(value==Boolean.FALSE) {
					getBadBeans().add(bin);
				}
				else if (value==Boolean.TRUE) {
					getGoodBeans().add(bin);
				}
			}		
		}
		System.out.println("Finished data update");
		return badBins;
	}


	public List<Bin> getGoodBeans() {
		return goodBeans;
	}


	public void setGoodBeans(List<Bin> goodBeans) {
		this.goodBeans = goodBeans;
	}


	public List<Bin> getBadBeans() {
		return badBeans;
	}


	public void setBadBeans(List<Bin> badBeans) {
		this.badBeans = badBeans;
	}	
}
