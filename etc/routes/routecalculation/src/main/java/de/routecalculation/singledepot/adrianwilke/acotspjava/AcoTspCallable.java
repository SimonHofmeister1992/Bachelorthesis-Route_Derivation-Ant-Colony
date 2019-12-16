package de.routecalculation.singledepot.adrianwilke.acotspjava;

import java.util.ArrayList;
import java.util.concurrent.Callable;

import de.routecalculation.Config;
import de.routecalculation.TourDTO;

public class AcoTspCallable implements Callable<TourDTO> {

    /*
     * ################################################
     * ########## ACO algorithms for the TSP ##########
     * ################################################
     * 
     * Version: 2.0
     * 
     * created by: Simon Hofmeister, date: 06.05.2019, reason: solving of more than 1 aco instance at a time is now possible, implemented as webservice
     * 
     * Purpose: start one instance of the aco calculation
     */
	
	
	private String[] arguments;

	public AcoTspCallable(String[] arguments) {
		this.arguments = arguments;
	}
	
    @Override
    public TourDTO call() throws Exception{
    	
    	TourDTO tour = new TourDTO();
    	
    	Config config = new Config();
    	Timer timer = new Timer();
    	LocalSearch localSearch = new LocalSearch();
    	Tsp tsp = localSearch.getTsp();
    	Ants ants = new Ants(timer, localSearch, tsp);
    	tsp.setAnts(ants);
    	InOut inOut = new InOut(timer, localSearch, tsp, ants, config);
    	tsp.setInOut(inOut);
    	ants.setInOut(inOut);
    	AcoTsp shorter = new AcoTsp(timer, inOut, ants, localSearch, tsp);
        String[] shortResult = shorter.runAco(false, arguments);
        shortResult[1] = translateToLocids(shortResult[1], inOut);
        if(inOut.startLocation >= 0) shortResult[1] = rotateResult(shortResult[1], inOut.startLocation);
        tour.setRoute(shortResult[1]);
        tour.setTourLength(Long.valueOf(shortResult[0]));
        tour.setCancelled(false);
        tour.setDone(true);
        return tour;
    }

	private static String translateToLocids(String result, InOut inOut) {
		result = result.replace("[", "").replace("]", "").replace(" ", "");
		String[] resultSplit = result.split(",");
		
		StringBuffer sb = new StringBuffer();
		sb.append("[");
		for(int i = 0; i < resultSplit.length; i++) {
			if(i!=0) sb.append(",");
			sb.append(String.valueOf(inOut.locIds[Integer.valueOf(resultSplit[i].trim())]));
		}
		sb.append("]");
		String locids = sb.toString();

 		return locids;
	}

	// rotates result so the roundtour will start at the startposition
	private static String rotateResult(String result, int startLocation) {
		
		String route;
		StringBuffer finalRoute = new StringBuffer();
		ArrayList<Integer> subRoute = new ArrayList<Integer>();
		
		route = result.replace("[", "").replace("]", "").replace(" ", "");
		for(String locid : route.split(",")) {
			subRoute.add(Integer.parseInt(locid.trim()));
		}
		
		if(!subRoute.get(0).equals(startLocation)) {
			// remove doubled "random start location"
			subRoute.remove(0);
			
			int temp = 0;
			// rotate as long until start location is first position
			while(!subRoute.get(0).equals(startLocation)) {
				temp = subRoute.get(0);
				subRoute.remove(0);
				subRoute.add(temp);
			}
			subRoute.add(startLocation);
			finalRoute.append("[");
			for(int i = 0; i < subRoute.size(); i++) {
				if(i != 0) finalRoute.append(",");
				finalRoute.append(subRoute.get(i));
			}
			finalRoute.append("]");
		}
		
		return finalRoute.toString();
	}
}
