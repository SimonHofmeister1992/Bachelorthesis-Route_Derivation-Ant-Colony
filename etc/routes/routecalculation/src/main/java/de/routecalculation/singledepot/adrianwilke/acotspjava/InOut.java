package de.routecalculation.singledepot.adrianwilke.acotspjava;


import java.io.*;
import java.util.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;

import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

import de.routecalculation.Config;
import de.routecalculation.singledepot.adrianwilke.acotspjava.Tsp.problem;

/**
 * ACO algorithms for the TSP
 * 
 * This code is based on the ACOTSP project of Thomas Stuetzle.
 * It was initially ported from C to Java by Adrian Wilke.
 * 
 * Project website: http://adibaba.github.io/ACOTSPJava/
 * Source code: https://github.com/adibaba/ACOTSPJava/
 */
public class InOut {
    /*
     * ################################################
     * ########## ACO algorithms for the TSP ##########
     * ################################################
     * 
     * Version: 2.0
     * 
     * modified by: Simon Hofmeister, date: 06.05.2019, reason: solving of more than 1 aco instance at a time is now possible, implemented as webservice
     * 
     * Author: Thomas Stuetzle
     * Purpose: mainly input / output / statistic routines
     * Check: README and gpl.txt
     * Copyright (C) 2002 Thomas Stuetzle
     */

    /***************************************************************************
     * Program's name: acotsp
     * 
     * Ant Colony Optimization algorithms (AS, ACS, EAS, RAS, MMAS, BWAS) for the
     * symmetric TSP
     * 
     * Copyright (C) 2004 Thomas Stuetzle
     * 
     * This program is free software; you can redistribute it and/or modify
     * it under the terms of the GNU General Public License as published by
     * the Free Software Foundation; either version 2 of the License, or
     * (at your option) any later version.
     * 
     * This program is distributed in the hope that it will be useful,
     * but WITHOUT ANY WARRANTY; without even the implied warranty of
     * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
     * GNU General Public License for more details.
     * 
     * You should have received a copy of the GNU General Public License
     * along with this program; if not, write to the Free Software
     * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
     * 
     * email: stuetzle no@spam informatik.tu-darmstadt.de
     * mail address: Universitaet Darmstadt
     * Fachbereich Informatik
     * Hochschulstr. 10
     * D-64283 Darmstadt
     * Germany
     ***************************************************************************/

	private String db_server_conn;
	private String db_name;
	private String db_user;
	private String db_pw;
	
	
    enum Distance_type {
	EUC_2D, CEIL_2D, GEO, ATT, EXPLICIT
    };

    Distance_type distance_type;

    public static final String PROG_ID_STR = "ACO algorithms for the TSP";

    int[] best_in_try;
    int[] best_found_at;
    double[] time_best_found;
    double[] time_total_run;
    String[] aw_best_tour_in_try;

    int n_try; /* try counter */
    int n_tours; /* counter of number constructed tours */
    int iteration; /* iteration counter */
    int restart_iteration; /* remember iteration when restart was done if any */
    double restart_time; /* remember time when restart was done if any */
    int max_tries; /* maximum number of independent tries */
    int max_tours; /* maximum number of tour constructions in one try */
    double lambda; /* Parameter to determine branching factor */
    double branch_fac; /* If branching factor < branch_fac => update trails */

    double max_time; /* maximal allowed run time of a try */
    double time_used; /* time used until some given event */
    double time_passed; /* time passed until some moment */
    int optimal; /* optimal solution or bound to find */

    double mean_ants; /* average tour length */
    double stddev_ants; /* stddev of tour lengths */
    double branching_factor; /* average node branching factor when searching */
    double found_branching; /* branching factor when best solution is found */

    int found_best; /* iteration in which best solution is found */
    int restart_found_best;/* iteration in which restart-best solution is found */

    String mysql_url;
	Connection conn;
	Statement stmt;
	ResultSet rs;

	int[] locIds;
	int[] remlocIds;
	int nLocIdsToDrop;
	int startLocation;

	/* ------------------------------------------------------------------------ */

    File report, comp_report, stat_report;
    Map<String, BufferedWriter> writer = new HashMap<String, BufferedWriter>();

    String name_buf;
    int opt;
    boolean quiet_flag; /* --quiet was given in the command-line. */

    private Timer timer;
    private LocalSearch localSearch;
    private Parse parse;
    private Tsp tsp;
    private Ants ants;
    
    public InOut(Timer timer, LocalSearch localSearch, Tsp tsp, Ants ants, Config config) {
    	this.timer = timer;
    	this.localSearch = localSearch;
    	this.parse = new Parse(localSearch, this, ants);
    	this.tsp = tsp;
    	this.ants = ants;
    	
    	this.db_server_conn=config.getProperty("mDbHost");
    	this.db_name=config.getProperty("mDbName");
    	this.db_user=config.getProperty("mDbUser");
    	this.db_pw=config.getProperty("mDbPwd");
    	
    	mysql_url = "jdbc:mysql://" + db_server_conn + "/" + db_name + "?useSSL=false" +
				  "&useUnicode=true&useJDBCCompliantTimezoneShift=true" + "" +
                  "&useLegacyDatetimeCode=false" +
                  "&serverTimezone=UTC";
    	
    }

    public Parse getParse() {
    	return this.parse;
    }
    
