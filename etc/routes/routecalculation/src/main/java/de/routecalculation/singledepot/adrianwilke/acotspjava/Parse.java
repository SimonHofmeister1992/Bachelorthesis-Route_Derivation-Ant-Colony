package de.routecalculation.singledepot.adrianwilke.acotspjava;

import java.util.Comparator;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * ACO algorithms for the TSP
 * 
 * This code is based on the ACOTSP project of Thomas Stuetzle.
 * It was initially ported from C to Java by Adrian Wilke.
 * 
 * Project website: http://adibaba.github.io/ACOTSPJava/
 * Source code: https://github.com/adibaba/ACOTSPJava/
 */
public class Parse {
	
    /*
     * 
     * ################################################
     * ########## ACO algorithms for the TSP ##########
     * ################################################
     * Version: 2.0
     * 
     * modified by: Simon Hofmeister, date: 06.05.2019, reason: solving of more than 1 aco instance at a time is now possible, implemented as webservice, added parameter of possible startLocation
     * 
     * File: 
     * Author: Thomas Stuetzle
     * Purpose: implementation of local search routines
     * Check: README and gpl.txt
     * Copyright (C) 1999 Thomas Stuetzle
     */
	
    /***************************************************************************
     * Program's name: ACOTSPJava
     * 
     * Command line parser for 'ACO algorithms for the TSP'
     * 
     * Copyright (C) 2014 Adrian Wilke
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
     * You should have received a copy of the GNU General Public License along
     * with this program; if not, write to the Free Software Foundation, Inc.,
     * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
     ***************************************************************************/

	private LocalSearch localSearch;
	private InOut inOut;
	private Ants ants;
	
	public Parse(LocalSearch localSearch, InOut inOut, Ants ants) {
		this.localSearch = localSearch;
		this.inOut = inOut;
		this.ants = ants;
	}
	
    class OptComparator implements Comparator<Option> {

	Map<String, Integer> opt = new HashMap<String, Integer>();

	public OptComparator() {
	    int i = 0;
	    opt.put("r", i++);
	    opt.put("s", i++);
	    opt.put("t", i++);
	    opt.put("seed", i++);
//	    opt.put("i", i++);
	    opt.put("o", i++);
	    opt.put("g", i++);
	    opt.put("a", i++);
	    opt.put("b", i++);
	    opt.put("e", i++);
	    opt.put("q", i++);
	    opt.put("c", i++);
	    opt.put("f", i++);
	    opt.put("k", i++);
	    opt.put("l", i++);
	    opt.put("d", i++);
	    opt.put("u", i++);
	    opt.put("v", i++);
	    opt.put("w", i++);
	    opt.put("x", i++);
	    opt.put("quiet", i++);
	    opt.put("h", i++);
	    opt.put("p", i++);
	}

	@Override
	public int compare(Option o1, Option o2) {
	    if (o1.getValue() == null || o2.getValue() == null)
		return 0;
	    else
		return (opt.get(o1.getOpt()) - opt.get(o2.getOpt()));
	}
    }

