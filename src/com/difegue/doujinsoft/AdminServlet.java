package com.difegue.doujinsoft;

import java.io.*;
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

import com.difegue.doujinsoft.templates.BaseMio;
import com.difegue.doujinsoft.templates.Collection;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.error.PebbleException;
import com.mitchellbosecke.pebble.template.PebbleTemplate;
import com.xperia64.diyedit.FileByteOperations;
import com.xperia64.diyedit.metadata.Metadata;

import org.apache.commons.codec.binary.Base64;

@WebServlet("/manage")
public class AdminServlet extends HttpServlet { 

    private static final long serialVersionUID = 1L;
    private Logger ServletLog;

    HashMap validUsers = new HashMap();

    /**
     * @see HttpServlet#HttpServlet()
     */
    public AdminServlet() {
        super(); 
        ServletLog = Logger.getLogger("AdminServlet");
        ServletLog.addHandler(new StreamHandler(System.out, new SimpleFormatter()));    
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse res)
                    throws ServletException, IOException {
        
        res.setContentType("text/html");

        if (!authenticate(req,res))
            return;

        // Allowed
        res.setContentType("text/html; charset=UTF-8");
        ServletContext application = getServletConfig().getServletContext();
        String output = "";
        
        try {
            output = doStandardPage(application);
            res.getWriter().append(output);
                
        } catch (PebbleException e) {
            ServletLog.log(Level.SEVERE, e.getMessage());
        }
    }

    /**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		boolean gotFileContentType = false;
		String output = "Invalid file.";

        if (!authenticate(req,res))
            return;

		if (req.getParameter("operation").equals("approvefiles")) {
			// Content approved
			try {
				output = ""; //sendWC24(req, res);
			} catch (Exception e) {
				output = e.getMessage();
			}
		}

		if (req.getParameter("method").equals("rejectfiles")) {
			// Collection created
			try {
				gotFileContentType = true; //injectMios(req, res);
			} catch (Exception e) {
				ServletLog.log(Level.SEVERE, e.getMessage());
			}
		}

        if (req.getParameter("method").equals("addtocollection")) {
            // Add ID to collection
            try {
                gotFileContentType = true ;//injectMios(req, res);
            } catch (Exception e) {
                ServletLog.log(Level.SEVERE, e.getMessage());
            }
        }

        if (req.getParameter("method").equals("createcollection")) {
            // Add ID to collection
            try {
                gotFileContentType = true ;//injectMios(req, res);
            } catch (Exception e) {
                ServletLog.log(Level.SEVERE, e.getMessage());
            }
        }

		//Output is JSON with the result
		if (!gotFileContentType)
		{
			res.setContentType("text/html; charset=UTF-8");
			res.getWriter().append(output);
		}
	}

    //Generates the regular landing page.
    private String doStandardPage(ServletContext application) throws PebbleException, IOException {

        String dataDir = application.getInitParameter("dataDirectory");
        Map<String, Object> context = new HashMap<>();

        // Parse pending .mios and add them to the context
        File[] files = new File(dataDir+"/pending/").listFiles();
        HashMap<String, BaseMio> pending = new HashMap<>();
        if (files != null) for (File f: files) {
            if (!f.isDirectory()) {
                byte[] mioData = FileByteOperations.read(f.getAbsolutePath());
                Metadata metadata = new Metadata(mioData);
                BaseMio mio = new BaseMio(metadata);
                pending.put(f.getAbsolutePath(), mio);
            }
    }
        context.put("pendingMios", pending);

        // Parse collections and add them to the context
        File[] collections = new File(dataDir+"/collections/").listFiles();
        HashMap<String,Collection> parsedCollections = new HashMap<>();

        if (collections != null) for (File f: collections) {
            if (!f.isDirectory()) {
                //Try opening the matching JSON file 
                Gson gson = new Gson();
                JsonReader jsonReader = new JsonReader(new FileReader(f));
                //Auto bind the json to a class
                parsedCollections.put(f.getAbsolutePath(),gson.fromJson(jsonReader, Collection.class));
            }
        }
        context.put("collections", parsedCollections);

        PebbleEngine engine = new PebbleEngine.Builder().build();
        PebbleTemplate compiledTemplate;

        //Getting base template
        compiledTemplate = engine.getTemplate(application.getRealPath("/WEB-INF/templates/admin.html"));
        
        //Output to client
        Writer writer = new StringWriter();
        compiledTemplate.evaluate(writer, context);
        String output = writer.toString();
        
        return output;
        
    }

    private boolean authenticate(HttpServletRequest req, HttpServletResponse res) throws IOException {
        // Get Authorization header
        String auth = req.getHeader("Authorization");
        // Do we allow that user?
        if (!allowUser(auth)) {
            // Not allowed, so report he's unauthorized
            res.setHeader("WWW-Authenticate", "BASIC realm=\"DoujinSoft Authentication\"");
            res.sendError(res.SC_UNAUTHORIZED);
            return false;
        } else {
            return true;
        }
    }

    // This method checks the user information sent in the Authorization
    // header against the database of users maintained in the users Hashtable.
    private boolean allowUser(String auth) throws IOException {

        if (auth == null) {
            return false;  // no auth
        }
        if (!auth.toUpperCase().startsWith("BASIC ")) {
            return false;  // we only do BASIC
        }
        // Get encoded user and password, comes after "BASIC "
        String userpassEncoded = auth.substring(6);
        // Decode it, using any base 64 decoder
        String userpassDecoded = new String(Base64.decodeBase64(userpassEncoded));

        // Check against the env var to see if we have access
        if (userpassDecoded.equals("jswan:mypassword")) { //System.getenv("DSOFT_PASS")
            return true;
        } else {
            return false;
        }
    }

}
