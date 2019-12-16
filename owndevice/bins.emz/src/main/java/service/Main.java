package service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.web.client.RestTemplate;

import service.etl.CsvFileExtractor;
import service.etl.ETLStages;
import service.etl.IExtractor;
import service.etl.IStageNames;
import service.etl.ITransformer;
import service.etl.Loader;
import service.etl.Logger;
import service.etl.Transformer;


/**
 * 
 * @author Simon
 * extracts data of emz Hanauer and persists it into the database
 * csv-reader: only the latest file is concerned
 * a route can be calculated and appended into the file
 *
 */
public class Main {
	
	private static ArrayList<Bin> bins;
	private static long tourLength;

	public static void main(String[] args) {
		Logger logger = Logger.getInstance();
		
		Config config = new Config();
		Long orgId, fractionId, regionId;
		orgId = Long.valueOf(config.getProperty("orgId"));
		fractionId = Long.valueOf(config.getProperty("fractionId"));
		regionId = Long.valueOf(config.getProperty("regionId"));
		
		if(orgId==null || fractionId==null || regionId == null) {
			System.out.println("Please assure that the config file exists and the values for orgId, fraktionId and regionId are set correctly");
			return;
		}
		ETLStages etl = execETLProcess(logger, orgId, fractionId, regionId);

		CommandLineParser clp = new DefaultParser();
		CommandLine cliOptions = setCLIOptions(clp, args);
		
		
		if(etl.getLoader().getBadBeans().size() == 0 && !cliOptions.hasOption("sr")) {
			getMappingBinIdToLocId(config, orgId, fractionId, regionId);
			List<Bin> orderedBins = consumeAcoAlgorithm(config, etl.getLoader());				
			rewriteCsvFile(orderedBins, etl, config);
		}
		else {
			if(etl.getLoader().getBadBeans().size() != 0) System.out.println("Result was not written into the result file, bad beans exist");
		}
		logger.closeWriter();  
		System.out.println("Application finished task");
	}
	
	private static void getMappingBinIdToLocId(Config cfg, Long orgId, Long fractionId, Long regionId) {
		RestTemplate rt = new RestTemplate();
		
		String url = cfg.getProperty("pathToBinManagerService") + "/request-bins?"
				+"orgId=" + orgId + "&fractionId=" + fractionId + "&regionId=" + regionId
				+ "&infotype=binnum";
		Bin[] responseEntity = rt.getForObject(url, Bin[].class);
		bins = new ArrayList<Bin>();
		for(Bin b : responseEntity) {
			bins.add(b);
		}
	}

	private static void rewriteCsvFile(List<Bin> orderedBins, ETLStages etl, Config config) {
		ArrayList<String> newFileContent = new ArrayList<String>();
		String line;
		try {
			FileReader fr = new FileReader(etl.getExtractor().getFile());
			BufferedReader br = new BufferedReader(fr);
			int linenr=0;
			while((line = br.readLine()) != null) {
				if(linenr > CsvFileExtractor.LINE_OF_TABLE_HEADER) {
					break;
				}
				else if(linenr == CsvFileExtractor.LINE_OF_TABLE_HEADER) {
					line = "Reihenfolge" + ";" + line;
					newFileContent.add(line);
					linenr++;
				}
				else {
					newFileContent.add(line);
					linenr++;
				}
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		StringBuffer sb;
		for(Bin b : etl.getTransformer().getGoodBeans()) {
			for(Bin ordBin : orderedBins) {
				if(b.getBinid()==ordBin.getBinid()) {
				sb = new StringBuffer();
					sb.append(orderedBins.indexOf(ordBin));
					for(String s : b.getLineAsStringArray()) {
						sb.append(";" + s);
					}
					line=sb.toString();
					newFileContent.add(line);
				}
			}
		}
		line="Resultierende Route: Tour Länge: " + tourLength + "m";
		newFileContent.add(line);

		FileWriter fw;
		try {
			fw = new FileWriter(etl.getExtractor().getFile(),false);
			BufferedWriter bw = new BufferedWriter(fw);
			for(String l : newFileContent) {
				bw.write(l);
				bw.newLine();
			}
			bw.flush();
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static List<Bin> consumeAcoAlgorithm(Config config, Loader ld) {
//		String distanceAsString;
//		Long distance=Long.MAX_VALUE;
//		
		List<Bin> binsOrdered = new ArrayList();
		RestTemplate rt = new RestTemplate();
		Long slId = Long.parseLong(config.getProperty("startLocationId").trim());
		
		String url = config.getProperty("pathToBinAcoAlgorithm") + "?"
				+ "acs=true&tries=1&time=5";
		url += "&locationsToVisit=";
		List<Bin> gB = ld.getGoodBeans();
		for(Bin b : gB) {
			for(Bin a : bins) {
				if(b.getBinid()==a.getBinid()) {
					url += a.getLocId()+",";
					break;
				}
			}
		}
		if(slId>0) url += slId+ "&startLocationId=" + slId;
		
		String responseEntity = rt.getForObject(url, String.class);
		
		boolean done=false, cancelled=true, success=false;
		for(String s : responseEntity.split(",")) {
			if(s.contains("done")) {
				done=Boolean.parseBoolean(s.split(":")[1].replace("{", "").replace("}", ""));
			}
			if(s.contains("cancelled")) {
				cancelled=Boolean.parseBoolean(s.split(":")[1].replace("{", "").replace("}", ""));
			}
		}
		success = done && !cancelled;
		if(success) {
			
			for(String s : responseEntity.split("\"")) {
				// location reihenfolge extrahieren
				if(s.contains("route")) {
					String route = responseEntity.substring(responseEntity.indexOf("[")+1, responseEntity.indexOf("]"));
					for(String location : route.split(",")) {
						for(Bin b : bins) {
							if(Long.valueOf(location).equals(b.getLocId())) {
								binsOrdered.add(b);
							}
						}
					}
				}
				if(s.contains("tourLength")) {
					tourLength=Long.valueOf(responseEntity.split(",")[0].split(":")[1]);
				}
			}
		}

		return binsOrdered;
		
	}

	private static CommandLine setCLIOptions(CommandLineParser clp, String[] args) {
		CommandLine cmd = null;
		Options options = new Options();
		options.addOption("sr", "suppressresult", false, "# suppress writing into result file");
		try {
			cmd = clp.parse(options, args);
		} catch (ParseException e) {
		    System.err.println("Error: " + e.getMessage());
		    System.exit(1);
		}
		return cmd;
	}

	// execute ETL process, returns number of bad bins
	private static ETLStages execETLProcess(Logger logger, Long orgId, Long fractionId, Long regionId) {
		ETLStages etl = new ETLStages();
		IExtractor ex = new CsvFileExtractor();
		ITransformer tr = new Transformer(ex.getEntries(), ex.getMapHeaderNameToColumn());
		
		for(String[] bin : tr.getBadBeans()) {
			logger.log(IStageNames.TRANSFORM_STAGE, orgId, fractionId, regionId, null, bin.toString() + " failed");
		}
		
		Loader ld = new Loader(tr, orgId, fractionId, regionId);
		List<Long> badBeans = ld.load(); 
		
		for(Long deviceNum : badBeans) {
			logger.log(IStageNames.LOAD_STAGE, orgId, fractionId, regionId, deviceNum, "failed");
		}
		etl.setExtractor(ex);
		etl.setTransformer(tr);
		etl.setLoader(ld);
		return etl;
	}

}
