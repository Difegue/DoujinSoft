package com.difegue.doujinsoft.utils;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Constructor;
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
public class TemplateBuilder {

	private ArrayList items = new ArrayList();
	private Constructor classConstructor;

	private Map<String, Object> context = new HashMap<>();
	private Connection connection;
	private Statement statement;
	private ServletContext application;
	private HttpServletRequest request;

	private String tableName, dataDir;
	private boolean isNameSearch, isCreatorSearch;
	
	private PebbleEngine engine = new PebbleEngine.Builder().build();
	private PebbleTemplate compiledTemplate;

	public TemplateBuilder(ServletContext application, HttpServletRequest request) throws SQLException {

		this.application = application;
		this.request = request;
		dataDir = application.getInitParameter("dataDirectory");

	    // create a database connection
	    connection = DriverManager.getConnection("jdbc:sqlite:"+dataDir+"/mioDatabase.sqlite");
	    statement = connection.createStatement();
		statement.setQueryTimeout(30);  // set timeout to 30 sec.
		
	}

	private void initializeTemplate(int type, boolean isDetail) throws NoSuchMethodException, PebbleException {

		String templatePath = "/WEB-INF/templates/";
		//Getting base template and other type dependant data
		//If in search mode, we only use the part of the template containing the item cards 
		switch (type) {
			case Types.GAME:
				templatePath += "game";
				tableName = "Games";
				classConstructor = Game.class.getConstructor(ResultSet.class);
				break;
			case Types.MANGA:
				templatePath += "manga";
				tableName = "Manga";
				classConstructor = Manga.class.getConstructor(ResultSet.class);
				break;
			case Types.RECORD:
				templatePath += "records";
				tableName = "Records";
				classConstructor = Record.class.getConstructor(ResultSet.class);
				break;
			}

		if (isDetail) {
			templatePath += "Detail";	
			isNameSearch    = request.getParameterMap().containsKey("name") && !request.getParameter("name").isEmpty();
			isCreatorSearch = request.getParameterMap().containsKey("creator") && !request.getParameter("creator").isEmpty();
		}
			
		compiledTemplate = engine.getTemplate(application.getRealPath(templatePath+".html"));
	}

	private String writeToTemplate() throws PebbleException, IOException {

		Writer writer = new StringWriter();
		compiledTemplate.evaluate(writer, context);
		String output = writer.toString();
		
		return output;
	}

	/*
	 * For GET requests. Grab the standard template, and add the first page of items.
	 */
	public String doStandardPageGeneric(int type) throws Exception {

		initializeTemplate(type, false);
  		ResultSet result = statement.executeQuery("select * from "+tableName+" WHERE id NOT LIKE '%nint%' AND id NOT LIKE '%them%' ORDER BY normalizedName ASC LIMIT 15");
  		
  		while(result.next()) 
	    	items.add(classConstructor.newInstance(result));
  		
	    result = statement.executeQuery("select COUNT(id) from "+tableName+" WHERE id NOT LIKE '%nint%' AND id NOT LIKE '%them%'");
		context.put("items", items);
		context.put("totalitems", result.getInt(1));
		
		//Output to client
		return writeToTemplate();
	}
	
	/*
	 * Collection version - Uses the collection's specific type and a custom query.
	 */
	public String doStandardPageCollection(Collection c) throws Exception {
		
		initializeTemplate(c.getType(), false);
	    compiledTemplate = engine.getTemplate(application.getRealPath("/WEB-INF/templates/collection.html"));	    
  		ResultSet result = statement.executeQuery("select * from "+tableName+" WHERE hash IN "+c.getMioSQL()+" ORDER BY normalizedName ASC LIMIT 15");
  		
  		while(result.next()) 
		  	items.add(classConstructor.newInstance(result));
  		
  		context.put("items", items);
		context.put("totalitems", c.mios.length);
		context.put("collection", c);
		
		return writeToTemplate();
	}

	/*
	 * For POST requests. Perform a request based on the parameters given (search and/or pages) and return the matching subtemplate.
	 */
	public String doSearchGeneric(int type) throws Exception {

		initializeTemplate(type, true);
		
		// Build both data and count queries
	    String queryBase = "FROM "+tableName+" WHERE ";
	    queryBase += (isNameSearch || isCreatorSearch) ? "name LIKE ? AND creator LIKE ? AND ": "";
	    queryBase += "id NOT LIKE '%nint%' AND id NOT LIKE '%them%'";
		
	    String query = "SELECT * " + queryBase + " ORDER BY normalizedName ASC LIMIT 15 OFFSET ?";
	    String queryCount = "SELECT COUNT(id) " + queryBase;
		
		PreparedStatement ret = connection.prepareStatement(query);	
		PreparedStatement retCount = connection.prepareStatement(queryCount);

		//Those filters go in the LIKE parts of the query
		String name    = isNameSearch ? "%"+request.getParameter("name")+"%" : "%";
		String creator = isCreatorSearch ? "%"+request.getParameter("creator")+"%" : "%";

		int page = 1;
		if (request.getParameterMap().containsKey("page") && !request.getParameter("page").isEmpty())
			page = Integer.parseInt(request.getParameter("page"));
		
		if (isNameSearch || isCreatorSearch) {
			ret.setString(1, name);
			ret.setString(2, creator);
			retCount.setString(1, name);
		    retCount.setString(2, creator);
			ret.setInt(3, page*15-15);
		} else 
			ret.setInt(1, page*15-15);
    	
		ResultSet result = ret.executeQuery();
	    
	    while(result.next()) 
	    	items.add(classConstructor.newInstance(result));

		context.put("items", items);
		context.put("totalitems", retCount.executeQuery().getInt(1));

		return writeToTemplate();
    }
	
	/*
	 * POST requests in collections.
	 */
	public String doSearchCollection(Collection c) throws Exception {
		
		initializeTemplate(c.getType(), true);
		
	    String queryBase = "FROM "+tableName+" WHERE hash IN "+c.getMioSQL();
	    queryBase += (isNameSearch || isCreatorSearch) ? " AND name LIKE ? AND creator LIKE ?" : "";
		
	    String query = "SELECT * " + queryBase + " ORDER BY normalizedName ASC LIMIT 15 OFFSET ?";
	    String queryCount = "SELECT COUNT(hash) " + queryBase;
	    
		PreparedStatement ret = connection.prepareStatement(query);
		PreparedStatement retCount = connection.prepareStatement(queryCount);

		//Those filters go in the LIKE parts of the query
		String name    = isNameSearch ? "%"+request.getParameter("name")+"%" : "%";
		String creator = isCreatorSearch ? "%"+request.getParameter("creator")+"%" : "%";

		int page = 1;
		if (request.getParameterMap().containsKey("page") && !request.getParameter("page").isEmpty())
			page = Integer.parseInt(request.getParameter("page"));
		
		if (isNameSearch || isCreatorSearch) {
			ret.setString(1, name);
			ret.setString(2, creator);
			retCount.setString(1, name);
		    retCount.setString(2, creator);
			ret.setInt(3, page*15-15);
		} else 
			ret.setInt(1, page*15-15);
		
		ResultSet result = ret.executeQuery();
		
	    while(result.next()) 
			items.add(classConstructor.newInstance(result));
		
		context.put("items", items);
		context.put("totalitems", retCount.executeQuery().getInt(1));
		
		return writeToTemplate();
    }
	
}
