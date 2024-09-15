package com.difegue.doujinsoft;

import java.io.*;
import java.sql.*;
import java.util.*;
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
import com.difegue.doujinsoft.utils.CollectionUtils;
import com.difegue.doujinsoft.utils.MioStorage;
import com.difegue.doujinsoft.wc24.MailItem;
import com.difegue.doujinsoft.wc24.WiiConnect24Api;
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

        if (!authenticate(req, res))
            return;

        // ./manage?preview=true&file={file} can be used to download pending mio files.
        if (req.getParameterMap().containsKey("preview")) {
            ServletContext application = getServletConfig().getServletContext();
            String dataDir = application.getInitParameter("dataDirectory");

            // Serve file asked for
            String filename = req.getParameter("file");
            File previewMio = new File(dataDir + "/pending/" + filename);
            if (previewMio.exists()) {
                res.setContentType("application/octet-stream");
                res.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
                res.setContentLength((int) previewMio.length());
                FileInputStream fileIn = new FileInputStream(previewMio);
                OutputStream out = res.getOutputStream();
                byte[] outputByte = new byte[4096];
                // copy binary content to output stream
                while (fileIn.read(outputByte, 0, 4096) != -1) {
                    out.write(outputByte, 0, 4096);
                }
                fileIn.close();
                out.flush();
                out.close();
            } else {
                res.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
            return;
        }

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
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
     *      response)
     */
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

        ServletContext application = getServletConfig().getServletContext();
        String dataDir = application.getInitParameter("dataDirectory");
        String output = "Nothing!";
        // Needed for mio filenames with non-standard characters
        req.setCharacterEncoding("UTF-8");

        if (!authenticate(req, res))
            return;

        if (req.getParameterMap().containsKey("nsfw_ids")) {
            // Add hashes to NSFW list
            // Get content of textarea and split by line
            output = "param is " + req.getParameter("nsfw_ids");
            String[] nsfwHashes = req.getParameter("nsfw_ids").split("\\r?\\n");
            // Add to NSFW list
            try (Connection connection = DriverManager
                    .getConnection("jdbc:sqlite:" + dataDir + "/mioDatabase.sqlite")) {
                Statement statement = connection.createStatement();
                statement.setQueryTimeout(30); // set timeout to 30 sec.
                for (String hash : nsfwHashes) {
                    output += "Adding " + hash + " to NSFW list... \n";
                    statement.executeUpdate("update Games set isNsfw=1 where hash='" + hash + "'");
                }
            } catch (SQLException e) {
                ServletLog.log(Level.SEVERE, e.getMessage());
                output += e.getMessage();
            }
            output += "Added " + nsfwHashes.length + " hashes to NSFW list.";
        }

        if (req.getParameterMap().containsKey("collection_name")) {
            // New collection
            Collection c = new Collection();
            c.collection_type = req.getParameter("collection_type");
            c.collection_name = req.getParameter("collection_name");
            c.collection_icon = req.getParameter("collection_icon");
            c.collection_desc = req.getParameter("collection_desc");
            c.collection_desc2 = req.getParameter("collection_desc2");
            c.collection_color = req.getParameter("collection_color");
            c.background_pic = req.getParameter("background_pic");
            c.mios = new String[0];

            // Serialize new collection to file
            File collectionFile = new File(dataDir + "/collections/" + req.getParameter("collection_id") + ".json");
            CollectionUtils.SaveCollectionToFile(c, collectionFile.getAbsolutePath());
            output = "Collection created at " + collectionFile.getAbsolutePath();

        }

        if (req.getParameterMap().containsKey("approvedmios")) {
            output = "";
            // Approved/denied files w. collections
            for (String key : req.getParameterMap().keySet()) {

                // Only take approve-x.mio keys
                if (!key.startsWith("approve-"))
                    continue;

                var s = key.replace("approve-", "");
                output += "Looking for approved file " + s + "...\n";
                File approvedMio = new File(dataDir + "/pending/" + s);
                if (approvedMio.exists()) {
                    output += "Found! Adding to mio folder.\n";
                    // Add to collection
                    var cKey = "collection-" + s;
                    if (req.getParameterMap().containsKey(cKey) && !req.getParameter(cKey).isEmpty()) {

                        output += "Added to collection " + req.getParameter(cKey) + "\n";

                        // Deserialize collection, add new file hash and reserialize it
                        String path = dataDir + "/collections/" + req.getParameter(cKey);
                        Collection c = CollectionUtils.GetCollectionFromFile(path);
                        c.addMioHash(MioStorage.computeMioHash(FileByteOperations.read(approvedMio.getAbsolutePath())));
                        CollectionUtils.SaveCollectionToFile(c, path);

                    }
                    approvedMio.renameTo(new File(dataDir + "/mio/" + s));
                }
            }

            // Delete all remaining files in the pending directory
            File[] files = new File(dataDir + "/pending/").listFiles();
            if (files != null)
                for (File f : files) {
                    output += "File " + f.getName() + " not approved -- deleting.\n";
                    f.delete();
                }

            // Parse files in the mio dir
            try {
                MioStorage.ScanForNewMioFiles(dataDir, ServletLog);
            } catch (SQLException e) {
                ServletLog.log(Level.SEVERE, e.getMessage());
                output = e.getMessage();
            }
        }

        if (req.getParameterMap().containsKey("sendmail")) {
            // Send Wii mail through WC24
            ArrayList<MailItem> mails = new ArrayList<>();
            String message = req.getParameter("mail_content");
            String code = req.getParameter("wii_code");

            try (Connection connection = DriverManager
                    .getConnection("jdbc:sqlite:" + dataDir + "/mioDatabase.sqlite")) {
                if (code.equals("0")) {
                    // Get all wii codes stored in the friends table and send a mail to each one
                    Statement statement = connection.createStatement();
                    statement.setQueryTimeout(30); // set timeout to 30 sec.
                    ResultSet result = statement.executeQuery("select friendcode from Friends");

                    while (result.next())
                        mails.add(new MailItem(result.getString("friendcode"), message));

                    result.close();
                    statement.close();
                } else {
                    mails.add(new MailItem(code, message));
                }

                WiiConnect24Api wc24 = new WiiConnect24Api(application);
                output = wc24.sendMails(mails);

            } catch (Exception e) {
                ServletLog.log(Level.SEVERE, e.getMessage());
                output = e.getMessage();
            }
        }

        if (req.getParameterMap().containsKey("fetch_wc24_mail")) {

            try {
                WiiConnect24Api wc24 = new WiiConnect24Api(application);
                output = wc24.receiveMails();
                wc24.deleteMails();
            } catch (Exception e) {
                ServletLog.log(Level.SEVERE, e.getMessage());
                output = e.getMessage();
            }

        }

        // Set/reset creator and cartridge IDs
        if (req.getParameterMap().containsKey("set_creator_ids")) {

            try (Connection connection = DriverManager
                    .getConnection("jdbc:sqlite:" + dataDir + "/mioDatabase.sqlite")) {

                ServletLog.log(Level.INFO, "Updating metadata for all .mio files...");
                MioStorage.UpdateMetadata(connection, dataDir, ServletLog, "game");
                MioStorage.UpdateMetadata(connection, dataDir, ServletLog, "manga");
                MioStorage.UpdateMetadata(connection, dataDir, ServletLog, "record");

            } catch (Exception e) {
                ServletLog.log(Level.SEVERE, e.getMessage());
                output = e.getMessage();
            }
        }

        // Output is dumb HTML
        res.setContentType("text/html; charset=UTF-8");
        res.getWriter().append(output);
    }

    // Generates the regular landing page.
    private String doStandardPage(ServletContext application) throws PebbleException, IOException {

        String dataDir = application.getInitParameter("dataDirectory");
        Map<String, Object> context = new HashMap<>();

        // Parse pending .mios and add them to the context
        File[] files = new File(dataDir + "/pending/").listFiles();
        HashMap<String, BaseMio> pending = new HashMap<>();
        if (files != null)
            for (File f : files) {
                if (!f.isDirectory()) {
                    byte[] mioData = FileByteOperations.read(f.getAbsolutePath());
                    Metadata metadata = new Metadata(mioData);
                    BaseMio mio = new BaseMio(metadata);
                    pending.put(f.getName(), mio);
                }
            }
        context.put("pendingMios", pending);

        // Parse collections and add them to the context
        File[] collections = new File(dataDir + "/collections/").listFiles();
        HashMap<String, Collection> parsedCollections = new HashMap<>();

        if (collections != null)
            for (File f : collections) {
                if (!f.isDirectory()) {
                    // Try opening the matching JSON file
                    Gson gson = new Gson();
                    JsonReader jsonReader = new JsonReader(new FileReader(f));
                    // Auto bind the json to a class
                    parsedCollections.put(f.getName(), gson.fromJson(jsonReader, Collection.class));
                }
            }
        context.put("collections", parsedCollections);

        PebbleEngine engine = new PebbleEngine.Builder().build();
        PebbleTemplate compiledTemplate;

        // Getting base template
        compiledTemplate = engine.getTemplate(application.getRealPath("/WEB-INF/templates/admin.html"));

        // Output to client
        Writer writer = new StringWriter();
        compiledTemplate.evaluate(writer, context);
        String output = writer.toString();

        return output;

    }

    private boolean authenticate(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        // Get Authorization header
        String auth = req.getHeader("Authorization");
        // Do we allow that user?
        if (!allowUser(auth)) {
            // Not allowed, so report he's unauthorized
            res.setHeader("WWW-Authenticate", "BASIC realm=\"DoujinSoft Authentication\"");
            res.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        } else {
            return true;
        }
    }

    // This method checks the user information sent in the Authorization
    // header against the database of users maintained in the users Hashtable.
    private boolean allowUser(String auth) throws ServletException {

        if (auth == null) {
            return false; // no auth
        }
        if (!auth.toUpperCase().startsWith("BASIC ")) {
            return false; // we only do BASIC
        }
        // Get encoded user and password, comes after "BASIC "
        String userpassEncoded = auth.substring(6);
        // Decode it, using any base 64 decoder
        String userpassDecoded = new String(Base64.decodeBase64(userpassEncoded));

        // Check against the env var to see if we have access
        if (!System.getenv().containsKey("DSOFT_PASS")) {
            ServletLog.log(Level.SEVERE, "Environment variable DSOFT_PASS not set, Admin Console will be unavailable.");
            throw new ServletException("Password not set.");
        }

        if (userpassDecoded.equals(System.getenv("DSOFT_PASS"))) {
            return true;
        } else {
            return false;
        }
    }

}
