package com.difegue.doujinsoft.templates;

import java.sql.ResultSet;
import java.sql.SQLException;

/*
 * Class for representing MIO Records. There are no differences in metadata..for now.
 */
public class Record extends BaseMio{ 	
	
	public Record(ResultSet result) throws SQLException{
	
		super(result);
	
	}

}


