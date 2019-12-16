package bins.management.db;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import bins.management.entity.Bin;
import bins.management.entity.DistanceMatrixEntry;
import bins.management.rest.BinManagerService;
import bins.management.rest.GoogleDistanceApiConsumer;
import bins.management.rest.IDistanceMatrixApiConsumer;
import bins.management.rest.IInfoType;

public class BinUpdateOrInserter {
	private Connection conn = null; 
	private MySqlDatabaseAccessor dba;
	private long locType;

	
	private PreparedStatement preparedBinEmptyStatement;
	private PreparedStatement preparedBinSelectStatement;
	private PreparedStatement preparedBinInsertStatement;
	private PreparedStatement preparedBinUpdateStatement;
	private PreparedStatement preparedLocationSelectStatement;
	private PreparedStatement preparedLocationInsertStatement;
	private PreparedStatement preparedAddressSelectStatement;
	private PreparedStatement preparedAddressWithoutHosueNrSelectStatement;
	private PreparedStatement preparedAddressInsertStatement;
	private PreparedStatement preparedCitySelectStatement;
	private PreparedStatement preparedCityInsertStatement;
	private PreparedStatement preparedCountrySelectStatement;
	private PreparedStatement preparedCountryInsertStatement;
	private PreparedStatement preparedDistanceMatrixInsertStatement;
	
	private Long orgId, fractionId, regionId;
	
	public BinUpdateOrInserter(long orgId, long fractionId, long regionId) throws SQLException {
		
		this.orgId=orgId;
		this.fractionId=fractionId;
		this.regionId=regionId;
		
		dba = new MySqlDatabaseAccessor();
		conn = dba.getConnection();
		Statement createStatement = conn.createStatement();
		ResultSet executeQuery = createStatement.executeQuery("SELECT id as id FROM distances.location_type WHERE organization_id='" + orgId + "' AND fraction_id='" + fractionId + "' AND region_id='" + regionId + "';");
		while (executeQuery.next()) {
			this.locType=executeQuery.getLong("id");			
		}
			preparedBinSelectStatement = conn.prepareStatement("SELECT b.id as id, b.loc_id as locId, l.address_id as address, l.longitude as lon, l.latitude as lat "
					+ "FROM devices.bin b "
					+ "INNER JOIN distances.locations l ON b.loc_id=l.loc_id "
					+ "WHERE devicenum=? AND location_type_id=?;");
			preparedBinInsertStatement = conn.prepareStatement("INSERT INTO devices.bin (devicenum, filllevel, lastfl, loc_id, timestamp,  lastfltimestamp) VALUES (?,?,?,?,?,?);");
			preparedBinUpdateStatement = conn.prepareStatement("Update devices.bin Set devicenum=?, filllevel=?, lastfl=?, loc_id=?, timestamp=?,  lastfltimestamp=? WHERE id=? AND loc_id=?;");
			preparedLocationSelectStatement = conn.prepareStatement("SELECT loc_id as id, latitude as lat, longitude as lon FROM distances.locations WHERE location_type_id=? AND address_id=? AND latitude=? AND longitude=?;");
			preparedLocationInsertStatement = conn.prepareStatement("INSERT INTO distances.locations(location_type_id, latitude, longitude, geo_id, address_id) VALUES (?,?,?,?,?);");
			preparedAddressSelectStatement = conn.prepareStatement("SELECT id as id FROM distances.address WHERE street=? AND city_id=? AND house_nr=?;");
			preparedAddressWithoutHosueNrSelectStatement = conn.prepareStatement("SELECT id as id FROM distances.address WHERE street=? AND city_id=?;");
			preparedAddressInsertStatement = conn.prepareStatement("INSERT INTO distances.address (street, house_nr, city_id) VALUES (?,?,?);");
			preparedCitySelectStatement = conn.prepareStatement("SELECT id as id FROM distances.city WHERE plz=? AND name=? AND country_id=?;");
			preparedCityInsertStatement = conn.prepareStatement("INSERT INTO distances.city (plz, name, country_id) VALUES (?,?,?);");
			preparedCountrySelectStatement = conn.prepareStatement("SELECT id as id FROM distances.country WHERE name=?;");
			preparedCountryInsertStatement = conn.prepareStatement("INSERT INTO distances.country (name) VALUES (?);");
			
			preparedDistanceMatrixInsertStatement = conn.prepareStatement("INSERT INTO distances.distance_matrix (from_loc_id, to_loc_id, distance) VALUES (?,?,?);");
			preparedBinEmptyStatement = conn.prepareStatement("UPDATE devices.bin AS b INNER JOIN distances.locations l ON l.loc_id=b.loc_id SET filllevel=?, timestamp=? WHERE l.location_type_id=?;");
	
	}
	
