package de.routecalculation.singledepot.adrianwilke.acotspjava;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;


/**
 * ACO algorithms for the TSP
 * 
 * This code is based on the ACOTSP project of Thomas Stuetzle.
 * It was initially ported from C to Java by Adrian Wilke.
 * 
 * Project website: http://adibaba.github.io/ACOTSPJava/
 * Source code: https://github.com/adibaba/ACOTSPJava/
 */
public class AcoTsp {
    /*
     * ################################################
     * ########## ACO algorithms for the TSP ##########
     * ################################################
     * 
     * Version: 2.0
     * 
     * modified by: Simon Hofmeister, date: 06.05.2019, reason: solving of more than 1 aco instance at a time is now possible, implemented as webservice
     * 
     * File: main.c
     * Author: Thomas Stuetzle
     * Purpose: main routines and control for the ACO algorithms
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

	private Timer timer;
	private InOut inOut;
	private Ants ants;
	private LocalSearch localSearch;
	private Tsp tsp;
	
	public AcoTsp(Timer timer, InOut inOut, Ants ants, LocalSearch localSearch, Tsp tsp) {
		this.timer = timer;
		this.inOut = inOut;
		this.ants = ants;
		this.localSearch = localSearch;
		this.tsp = tsp;
	}
	
    boolean termination_condition()
    /*
     * FUNCTION: checks whether termination condition is met
     * INPUT: none
     * OUTPUT: 0 if condition is not met, number neq 0 otherwise
     * (SIDE)EFFECTS: none
     */
    {
	return (((inOut.n_tours >= inOut.max_tours) && (timer.elapsed_time() >= inOut.max_time)) || (ants.best_so_far_ant.tour_length <= inOut.optimal));
    }

    void construct_solutions()
    /*
     * FUNCTION: manage the solution construction phase
     * INPUT: none
     * OUTPUT: none
     * (SIDE)EFFECTS: when finished, all ants of the colony have constructed a solution
     */
    {
	int k; /* counter variable */
	int step; /* counter of the number of construction steps */

	// TRACE ( System.out.println("construct solutions for all ants\n"); );

	/* Mark all cities as unvisited */
	for (k = 0; k < ants.n_ants; k++) {
		ants.ant_empty_memory(ants.ant[k]);
	}

	step = 0;
	/* Place the ants on same initial city */
	for (k = 0; k < ants.n_ants; k++)
		ants.place_ant(ants.ant[k], step);

	while (step < tsp.n - 1) {
	    step++;
	    for (k = 0; k < ants.n_ants; k++) {
	    	ants.neighbour_choose_and_move_to_next(ants.ant[k], step);
		if (ants.acs_flag)
			ants.local_acs_pheromone_update(ants.ant[k], step);
	    }
	}

	step = tsp.n;
	for (k = 0; k < ants.n_ants; k++) {
	    ants.ant[k].tour[tsp.n] = ants.ant[k].tour[0];
	    ants.ant[k].tour_length = tsp.compute_tour_length(ants.ant[k].tour);
	    if (ants.acs_flag)
	    	ants.local_acs_pheromone_update(ants.ant[k], step);
	}
	inOut.n_tours += ants.n_ants;
    }

