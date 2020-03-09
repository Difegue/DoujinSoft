package com.difegue.doujinsoft.utils;

import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import com.difegue.doujinsoft.templates.*;

/**
 * TemplateBuilder extension for Collections.
 */
public class TemplateBuilderCollection extends TemplateBuilder {

    public TemplateBuilderCollection(ServletContext application, HttpServletRequest request) throws SQLException {
        super(application, request);
    }

    /*
	 * Collection version - Uses the collection's specific type and a custom query.
	 */
	public String doStandardPageCollection(Collection c) throws Exception {
		
		initializeTemplate(c.getType(), false);

		Statement statement = connection.createStatement();
	    compiledTemplate = engine.getTemplate(application.getRealPath("/WEB-INF/templates/collection.html"));

	    if (c.getMioSQL().equals(")")) {
			context.put("totalitems", 0);
			context.put("collection", c);
			return writeToTemplate();
		}

  		ResultSet result = statement.executeQuery("select * from "+tableName+" WHERE hash IN "+c.getMioSQL()+" ORDER BY normalizedName ASC LIMIT 15");
  		while(result.next()) 
		  	items.add(classConstructor.newInstance(result));
  		
  		context.put("items", items);
		context.put("totalitems", c.mios.length);
		context.put("collection", c);
		
		result.close();
		statement.close();
		connection.close();
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
		
		result.close();
		ret.close();

		context.put("items", items);
		context.put("totalitems", retCount.executeQuery().getInt(1));
		
		retCount.close();
		connection.close();
		return writeToTemplate();
	}

}