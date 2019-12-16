package de.routecalculation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.springframework.util.ResourceUtils;

public class Config {

	private Properties configFile;
	private String fileName = "config.properties";
	public Config() {
		configFile = new java.util.Properties();
		try {
			File file = ResourceUtils.getFile(fileName);
			System.out.println("Properties-File found : " + file.exists());
			InputStream is = new FileInputStream(file);
			configFile.load(is);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	public String getProperty(String key) {
		String value = this.configFile.getProperty(key);
		return value;
	}
	
}
