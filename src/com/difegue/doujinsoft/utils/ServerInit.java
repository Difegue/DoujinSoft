package com.difegue.doujinsoft.utils;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import com.difegue.doujinsoft.utils.MioUtils.Types;
import com.xperia64.diyedit.FileByteOperations;
import com.xperia64.diyedit.editors.GameEdit;
import com.xperia64.diyedit.editors.MangaEdit;
import com.xperia64.diyedit.editors.RecordEdit;
import com.xperia64.diyedit.metadata.Metadata;

/*
 * Ran on each server startup - Handles database updating.
 */
public class ServerInit implements javax.servlet.ServletContextListener {


    //Database structure, straightforward stuff
    private void databaseDefinition(Statement statement) throws SQLException
    {
        statement.executeUpdate("CREATE TABLE IF NOT EXISTS Games "
                + "(hash TEXT, id TEXT, name TEXT, creator TEXT, brand TEXT, description TEXT, timeStamp INTEGER, color TEXT, colorLogo TEXT, logo INTEGER, "
                + "previewPic TEXT, PRIMARY KEY(`hash`) )");

        statement.executeUpdate("CREATE TABLE IF NOT EXISTS Manga "
                + "(hash TEXT, id TEXT, name TEXT, creator TEXT, brand TEXT, description TEXT, timeStamp INTEGER, color TEXT, colorLogo TEXT, logo INTEGER, "
                + "frame0 TEXT, frame1 TEXT, frame2 TEXT, frame3 TEXT, PRIMARY KEY(`hash`) )");

        statement.executeUpdate("CREATE TABLE IF NOT EXISTS Records "
                + "(hash TEXT, id TEXT, name TEXT, creator TEXT, brand TEXT, description TEXT, timeStamp INTEGER, color TEXT, colorLogo TEXT, logo INTEGER, "
                + "PRIMARY KEY(`hash`) )");
    }

    /*
     * Standard parsing for every .mio file - Returns the first values of the final SQL Statement.
     */
    private PreparedStatement parseMioBase(Metadata mio, String hash, String ID, Connection co, int type) throws SQLException {

        PreparedStatement ret = null;
        String query = "";

        switch (type) {
            case Types.GAME: query = "INSERT INTO Games VALUES (?,?,?,?,?,?,?,?,?,?,?)"; break;
            case Types.MANGA: query = "INSERT INTO Manga VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)"; break;
            case Types.RECORD: query = "INSERT INTO Records VALUES (?,?,?,?,?,?,?,?,?,?)"; break;
        }

        ret = co.prepareStatement(query);

        ret.setString(1, hash);
        ret.setString(2, ID);
        ret.setString(3, mio.getName());
        ret.setString(4, mio.getCreator());
        ret.setString(5, mio.getBrand());
        ret.setString(6, mio.getDescription());
        ret.setInt(7, mio.getTimestamp());

        return ret;
    }

    @Override
    public void contextInitialized(ServletContextEvent arg0) {

        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e1) {
            e1.printStackTrace();
        }
        System.out.println("DoujinSoft Store Deployed.");

        // Hee to the ho and here we go
        Logger SQLog = Logger.getLogger("SQLite");
        SQLog.addHandler(new StreamHandler(System.out, new SimpleFormatter()));

        SQLog.log(Level.INFO, "Looking for new .mio files...");
        Connection connection = null;

        //Create database if nonexistent + parse .mios in "new" folder before renaming+moving them
        ServletContext application = arg0.getServletContext();
        String dataDir = application.getInitParameter("dataDirectory");

