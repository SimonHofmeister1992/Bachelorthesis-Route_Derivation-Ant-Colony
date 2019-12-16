package de.routecalculation;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class TourDTO implements Future<TourDTO> {
	
    /*
     * ################################################
     * ########## ACO algorithms for the TSP ##########
     * ################################################
     * 
     * Version: 2.0
     * 
     * created by: Simon Hofmeister, date: 06.05.2019, reason: use aco algorithm as webservice
     * 
     * Purpose: return json as result of webservice
     */
	
    private long tourLength;
    private String route;
    private boolean isDone = false;
    private boolean isCancelled = true;
    private long timeInMiliSeconds;

    public long getTourLength() {
        return tourLength;
    }

    public String getRoute() {
        return route;
    }

	@Override
	public boolean cancel(boolean arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public TourDTO get() throws InterruptedException, ExecutionException {
		// TODO Auto-generated method stub
		return this;
	}

	@Override
	public TourDTO get(long arg0, TimeUnit arg1) throws InterruptedException, ExecutionException, TimeoutException {
		// TODO Auto-generated method stub
		return this;
	}

	@Override
	public boolean isCancelled() {
		// TODO Auto-generated method stub
		return isCancelled;
	}

	public void setCancelled(boolean isCancelled) {
		this.isCancelled = isCancelled;
	}
	
	public void setDone(boolean isDone) {
		this.isDone = isDone;
	}
	
	@Override
	public boolean isDone() {
		return isDone;
	}

	public void setTourLength(long tourLength) {
		this.tourLength = tourLength;
	}

	public void setRoute(String route) {
		this.route = route;
	}

	public long getTimeInMiliSeconds() {
		return timeInMiliSeconds;
	}

	public void setTimeInMiliSeconds(long timeInMiliSeconds) {
		this.timeInMiliSeconds = timeInMiliSeconds;
	}

	
}
