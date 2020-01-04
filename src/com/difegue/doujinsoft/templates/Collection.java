package com.difegue.doujinsoft.templates;

import com.difegue.doujinsoft.utils.MioUtils.Types;

import java.util.Arrays;

/*
 * Instances of this class are created when gson parses a collection json specifier file.
 */
public class Collection {
	  public String id;
	  public String collection_type;
	  public String collection_name;
	  public String collection_color;
	  public String collection_icon;
	  public String background_pic;
	  public String collection_desc;
	  public String collection_desc2;
	  public String[] mios;
	  
	  //Returns a string containing the mio hashes for SQL queries.
	  //Format of string is ("id1", "id2", "id3"...)
	  public String getMioSQL() {
		  
		  String query = "(";
				    
	    //ID lookup is done here
	    for(String mio : mios)
	    	query+="\""+mio+"\",";
				    
	    query = query.substring(0, query.length()-1);
	    query+=")";
	    
	    return query;
	  }

	  public int getType() {
		  
		if (collection_type != null) 
			switch (collection_type) {
				case "game": return Types.GAME;
				case "manga": return Types.MANGA;
				case "record": return Types.RECORD;
			}

		//Default to games
		return Types.GAME;
	  }

	  public void addMioHash(String hash) {
		  if (mios == null)
		  	mios = new String[0];

		  String[] newMios = Arrays.copyOf(mios, mios.length + 1);
		  newMios[mios.length] = hash;
		  mios = newMios;
	  }

}