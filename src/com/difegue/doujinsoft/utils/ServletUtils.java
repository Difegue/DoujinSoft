package com.difegue.doujinsoft.utils;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Constructor;
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

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import com.difegue.doujinsoft.templates.Collection;
import com.difegue.doujinsoft.templates.Game;
import com.difegue.doujinsoft.templates.Manga;
import com.difegue.doujinsoft.templates.Record;
import com.difegue.doujinsoft.utils.MioUtils.Types;
import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.error.PebbleException;
import com.mitchellbosecke.pebble.template.PebbleTemplate;

/*
 * This class contains generic get/search methods used across all three major servlets
 * (Games, Comics and Records)
 */
public class ServletUtils {

	/*
	 * For GET requests. Grab the standard template, and add the first page of items.
	 */
	public static String doStandardPageGeneric(int type, ServletContext application) 
			throws PebbleException, SQLException, IOException, NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
    	
		//Generics
		ArrayList items = new ArrayList();
		Constructor classConstructor = null;
		
    	Map<String, Object> context = new HashMap<>();
		Connection connection = null;
		String tableName = "";
		String contextTable = "";
		
    	PebbleEngine engine = new PebbleEngine.Builder().build();
		PebbleTemplate compiledTemplate = null;
		ResultSet result = null;
		
		
		String dataDir = application.getInitParameter("dataDirectory");

	    // create a database connection
	    connection = DriverManager.getConnection("jdbc:sqlite:"+dataDir+"/mioDatabase.sqlite");
	    Statement statement = connection.createStatement();
	    statement.setQueryTimeout(30);  // set timeout to 30 sec.

	  //Getting base template and other type dependant data
  		switch (type) {
  		case Types.GAME:
  				compiledTemplate = engine.getTemplate(application.getRealPath("/WEB-INF/templates/game.html"));
  				tableName = "Games";
  				contextTable = "games";
  				classConstructor = Game.class.getConstructor(ResultSet.class);
  			break;
  		case Types.MANGA:
  				compiledTemplate = engine.getTemplate(application.getRealPath("/WEB-INF/templates/manga.html"));
				tableName = "Manga";
				contextTable = "mangas";			
				classConstructor = Manga.class.getConstructor(ResultSet.class);
  			break;
  			
  		case Types.RECORD:
  				compiledTemplate = engine.getTemplate(application.getRealPath("/WEB-INF/templates/records.html"));
				tableName = "Records";
				contextTable = "records";
				classConstructor = Record.class.getConstructor(ResultSet.class);
  			break;
  		}
  		
  		result = statement.executeQuery("select * from "+tableName+" WHERE id NOT LIKE '%nint%' AND id NOT LIKE '%them%' ORDER BY normalizedName ASC LIMIT 15");
  		
  		while(result.next()) 
	    	items.add(classConstructor.newInstance(result));
  		
	    	result = statement.executeQuery("select COUNT(id) from "+tableName+" WHERE id NOT LIKE '%nint%' AND id NOT LIKE '%them%'");
	    
		context.put(contextTable, items);
		context.put("totalitems", result.getInt(1));
		
		
		//Output to client
		Writer writer = new StringWriter();
		compiledTemplate.evaluate(writer, context);
		String output = writer.toString();
		
