package com.difegue.doujinsoft.utils;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;
import java.util.Scanner;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.difegue.doujinsoft.wc24.MailItemParser;
import com.difegue.doujinsoft.wc24.WiiConnect24Api;

/*
 * Ran on each server startup - Handles database updating.
 */
public class ServerInit implements javax.servlet.ServletContextListener {

    private ScheduledExecutorService scheduler;

    // Database structure, straightforward stuff
    private void databaseDefinition(Statement statement) throws SQLException {
        statement.executeUpdate("CREATE TABLE IF NOT EXISTS Games "
                + "(hash TEXT, id TEXT, name TEXT, normalizedName TEXT, creator TEXT, brand TEXT, description TEXT, timeStamp INTEGER, color TEXT, colorLogo TEXT, logo INTEGER, "
                + "previewPic TEXT, creatorID TEXT, isNsfw BOOLEAN DEFAULT 0, cartridgeID TEXT, PRIMARY KEY(`hash`) )");

        statement.executeUpdate("CREATE TABLE IF NOT EXISTS Manga "
                + "(hash TEXT, id TEXT, name TEXT, normalizedName TEXT, creator TEXT, brand TEXT, description TEXT, timeStamp INTEGER, color TEXT, colorLogo TEXT, logo INTEGER, "
                + "frame0 TEXT, frame1 TEXT, frame2 TEXT, frame3 TEXT, creatorID TEXT, cartridgeID TEXT, PRIMARY KEY(`hash`) )");

        statement.executeUpdate("CREATE TABLE IF NOT EXISTS Records "
                + "(hash TEXT, id TEXT, name TEXT, normalizedName TEXT, creator TEXT, brand TEXT, description TEXT, timeStamp INTEGER, color TEXT, colorLogo TEXT, logo INTEGER, "
                + "creatorID TEXT, cartridgeID TEXT, PRIMARY KEY(`hash`) )");

        statement.executeUpdate("CREATE TABLE IF NOT EXISTS Surveys "
                + "(timestamp INTEGER, type INTEGER, name TEXT, stars INTEGER, commentId INTEGER, friendcode TEXT,"
                + "PRIMARY KEY(`timestamp`) )");

        statement.executeUpdate("CREATE TABLE IF NOT EXISTS Friends "
                + "(friendcode TEXT, "
                + "PRIMARY KEY(`friendcode`) )");
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

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dataDir + "/mioDatabase.sqlite")) {
            // Create database if nonexistent
            SQLog.log(Level.INFO, "Connected to database at " + dataDir + "/mioDatabase.sqlite");

            Statement statement = connection.createStatement();
            statement.setQueryTimeout(30); // set timeout to 30 sec.

            databaseDefinition(statement);

            // Look for a WC24 mail dump file and parse it if it exists
            SQLog.log(Level.INFO, "Looking for mails.wc24 file...");
            File wc24Mails = new File(dataDir + "/mails.wc24");

            if (wc24Mails.exists()) {
                try (Scanner s = new Scanner(wc24Mails)) {
                    String emailData = s.useDelimiter("\\Z").next();
                    new MailItemParser(application).consumeEmails(emailData);
                }
                wc24Mails.delete();
            }

            // Create the mio directory if it doesn't exist - although that means we
            // probably won't find any games to parse...
            if (!new File(dataDir + "/mio/").exists())
                new File(dataDir + "/mio/").mkdirs();

            // Parse .mios in "new" folder before renaming+moving them
            SQLog.log(Level.INFO, "Looking for new .mio files...");
            MioStorage.ScanForNewMioFiles(dataDir, SQLog);

            statement.executeUpdate("PRAGMA journal_mode=WAL;");
            statement.executeUpdate("DROP INDEX IF EXISTS Games_idx;");
            statement.executeUpdate("DROP INDEX IF EXISTS Manga_idx;");
            statement.executeUpdate("DROP INDEX IF EXISTS Record_idx;");
            statement.executeUpdate("DROP INDEX IF EXISTS Games_search_idx;");
            statement.executeUpdate("DROP INDEX IF EXISTS Manga_search_idx;");
            statement.executeUpdate("DROP INDEX IF EXISTS Record_search_idx;");
            statement.executeUpdate("DROP INDEX IF EXISTS Games_search_idx2;");
            statement.executeUpdate("DROP INDEX IF EXISTS Manga_search_idx2;");
            statement.executeUpdate("DROP INDEX IF EXISTS Record_search_idx2;");
            statement.executeUpdate("DROP INDEX IF EXISTS Games_search_idx3;");
            statement.executeUpdate("DROP INDEX IF EXISTS Manga_search_idx3;");
            statement.executeUpdate("DROP INDEX IF EXISTS Record_search_idx3;");
            statement.executeUpdate("DROP INDEX IF EXISTS Games_search_idx4;");
            statement.executeUpdate("DROP INDEX IF EXISTS Manga_search_idx4;");
            statement.executeUpdate("DROP INDEX IF EXISTS Record_search_idx4;");
            statement.executeUpdate("DROP INDEX IF EXISTS Games_search_idx5;");
            statement.executeUpdate("DROP INDEX IF EXISTS Manga_search_idx5;");
            statement.executeUpdate("DROP INDEX IF EXISTS Record_search_idx5;");

            // Rebuild indexes
            statement.executeUpdate("CREATE INDEX Games_idx ON Games (normalizedName ASC, id);");
            statement.executeUpdate("CREATE INDEX Manga_idx ON Manga (normalizedName ASC, id);");
            statement.executeUpdate("CREATE INDEX Record_idx ON Records (normalizedName ASC, id);");
            statement.executeUpdate("CREATE INDEX Games_search_idx ON Games (name COLLATE NOCASE);");
            statement.executeUpdate("CREATE INDEX Manga_search_idx ON Manga (name COLLATE NOCASE);");
            statement.executeUpdate("CREATE INDEX Record_search_idx ON Records (name COLLATE NOCASE);");
            statement.executeUpdate("CREATE INDEX Games_search_idx2 ON Games (creator COLLATE NOCASE);");
            statement.executeUpdate("CREATE INDEX Manga_search_idx2 ON Manga (creator COLLATE NOCASE);");
            statement.executeUpdate("CREATE INDEX Record_search_idx2 ON Records (creator COLLATE NOCASE);");
            statement.executeUpdate("CREATE INDEX Games_search_idx3 ON Games (timeStamp);");
            statement.executeUpdate("CREATE INDEX Manga_search_idx3 ON Manga (timeStamp);");
            statement.executeUpdate("CREATE INDEX Record_search_idx3 ON Records (timeStamp);");

            statement.executeUpdate("CREATE INDEX Games_search_idx4 ON Games (cartridgeID);");
            statement.executeUpdate("CREATE INDEX Manga_search_idx4 ON Manga (cartridgeID);");
            statement.executeUpdate("CREATE INDEX Record_search_idx4 ON Records (cartridgeID);");

            statement.executeUpdate("CREATE INDEX Games_search_idx5 ON Games (creatorID);");
            statement.executeUpdate("CREATE INDEX Manga_search_idx5 ON Manga (creatorID);");
            statement.executeUpdate("CREATE INDEX Record_search_idx5 ON Records (creatorID);");

            statement.close();
        } catch (Exception e) {
            // if the error message is "out of memory",
            // it probably means no database file is found
            e.printStackTrace();
            SQLog.log(Level.SEVERE, e.getMessage());
        }

        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(new WiiConnect24MailCollection(application), 0, 1, TimeUnit.HOURS);
    }

    @Override
    public void contextDestroyed(ServletContextEvent arg0) {
        scheduler.shutdownNow();
    }

    public class WiiConnect24MailCollection implements Runnable {

        private ServletContext application;

        public WiiConnect24MailCollection(ServletContext a) {
            application = a;
        }

        @Override
        public void run() {
            // Receive mails once per hour.
            try {
                Logger.getLogger("WiiConnect24").info("Collecting mails from WiiConnect24 now.");
                WiiConnect24Api wc24 = new WiiConnect24Api(application);
                wc24.receiveMails();
                wc24.deleteMails();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
