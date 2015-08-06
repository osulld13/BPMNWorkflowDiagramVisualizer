

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

/**
 * Servlet implementation class BPMNAMASERestClient
 */
@WebServlet("/BPMNAMASERestClient")
public class BPMNAMASERestClient extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public BPMNAMASERestClient() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
					    
    	String processID = "avico-module1-v2";		    
	    
    	String bpmnRelativeDirectoryPath = "/BPMNData";
	    checkForAndCreateNewDirectory(bpmnRelativeDirectoryPath);

	    /*
	     * Write AMASE response data to a new file if it exists
	     */
	    File bpmnFile = new File(getServletContext().getRealPath(bpmnRelativeDirectoryPath + "/" + processID + ".bpmn"));
	    if(!bpmnFile.exists()){
	    	createNewFile(bpmnFile, retrieveAMASEData(processID));
	    }
	    
	    /*
	     * Create graph data directory and parse JBPM file into JSON file
	     */
	    String jsonDirectoryPath = "/GraphData";
	    checkForAndCreateNewDirectory(jsonDirectoryPath);
	    
	    File jsonFile = new File(getServletContext().getRealPath(jsonDirectoryPath + "/" + processID + ".js"));
	    if(!jsonFile.exists()){
	    	BPMNXMLtoJSONParser parser = new BPMNXMLtoJSONParser();
		    String XMLFilePath = getServletContext().getRealPath("/BPMNData/" + processID + ".bpmn");
		    String JSONFilePath = getServletContext().getRealPath("/GraphData/" + processID + ".js");
		    try {
				parser.parseBPMNFile(XMLFilePath, JSONFilePath);
			} catch (Exception e) {
				e.printStackTrace();
			}
	    }	    
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}
	
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
	
	
}
