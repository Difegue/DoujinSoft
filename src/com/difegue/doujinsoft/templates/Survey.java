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
				color = "green";
				type = Types.GAME;
				break;
			case 1:
				color = "pink";
				type = Types.RECORD;
				break;
			case 2:
				color = "blue";
				type = Types.MANGA;
				break;
		}

		name = result.getString("name");
		starCount = result.getInt("stars");
		
		comment = getComment(type, commentId);
	}
	
	public int starCount, type;
	public String name, comment, color;

	private String getComment(int type, int commentId) {
		switch (type) {
			case Types.GAME: switch (commentId) {
				case 0: return "Looked very professional!";
				case 5: return "Didn't see that coming!";
				case 1: return "I was so moved!";
				case 6: return "I want a sequel!";
				case 2: return "Funny!";
				case 7: return "Very original!";
				case 3: return "Cool graphics!";
				case 8: return "That was hard!";
				case 4: return "Great music!";
				case 9: return "You must have worked hard!";
			}; break;
			case Types.MANGA: switch (commentId) {
				case 0: return "Looked very professional!";
				case 5: return "That was surprising!";
				case 1: return "Very...moving!";
				case 6: return "I wanna read the next one!";
				case 2: return "Funny!";
				case 7: return "Unique!";
				case 3: return "Great art style!";
				case 8: return "Surreal...";
				case 4: return "Hilarious punch line!";
				case 9: return "You must have worked hard!";
			}; break;
			case Types.RECORD: switch (commentId) {
				case 0: return "Sounded very professional!";
				case 5: return "Wasn't expecting that!";
				case 1: return "I was so moved!";
				case 6: return "I wanna hear the next one!";
				case 2: return "Funny!";
				case 7: return "Unique!";
				case 3: return "Made me wanna sing!";
				case 8: return "Fun to play!";
				case 4: return "Can't wait to hear it again!";
				case 9: return "You must have worked hard!";
			}; break;
		}

		return "barf";
	}
}


