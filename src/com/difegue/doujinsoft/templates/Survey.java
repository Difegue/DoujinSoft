package com.difegue.doujinsoft.templates;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.difegue.doujinsoft.utils.MioUtils.Types;

/*
 * Class for representing Survey results. 
 * Contains a method to transcribe comment IDs to their textual equivalent.
 */
public class Survey { 	
	
	public Survey(ResultSet result) throws SQLException{
	
		int commentId = result.getInt("commentId");
		
		switch (result.getInt("type")) {
			case 0:
				type = Types.GAME;
			case 1:
				type = Types.MANGA;
			case 2:
				type = Types.RECORD;
		}

		name = result.getString("name");
		starCount = result.getInt("stars");
		
		comment = getComment(type, commentId);
	}
	
	public int type, starCount;
	public String name, comment;

	private String getComment(int type, int commentId) {
		return "barf";
	}
}


