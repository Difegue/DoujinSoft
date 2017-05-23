package com.difegue.doujinsoft;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
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

import com.difegue.doujinsoft.templates.Game;
import com.difegue.doujinsoft.utils.MioUtils.Types;
import com.difegue.doujinsoft.utils.ServletUtils;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.error.PebbleException;
import com.mitchellbosecke.pebble.template.PebbleTemplate;


/**
 * Servlet implementation class for Collections
 * Collections specify a list of MIO IDs from a JSON file present in [DATADIRECTORY]/collections.
 * From that list, we build and return a page containing only those IDs.
 */
@WebServlet("/collection/*")
public class CollectionServlet extends HttpServlet {

	class Collection {
		  public String collection_name;
		  public int collection_desc;
		  public String[] mios;
	}
	
	private static final long serialVersionUID = 1L;
    private Logger ServletLog;
    
    /*
     * Get the matching Collection object for the requested collection.
     */
	private Collection initCollection(HttpServletRequest request) throws FileNotFoundException {
		
		ServletContext application = getServletConfig().getServletContext();	
		String dataDir = application.getInitParameter("dataDirectory");

		if (!new File (dataDir+"/collections").exists())
		  	  new File(dataDir+"/collections").mkdirs();
		
		//Collection name is after the /collection/ part of the URL
		String collectionName = request.getPathInfo().substring(1);
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
		String dataDir = application.getInitParameter("dataDirectory");
		String output = "Collection doesn't exist!";
			
		try {
			Collection c = initCollection(request);
			if (c!=null)
				output = doStandardPageCollection(Types.GAME, c, application);
			
			response.getWriter().append(output);
		} catch (Exception e) {
			ServletLog.log(Level.SEVERE, e.getMessage());
		}
	
		//SELECT * FROM TABLE WHERE id IN (id1, id2, ..., idn)
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
				output = doSearchCollection(Types.GAME, c, application, request);

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
