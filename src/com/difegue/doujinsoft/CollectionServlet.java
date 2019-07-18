package com.difegue.doujinsoft;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.io.PrintWriter;
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

import com.difegue.doujinsoft.templates.Collection;
import com.difegue.doujinsoft.utils.TemplateBuilderCollection;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;


/**
 * Servlet implementation class for Collections
 * Collections specify a list of MIO IDs from a JSON file present in WEB-INF/collections.
 * From that list, we build and return a page containing only those IDs.
 */
@WebServlet("/collection")
public class CollectionServlet extends HttpServlet {
	
	private static final long serialVersionUID = 1L;
    private Logger ServletLog;
    
    /*
     * Get the matching Collection object for the requested collection.
     */
	private Collection initCollection(HttpServletRequest request) throws FileNotFoundException {
		
		ServletContext application = getServletConfig().getServletContext();	
		String dataDir = application.getInitParameter("dataDirectory");
		
		//Collection name is after the /collection/ part of the URL
		String collectionName = request.getParameter("id");
		
		String collectionFile = dataDir+"/collections/"+collectionName+".json";
		
		if (new File(collectionFile).exists())
		{
			//Try opening the matching JSON file 
			Gson gson = new Gson();
			JsonReader jsonReader = new JsonReader(new FileReader(collectionFile));
			//Auto bind the json to a class
			return gson.fromJson(jsonReader, Collection.class);
		}
		
		return null;
	}
    
    
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		response.setContentType("text/html; charset=UTF-8");
		ServletContext application = getServletConfig().getServletContext();
		String output = "Collection doesn't exist!";
			
		try {
			Collection c = initCollection(request);
			if (c!=null)
				output = new TemplateBuilderCollection(application, request).doStandardPageCollection(c);
			
			response.getWriter().append(output);
		} catch (Exception e) {

			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);

			ServletLog.log(Level.SEVERE, sw.toString());
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
			Collection c = initCollection(request);
			
			if (!request.getParameterMap().isEmpty() && c!=null)
				output = new TemplateBuilderCollection(application, request).doSearchCollection(c);

			response.getWriter().append(output);
			
		} catch (Exception e) {
			ServletLog.log(Level.SEVERE, e.getMessage());
		}
		
	}
	
	
    /**
     * @see HttpServlet#HttpServlet()
     */
    public CollectionServlet() {
        super(); 
        ServletLog = Logger.getLogger("CollectionServlet");
        ServletLog.addHandler(new StreamHandler(System.out, new SimpleFormatter()));     
    }
   
}
