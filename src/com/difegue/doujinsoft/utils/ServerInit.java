package com.difegue.doujinsoft.utils;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;
import java.util.Scanner;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import com.difegue.doujinsoft.utils.MioUtils.Types;
import com.difegue.doujinsoft.wc24.MailItemParser;
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
                + "(hash TEXT, id TEXT, name TEXT, normalizedName TEXT, creator TEXT, brand TEXT, description TEXT, timeStamp INTEGER, color TEXT, colorLogo TEXT, logo INTEGER, "
                + "previewPic TEXT, PRIMARY KEY(`hash`) )");

        statement.executeUpdate("CREATE TABLE IF NOT EXISTS Manga "
                + "(hash TEXT, id TEXT, name TEXT, normalizedName TEXT, creator TEXT, brand TEXT, description TEXT, timeStamp INTEGER, color TEXT, colorLogo TEXT, logo INTEGER, "
                + "frame0 TEXT, frame1 TEXT, frame2 TEXT, frame3 TEXT, PRIMARY KEY(`hash`) )");

        statement.executeUpdate("CREATE TABLE IF NOT EXISTS Records "
                + "(hash TEXT, id TEXT, name TEXT, normalizedName TEXT, creator TEXT, brand TEXT, description TEXT, timeStamp INTEGER, color TEXT, colorLogo TEXT, logo INTEGER, "
                + "PRIMARY KEY(`hash`) )");

        statement.executeUpdate("CREATE TABLE IF NOT EXISTS Surveys "
                + "(timestamp INTEGER, type INTEGER, name TEXT, stars INTEGER, commentId INTEGER, "
                + "PRIMARY KEY(`timestamp`) )");
    }

    /*
     * Standard parsing for every .mio file - Returns the first values of the final SQL Statement.
     */
    private PreparedStatement parseMioBase(Metadata mio, String hash, String ID, Connection co, int type) throws SQLException {

        PreparedStatement ret = null;
        String query = "";

        switch (type) {
            case Types.GAME: query = "INSERT INTO Games VALUES (?,?,?,?,?,?,?,?,?,?,?,?)"; break;
            case Types.MANGA: query = "INSERT INTO Manga VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"; break;
            case Types.RECORD: query = "INSERT INTO Records VALUES (?,?,?,?,?,?,?,?,?,?,?)"; break;
        }

        ret = co.prepareStatement(query);

        String normalizedName = mio.getName().replaceAll("\\p{Punct}", "z");
        
        ret.setString(1, hash);
        ret.setString(2, ID);
        ret.setString(3, mio.getName());
        ret.setString(4, normalizedName);
        ret.setString(5, mio.getCreator());
        ret.setString(6, mio.getBrand());
        ret.setString(7, mio.getDescription());
        ret.setInt(8, mio.getTimestamp());

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
        ServletContext application = arg0.getServletContext();
        String dataDir = application.getInitParameter("dataDirectory");
       
        Connection connection = null;

        try {
            // Create database if nonexistent
            SQLog.log(Level.INFO, "Connecting to database at "+dataDir+"/mioDatabase.sqlite");

            connection = DriverManager.getConnection("jdbc:sqlite:"+dataDir+"/mioDatabase.sqlite");
            Statement statement = connection.createStatement();
            statement.setQueryTimeout(30);  // set timeout to 30 sec.

            databaseDefinition(statement);

            // Look for a WC24 mail dump file and parse it if it exists
            SQLog.log(Level.INFO, "Looking for mails.wc24 file...");
            File wc24Mails = new File(dataDir+"/mails.wc24");
            
            if (wc24Mails.exists()) 
            try (Scanner s = new Scanner(wc24Mails)) {
                String emailData = s.useDelimiter("\\Z").next();
                new MailItemParser(application).consumeEmails(emailData);
                wc24Mails.delete();
            }

            // Parse .mios in "new" folder before renaming+moving them
            SQLog.log(Level.INFO, "Looking for new .mio files...");

            //Create the mio directory if it doesn't exist - although that means we probably won't find any games to parse...
            if (!new File (dataDir+"/mio/").exists())
                new File(dataDir+"/mio/").mkdirs();
            
            File[] files = new File(dataDir+"/mio/").listFiles();

            for (File f: files) {
                if (!f.isDirectory()) {

                    byte[] mioData = FileByteOperations.read(f.getAbsolutePath());
                    Metadata metadata = new Metadata(mioData);
                    String hash = MioStorage.computeMioHash(mioData);
                    String ID = MioStorage.computeMioID(metadata);
                    int type = mioData.length;

                    PreparedStatement insertQuery = parseMioBase(metadata, hash, ID, connection, type);

                    //The file is game, manga or record, depending on its size.
                    if (mioData.length == Types.GAME) {
                        GameEdit game = new GameEdit(mioData);

                        //Game-specific: add the preview picture
                        insertQuery.setString(9, MioUtils.mapColorByte(game.getCartColor()));
                        insertQuery.setString(10, MioUtils.mapColorByte(game.getLogoColor()));
                        insertQuery.setInt(11, game.getLogo());
                        insertQuery.setString(12, MioUtils.getBase64GamePreview(mioData));

                        SQLog.log(Level.INFO, "Game;"+hash+";"+ID+";"+game.getName()+"\n");
                    }

                    if (mioData.length == Types.MANGA) {
                        MangaEdit manga = new MangaEdit(mioData);

                        //Manga-specific: add the panels
                        insertQuery.setString(9, MioUtils.mapColorByte(manga.getMangaColor()));
                        insertQuery.setString(10, MioUtils.mapColorByte(manga.getLogoColor()));
                        insertQuery.setInt(11, manga.getLogo());
                        insertQuery.setString(12, MioUtils.getBase64Manga(mioData, 0));
                        insertQuery.setString(13, MioUtils.getBase64Manga(mioData, 1));
                        insertQuery.setString(14, MioUtils.getBase64Manga(mioData, 2));
                        insertQuery.setString(15, MioUtils.getBase64Manga(mioData, 3));

                        SQLog.log(Level.INFO, "Manga;"+hash+";"+ID+";"+manga.getName()+"\n");
                    }

                    if (mioData.length == Types.RECORD) {
                        RecordEdit record = new RecordEdit(mioData);

                        insertQuery.setString(9, MioUtils.mapColorByte(record.getRecordColor()));
                        insertQuery.setString(10, MioUtils.mapColorByte(record.getLogoColor()));
                        insertQuery.setInt(11, record.getLogo());

                        SQLog.log(Level.INFO, "Record;"+hash+";"+ID+";"+record.getName()+"\n");
                    }

                    SQLog.log(Level.INFO, "Inserting into DB");

                    try {
                        insertQuery.executeUpdate();
                        MioStorage.consumeMio(f, hash, type);
                    } catch (SQLException e) {
                        SQLog.log(Level.SEVERE, "Couldn't insert this mio in the database - Likely a duplicate file, moving on.");
                        SQLog.log(Level.SEVERE, e.getMessage());
                    }
                }
            }
            
            statement.executeUpdate("DROP INDEX IF EXISTS Games_idx;");
            statement.executeUpdate("DROP INDEX IF EXISTS Manga_idx;");
            statement.executeUpdate("DROP INDEX IF EXISTS Record_idx;");   

            // Re-create indexes
            statement.executeUpdate("CREATE INDEX Games_idx ON Games (normalizedName ASC, id);");
            statement.executeUpdate("CREATE INDEX Manga_idx ON Manga (normalizedName ASC, id);");
            statement.executeUpdate("CREATE INDEX Record_idx ON Records (normalizedName ASC, id);");
            
        }
        catch(Exception e){
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
