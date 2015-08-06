import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.drools.KnowledgeBase;
import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderError;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.builder.ResourceType;
import org.drools.definition.process.Connection;
import org.drools.definition.process.Process;
import org.drools.io.Resource;
import org.drools.io.ResourceFactory;
import org.jbpm.process.core.context.variable.Variable;
import org.jbpm.ruleflow.core.RuleFlowProcess;
import org.jbpm.workflow.core.Constraint;
import org.jbpm.workflow.core.node.Join;
import org.jbpm.workflow.core.node.Split;
import org.jbpm.workflow.core.node.CompositeContextNode;
import org.jbpm.workflow.core.node.EventNode;

public class BPMNXMLtoJSONParser {

	
	private static String closeObject = "},\n";
	private static String openObject = "{\n";
	
	private static String startProcessesString = "processes";
	private static String startNodeDataString = "nodeData";
	private static String startVariablesString = "variables";
	private static String startConnectionsString = "connections";
	
	private static String bpmnFileName = "sqlcourse8.5.bpmn";
	//private static String bpmnFileName = "java2.bpmn";
	
	private static String gatewayName = "Gateway";
	
	private static ArrayList<CompositeContextNode> subProcessList = new ArrayList<CompositeContextNode>();

	public BPMNXMLtoJSONParser(){
	}
	
	public void parseBPMNFile(String XMLFilePath, String JSONFilePath) throws Exception {
		
		//String absoluteFolderPath = getServletContext().getRealPath(relativeFolderPath);
		
		//Only one of these can be included
		String processFile = XMLFilePath;		
		
		//FileWriter out = new FileWriter("diagram_data/graphData.js");
		FileWriter out = new FileWriter(JSONFilePath);
		/* 
		 * Keeps track of the level of nesting in the file is being written to.
		 * Tabs are added to output string accordingly
		 */
		int currentNesting = 0;

		// load up the knowledge base
		//KnowledgeBase kbase = readKnowledgeBase(processFile);
		KnowledgeBase kbase = readKnowledgeBase(XMLFilePath);
		
		//retrieve the processes from the knowledge base
		Collection<Process> processes = kbase.getProcesses();
				
		writeProcessesToFile(processes, out, currentNesting);
		
		out.close();
		
	}
	
	/* 
	 * Function that writes all of the processes and their contents to a JSON file
	 * The parameters for the function are: 
	 *  - A collection of processes
	 *  - A FileWriter
	 *  - An integer representing the current nesting level
	 * Behaviour:
	 *  The collection of processes is transformed into an array so that it may be iterable.
	 *  Each of its attributes is then extracted and written to the JSON output file
	 */	
	private static void writeProcessesToFile(Collection<Process> processes, FileWriter out, int currentNesting) throws IOException{
		
		currentNesting = newJSON(startProcessesString, out, currentNesting);
		
		for(Process uncastProcess: processes.toArray(new Process [0])){
			
			openJSONObject(out, currentNesting);
		
			RuleFlowProcess process = (RuleFlowProcess) uncastProcess;
			
			writeDataToJSONFile("id", process.getId(), out, currentNesting);
			writeDataToJSONFile("name", process.getName(), out, currentNesting);
			writeDataToJSONFile("packageName", process.getPackageName(), out, currentNesting);
			writeVariablesToFile(process, out, currentNesting);
			writeNodesToFile(process, out, currentNesting);
			writeConnectionsToFile(process, out, currentNesting);

			closeJSONObject(out, currentNesting);
		}
		
		currentNesting = endJSON(out, currentNesting);
	}
	
	/*
	 * A function that writes the variables contained within a process to the output file.
	 * The parameters for the function are: 
	 * 	- The RuleFlowProcess that is to be operated on.
	 * 	- The file writer that is to be used
	 *  - The current level of nesting being operated at
	 * Behaviour
	 *  The list of variables stored within the process is iterated over and the relevant 
	 *  values are written to the output file.
	 */
	private static void writeVariablesToFile(RuleFlowProcess process, FileWriter out, int currentNesting) throws IOException{

		currentNesting = openArray(startVariablesString, out, currentNesting);
		
		for (Variable v: process.getVariableScope().getVariables()){
			
			openJSONObject(out, currentNesting);
			
			writeDataToJSONFile("name", v.getName(), out, currentNesting);
			
			writeDataToJSONFile("value", v.getValue(), out, currentNesting);
			
			closeJSONObject(out, currentNesting);
		}
		currentNesting = closeArray(out, currentNesting);
	}	