    Tsp.point[] read_etsp(String tsp_file_name) throws IOException
    /*
     * FUNCTION: parse and read tsp.instance file
     * INPUT: tsp.instance name
     * OUTPUT: list of coordinates for all nodes
     * COMMENTS: Instance files have to be in TSPLIB format, otherwise procedure fails
     */ {
		String buf;
		int i;
		Tsp.point[] nodeptr = null;

		if (tsp_file_name == null) {
			System.err.println("No instance file specified, abort");
			System.exit(1);
		}

		if (!new File(tsp_file_name).canRead()) {
			System.err.println("Can not read file " + tsp_file_name);
			System.exit(1);
		}

		System.out.println("\nreading tsp-file " + tsp_file_name + " ... ");

		i = 0;
		boolean found_coord_section = false;
		boolean foundEdgeWeightSection = false;
		Reader reader = new InputStreamReader(new FileInputStream(tsp_file_name), "UTF8");
		BufferedReader bufferedReader = new BufferedReader(reader);
		String line = bufferedReader.readLine();
		while (line != null) {

			if (line.startsWith("EOF")) {
				break;
			}

			if (!found_coord_section && !foundEdgeWeightSection) {
				if (line.startsWith("NAME")) {
					tsp.instance.name = line.split(":")[1].trim();
				} else if (line.startsWith("COMMENT")) {
				} else if (line.startsWith("TYPE") && !line.contains("tsp") && !line.contains("ATSP")) {
					System.err.println("Not a TSP tsp.instance in TSPLIB format !!");
					System.exit(1);
				} else if (line.startsWith("DIMENSION")) {
					tsp.n = Integer.parseInt(line.split(":")[1].trim());
					tsp.instance.n = tsp.n;
					nodeptr = new Tsp.point[tsp.n];
					assert (tsp.n > 2 && tsp.n < 6000);
				} else if (line.startsWith("DISPLAY_DATA_TYPE")) {
				} else if (line.startsWith("EDGE_WEIGHT_TYPE")) {
					buf = line.split(":")[1].trim();
					if (buf.equals("EUC_2D")) {
						distance_type = Distance_type.EUC_2D;
					} else if (buf.equals("CEIL_2D")) {
						distance_type = Distance_type.CEIL_2D;
					} else if (buf.equals("GEO")) {
						distance_type = Distance_type.GEO;
					} else if (buf.equals("ATT")) {
						distance_type = Distance_type.ATT;
					} else if (buf.equals("EXPLICIT")) {
						distance_type = Distance_type.EXPLICIT;
						tsp.instance.distance = new int[tsp.instance.n][tsp.instance.n];
					} else {
						System.err.println("EDGE_WEIGHT_TYPE " + buf + " not implemented");
						System.exit(1);
					}
				}
			} else if (found_coord_section) {
				String[] city_info = line.split(" ");
				nodeptr[i] = new Tsp.point();
				nodeptr[i].x = Double.parseDouble(city_info[1]);
				nodeptr[i].y = Double.parseDouble(city_info[2]);
				i++;
			} else if (foundEdgeWeightSection) {
				int j = 0;
				String distances[] = line.trim().replaceAll("\\s+", " ").split(" ");
				for(String dist: distances){
					tsp.instance.distance[i][j] = Integer.parseInt(dist);
					j++;
				}
				i++;
			}

			if (line.startsWith("NODE_COORD_SECTION")) {
				found_coord_section = true;
			}

			if (line.startsWith("EDGE_WEIGHT_SECTION")){
				foundEdgeWeightSection = true;
			}
			if (line.startsWith("EDGE_WEIGHT_FORMAT") && distance_type == Distance_type.EXPLICIT
					&& !line.contains("FULL_MATRIX")) {
				System.err.println("Only FULL_MATRIX supported, when using explicit distances");
				System.exit(1);
			}
			line = bufferedReader.readLine();
		}

		if (!found_coord_section && !foundEdgeWeightSection) {
			System.err.println("Some error ocurred finding start of coordinates from tsp file !!");
			System.exit(1);
		}

		bufferedReader.close();

		// TRACE ( System.out.println("number of cities is %ld\n",tsp.n); )
		// TRACE ( System.out.println("\n... done\n"); )
		System.out.println();

		return (nodeptr);

	}

    void write_report()
    /*
     * FUNCTION: output some info about trial (best-so-far solution quality, time)
     * INPUT: none
     * OUTPUT: none
     * COMMENTS: none
     */
    {
	System.out.println("best " + ants.best_so_far_ant.tour_length + ", iteration: " + iteration + ", time "
		+ timer.elapsed_time());
	if (comp_report != null)
	    printToFile(comp_report, "best " + ants.best_so_far_ant.tour_length + "\t iteration " + iteration
		    + "\t tours " + n_tours + "\t time " + time_used);
    }

    void fprintf_parameters(File file) {
	printToFile(file, "max_tries\t\t " + max_tries);
	printToFile(file, "max_tours\t\t " + max_tours);
	printToFile(file, "max_time\t\t " + max_time);
	printToFile(file, "Utilities.seed\t\t " + Utilities.seed);
	printToFile(file, "optimum\t\t\t " + optimal);
	printToFile(file, "n_ants\t\t\t " + ants.n_ants);
	printToFile(file, "ants.nn_ants\t\t " + ants.nn_ants);
	printToFile(file, "ants.alpha\t\t " + ants.alpha);
	printToFile(file, "ants.beta\t\t " + ants.beta);
	printToFile(file, "ants.rho\t\t " + ants.rho);
	printToFile(file, "ants.q_0\t\t " + ants.q_0);
	printToFile(file, "ants.elitist_ants\t " + ants.elitist_ants);
	printToFile(file, "ants.ras_ranks\t\t " + ants.ras_ranks);
	printToFile(file, "LocalSearch.ls_flag\t " + localSearch.ls_flag);
	printToFile(file, "LocalSearch.nn_ls\t " + localSearch.nn_ls);
	printToFile(file, "LocalSearch.dlb_flag\t " + localSearch.dlb_flag);
	printToFile(file, "ants.as_flag\t\t " + ants.as_flag);
	printToFile(file, "ants.eants.as_flag\t " + ants.eas_flag);
	printToFile(file, "rants.as_flag\t\t " + ants.ras_flag);
	printToFile(file, "mmants.as_flag\t\t " + ants.mmas_flag);
	printToFile(file, "ants.bwants.as_flag\t " + ants.bwas_flag);
	printToFile(file, "ants.acs_flag\t\t " + ants.acs_flag);
    }

    void print_default_parameters()
    /*
     * FUNCTION: output default parameter settings
     * INPUT: none
     * OUTPUT: none
     * COMMENTS: none
     */
    {
	System.err.println("\nDefault parameter settings are:\n\n");
	fprintf_parameters(null);
    }

