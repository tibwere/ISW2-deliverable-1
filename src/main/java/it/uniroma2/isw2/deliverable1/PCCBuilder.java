package it.uniroma2.isw2.deliverable1;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class PCCBuilder {
	
	private static Logger LOG = Logger.getLogger("ISW2-deliverable-1");

	private String projectName;
	private String issueType;
	private String resolution;
	private String fields;
	private String status;
	

	public PCCBuilder(String projectName, String issueType, String resolution, String fields, String status) {
		this.projectName = projectName;
		this.issueType = issueType;
		this.resolution = resolution;
		this.fields = fields;
		this.status = status;
	}

	private String getAPIURL(int startIndex, int maxResults) {

		StringBuilder urlBuilder = new StringBuilder("https://issues.apache.org/jira/rest/api/2/search?jql=")
				.append("project=").append(this.projectName).append(" AND ")
				.append("issueType=").append(this.issueType).append(" AND ")
				.append("resolution=").append(this.resolution).append(" AND ")
				.append("status in (").append(this.status).append(")")
				.append("&fields=").append(this.fields)
				.append("&startAt=").append(startIndex)
				.append("&maxResults=").append(maxResults);
				
		String url = urlBuilder.toString().replace(" ", "%20").replace("\"", "%22");
		LOG.info("URL: " + url);
		return url;
	}

	private String getJSONResult(int startIndex, int maxResults) throws IOException {
		OkHttpClient client = new OkHttpClient();
		Request req = new Request.Builder()
				.url(this.getAPIURL(startIndex, maxResults))
				.build();
		
		Response res = client.newCall(req).execute();
		LOG.info("Retrieved results from JIRA");
		return res.body().string();
	}
	
	private List<String> extractDateList() throws JsonSyntaxException, IOException {
		int start = 0;
		int end = 0;
		int total = 0;
		List<String> dates = new ArrayList<>();
		
		do {
			end = start + Parameters.MAX_RESULTS;
			JsonElement body = JsonParser.parseString(this.getJSONResult(start, end));
			JsonArray jsonIssues = body.getAsJsonObject().get("issues").getAsJsonArray();
			total = body.getAsJsonObject().get("total").getAsInt();
			
			for (; start<total && start<end; ++start) {
				JsonObject jsonIssue = jsonIssues.get(start % Parameters.MAX_RESULTS).getAsJsonObject();
				String resolutionDate = jsonIssue.get("fields")
						.getAsJsonObject().get("resolutiondate")
						.getAsString();	
				LOG.info("Added new date: " + resolutionDate);
				dates.add(resolutionDate);
			}
		} while(start<total);
		
		return dates;
	}
	
	private Map<String, Integer> getRawValuesFromPCC() throws JsonSyntaxException, IOException {
		Map<String, Integer> points = new HashMap<>();
		List<String> dates = this.extractDateList();
		
		for (String d : dates) {
			/* From 2005-08-10T15:17:17.000+0000 to 2005-08 (7 chars) */
			String key = d.substring(0, 7);
			
			if (points.containsKey(key)) {
				int newCounter = points.get(key) + 1;
				LOG.info(String.format("Updated %s (new counter %d)", key, newCounter));
				points.put(key, newCounter);
			} else {
				LOG.info(String.format("%s inserted", key));
				points.put(key, 1);
			}
		}
		
		return points;
	}
	
	public void createChart() throws IOException {

		File chart = new File(String.format("%s_PCC_data.csv", this.projectName));
		
		Map<String, Integer> rawData = this.getRawValuesFromPCC();
		FileWriter csvWriter = new FileWriter(chart,false);
		
		/* Header of csv */
		csvWriter.append("Date,Number of tickets\n");
		for (String key : rawData.keySet()) {
			LOG.info(String.format("Writing \"%s,%s\" on file", key, rawData.get(key)));
			csvWriter.append(String.format("%s,%s\n", key, rawData.get(key)));
		}
		
		csvWriter.close();
	}


	public static void main(String[] args) throws IOException {
		
		PCCBuilder builder = new PCCBuilder(Parameters.PROJECT_NAME, Parameters.ISSUE_TYPE, Parameters.RESOLUTION,
				Parameters.FIELDS, Parameters.STATUS);

		builder.createChart();
	}
}
