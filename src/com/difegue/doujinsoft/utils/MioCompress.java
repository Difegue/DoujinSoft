package com.difegue.doujinsoft.utils;

import java.io.*;
import java.nio.file.Files;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class MioCompress {

    /**
     * Compress and uncompress .mio files with ZipEntry.
     */

    public static void compressMio(File orig, File dest, String desiredName) throws IOException {

        String zipFileName = dest.getAbsolutePath();

        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFileName));
        // Just put the base ID in the zip, no need for the zip suffix
        zos.putNextEntry(new ZipEntry(desiredName + ".mio"));

        byte[] bytes = Files.readAllBytes(orig.toPath());
        zos.write(bytes, 0, bytes.length);
        zos.closeEntry();
        zos.close();
    }

    public static File uncompressMio(File compressedMio) throws IOException {

        Logger logger = Logger.getLogger("Mio Unzip");
        String tDir = System.getProperty("java.io.tmpdir");

        // Uncompress given file
        ZipInputStream zis = new ZipInputStream(new FileInputStream(compressedMio.getAbsolutePath()));
        ZipEntry entry = zis.getNextEntry();
        byte[] buffer = new byte[1024];

        File uncompressedMio = null;
        while (entry != null) {

            String fileName = entry.getName();
            uncompressedMio = new File(tDir, fileName);
            if (uncompressedMio.exists()) {
                // You can get a race condition here if someone starts a download and a concurrent user starts one right afterwards.
                // User n°2 might get an incomplete .mio file. Big deal.
                break;
            }
            FileOutputStream fos = new FileOutputStream(uncompressedMio);
            logger.info("Uncompressing .mio to "+uncompressedMio.getAbsolutePath());

            int len;
            while ((len = zis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }

            fos.close();
            entry = zis.getNextEntry();
        }

        zis.closeEntry();
        zis.close();

        // Return away
        return uncompressedMio;
    }

}
