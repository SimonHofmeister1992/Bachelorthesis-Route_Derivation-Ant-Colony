package bins.management.entity;

import java.util.Date;

public class Bin {
	private String[] lineAsStringArray; // only needed for logging if bean is good, but updating goes wrong
	private long binid; // equals devicenum, not id of entity in database
	private long locId = Long.MIN_VALUE; // foreign key to distances.locations
	private String street;
	private long fraction;
	private String postCode;
	private String city;
	private String countryName;
	private String latitude;
	private String longitude;
	private int lastValidFillLevel;
	private Date lastValidFillDate;
	private String geoId;	// only an identifier
	private String houseNr;
	private int fillLevel;
	private Date fillDate;
	private transient long addressId;
	private String country;
	
	public long getBinid() {
		return binid;
	}
	public void setBinid(long binid) {
		this.binid = binid;
	}
	public String getStreet() {
		return street;
	}
	public void setStreet(String street) {
		this.street = street;
	}
	public String getPostCode() {
		return postCode;
	}
	public void setPostCode(String postCode) {
		this.postCode = postCode;
	}
	public String getLatitude() {
		return latitude;
	}
	public void setLatitude(String latitude) {
		while(latitude.lastIndexOf(".") > 3) latitude = latitude.substring(0, latitude.lastIndexOf(".")) + latitude.substring(latitude.lastIndexOf(".")+1, latitude.length()); 
		this.latitude = latitude;
	}
	public int getLastValidFillLevel() {
		return lastValidFillLevel;
	}
	public void setLastValidFillLevel(int lastValidFillLevel) {
		this.lastValidFillLevel = lastValidFillLevel;
	}
	public Date getLastValidFillDate() {
		return lastValidFillDate;
	}
	public void setLastValidFillDate(Date lastValidFillDate) {
		this.lastValidFillDate = lastValidFillDate;
	}
	public Long getFraction() {
		return fraction;
	}
	public void setFraction(long fractionId) {
		this.fraction = fractionId;
	}
	public String getCity() {
		return city;
	}
	public void setCity(String city) {
		this.city = city;
	}
	public String getLongitude() {
		return longitude;
	}
	public void setLongitude(String longitude) {
		while(longitude.lastIndexOf(".") > 3) longitude = longitude.substring(0, longitude.lastIndexOf(".")) + longitude.substring(longitude.lastIndexOf(".")+1, longitude.length()); 
		this.longitude = longitude;
	}
	public String getGeoId() {
		return geoId;
	}
	public void setGeoId(String geoId) {
		this.geoId = geoId;
	}
	public String[] getLineAsStringArray() {
		return lineAsStringArray;
	}
	public void setLineAsStringArray(String[] lineAsStringArray) {
		this.lineAsStringArray = lineAsStringArray;
	}
	public String getHouseNr() {
		return houseNr;
	}
	public void setHouseNr(String houseNr) {
		this.houseNr = houseNr;
	}
	public int getFillLevel() {
		return fillLevel;
	}
	public void setFillLevel(int fillLevel) {
		this.fillLevel = fillLevel;
	}
	public Date getFillDate() {
		return fillDate;
	}
	public void setFillDate(Date fillDate) {
		this.fillDate = fillDate;
	}
	public long getLocId() {
		return locId;
	}
	public void setLocId(long locId) {
		this.locId = locId;
	}

	@Override
	public boolean equals(Object o) {
		if(this.getBinid() == ((Bin) o).getBinid()) return true;
		return false;
	}
	public String getCountryName() {
		return countryName;
	}
	public void setCountryName(String countryName) {
		this.countryName = countryName;
	}
	public long getAddressId() {
		return addressId;
	}
	public void setAddressId(long addressId) {
		this.addressId = addressId;
	}
	public String getCountry() {
		return country;
	}
	public void setCountry(String country) {
		this.country = country;
	}
}