	/*
	 * A function that writes the nodes contained within a process to the output file.
	 * The parameters for the function are: 
	 * 	- The RuleFlowProcess that is to be operated on.
	 * 	- The file writer that is to be used
	 *  - The current level of nesting being operated at
	 * Behaviour:
	 *  The list of nodes stored within the process is iterated over and the relevant 
	 *  values are written to the output file.
	 */
	private static void writeNodesToFile(RuleFlowProcess process, FileWriter out, int currentNesting) throws IOException{

		currentNesting = openArray(startNodeDataString, out, currentNesting);
		
		for (org.drools.definition.process.Node n: process.getNodes()){
			
			openJSONObject(out, currentNesting);
			
			//get node type
			//The type of a string is included as a substring in its class definition.
			//To get the type of a node it must be extracted from the class definition
			String type = n.getClass().toString().substring(34);
			if(type.equals("Join") || type.equals("Split")){
				type = gatewayName;
			}
			writeDataToJSONFile("nodeType", type, out, currentNesting);
			
			//get Name of node
			//If the node is a gateway, we use its type as its name
			String name;
			if(n.getName().equals(gatewayName)){
				int gatewayType;
				try{
					Join nTemp = (Join)n;
					gatewayType = nTemp.getType();
				}catch(Exception e){
					Split nTemp = (Split)n;
					gatewayType = nTemp.getType();
				}
				switch(gatewayType){
					case Join.TYPE_AND:
						name = "AND";
						break;
					case Join.TYPE_XOR:
						name = "XOR";
						break;
					default:
						name = null;
						break;
				}
			}
			else if (type.equals("EventNode")){
				EventNode event = (EventNode) n;
				name = event.getType();
			}
			else {
				name = n.getName();
			}
			
			writeDataToJSONFile("name", name, out, currentNesting);
			
			//get userId of node
			//user Id is a variable that is found within the process
			Variable v = process.getVariableScope().findVariable("userID");
			if (v != null){
				writeDataToJSONFile("userID", v.getValue(), out, currentNesting);
			}
			
			//get id of node			
			writeDataToJSONFile("key", n.getId(), out, currentNesting);

			
			if(type.equals("CompositeContextNode")){
				writeDataToJSONFile("isGroup", "true", out, currentNesting);
			}
			
			writeDataToJSONFile("completed", "false", out, currentNesting);
			
			closeJSONObject(out, currentNesting);
			
			if(type.equals("CompositeContextNode")){
				CompositeContextNode subProcessN = (CompositeContextNode) n;
				writeSubProcessNodesToFile(process, subProcessN, out, currentNesting);
				subProcessList.add(subProcessN);
			}
						
		}
		currentNesting = closeArray(out, currentNesting);
	}
	
	private static void writeSubProcessNodesToFile(RuleFlowProcess process, CompositeContextNode subProcess, FileWriter out, int currentNesting) throws IOException{
		
		for (org.drools.definition.process.Node n: subProcess.getNodes()){
			
			openJSONObject(out, currentNesting);
			
			//get node type
			//The type of a string is included as a substring in its class definition.
			//To get the type of a node it must be extracted from the class definition
			String type = n.getClass().toString().substring(34);
			if(type.equals("Join") || type.equals("Split")){
				type = gatewayName;
			}
			writeDataToJSONFile("nodeType", type, out, currentNesting);
			
			//get Name of node
			//If the node is a gateway, we use its type as its name
			String name;
			if(n.getName().equals(gatewayName)){
				int gatewayType;
				try{
					Join nTemp = (Join)n;
					gatewayType = nTemp.getType();
				}catch(Exception e){
					Split nTemp = (Split)n;
					gatewayType = nTemp.getType();
				}
				switch(gatewayType){
					case Join.TYPE_AND:
						name = "AND";
						break;
					case Join.TYPE_XOR:
						name = "XOR";
						break;
					default:
						name = null;
						break;
				}
			}
			else if (type.equals("EventNode")){
				EventNode event = (EventNode) n;
				name = event.getType();
			}
			else {
				name = n.getName();
			}
			
			writeDataToJSONFile("name", name, out, currentNesting);
			
			//get userId of node
			//user Id is a variable that is found within the process
			Variable v = process.getVariableScope().findVariable("userID");
			if (v != null){
				writeDataToJSONFile("userID", v.getValue(), out, currentNesting);
			}
			
			//get id of node			
			writeDataToJSONFile("key", subProcess.getId() + "-" + n.getId(), out, currentNesting);

			
			if(type.equals("CompositeContextNode")){
				writeDataToJSONFile("isGroup", "true", out, currentNesting);
			}
			
			//get group of node			
			writeDataToJSONFile("group", subProcess.getId(), out, currentNesting);
			
			writeDataToJSONFile("completed", "false", out, currentNesting);

			closeJSONObject(out, currentNesting);
			
			if(type.equals("CompositeContextNode")){
				CompositeContextNode subProcessN = (CompositeContextNode) n;
				writeSubProcessNodesToFile(process, subProcessN, out, currentNesting);
				subProcessList.add(subProcessN);
			}
			
			
		}
	}

