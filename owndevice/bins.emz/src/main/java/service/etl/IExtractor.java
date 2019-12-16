package service.etl;

import java.io.File;
import java.util.HashMap;
import java.util.List;

public interface IExtractor {
	List<String[]> getEntries();
	void setEntries(List<String[]> entries);
	HashMap<Integer, String> getMapHeaderNameToColumn();
	void setMapHeaderNameToColumn(HashMap<Integer, String> mapHeaderNameToColumn);
	File getFile();
}
