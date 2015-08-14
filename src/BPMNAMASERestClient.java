import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.UriBuilder;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;

/*
 * This servlet process the request from the process selection form
 * and displays the relevant diagram with processState
 */

@WebServlet("/BPMNAMASERestClient")
public class BPMNAMASERestClient extends HttpServlet {
	private static final long serialVersionUID = 1L;

	public BPMNAMASERestClient() {
		super();
	}

	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		// Retrieve the request parameters
		String processID = request.getParameter("process");
		String processInstanceID = request.getParameter("processInstanceID");

		// Set the directory to write to
		String bpmnRelativeDirectoryPath = "/BPMNData";
		checkForAndCreateNewDirectory(bpmnRelativeDirectoryPath);

		// Write AMASE response data to a new file if it exists
		File bpmnFile = new File(getServletContext().getRealPath(
				bpmnRelativeDirectoryPath + "/" + processID + ".bpmn"));
		if (!bpmnFile.exists()) {
			createNewFile(bpmnFile, retrieveAMASEData(processID));
		}

		// Create graph data directory and parse JBPM file into JSON file
		String jsonDirectoryPath = "/GraphData";
		checkForAndCreateNewDirectory(jsonDirectoryPath);
		File jsonFile = new File(getServletContext().getRealPath(
				jsonDirectoryPath + "/" + processID + ".js"));
		if (!jsonFile.exists()) {
			try {
				writeBPMNToJSON(processID);
				// Program sleeps in order to allow for writing of json file
				Thread.sleep(5 * 1000);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		String processStateDataJsonString = "";
		
		if (!processInstanceID.equals("")) {
			// retrieve process state from server
			String processStateDataXml = retrieveProcessState(processInstanceID);

			// Convert process state from XML to JSON
			JSONObject processStateDataJson = new JSONObject();
			try {
				processStateDataJson = XML.toJSONObject(processStateDataXml);
				processStateDataJsonString = processStateDataJson.toString();
			} catch (JSONException je) {
				System.out.println(je.toString());
			}

			// the XML data makes use of the '-' char, which is incomptible with
			// json therefore this is removed.
			processStateDataJsonString = processStateDataJson.toString()
					.replace("-", "");
		}
		response = writeToDom(response, processID, processStateDataJsonString);

	}

	/*
	 * Post requests redirect input into the doGet() method
	 */
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

	/*
	 * Retrieve data from AMASE engine
	 */
	private String retrieveAMASEData(String processID) {
		String SERVER_LOCATION = "sweep.scss.tcd.ie";
		URI uri = UriBuilder.fromUri(
				"http://" + SERVER_LOCATION + "/amase.services").build();
		ClientConfig config = new DefaultClientConfig();
		Client client = Client.create(config);
		WebResource service = client.resource(uri);
		return service.path("process-file").path(processID).get(String.class);
	}

	/*
	 * Retrieve data on process state from AMAS engine
	 */
	private String retrieveProcessState(String processInstanceID) {
		String SERVER_LOCATION = "sweep.scss.tcd.ie";
		URI uri = UriBuilder.fromUri(
				"http://" + SERVER_LOCATION + "/amase.services").build();
		ClientConfig config = new DefaultClientConfig();
		Client client = Client.create(config);
		WebResource service = client.resource(uri);
		return service.path("process-state").path(processInstanceID)
				.get(String.class);
	}

	/*
	 * Checking for and creating folder
	 */
	private void checkForAndCreateNewDirectory(String relativeFolderPath) {
		String absoluteFolderPath = getServletContext().getRealPath(
				relativeFolderPath);
		if (Files.notExists(Paths.get(absoluteFolderPath))) {
			(new File(absoluteFolderPath)).mkdirs();
		}
	}

	/*
	 * Creates a file with the content passed to the method
	 */
	private void createNewFile(File file, String content) throws IOException {
		file.createNewFile();
		FileWriter fileOut = new FileWriter(file);
		fileOut.write(content, 0, content.length());
		fileOut.flush();
		fileOut.close();

	}

	/*
	 * Creates a parser and converts the BPMN XML file content to JSON and
	 * writes it to a file.
	 */
	private void writeBPMNToJSON(String processID) throws Exception {
		BPMNXMLtoJSONParser parser = new BPMNXMLtoJSONParser();
		String XMLFilePath = getServletContext().getRealPath(
				"/BPMNData/" + processID + ".bpmn");
		String JSONFilePath = getServletContext().getRealPath(
				"/GraphData/" + processID + ".js");
		parser.parseBPMNFile(XMLFilePath, JSONFilePath);
	}

	/*
	 * Writes HTML response for user. The paths to the relevant data file for
	 * the diagram are dynamically generated using there processID and process
	 * state information is embedded in the document as JSON
	 */
	private HttpServletResponse writeToDom(HttpServletResponse response,
			String processID, String processStateData) throws IOException {
		response.setContentType("text/html");
		PrintWriter out = response.getWriter();
		out.println("<!DOCTYPE html>"
				+ "<html>"
				+ "<head>"
				+ "<link type=\"text/css\" rel=\"stylesheet\" href=\"CSS/reset.css\"/>"
				+ "<link type=\"text/css\" rel=\"stylesheet\" href=\"CSS/stylesheet.css\"/>"
				+ "</head>"
				+ "<body>"
				+ "<div id=\"container\" >"
				+ "<div id=\"myDiagramDiv\"></div>"
				+

				"<div id=\"nav_bar\">"
				+ "<div id=\"nav_bar_contents\">"
				+ "<span id=\"nav_bar_label\">Navigation: </span>"
				+ "<button class=\"nav_button\" id=\"prev_button\" onclick=\"prevNode();\"><< Prev</button>"
				+ "<button class=\"nav_button\" id=\"start_button\" onclick=\"startCourse();\">Start</button>"
				+ "<button class=\"nav_button\" id=\"next_button\" onclick=\"nextNode();\">Next >></button>"
				+ "</div>" + "</div>" +

				"<div id=\"dataDisplayContainer\">"
				+ "<table id=\"dataDisplay\">" + "</table>" + "</div>"
				+ "</div>" + "</body>"
				+ "<script src=\"JS/lib/go-debug.js\"></script>"
				+ "<script src=\"GraphData/" + processID + ".js\"></script>"
				+ "<script src=\"JS/diagram_init.js\"></script>"
				+ "<script src=\"JS/diagram_interaction.js\"></script>"
				+ "<script src=\"JS/data_display.js\"></script>"
				+ "<script src=\"JS/course_interaction.js\"></script>");

		//if the user has specified a processState the data is embedded as json in the response
		//document
		if (!processStateData.equals("")) {
			out.println("<script> var processStateData =" + processStateData + ";</script>");
		}
		
		out.println("<script src=\"JS/main.js\"></script></html>");
		return response;
	}
}
