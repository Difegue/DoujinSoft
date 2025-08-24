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
		compiledTemplate = engine.getTemplate(application.getRealPath("/WEB-INF/templates/collection.html"));
		context.put("collection", c);

		if (c.getMioSQL().equals(")")) {
			context.put("totalitems", 0);
			return writeToTemplate();
		}

		// Unlike the regular pages, ordering by timestamp is the default for collections
		if (isContentCreatorSearch && !isContentNameSearch && !isCreatorNameSearch) {
			performCreatorSearchQuery("timeStamp DESC", "hash IN " + c.getMioSQL(), true);
			GetCreatorInfo();
		} else {
			performSearchQuery("timeStamp DESC", "hash IN " + c.getMioSQL(), true); 
		}

		// JSON hijack if specified in the parameters
		if (request.getParameterMap().containsKey("format") && request.getParameter("format").equals("json")) {
			Gson gson = new Gson();
			return gson.toJson(context);
		}

		connection.close();
		return writeToTemplate();
	}

	/*
	 * POST requests in collections.
	 */
	public String doSearchCollection(Collection c) throws Exception {

		initializeTemplate(c.getType(), true);

		if (isContentCreatorSearch && !isContentNameSearch && !isCreatorNameSearch) {
			performCreatorSearchQuery("timeStamp DESC", "hash IN " + c.getMioSQL(), true);
			GetCreatorInfo();
		} else {
			// Unlike the regular pages, ordering by timestamp is the default for
			// collections
			performSearchQuery("timeStamp DESC", "hash IN " + c.getMioSQL(), true);
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