    int parse_commandline(String args[]) {

	// TODO range check

	if (args.length == 0) {
	    System.err.println("No options are specified.");
	    System.err.println("Try `--help' for more information.");
	    System.exit(1);
	}

	Options options = new Options();
	options.addOption("r", "tries", true, "# number of independent trials");
	options.addOption("s", "tours", true, "# number of steps in each trial");
	options.addOption("t", "time", true, "# maximum time for each trial");
	options.addOption("seed", true, "# seed for the random number generator");
//	options.addOption("i", "tsplibfile", true, "f inputfile (TSPLIB format necessary)");
	options.addOption("o", "optimum", true, "# stop if tour better or equal optimum is found");
	options.addOption("m", "ants", true, "# number of ants");
	options.addOption("g", "nnants", true, "# nearest neighbours in tour construction");
	options.addOption("a", "alpha", true, "# alpha (influence of pheromone trails)");
	options.addOption("b", "beta", true, "# beta (influence of heuristic information)");
	options.addOption("e", "rho", true, "# rho: pheromone trail evaporation");
	options.addOption("q", "q0", true, "# q_0: prob. of best choice in tour construction");
	options.addOption("c", "elitistants", true, "# number of elitist ants");
	options.addOption("f", "rasranks", true, "# number of ranks in rank-based Ant System");
	options.addOption("k", "nnls", true, "# No. of nearest neighbors for local search");
	options.addOption("l", "localsearch", true, "0:no local search  1:2-opt  2:2.5-opt  3:3-opt");
	options.addOption("d", "dlb", false, "1 use don't look bits in local search");
	options.addOption("u", "as", false, "apply basic Ant System");
	options.addOption("v", "eas", false, "apply elitist Ant System");
	options.addOption("w", "ras", false, "apply rank-based version of Ant System");
	options.addOption("x", "mmas", false, "apply MAX-MIN ant_colony system");
	options.addOption("y", "bwas", false, "apply best-worst ant_colony system");
	options.addOption("z", "acs", false, "apply ant_colony colony system");
	options.addOption("quiet", false, "reduce output to a minimum, no extra files");
	options.addOption("h", "help", false, "display this help text and exit");
	options.addOption("p", "drop", true, "number of random locations NOT to visit");
	options.addOption("sl", "startlocation", true, "start location of the tour");

	CommandLine cmd = null;
	CommandLineParser parser = new DefaultParser();
	try {
	    cmd = parser.parse(options, args);
	} catch (ParseException e) {
	    System.err.println("Error: " + e.getMessage());
	    System.exit(1);
	}

	if (cmd.hasOption("h")) {
	    HelpFormatter formatter = new HelpFormatter();
	    formatter.setSyntaxPrefix("Usage: ");
	    formatter.setOptionComparator(new OptComparator());
	    formatter.printHelp(InOut.PROG_ID_STR + " [OPTION]... [ARGUMENT]...", "Options:", options, "");
	    System.exit(0);
	}

	System.out.println("OPTIONS:");

	if (cmd.hasOption("quiet")) {
	    inOut.quiet_flag = true;
	    System.out.println("-quiet Quiet mode is set");
	}

	if (cmd.hasOption("t")) {
		inOut.max_time = Float.parseFloat(cmd.getOptionValue("t"));
	    System.out.println("-t/time Time limit with argument " + inOut.max_time);
	} else {
	    System.out.println("Note: Time limit is set to default " + inOut.max_time + " seconds");
	}

	if (cmd.hasOption("r")) {
		inOut.max_tries = Integer.parseInt(cmd.getOptionValue("r"));
	    System.out.println("-r/tries Number of tries with argument " + inOut.max_tries);
	} else {
	    System.out.println("Note: Number of tries is set to default " + inOut.max_tries);
	}

	if (cmd.hasOption("s")) {
		inOut.max_tours = Integer.parseInt(cmd.getOptionValue("s"));
	    System.out.println("-s/tours Maximum number tours with argument " + inOut.max_tours);
	} else {
	    System.out.println("Note: Maximum number tours is set to default " + inOut.max_tours);
	}

	if (cmd.hasOption("seed")) {
	    Utilities.seed = Integer.parseInt(cmd.getOptionValue("seed"));
	    System.out.println("-seed with argument " + Utilities.seed);
	} else {
	    System.out.println("Note: A seed was generated as " + Utilities.seed);
	}

	if (cmd.hasOption("o")) {
		inOut.optimal = Integer.parseInt(cmd.getOptionValue("o"));
	    System.out.println("-o/optimum Optimal solution with argument " + inOut.optimal);
	} else {
	    System.out.println("Note: Optimal solution value is set to default " + inOut.optimal);
	}
/*
	if (cmd.hasOption("i")) {
	    InOut.name_buf = cmd.getOptionValue("i");
	    System.out.println("-i/tsplibfile File with argument " + InOut.name_buf);
	} else {
	    System.err.println("Error: No input file given");
	    System.exit(1);
	}
*/

	// Choice of ONE algorithm
	int algorithmCount = 0;
	if (cmd.hasOption("u")) {
	    algorithmCount++;
	}
	if (cmd.hasOption("w")) {
	    algorithmCount++;
	}
	if (cmd.hasOption("x")) {
	    algorithmCount++;
	}
	if (cmd.hasOption("v")) {
	    algorithmCount++;
	}
	if (cmd.hasOption("y")) {
	    algorithmCount++;
	}
	if (cmd.hasOption("z")) {
	    algorithmCount++;
	}
	if (algorithmCount > 1) {
	    System.err.println("Error: More than one ACO algorithm enabled in the command line.");
	    System.exit(1);
	} else if (algorithmCount == 1) {
	    ants.as_flag = false;
	    ants.eas_flag = false;
	    ants.ras_flag = false;
	    ants.mmas_flag = false;
	    ants.bwas_flag = false;
	    ants.acs_flag = false;
	}

	if (cmd.hasOption("u")) {
		ants.as_flag = true;
	    inOut.set_default_as_parameters();
	    System.out.println("-u/as is set, run basic Ant System");
	}
	if (cmd.hasOption("v")) {
		ants.eas_flag = true;
	    inOut.set_default_eas_parameters();
	    System.out.println("-v/eas is set, run Elitist Ant System");
	}
	if (cmd.hasOption("w")) {
		ants.ras_flag = true;
	    inOut.set_default_ras_parameters();
	    System.out.println("-w/ras is set, run Rank-based Ant System");
	}
	if (cmd.hasOption("x") || algorithmCount == 0) {
		ants.mmas_flag = true;
	    inOut.set_default_mmas_parameters();
	    System.out.println("-x/mmas is set, run MAX-MIN Ant System");
	}
	if (cmd.hasOption("y")) {
		ants.bwas_flag = true;
	    inOut.set_default_bwas_parameters();
	    System.out.println("-y/bwas is set, run Best-Worst Ant System");
	}
	if (cmd.hasOption("z")) {
		ants.acs_flag = true;
	    inOut.set_default_acs_parameters();
	    System.out.println("-z/acs is set, run Ant Colony System");
	}

	// Local search
	if (cmd.hasOption("l")) {
		localSearch.ls_flag = Integer.parseInt(cmd.getOptionValue("l"));

	    switch (localSearch.ls_flag) {
	    case 0:
		System.out.println("Note: local search flag is set to default 0 (disabled)");
		break;
	    case 1:
		System.out.println("Note: local search flag is set to default 1 (2-opt)");
		break;
	    case 2:
		System.out.println("Note: local search flag is set to default 2 (2.5-opt)");
		break;
	    case 3:
		System.out.println("Note: local search flag is set to default 3 (3-opt)");
		break;
	    default:
		System.out.println("-l/localsearch with argument " + localSearch.ls_flag);
		break;
	    }
	}
	if (localSearch.ls_flag != 0) {
	    inOut.set_default_ls_parameters();
	}

	if (cmd.hasOption("m")) {
		ants.n_ants = Integer.parseInt(cmd.getOptionValue("m"));
	    System.out.println("-m/ants Number of ants with argument " + ants.n_ants);
	} else {
	    System.out.println("Note: Number of ants is set to default " + ants.n_ants);
	}

	if (cmd.hasOption("a")) {
		ants.alpha = Float.parseFloat(cmd.getOptionValue("a"));
	    System.out.println("-a/alpha with argument " + ants.alpha);
	} else {
	    System.out.println("Note: Alpha is set to default " + ants.alpha);
	}

	if (cmd.hasOption("b")) {
		ants.beta = Float.parseFloat(cmd.getOptionValue("b"));
	    System.out.println("-b/beta with argument " + ants.beta);
	} else {
	    System.out.println("Note: Beta is set to default " + ants.beta);
	}

	if (cmd.hasOption("e")) {
		ants.rho = Float.parseFloat(cmd.getOptionValue("e"));
	    System.out.println("-e/rho with argument " + ants.rho);
	} else {
	    System.out.println("Note: Rho is set to default " + ants.rho);
	}

	if (cmd.hasOption("q")) {
		ants.q_0 = Float.parseFloat(cmd.getOptionValue("q"));
	    System.out.println("-q/q0 with argument " + ants.q_0);
	} else {
	    System.out.println("Note: q0 is set to default " + ants.q_0);
	}

	if (cmd.hasOption("c")) {
		ants.elitist_ants = Integer.parseInt(cmd.getOptionValue("c"));
	    System.out.println("-c/elitistants Number of elitist ants with argument " + ants.elitist_ants);
	} else {
	    System.out.println("Note: Number of elitist ants is set to default " + ants.elitist_ants);
	}

	if (cmd.hasOption("f")) {
		ants.ras_ranks = Integer.parseInt(cmd.getOptionValue("f"));
	    System.out.println("-f/rasranks Number of ranks with argument " + ants.ras_ranks);
	} else {
	    System.out.println("Note: Number of ranks is set to default " + ants.ras_ranks);
	}

	if (cmd.hasOption("k")) {
		localSearch.nn_ls = Integer.parseInt(cmd.getOptionValue("k"));
	    System.out.println("-k/nnls Number nearest neighbours with argument " + localSearch.nn_ls);
	} else {
	    System.out
		    .println("Note: Number nearest neighbours in local search is set to default " + localSearch.nn_ls);
	}

	if (cmd.hasOption("d")) {
		localSearch.dlb_flag = true;
	    System.out.println("-d/dlb Don't-look-bits flag with argument " + localSearch.dlb_flag);
	} else {
	    System.out.println("Note: Don't-look-bits flag is set to default " + localSearch.dlb_flag);
	}

	if (cmd.hasOption("p")) {
		inOut.nLocIdsToDrop = Integer.parseInt(cmd.getOptionValue("p"));
	} else {
	    System.out.println("Note: Number of locations to drop is set to default (0)");
	    inOut.nLocIdsToDrop = 0;
    }


	if (cmd.hasOption("g")) {
		ants.nn_ants = Integer.parseInt(cmd.getOptionValue("g"));
	}
	
	if (cmd.hasOption("sl")) {
		inOut.startLocation = Integer.parseInt(cmd.getOptionValue("sl"));
	} else {
	    System.out.println("Note: start location is set to a random location");
    }

	return 0;
    }
}
