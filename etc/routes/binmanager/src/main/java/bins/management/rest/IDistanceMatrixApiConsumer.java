package bins.management.rest;

import bins.management.entity.DistanceMatrixEntry;

public interface IDistanceMatrixApiConsumer {
	public DistanceMatrixEntry getDistanceByDistanceMatrixEntry(DistanceMatrixEntry dme);
}
