package it.uniroma2.isw2.deliverable1;

public class Parameters {
	
	private Parameters() {
		throw new IllegalStateException("This class should not be instantiated");
	}
	public static final String PROJECT_NAME = "TAJO";	
	public static final String ISSUE_TYPE = "\"New Feature\"";
	public static final String RESOLUTION = "fixed";
	public static final String FIELDS= "resolutiondate";
	public static final String STATUS= "Resolved,Closed";
	public static final int MAX_RESULTS = 1000;
}