	/*
	 * A function that writes the connections contained within a process to the output file.
	 * The parameters for the function are: 
	 * 	- The RuleFlowProcess that is to be operated on.
	 * 	- The file writer that is to be used
	 *  - The current level of nesting being operated at
	 * Behaviour:
	 *  The list of nodes stored within the process is iterated over and the connections for
	 *  each node are extracted.
	 *  The data within each connection is then extracted and written to the output file.
	 */
	private static void writeConnectionsToFile(RuleFlowProcess process, FileWriter out, int currentNesting) throws IOException{

		currentNesting = openArray(startConnectionsString, out, currentNesting);
		
		for (org.drools.definition.process.Node n: process.getNodes()){
			
			//Connections are retrieved from node as a Map of String, List<Connection>
			Map<String, List<Connection>> connectionListCollection = n.getOutgoingConnections();
			//Map is converted to its entry set so that it may be iterated on.
			Set<Map.Entry<String, List<Connection>>> connectionEntrySet = connectionListCollection.entrySet();
			
			//Here the entry set is iterated over and the List of connections in each value is retrieved
			for(Map.Entry<String, List<Connection>> entry: connectionEntrySet){
				//Each list of connections is then iterated over and the individual connection values are retrieved
				for(Connection con: entry.getValue()){

					openJSONObject(out, currentNesting);
					
					writeDataToJSONFile("from", con.getFrom().getId(), out, currentNesting);
					writeDataToJSONFile("to", con.getTo().getId(), out, currentNesting);
					
					String type = n.getClass().toString().substring(34);
					if(type.equals("Split")){
						org.jbpm.workflow.core.node.Split split = (Split) n;
						if(split.getType() == Split.TYPE_XOR || split.getType() == Split.TYPE_OR ){
							Constraint constraint = (Constraint)split.getConstraint(con);		                
				            writeDataToJSONFile("condition", constraint.getConstraint(), out, currentNesting);
						}
					}
					
					closeJSONObject(out, currentNesting);
				}
			}
		}
		
		//write subprocess connections to file
		for(CompositeContextNode subProcess: subProcessList){
			for (org.drools.definition.process.Node n: subProcess.getNodes()){
				
				//Connections are retrieved from node as a Map of String, List<Connection>
				Map<String, List<Connection>> connectionListCollection = n.getOutgoingConnections();
				//Map is converted to its entry set so that it may be iterated on.
				Set<Map.Entry<String, List<Connection>>> connectionEntrySet = connectionListCollection.entrySet();
				
				//Here the entry set is iterated over and the List of connections in each value is retrieved
				for(Map.Entry<String, List<Connection>> entry: connectionEntrySet){
					//Each list of connections is then iterated over and the individual connection values are retrieved
					for(Connection con: entry.getValue()){

						openJSONObject(out, currentNesting);
						
						writeDataToJSONFile("from", subProcess.getId() + "-" + con.getFrom().getId(), out, currentNesting);
						writeDataToJSONFile("to", subProcess.getId() + "-" + con.getTo().getId(), out, currentNesting);
						
						String type = n.getClass().toString().substring(34);
						if(type.equals("Split")){
							org.jbpm.workflow.core.node.Split split = (Split) n;
							if(split.getType() == Split.TYPE_XOR || split.getType() == Split.TYPE_OR ){
								Constraint constraint = (Constraint)split.getConstraint(con);		                
					            writeDataToJSONFile("condition", constraint.getConstraint(), out, currentNesting);
							}
						}
						
						closeJSONObject(out, currentNesting);
					}
				}
			}
		}
		
		currentNesting = closeArray(out, currentNesting);
	}
	

	/*
	 * A function that opens new array within the output JSON file.
	 * The parameters for the function are: 
	 * 	- The name of the array that is to be opened.
	 * 	- The file writer that is to be used
	 *  - The current level of nesting being operated at
	 *  Returns:
	 *  - The adjusted value for the current level of nesting.
	 */
	private static int openArray(String name, FileWriter out, int currentNesting) throws IOException{
		String tofile ="";
		tofile = addNestingToString(tofile, currentNesting);
		tofile += "\"" + name + "\"" + ": [\n";
		out.write(tofile, 0, tofile.length());
		return currentNesting + 1;
	}
	
