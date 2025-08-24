package com.difegue.doujinsoft.test;

import com.difegue.doujinsoft.utils.MioUtils;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;

/**
 * Simple test class to verify HD thumbnail generation works
 */
public class HDThumbnailTest {
    
    public static void main(String[] args) {
        System.out.println("HD Thumbnail Test");
        System.out.println("=================");
        
        // Test with invalid data
        byte[] invalidData = new byte[100];
        String result = MioUtils.getHDGamePreview(invalidData);
        System.out.println("Invalid data test: " + (result == null ? "PASS (returned null)" : "FAIL"));
        
        // Test with wrong size data
        byte[] wrongSize = new byte[65535]; // One byte short
        result = MioUtils.getHDGamePreview(wrongSize);
        System.out.println("Wrong size test: " + (result == null ? "PASS (returned null)" : "FAIL"));
        
        // Test with right size but wrong magic
        byte[] wrongMagic = new byte[65536];
        result = MioUtils.getHDGamePreview(wrongMagic);
        System.out.println("Wrong magic test: " + (result == null ? "PASS (returned null)" : "FAIL"));
        
        // Try to create a minimal valid .mio structure for testing
        byte[] testMio = createMinimalValidMio();
        
        // Test the HD vs standard preview methods
        result = MioUtils.getGamePreview(testMio, false);
        System.out.println("Standard preview test: " + (result != null ? "PASS" : "FAIL"));
        
        result = MioUtils.getGamePreview(testMio, true);
        System.out.println("HD preview with fallback test: " + (result != null ? "PASS" : "FAIL"));
        
        result = MioUtils.getGamePreview(testMio);
        System.out.println("Default preview test: " + (result != null ? "PASS" : "FAIL"));
        
        result = MioUtils.getHDGamePreview(testMio);
        System.out.println("Minimal valid .mio test: " + (result != null ? "PASS (generated image)" : "FAIL (returned null)"));
        
        if (result != null) {
            try {
                // Save the result to a file for manual inspection
                String base64Data = result.replace("data:image/png;base64,", "");
                byte[] imageBytes = Base64.getDecoder().decode(base64Data);
                
                File outputFile = new File("/tmp/test_hd_thumbnail.png");
                try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                    fos.write(imageBytes);
                }
                System.out.println("Test image saved to: " + outputFile.getAbsolutePath());
                System.out.println("Image size: " + imageBytes.length + " bytes");
            } catch (Exception e) {
                System.out.println("Error saving test image: " + e.getMessage());
            }
        }
        
        System.out.println("Test completed.");
    }
    
    /**
     * Create a minimal valid .mio file structure for testing
     */
    private static byte[] createMinimalValidMio() {
        byte[] mio = new byte[65536];
        
        // Set magic value at offset 0x8
        String magic = "DSMIO_S";
        byte[] magicBytes = magic.getBytes();
        System.arraycopy(magicBytes, 0, mio, 0x8, magicBytes.length);
        
        // Initialize background data area with some test pattern
        for (int i = 0x100; i < 0x100 + 0x3000; i++) {
            mio[i] = (byte) ((i % 16) | ((i / 16) % 16) << 4);
        }
        
        // Set object order (all zeros for no objects)
        for (int i = 0xE5F6; i < 0xE5F6 + 15; i++) {
            mio[i] = 0;
        }
        
        return mio;
    }
}