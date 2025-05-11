package com.difegue.doujinsoft.utils;

import java.util.logging.Level;
import java.util.logging.Logger;

import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Base64;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.time.*;
import java.time.format.DateTimeFormatter;

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
    public static boolean saveSurveyAnswer(String dataDir, String sender, byte type, String title, byte stars, byte comment, String miohash) {

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dataDir + "/mioDatabase.sqlite")) {

            PreparedStatement ret = connection.prepareStatement("INSERT INTO Surveys VALUES (?,?,?,?,?,?,?)");
            ret.setLong(1, System.currentTimeMillis());
            ret.setInt(2, type & 0xFF);
            ret.setString(3, title);
            ret.setInt(4, stars & 0xFF);
            ret.setInt(5, comment & 0xFF);
            ret.setString(6, sender);
            ret.setString(7, miohash); 

            ret.executeUpdate();

            ret.close();
            connection.close();
            return true;

        } catch (SQLException e) {
            return false;
        }
    }



}