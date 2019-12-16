package de.routecalculation;


import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import de.routecalculation.singledepot.adrianwilke.acotspjava.AcoTspCallable;

	@RestController
	public class RESTController {
		
		final int MIN_BUFFER_TIME_IN_SECONDS = 10;
		final double MAX_BUFFER_TIME_IN_PERCENT = 0.05;
		private ScheduledThreadPoolExecutor executorService;
	    /*
	     * ################################################
	     * ########## ACO algorithms for the TSP ##########
	     * ################################################
	     * 
	     * Version: 2.0
	     * 
	     * created by: Simon Hofmeister, date: 06.05.2019, reason: solving of more than 1 aco instance at a time is now possible, implemented as webservice
	     * 
	     * Purpose: webservices for tsp calculation, i.e. aco (ant colony optimization)
	     */
		
		public RESTController() {
			executorService = new ScheduledThreadPoolExecutor(Application.MAX_THREADS);
			executorService.setRemoveOnCancelPolicy(true);			
		}

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
		 * purpose: calculate single-depot route with ant colony optimization algorithm
		 * interpretation of parameters: see adrianwilke.acotspjava: Parse.java
		 * input: parameters (see request parameters), 
		 *        locationsToVisit in Form "locId_1,locId_2,locId_3,...,locId_n" 
		 * 		  where the locIds are the locations to visit.
		 * output: Tour-Object in JSON with params 
		 * 		   tourLength (Long), 
		 * 		   route (String): in Form: "[locId_1,locId_2,locId_3,...,locId_n]" -> locationsToVisit sorted
		 * 		   isCancelled (Boolean): true if the calculation was aborted due to an interrupt
		 * 		   isDone (Boolean): true if the calculation finished successfully
		 * 
		 */	
	    @GetMapping(path="/single-depot/aco")
	    public ResponseEntity<TourDTO> calculateRoute(@RequestParam(value="acs", defaultValue="true") boolean acs,
	    		@RequestParam(value="tries", defaultValue="1") int numOfTries,
	    		@RequestParam(value="maxSecondsPerTry", defaultValue="5") int maxSecondsPerTry,
	    		@RequestParam(value="localSearchMode", defaultValue="0") int localSearchMode, //0=disables, 1=2opt, 2=2.5opt, 3=3opt
	    		@RequestParam(value="quietMode", defaultValue="true") boolean quietMode,
	    		@RequestParam(value="startLocationId", defaultValue="-1") long startLocationId,
	    		@RequestParam(value="locationsToVisit") String locationsToVisit,
	    		@RequestParam(value="tours",defaultValue="-1") int tours, // number of steps in each trial
	    		@RequestParam(value="optimum", defaultValue="false") Boolean optimum, 
	    		@RequestParam(value="numberOfAnts", defaultValue="10") int numberOfAnts, 
	    		@RequestParam(value="nearestNeighbours", defaultValue="-1") int nearestNeighbours, 
	    		@RequestParam(value="alpha", defaultValue="-1") int alpha, 
	    		@RequestParam(value="beta", defaultValue="-1") int beta, 
	    		@RequestParam(value="rho", defaultValue="-1") int rho, 
	    		@RequestParam(value="elitistants", defaultValue="-1") int elitistants, 
	    		@RequestParam(value="rasranks", defaultValue="-1") int rasranks, 
	    		@RequestParam(value="nnls", defaultValue="-1") int nnls, 
	    		@RequestParam(value="dlb", defaultValue="false") boolean dlb, 
	    		@RequestParam(value="eas", defaultValue="false") boolean eas, 
	    		@RequestParam(value="as", defaultValue="false") boolean as, 
	    		@RequestParam(value="ras", defaultValue="false") boolean ras, 
	    		@RequestParam(value="mmas", defaultValue="false") boolean mmas, 
	    		@RequestParam(value="bwas", defaultValue="false") boolean bwas, 
	    		@RequestParam(value="probabilityOutput", defaultValue="false") boolean probabilityOutput, 
	    		@RequestParam(value="randomNumbersOfLocationsToDrop", defaultValue="2") int randomLocationsToDrop) {
	    	Date startTime = new Date();
	    	locationsToVisit = "[" + locationsToVisit.replace("\"", "") + "]";
	    	if(locationsToVisit.length() == 2) return new ResponseEntity<TourDTO>(HttpStatus.BAD_REQUEST);
	    	StringBuffer sb = new StringBuffer();
	    	if(acs) sb.append("--acs;");
	    	sb.append("--tries;" + numOfTries + ";");
	    	sb.append("--time;" + maxSecondsPerTry + ";");
	    	sb.append("--localsearch;" + localSearchMode + ";");
	    	if(tours>=0) sb.append("--tours;" + tours + ";");
	    	if(nearestNeighbours>=0) sb.append("--nnants;" + nearestNeighbours + ";");
	    	if(alpha>=0) sb.append("--a;" + alpha + ";");
	    	if(beta>=0) sb.append("--b;" + beta + ";");
	    	if(rho>=0) sb.append("--e;" + rho + ";");
	    	if(elitistants>=0) sb.append("--c;" + elitistants + ";");
	    	if(rasranks>=0) sb.append("--f;" + rasranks + ";");
	    	if(nnls>=0) sb.append("--nnls;" + nnls + ";");
	    	sb.append("--ants;" + numberOfAnts + ";");
	    	if(probabilityOutput) sb.append("--q;" + probabilityOutput + ";");
	    	if(optimum) sb.append("--o;" + optimum + ";");
	    	if(dlb) sb.append("--dlb;" + dlb + ";");
	    	if(as) sb.append("--as;" + as + ";");
	    	if(ras) sb.append("--ras;" + ras + ";");
	    	if(eas) sb.append("--eas;" + eas + ";");
	    	if(mmas) sb.append("--mmas;" + mmas + ";");
	    	if(bwas) sb.append("--bwas;" + bwas + ";");
	    	if(quietMode) sb.append("-quiet;");
	    	sb.append("-p;");
	    	if(randomLocationsToDrop > 1) sb.append(randomLocationsToDrop+ ";");
	    	else sb.append("2;");
	    	if(startLocationId < 0) startLocationId=Long.valueOf(locationsToVisit.replace("[", "").split(",")[0].trim());
	    	sb.append("-sl;" + startLocationId + ";");
	    	sb.append(locationsToVisit);
	    	String[] arguments = sb.toString().split(";");
	    	
	    	AcoTspCallable callable = new AcoTspCallable(arguments);
	    	int maxBufferTimeInSeconds = (int)((maxSecondsPerTry * numOfTries)* MAX_BUFFER_TIME_IN_PERCENT)+1;
	    	int bufferTimeInSeconds = maxBufferTimeInSeconds > MIN_BUFFER_TIME_IN_SECONDS ? maxBufferTimeInSeconds : MIN_BUFFER_TIME_IN_SECONDS;
	    	int neededTimeInSeconds =  (maxSecondsPerTry * numOfTries) + bufferTimeInSeconds;   	
	    	neededTimeInSeconds *=  1000;
	    	Date temp = new Date();
	    	long timeVal = startTime.getTime() + neededTimeInSeconds;
	    	temp.setTime(timeVal);
	    	System.out.println("time from: " + startTime);
	    	System.out.println("max time until: " + temp);
	    	Future<?> futureTour = executorService.submit(callable);
	    	TourDTO tour = new TourDTO();
			try {
				executorService.schedule(new Runnable() {
					public void run() {
						try {
							if(futureTour.get() == null) futureTour.cancel(true);
						} catch (InterruptedException e) {
							futureTour.cancel(true);
						} catch (ExecutionException e) {
							futureTour.cancel(true);
						}
					}
				}, neededTimeInSeconds, TimeUnit.MILLISECONDS);
				tour = (TourDTO) futureTour.get();
			} catch (InterruptedException e) {
				tour.setCancelled(true);
			} catch (ExecutionException e) {
				tour.setCancelled(true);
			}
			Date endTime = new Date();
			tour.setTimeInMiliSeconds(endTime.getTime()-startTime.getTime());
	    	return new ResponseEntity<TourDTO>(tour, HttpStatus.OK);
	    }    
	}

