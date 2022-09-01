package com.difegue.doujinsoft.templates;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.difegue.doujinsoft.utils.MioStorage;
import com.difegue.doujinsoft.utils.MioUtils;
import com.xperia64.diyedit.metadata.Metadata;

/*
 * Base class used for binding with pebble templates.
 */
public class BaseMio {

	/**
	 * Basic constructor from a generic mio Metadata object.
	 * @param m
	 */
	public BaseMio(Metadata m) {

		name = m.getName();
		hash = MioStorage.computeMioHash(m.file);
		creator = m.getCreator();
		brand = m.getBrand();
		timestamp = MioUtils.getTimeString(m.getTimestamp());

		int type = m.file.length;
		switch (type) {
			case MioUtils.Types.GAME: mioID = "G-"; break;
			case MioUtils.Types.MANGA: mioID = "M-"; break;
			case MioUtils.Types.RECORD: mioID = "R-"; break;
		}

		mioID += MioStorage.computeMioID(m);
		creatorID = MioStorage.computeCreatorID(m);

		if (m.getDescription().length() > 19) {
			mioDesc1 = m.getDescription().substring(0,18);
			mioDesc2 = m.getDescription().substring(18);
		} else {
			mioDesc1 = m.getDescription();
		}
		
	}

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
		creatorID = result.getString("creatorId");

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
	
	public String name, timestamp, mioID, hash, brand, creator, mioDesc1, mioDesc2, colorLogo, colorCart, specialBrand, creatorID;
	public int logo;
	
}


