package com.difegue.doujinsoft;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
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
import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.error.PebbleException;
import com.mitchellbosecke.pebble.template.PebbleTemplate;


/**
 * Servlet implementation class GameListing
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
   
    
    //Generates the regular landing page for games.
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
		
		//Output to client
		Writer writer = new StringWriter();
		compiledTemplate.evaluate(writer, context);
		String output = writer.toString();
		
		return output;
    	
    }

}