    void init_try(int ntry)
    /*
     * FUNCTION: initilialize variables appropriately when starting a trial
     * INPUT: trial number
     * OUTPUT: none
     * COMMENTS: none
     */
    {

	// TRACE ( System.out.println("INITUtilities.IALIZE TRUtilities.IAL\n"); );

    timer.start_timers();
    inOut.time_used = timer.elapsed_time();
    inOut.time_passed = inOut.time_used;

	if (inOut.comp_report != null) {
	    inOut.printToFile(inOut.comp_report, "Utilities.seed " + Utilities.seed);
	}
	/* Initialize variables concerning statistics etc. */

	inOut.n_tours = 1;
	inOut.iteration = 1;
	inOut.restart_iteration = 1;
	inOut.lambda = 0.05;
	ants.best_so_far_ant.tour_length = Integer.MAX_VALUE;
	inOut.found_best = 0;

	/*
	 * Initialize the Pheromone trails, only if ACS is used, ants.pheromones
	 * have to be initialized differently
	 */
	if (!(ants.acs_flag || ants.mmas_flag || ants.bwas_flag)) {
	    ants.trail_0 = 1. / ((ants.rho) * ants.nn_tour());
	    /*
	     * in the original papers on Ant System, Elitist Ant System, and
	     * Rank-based Ant System it is not exactly defined what the
	     * initial value of the ants.pheromones is. Here we set it to some
	     * small constant, analogously as done in MAX-MIN Ant System.
	     */
	    ants.init_pheromone_trails(ants.trail_0);
	}
	if (ants.bwas_flag) {
	    ants.trail_0 = 1. / ((double) tsp.n * (double) ants.nn_tour());
	    ants.init_pheromone_trails(ants.trail_0);
	}
	if (ants.mmas_flag) {
	    ants.trail_max = 1. / ((ants.rho) * ants.nn_tour());
	    ants.trail_min = ants.trail_max / (2. * tsp.n);
	    ants.init_pheromone_trails(ants.trail_max);
	}
	if (ants.acs_flag) {
	    ants.trail_0 = 1. / ((double) tsp.n * (double) ants.nn_tour());
	    ants.init_pheromone_trails(ants.trail_0);
	}

	/* Calculate combined information ants.pheromone times heuristic information */
	ants.compute_total_information();

	if (inOut.comp_report != null)
		inOut.printToFile(inOut.comp_report, "begin try " + ntry);
	if (inOut.stat_report != null)
		inOut.printToFile(inOut.stat_report, "begin try " + ntry);
    }

    void local_search()
    /*
     * FUNCTION: manage the local search phase; apply local search to ALL ants; in
     * dependence of LocalSearch.ls_flag one of 2-opt, 2.5-opt, and 3-opt local search
     * is chosen.
     * INPUT: none
     * OUTPUT: none
     * (SIDE)EFFECTS: all ants of the colony have locally optimal tours
     * COMMENTS: typically, best performance is obtained by applying local search
     * to all ants. It is known that some improvements (e.g. convergence
     * speed towards high quality solutions) may be obtained for some
     * ACO algorithms by applying local search to only some of the ants.
     * Overall best performance is typcially obtained by using 3-opt.
     */
    {
	int k;

	// TRACE ( System.out.println("apply local search to all ants\n"); );

	for (k = 0; k < ants.n_ants; k++) {
	    switch (localSearch.ls_flag) {
	    case 1:
	    	localSearch.two_opt_first(ants.ant[k].tour); /* 2-opt local search */
		break;
	    case 2:
	    	localSearch.two_h_opt_first(ants.ant[k].tour); /* 2.5-opt local search */
		break;
	    case 3:
	    	localSearch.three_opt_first(ants.ant[k].tour); /* 3-opt local search */
		break;
	    default:
		System.err.println("type of local search procedure not correctly specified");
		System.exit(1);
	    }
	    ants.ant[k].tour_length = tsp.compute_tour_length(ants.ant[k].tour);
	    if (termination_condition())
		return;
	}
    }

