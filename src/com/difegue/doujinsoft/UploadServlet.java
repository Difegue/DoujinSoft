package com.difegue.doujinsoft;

import java.io.*;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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

import com.difegue.doujinsoft.utils.MioStorage;
import com.difegue.doujinsoft.utils.MioUtils;
import com.difegue.doujinsoft.utils.SaveExtractor;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.error.PebbleException;
import com.mitchellbosecke.pebble.template.PebbleTemplate;
import com.xperia64.diyedit.editors.GameEdit;
import com.xperia64.diyedit.editors.MangaEdit;
import com.xperia64.diyedit.editors.RecordEdit;
import com.xperia64.diyedit.metadata.Metadata;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.entity.StringEntity;

/**
 * Servlet implementation class for Uploads.
 * Uploaded .mio files are checked through DIYEdit and then dropped in the
 * "pending" directory.
 * If enabled by the matching environment variable, a webhook is triggered to
 * warn of the newly-uploaded file(s).
 */
@WebServlet("/upload")
@MultipartConfig(fileSizeThreshold = 1024 * 8, // 8KB - smallest .mio filetype, records
        maxFileSize = SaveExtractor.MAX_SAVE_SIZE, // .mio files can't go past 64KB, but DS save files are 32MB
        maxRequestSize = SaveExtractor.MAX_SAVE_SIZE + 1024 * 1024 * 5) // A save file plus a few .mio files.
public class UploadServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private Logger ServletLog;

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
     *      response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

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
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
     *      response)
     */
    protected void doPost(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        // gets absolute path of the web application
        ServletContext application = getServletConfig().getServletContext();
        String dataDir = application.getInitParameter("dataDirectory");

        // Uploaded .mios land in the "pending" directory
        String savePath = dataDir + "/pending";

        // creates the save directory if it does not exists
        File fileSaveDir = new File(savePath);
        if (!fileSaveDir.exists()) {
            fileSaveDir.mkdir();
        }

        String uploadSessionDesc = request.getParameter("description");
        String uploadSessionFiles = "";
        if (uploadSessionDesc == null || uploadSessionDesc.isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing description");
            return;
        }

        ServletLog.log(Level.INFO, "Upload session description: " + uploadSessionDesc);

        // Treat files one by one
        for (Part part : request.getParts()) {

            if (part.getName().equals("description")) {
                continue;
            }

            // Refine the fileName in case it is an absolute path
            String fileName = new File(extractFileName(part)).getName();
            byte[] data = part.getInputStream().readAllBytes();
            JsonObject json;

            if (SaveExtractor.looksLikeSave(data, fileName)) {
                // DS/Wii save file: every mio inside it goes to the pending directory.
                json = new JsonObject();
                JsonArray mios = new JsonArray();
                String base = fileName.replaceAll("\\.[^.]*$", "").replaceAll("[^A-Za-z0-9._-]", "_");
                Set<String> seen = new HashSet<>();
                try {
                    for (byte[] mio : SaveExtractor.extract(data, fileName)) {
                        String mioName = base + "-" + MioStorage.computeMioHash(mio).substring(0, 8) + ".mio";
                        if (!seen.add(mioName))
                            continue; // same mio referenced by several slots
                        JsonObject result = processMio(mio, mioName, savePath);
                        if (result.get("success").getAsBoolean()) {
                            mios.add(result);
                            uploadSessionFiles += mioName + "; ";
                        }
                    }
                } catch (Exception e) {
                    ServletLog.log(Level.WARNING, "Couldn't read save file " + fileName + ": " + e.getMessage());
                }
                json.addProperty("filename", fileName);
                json.addProperty("success", mios.size() > 0);
                json.add("mios", mios);
            } else {
                json = processMio(data, fileName, savePath);
                if (json.get("success").getAsBoolean())
                    uploadSessionFiles += fileName + "; ";
            }

            // Send JSON response
            response.setContentType("application/json");
            response.setCharacterEncoding("utf-8");
            PrintWriter out = response.getWriter();
            out.print(json.toString());
        }

        ServletLog.log(Level.INFO, "Upload session files: " + uploadSessionFiles);

        // Send webhook if enabled
        try {
            // Trigger a webhook
            if (System.getenv().containsKey("WEBHOOK_URL")) {
                HttpClient httpclient = HttpClients.createDefault();

                HttpPost httppost = new HttpPost(System.getenv("WEBHOOK_URL"));
                httppost.addHeader("Content-Type", "application/json; charset=UTF-8");
                httppost.addHeader("User-Agent", "DoujinSoft");
                httppost.addHeader("Accept", "application/json");

                // Create JSON payload
                JsonObject json = new JsonObject();
                json.addProperty("description", uploadSessionDesc);
                json.addProperty("files", uploadSessionFiles);
                json.addProperty("timestamp", System.currentTimeMillis());

                // Send JSON payload
                httppost.setEntity(new StringEntity(json.toString()));
                httpclient.execute(httppost);
            }
        } catch (Exception e) {
            ServletLog.log(Level.WARNING, "Couldn't hit upload webhook: " + e.getMessage());
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

    // Generates the regular landing page for the Uploader.
    private String doStandardPage(ServletContext application) throws PebbleException, SQLException, IOException {

        Map<String, Object> context = new HashMap<>();
        PebbleEngine engine = new PebbleEngine.Builder().build();
        PebbleTemplate compiledTemplate;

        // Getting base template
        compiledTemplate = engine.getTemplate(application.getRealPath("/WEB-INF/templates/upload.html"));

        // Output to client
        Writer writer = new StringWriter();
        compiledTemplate.evaluate(writer, context);
        String output = writer.toString();

        return output;
    }

    /**
     * Checks that the data is a proper .mio through DIYEdit and saves it to the pending directory if so.
     * Returns the JSON describing the result.
     */
    private JsonObject processMio(byte[] mioData, String fileName, String savePath) throws IOException {
        String type = "", name = "", creator = "", reason = "";
        boolean valid = true;
        try {
            Metadata m = null;
            switch (mioData.length) { // Do some tests
                case MioUtils.Types.GAME:
                    m = new GameEdit(mioData);
                    type = "game";
                    MioUtils.getBase64GamePreview(mioData);
                    break;
                case MioUtils.Types.MANGA:
                    m = new MangaEdit(mioData);
                    type = "manga";
                    MioUtils.getBase64Manga(mioData, 0);
                    break;
                case MioUtils.Types.RECORD:
                    m = new RecordEdit(mioData);
                    type = "record";
                    break;
                default:
                    valid = false;
                    break;
            }
            name = m.getName();
            creator = m.getCreator();

            if (MioUtils.isConvertedMio(m)) {
                valid = false;
                reason = "was rejected: generated by midi2mio.";
            }
        } catch (Exception e) {
            // Error while verifying file
            valid = false;
        }

        if (valid) {
            try (FileOutputStream fos = new FileOutputStream(savePath + File.separator + fileName)) {
                fos.write(mioData);
            }
        }

        JsonObject json = new JsonObject();
        json.addProperty("filename", fileName);
        json.addProperty("type", type);
        json.addProperty("name", name);
        json.addProperty("creator", creator);
        json.addProperty("success", valid);
        json.addProperty("reason", reason);
        return json;
    }

    /**
     * Extracts file name from HTTP header content-disposition
     */
    private String extractFileName(Part part) {
        String contentDisp = part.getHeader("content-disposition");
        String[] items = contentDisp.split(";");
        for (String s : items) {
            if (s.trim().startsWith("filename")) {
                return s.substring(s.indexOf("=") + 2, s.length() - 1);
            }
        }
        return "";
    }

}