	public boolean updateOrInsert (Bin bin) {
		
		clearPreparedStatements();
		
		boolean isSuccessfull = false;
		int size;
		// decision if insert or update BIN => check if bin already exists
		try {
			preparedBinSelectStatement.setLong(1, bin.getBinid());
			preparedBinSelectStatement.setLong(2, locType);
			ResultSet persistedBins = preparedBinSelectStatement.executeQuery();
			persistedBins.last();
			size=persistedBins.getRow();
			persistedBins.beforeFirst();
			if(size==0) {
				isSuccessfull = insertBin(bin);
			}
			else if(size==1) {
				Bin persistedBin = new Bin();
				
				while(persistedBins.next()) {
					persistedBin.setBinid(persistedBins.getLong("id"));
					persistedBin.setLocId(persistedBins.getLong("locId"));
					persistedBin.setAddressId(persistedBins.getLong("address"));
					persistedBin.setLongitude(persistedBins.getString("lon"));
					persistedBin.setLatitude(persistedBins.getString("lat"));
				}
				isSuccessfull = updateBin(bin, persistedBin);
			}
			else {
				return isSuccessfull;
			}			
		} catch (SQLException e) {
			return isSuccessfull;
		}
		return isSuccessfull;
	}
	
	
	private boolean insertBin(Bin bin) {
		// check if necessary information is available
		if((bin.getLatitude() != null && bin.getLongitude() != null) || (bin.getCity()!=null && bin.getPostCode()!= null && bin.getStreet()!=null && bin.getCountryName()!=null)) {
			Long addressId=null, locId=null;
			int size;
			try {
				
				addressId = manageAddress(bin);

				preparedLocationSelectStatement.setLong(1, locType);
				preparedLocationSelectStatement.setLong(2, addressId);
				preparedLocationSelectStatement.setString(3, bin.getLatitude());
				preparedLocationSelectStatement.setString(4, bin.getLongitude());
				ResultSet locIdSet = preparedLocationSelectStatement.executeQuery();
				locIdSet.last();
				size=locIdSet.getRow();
				locIdSet.beforeFirst();
				if(size==0) {
					preparedLocationInsertStatement.setLong(1, locType);
					preparedLocationInsertStatement.setString(2, bin.getLatitude());
					preparedLocationInsertStatement.setString(3, bin.getLongitude());
					preparedLocationInsertStatement.setString(4, bin.getGeoId());
					preparedLocationInsertStatement.setLong(5, addressId);
					preparedLocationInsertStatement.execute();
					locIdSet = preparedLocationSelectStatement.executeQuery();
				}
				while(locIdSet.next()) {
					locId = locIdSet.getLong("id");
				}
				bin.setLocId(locId);
				if(size==0) {
					updateDistanceMatrix(bin);					
				}
				
				preparedBinInsertStatement.setLong(1, bin.getBinid());
				preparedBinInsertStatement.setInt(2, bin.getFillLevel());
				preparedBinInsertStatement.setInt(3, bin.getLastValidFillLevel());
				preparedBinInsertStatement.setLong(4, locId);
				preparedBinInsertStatement.setTimestamp(5, new java.sql.Timestamp(bin.getFillDate().getTime()));
				preparedBinInsertStatement.setTimestamp(6, new java.sql.Timestamp(bin.getLastValidFillDate().getTime()));
				preparedBinInsertStatement.execute();
				return true;
				
			} catch (SQLException e) {
				return false;
			}
		}
		else {
			return false;
		}
	}