    void update_statistics()
    /*
     * FUNCTION: manage some statistical information about the trial, especially
     * if a new best solution (best-so-far or restart-best) is found and
     * adjust some parameters if a new best solution is found
     * INPUT: none
     * OUTPUT: none
     * (SIDE)EFFECTS: restart-best and best-so-far ant may be updated; ants.trail_min
     * and ants.trail_max used by MMAS may be updated
     */
    {

	int iteration_best_ant;
	double p_x; /* only used by MMAS */

	iteration_best_ant = ants.find_best(); /* iteration_best_ant is a global variable */

	if (ants.ant[iteration_best_ant].tour_length < ants.best_so_far_ant.tour_length) {

		inOut.time_used = timer.elapsed_time(); /* best sol found after time_used */
	    ants.copy_from_to(ants.ant[iteration_best_ant], ants.best_so_far_ant);
	    ants.copy_from_to(ants.ant[iteration_best_ant], ants.restart_best_ant);

	    inOut.found_best = inOut.iteration;
	    inOut.restart_found_best = inOut.iteration;
	    inOut.found_branching = inOut.node_branching(inOut.lambda);
	    inOut.branching_factor = inOut.found_branching;
	    if (ants.mmas_flag) {
		if (localSearch.ls_flag == 0) {
		    p_x = Math.exp(Math.log(0.05) / tsp.n);
		    ants.trail_min = 1. * (1. - p_x) / (p_x * ((ants.nn_ants + 1) / 2));
		    ants.trail_max = 1. / ((ants.rho) * ants.best_so_far_ant.tour_length);
		    ants.trail_0 = ants.trail_max;
		    ants.trail_min = ants.trail_max * ants.trail_min;
		} else {
		    ants.trail_max = 1. / ((ants.rho) * ants.best_so_far_ant.tour_length);
		    ants.trail_min = ants.trail_max / (2. * tsp.n);
		    ants.trail_0 = ants.trail_max;
		}
	    }
	    inOut.write_report();
	}
	if (ants.ant[iteration_best_ant].tour_length < ants.restart_best_ant.tour_length) {
	    ants.copy_from_to(ants.ant[iteration_best_ant], ants.restart_best_ant);
	    inOut.restart_found_best = inOut.iteration;
	    System.out.println("restart best: " + ants.restart_best_ant.tour_length + " restart_found_best "
		    + inOut.restart_found_best + ", time " + timer.elapsed_time());
	}
    }

    void search_control_and_statistics()
    /*
     * FUNCTION: occasionally compute some statistics and check whether algorithm
     * is converged
     * INPUT: none
     * OUTPUT: none
     * (SIDE)EFFECTS: restart-best and best-so-far ant may be updated; ants.trail_min
     * and ants.trail_max used by MMAS may be updated
     */
    {
	// TRACE ( System.out.println("SEARCH CONTROL AND STATISTICS\n"); );

	if ((inOut.iteration % 100) == 0) {
		inOut.population_statistics();
		inOut.branching_factor = inOut.node_branching(inOut.lambda);
	    System.out.println("best so far " + ants.best_so_far_ant.tour_length + ", iteration: " + inOut.iteration
		    + ", time " + timer.elapsed_time() + ", b_fac " + inOut.branching_factor);

	    if (ants.mmas_flag && (inOut.branching_factor < inOut.branch_fac)
		    && (inOut.iteration - inOut.restart_found_best > 250)) {
		/*
		 * MAX-MIN Ant System was the first ACO algorithm to use
		 * ants.pheromone trail re-initialisation as implemented
		 * here. Other ACO algorithms may also profit from this mechanism.
		 */
		System.out.println("INIT TRAILS!!!\n");
		ants.restart_best_ant.tour_length = Integer.MAX_VALUE;
		ants.init_pheromone_trails(ants.trail_max);
		ants.compute_total_information();
		inOut.restart_iteration = inOut.iteration;
		inOut.restart_time = timer.elapsed_time();
	    }
	    System.out.println("try " + inOut.n_try + " iteration " + inOut.iteration + ", b-fac "
		    + inOut.branching_factor);
	}
    }

    void as_update()
    /*
     * FUNCTION: manage global ants.pheromone deposit for Ant System
     * INPUT: none
     * OUTPUT: none
     * (SIDE)EFFECTS: all ants deposit ants.pheromones on matrix "ants.pheromone"
     */
    {
	int k;

	// TRACE ( System.out.println("Ant System ants.pheromone deposit\n"); );

	for (k = 0; k < ants.n_ants; k++)
	    ants.global_update_pheromone(ants.ant[k]);
    }

