package com.difegue.doujinsoft;

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

import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.error.PebbleException;
import com.mitchellbosecke.pebble.template.PebbleTemplate;


/**
 * Servlet implementation class GameListing
 */
@WebServlet("/games")
public class GameServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
    private Logger ServletLog;
    
    /*
     * Class used for binding with the template.
     */
    public class Game { 	
    	
		public Game(ResultSet result) throws SQLException{
		
			//Compute timestamp 
	    	timestamp = MioUtils.getTimeString(result.getInt("timeStamp"));
		
	    	String desc = result.getString("description");
	    	colorLogo = result.getString("colorLogo");
	    	
	    	//Special case to make black logos readable on the user interface
	    	if (colorLogo.equals("grey darken-4"))
	    		colorLogo = "grey";
	    	
	    	name = result.getString("name");
	    	mioID = result.getString("id");
			brand = result.getString("brand");
			creator = result.getString("creator");
			if (desc.length() > 18) {
				mioDesc1 = desc.substring(0,18);
				mioDesc2 = desc.substring(18);
			}
			else {
				mioDesc1 = desc;
				mioDesc2 = "_";
			}
			preview = result.getString("previewPic");
			colorCart = result.getString("color");
			logo = result.getInt("logo");
		
		}
		
		public String name, timestamp, mioID, brand, creator, mioDesc1, mioDesc2, preview, colorLogo, colorCart;
    	public int logo;
    	
    }
    
    /**
     * @see HttpServlet#HttpServlet()
     */
    public GameServlet() {
        super();
        
        
        
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		ServletLog = Logger.getLogger("GameServlet");
    	ServletLog.addHandler(new StreamHandler(System.out, new SimpleFormatter()));
    	int page = 1;
    	
    	if (request.getParameterMap().containsKey("page"))
    		page = Integer.parseInt(request.getParameter("page"));
    	
		response.setContentType("text/html; charset=UTF-8");
		ServletContext application = getServletConfig().getServletContext();	
		String dataDir = application.getInitParameter("dataDirectory");
		
		
		PebbleEngine engine = new PebbleEngine.Builder().build();
		PebbleTemplate compiledTemplate;
		try {
			//Getting base template
			compiledTemplate = engine.getTemplate(application.getRealPath("/WEB-INF/templates/game.html"));
			Map<String, Object> context = new HashMap<>();
			
			//Fill 'er up
			ArrayList<Game> games = new ArrayList<Game>();
			Connection connection = null;
			
		    // create a database connection
		    connection = DriverManager.getConnection("jdbc:sqlite:"+dataDir+"/mioDatabase.sqlite");
		    Statement statement = connection.createStatement();
		    statement.setQueryTimeout(30);  // set timeout to 30 sec.
		    
		    //todo: limits on this select w. a parameter of get 
		    ResultSet result = statement.executeQuery("select * from Games LIMIT 9 OFFSET "+(page*9-9));
		    
		    while(result.next()) {
		    	games.add(new Game(result));
		    }
			
			context.put("games", games);
			
			//Output to client
			Writer writer = new StringWriter();
			compiledTemplate.evaluate(writer, context);
			String output = writer.toString();
			response.getWriter().append(output);
			
		} catch (PebbleException e ) {
			ServletLog.log(Level.SEVERE, e.getPebbleMessage());
		}
		  catch(SQLException e){
			ServletLog.log(Level.SEVERE, e.getSQLState());
		}

	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

}
