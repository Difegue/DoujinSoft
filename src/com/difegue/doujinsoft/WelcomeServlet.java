package com.difegue.doujinsoft;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
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
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.error.PebbleException;
import com.mitchellbosecke.pebble.template.PebbleTemplate;


/**
 * Servlet implementation class Home
 */
@WebServlet("/home")
public class WelcomeServlet extends HttpServlet {
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
			
	    	output = doStandardPage(application);
			response.getWriter().append(output);
				
		} catch (SQLException | PebbleException e) {
			ServletLog.log(Level.SEVERE, e.getMessage());
		}

	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		doGet(request, response);
		
	}

    
    /**
     * @see HttpServlet#HttpServlet()
     */
    public WelcomeServlet() {
        super(); 
        ServletLog = Logger.getLogger("GameServlet");
        ServletLog.addHandler(new StreamHandler(System.out, new SimpleFormatter()));     
    }
   
    private ArrayList<Collection> getAllCollectionsInDataDir(String dataDir) throws FileNotFoundException {
    	
    	ArrayList<Collection> ret = new ArrayList<Collection>();
    	
		if (!new File (dataDir+"/collections/").exists())
			  new File(dataDir+"/collections/").mkdirs();
    	
    	File[] files = new File(dataDir+"/collections/").listFiles();
        
        for (File f: files) {
    	
        	//Try opening the matching JSON file 
			Gson gson = new Gson();
			JsonReader jsonReader = new JsonReader(new FileReader(f));
			//Auto bind the json to a class
			Collection c = gson.fromJson(jsonReader, Collection.class);
			c.id = f.getName().substring(0,f.getName().length()-5);
			ret.add(c);
        	
        }
    	
        return ret;
    }
    
    //Generates the homepage.
    private String doStandardPage(ServletContext application) throws PebbleException, SQLException, IOException {

    	Map<String, Object> context = new HashMap<>();
    	
    	Connection connection = null;
    	PebbleEngine engine = new PebbleEngine.Builder().build();
		PebbleTemplate compiledTemplate;

		//Getting base template
		compiledTemplate = engine.getTemplate(application.getRealPath("/WEB-INF/templates/home.html"));
		String dataDir = application.getInitParameter("dataDirectory");

		// create a database connection
	    connection = DriverManager.getConnection("jdbc:sqlite:"+dataDir+"/mioDatabase.sqlite");
	    Statement statement = connection.createStatement();
	    statement.setQueryTimeout(30);  // set timeout to 30 sec.
		
	    ResultSet result = statement.executeQuery("select COUNT(id) from Games");
		context.put("totalGames", result.getInt(1));
		
		result = statement.executeQuery("select COUNT(id) from Records");
		context.put("totalRecords", result.getInt(1));
		
		result = statement.executeQuery("select COUNT(id) from Manga");
		context.put("totalComics", result.getInt(1));
		
		context.put("collections", getAllCollectionsInDataDir(dataDir));
		
		//The little news string at the top of the page is stored in a news.txt file in the data directory.
		//Simple stuff.
		String news = "Welcome to DoujinSoft!";
		if (new File(dataDir+"/news.txt").exists()) {
			news = new Scanner(new File(dataDir+"/news.txt")).useDelimiter("\\Z").next();
		}
		context.put("news", news);
		
		//Output to client
		Writer writer = new StringWriter();
		compiledTemplate.evaluate(writer, context);
		String output = writer.toString();

		result.close();
		statement.close();
		connection.close();
		return output;
    	
    }

}
