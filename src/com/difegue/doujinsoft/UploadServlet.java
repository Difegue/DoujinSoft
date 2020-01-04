package com.difegue.doujinsoft;

import java.io.*;
import java.sql.SQLException;
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

import com.difegue.doujinsoft.utils.ExportMidi;
import com.difegue.doujinsoft.utils.MioUtils;
import com.google.gson.JsonObject;
import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.error.PebbleException;
import com.mitchellbosecke.pebble.template.PebbleTemplate;
import com.xperia64.diyedit.editors.GameEdit;
import com.xperia64.diyedit.editors.MangaEdit;
import com.xperia64.diyedit.editors.RecordEdit;
import com.xperia64.diyedit.metadata.GameMetadata;
import com.xperia64.diyedit.metadata.MangaMetadata;
import com.xperia64.diyedit.metadata.Metadata;
import com.xperia64.diyedit.metadata.RecordMetadata;


/**
 * Servlet implementation class for Uploads.
 * Uploaded .mio files are checked through DIYEdit and then dropped in the "pending" directory.
 * If enabled by the matching environment variable, a webhook is triggered to warn of the newly-uploaded file(s).
 */
@WebServlet("/upload")
@MultipartConfig(fileSizeThreshold=1024*8,	// 8KB - smallest .mio filetype, records
        maxFileSize=1024*64,		        // 64KB - .mio files can't go past that size
        maxRequestSize=1024*1024*5)	        // 5MB - About 80 games, which is more than enough for a single request.
public class UploadServlet extends HttpServlet {
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
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response) throws ServletException, IOException {
        // gets absolute path of the web application
        ServletContext application = getServletConfig().getServletContext();
        String dataDir = application.getInitParameter("dataDirectory");

        // Uploaded .mios land in the "pending" directory
        String savePath = dataDir +"/pending";

        // creates the save directory if it does not exists
        File fileSaveDir = new File(savePath);
        if (!fileSaveDir.exists()) {
            fileSaveDir.mkdir();
        }

        for (Part part : request.getParts()) {
            String fileName = extractFileName(part);

            // Check if this is a proper .mio through DIYEdit
            String type = "",name = "",creator = "";
            byte[] mioData = part.getInputStream().readAllBytes();
            boolean valid = true;
            try {
                Metadata m = null;
                switch (mioData.length) { // Do some tests
                    case MioUtils.Types.GAME: m = new GameEdit(mioData); type ="game"; MioUtils.getBase64GamePreview(mioData); break;
                    case MioUtils.Types.MANGA: m = new MangaEdit(mioData); type="manga"; MioUtils.getBase64Manga(mioData,0); break;
                    case MioUtils.Types.RECORD: m = new RecordEdit(mioData); type="record"; break;
                    default: valid = false; break;
                }
                name = m.getName();
                creator = m.getCreator();
            } catch (Exception e) {
                // Error while verifying file
                valid = false;
            }

            // Skip file if invalid
            if (valid) {
                // Refine the fileName in case it is an absolute path
                fileName = new File(fileName).getName();
                // Save it to the pending directory
                File mio = new File (savePath + File.separator + fileName);
                try (FileOutputStream fos = new FileOutputStream(mio.getAbsolutePath())) {
                    fos.write(mioData);
                }
            }

            // Send JSON response
            response.setContentType("application/json");
            response.setCharacterEncoding("utf-8");
            PrintWriter out = response.getWriter();

            //create Json Object
            JsonObject json = new JsonObject();

            // put some value pairs into the JSON object
            json.addProperty("filename", fileName);
            json.addProperty("type", type);
            json.addProperty("name", name);
            json.addProperty("creator", creator);
            json.addProperty("success", valid);

            // finally output the json string
            out.print(json.toString());
        }

    }

    /**
     * @see HttpServlet#HttpServlet()
     */
    public UploadServlet() {
        super();
        ServletLog = Logger.getLogger("UploadServlet");
        ServletLog.addHandler(new StreamHandler(System.out, new SimpleFormatter()));
    }

    //Generates the regular landing page for the Uploader.
    private String doStandardPage(ServletContext application) throws PebbleException, SQLException, IOException {

        Map<String, Object> context = new HashMap<>();
        PebbleEngine engine = new PebbleEngine.Builder().build();
        PebbleTemplate compiledTemplate;

        //Getting base template
        compiledTemplate = engine.getTemplate(application.getRealPath("/WEB-INF/templates/upload.html"));

        //Output to client
        Writer writer = new StringWriter();
        compiledTemplate.evaluate(writer, context);
        String output = writer.toString();

        return output;
    }

    /**
     * Extracts file name from HTTP header content-disposition
     */
    private String extractFileName(Part part) {
        String contentDisp = part.getHeader("content-disposition");
        String[] items = contentDisp.split(";");
        for (String s : items) {
            if (s.trim().startsWith("filename")) {
                return s.substring(s.indexOf("=") + 2, s.length()-1);
            }
        }
        return "";
    }

}
