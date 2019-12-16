package bins.management.entity;

public class DistanceMatrixEntry {

	private Long locId1;
	private Long locId2;
	private String longLat1OrAddress1;
	private String longLat2OrAddress2;
	private Long distance;
	public Long getLocId1() {
		return locId1;
	}
	public void setLocId1(Long locId1) {
		this.locId1 = locId1;
	}
	public Long getLocId2() {
		return locId2;
	}
	public void setLocId2(Long locId2) {
		this.locId2 = locId2;
	}
	public String getLongLat1OrAddress1() {
		return longLat1OrAddress1;
	}
	public void setLongLat1OrAddress1(String longLat1OrAddress1) {
		this.longLat1OrAddress1 = longLat1OrAddress1;
	}
	public String getLongLat2OrAddress2() {
		return longLat2OrAddress2;
	}
	public void setLongLat2OrAddress2(String longLat2OrAddress2) {
		this.longLat2OrAddress2 = longLat2OrAddress2;
	}
	public Long getDistance() {
		return distance;
	}
	public void setDistance(Long distance) {
		this.distance = distance;
	}
	
	
}
