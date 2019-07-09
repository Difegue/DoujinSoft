package com.difegue.doujinsoft.templates;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/*
 * Class for representing MIO comics. Only difference from base metadata are the base64 encoded pages.
 */
public class Manga extends BaseMio { 	
	
	public Manga(ResultSet result) throws SQLException{
	
		super(result);

		pages = new ArrayList<String>();
		
		pages.add(result.getString("frame0"));
		pages.add(result.getString("frame1"));
		pages.add(result.getString("frame2"));
		pages.add(result.getString("frame3"));
		
	}
	
	public ArrayList<String> pages;
	
}