	private boolean updateBin(Bin bin, Bin persistedBin) {
		clearPreparedStatements();
		
		Long persistedLocId=null;
		String persistedLatitude=null, persistedLongitude=null;
		Long persistedAddressId=manageAddress(persistedBin);
		if(persistedAddressId==null) persistedAddressId=persistedBin.getAddressId();
		Long addressId = null;
		Boolean locationChanged = false;
		Boolean binHasLocationInfo = false;
		
		if((bin.getLongitude()!= null && bin.getLatitude()!=null) || (bin.getStreet()!=null && bin.getCity()!=null)) binHasLocationInfo=true;
		
		try {		
			// persisted location
			preparedLocationSelectStatement.setLong(1, locType);
			preparedLocationSelectStatement.setLong(2, persistedAddressId);
			preparedLocationSelectStatement.setString(3, persistedBin.getLatitude());
			preparedLocationSelectStatement.setString(4, persistedBin.getLongitude());
			ResultSet persistedLocIdSet = preparedLocationSelectStatement.executeQuery();
			while(persistedLocIdSet.next()) {
				persistedLocId = persistedLocIdSet.getLong("id");
				persistedLatitude = persistedLocIdSet.getString("lat");
				persistedLongitude = persistedLocIdSet.getString("lon");
			}
		} catch (SQLException e) {
			return false;
		}
		
		
		if(binHasLocationInfo) addressId = manageAddress(bin);
		
		
		
		// HINT: now we have: persisted bin with locId, actual location infos (address id, longitude, latitude, location_id_old)
		//		              new bin: address id, longitude, latitude
		
		// => (address id, longitude, latitude) changed ? update location id AND update distance matrix : only update bin

		
		if(binHasLocationInfo) {
			if(!(addressId.equals(persistedAddressId) && persistedLongitude.equals(bin.getLongitude()) && persistedLatitude.equals(bin.getLatitude()))) {
				
				// request or insert location 
				// AND update location id
				int size;
				try {
					
					preparedLocationSelectStatement.clearParameters();
					
					preparedLocationSelectStatement.setLong(1, locType);
					preparedLocationSelectStatement.setLong(2, addressId);
					preparedLocationSelectStatement.setString(3, bin.getLatitude());
					preparedLocationSelectStatement.setString(4, bin.getLongitude());
					
					ResultSet newLocation = preparedLocationSelectStatement.executeQuery();
					newLocation.last();
					size = newLocation.getRow();
					newLocation.beforeFirst();
					if(newLocation!= null && size>0) {
						while(newLocation.next()) {
							bin.setLocId(newLocation.getLong("id"));
						}
					}
					else {
						preparedLocationInsertStatement.setLong(1, locType);
						preparedLocationInsertStatement.setString(2, bin.getLatitude());
						preparedLocationInsertStatement.setString(3, bin.getLongitude());
						preparedLocationInsertStatement.setString(4, bin.getGeoId());
						preparedLocationInsertStatement.setLong(5, addressId);
						preparedLocationInsertStatement.execute();
						
						preparedLocationSelectStatement.clearParameters();
						
						preparedLocationSelectStatement.setLong(1, locType);
						preparedLocationSelectStatement.setLong(2, addressId);
						preparedLocationSelectStatement.setString(3, bin.getLatitude());
						preparedLocationSelectStatement.setString(4, bin.getLongitude());
						newLocation = preparedLocationSelectStatement.executeQuery();
						while(newLocation.next()) {
							bin.setLocId(newLocation.getLong("id"));
						}
						locationChanged = true;
					}
				} catch (SQLException e) {
					e.printStackTrace();
				}
				
				// AND update distance matrix
				if(locationChanged) {
					updateDistanceMatrix(bin);
				}
				
			}
		}

		
		// finally update bin info. "Update devices.bin Set devicenum=?, filllevel=?, lastfl=?, loc_id=?, timestamp=?,  lastfltimestamp=? WHERE oldDevicenum=? AND oldLocId=?
		try {
			preparedBinUpdateStatement.setLong(1, bin.getBinid());
			preparedBinUpdateStatement.setLong(2, bin.getFillLevel());
			preparedBinUpdateStatement.setLong(3, bin.getLastValidFillLevel());
			if(bin.getLocId()>0) preparedBinUpdateStatement.setLong(4, bin.getLocId());
			else preparedBinUpdateStatement.setLong(4, persistedBin.getLocId());
			preparedBinUpdateStatement.setTimestamp(5, new java.sql.Timestamp(bin.getFillDate().getTime()));
			preparedBinUpdateStatement.setTimestamp(6, new java.sql.Timestamp(bin.getLastValidFillDate().getTime()));
			preparedBinUpdateStatement.setLong(7, persistedBin.getBinid());
			preparedBinUpdateStatement.setLong(8, persistedBin.getLocId());
			int num = preparedBinUpdateStatement.executeUpdate();
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
		}

		
		return false;
	}

