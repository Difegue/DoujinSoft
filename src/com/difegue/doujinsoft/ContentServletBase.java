package com.difegue.doujinsoft;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.difegue.doujinsoft.utils.TemplateBuilder;


/**
 * Base servlet implementation for DIY Content - Handles GET/POST with json output.
 */
public abstract class ContentServletBase extends HttpServlet {
	private static final long serialVersionUID = 1L;
    private Logger ServletLog;

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response, int contentType) throws ServletException, IOException {

		if (request.getParameterMap().containsKey("format") && request.getParameter("format").equals("json"))
			response.setContentType("application/json; charset=UTF-8");
		else 
			response.setContentType("text/html; charset=UTF-8");

		ServletContext application = getServletConfig().getServletContext();			
		String output = ""; 
		
		try {
	    	output = new TemplateBuilder(application, request).doStandardPageGeneric(contentType);
			response.getWriter().append(output);
		} catch (Exception e) {
			ServletLog.log(Level.SEVERE, e.getMessage());
		}

	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response, int contentType) throws ServletException, IOException {
		
		if (request.getParameterMap().containsKey("format") && request.getParameter("format").equals("json")) 
			response.setContentType("application/json; charset=UTF-8");
		else 
            response.setContentType("text/html; charset=UTF-8");
            
		ServletContext application = getServletConfig().getServletContext();	
		String output = "Who are you running from?";
		
		try {
			
			if (!request.getParameterMap().isEmpty())
				output = new TemplateBuilder(application, request).doSearchGeneric(contentType);

			response.getWriter().append(output);
			
		} catch (Exception e) {
			ServletLog.log(Level.SEVERE, e.getMessage());
		}
		
	}

    /**
     * @see HttpServlet#HttpServlet()
     */
    public ContentServletBase(String servletName) {
        super(); 
        ServletLog = Logger.getLogger(servletName);
        ServletLog.addHandler(new StreamHandler(System.out, new SimpleFormatter()));     
    }
   
}