    void set_default_as_parameters() {
	assert (ants.as_flag);
	ants.n_ants = -1; /* number of ants (-1 means tsp.instance size) */
	ants.nn_ants = 20; /* number of nearest neighbours in tour construction */
	ants.alpha = 1.0;
	ants.beta = 2.0;
	ants.rho = 0.5;
	ants.q_0 = 0.0;
	ants.ras_ranks = 0;
	ants.elitist_ants = 0;
    }

    void set_default_eas_parameters() {
	assert (ants.eas_flag);
	ants.n_ants = -1; /* number of ants (-1 means tsp.instance size) */
	ants.nn_ants = 20; /* number of nearest neighbours in tour construction */
	ants.alpha = 1.0;
	ants.beta = 2.0;
	ants.rho = 0.5;
	ants.q_0 = 0.0;
	ants.ras_ranks = 0;
	ants.elitist_ants = ants.n_ants;
    }

    void set_default_ras_parameters() {
	assert (ants.ras_flag);
	ants.n_ants = -1; /* number of ants (-1 means tsp.instance size) */
	ants.nn_ants = 20; /* number of nearest neighbours in tour construction */
	ants.alpha = 1.0;
	ants.beta = 2.0;
	ants.rho = 0.1;
	ants.q_0 = 0.0;
	ants.ras_ranks = 6;
	ants.elitist_ants = 0;
    }

    void set_default_bwas_parameters() {
	assert (ants.bwas_flag);
	ants.n_ants = -1; /* number of ants (-1 means tsp.instance size) */
	ants.nn_ants = 20; /* number of nearest neighbours in tour construction */
	ants.alpha = 1.0;
	ants.beta = 2.0;
	ants.rho = 0.1;
	ants.q_0 = 0.0;
	ants.ras_ranks = 0;
	ants.elitist_ants = 0;
    }

    void set_default_mmas_parameters() {
	assert (ants.mmas_flag);
	ants.n_ants = -1; /* number of ants (-1 means tsp.instance size) */
	ants.nn_ants = 20; /* number of nearest neighbours in tour construction */
	ants.alpha = 1.0;
	ants.beta = 2.0;
	ants.rho = 0.02;
	ants.q_0 = 0.0;
	ants.ras_ranks = 0;
	ants.elitist_ants = 0;
    }

    void set_default_acs_parameters() {
	assert (ants.acs_flag);

	ants.n_ants = 10; /* number of ants (-1 means tsp.instance size) */
	if (tsp.n < 20) {
		ants.nn_ants = tsp.n;
	} else {
		ants.nn_ants = 20;
	}

	ants.alpha = 1.0;
	ants.beta = 2.0;
	ants.rho = 0.1;
	ants.q_0 = 0.9;
	ants.ras_ranks = 0;
	ants.elitist_ants = 0;
    }

    void set_default_ls_parameters() {
	assert (localSearch.ls_flag != 0);
	localSearch.dlb_flag = true; /* apply don't look bits in local search */
	localSearch.nn_ls = 20; /* use fixed radius search in the 20 nearest neighbours */

	ants.n_ants = 25; /* number of ants */
	ants.nn_ants = 20; /* number of nearest neighbours in tour construction */
	ants.alpha = 1.0;
	ants.beta = 2.0;
	ants.rho = 0.5;
	ants.q_0 = 0.0;

	if (ants.mmas_flag) {
	    ants.n_ants = 25;
	    ants.rho = 0.2;
	    ants.q_0 = 0.00;
	} else if (ants.acs_flag) {
	    ants.n_ants = 10;
	    ants.rho = 0.1;
	    ants.q_0 = 0.98;
	} else if (ants.eas_flag) {
	    ants.elitist_ants = ants.n_ants;
	}
    }

    void set_default_parameters()
    /*
     * FUNCTION: set default parameter settings
     * INPUT: none
     * OUTPUT: none
     * COMMENTS: none
     */
    {
    localSearch.ls_flag = 3; /* per default run 3-opt */
    localSearch.dlb_flag = true; /* apply don't look bits in local search */
    localSearch.nn_ls = 20; /* use fixed radius search in the 20 nearest neighbours */
	ants.n_ants = 25; /* number of ants */
	ants.nn_ants = 20; /* number of nearest neighbours in tour construction */
	ants.alpha = 1.0;
	ants.beta = 2.0;
	ants.rho = 0.5;
	ants.q_0 = 0.0;
	max_tries = 10;
	max_tours = 0;
	Utilities.seed = (int) System.currentTimeMillis();
	max_time = 10.0;
	optimal = 1;
	branch_fac = 1.00001;
	ants.u_gb = Integer.MAX_VALUE;
	ants.as_flag = false;
	ants.eas_flag = false;
	ants.ras_flag = false;
	ants.mmas_flag = true;
	ants.bwas_flag = false;
	ants.acs_flag = false;
	ants.ras_ranks = 0;
	ants.elitist_ants = 0;
    }

    void population_statistics()
    /*
     * FUNCTION: compute some population statistics like average tour length,
     * standard deviations, average distance, branching-factor and
     * output to a file gathering statistics
     * INPUT: none
     * OUTPUT: none
     * (SIDE)EFFECTS: none
     */
    {
	int j, k;
	int[] l;
	double pop_mean, pop_stddev, avg_distance = 0.0;

	l = new int[ants.n_ants];
	for (k = 0; k < ants.n_ants; k++) {
	    l[k] = ants.ant[k].tour_length;
	}

	pop_mean = Utilities.mean(l, ants.n_ants);
	pop_stddev = Utilities.std_deviation(l, ants.n_ants, pop_mean);
	branching_factor = node_branching(lambda);

	for (k = 0; k < ants.n_ants - 1; k++)
	    for (j = k + 1; j < ants.n_ants; j++) {
		avg_distance += ants.distance_between_ants(ants.ant[k], ants.ant[j]);
	    }
	avg_distance /= ((double) ants.n_ants * (double) (ants.n_ants - 1) / 2.);

	if (stat_report != null) {
	    printToFile(stat_report, iteration + "\t" + pop_mean + "\t" + pop_stddev + "\t" + (pop_stddev / pop_mean)
		    + "\t" + branching_factor + "\t" + ((branching_factor - 1.) * tsp.n) + "\t" + avg_distance
		    + "\t" + (avg_distance / tsp.n));
	}
    }

