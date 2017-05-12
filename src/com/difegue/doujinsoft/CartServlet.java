package com.difegue.doujinsoft;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.apache.tomcat.jni.Time;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.error.PebbleException;
import com.mitchellbosecke.pebble.template.PebbleTemplate;
import com.xperia64.diyedit.saveutils.SaveHandler;


/**
 * Servlet implementation class for the Cart.
 */
@WebServlet("/cart")
@MultipartConfig
public class CartServlet extends HttpServlet {
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
		
		//post contains a gameSave
		if (!request.getParameterMap().isEmpty()) 
			injectMios(request, response);
		else
		{
			response.setContentType("text/html; charset=UTF-8");
			String output = "Who are you running from ?";
			response.getWriter().append(output);
		}
		
	}

    
    private void injectMios(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		
    	ServletContext application = getServletConfig().getServletContext();	
		String dataDir = application.getInitParameter("dataDirectory");
		
    	// gets MIME type of the file
		String mimeType = "application/octet-stream";
		// Set response
		response.setContentType(mimeType);
		
		//Get byte[] save from request parameters
	    Part filePart = request.getPart("save"); 
	    String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString(); // MSIE fix.
	    InputStream fileContent = filePart.getInputStream();
	    
	    //Get the cart data and deserialize it.
	    JsonArray games = new JsonParser().parse(request.getParameter("games")).getAsJsonArray();
	    JsonArray manga = new JsonParser().parse(request.getParameter("manga")).getAsJsonArray();
	    JsonArray records = new JsonParser().parse(request.getParameter("records")).getAsJsonArray();
		
	    //Drop the file in a temp file
	    File targetFile = File.createTempFile(fileName+Time.now(), "bin");

	    Files.copy(
	      fileContent, 
	      targetFile.toPath(), 
	      StandardCopyOption.REPLACE_EXISTING);
	    
	    //Call DIYEdit's SaveHandler on it, and it only does everything™
        SaveHandler sHand = new SaveHandler(targetFile.getAbsolutePath());
        
        //Go through our arrays and inject mios from the DB
        for( JsonElement o: games) {
        	
        	String id = o.getAsJsonObject().get("id").getAsString();
        	String mioPath = dataDir+"/mio/game/"+id+".mio";
        	
        	int emptySlot = getEmptySlot(sHand, 0);
        	if (emptySlot != -1)
        		sHand.setMio(mioPath, emptySlot);
        	
        }
        
		for( JsonElement o: records) {
		        	
        	String id = o.getAsJsonObject().get("id").getAsString();
        	String mioPath = dataDir+"/mio/record/"+id+".mio";
        	
        	int emptySlot = getEmptySlot(sHand, 1);
        	if (emptySlot != -1)
        		sHand.setMio(mioPath, emptySlot);
        	
        }

		for( JsonElement o: manga) {
			
			String id = o.getAsJsonObject().get("id").getAsString();
			String mioPath = dataDir+"/mio/manga/"+id+".mio";
			
			int emptySlot = getEmptySlot(sHand, 2);
			if (emptySlot != -1)
				sHand.setMio(mioPath, emptySlot);
			
		}
        
		sHand.saveChanges();
		
		
    	// forces download
		String headerKey = "Content-Disposition";
		String headerValue = String.format("attachment; filename=\"%s\"", targetFile.getName());
		response.setHeader(headerKey, headerValue);
		
		// obtains response's output stream
		OutputStream outStream = response.getOutputStream();
		
		byte[] buffer = new byte[4096];
		int bytesRead = -1;
		FileInputStream inStream = new FileInputStream(targetFile);
		
		while ((bytesRead = inStream.read(buffer)) != -1) {
			outStream.write(buffer, 0, bytesRead);
		}
		
		inStream.close();
		outStream.close();
    	targetFile.delete();
	}

    /*
     * Go through the save for a given mode (0: games, 1: records, 2: manga) and return the first empty slot.
     * Returns -1 if there are no slots available, -2 if the savefile is incorrect.
     */
	private int getEmptySlot(SaveHandler sHand, int mode) {
		
		ArrayList<byte[]> b = sHand.getMios(mode);
		
		if(b==null){
    		return -2;
    	}
		
    	//Returns the first null slot
		for(byte[] mio: b) {
			if (mio == null)
				return b.indexOf(mio);
		}
		
		return -1;
	}

	/**
     * @see HttpServlet#HttpServlet()
     */
    public CartServlet() {
        super(); 
        ServletLog = Logger.getLogger("CartServlet");
        ServletLog.addHandler(new StreamHandler(System.out, new SimpleFormatter()));     
    }
   
    
    //Generates the regular landing page for games.
    private String doStandardPage(ServletContext application) throws PebbleException, SQLException, IOException {
    	
    	Map<String, Object> context = new HashMap<>();
		
    	PebbleEngine engine = new PebbleEngine.Builder().build();
		PebbleTemplate compiledTemplate;

		//Getting base template
		compiledTemplate = engine.getTemplate(application.getRealPath("/WEB-INF/templates/cart.html"));
		
		//Output to client
		Writer writer = new StringWriter();
		compiledTemplate.evaluate(writer, context);
		String output = writer.toString();
		
		return output;
    	
    }

}
