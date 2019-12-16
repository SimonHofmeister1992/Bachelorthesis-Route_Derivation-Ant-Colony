package service.etl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

public class CsvFileExtractor implements IExtractor {
		
	private Pattern datetime = Pattern.compile("\\D");
	private File file;
	private final static String FILE_SUFFIX = "csv";
	private final static File FILE_PATH = new File("src/main/resources");
	public final static int LINE_OF_TABLE_HEADER = 3; // started to count by 0
	final static String CSV_FILE_DELIMITER = ";";
	
	private HashMap<Integer, String> mapHeaderNameToColumn;
	private List<String[]> entries;

	public CsvFileExtractor() {
		File latestFile = findLatestFile();
		if(latestFile != null) {
			this.setEntries(readFile(latestFile));
		}
		else {
			System.out.println("no file found");
		}
	}
	
	private List<String[]> readFile(File file) {
		System.out.println("Filename: " +  file.getName());
		this.file=file;
		List<String[]> tableEntries = new ArrayList<String[]>();
		FileReader fr;
		BufferedReader br;
		try {
			fr = new FileReader(file);
			br = new BufferedReader(fr);
			String line="";
			int linenr = 0;
			setMapHeaderNameToColumn(new HashMap<Integer, String>());
			String[] lineEntries;
			while((line = br.readLine()) != null) {
				if(linenr < LINE_OF_TABLE_HEADER) {
					linenr++;
					continue;
				}
				else if (linenr == LINE_OF_TABLE_HEADER) {
					String[] header = line.split(CSV_FILE_DELIMITER);
					for (int i = 0; i < header.length; i++) {
						getMapHeaderNameToColumn().put(i, header[i].toLowerCase().trim());
					}
					linenr++;
				}
				else {
					lineEntries = line.split(CSV_FILE_DELIMITER);
					tableEntries.add(lineEntries);
					linenr++;
				}
			}
			br.close();
		} catch (FileNotFoundException e) {
			System.out.println("File not found, please assure it's located in src/main/ressources");
		} catch (IOException e) {
			System.out.println("Couldn't read content of the file. Please assure if the file is correct and that the standard (to developer: compare ITableColumnNames to csv-file) didn't change.");
		}
		return tableEntries;
		
	}
	
	private File findLatestFile() {
		File latestFile = null;
		Long date, newestDate=0L; // Format: YYYYMMDDhhmmss
		
		for(String file : CsvFileExtractor.FILE_PATH.list()) {
			if(file.endsWith(FILE_SUFFIX)){ {
				date = Long.parseLong(datetime.matcher(file).replaceAll(""));
				if(date > newestDate) {
					newestDate = date;
					latestFile = new File(CsvFileExtractor.FILE_PATH.getAbsoluteFile() + File.separator + file);
				}	
			}			
		}
		}
		return latestFile;
	}

	public List<String[]> getEntries() {
		return entries;
	}

	public void setEntries(List<String[]> entries) {
		this.entries = entries;
	}

	public HashMap<Integer, String> getMapHeaderNameToColumn() {
		return mapHeaderNameToColumn;
	}

	public void setMapHeaderNameToColumn(HashMap<Integer, String> mapHeaderNameToColumn) {
		this.mapHeaderNameToColumn = mapHeaderNameToColumn;
	}

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}

}
