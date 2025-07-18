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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import com.google.gson.Gson;

import com.difegue.doujinsoft.templates.*;
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

	protected ArrayList<Object> items = new ArrayList<Object>();
	protected Constructor classConstructor;

	protected Map<String, Object> context = new HashMap<>();
	protected Connection connection;
	protected ServletContext application;
	protected HttpServletRequest request;

	protected String tableName, dataDir;

	/*
	 * isContentNameSearch: Search by title name of the game, comic, or record
	 * isCreatorNameSearch: Search by author's name of the game, comic, or record
	 * isContentCreatorSearch: Search by cartridge ID or creator ID of the game,
	 * comic, or record
	 * isSortedBy: Sort content flag
	 */
	protected boolean isContentNameSearch, isCreatorNameSearch, isContentCreatorSearch, isSortedBy;

	protected PebbleEngine engine = new PebbleEngine.Builder().build();
	protected PebbleTemplate compiledTemplate;

	public TemplateBuilder(ServletContext application, HttpServletRequest request) throws SQLException {

		this.application = application;
		this.request = request;
		dataDir = application.getInitParameter("dataDirectory");

		// create a database connection
		connection = DriverManager.getConnection("jdbc:sqlite:" + dataDir + "/mioDatabase.sqlite");
	}

	protected void initializeTemplate(int type, boolean isDetail) throws NoSuchMethodException, PebbleException {

		String templatePath = "/WEB-INF/templates/";
		// Getting base template and other type dependant data
		// If in search mode, we only use the part of the template containing the item
		// cards
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
			case Types.SURVEY:
				templatePath += "surveys";
				tableName = "Surveys";
				classConstructor = Survey.class.getConstructor(ResultSet.class);
		}

		if (isDetail) {
			templatePath += "Detail";
		}

		isContentNameSearch = request.getParameterMap().containsKey("name") && !request.getParameter("name").isEmpty();
		isCreatorNameSearch = request.getParameterMap().containsKey("creator")
				&& !request.getParameter("creator").isEmpty();
		isContentCreatorSearch = (request.getParameterMap().containsKey("cartridge_id")
				&& !request.getParameter("cartridge_id").isEmpty())
				|| (request.getParameterMap().containsKey("creator_id")
						&& !request.getParameter("creator_id").isEmpty());
		isSortedBy = request.getParameterMap().containsKey("sort_by") && !request.getParameter("sort_by").isEmpty();

		compiledTemplate = engine.getTemplate(application.getRealPath(templatePath + ".html"));
	}

	protected String writeToTemplate() throws PebbleException, IOException {

		Writer writer = new StringWriter();
		compiledTemplate.evaluate(writer, context);
		String output = writer.toString();

		return output;
	}

	/*
	 * For GET requests. Grab the standard template, and add the first page of
	 * items.
	 */
	public String doStandardPageGeneric(int type) throws Exception {

		initializeTemplate(type, false);
		String queryBase = "FROM " + tableName + " WHERE ";
		queryBase += (isContentNameSearch || isCreatorNameSearch) ? "name LIKE ? AND creator LIKE ? AND " : "";
		queryBase += "id NOT LIKE '%them%'";

		// Specific hash request
		if (request.getParameterMap().containsKey("id")) {

			PreparedStatement statement = connection
					.prepareStatement("select * from " + tableName + " WHERE hash == ?");
			statement.setString(1, request.getParameter("id"));

			// disable search by setting totalitems to -1
			context.put("totalitems", -1);
			context.put("singleitem", true);

			ResultSet result = statement.executeQuery();

			while (result.next())
				items.add(classConstructor.newInstance(result));

			result.close();
			context.put("items", items);
			statement.close();
		} else if (isContentCreatorSearch && !isContentNameSearch && !isCreatorNameSearch) {
			performCreatorSearchQuery(queryBase, "normalizedName ASC");
			GetCreatorInfo();
		} else {
			performSearchQuery(queryBase, "normalizedName ASC");
		}

		// JSON hijack if specified in the parameters
		if (request.getParameterMap().containsKey("format") && request.getParameter("format").equals("json")) {
			Gson gson = new Gson();
			return gson.toJson(context);
		}

		connection.close();
		// Output to client
		return writeToTemplate();
	}

	/*
	 * For POST requests. Perform a request based on the parameters given (search
	 * and/or pages) and return the matching subtemplate.
	 */
	public String doSearchGeneric(int type) throws Exception {

		initializeTemplate(type, true);

		// Build both data and count queries
		String queryBase = "FROM " + tableName + " WHERE ";
		queryBase += (isContentNameSearch || isCreatorNameSearch) ? "name LIKE ? AND creator LIKE ? AND " : "";
		queryBase += "id NOT LIKE '%them%'";

		if (isContentCreatorSearch && !isContentNameSearch && !isCreatorNameSearch) {
			performCreatorSearchQuery(queryBase, "normalizedName ASC");
			GetCreatorInfo();
		} else
			performSearchQuery(queryBase, "normalizedName ASC");

		// JSON hijack if specified in the parameters
		if (request.getParameterMap().containsKey("format") && request.getParameter("format").equals("json")) {
			Gson gson = new Gson();
			return gson.toJson(context);
		}

		connection.close();
		return writeToTemplate();
	}

	/*
	 * Default query or search by creator name and/or content name
	 */
	protected void performSearchQuery(String queryBase, String defaultOrderBy) throws Exception {

		String orderBy = defaultOrderBy;

		// Change order if the parameter was given
		if (isSortedBy && request.getParameter("sort_by").equals("date")) {
			orderBy = "timeStamp DESC";
		}

		if (isSortedBy && request.getParameter("sort_by").equals("name")) {
			orderBy = "normalizedName ASC";
		}

		String query = "SELECT * " + queryBase + " ORDER BY " + orderBy + " LIMIT 15 OFFSET ?";
		String queryCount = "SELECT COUNT(id) " + queryBase;

		PreparedStatement ret = connection.prepareStatement(query);
		PreparedStatement retCount = connection.prepareStatement(queryCount);

		// Those filters go in the LIKE parts of the query
		String name = isContentNameSearch ? request.getParameter("name") + "%" : "%";
		String creator = isCreatorNameSearch ? request.getParameter("creator") + "%" : "%";

		// Remove last char for context display
		context.put("nameSearch", name.substring(0, name.length() - 1));
		context.put("creatorSearch", creator.substring(0, creator.length() - 1));

		// Remove creator and cartridge IDs from context
		context.remove("creatorIdSearch");
		context.remove("cartridgeIdSearch");

		int page = 1;
		if (request.getParameterMap().containsKey("page") && !request.getParameter("page").isEmpty())
			page = Integer.parseInt(request.getParameter("page"));

		if (isContentNameSearch || isCreatorNameSearch) {
			ret.setString(1, name);
			ret.setString(2, creator);
			retCount.setString(1, name);
			retCount.setString(2, creator);
			ret.setInt(3, page * 15 - 15);
		} else
			ret.setInt(1, page * 15 - 15);

		ResultSet result = ret.executeQuery();

		while (result.next())
			items.add(classConstructor.newInstance(result));

		result.close();
		ret.close();

		context.put("items", items);
		context.put("totalitems", retCount.executeQuery().getInt(1));
		retCount.close();
	}

	/*
	 * Query search by creator ID or cartridge ID
	 */
	protected void performCreatorSearchQuery(String queryBase, String defaultOrderBy) throws Exception {
		// Get creatorId and cartridgeId for search query
		String creatorId = request.getParameter("creator_id");
		String cartridgeId = request.getParameter("cartridge_id");
		boolean isLegitCart = !cartridgeId.equals("00000000000000000000000000000000");

		// Add creator/cartID checks to query
		queryBase += " AND creatorID = ? ";
		queryBase += isLegitCart ? " OR cartridgeID = ? " : "";

		// Default orderBy
		String orderBy = defaultOrderBy;

		// Change order if the parameter was given
		if (isSortedBy && request.getParameter("sort_by").equals("date")) {
			orderBy = "timeStamp DESC";
		}

		if (isSortedBy && request.getParameter("sort_by").equals("name")) {
			orderBy = "normalizedName ASC";
		}

		String query = "SELECT * " + queryBase + " ORDER BY " + orderBy + " LIMIT 15 OFFSET ?";
		String queryCount = "SELECT COUNT(id) " + queryBase;

		PreparedStatement ret = connection.prepareStatement(query);
		PreparedStatement retCount = connection.prepareStatement(queryCount);

		// Add creator and cartridge IDs to context
		context.put("creatorIdSearch", creatorId);
		context.put("cartridgeIdSearch", cartridgeId);

		// remove name and creator search fields from context
		context.remove("nameSearch");
		context.remove("creatorSearch");

		int page = 1;
		if (request.getParameterMap().containsKey("page") && !request.getParameter("page").isEmpty())
			page = Integer.parseInt(request.getParameter("page"));

		// Set values for prepared statement
		ret.setString(1, creatorId);
		retCount.setString(1, creatorId);

		if (isLegitCart) {
			ret.setString(2, cartridgeId);
			retCount.setString(2, cartridgeId);
			ret.setInt(3, page * 15 - 15);
		} else
			ret.setInt(2, page * 15 - 15);

		ResultSet result = ret.executeQuery();

		while (result.next())
			items.add(classConstructor.newInstance(result));

		result.close();
		ret.close();

		context.put("items", items);
		context.put("totalitems", retCount.executeQuery().getInt(1));

		retCount.close();
	}

	protected void GetCreatorInfo() throws Exception {
		/*
		 * TODO:
		 * This method is fully functional but is commented out so that it
		 * will not go into the current release to prevent unnecessary DB calls
		 */

		// String creatorId = request.getParameter("creator_id");
		// String cartridgeId = request.getParameter("cartridge_id");

		// CreatorDetails creatorDetails = new CreatorDetails(connection, creatorId,
		// cartridgeId);

		// context.put("displaycreatordetails", true);
		// context.put("creatordetails", creatorDetails);
	}
}