		return output;
    	
	}
	
	/*
	 * For POST requests. Perform a request based on the parameters given (search and/or pages) and return the matching subtemplate.
	 */
	public static String doSearchGeneric(int type, ServletContext application, HttpServletRequest request ) 
			throws SQLException, PebbleException, IOException, NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
    	
    	ArrayList items = new ArrayList();
    	Map<String, Object> context = new HashMap<>();
		Connection connection = null;
		String tableName = "";
		String contextTable = "";
		Constructor classConstructor = null;
		
    		PebbleEngine engine = new PebbleEngine.Builder().build();
		PebbleTemplate compiledTemplate = null;
		
		//We only use the part of the template containing the item cards here
		switch (type) {
  		case Types.GAME:
  				compiledTemplate = engine.getTemplate(application.getRealPath("/WEB-INF/templates/gameDetail.html"));
  				tableName = "Games";
  				contextTable = "games";
  				classConstructor = Game.class.getConstructor(ResultSet.class);
  			break;
  		case Types.MANGA:
  				compiledTemplate = engine.getTemplate(application.getRealPath("/WEB-INF/templates/mangaDetail.html"));
				tableName = "Manga";
				contextTable = "mangas";			
				classConstructor = Manga.class.getConstructor(ResultSet.class);
  			break;
  			
  		case Types.RECORD:
  				compiledTemplate = engine.getTemplate(application.getRealPath("/WEB-INF/templates/recordsDetail.html"));
				tableName = "Records";
				contextTable = "records";
				classConstructor = Record.class.getConstructor(ResultSet.class);
  			break;
  		}

		
		String dataDir = application.getInitParameter("dataDirectory");
		
	    // create a database connection
	    connection = DriverManager.getConnection("jdbc:sqlite:"+dataDir+"/mioDatabase.sqlite");
	    boolean isNameSearch = request.getParameterMap().containsKey("name") && !request.getParameter("name").isEmpty();
	    boolean isCreatorSearch = request.getParameterMap().containsKey("creator") && !request.getParameter("creator").isEmpty();
    	
	    String queryBase = "FROM "+tableName+" WHERE ";
	    queryBase += (isNameSearch || isCreatorSearch) ? "name LIKE ? AND creator LIKE ? AND ": "";
	    queryBase += "id NOT LIKE '%nint%' AND id NOT LIKE '%them%'";
		
	    String query = "SELECT * " + queryBase + " ORDER BY normalizedName ASC LIMIT 15 OFFSET ?";
	    String queryCount = "SELECT COUNT(id) " + queryBase;
		
		PreparedStatement ret = connection.prepareStatement(query);
		
		
		int page = 1;
		//Those filters go in the LIKE parts of the query
		String name = "%";
		String creator = "%";
		if (request.getParameterMap().containsKey("page") && !request.getParameter("page").isEmpty())
			page = Integer.parseInt(request.getParameter("page"));
		
		if (isNameSearch)
			name = "%"+request.getParameter("name")+"%";
		
		if (isCreatorSearch)
			creator = "%"+request.getParameter("creator")+"%";
		
		if (isNameSearch || isCreatorSearch) {
			ret.setString(1, name);
			ret.setString(2, creator);
			ret.setInt(3, page*15-15);
		} else 
			ret.setInt(1, page*15-15);
    	
		ResultSet result = ret.executeQuery();
	    
	    while(result.next()) 
	    	items.add(classConstructor.newInstance(result));

	    PreparedStatement ret2 = connection.prepareStatement(queryCount);
	    
		if (isNameSearch || isCreatorSearch) {
		    ret2.setString(1, name);
		    ret2.setString(2, creator);
		}
	    result = ret2.executeQuery();
		
		context.put(contextTable, items);
		context.put("totalitems", result.getInt(1));

		//Output to client
		Writer writer = new StringWriter();
		compiledTemplate.evaluate(writer, context);
		String output = writer.toString();

		return output;
    }
	
	/*
	 * Collection versions - Takes a few shortcuts compared to the regular variants.
	 */
	public static String doStandardPageCollection(Collection c, ServletContext application) 
			throws PebbleException, SQLException, IOException {
    	
		ArrayList<Game> items = new ArrayList<Game>();
		
    	Map<String, Object> context = new HashMap<>();
		Connection connection = null;
		
    	PebbleEngine engine = new PebbleEngine.Builder().build();
		PebbleTemplate compiledTemplate = null;
		ResultSet result = null;
		
		String dataDir = application.getInitParameter("dataDirectory");

	    // create a database connection
	    connection = DriverManager.getConnection("jdbc:sqlite:"+dataDir+"/mioDatabase.sqlite");
	    Statement statement = connection.createStatement();
	    statement.setQueryTimeout(30);  // set timeout to 30 sec.

	    compiledTemplate = engine.getTemplate(application.getRealPath("/WEB-INF/templates/collection.html"));	    
  		
	    String query = "select * from Games WHERE id IN "+c.getMioSQL()+" ORDER BY normalizedName ASC LIMIT 15";
	    
  		result = statement.executeQuery(query);
  		
  		while(result.next()) 
	    	items.add(new Game(result));
  		
  		context.put("games", items);
		context.put("totalitems", c.mios.length);
		context.put("collection", c);
		
		//Output to client
		Writer writer = new StringWriter();
		compiledTemplate.evaluate(writer, context);
		String output = writer.toString();
		
		return output;
    	
	}
	
	/*
	 * POST requests in collections.
	 */
	public static String doSearchCollection(Collection c, ServletContext application, HttpServletRequest request ) 
			throws SQLException, PebbleException, IOException {
    	
    	ArrayList<Game> items = new ArrayList<Game>();
    	Map<String, Object> context = new HashMap<>();
		Connection connection = null;
		String tableName = "";
		
    	PebbleEngine engine = new PebbleEngine.Builder().build();
		PebbleTemplate compiledTemplate = null;
		
		compiledTemplate = engine.getTemplate(application.getRealPath("/WEB-INF/templates/gameDetail.html"));
		tableName = "Games";

		String dataDir = application.getInitParameter("dataDirectory");
		
	    // create a database connection
	    connection = DriverManager.getConnection("jdbc:sqlite:"+dataDir+"/mioDatabase.sqlite");
		
	    boolean isNameSearch = request.getParameterMap().containsKey("name") && !request.getParameter("name").isEmpty();
	    boolean isCreatorSearch = request.getParameterMap().containsKey("creator") && !request.getParameter("creator").isEmpty();
		
	    String queryBase = "FROM "+tableName+" WHERE id IN "+c.getMioSQL();
	    queryBase += (isNameSearch || isCreatorSearch) ? " AND name LIKE ? AND creator LIKE ?" : "";
	    queryBase += " AND id NOT LIKE '%nint%' AND id NOT LIKE '%them%'";
		
	    String query = "SELECT * " + queryBase + " ORDER BY normalizedName ASC LIMIT 15 OFFSET ?";
	    String queryCount = "SELECT COUNT(id) " + queryBase;
	    
		PreparedStatement ret = connection.prepareStatement(query);
		
		int page = 1;
		//Those filters go in the LIKE parts of the query
		String name = "%";
		String creator = "%";
		if (request.getParameterMap().containsKey("page") && !request.getParameter("page").isEmpty())
			page = Integer.parseInt(request.getParameter("page"));
		
		if (isNameSearch)
			name = "%"+request.getParameter("name")+"%";
		
		if (isCreatorSearch)
			creator = "%"+request.getParameter("creator")+"%";
		
		if (isNameSearch || isCreatorSearch) {
			ret.setString(1, name);
			ret.setString(2, creator);
			ret.setInt(3, page*15-15);
		} else 
			ret.setInt(1, page*15-15);
		
		ResultSet result = ret.executeQuery();
	    
	    while(result.next()) 
	    	items.add(new Game(result));

	    PreparedStatement ret2 = connection.prepareStatement(queryCount);
	    
	    if (isNameSearch || isCreatorSearch) {
		    ret2.setString(1, name);
		    ret2.setString(2, creator);
		}
	    result = ret2.executeQuery();
		
		context.put("games", items);
		context.put("totalitems", result.getInt(1));
		
		//Output to client
		Writer writer = new StringWriter();
		compiledTemplate.evaluate(writer, context);
		String output = writer.toString();
		
		return output;
    }
	
}
