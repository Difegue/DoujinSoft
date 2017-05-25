package com.difegue.doujinsoft.templates;

/*
 * Instances of this class are created when gson parses a collection json specifier file.
 */
public class Collection {
	  public String id;
	  public String collection_name;
	  public String collection_color;
	  public String collection_icon;
	  public String background_pic;
	  public String collection_desc;
	  public String collection_desc2;
	  public String[] mios;
	  
	  //Returns a string containing the mio IDs for SQL queries.
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

}