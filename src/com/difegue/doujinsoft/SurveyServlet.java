package com.difegue.doujinsoft;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.difegue.doujinsoft.utils.TemplateBuilderSurvey;


/**
 * Servlet implementation class SurveyListing
 */
@WebServlet("/surveys")
public class SurveyServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
    private Logger ServletLog;
    
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		response.setContentType("text/html; charset=UTF-8");
		ServletContext application = getServletConfig().getServletContext();			
		String output = "";
		
		try {
	    	output = new TemplateBuilderSurvey(application, request).doGetSurveys();
			response.getWriter().append(output);
		} catch (Exception e) {
			ServletLog.log(Level.SEVERE, e.getMessage());
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		response.setContentType("text/html; charset=UTF-8");
		ServletContext application = getServletConfig().getServletContext();	
		String output = "Who are you running from?";
		
		try {
			
			if (!request.getParameterMap().isEmpty())
				output = new TemplateBuilderSurvey(application, request).doPostSurveys();

			response.getWriter().append(output);
			
		} catch (Exception e) {
			ServletLog.log(Level.SEVERE, e.getMessage());
		}
	}

    
    /**
     * @see HttpServlet#HttpServlet()
     */
    public SurveyServlet() {
        super(); 
        ServletLog = Logger.getLogger("SurveyServlet");
        ServletLog.addHandler(new StreamHandler(System.out, new SimpleFormatter()));     
    }
   
}
