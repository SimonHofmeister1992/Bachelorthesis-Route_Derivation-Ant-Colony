package service.etl;

public class ETLStages {
	private IExtractor extractor;
	private ITransformer transformer;
	private Loader loader;
	public IExtractor getExtractor() {
		return extractor;
	}
	public void setExtractor(IExtractor extractor) {
		this.extractor = extractor;
	}
	public ITransformer getTransformer() {
		return transformer;
	}
	public void setTransformer(ITransformer transformer) {
		this.transformer = transformer;
	}
	public Loader getLoader() {
		return loader;
	}
	public void setLoader(Loader loader) {
		this.loader = loader;
	}
	
}
