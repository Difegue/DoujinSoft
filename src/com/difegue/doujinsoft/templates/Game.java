package com.difegue.doujinsoft.templates;

import java.sql.ResultSet;
import java.sql.SQLException;

/*
 * Class for representing MIO Games. Only difference from base metadata is the preview picture.
 */
public class Game extends BaseMio {

	public Game(ResultSet result) throws SQLException {

		super(result);

		preview = result.getString("previewPic");
		isNsfw = result.getBoolean("isNsfw");
	}

	public String preview;
	public Boolean isNsfw;
}
