package com.difegue.doujinsoft.templates;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.difegue.doujinsoft.utils.MioUtils;

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
    	hash = result.getString("hash");
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

		if (name.replaceAll("\\s+","").equals(""))
			name = "No Title";

		if (mioDesc1.replaceAll("\\s+","").equals(""))
			mioDesc1 = "No Description.";
		

		if (this.mioID.contains("them"))
			specialBrand = "theme";
		if (this.mioID.contains("wari"))
			specialBrand = "wario";
		if (this.mioID.contains("nint"))
			specialBrand = "nintendo";
		
		colorCart = result.getString("color");
		logo = result.getInt("logo");
	
	}
	
	public String name, timestamp, mioID, hash, brand, creator, mioDesc1, mioDesc2, colorLogo, colorCart, specialBrand;
	public int logo;
	
}


