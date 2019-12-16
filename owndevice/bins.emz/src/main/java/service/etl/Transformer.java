package service.etl;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import service.Bin;

public class Transformer implements ITableColumnNames, ITransformer {
	private List <String[]> entries;
	private HashMap<Integer, String> mapHeaderNameToColumn;
	
	private List<String[]> badBeans;
	private List<Bin> goodBeans; 
	


	
	public Transformer(List<String[]> entries, HashMap<Integer, String> mapHeaderNameToColumn) {
		this.entries = entries;
		this.mapHeaderNameToColumn = mapHeaderNameToColumn;
		this.goodBeans = new ArrayList<Bin>();
		this.badBeans = new ArrayList<String[]>();


		transform();
	}
	
	public void transform() {
		int i;
		Bin bin;
		boolean isBadBean;
		long binid;
		SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy hh:mm:ss");
		SimpleDateFormat df2 = new SimpleDateFormat("dd.MM.yyyy hh:mm");
		Date date;
		for(String[] entry : entries) {
			bin = new Bin();
			bin.setLineAsStringArray(entry);
			isBadBean = false;		
			for(i = 0; i < entry.length; i++) {
				if(isBadBean) break;
				String columnName = mapHeaderNameToColumn.get(i);
				try {
					if(columnName.equals(HEADER_NAME_OF_BIN_ID.toLowerCase().trim())) {
						binid = Long.valueOf(entry[i].replace("\"", ""));
						bin.setBinid(Long.valueOf(binid));
					}
					else if(columnName.equals(HEADER_NAME_OF_CITY.toLowerCase().trim())) {
						bin.setCity(entry[i]);
					}
					else if(columnName.equals(HEADER_NAME_OF_GEO_ID.toLowerCase().trim())) {
						bin.setGeoId(entry[i]);
					}
					else if(columnName.equals(HEADER_NAME_OF_LAST_VALID_FILL_DATE.toLowerCase().trim())) {
						try {
							date = df.parse(entry[i].replace("\"", ""));
						}
						catch(ParseException e) {
							date = df2.parse(entry[i].replace("\"", ""));
						}
						bin.setLastValidFillDate(date);
						bin.setFillDate(date);
					}
					else if(columnName.equals(HEADER_NAME_OF_LAST_VALID_FILL_LEVEL.toLowerCase().trim())) {
						bin.setLastValidFillLevel(Integer.parseInt(entry[i].replace("\"", "")));
						bin.setFillLevel(Integer.parseInt(entry[i].replace("\"", "")));
					}
					else if(columnName.equals(HEADER_NAME_OF_LATITUDE.toLowerCase().trim())) {
						bin.setLatitude(entry[i].replace("\"", ""));
					}
					else if(columnName.equals(HEADER_NAME_OF_LONGITUDE.toLowerCase().trim())) {
						bin.setLongitude(entry[i].replace("\"", ""));
					}
					else if(columnName.equals(HEADER_NAME_OF_POST_CODE.toLowerCase().trim())) {
						bin.setPostCode(entry[i]);
					}
					else if(columnName.equals(HEADER_NAME_OF_STREET.toLowerCase().trim())) {
						bin.setStreet(entry[i]);
					}
				}
				catch(NumberFormatException | ParseException ex) {
					badBeans.add(entry);
					isBadBean = true;
				} 	
			}
			if(!isBadBean) goodBeans.add(bin);
		}	
	}

	public List<String[]> getBadBeans() {
		return badBeans;
	}

	public void setBadBeans(List<String[]> badBeans) {
		this.badBeans = badBeans;
	}

	public List<Bin> getGoodBeans() {
		return goodBeans;
	}

	public void setGoodBeans(List<Bin> goodBeans) {
		this.goodBeans = goodBeans;
	}
	
}