    double node_branching(double l)
    /*
     * FUNCTION: compute the average node lambda-branching factor
     * INPUT: lambda value
     * OUTPUT: average node branching factor
     * (SIDE)EFFECTS: none
     * COMMENTS: see the ACO book for a definition of the average node
     * lambda-branching factor
     */
    {
	int i, m;
	double min, max, cutoff;
	double avg;
	double[] num_branches;

	num_branches = new double[tsp.n];

	for (m = 0; m < tsp.n; m++) {
	    /* determine max, min to calculate the cutoff value */
	    min = ants.pheromone[m][tsp.instance.nn_list[m][1]];
	    max = ants.pheromone[m][tsp.instance.nn_list[m][1]];
	    for (i = 0; i < ants.nn_ants-1; i++) {
		if (ants.pheromone[m][tsp.instance.nn_list[m][i]] > max)
		    max = ants.pheromone[m][tsp.instance.nn_list[m][i]];
		if (ants.pheromone[m][tsp.instance.nn_list[m][i]] < min)
		    min = ants.pheromone[m][tsp.instance.nn_list[m][i]];
	    }
	    cutoff = min + l * (max - min);

	    for (i = 0; i < ants.nn_ants; i++) {
		if (ants.pheromone[m][tsp.instance.nn_list[m][i]] > cutoff)
		    num_branches[m] += 1.;
	    }
	}
	avg = 0.;
	for (m = 0; m < tsp.n; m++) {
	    avg += num_branches[m];
	}
	/* Norm branching factor to minimal value 1 */
	return (avg / (tsp.n * 2));
    }

    void output_solution()
    /*
     * FUNCTION: output a solution together with node coordinates
     * INPUT: none
     * OUTPUT: none
     * COMMENTS: not used in the default implementation but may be useful anyway
     */
    {

	int i;
	if (stat_report != null) {
	    for (i = 0; i < tsp.n; i++) {
		printToFile(stat_report, ants.best_so_far_ant.tour[i] + " "
			+ tsp.instance.nodeptr[ants.best_so_far_ant.tour[i]].x + " "
			+ tsp.instance.nodeptr[ants.best_so_far_ant.tour[i]].y);
	    }

	}
    }

    void exit_try(int ntry)
    /*
     * FUNCTION: save some statistical information on a trial once it finishes
     * INPUT: trial number
     * OUTPUT: none
     * COMMENTS:
     */
    {
	checkTour(ants.best_so_far_ant.tour);
	/* printTourFile( best_so_far_ant.tour ); */

	System.out.println("Best Solution in try " + ntry + " is " + ants.best_so_far_ant.tour_length);

	if (report != null)
	    printToFile(report, "Best: " + ants.best_so_far_ant.tour_length + "\t Iterations: " + found_best
		    + "\t B-Fac " + found_branching + "\t Time " + time_used + "\t Tot.time " + timer.elapsed_time());
	System.out.println(" Best Solution was found after " + found_best + " iterations\n");

	best_in_try[ntry] = ants.best_so_far_ant.tour_length;
	best_found_at[ntry] = found_best;
	time_best_found[ntry] = time_used;
	time_total_run[ntry] = timer.elapsed_time();
	aw_best_tour_in_try[ntry] = Arrays.toString(ants.best_so_far_ant.tour);

	System.out.println("\ntry " + ntry + ", Best " + best_in_try[ntry] + ", found at iteration "
		+ best_found_at[ntry] + ", found at time " + time_best_found[ntry] + "\n");

	if (comp_report != null)
	    printToFile(comp_report, "end try " + ntry + "\n");
	if (stat_report != null)
	    printToFile(stat_report, "end try " + ntry + "\n");
	// TRACE (output_solution();)

    }

    void exit_program()
    /*
     * FUNCTION: save some final statistical information on a trial once it finishes
     * INPUT: none
     * OUTPUT: none
     * COMMENTS:
     */
    {
	int best_tour_length, worst_tour_length;
	double t_avgbest, t_stdbest, t_avgtotal, t_stdtotal;
	double avg_sol_quality = 0., avg_cyc_to_bst = 0., stddev_best, stddev_iterations;

	best_tour_length = Utilities.best_of_vector(best_in_try, max_tries);
	worst_tour_length = Utilities.worst_of_vector(best_in_try, max_tries);

	avg_cyc_to_bst = Utilities.mean(best_found_at, max_tries);
	stddev_iterations = Utilities.std_deviation(best_found_at, max_tries, avg_cyc_to_bst);

	avg_sol_quality = Utilities.mean(best_in_try, max_tries);
	stddev_best = Utilities.std_deviation(best_in_try, max_tries, avg_sol_quality);

	t_avgbest = Utilities.meanr(time_best_found, max_tries);
	System.out.println(" t_avgbest = " + t_avgbest);
	t_stdbest = Utilities.std_deviationr(time_best_found, max_tries, t_avgbest);

	t_avgtotal = Utilities.meanr(time_total_run, max_tries);
	System.out.println(" t_avgtotal = " + t_avgtotal);
	t_stdtotal = Utilities.std_deviationr(time_total_run, max_tries, t_avgtotal);

	if (report != null) {
	    printToFile(report, "\nAverage-Best: " + avg_sol_quality + "\t Average-Iterations: " + avg_cyc_to_bst);
	    printToFile(report, "Stddev-Best: " + stddev_best + " \t Stddev Iterations: " + stddev_iterations);
	    printToFile(report, "Best try: " + best_tour_length + "\t\t Worst try: " + worst_tour_length);
	    printToFile(report, "\nAvg.time-best: " + t_avgbest + " stddev.time-best: " + t_stdbest);
	    printToFile(report, "\nAvg.time-ants.total: " + t_avgtotal + " stddev.time-ants.total: " + t_stdtotal);

	    if (optimal > 0) {
		printToFile(report, " excess best = " + ((double) (best_tour_length - optimal) / (double) optimal)
			+ ", excess average = " + ((avg_sol_quality - optimal) / optimal) + ","
			+ " excess worst = " + ((double) (worst_tour_length - optimal) / (double) optimal));
	    }
	}

	if (comp_report != null)
	    printToFile(comp_report, "end problem " + tsp.instance.name);

	for (String key : writer.keySet()) {
	    try {
		writer.get(key).close();
	    } catch (IOException e) {
		System.err.println("Could not close file " + key + " " + e.getMessage());
	    }
	}
    }
    