	// returns the address-id (already existing or after creating new)
	private Long manageAddress(Bin bin) {
		clearPreparedStatements();
		Long countryId=null, cityId=null, addressId=null;
		int size;
		if(bin.getCountryName()==null) bin.setCountryName("null");
		if(bin.getHouseNr()==null) preparedAddressSelectStatement=preparedAddressWithoutHosueNrSelectStatement;
		if(bin.getCity()!=null && bin.getPostCode()!= null && bin.getStreet()!=null) {
			try {
				preparedCountrySelectStatement.setString(1, bin.getCountryName());
				ResultSet countryIdSet = preparedCountrySelectStatement.executeQuery();
				countryIdSet.last();
				size=countryIdSet.getRow();
				countryIdSet.beforeFirst();
				if(size==0) {
					preparedCountryInsertStatement.setString(1, bin.getCountryName());
					preparedCountryInsertStatement.execute();
					countryIdSet = preparedCountrySelectStatement.executeQuery();
				}
				while(countryIdSet.next()) {
					countryId = countryIdSet.getLong("id");
				}
			
				preparedCitySelectStatement.setString(1, bin.getPostCode());
				preparedCitySelectStatement.setString(2, bin.getCity());
				if(countryId != null) preparedCitySelectStatement.setLong(3, countryId);
				else preparedCitySelectStatement.setString(3, "*");
				ResultSet cityIdSet = preparedCitySelectStatement.executeQuery();
				cityIdSet.last();
				size=cityIdSet.getRow();
				cityIdSet.beforeFirst();
				if(size==0) {
					preparedCityInsertStatement.setString(1, bin.getPostCode());
					preparedCityInsertStatement.setString(2, bin.getCity());
					if(countryId != null) preparedCityInsertStatement.setLong(3, countryId);
					else {
						preparedCityInsertStatement.setLong(3, -1L);
					}
					preparedCityInsertStatement.execute();
					cityIdSet = preparedCitySelectStatement.executeQuery();
				}
				while(cityIdSet.next()) {
					cityId = cityIdSet.getLong("id");
				}
				
				preparedAddressSelectStatement.setString(1, bin.getStreet());
				preparedAddressSelectStatement.setLong(2, cityId);
				if(bin.getHouseNr()!=null)preparedAddressSelectStatement.setString(3, bin.getHouseNr());
				ResultSet addressIdSet = preparedAddressSelectStatement.executeQuery();
				addressIdSet.last();
				size=addressIdSet.getRow();
				addressIdSet.beforeFirst();
				if(size==0) {
					preparedAddressInsertStatement.setString(1, bin.getStreet());
					preparedAddressInsertStatement.setString(2, bin.getHouseNr());
					preparedAddressInsertStatement.setLong(3, cityId);
					preparedAddressInsertStatement.execute();
					preparedAddressSelectStatement.clearParameters();
					preparedAddressSelectStatement.setString(1, bin.getStreet());
					preparedAddressSelectStatement.setLong(2, cityId);
					if(bin.getHouseNr()!=null)preparedAddressSelectStatement.setString(3, bin.getHouseNr());
					addressIdSet = preparedAddressSelectStatement.executeQuery();
				}
				addressIdSet.beforeFirst();
				while(addressIdSet.next()) {
					addressId = addressIdSet.getLong("id");
				}
			}catch(SQLException e) {
				return null;
			}	
		}
		return addressId;
	}
	
