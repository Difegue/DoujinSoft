package com.difegue.doujinsoft.templates;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.difegue.doujinsoft.MioUtils;

/*
 * Base class used for binding with pebble templates.
 */
public class BaseMio { 	
	
	public BaseMio(ResultSet result) throws SQLException{
	
		//Compute timestamp 
    	timestamp = MioUtils.getTimeString(result.getInt("timeStamp"));
	
    	String desc = result.getString("description");
    	colorLogo = result.getString("colorLogo");
    	
    	//Special case to make black logos readable on the user interface
    	if (colorLogo.equals("grey darken-4"))
    		colorLogo = "grey";
    	
    	name = result.getString("name");
    	mioID = result.getString("id");
		brand = result.getString("brand");
		creator = result.getString("creator");
		if (desc.length() > 18) {
			mioDesc1 = desc.substring(0,18);
			mioDesc2 = desc.substring(18);
		}
		else {
			mioDesc1 = desc;
			mioDesc2 = "";
		}
		
		if (mioDesc1.equals(""))
			mioDesc1 = "No Description.";
		
		colorCart = result.getString("color");
		logo = result.getInt("logo");
	
	}
	
	public String name, timestamp, mioID, brand, creator, mioDesc1, mioDesc2, colorLogo, colorCart;
	public int logo;
	
}