        try {
            // create a database connection
            SQLog.log(Level.INFO, "Connecting to database at "+dataDir+"/mioDatabase.sqlite");

            connection = DriverManager.getConnection("jdbc:sqlite:"+dataDir+"/mioDatabase.sqlite");
            Statement statement = connection.createStatement();
            statement.setQueryTimeout(30);  // set timeout to 30 sec.

            databaseDefinition(statement);

            //Create the mio directory if it doesn't exist - although that means we probably won't find any games to parse...
            if (!new File (dataDir+"/mio/").exists())
                new File(dataDir+"/mio/").mkdirs();

            //Let's jam some .mios in this
            File[] files = new File(dataDir+"/mio/").listFiles();

            Date today = Calendar.getInstance().getTime();
            SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy-hh.mm.ss");
            String logFileName = "NewMios-"+formatter.format(today);

            // create a logFile for this deployment session
            FileWriter fw = new FileWriter(dataDir+"/"+logFileName+".csv");
            BufferedWriter bw = new BufferedWriter(fw);

            for (File f: files) {
                if (!f.isDirectory()) {

                    SQLog.log(Level.INFO, "Parsing file "+f.getName());
                    byte[] mioData = FileByteOperations.read(f.getAbsolutePath());
                    String hash = MioStorage.computeMioHash(mioData);
                    String ID;
                    PreparedStatement insertQuery = null;

                    //The file is game, manga or record, depending on its size.
                    if (mioData.length == Types.GAME) {
                        SQLog.log(Level.INFO, "Detected as game.");
                        GameEdit game = new GameEdit(mioData);

                        ID = MioStorage.computeMioID(f, game);
                        insertQuery = parseMioBase(game, hash, ID, connection, Types.GAME);
                        MioStorage.consumeMio(f, hash, Types.GAME);

                        //Game-specific: add the preview picture
                        insertQuery.setString(8, MioUtils.mapColorByte(game.getCartColor()));
                        insertQuery.setString(9, MioUtils.mapColorByte(game.getLogoColor()));
                        insertQuery.setInt(10, game.getLogo());
                        insertQuery.setString(11, MioUtils.getBase64GamePreview(mioData));

                        bw.write("Game;"+hash+";"+ID+";"+game.getName()+"\n");
                    }

                    if (mioData.length == Types.MANGA) {
                        SQLog.log(Level.INFO, "Detected as comic.");
                        MangaEdit manga = new MangaEdit(mioData);

                        ID = MioStorage.computeMioID(f, manga);
                        insertQuery = parseMioBase(manga, hash, ID, connection, Types.MANGA);
                        MioStorage.consumeMio(f, hash, Types.MANGA);

                        //Manga-specific: add the panels
                        insertQuery.setString(8, MioUtils.mapColorByte(manga.getMangaColor()));
                        insertQuery.setString(9, MioUtils.mapColorByte(manga.getLogoColor()));
                        insertQuery.setInt(10, manga.getLogo());
                        insertQuery.setString(11, MioUtils.getBase64Manga(mioData, 0));
                        insertQuery.setString(12, MioUtils.getBase64Manga(mioData, 1));
                        insertQuery.setString(13, MioUtils.getBase64Manga(mioData, 2));
                        insertQuery.setString(14, MioUtils.getBase64Manga(mioData, 3));

                        bw.write("Manga;"+hash+";"+ID+";"+manga.getName()+"\n");
                    }

                    if (mioData.length == Types.RECORD) {
                        SQLog.log(Level.INFO, "Detected as record.");
                        RecordEdit record = new RecordEdit(mioData);

                        ID = MioStorage.computeMioID(f, record);
                        insertQuery = parseMioBase(record, hash, ID, connection, Types.RECORD);
                        MioStorage.consumeMio(f, hash, Types.RECORD);

                        insertQuery.setString(8, MioUtils.mapColorByte(record.getRecordColor()));
                        insertQuery.setString(9, MioUtils.mapColorByte(record.getLogoColor()));
                        insertQuery.setInt(10, record.getLogo());

                        bw.write("Record;"+hash+";"+ID+";"+record.getName()+"\n");
                    }

                    SQLog.log(Level.INFO, "Inserting into DB");

                    try {
                        insertQuery.executeUpdate();
                    } catch (SQLException e) {
                        SQLog.log(Level.SEVERE, "Couldn't insert this mio in the database - Likely a duplicate file, moving on.");
                        SQLog.log(Level.SEVERE, e.getMessage());
                    }
                }
            }

            // Close stream
            bw.close();
            
            //If logfile is empty, no mios were handled -> delete it
            File logfile = new File(dataDir+"/"+logFileName+".csv");
            if (logfile.length() == 0)
                logfile.delete();
            else {
                statement.executeUpdate("DROP INDEX IF EXISTS Games_idx;");
                statement.executeUpdate("DROP INDEX IF EXISTS Manga_idx;");
                statement.executeUpdate("DROP INDEX IF EXISTS Record_idx;");   
                
                // Re-create indexes
                statement.executeUpdate("CREATE INDEX Games_idx ON Games (name ASC, id);");
                statement.executeUpdate("CREATE INDEX Manga_idx ON Manga (name ASC, id);");
                statement.executeUpdate("CREATE INDEX Record_idx ON Records (name ASC, id);");
            }
        }
        catch(SQLException | IOException e){
            // if the error message is "out of memory",
            // it probably means no database file is found
            SQLog.log(Level.SEVERE, e.getMessage());
        }
        finally {
            try {
                if(connection != null)
                    connection.close();
            }
            catch(SQLException e) {
                SQLog.log(Level.SEVERE, "connection close failed: " + e.getMessage());
            }
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent arg0) {

    }


}