    static boolean contains(int[] arr, int item) {
      for (int n : arr) {
         if (item == n) {
            return true;
         }
      }
      return false;
   }

    void init_program(Boolean full, String[] args)
    /*
     * FUNCTION: initialize the program,
     * INPUT: program arguments, needed for parsing commandline
     * OUTPUT: none
     * COMMENTS:
     */
    {
	tsp.instance = new problem();

	String temp_buffer;
        String remlist = "";
	System.out.println(InOut.PROG_ID_STR);
	set_default_parameters();
	parse.parse_commandline(args);

	assert (max_tries <= Utilities.MAXIMUM_NO_TRIES);

	best_in_try = new int[max_tries];
	best_found_at = new int[max_tries];
	time_best_found = new double[max_tries];
	time_total_run = new double[max_tries];

	aw_best_tour_in_try = new String[max_tries];
        
        for(String arg: args) {
            if(arg.contains("["))
            {
                arg = arg.replace("[", "");
                arg = arg.replace("]", "");
                remlist = arg;
            }
        }

	// TRACE ( System.out.println("read problem data  ..\n\n");
	try {
		conn = DriverManager.getConnection(mysql_url, db_user, db_pw);
		stmt = conn.createStatement();
		int n = 0;
		Random rand = new Random();
		rand.setSeed(System.currentTimeMillis());
		rs = stmt.executeQuery("SELECT count(*) as n FROM locations");
		if (rs.next()) {
			n = rs.getInt("n");
		}
		locIds = new int[n];
		rs = stmt.executeQuery("SELECT loc_id FROM locations");

		for (int i = 0; rs.next(); i++) {
			locIds[i] = rs.getInt("loc_id");
		}

		//for (int i = 0; i <= nLocIdsToDrop; i++) {
			//locIds = ArrayUtils.remove(locIds, rand.nextInt(locIds.length));
	//	}

                if(full == false) {

                	if(remlist.length() > 0) {
                    List<String> tmpList = Arrays.asList(remlist.split(","));
                    List<Integer> uniquelocs = new ArrayList<Integer>();
                    int locid= -1;    
                    for(String i: tmpList)
                        {
/*                            if(contains(locIds, Integer.parseInt(i)))
                            {
                                locIds = ArrayUtils.remove(locIds, Integer.parseInt(i));
                            }
 */
                        	locid = Integer.parseInt(i);
                        	if(!uniquelocs.contains(locid)) {
                        		uniquelocs.add(locid);
                        	}

                        }
                        remlocIds = new int[uniquelocs.size()];
                        for(int pos = 0; pos < uniquelocs.size(); pos++) {
                        	remlocIds[pos] = uniquelocs.get(pos);
                        }

                	}
                        locIds = remlocIds;
                    }
                
	} catch (SQLException ex){
		System.out.println("Initializing failed.");
		System.out.println("SQLException: " + ex.getMessage());
	}

	createTspFileFromDB(locIds);
	try {
	    tsp.instance.nodeptr = read_etsp(name_buf);
		printDistanceMatrix();
	} catch (IOException e) {
	    System.err.println("Could not read input file. " + e.getMessage());
	    System.exit(1);
	}

	// TRACE ( System.out.println("\n .. done\n\n"); )

	if (ants.n_ants < 0)
	    ants.n_ants = tsp.n;
	/*
	 * default setting for ants.elitist_ants is 0; if EAS is applied and
	 * option ants.elitist_ants is not used, we set the default to
	 * ants.elitist_ants = n
	 */
	if (ants.eas_flag && ants.elitist_ants <= 0)
	    ants.elitist_ants = tsp.n;

	localSearch.nn_ls = Math.min(tsp.n - 1, localSearch.nn_ls);

	assert (ants.n_ants < Ants.MAX_ANTS - 1);
	assert (ants.nn_ants < Ants.MAX_NEIGHBOURS);
	assert (ants.nn_ants > 0);
	assert (localSearch.nn_ls > 0);

	if (!quiet_flag) {
	    Writer w;
	    try {
		temp_buffer = "best." + tsp.instance.name;
		// // TRACE ( System.out.println("%s\n",temp_buffer); )
		report = new File(temp_buffer);
		w = new OutputStreamWriter(new FileOutputStream(temp_buffer), "UTF8");
		writer.put(report.getName(), new BufferedWriter(w));

		temp_buffer = "cmp." + tsp.instance.name;
		// // TRACE ( System.out.println("%s\n",temp_buffer); )
		comp_report = new File(temp_buffer);
		w = new OutputStreamWriter(new FileOutputStream(temp_buffer), "UTF8");
		writer.put(comp_report.getName(), new BufferedWriter(w));

		temp_buffer = "stat." + tsp.instance.name;
		// // TRACE ( System.out.println("%s\n",temp_buffer); )
		stat_report = new File(temp_buffer);
		w = new OutputStreamWriter(new FileOutputStream(temp_buffer), "UTF8");
		writer.put(stat_report.getName(), new BufferedWriter(w));
	    } catch (IOException e) {
		System.err.println("Could not write file. " + e.getMessage());
		System.exit(1);
	    }
	} else {
	    report = null;
	    comp_report = null;
	    stat_report = null;
	}
	if (distance_type == Distance_type.EXPLICIT) {
		System.out.println("no need to calculate distance matrix, it is given explicitly.");
	} else {
		System.out.println("calculating distance matrix ..");
		tsp.instance.distance = tsp.compute_distances();
		System.out.println(" .. done\n");
	}
	write_params();

	if (comp_report != null)
	    printToFile(comp_report, "begin problem " + name_buf);
	System.out.println("allocate ants' memory ..");
	ants.allocate_ants();
	System.out.println(" .. done\n");
    }

