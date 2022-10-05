package com.difegue.doujinsoft.utils;

import com.xperia64.diyedit.FileByteOperations;
import com.xperia64.diyedit.editors.GameEdit;
import com.xperia64.diyedit.editors.MangaEdit;
import com.xperia64.diyedit.editors.RecordEdit;
import com.xperia64.diyedit.metadata.Metadata;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Deals with .mio files and their positioning in the database.
 */
public class MioStorage {

    public static String computeMioHash(byte[] data) {

        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.update(data);
            byte[] digest = messageDigest.digest();

            return convertByteToHex(digest);
        } catch (NoSuchAlgorithmException e) {
            return "";
        }

    }

    private static String convertByteToHex(byte[] byteData) {

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < byteData.length; i++) {
            sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
        }

        return sb.toString();
    }

    public static void ScanForNewMioFiles(String dataDir, Logger logger) throws SQLException {
        File[] files = new File(dataDir + "/mio/").listFiles();

        for (File f : files) {
            if (!f.isDirectory()) {
                try (Connection connection = DriverManager
                        .getConnection("jdbc:sqlite:" + dataDir + "/mioDatabase.sqlite")) {
                    byte[] mioData = FileByteOperations.read(f.getAbsolutePath());
                    Metadata metadata = new Metadata(mioData);
                    String hash = MioStorage.computeMioHash(mioData);
                    String ID = MioUtils.computeMioID(metadata);
                    String creatorID = MioUtils.computeCreatorID(metadata);
                    int type = mioData.length;
                    PreparedStatement insertQuery = parseMioBase(metadata, hash, ID, creatorID, connection, type);

                    // The file is game, manga or record, depending on its size.
                    if (mioData.length == MioUtils.Types.GAME) {
                        GameEdit game = new GameEdit(mioData);

                        // Game-specific: add the preview picture and isNsfw flag.
                        // CreatorID is added here as it was put in later - ditto for Manga/Records.
                        insertQuery.setString(9, MioUtils.mapColorByte(game.getCartColor()));
                        insertQuery.setString(10, MioUtils.mapColorByte(game.getLogoColor()));
                        insertQuery.setInt(11, game.getLogo());
                        insertQuery.setString(12, MioUtils.getBase64GamePreview(mioData));
                        insertQuery.setString(13, creatorID);
                        insertQuery.setBoolean(14, false);

                        logger.log(Level.INFO, "Game;" + hash + ";" + ID + ";" + game.getName() + "\n");
                    }

                    if (mioData.length == MioUtils.Types.MANGA) {
                        MangaEdit manga = new MangaEdit(mioData);

                        // Manga-specific: add the panels
                        insertQuery.setString(9, MioUtils.mapColorByte(manga.getMangaColor()));
                        insertQuery.setString(10, MioUtils.mapColorByte(manga.getLogoColor()));
                        insertQuery.setInt(11, manga.getLogo());
                        insertQuery.setString(12, MioUtils.getBase64Manga(mioData, 0));
                        insertQuery.setString(13, MioUtils.getBase64Manga(mioData, 1));
                        insertQuery.setString(14, MioUtils.getBase64Manga(mioData, 2));
                        insertQuery.setString(15, MioUtils.getBase64Manga(mioData, 3));
                        insertQuery.setString(16, creatorID);

                        logger.log(Level.INFO, "Manga;" + hash + ";" + ID + ";" + manga.getName() + "\n");
                    }

                    if (mioData.length == MioUtils.Types.RECORD) {
                        RecordEdit record = new RecordEdit(mioData);

                        insertQuery.setString(9, MioUtils.mapColorByte(record.getRecordColor()));
                        insertQuery.setString(10, MioUtils.mapColorByte(record.getLogoColor()));
                        insertQuery.setInt(11, record.getLogo());
                        insertQuery.setString(12, creatorID);

                        logger.log(Level.INFO, "Record;" + hash + ";" + ID + ";" + record.getName() + "\n");
                    }

                    logger.log(Level.INFO, "Inserting into DB");

                    insertQuery.executeUpdate();
                    insertQuery.close();
                    consumeMio(f, hash, type);
                } catch (SQLException e) {
                    logger.log(Level.SEVERE,
                            "Couldn't insert this mio in the database - Likely a duplicate file, moving on.");
                    new File(dataDir + "/duplicates").mkdirs();
                    File target = new File(dataDir + "/duplicates/" + f.getName());
                    if (target.exists())
                        target.delete();
                    f.renameTo(target);
                    logger.log(Level.SEVERE, e.getMessage());
                }
            }
        }
    }

    /*
     * Standard parsing for every .mio file - Returns the first values of the final
     * SQL Statement.
     */
    private static PreparedStatement parseMioBase(Metadata mio, String hash, String ID, String creatorID, Connection co,
            int type)
            throws SQLException {

        PreparedStatement ret = null;
        String query = "";

        switch (type) {
            case MioUtils.Types.GAME:
                query = "INSERT INTO Games VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
                break;
            case MioUtils.Types.MANGA:
                query = "INSERT INTO Manga VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
                break;
            case MioUtils.Types.RECORD:
                query = "INSERT INTO Records VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
                break;
        }

        ret = co.prepareStatement(query);

        String normalizedName = mio.getName().replaceAll("\\p{Punct}", "z");

        ret.setString(1, hash);
        ret.setString(2, ID);
        ret.setString(3, mio.getName());
        ret.setString(4, normalizedName);
        ret.setString(5, mio.getCreator());
        ret.setString(6, mio.getBrand());
        ret.setString(7, mio.getDescription());

        int timestamp = mio.getTimestamp();
        // If the timestamp is larger than today's date, set it to today's date
        if (MioUtils.DIY_TIMESTAMP_ORIGIN.plusDays(timestamp).toLocalDate().isAfter(LocalDate.now()))
            timestamp = (int) MioUtils.DIY_TIMESTAMP_ORIGIN.until(ZonedDateTime.now(), ChronoUnit.DAYS);

        ret.setInt(8, timestamp);

        return ret;
    }

    /*
     * Compress file, move to directory and delete initial file.
     */
    private static boolean consumeMio(File f, String hash, int type) {
        Logger SQLog = Logger.getLogger("SQLite");
        String baseDir = "";

        switch (type) {
            case (MioUtils.Types.GAME):
                baseDir = f.getParent() + "/game/";
                break;
            case (MioUtils.Types.MANGA):
                baseDir = f.getParent() + "/manga/";
                break;
            case (MioUtils.Types.RECORD):
                baseDir = f.getParent() + "/record/";
                break;
        }

        // Create directories if they don't exist
        if (!new File(baseDir).exists())
            new File(baseDir).mkdirs();

        SQLog.log(Level.INFO, "Moving file to " + baseDir + hash + ".miozip");
        File f2 = new File(baseDir + hash + ".miozip");

        try {

            if (f2.exists())
                throw new Exception("Destination miozip already exists! How could this happen?");

            MioCompress.compressMio(f, f2, f.getName());
            // Only delete the initial .mio if the zipped variant has been properly
            // processed
            if (f2.exists())
                f.delete();

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
