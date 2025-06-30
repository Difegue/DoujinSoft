package com.difegue.doujinsoft.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DatabaseUtils {

    /**
     * Checks whether the given Wii FC is already saved in the database.
     * 
     * @param dataDir The directory where the database is located.
     * @param code    The Wii FC to check.
     * @return true if the Wii FC is already saved, false otherwise.
     */
    public static boolean isFriendCodeSaved(String dataDir, String code) {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dataDir + "/mioDatabase.sqlite")) {

            PreparedStatement ret = connection.prepareStatement("select COUNT(*) from Friends WHERE friendcode = ?");
            ret.setString(1, code);

            ResultSet result = ret.executeQuery();
            int res = result.getInt(1);
            return (res == 1);
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Saves a survey answer to the database.
     * 
     * @param dataDir The directory where the database is located.
     * @param sender  The sender's unique ID. (Wii FC or other)
     * @param type    The type of survey.
     * @param title   The title of the rated MIO.
     * @param stars   The number of stars given in the survey.
     * @param comment The comment ID.
     * @param miohash The hash of the MIO file (optional).
     * @return true if the survey answer was saved successfully, false otherwise.
     */
    public static boolean saveSurveyAnswer(String dataDir, String sender, int type, String title, int stars,
            int comment, String miohash) {

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dataDir + "/mioDatabase.sqlite")) {

            // Has the user already answered a survey for this mio?
            PreparedStatement check = connection.prepareStatement(
                    "SELECT COUNT(*) FROM Surveys WHERE friendcode = ? AND type = ? AND name = ?");
            check.setString(1, sender);
            check.setInt(2, type);
            check.setString(3, title);
            ResultSet result = check.executeQuery();
            int res = result.getInt(1);
            if (res > 0) {
                // User has already answered this survey, do not save again
                connection.close();
                return false;
            }

            PreparedStatement ret = connection.prepareStatement("INSERT INTO Surveys VALUES (?,?,?,?,?,?,?)");
            ret.setLong(1, System.currentTimeMillis());
            ret.setInt(2, type);
            ret.setString(3, title);
            ret.setInt(4, stars);
            ret.setInt(5, comment);
            ret.setString(6, sender);
            ret.setString(7, miohash);

            ret.executeUpdate();

            ret.close();
            connection.close();
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

}