    void eas_update()
    /*
     * FUNCTION: manage global ants.pheromone deposit for Elitist Ant System
     * INPUT: none
     * OUTPUT: none
     * (SIDE)EFFECTS: all ants plus elitist ant deposit ants.pheromones on matrix "ants.pheromone"
     */
    {
	int k;

	// TRACE ( System.out.println("Elitist Ant System ants.pheromone deposit\n"); );

	for (k = 0; k < ants.n_ants; k++)
	    ants.global_update_pheromone(ants.ant[k]);
	ants.global_update_pheromone_weighted(ants.best_so_far_ant, ants.elitist_ants);
    }

    void ras_update()
    /*
     * FUNCTION: manage global ants.pheromone deposit for Rank-based Ant System
     * INPUT: none
     * OUTPUT: none
     * (SIDE)EFFECTS: the ants.ras_ranks-1 best ants plus the best-so-far ant deposit ants.pheromone
     * on matrix "ants.pheromone"
     * COMMENTS: this procedure could be implemented slightly faster, but it is
     * anyway not critical w.r.t. CPU time given that ants.ras_ranks is
     * typically very small.
     */
    {
	int i, k, b, target;
	int[] help_b;

	// TRACE ( System.out.println("Rank-based Ant System ants.pheromone deposit\n"); );

	help_b = new int[ants.n_ants];
	for (k = 0; k < ants.n_ants; k++)
	    help_b[k] = ants.ant[k].tour_length;

	for (i = 0; i < ants.ras_ranks - 1; i++) {
	    b = help_b[0];
	    target = 0;
	    for (k = 0; k < ants.n_ants; k++) {
		if (help_b[k] < b) {
		    b = help_b[k];
		    target = k;
		}
	    }
	    help_b[target] = Integer.MAX_VALUE;
	    ants.global_update_pheromone_weighted(ants.ant[target], ants.ras_ranks - i - 1);
	}
	ants.global_update_pheromone_weighted(ants.best_so_far_ant, ants.ras_ranks);

    }

    void mmas_update()
    /*
     * FUNCTION: manage global ants.pheromone deposit for MAX-MIN Ant System
     * INPUT: none
     * OUTPUT: none
     * (SIDE)EFFECTS: either the iteration-best or the best-so-far ant deposit ants.pheromone
     * on matrix "ants.pheromone"
     */
    {
	/*
	 * we use default upper ants.pheromone trail limit for MMAS and hence we
	 * do not have to worry regarding keeping the upper limit
	 */

	int iteration_best_ant;

	// TRACE ( System.out.println("MAX-MIN Ant System ants.pheromone deposit\n"); );

	if (inOut.iteration % ants.u_gb == 0) {
	    iteration_best_ant = ants.find_best();
	    ants.global_update_pheromone(ants.ant[iteration_best_ant]);
	} else {
	    if (ants.u_gb == 1 && (inOut.iteration - inOut.restart_found_best > 50))
		ants.global_update_pheromone(ants.best_so_far_ant);
	    else
		ants.global_update_pheromone(ants.restart_best_ant);
	}

	if (localSearch.ls_flag != 0) {
	    /*
	     * implement the schedule for ants.u_gb as defined in the
	     * Future Generation Computer Systems article or in Stuetzle's PhD thesis.
	     * This schedule is only applied if local search is used.
	     */
	    if ((inOut.iteration - inOut.restart_iteration) < 25)
		ants.u_gb = 25;
	    else if ((inOut.iteration - inOut.restart_iteration) < 75)
		ants.u_gb = 5;
	    else if ((inOut.iteration - inOut.restart_iteration) < 125)
		ants.u_gb = 3;
	    else if ((inOut.iteration - inOut.restart_iteration) < 250)
		ants.u_gb = 2;
	    else
		ants.u_gb = 1;
	} else
	    ants.u_gb = 25;

    }

