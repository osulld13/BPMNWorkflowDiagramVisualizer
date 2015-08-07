

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.UriBuilder;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;


@WebServlet("/BPMNAMASERestClient")
public class BPMNAMASERestClient extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    public BPMNAMASERestClient() {
        super();
    }

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
					    
    	String processID = "avico-module1-v2";		    
	    
    	String bpmnRelativeDirectoryPath = "/BPMNData";
	    checkForAndCreateNewDirectory(bpmnRelativeDirectoryPath);

	    // Write AMASE response data to a new file if it exists	    
	    File bpmnFile = new File(getServletContext().getRealPath(bpmnRelativeDirectoryPath + "/" + processID + ".bpmn"));
	    if(!bpmnFile.exists()){
	    	createNewFile(bpmnFile, retrieveAMASEData(processID));
	    }
	    
	    
	    //Create graph data directory and parse JBPM file into JSON file
	    String jsonDirectoryPath = "/GraphData";
	    checkForAndCreateNewDirectory(jsonDirectoryPath);
	    File jsonFile = new File(getServletContext().getRealPath(jsonDirectoryPath + "/" + processID + ".js"));
	    if(!jsonFile.exists()){
	    	writeBPMNToJSON(processID);
	    }
	    
	    response = writeToDom(response);
	    
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {}
	
	/*
	 * Retrieve data from AMASE engine
	 */
	private String retrieveAMASEData(String processID){
		String SERVER_LOCATION = "sweep.scss.tcd.ie";
		URI uri = UriBuilder.fromUri("http://"+SERVER_LOCATION+"/amase.services").build();
		ClientConfig config = new DefaultClientConfig();
		Client client = Client.create(config);
		WebResource service = client.resource( uri );
    	return service.path("process-file").path(processID).get(String.class);		
	}
	
	/*
     * Checking for and creating folder
     */
	private void checkForAndCreateNewDirectory(String relativeFolderPath ){
	    String absoluteFolderPath = getServletContext().getRealPath(relativeFolderPath); 
	    if(Files.notExists( Paths.get(absoluteFolderPath) )){
	    	(new File(absoluteFolderPath)).mkdirs();
	    }
	}
	
	private void createNewFile(File file, String content) throws IOException{
	    file.createNewFile();
	    FileWriter fileOut = new FileWriter(file);
	    fileOut.write(content, 0, content.length());
	    fileOut.flush();
	    fileOut.close();
	}
	
	private void writeBPMNToJSON(String processID){
		BPMNXMLtoJSONParser parser = new BPMNXMLtoJSONParser();
	    String XMLFilePath = getServletContext().getRealPath("/BPMNData/" + processID + ".bpmn");
	    String JSONFilePath = getServletContext().getRealPath("/GraphData/" + processID + ".js");
	    try {
			parser.parseBPMNFile(XMLFilePath, JSONFilePath);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private HttpServletResponse writeToDom(HttpServletResponse response) throws IOException{
		response.setContentType("text/html");
	    PrintWriter out = response.getWriter();
	    out.println(
	    "<!DOCTYPE html>" +
		"<html>" +
		  "<head>" +
		    "<link type=\"text/css\" rel=\"stylesheet\" href=\"CSS/reset.css\"/>" +
		    "<link type=\"text/css\" rel=\"stylesheet\" href=\"CSS/stylesheet.css\"/>" +
		  "</head>" +
		  "<body>" +
		    "<h1>BPMN Diagram Visualiser</h1>" +
		    "<form action=\"\" method=\"GET\">" +
		      "<input type=\"text\" name=\"activityName\">" +
		      "<input type=\"submit\" name=\"submit\">" +
		    "</form>" +
		    "<div id=\"container\" >" +
		      "<div id=\"myDiagramDiv\"></div>" +
		
		      "<div id=\"nav_bar\">" +
		        "<div id=\"nav_bar_contents\">" +
		          "<span id=\"nav_bar_label\">Navigation: </span>" +
		          "<button class=\"nav_button\" id=\"prev_button\" onclick=\"prevNode();\"><< Prev</button>" +
		          "<button class=\"nav_button\" id=\"start_button\" onclick=\"startCourse();\">Start</button>" +
		          "<button class=\"nav_button\" id=\"next_button\" onclick=\"nextNode();\">Next >></button>" +
		        "</div>" +
		      "</div>" +
		
		      "<div id=\"dataDisplayContainer\">" +
		        "<table id=\"dataDisplay\">" +
		        "</table>" +
		      "</div>" +
		    "</div>" +
		 
		  "</body>" +
		    "<script src=\"JS/lib/go-debug.js\"></script>" +
		    "<script src=\"diagram_data/graphData.js\"></script>" +
		    "<script src=\"JS/diagram_init.js\"></script>" +
		    "<script src=\"JS/diagram_interaction.js\"></script>" +
		    "<script src=\"JS/data_display.js\"></script>" +
		    "<script src=\"JS/course_interaction.js\"></script>" +
		    "<script src=\"JS/main.js\"></script>" +
		"</html>"
	    );
	    return response;
	}
	
}
