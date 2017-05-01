package com.difegue.doujinsoft;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import com.difegue.doujinsoft.MioUtils.Types;
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
      + "(id TEXT, name TEXT, creator TEXT, brand TEXT, description TEXT, timeStamp INTEGER, color TEXT, colorLogo TEXT, logo INTEGER, isAdult INTEGER, "
      + "previewPic TEXT, PRIMARY KEY(`id`) )");
    
    statement.executeUpdate("CREATE TABLE IF NOT EXISTS Manga "
      + "(id TEXT, name TEXT, creator TEXT, brand TEXT, description TEXT, timeStamp INTEGER, color TEXT, colorLogo TEXT, logo INTEGER, isAdult INTEGER, "
      + "frame0 TEXT, frame1 TEXT, frame2 TEXT, frame3 TEXT, PRIMARY KEY(`id`) )");
    
    statement.executeUpdate("CREATE TABLE IF NOT EXISTS Records "
      + "(id TEXT, name TEXT, creator TEXT, brand TEXT, description TEXT, timeStamp INTEGER, color TEXT, colorLogo TEXT, logo INTEGER, "
      + "PRIMARY KEY(`id`) )");
	
}

/* 
 * Standard parsing for every .mio file - Returns the first values of the final SQL Statement.
 */
private PreparedStatement parseMioBase(Metadata mio, String ID, Connection co, int type) throws SQLException {
	
	PreparedStatement ret = null;
	String query = "";
	
	switch (type) {
		case Types.GAME: query = "INSERT INTO Games VALUES (?,?,?,?,?,?,?,?,?,?,?)"; break;
		case Types.MANGA: query = "INSERT INTO Manga VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)"; break;
		case Types.RECORD: query = "INSERT INTO Records VALUES (?,?,?,?,?,?,?,?,?)"; break;
	}
	
	ret = co.prepareStatement(query);
	
	ret.setString(1, ID);
	ret.setString(2, mio.getName());
	ret.setString(3, mio.getCreator());
	ret.setString(4, mio.getBrand());
	ret.setString(5, mio.getDescription());
	ret.setInt(6, mio.getTimestamp());
	
	if (type == Types.GAME || type == Types.MANGA)
		ret.setInt(10, 0);
	
	return ret;
}

/*
 * Craft ID from .mio metadata, and check if it's available.
 * If it is, move and rename the original file to the appropriate spot in the data directory.
 */
private String computeMioID(File f, Metadata mio, int type) {
	
	
	Logger SQLog = Logger.getLogger("SQLite");
	String ID = mio.getSerial1()+"-"+mio.getSerial2()+"-"+mio.getSerial3();
	File f2 = null;
	String baseDir = "";
	
	//Check if ID already exists, add a ' if it does
	switch (type) {
	
		case (Types.GAME): baseDir = f.getParent()+"/game/"; break;
		case (Types.MANGA): baseDir = f.getParent()+"/manga/"; break;
		case (Types.RECORD): baseDir = f.getParent()+"/record/"; break;
	}
	SQLog.log(Level.INFO, "Moving file to "+baseDir+ID+".mio");
	f2 = new File(baseDir+ID+".mio");
	
	while(f2.exists() && !f2.isDirectory()) { 
	    ID = ID+"2";
	    SQLog.log(Level.INFO, "Name already exists, moving file to "+baseDir+ID+".mio");
	    f2 = new File(baseDir+ID+".mio");
	}
	
	//Once parsed, the file is moved to its appropriate directory.
	f.renameTo(f2);
	return ID;
	
}


@Override
public void contextInitialized(ServletContextEvent arg0) {
	
	try {
		Class.forName("org.sqlite.JDBC");
	} catch (ClassNotFoundException e1) {
		e1.printStackTrace();
	}
	System.out.println("DoujinSoft Database builder started");	
	
	// Hee to the ho and here we go
	Logger SQLog = Logger.getLogger("SQLite");
	SQLog.addHandler(new StreamHandler(System.out, new SimpleFormatter()));
	
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
      
      //Let's jam some .mios in this
      File[] files = new File(dataDir+"/mio/").listFiles();
      
      for (File f: files) {
          if (!f.isDirectory()) {
        	  
        	  SQLog.log(Level.INFO, "Parsing file "+f.getName());
        	  byte[] mioFile = FileByteOperations.read(f.getAbsolutePath());
        	  PreparedStatement insertQuery = null;
        	  String ID = "";
        	  
              //The file is game, manga or record, depending on its size.
        	  if (mioFile.length == Types.GAME) {
        		  SQLog.log(Level.INFO, "Detected as game.");
        		  GameEdit game = new GameEdit(mioFile);
        		  ID = computeMioID(f, game, Types.GAME);
        		  insertQuery = parseMioBase(game, ID, connection, Types.GAME);
        		  	  
        		  //Game-specific: add the preview picture	  
        		  insertQuery.setString(7, MioUtils.mapColorByte(game.getCartColor()));
        		  insertQuery.setString(8, MioUtils.mapColorByte(game.getLogoColor()));
        		  insertQuery.setInt(9, game.getLogo());
        		  insertQuery.setString(11, MioUtils.getBase64GamePreview(mioFile));
        		  
      	    	}
        	  
        	  if (mioFile.length == Types.MANGA) {
        		  SQLog.log(Level.INFO, "Detected as comic.");
        	      MangaEdit manga = new MangaEdit(mioFile);
        	      ID = computeMioID(f, manga, Types.MANGA);
        	      insertQuery = parseMioBase(manga, ID, connection, Types.MANGA);
        	      
        	      //Manga-specific: add the panels
        	      insertQuery.setString(7, MioUtils.mapColorByte(manga.getMangaColor()));
        		  insertQuery.setString(8, MioUtils.mapColorByte(manga.getLogoColor()));
        		  insertQuery.setInt(9, manga.getLogo());
        		  insertQuery.setString(11,MioUtils.getBase64Manga(mioFile, 0));
        		  insertQuery.setString(12,MioUtils.getBase64Manga(mioFile, 1));
        		  insertQuery.setString(13,MioUtils.getBase64Manga(mioFile, 2));
        		  insertQuery.setString(14,MioUtils.getBase64Manga(mioFile, 3));

        	    }
        	    
        	  if (mioFile.length == Types.RECORD) {
        		  SQLog.log(Level.INFO, "Detected as record.");
        	      RecordEdit record = new RecordEdit(mioFile);
        	      ID = computeMioID(f, record, Types.RECORD);
        	      insertQuery = parseMioBase(record, ID, connection, Types.RECORD);
        	      
        	      insertQuery.setString(7, MioUtils.mapColorByte(record.getRecordColor()));
        	      insertQuery.setString(8, MioUtils.mapColorByte(record.getLogoColor()));
        	      insertQuery.setInt(9, record.getLogo());
        		  
        	    }
        	  
        	  SQLog.log(Level.INFO, "Inserting into DB");
    		  
    		  insertQuery.executeUpdate();
    		  
        	  
          } 
      }
      
    }
    catch(SQLException e){
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
	// TODO Auto-generated method stub
	
}

	
}