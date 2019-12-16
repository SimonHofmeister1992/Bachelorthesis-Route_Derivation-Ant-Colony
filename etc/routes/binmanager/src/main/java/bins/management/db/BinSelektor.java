package bins.management.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import bins.management.entity.Bin;

public class BinSelektor {

	private Connection conn = null; 
	private static MySqlDatabaseAccessor dba = new MySqlDatabaseAccessor();
	private long orgId, fractionId;
	private Long regionId;
	
	public BinSelektor(long orgId, long fractionId, Long regionId) {
		conn = dba.getConnection();
		this.orgId = orgId;
		this.fractionId = fractionId;
		this.regionId = regionId;
		
	}
	
	public ArrayList<Bin> getBinsWithFullInformation() {
    	
		ArrayList<Bin> bins = new ArrayList<Bin>();
		
		Bin bin = null;
		ResultSet rs = null;		
		
		if(conn != null) {
			String query= 
					"SELECT "
					+ "l.loc_id as loc_id, "
	    			+ "l.longitude as longitude, "
	    			+ "l.latitude as latitude, "
	    			+ "l.geo_id as geo_id, "
	    			+ "a.street as street, "
	    			+ "a.house_nr as house_nr, "
	    			+ "c.name as cityname, "
	    			+ "c.plz as postcode, "
	    			+ "b.devicenum as devicenum, "
	    			+ "b.filllevel as filllevel, "
	    			+ "b.lastfl as lastvalidfilllevel, "
	    			+ "b.timestamp as timestamp, "
	    			+ "b.lastfltimestamp as lastvalidtimestamp "
	    			+ "FROM devices.bin as b "
	    			+ "INNER JOIN distances.locations as l ON b.loc_id=l.loc_id "
	    			+ "INNER JOIN distances.location_type as t ON l.location_type_id=t.id "
	    			+ "INNER JOIN distances.address as a ON l.address_id=a.id "
	    			+ "INNER JOIN distances.city as c ON a.city_id = c.id "
	    			+ "WHERE t.organization_id=" + orgId + " AND t.fraction_id=" + fractionId + " AND t.region_id='" + regionId + "';";
	    	rs = dba.fireQuery(conn, query, StatementType.SELECT);
	    	
	    	if(rs != null) {
		    	try {
					GregorianCalendar c = new GregorianCalendar();
					GregorianCalendar d = new GregorianCalendar();
					GregorianCalendar t = new GregorianCalendar();
					while(rs.next()) {
						bin = new Bin();
						bin.setFraction(fractionId);
						bin.setLocId(rs.getLong("loc_id"));
						bin.setBinid(rs.getLong("devicenum"));
						bin.setLatitude(rs.getString("latitude"));
						bin.setLongitude(rs.getString("longitude"));
						bin.setGeoId(rs.getString("geo_id"));
						bin.setStreet(rs.getString("street"));
						bin.setHouseNr(rs.getString("house_nr"));
						bin.setPostCode(rs.getString("postcode"));
						bin.setCity(rs.getString("cityname"));
						d.setTime(rs.getTime("timestamp"));
						t.setTime(rs.getTime("timestamp"));
						c.set(d.get(Calendar.YEAR), d.get(Calendar.MONTH), d.get(Calendar.DAY_OF_MONTH), t.get(Calendar.HOUR)-1, t.get(Calendar.MINUTE), t.get(Calendar.SECOND));
						bin.setFillDate(c.getTime());
						d.setTime(rs.getDate("lastvalidtimestamp"));
						t.setTime(rs.getDate("lastvalidtimestamp"));
						c.set(d.get(Calendar.YEAR), d.get(Calendar.MONTH), d.get(Calendar.DAY_OF_MONTH), t.get(Calendar.HOUR)-1, t.get(Calendar.MINUTE), t.get(Calendar.SECOND));
						bin.setLastValidFillDate(c.getTime());
						bin.setFillLevel(rs.getInt("filllevel"));
						bin.setLastValidFillLevel(rs.getInt("lastvalidfilllevel"));
						bins.add(bin);
					}
				} catch (SQLException e) {
					e.printStackTrace();
				}
	    	}
		}
		return bins;
	}
	public ArrayList<Bin> getBinsWithLocationInformation() {
    	
		conn = dba.getConnection();
		
		ArrayList<Bin> bins = new ArrayList<Bin>();
		
		Bin bin = null;
		ResultSet rs = null;		
		
		if(conn != null) {
			String 	query= "SELECT "
					+ "l.loc_id as loc_id, "
	    			+ "l.longitude as longitude, "
	    			+ "l.latitude as latitude, "
	    			+ "l.geo_id as geo_id, "
	    			+ "b.devicenum as devicenum, "
	    			+ "b.filllevel as filllevel, "
	    			+ "b.lastfl as lastvalidfilllevel, "
	    			+ "b.timestamp as timestamp, "
	    			+ "b.lastfltimestamp as lastvalidtimestamp "
	    			+ "FROM devices.bin as b "
	    			+ "INNER JOIN distances.locations as l ON b.loc_id=l.loc_id "
	    			+ "INNER JOIN distances.location_type as t ON l.location_type_id=t.id "
	    			+ "WHERE t.organization_id=" + orgId + " AND t.fraction_id=" + fractionId + " AND t.region_id='" + regionId + "';";
	    	rs = dba.fireQuery(conn, query, StatementType.SELECT);
	    	
	    	if(rs != null) {
		    	try {
					GregorianCalendar c = new GregorianCalendar();
					GregorianCalendar d = new GregorianCalendar();
					GregorianCalendar t = new GregorianCalendar();
					while(rs.next()) {
						bin = new Bin();
						bin.setFraction(fractionId);
						bin.setLocId(rs.getLong("loc_id"));
						bin.setBinid(rs.getLong("devicenum"));
						bin.setLatitude(rs.getString("latitude"));
						bin.setLongitude(rs.getString("longitude"));
						bin.setGeoId(rs.getString("geo_id"));
						d.setTime(rs.getDate("timestamp"));
						t.setTime(rs.getTime("timestamp"));
						c.set(d.get(Calendar.YEAR), d.get(Calendar.MONTH), d.get(Calendar.DAY_OF_MONTH), t.get(Calendar.HOUR)-1, t.get(Calendar.MINUTE), t.get(Calendar.SECOND));
						bin.setFillDate(c.getTime());
						d.setTime(rs.getDate("lastvalidtimestamp"));
						t.setTime(rs.getTime("lastvalidtimestamp"));
						c.set(d.get(Calendar.YEAR), d.get(Calendar.MONTH), d.get(Calendar.DAY_OF_MONTH), t.get(Calendar.HOUR)-1, t.get(Calendar.MINUTE), t.get(Calendar.SECOND));
						bin.setLastValidFillDate(c.getTime());
						bin.setFillLevel(rs.getInt("filllevel"));
						bin.setLastValidFillLevel(rs.getInt("lastvalidfilllevel"));
						bins.add(bin);
					}
				} catch (SQLException e) {
					e.printStackTrace();
				}
	    	}
		}
		return bins;
	}
	public ArrayList<Bin> getBinsWithBasicInformation() {
    	
		conn = dba.getConnection();
		
		ArrayList<Bin> bins = new ArrayList<Bin>();
		
		Bin bin = null;
		ResultSet rs = null;		
		
		if(conn != null) {
			String 	query= 
					"SELECT "
					+ "l.loc_id as loc_id, "
	    			+ "b.devicenum as devicenum, "
	    			+ "b.filllevel as filllevel, "
	    			+ "b.lastfl as lastvalidfilllevel, "
	    			+ "b.timestamp as timestamp, "
	    			+ "b.lastfltimestamp as lastvalidtimestamp "
	    			+ "FROM devices.bin as b "
	    			+ "INNER JOIN distances.locations as l ON b.loc_id=l.loc_id "
	    			+ "INNER JOIN distances.location_type as t ON l.location_type_id=t.id "
	    			+ "WHERE t.organization_id=" + orgId + " AND t.fraction_id=" + fractionId + " AND t.region_id='" + regionId + "';";
	    	rs = dba.fireQuery(conn, query, StatementType.SELECT);
	    	
	    	if(rs != null) {
		    	try {
					GregorianCalendar c = new GregorianCalendar();
					GregorianCalendar d = new GregorianCalendar();
					GregorianCalendar t = new GregorianCalendar();
					while(rs.next()) {
						bin = new Bin();
						bin.setFraction(fractionId);
						bin.setLocId(rs.getLong("loc_id"));
						bin.setBinid(rs.getLong("devicenum"));
						d.setTime(rs.getDate("timestamp"));
						t.setTime(rs.getTime("timestamp"));
						c.set(d.get(Calendar.YEAR), d.get(Calendar.MONTH), d.get(Calendar.DAY_OF_MONTH), t.get(Calendar.HOUR)-1, t.get(Calendar.MINUTE), t.get(Calendar.SECOND));
						bin.setFillDate(c.getTime());
						d.setTime(rs.getDate("lastvalidtimestamp"));
						t.setTime(rs.getTime("lastvalidtimestamp"));
						c.set(d.get(Calendar.YEAR), d.get(Calendar.MONTH), d.get(Calendar.DAY_OF_MONTH), t.get(Calendar.HOUR)-1, t.get(Calendar.MINUTE), t.get(Calendar.SECOND));
						bin.setLastValidFillDate(c.getTime());
						bin.setFillLevel(rs.getInt("filllevel"));
						bin.setLastValidFillLevel(rs.getInt("lastvalidfilllevel"));
						bins.add(bin);
					}
				} catch (SQLException e) {
					e.printStackTrace();
				}
	    	}
		}
		return bins;
	}
	public ArrayList<Bin> getBinsWithoutInformation() {
    	
		conn = dba.getConnection();
		
		ArrayList<Bin> bins = new ArrayList<Bin>();
		
		Bin bin = null;
		ResultSet rs = null;		
		
		if(conn != null) {
			String 	query= 
					"SELECT "
					+ "b.devicenum as devicenum, "
					+ "b.loc_id as loc_id "
			    	+ "FROM devices.bin as b "
			    	+ "INNER JOIN distances.locations as l ON b.loc_id=l.loc_id "
			    	+ "INNER JOIN distances.location_type as t ON l.location_type_id=t.id "
			    	+ "WHERE t.organization_id=" + orgId + " AND t.fraction_id=" + fractionId + " AND t.region_id='" + regionId + "';";
	    	rs = dba.fireQuery(conn, query, StatementType.SELECT);
	    	
	    	if(rs != null) {
		    	try {
					while(rs.next()) {
						bin = new Bin();
						bin.setFraction(fractionId);
						bin.setLocId(rs.getLong("loc_id"));
						bin.setBinid(rs.getLong("devicenum"));
						bins.add(bin);
					}
				} catch (SQLException e) {
					e.printStackTrace();
				}
	    	}
		}
		return bins;
	}

