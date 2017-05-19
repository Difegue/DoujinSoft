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

import com.difegue.doujinsoft.templates.Record;
import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.error.PebbleException;
import com.mitchellbosecke.pebble.template.PebbleTemplate;


/**
 * Servlet implementation class for Records
 */
@WebServlet("/records")
public class MusicServlet extends HttpServlet {
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
		
		response.setContentType("text/html; charset=UTF-8");
		ServletContext application = getServletConfig().getServletContext();	
		String output = "Who are you running from?";
		
		try {
			
			if (!request.getParameterMap().isEmpty())
				output = doSearch(application, request);

			response.getWriter().append(output);
			
		} catch (SQLException | PebbleException e) {
			ServletLog.log(Level.SEVERE, e.getMessage());
		}
		
	}

    
    /**
     * @see HttpServlet#HttpServlet()
     */
    public MusicServlet() {
        super(); 
        ServletLog = Logger.getLogger("MusicServlet");
        ServletLog.addHandler(new StreamHandler(System.out, new SimpleFormatter()));     
    }
   
    
    //Generates the regular landing page for games.
    private String doStandardPage(ServletContext application) throws PebbleException, SQLException, IOException {
    	
    	ArrayList<Record> items = new ArrayList<Record>();
    	Map<String, Object> context = new HashMap<>();
		Connection connection = null;
		
    	PebbleEngine engine = new PebbleEngine.Builder().build();
		PebbleTemplate compiledTemplate;

		//Getting base template
		compiledTemplate = engine.getTemplate(application.getRealPath("/WEB-INF/templates/records.html"));
		String dataDir = application.getInitParameter("dataDirectory");

	    // create a database connection
	    connection = DriverManager.getConnection("jdbc:sqlite:"+dataDir+"/mioDatabase.sqlite");
	    Statement statement = connection.createStatement();
	    statement.setQueryTimeout(30);  // set timeout to 30 sec.
	    
	    ResultSet result = statement.executeQuery("select * from Records LIMIT 9");
	    
	    while(result.next()) 
	    	items.add(new Record(result));
		
	    result = statement.executeQuery("select COUNT(id) from Records");
	    
		context.put("records", items);
		context.put("totalitems", result.getInt(1));
		
		
		//Output to client
		Writer writer = new StringWriter();
		compiledTemplate.evaluate(writer, context);
		String output = writer.toString();
		
		return output;
    	
    }
    
    //Generates a smaller HTML for searches/pages.
    private String doSearch(ServletContext application, HttpServletRequest request ) throws SQLException, PebbleException, IOException {
    	
    	ArrayList<Record> items = new ArrayList<Record>();
    	Map<String, Object> context = new HashMap<>();
		Connection connection = null;
		
    	PebbleEngine engine = new PebbleEngine.Builder().build();
		PebbleTemplate compiledTemplate;

		//We only use the part of the template containing the game cards here
		compiledTemplate = engine.getTemplate(application.getRealPath("/WEB-INF/templates/recordsDetail.html"));	
		String dataDir = application.getInitParameter("dataDirectory");
		
	    // create a database connection
	    connection = DriverManager.getConnection("jdbc:sqlite:"+dataDir+"/mioDatabase.sqlite");
    	
	    String query = "SELECT * FROM Records WHERE name LIKE ? AND creator LIKE ? ORDER BY id ASC LIMIT 9 OFFSET ?";
	    String queryCount = "SELECT COUNT(id) FROM Records WHERE name LIKE ? AND creator LIKE ?";
		
		PreparedStatement ret = connection.prepareStatement(query);
		
		
		int page = 1;
		String name = "%";
		String creator = "%";
		if (request.getParameterMap().containsKey("page") && !request.getParameter("page").isEmpty())
			page = Integer.parseInt(request.getParameter("page"));
		
		if (request.getParameterMap().containsKey("name") && !request.getParameter("name").isEmpty())
			name = "%"+request.getParameter("name")+"%";
		
		if (request.getParameterMap().containsKey("creator"))
			creator = "%"+request.getParameter("creator")+"%";
		
		ret.setString(1, name);
		ret.setString(2, creator);
		ret.setInt(3, page*9-9);
		
    	
		ResultSet result = ret.executeQuery();
	    
	    while(result.next()) 
	    	items.add(new Record(result));

	    PreparedStatement ret2 = connection.prepareStatement(queryCount);
	    
	    ret2.setString(1, name);
		ret2.setString(2, creator);
	    result = ret2.executeQuery();
		
	    context.put("records", items);
		context.put("totalitems", result.getInt(1));
		
		//Output to client
		Writer writer = new StringWriter();
		compiledTemplate.evaluate(writer, context);
		String output = writer.toString();
		
		return output;
    }

}