    void bwas_update()
    /*
     * FUNCTION: manage global ants.pheromone deposit for Best-Worst Ant System
     * INPUT: none
     * OUTPUT: none
     * (SIDE)EFFECTS: either the iteration-best or the best-so-far ant deposit ants.pheromone
     * on matrix "ants.pheromone"
     */
    {
	int iteration_worst_ant, distance_best_worst;

	// TRACE ( System.out.println("Best-worst Ant System ants.pheromone deposit\n"); );

	ants.global_update_pheromone(ants.best_so_far_ant);
	iteration_worst_ant = ants.find_worst();
	ants.bwas_worst_ant_update(ants.ant[iteration_worst_ant], ants.best_so_far_ant);
	distance_best_worst = ants.distance_between_ants(ants.best_so_far_ant, ants.ant[iteration_worst_ant]);
	/*
	 * System.out.println("distance_best_worst %ld, tour length worst %ld\n",distance_best_worst,ant[iteration_worst_ant
	 * ].tour_length);
	 */
	if (distance_best_worst < (int) (0.05 * tsp.n)) {
	    ants.restart_best_ant.tour_length = Integer.MAX_VALUE;
	    ants.init_pheromone_trails(ants.trail_0);
	    inOut.restart_iteration = inOut.iteration;
	    inOut.restart_time = timer.elapsed_time();
	    System.out.println("init ants.pheromone trails with " + ants.trail_0 + ", iteration " + inOut.iteration);
	} else
		ants.bwas_pheromone_mutation();
    }

    void acs_global_update()
    /*
     * FUNCTION: manage global ants.pheromone deposit for Ant Colony System
     * INPUT: none
     * OUTPUT: none
     * (SIDE)EFFECTS: the best-so-far ant deposits ants.pheromone on matrix "ants.pheromone"
     * COMMENTS: global ants.pheromone deposit in ACS is done per default using
     * the best-so-far ant; Gambardella & Dorigo examined also iteration-best
     * update (see their IEEE Trans. on Evolutionary Computation article),
     * but did not use it for the published computational results.
     */
    {
	// TRACE ( System.out.println("Ant colony System global ants.pheromone deposit\n"); );

	ants.global_acs_pheromone_update(ants.best_so_far_ant);
    }

    void pheromone_trail_update()
    /*
     * FUNCTION: manage global ants.pheromone trail update for the ACO algorithms
     * INPUT: none
     * OUTPUT: none
     * (SIDE)EFFECTS: ants.pheromone trails are evaporated and ants.pheromones are deposited
     * according to the rules defined by the various ACO algorithms.
     */
    {
	/*
	 * Simulate the ants.pheromone evaporation of all ants.pheromones; this is not necessary
	 * for ACS (see also ACO Book)
	 */
	if (ants.as_flag || ants.eas_flag || ants.ras_flag || ants.bwas_flag || ants.mmas_flag) {
	    if (localSearch.ls_flag != 0) {
		if (ants.mmas_flag)
		    ants.mmas_evaporation_nn_list();
		else
		    ants.evaporation_nn_list();
		/*
		 * evaporate only ants.pheromones on arcs of candidate list to make the
		 * ants.pheromone evaporation faster for being able to tackle large TSP
		 * tsp.instances. For MMAS additionally check lower ants.pheromone trail limits.
		 */
	    } else {
		/* if no local search is used, evaporate all ants.pheromone trails */
		ants.evaporation();
	    }
	}

	/* Next, apply the ants.pheromone deposit for the various ACO algorithms */
	if (ants.as_flag)
	    as_update();
	else if (ants.eas_flag)
	    eas_update();
	else if (ants.ras_flag)
	    ras_update();
	else if (ants.mmas_flag)
	    mmas_update();
	else if (ants.bwas_flag)
	    bwas_update();
	else if (ants.acs_flag)
	    acs_global_update();

	/*
	 * check ants.pheromone trail limits for MMAS; not necessary if local
	 * search is used, because in the local search case lower ants.pheromone trail
	 * limits are checked in procedure mmas_evaporation_nn_list
	 */
	if (ants.mmas_flag && localSearch.ls_flag == 0)
	    ants.check_pheromone_trail_limits();

	/*
	 * Compute combined information ants.pheromone times heuristic info after
	 * the ants.pheromone update for all ACO algorithms except ACS; in the ACS case
	 * this is already done in the ants.pheromone update procedures of ACS
	 */
	if (ants.as_flag || ants.eas_flag || ants.ras_flag || ants.mmas_flag || ants.bwas_flag) {
	    if (localSearch.ls_flag != 0) {
		ants.compute_nn_list_total_information();
	    } else {
		ants.compute_total_information();
	    }
	}
    }

