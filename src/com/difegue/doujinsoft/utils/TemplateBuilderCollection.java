package com.difegue.doujinsoft.utils;

import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import com.google.gson.Gson;

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

		// Unlike the regular pages, ordering by timestamp is the default for
		// collections
		ResultSet result = statement.executeQuery(
				"select * from " + tableName + " WHERE hash IN " + c.getMioSQL() + " ORDER BY timeStamp DESC LIMIT 15");
		while (result.next())
			items.add(classConstructor.newInstance(result));

		context.put("items", items);
		context.put("totalitems", c.mios.length);
		context.put("collection", c);

		result.close();
		statement.close();
		connection.close();

		// JSON hijack if specified in the parameters
		if (request.getParameterMap().containsKey("format") && request.getParameter("format").equals("json")) {
			Gson gson = new Gson();
			return gson.toJson(context);
		}

		return writeToTemplate();
	}

	/*
	 * POST requests in collections.
	 */
	public String doSearchCollection(Collection c) throws Exception {

		initializeTemplate(c.getType(), true);

		String queryBase = "FROM " + tableName + " WHERE hash IN " + c.getMioSQL();
		queryBase += (isContentNameSearch || isCreatorNameSearch) ? " AND name LIKE ? AND creator LIKE ?" : "";

		if (isContentCreatorSearch && !isContentNameSearch && !isCreatorNameSearch) {
			performCreatorSearchQuery(queryBase, "timeStamp DESC");
			GetCreatorInfo();
		} else {
			// Unlike the regular pages, ordering by timestamp is the default for
			// collections
			performSearchQuery(queryBase, "timeStamp DESC");
		}

		// JSON hijack if specified in the parameters
		if (request.getParameterMap().containsKey("format") && request.getParameter("format").equals("json")) {
			Gson gson = new Gson();
			return gson.toJson(context);
		}

		connection.close();
		return writeToTemplate();
	}

}