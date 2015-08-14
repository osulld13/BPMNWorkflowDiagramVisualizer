

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class ProcessSelector
 */
@WebServlet("/ProcessSelector")
public class ProcessSelector extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ProcessSelector() {
        super();
        // TODO Auto-generated constructor stub
    }

	/*
	 * Writes sends a form to the user where they  can select the diagram the wish to use
	 * as well as define the process state they wish to use.
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.setContentType("text/html");
		PrintWriter out = response.getWriter();
		
		out.println(
			"<!DOCTYPE html>"
			+ "<html>"
			+ "<head></head>"
			+ "<body>"
			+ "<form action=\"BPMNAMASERestClient\" method=\"POST\">"
			+ "<select name=\"process\">"
			+ "<option value=\"avico-module1-v2\">avico-module1-v2</option>"
			+ "<option value=\"java3\">java3</option>"
			+ "<option value=\"java5\">java5</option>"
			+ "</select>"
			+ "<input type=\"text\" name=\"processInstanceID\" />"
			+ "<input type=\"submit\" value=\"submit\">"
			+ "</form>"
			+ "</body>"
		);
		
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}

}
