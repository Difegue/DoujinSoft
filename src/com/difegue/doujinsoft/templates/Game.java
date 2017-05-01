package com.difegue.doujinsoft.templates;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.difegue.doujinsoft.MioUtils;

/*
 * Class for representing MIO Games. Only difference from base metadata is the preview picture.
 */
public class Game extends BaseMio{ 	
	
	public Game(ResultSet result) throws SQLException{
	
		super(result);
		
		preview = result.getString("previewPic");

	}
	
	public String preview;
	
}