    void printDist()
    /*
     * FUNCTION: print distance matrix
     * INPUT: none
     * OUTPUT: none
     */
    {
	int i, j;

	System.out.println("Distance Matrix:\n");
	for (i = 0; i < tsp.n; i++) {
	    System.out.println("From " + i);
	    for (j = 0; j < tsp.n - 1; j++) {
		System.out.println(" " + tsp.instance.distance[i][j]);
	    }
	    System.out.println(" " + tsp.instance.distance[i][tsp.n - 1]);
	    System.out.println("\n");
	}
	System.out.println("\n");
    }

    void printDistanceMatrix() {
        System.out.println("Distance Matrix:");
        for (int i = 0; i < tsp.instance.n; i++) {
			for (int j = 0; j < tsp.instance.n; j++) {
        	System.out.print(Integer.toString(tsp.instance.distance[i][j]) + "\t");
			}
			System.out.println("");
		}
    }

    void writeNormalizedPheromones() {
        double maxPheromone = Double.MIN_VALUE;
        // Search maximum
        for (int i = 0; i < tsp.n; i++){
            for (int j = 0; j < tsp.n; j++){
                if (ants.total[i][j] > maxPheromone)
                     maxPheromone = ants.total[i][j];
            }
        }
        System.out.println("Max. pheromone: " + Double.toString(maxPheromone));

        // Normalize by dividing every pheromone p / maxPheromone and write to file
        try {
        	conn = DriverManager.getConnection(mysql_url, db_user, db_pw);
            stmt = conn.createStatement();

            for (int i = 0; i < tsp.n; i++){
                for (int j = 0; j < tsp.n; j++){
                	String query = "INSERT INTO pheromones (from_loc_id,"
							+ "to_loc_id, pheromone_level) VALUES (" + i + "," + j + ","
							+ ants.pheromone[i][j] / maxPheromone +") ON DUPLICATE KEY UPDATE "
							+ "pheromone_level=" + ants.pheromone[i][j] / maxPheromone;
                	stmt.executeUpdate(query);
                }

            }

		} catch (SQLException ex) {
			System.out.println("SQLException: " + ex.getMessage());
			System.out.println("SQLState: " + ex.getSQLState());
			System.out.println("VendorError: " + ex.getErrorCode());
        }
        finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException sqlEx) { } // ignore
				rs = null;
			}

			if (stmt != null) {
				try {
					stmt.close();
				} catch (SQLException sqlEx) { } // ignore
				stmt = null;
			}
		}
	}

	void readNormalizedPheromones() {
        String query = "";
        try {
        	conn = DriverManager.getConnection(mysql_url, db_user, db_pw);
        	stmt = conn.createStatement();
			query = "SELECT * FROM pheromones WHERE from_loc_id IN (";
			for (int id : locIds) {
				query = query.concat(Integer.toString(id) + ",");
			}
			query = query.substring(0, query.length() - 1).concat(") AND to_loc_id IN (");
			for (int id : locIds) {
				query = query.concat(Integer.toString(id) + ",");
			}
			query = query.substring(0, query.length() - 1).concat(")");
			rs = stmt.executeQuery(query);

			int i = 0;
			int j = 0;
			while (rs.next()) {
				ants.total[i][j] = rs.getDouble("pheromone_level");;
				if (i < ants.total.length - 1) {
					i++;
				} else if ( i == ants.total.length - 1){
					i = 0;
					j++;
				}
			}

 		} catch (SQLException ex) {
			System.out.println("reading Pheromones failed.");
			System.out.println("SQLQuery: " + query);
			System.out.println("SQLException: " + ex.getMessage());
			System.out.println("SQLState: " + ex.getSQLState());
			System.out.println("VendorError: " + ex.getErrorCode());
        } finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException sqlEx) { } // ignore
				rs = null;
			}

			if (stmt != null) {
				try {
					stmt.close();
				} catch (SQLException sqlEx) { } // ignore
				stmt = null;
			}
		}

	}

    void printHeur()
    /*
     * FUNCTION: print heuristic information
     * INPUT: none
     * OUTPUT: none
     */
    {
	int i, j;

	System.out.println("Heuristic information:\n");
	for (i = 0; i < tsp.n; i++) {
	    System.out.println("From " + i + ":  ");
	    for (j = 0; j < tsp.n - 1; j++) {
		System.out.println(" " + ants.HEURISTIC(i, j));
	    }
	    System.out.println(" " + ants.HEURISTIC(i, j));
	    System.out.println("\n");
	}
	System.out.println("\n");
    }

    void printTrail()
    /*
     * FUNCTION: print ants.pheromone trail values
     * INPUT: none
     * OUTPUT: none
     */
    {
	int i, j;

	System.out.println("ants.pheromone Trail matrix, iteration: " + iteration + "\n");
	for (i = 0; i < tsp.n; i++) {
	    System.out.println("From " + i + ": ");
	    for (j = 0; j < tsp.n; j++) {
		System.out.println(" " + ants.pheromone[i][j] + " ");
		if (ants.pheromone[i][j] > 1.0)
		    System.out.println("XXXXX\n");
	    }
	    System.out.println("\n");
	}
	System.out.println("\n");
    }

    void printTotal()
    /*
     * FUNCTION: print values of ants.pheromone times heuristic information
     * INPUT: none
     * OUTPUT: none
     */
    {
	int i, j;

	System.out.println("combined ants.pheromone and heuristic info\n\n");
	for (i = 0; i < tsp.n; i++) {
	    for (j = 0; j < tsp.n - 1; j++) {
		System.out.println(" " + ants.total[i][j]);
		if (ants.total[i][j] > 1.0)
		    System.out.println("XXXXX\n");
	    }
	    System.out.println(" " + ants.total[i][tsp.n - 1]);
	    if (ants.total[i][tsp.n - 1] > 1.0)
		System.out.println("XXXXX\n");
	}
	System.out.println("\n");
    }

    void printProbabilities()
    /*
     * FUNCTION: prints the selection probabilities as encountered by an ant
     * INPUT: none
     * OUTPUT: none
     * COMMENTS: this computation assumes that no choice has been made yet.
     */
    {
	int i, j;
	double p[];
	double sum_prob;

	System.out.println("Selection Probabilities, iteration: " + iteration);
	p = new double[tsp.n];

	for (i = 0; i < tsp.n; i++) {
	    System.out.println("From " + i);
	    sum_prob = 0.;
	    for (j = 0; j < tsp.n; j++) {
		if (i == j)
		    p[j] = 0.;
		else
		    p[j] = ants.total[i][j];
		sum_prob += p[j];
	    }
	    for (j = 0; j < tsp.n; j++) {
		p[j] = p[j] / sum_prob;
	    }
	    for (j = 0; j < tsp.n - 1; j++) {
		System.out.println(" " + p[j] + " ");
	    }
	    System.out.println(" " + p[tsp.n - 1]);
	    if ((j % 26) == 0) {
		System.out.println("\n");
	    }
	    System.out.println("\n");
	}
	System.out.println("\n");
    }

    void printTour(int[] t)
    /*
     * FUNCTION: print the tour *t
     * INPUT: pointer to a tour
     * OUTPUT: none
     */
    {
	int i;

	System.out.println("\n");
	for (i = 0; i <= tsp.n; i++) {
	    if (i % 25 == 0)
		System.out.println("\n");
	    System.out.println(t[i]);
	}
	System.out.println("\n");
	System.out.println("Tour length = " + tsp.compute_tour_length(t));
    }

    void printTourFile(int[] t)
    /*
     * FUNCTION: print the tour *t to cmp.tsplibfile
     * INPUT: pointer to a tour
     * OUTPUT: none
     */
    {
	int i;
	if (comp_report == null)
	    return;

	printToFile(comp_report, "begin solution\n");
	for (i = 0; i < tsp.n; i++) {
	    printToFile(comp_report, t[i] + " ");
	}
	printToFile(comp_report, "\n");
	printToFile(comp_report, "Tour length " + tsp.compute_tour_length(t));
	printToFile(comp_report, "end solution\n");
    }

    void checkTour(int[] t)
    /*
     * FUNCTION: make a simple check whether tour *t can be feasible
     * INPUT: pointer to a tour
     * OUTPUT: none
     */
    {
	int i, sum = 0;

	for (i = 0; i < tsp.n; i++) {
	    sum += t[i];
	}
	if (sum != (tsp.n - 1) * tsp.n / 2) {
	    System.err.println("Next tour must be flawed !!\n");
	    printTour(t);
	    System.exit(1);
	}
    }

    void write_params()
    /*
     * FUNCTION: writes chosen parameter settings in standard output and in
     * report files
     * INPUT: none
     * OUTPUT: none
     */
    {
	System.out.println("Parameter-settings:\n");
	fprintf_parameters(null);
	System.out.println("\n");

	if (report != null) {
	    printToFile(report, "Parameter-settings: \n\n");
	    fprintf_parameters(report);
	    printToFile(report, "\n");
	}

	if (comp_report != null) {
	    printToFile(comp_report, PROG_ID_STR);
	    printToFile(comp_report, "Parameter-settings: \n\n");
	    fprintf_parameters(comp_report);
	    printToFile(comp_report, "\n");
	}
    }

    void printToFile(File file, String string) {
	if (file == null) {
	    System.out.println(string);
	} else {
	    try {
		writer.get(file.getName()).write(string + "\n");
	    } catch (IOException e) {
		System.err.print("Could not write file " + file.getName() + " " + e.getMessage());
		System.exit(1);
	    }
	}
    }

    void createTspFileFromDB(int[] locIds) {
        try {
			tsp.instance.name = "bins";
			conn = DriverManager.getConnection(mysql_url, db_user, db_pw);
			stmt = conn.createStatement();

			int dimension = locIds.length;
			int[][] distanceMatrix = new int[dimension][dimension];

			String query = "SELECT * FROM distance_matrix WHERE from_loc_id in (";
			for (int id : locIds) {
				query = query.concat(Integer.toString(id) + ",");
			}
			query = query.substring(0, query.length() - 1).concat(") AND to_loc_id in (");
			for (int id : locIds) {
				query = query.concat(Integer.toString(id) + ",");
			}
			query = query.substring(0, query.length() - 1).concat(") ORDER BY from_loc_id, to_loc_id");
			rs = stmt.executeQuery(query);

			int i = 0;
			int j = 0;
			while (rs.next()) {
				int distance = rs.getInt("distance");
				distanceMatrix[j][i] = distance;
				if (i < dimension - 1) {
					i++;
				} else if ( i == dimension -1){
					i = 0;
					j++;
				}
			}
			String directoryName = "tsp-problem-instances";
			String filename = directoryName + "/route" + (dimension+1) + "-" + Thread.currentThread().getId() + ".atsp";
			File directory = new File(directoryName);
			if (! directory.exists()){
				directory.mkdir();
			}
			name_buf = filename;
            BufferedWriter writer = new BufferedWriter(new FileWriter(
            		new File(filename)));
           	writer.write("NAME: rgb" + dimension+"\n");
            writer.write("TYPE: ATSP\n");
            writer.write("COMMENT: " + dimension + " random\n");
            writer.write("DIMENSION: " + dimension +"\n");
            writer.write("EDGE_WEIGHT_TYPE: EXPLICIT\n");
			writer.write("EDGE_WEIGHT_FORMAT: FULL_MATRIX\n");
			writer.write("EDGE_WEIGHT_SECTION\n");
			for(int k = 0; k < dimension; k++) {
				for(int l = 0; l < dimension; l++) {
					writer.write(distanceMatrix[k][l] + "\t");
				}
				writer.write("\n");
			}
			locIds=bubbleSort(locIds);
			writer.write("EOF");
            writer.close();
        } catch (IOException e) {
			e.printStackTrace();
		} catch (SQLException ex) {
			// handle any errors
			System.out.println("SQLException: " + ex.getMessage());
			System.out.println("SQLState: " + ex.getSQLState());
			System.out.println("VendorError: " + ex.getErrorCode());
		}
		finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException sqlEx) { } // ignore
				rs = null;
			}

			if (stmt != null) {
				try {
					stmt.close();
				} catch (SQLException sqlEx) { } // ignore
				stmt = null;
			}
		}
	}
    
    private int[] bubbleSort(int[] locIds) {
    	int temp=0;
		for(int i = 0; i < locIds.length;i++) {
			for(int j = i+1; j<locIds.length; j++) {
				if(locIds[j]<locIds[i]) {
					temp=locIds[i];
					locIds[i]=locIds[j];
					locIds[j]=temp;
				}
			}
		}
		return locIds;
	}

	public int find(int[] array, int value) {
    for(int i=0; i<array.length; i++) 
         if(array[i] == value)
             return i;
    return -1;
    }

	void generateMap(String[] fullResult, String[] shortResult, String[] args) {
        
        
        int	shortTourLength = Integer.parseInt(shortResult[0]);
        int	fullTourLength = Integer.parseInt(fullResult[0]);

        String fullTour =  fullResult[1].substring(1, fullResult[1].length() - 1);
        String shortTour =  shortResult[1].substring(1, shortResult[1].length() - 1);

//			tour = tour.substring(1, tour.length() - 1);
//			List<String> locations = Arrays.asList(tour.split(","));

        List<String> fullLocations = Arrays.asList(fullTour.split(","));
        List<String> shortLocations = Arrays.asList(shortTour.split(","));

        
        HashMap<Integer, String> addresses = new HashMap<Integer, String>();

        try {
			conn = DriverManager.getConnection(mysql_url, db_user, db_pw);
        	stmt = conn.createStatement();

			VelocityEngine ve = new VelocityEngine();
			ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
			ve.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
			ve.init();

			Template t = ve.getTemplate("templates/map.vm","UTF-8");
			VelocityContext context = new VelocityContext();

			// incremental ids from tspfile => real location ids
			for (int i = 0; i < fullTour.split(",").length; i++){
				fullLocations.set(i,
//						Integer.toString(InOut.locIds[Integer.parseInt(fullLocations.get(i).trim())]));
				Integer.toString(Integer.parseInt(fullLocations.get(i).trim())));
			}
			//for (int i = 0; i < shortTour.split(",").length; i++){
                        //    System.out.println(i + " ");
                        //    System.out.print(Integer.toString(InOut.locIds[Integer.parseInt(shortLocations.get(i).trim())]));
			//	shortLocations.set(i,
			//			Integer.toString(InOut.locIds[Integer.parseInt(shortLocations.get(i).trim())]));
			//}
                        
                        //parse shortloc to remlocIds
                        
                        int [] tmp= new int[shortLocations.size()];
                        int [] shortAr = new int[shortLocations.size()];
                        for (int i = 0; i < shortLocations.size(); i++) {
                            shortAr[i] = Integer.parseInt(shortLocations.get(i).trim());
                        }
                        
                        int max = 0;
                        int maxremo = 0;
                        int index = 0;
                        
                        while(max > -1)
                        {
                            maxremo = Arrays.stream(remlocIds).max().getAsInt();
                            max = Arrays.stream(shortAr).max().getAsInt();
                            if(max == -1)
                                break;
                            
                            index = find(shortAr, max);
                            
                            tmp[index] = maxremo;
                            shortAr[index] = -1;
                            index = find(shortAr, max);
                            if (index != -1)
                            {
                                tmp[index] = maxremo;
                                shortAr[index] = -1;
                            }
                            
                            
                            index = find(remlocIds, maxremo);
                            remlocIds[index] = -1;
                        }
/*                        
                        for (int i = 0; i < tmp.length; i++) {
                           // System.out.println(tmp[i]);
                        }
                        for (int i = 0; i < tmp.length; i++){
                            shortLocations.set(i,Integer.toString(tmp[i]));
			}
 */
                        
                        
			System.out.println("Full tour location ids:\n" + fullLocations
					+ "(" + fullLocations.size() + ")");
			System.out.println("Short tour location ids:\n" + shortLocations
					+ "(" + shortLocations.size() + ")");

        	rs = stmt.executeQuery("SELECT * FROM locations WHERE loc_id in ("
					+ fullLocations.toString().substring(1, fullLocations.toString().length() - 1 )
					+ ")");

			while (rs.next()) {
				addresses.put(rs.getInt("loc_id"), rs.getString("address"));
			}

			for (int i = 0; i < fullLocations.size(); i++) {
				fullLocations.set(i, fullLocations.get(i).trim());
				fullLocations.set(i, addresses.get(Integer.parseInt(fullLocations.get(i))).toString());
			}
			for (int i = 0; i < shortLocations.size(); i++) {
				shortLocations.set(i, shortLocations.get(i).trim());
				shortLocations.set(i, addresses.get(Integer.parseInt(shortLocations.get(i))).toString());
			}
//                        for (int i = 0; i < tmp.length; i++) {
//                               System.out.print(i);  
//				shortLocations.set(i, Integer.toString(tmp[i]));
//				shortLocations.set(i, addresses.get(tmp[i]));
//			}

			context.put("fullLocations", fullLocations.iterator());
			context.put("fullBins", fullLocations.size()-1);
			context.put("fullLength", fullTourLength);

			context.put("shortLocations", shortLocations.iterator());
			context.put("shortBins", shortLocations.size()-1);
			context.put("shortLength", shortTourLength);

			File directory = new File("maps");
			if (! directory.exists()){
				directory.mkdir();
			}
			//File mapFile = new File("maps/" + "rgb_" + fullLocations.size()
			//		+ "_" + shortLocations.size() + ".html");
                        File mapFile = new File("maps/" + "rgbmap.html");
//					new File(name_buf).getName().replaceFirst("atsp", "html"));
			BufferedWriter bw = new BufferedWriter(new FileWriter(mapFile));

			t.merge(context, bw);
			bw.close();

 		} catch (SQLException ex) {
			System.out.println("SQLException: " + ex.getMessage());
			System.out.println("SQLState: " + ex.getSQLState());
			System.out.println("VendorError: " + ex.getErrorCode());
        } catch (IOException ex) {
        	System.out.println(ex.toString());
		}
        finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException sqlEx) { } // ignore
				rs = null;
			}

			if (stmt != null) {
				try {
					stmt.close();
				} catch (SQLException sqlEx) { } // ignore
				stmt = null;
			}
		}
	}
}