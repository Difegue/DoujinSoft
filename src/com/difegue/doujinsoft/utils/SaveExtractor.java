package com.difegue.doujinsoft.utils;

import com.xperia64.diyedit.saveutils.SaveHandler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Pulls the .mio files out of a WarioWare: DIY (DS) or DIY Showcase (Wii) save file.
 */
public class SaveExtractor {

    // Save sizes as recognized by DIYEdit's SaveHandler.
    private static final int[] DS_SIZES = { 16777216, 33554432, 33566720, 33554554 };
    private static final int WII_GAMES = 4719808, WII_RECORDS = 591040, WII_MANGA = 1033408;
    private static final int WII_COMPRESSED = 6438592; // data.bin

    /** Largest save we accept, a bit above the biggest DS dump. */
    public static final int MAX_SAVE_SIZE = 36 * 1024 * 1024;

    public static boolean looksLikeSave(byte[] data, String fileName) {
        if (fileName.toLowerCase().endsWith(".dsv"))
            return true;
        for (int size : DS_SIZES)
            if (data.length == size)
                return true;
        return data.length == WII_GAMES || data.length == WII_RECORDS
                || data.length == WII_MANGA || data.length == WII_COMPRESSED;
    }

    /**
     * Returns every non-empty mio in the save. Nothing is validated here: callers get raw
     * slots and should run the same checks they run on a directly-uploaded .mio.
     * SaveHandler only reads from disk, so the save goes through a temp directory.
     */
    public static List<byte[]> extract(byte[] data, String fileName) throws IOException {
        Path dir = Files.createTempDirectory("savedump_");
        try {
            // Keep the extension: SaveHandler treats .dsv specially.
            String ext = fileName.toLowerCase().endsWith(".dsv") ? ".dsv" : ".sav";
            Path save = dir.resolve("save" + ext);
            Files.write(save, data);
            SaveHandler handler = new SaveHandler(save.toString());

            // Raw Wii saves hold a single kind of mio (GDATA/RDATA/MDATA): the slot table is
            // the same, so reading the wrong mode would return garbage.
            int[] modes = { 0, 1, 2 };
            if (data.length == WII_GAMES) modes = new int[] { 0 };
            if (data.length == WII_RECORDS) modes = new int[] { 1 };
            if (data.length == WII_MANGA) modes = new int[] { 2 };

            List<byte[]> mios = new ArrayList<>();
            for (int mode : modes) {
                List<byte[]> slots = handler.getMios(mode);
                if (slots == null)
                    continue;
                for (byte[] mio : slots)
                    if (mio != null)
                        mios.add(mio);
            }
            return mios;
        } finally {
            try (Stream<Path> files = Files.walk(dir)) {
                files.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
            }
        }
    }
}
