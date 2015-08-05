

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

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
		
		/*
		 * Connect to AMASE engine
		 */
		String SERVER_LOCATION = "sweep.scss.tcd.ie";
		URI uri = UriBuilder.fromUri("http://"+SERVER_LOCATION+"/amase.services").build();
		ClientConfig config = new DefaultClientConfig();
		Client client = Client.create(config);
		WebResource service = client.resource( uri );
			    
		/*
	 	 * calling specific service
	 	 */
    	String processID = "avico-module1-v2";		    
		
		/*
		 * Writing text to DOM
		 */
		PrintWriter domOut = response.getWriter();
	    domOut.println( "" );
		
	    /*
	     * Checking for and creating BPMNData folder
	     */
	    String bpmnFolderName = "BPMNData";
	    Path bpmnFolderPath = FileSystems.getDefault().getPath(bpmnFolderName) ;
	    if(Files.notExists( bpmnFolderPath )){
	    	(new File(bpmnFolderName)).mkdirs();
	    }

	    /*
	     * Write AMASE response data to a new file if it exists
	     */
	    File bpmnFile = new File(bpmnFolderName + "/" + processID + ".bpmn");
	    if(!bpmnFile.exists()){
	    	
	    	/*
	    	 * Retrieve data from AMASE engine
	    	 */
	    	String content = service.path("process-file").path(processID).get(String.class);
	    	
	    	/*
	    	 * Write data to file
	    	 */
		    bpmnFile.createNewFile();
		    FileWriter bpmnFileOut = new FileWriter(bpmnFile);
			bpmnFileOut.write(content, 0, content.length());
			bpmnFileOut.flush();
			bpmnFileOut.close();
	    }
		
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}

}