	public ArrayList<Bin> getOnlyLocations() {
		conn = dba.getConnection();
		
		ArrayList<Bin> bins = new ArrayList<Bin>();
		
		Bin bin = null;
		ResultSet rs = null;		
		
		if(conn != null) {
			String 	query= 
					"SELECT "
					+ "l.loc_id as loc_id, "
					+ "l.latitude as lat, "
					+ "l.longitude as lon, "
					+ "l.address_id as address, "
					+ "l.geo_id as geoId "
			    	+ "FROM distances.locations as l "
			    	+ "INNER JOIN distances.location_type as t ON l.location_type_id=t.id "
			    	+ "WHERE t.organization_id=" + orgId + " AND t.fraction_id=" + fractionId + " AND t.region_id='" + regionId + "';";
	    	rs = dba.fireQuery(conn, query, StatementType.SELECT);
	    	
	    	if(rs != null) {
		    	try {
					while(rs.next()) {
						bin = new Bin();
						bin.setLocId(rs.getLong("loc_id"));
						bin.setAddressId(rs.getLong("address"));
						bin.setLongitude(rs.getString("lon"));
						bin.setLatitude(rs.getString("lat"));
						bin.setGeoId(rs.getString("geoId"));
						bins.add(bin);
					}
				} catch (SQLException e) {
					e.printStackTrace();
				}
	    	}
		}
		return bins;
	}	
}
