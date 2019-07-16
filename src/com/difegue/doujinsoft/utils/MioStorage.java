package com.difegue.doujinsoft.utils;

import com.xperia64.diyedit.metadata.Metadata;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    /*
     * Craft ID from .mio metadata.
     */
    public static String computeMioID(Metadata mio) {
        return mio.getSerial1() + "-" + mio.getSerial2() + "-" + mio.getSerial3();
    }

    /*
    Compress file, move to directory and delete initial file.
     */
    public static boolean consumeMio(File f, String hash, int type) {
        Logger SQLog = Logger.getLogger("SQLite");
        String baseDir = "";

        switch (type) {
            case (MioUtils.Types.GAME): baseDir = f.getParent()+"/game/"; break;
            case (MioUtils.Types.MANGA): baseDir = f.getParent()+"/manga/"; break;
            case (MioUtils.Types.RECORD): baseDir = f.getParent()+"/record/"; break;
        }

        //Create directories if they don't exist
        if (!new File (baseDir).exists())
            new File(baseDir).mkdirs();

        SQLog.log(Level.INFO, "Moving file to " + baseDir + hash + ".miozip");
        File f2 = new File(baseDir + hash + ".miozip");

        try {
            MioCompress.compressMio(f, f2, f.getName());
            // Only delete the initial .mio if the zipped variant has been properly processed
            if (f2.exists())
                f.delete();

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } 
    }
}