	/*
	 * A function that closes an array within the output JSON file.
	 * The parameters for the function are: 
	 * 	- The file writer that is to be used
	 *  - The current level of nesting being operated at
	 * Returns:
	 *  - The adjusted value for the current level of nesting.
	 */
	private static int closeArray(FileWriter out, int currentNesting) throws IOException{
		String tofile ="";
		currentNesting = currentNesting - 1;
		tofile = addNestingToString(tofile, currentNesting);
		tofile += "],\n";
		out.write(tofile, 0, tofile.length());
		return currentNesting;
	}
	
	/*
	 * A function that applies the appropriate level of nesting to entries within the output JSON file.
	 * The parameters for the function are: 
	 * 	- The file writer that is to be used
	 *  - The current level of nesting being operated at
	 *  Returns:
	 *   - The appropriately formated string.
	 */
	private static String addNestingToString(String In, int currentNesting){
		String tofile ="";
		for(int i = 0; i < currentNesting; i ++){
			tofile += "\t";
		}
		tofile+=In;
		return tofile;
	}
	
	/*
	 * Formats data for entry within the output JSON file.
	 * The parameters for the function are: 
	 * 	- Name of data to be used
	 *  - Data to be used
	 * Returns:
	 *   - The appropriately formated string.
	 */
	private static String toJSONFormat(String dataName, Object data){
		String dataStr = "";
		try{
			dataStr = "\"" + dataName + "\": " + Integer.parseInt(data.toString());
		}catch(Exception e){
			dataStr = "\"" + dataName + "\": " + "\"" + data + "\"";
		}
		return dataStr;
	}
	
	/*
	 * Writes data to the output JSON file.
	 * The parameters for the function are: 
	 * 	- Name of data to be used
	 *  - Data to be used
	 *  - FileWriter to be used
	 *  - The level of nesting currently being operated at.
	 */
	private static void writeDataToJSONFile(String dataName,  Object data, FileWriter out, int currentNesting) throws IOException{
		
		String JSONFormatedString = toJSONFormat(dataName, data);
		String tofile = addNestingToString(JSONFormatedString, currentNesting);
		tofile += ",\n";
		out.write(tofile, 0, tofile.length());
		
	}
	
	/*
	 * Reads a BPMN file and generates the associated knowledge base.
	 * The parameters for the function are: 
	 * 	- name of the file to be read
	 * Returns:
	 *  - The appropriate knowledge base.
	 */
	private static KnowledgeBase readKnowledgeBase(String file) throws Exception {
		KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
		kbuilder.add(ResourceFactory.newFileResource(file), ResourceType.BPMN2);
		if ( kbuilder.hasErrors() ) {
		    for(KnowledgeBuilderError e: kbuilder.getErrors()){
		    	System.out.println();
		    	System.out.println("Lines: " + e.getLines());
		    	System.out.println("Message: " + e.getMessage());
		    	System.out.println("Resource: " + e.getResource());
		    	System.out.println("Severity: " + e.getSeverity());
		    	System.out.println();
		    }
		 }
		assertFalse( kbuilder.hasErrors() );
		return kbuilder.newKnowledgeBase();
	}
	
	/*
	 * Opens a JSON object in the output file.
	 * Parameters:
	 * 	- The FileWriter to be used.
	 *  - The current level of nesting being operated at.
	 */
	private static void openJSONObject(FileWriter out, int currentNesting) throws IOException{
		String tofile = addNestingToString(openObject, currentNesting);
		out.write(tofile, 0, tofile.length());
	}

	/*
	 * Closes a JSON object in the output file.
	 * Parameters:
	 * 	- The FileWriter to be used.
	 *  - The current level of nesting being operated at.
	 */
	private static void closeJSONObject(FileWriter out, int currentNesting) throws IOException{
		String tofile = addNestingToString(closeObject, currentNesting);
		out.write(tofile, 0, tofile.length());
	}

	/*
	 * Creates a new variable containing JSON objects.
	 */
	private static int newJSON (String varName, FileWriter out, int currentNesting) throws IOException{
		String tofile = "var " + varName + " = [\n";
		tofile = addNestingToString(tofile, currentNesting);
		out.write(tofile, 0, tofile.length());
		return currentNesting + 1;
	}
	
	/*
	 * Closes a JSON variable.
	 */
	private static int endJSON (FileWriter out, int currentNesting) throws IOException{
		String tofile = "];\n";
		currentNesting = currentNesting - 1;
		tofile = addNestingToString(tofile, currentNesting);
		out.write(tofile, 0, tofile.length());
		return currentNesting;
	}
	
	public static void writeInputFile(File fileIn) throws IOException{
		FileReader in = new FileReader(fileIn);
				
				int x;
				while((x = in.read()) != -1)
					System.out.write(x);
				
				in.close();
	}
	
}
