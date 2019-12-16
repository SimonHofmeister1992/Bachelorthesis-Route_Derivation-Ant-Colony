package de.routecalculation;


import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {

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
	
	public static int MIN_THREADS = 1; // default value
	public static int OPTIMUM_THREADS = 2; // default value
	public static int MAX_THREADS = 4; // default value
	
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
        
    	Options options = new Options();
    	options.addOption("min", "minThreads", false, "minimum number of threads which is tried to use on this server, default: 1");
    	options.addOption("opt", "optNumOfThreads", false, "optimum number of threads which should be used on this server, default: 2");
    	options.addOption("max", "maxThreads", false, "maximum number of threads which may be used on this server, default: 4");
        
    	CommandLine cmd = null;
    	CommandLineParser parser = new DefaultParser();
    	try {
    	    cmd = parser.parse(options, args);
    	} catch (ParseException e) {
    	    System.err.println("Error: " + e.getMessage());
    	    System.exit(1);
    	}
        
    	if (cmd.hasOption("min")) {
    		Application.MIN_THREADS = Integer.parseInt(cmd.getOptionValue("min"));
    	} else {
    	    System.out.println("Note: minimum number of threads shall be: " + Application.MIN_THREADS);
        }
    	
    	if (cmd.hasOption("opt")) {
    		Application.OPTIMUM_THREADS = Integer.parseInt(cmd.getOptionValue("opt"));
    	} else {
    	    System.out.println("Note: optimum number of threads will be: " + Application.OPTIMUM_THREADS);
        }
    	
    	if (cmd.hasOption("max")) {
    		Application.MAX_THREADS = Integer.parseInt(cmd.getOptionValue("max"));
    	} else {
    	    System.out.println("Note: maximum number of threads will be: " + Application.MAX_THREADS);
        }
    	
    	
    	// TODO register at LocalRegistry-Project (not java-one)
    }
}
