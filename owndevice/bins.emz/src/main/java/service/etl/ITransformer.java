package service.etl;

import java.util.List;

import service.Bin;

public interface ITransformer {
	void transform();
	List<String[]> getBadBeans();
	void setBadBeans(List<String[]> badBeans);
	List<Bin> getGoodBeans();
	void setGoodBeans(List<Bin> goodBeans);
}