	private void clearPreparedStatements() {
		try {
			preparedBinSelectStatement.clearParameters();
			preparedBinInsertStatement.clearParameters();
			preparedLocationSelectStatement.clearParameters();
			preparedLocationInsertStatement.clearParameters();
			preparedAddressSelectStatement.clearParameters();
			preparedAddressInsertStatement.clearParameters();
			preparedCitySelectStatement.clearParameters();
			preparedCityInsertStatement.clearParameters();
			preparedCountrySelectStatement.clearParameters();
			preparedCountryInsertStatement.clearParameters();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private void updateDistanceMatrix(Bin bin) {
		// search for any bins related to deviceType.
//		System.out.println("updateDistanceMatrix");
		BinManagerService bms = new BinManagerService();
		List<Bin> bins = bms.getBins(orgId, fractionId, regionId, IInfoType.LOCATIONS_ONLY).getBody();
		List<Bin> withSelfLocation = new ArrayList();
		if(bins != null) {
			for (Bin b : bins) {
				if(b.getLocId() == bin.getLocId()) {
					withSelfLocation.add(b);
				}
			}
				for (Bin bn : withSelfLocation) {
					for(int i=bins.size()-1; i >= 0;i--) {
						if(bn.getLocId()==bins.get(i).getLocId()) bins.remove(i);
					}
				}
		}	
		HashMap<Long, DistanceMatrixEntry> distanceMatrix = new HashMap<Long, DistanceMatrixEntry>(); // locId1, locId2, longLat/Address1, longLat/Address2, distanc		as
		DistanceMatrixEntry dme1, dme2;
		Long idDirection1,idDirection2;
		
		Bin updatedBin, otherBin;
		String location1, location2;
		
		updatedBin = bin;
		if(updatedBin.getLongitude()!=null && updatedBin.getLatitude()!=null) {
			location1 = updatedBin.getLatitude() + "," + updatedBin.getLongitude();
		}
		else {
			location1 = updatedBin.getStreet() + " " + updatedBin.getHouseNr() + "," + updatedBin.getPostCode() + " " + updatedBin.getCity() + "," + updatedBin.getCountryName();
		}
		
		for(int i=0; i < bins.size(); i++) {
			otherBin = bins.get(i);
			if(otherBin.getLongitude()!=null && otherBin.getLatitude()!=null) {
				location2 = otherBin.getLatitude() + "," + otherBin.getLongitude();
			}
			else {
				location2 = otherBin.getStreet() + " " + otherBin.getHouseNr() + "," + otherBin.getPostCode() + " " + otherBin.getCity() + "," + otherBin.getCountryName();
			}
			
			// from new location to existing location
			
			dme1 = new DistanceMatrixEntry();					
			idDirection1=(long) i;
			dme1.setLocId1(updatedBin.getLocId());
			dme1.setLongLat1OrAddress1(location1);	
			dme1.setLocId2(otherBin.getLocId());
			dme1.setLongLat2OrAddress2(location2);
			distanceMatrix.putIfAbsent(idDirection1, dme1);
			
			// from existing location to new location
			
			dme2 = new DistanceMatrixEntry();
			idDirection2=(long) (bins.size()+i);
			dme2.setLocId1(otherBin.getLocId());
			dme2.setLongLat1OrAddress1(location2);
			dme2.setLocId2(updatedBin.getLocId());
			dme2.setLongLat2OrAddress2(location1);	
			distanceMatrix.putIfAbsent(idDirection2, dme2);
			
		}
		
		// also put distance 0 to same location
		
		dme1 = new DistanceMatrixEntry();					
		idDirection1=(long) distanceMatrix.size();
		dme1.setLocId1(updatedBin.getLocId());
		dme1.setLocId2(updatedBin.getLocId());
		distanceMatrix.putIfAbsent(idDirection1, dme1);
		
		IDistanceMatrixApiConsumer gdac = new GoogleDistanceApiConsumer();
		
		for(Long key : distanceMatrix.keySet()) {
			DistanceMatrixEntry distanceMatrixEntry = distanceMatrix.get(key);
			 // call google-api distances with locationStrings => assign to Long value "distances"
			if(!distanceMatrixEntry.getLocId1().equals(distanceMatrixEntry.getLocId2()))
				distanceMatrixEntry = gdac.getDistanceByDistanceMatrixEntry(distanceMatrixEntry);
			else{
				distanceMatrixEntry.setDistance(0L);
			}

			try {
				preparedDistanceMatrixInsertStatement.clearParameters();
				preparedDistanceMatrixInsertStatement.setLong(1, distanceMatrixEntry.getLocId1());
				preparedDistanceMatrixInsertStatement.setLong(2, distanceMatrixEntry.getLocId2());
				preparedDistanceMatrixInsertStatement.setLong(3, distanceMatrixEntry.getDistance());
				preparedDistanceMatrixInsertStatement.execute();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			
		}
	}
	
	public void emptyBins() {
		// preparedBinEmptyStatement = conn.prepareStatement("UPDATE devices.bin AS b INNER JOIN distances.locations AS l SET filllevel=?, timestamp=? WHERE l.location_type=?;");
		java.util.Date d = new java.util.Date();
		try {
			preparedBinEmptyStatement.clearParameters();
			preparedBinEmptyStatement.setLong(1, 0L);
			preparedBinEmptyStatement.setTimestamp(2, new java.sql.Timestamp(d.getTime()));
			preparedBinEmptyStatement.setLong(3, this.locType);
			preparedBinEmptyStatement.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();			
		}
	}

}
