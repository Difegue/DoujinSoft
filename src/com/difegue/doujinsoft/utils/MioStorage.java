package com.difegue.doujinsoft.utils;

import com.xperia64.diyedit.metadata.Metadata;
import net.jpountz.xxhash.StreamingXXHash32;
import net.jpountz.xxhash.XXHashFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MioStorage {

    public static int computeMioHash(byte[] data) throws IOException {
        XXHashFactory factory = XXHashFactory.fastestInstance();
        ByteArrayInputStream in = new ByteArrayInputStream(data);

        int seed = 0x9747b28c; // used to initialize the hash value
        StreamingXXHash32 hash32 = factory.newStreamingHash32(seed);
        byte[] buf = new byte[8192];
        for (;;) {
            int read = in.read(buf);
            if (read == -1) {
                break;
            }
            hash32.update(buf, 0, read);
        }
        return hash32.getValue();
    }

    /*
     * Craft ID from .mio metadata.
     */
    public static String computeMioID(File f, Metadata mio) {
        return mio.getSerial1() + "-" + mio.getSerial2() + "-" + mio.getSerial3();
    }

    /*
    Compress file, move to directory and delete initial file.
     */
    public static boolean consumeMio(File f, int hash, int type) {
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
        } finally {
            return false;
        }
    }
}
