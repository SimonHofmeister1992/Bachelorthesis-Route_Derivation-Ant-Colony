package service.etl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

public class Logger {
	
	private static Logger logger = null;
	private PrintWriter pw;
	
	private Logger() {
		File file = new File("loggedBadBeans.txt");
		
		FileWriter fw;
		try {
			fw = new FileWriter(file, true);
			BufferedWriter bw = new BufferedWriter(fw);
			this.pw = new PrintWriter(bw);			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static Logger getInstance() {
		if(Logger.logger==null) Logger.logger = new Logger();
		return Logger.logger;
	}
		
	public void log(String stage, Long orgId, Long fraktionId, Long regionId, Long devicenum, String message) {
		String s = "Date: " + new Date()
				+ ", " + "stage: " + stage
				+ ", " + "organizationId: " + orgId
		+ ", " + "fraktionId: " + fraktionId
		+ ", " + "regionId: " + regionId
		+ ", " + "deviceNumber: " + devicenum
		+ ", " + "message: " + message;
		pw.flush();
	}
	
	public void closeWriter() {
			pw.close();
	}
}
