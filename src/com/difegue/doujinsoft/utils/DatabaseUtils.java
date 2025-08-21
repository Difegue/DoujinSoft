package com.difegue.doujinsoft.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

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

    /**
     * Adds missing miohash data to Survey entries by searching matching tables.
     * 
     * @param dataDir The directory where the database is located.
     * @return A status message describing the operation results.
     */
    public static String addMissingMiohash(String dataDir) {
        StringBuilder output = new StringBuilder();
        int updatedCount = 0;
        
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dataDir + "/mioDatabase.sqlite")) {
            
            // Find all surveys with missing miohash
            PreparedStatement findMissing = connection.prepareStatement(
                "SELECT timestamp, type, name, friendcode FROM Surveys WHERE miohash IS NULL OR miohash = ''");
            
            ResultSet missingSurveys = findMissing.executeQuery();
            
            while (missingSurveys.next()) {
                long timestamp = missingSurveys.getLong("timestamp");
                int type = missingSurveys.getInt("type");
                String name = missingSurveys.getString("name");
                String friendcode = missingSurveys.getString("friendcode");
                
                // Determine table name based on type
                String tableName;
                switch (type) {
                    case 0: tableName = "Games"; break;
                    case 1: tableName = "Records"; break;
                    case 2: tableName = "Manga"; break;
                    default: 
                        output.append("Unknown type ").append(type).append(" for survey '").append(name).append("'\n");
                        continue;
                }
                
                // Search for matching entries in the corresponding table
                PreparedStatement findMatches = connection.prepareStatement(
                    "SELECT hash FROM " + tableName + " WHERE name LIKE ?");
                findMatches.setString(1, "%" + name + "%");
                
                ResultSet matches = findMatches.executeQuery();
                ArrayList<String> foundHashes = new ArrayList<>();
                
                while (matches.next()) {
                    foundHashes.add(matches.getString("hash"));
                }
                matches.close();
                findMatches.close();
                
                if (foundHashes.isEmpty()) {
                    output.append("No matches found for survey '").append(name).append("' in ").append(tableName).append("\n");
                    continue;
                }
                
                // If multiple matches, prioritize hashes that already exist in Surveys
                String selectedHash = null;
                if (foundHashes.size() > 1) {
                    // Build a parameterized query for checking existing hashes
                    StringBuilder placeholders = new StringBuilder();
                    for (int i = 0; i < foundHashes.size(); i++) {
                        if (i > 0) placeholders.append(",");
                        placeholders.append("?");
                    }
                    
                    PreparedStatement checkExisting = connection.prepareStatement(
                        "SELECT DISTINCT miohash FROM Surveys WHERE miohash IN (" + placeholders.toString() + ") AND miohash IS NOT NULL AND miohash != ''");
                    
                    for (int i = 0; i < foundHashes.size(); i++) {
                        checkExisting.setString(i + 1, foundHashes.get(i));
                    }
                    
                    ResultSet existingHashes = checkExisting.executeQuery();
                    if (existingHashes.next()) {
                        selectedHash = existingHashes.getString("miohash");
                        output.append("Using existing hash '").append(selectedHash).append("' for survey '").append(name).append("'\n");
                    }
                    existingHashes.close();
                    checkExisting.close();
                }
                
                // If no existing hash found, use the first match
                if (selectedHash == null) {
                    selectedHash = foundHashes.get(0);
                    output.append("Using first match hash '").append(selectedHash).append("' for survey '").append(name).append("'\n");
                }
                
                // Update the survey with the selected hash
                PreparedStatement updateSurvey = connection.prepareStatement(
                    "UPDATE Surveys SET miohash = ? WHERE timestamp = ? AND type = ? AND name = ? AND friendcode = ?");
                updateSurvey.setString(1, selectedHash);
                updateSurvey.setLong(2, timestamp);
                updateSurvey.setInt(3, type);
                updateSurvey.setString(4, name);
                updateSurvey.setString(5, friendcode);
                
                int updated = updateSurvey.executeUpdate();
                if (updated > 0) {
                    updatedCount++;
                }
                updateSurvey.close();
            }
            
            missingSurveys.close();
            findMissing.close();
            
            output.append("\nCompleted! Updated ").append(updatedCount).append(" survey entries with missing miohash data.");
            
        } catch (SQLException e) {
            output.append("Error: ").append(e.getMessage());
            e.printStackTrace();
        }
        
        return output.toString();
    }

}