    /* --- main program ------------------------------------------------------ */

    public String[] runAco(Boolean full, String[] args) {
        /*
         * FUNCTION: main control for running the ACO algorithms
         * INPUT: none
         * OUTPUT: none
         * (SIDE)EFFECTS: none
         * COMMENTS: this function controls the run of "max_tries" independent trials
        */

    	timer.start_timers();

    	inOut.init_program(full, args);

        tsp.instance.nn_list = tsp.compute_nn_lists();
        ants.pheromone = Utilities.generate_double_matrix(tsp.n, tsp.n);
        ants.total = Utilities.generate_double_matrix(tsp.n, tsp.n);

        inOut.readNormalizedPheromones();
        inOut.time_used = timer.elapsed_time();
        System.out.println("Initialization took " + inOut.time_used + " seconds\n");
        System.out.println("acotsp: before loop");
        for (inOut.n_try = 0; inOut.n_try < inOut.max_tries; inOut.n_try++) {
        	System.out.println("acotsp: before init");
            init_try(inOut.n_try);
            System.out.println("acotsp: inited");
            while (!termination_condition()) {
            	System.out.println("acotsp: not terminated");
            construct_solutions();
            System.out.println("acotsp: solutions");
            if (localSearch.ls_flag > 0) {
            	System.out.println("acotsp: locSearchStart");
            	local_search();
            	System.out.println("acotsp: locSearchEnd");
            }

            update_statistics();
            System.out.println("acotsp: Statistics");

            pheromone_trail_update();
            System.out.println("acotsp: pheromones");

            search_control_and_statistics();
            System.out.println("acotsp: searchControl");

            inOut.iteration++;
            }
            inOut.exit_try(inOut.n_try);
        }
        System.out.println("acotsp: after loop");
        inOut.exit_program();

        // Added by AW
        int aw_best_tour_length = Utilities.best_of_vector(inOut.best_in_try, inOut.max_tries);
        String aw_best_tour = inOut.aw_best_tour_in_try[Utilities.aw_best_tour_index(inOut.best_in_try, inOut.max_tries)];
        try {
            Writer w = new OutputStreamWriter(new FileOutputStream("tour." + tsp.instance.name), "UTF8");
            BufferedWriter out = new BufferedWriter(w);
            out.write(aw_best_tour_length + "\n");
            out.write(aw_best_tour);
            out.close();
        } catch (IOException e) {
            System.err.print("Could not write file tour." + tsp.instance.name + " " + e.getMessage());
            System.exit(1);
        }
        System.out.println();
        System.out.println("Best tour:");
        System.out.println(aw_best_tour_length);
        System.out.println(aw_best_tour);
        inOut.writeNormalizedPheromones();
    //	InOut.generateMap(aw_best_tour, aw_best_tour_length);
		String[] ret = {Integer.toString(aw_best_tour_length), aw_best_tour};
		return ret;
	}

}
