package com.difegue.doujinsoft.utils;

import java.io.*;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class MioCompress {

    /**
     * Compress and uncompress .mio files with ZipEntry.
     */

    public static void compressMio(File orig, File dest) throws IOException {

        String zipFileName = dest.getName();

        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFileName));
        // Just put the base ID in the zip, no need for the zip suffix
        zos.putNextEntry(new ZipEntry(orig.getName() + ".mio"));

        byte[] bytes = Files.readAllBytes(orig.toPath());
        zos.write(bytes, 0, bytes.length);
        zos.closeEntry();
        zos.close();
    }

    public static File uncompressMio(File compressedMio) throws IOException {

        // Create a temporary file
        File uncompressedMio = File.createTempFile(compressedMio.getName(), ".mio");

        // Uncompress given file
        ZipInputStream zis = new ZipInputStream(new FileInputStream(compressedMio.getAbsolutePath()));
        ZipEntry ze = zis.getNextEntry();
        byte[] buffer = new byte[1024];

        while (ze != null) {

            String fileName = ze.getName();
            if (!fileName.equals(compressedMio.getName()))
                continue;

            FileOutputStream fos = new FileOutputStream(uncompressedMio);

            int len;
            while ((len = zis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }

            fos.close();
            ze = zis.getNextEntry();
        }

        zis.closeEntry();
        zis.close();

        // Return away
        return uncompressedMio;
    }

}
