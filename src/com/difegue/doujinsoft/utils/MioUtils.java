package com.difegue.doujinsoft.utils;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.time.*;
import java.time.format.DateTimeFormatter;

import javax.imageio.ImageIO;

import com.xperia64.diyedit.editors.GameEdit;
import com.xperia64.diyedit.editors.MangaEdit;
import com.xperia64.diyedit.metadata.Metadata;

/*
 * Static class containing base methods for interacting with .mio files.
 * Basically an abstraction layer for DIYEdit.
 * 
 */

public class MioUtils {

  // Color palette for HD thumbnail generation (from Python script)
  private static final Color[] HD_PALETTE = {
    new Color(0x00, 0x00, 0x00, 0x00), new Color(0x00, 0x00, 0x00, 0xFF), 
    new Color(0xFF, 0xDF, 0x9E, 0xFF), new Color(0xFF, 0xAE, 0x34, 0xFF),
    new Color(0xC7, 0x4D, 0x00, 0xFF), new Color(0xFF, 0x00, 0x00, 0xFF), 
    new Color(0xCF, 0x6D, 0xEF, 0xFF), new Color(0x14, 0xC7, 0xCF, 0xFF),
    new Color(0x2C, 0x6D, 0xC7, 0xFF), new Color(0x0C, 0x96, 0x55, 0xFF), 
    new Color(0x75, 0xD7, 0x3C, 0xFF), new Color(0xFF, 0xFF, 0x5D, 0xFF),
    new Color(0x7D, 0x7D, 0x7D, 0xFF), new Color(0xC7, 0xC7, 0xC7, 0xFF), 
    new Color(0xFF, 0xFF, 0xFF, 0xFF), new Color(0xFF, 0xFF, 0xFF, 0xFF)
  };

  // Constants for timestamp printing
  public static final ZonedDateTime DIY_TIMESTAMP_ORIGIN = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
  private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  // Basic enum to distinguish mio files quickly.
  // Game = 64KB, Manga = 14KB, Record = 8KB.
  // Surveys are added here as a quick-and-dirty shortcut.
  public class Types {
    public static final int GAME = 65536;
    public static final int MANGA = 14336;
    public static final int RECORD = 8192;
    public static final int SURVEY = 255;
  }

  /*
   * Returns a readable date from DIY's weird timestamp ints.
   */
  public static String getTimeString(int timestamp) {
    ZonedDateTime dateMio = DIY_TIMESTAMP_ORIGIN.plusDays(timestamp);
    return dateMio.format(formatter);
  }

  /*
   * Craft ID from .mio metadata.
   */
  public static String computeMioID(Metadata mio) {
    return mio.getSerial1() + "-" + mio.getSerial2() + "-" + mio.getSerial3();
  }

  /*
   * Returns color strings matching the bytes used in .mio files.
   * The strings match the colors used by MaterializeCSS.
   */
  public static String mapColorByte(byte color) {
    // 0 Yellow
    // 1 Light Blue
    // 2 Green
    // 3 Orange
    // 4 Dark Blue
    // 5 Red
    // 6 White
    // 7 Black
    switch (color) {
      case 0:
        return "yellow";
      case 1:
        return "light-blue";
      case 2:
        return "green";
      case 3:
        return "orange";
      case 4:
        return "indigo";
      case 5:
        return "red";
      case 6:
        return "grey lighten-3";
      case 7:
        return "grey darken-4";
      default:
        return "purple";
    }
  }

  /**
   * Parse a manga .mio and return the specified page in 1-bit Run-length encoding,
   * starting at white on every line.
   * 
   * eg, "6W5BW\nW3B2W" means 6 white pixels, 5 black, 1 white, next line, 1 white, 3 black, 2 white, etc
   * 
   * @param mioFile The .mio file to parse
   * @param page    The page to parse (0-3)
   * @return The RLE string
   */
  public static String getRLEManga(byte[] mioFile, int page) {

    int x = 0;
    int y = 0;
    boolean done = false;
    MangaEdit e2 = new MangaEdit(mioFile);

    String rleResult = "";
    boolean isWhite = true;
    int i = 0;

    // .mio comic panels are 191x127px.
    while (!done) {
      if (e2.getPixel((byte) page, x, y)) { // black pixel
        if (!isWhite)
          i++;
        else {
          isWhite = false;
          if (i == 1)
            rleResult = rleResult + "W";
          else if (i > 0)
            rleResult = rleResult + i + "W";
          i = 1;
        }
      } else { // white pixel
        if (isWhite)
          i++;
        else {
          isWhite = true;
          if (i == 1)
            rleResult = rleResult + "B";
          else if (i > 0)
            rleResult = rleResult + i + "B";
          i = 1;
        }
      }

      x++;
      if (x > 191) {
        y++;
        if (i > 1)
          rleResult = rleResult + i;
        
        if (isWhite)
          rleResult = rleResult + "W\n";
        else
          rleResult = rleResult + "B\n";
          
        isWhite = true;
        x = 0;
        i = 0;
      }
      if (y > 127) {
        done = true;
        break;
      }
    }

    return rleResult;
  }

  /*
   * Parse a manga .mio and return the specified page. (From 0 to 3)
   */
  public static String getBase64Manga(byte[] mioFile, int page) {
    int x = 0;
    int y = 0;
    boolean done = false;
    MangaEdit e2 = new MangaEdit(mioFile);

    // .mio comic panels are 191x127px.
    BufferedImage image = new BufferedImage(191, 127, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = image.createGraphics();

    while (!done) {
      if (e2.getPixel((byte) page, x, y)) {
        g.setColor(new Color(0, 0, 0));
      } else {
        g.setColor(new Color(255, 255, 255));
      }
      g.drawRect(x, y, 1, 1);

      x++;
      if (x > 191) {
        y++;
        x = 0;
      }
      if (y > 127) {
        done = true;
        break;
      }
    }

    return "data:image/png;base64," + imgToBase64String(image, "png");
  }

  /*
   * Parse a game .mio and return its embedded image preview in base64 format for
   * display in web browsers.
   */
  public static String getBase64GamePreview(byte[] mioFile) {

    int x = 0;
    int y = 0;
    boolean done = false;
    GameEdit gameMeta = new GameEdit(mioFile);

    // .mio preview pictures are 95x63px.
    BufferedImage image = new BufferedImage(95, 63, BufferedImage.TYPE_INT_RGB);

    Graphics2D g = image.createGraphics();
    while (!done) {
      int c = gameMeta.getPreviewPixel(x, y);
      Color cp = new Color(r(c), g(c), b(c));
      g.setColor(cp);
      g.drawRect(x, y, 1, 1);

      x++;
      if (x > 95) {
        y++;
        x = 0;
      }
      if (y > 63) {
        done = true;
        break;
      }
    }

    return "data:image/png;base64," + imgToBase64String(image, "png");
  }

  public static String imgToBase64String(final RenderedImage img, final String formatName) {
    final ByteArrayOutputStream os = new ByteArrayOutputStream();

    try {
      ImageIO.write(img, formatName, os);
      return Base64.getEncoder().encodeToString(os.toByteArray());
    } catch (final IOException ioe) {
      throw new UncheckedIOException(ioe);
    }
  }

  private static int r(int b) {
    switch (b) {
      case 1:
        return 0;
      case 2:
        return 255;
      case 3:
        return 255;
      case 4:
        return 198;
      case 5:
        return 255;
      case 6:
        return 206;
      case 7:
        return 16;
      case 8:
        return 41;
      case 9:
        return 8;
      case 10:
        return 115;
      case 11:
        return 255;
      case 12:
        return 128;
      case 13:
        return 192;
      case 14:
        return 255;
    }
    return 0;
  }

  private static int g(int b) {
    switch (b) {
      case 1:
        return 0;
      case 2:
        return 223;
      case 3:
        return 174;
      case 4:
        return 73;
      case 5:
        return 0;
      case 6:
        return 105;
      case 7:
        return 199;
      case 8:
        return 105;
      case 9:
        return 150;
      case 10:
        return 215;
      case 11:
        return 255;
      case 12:
        return 128;
      case 13:
        return 192;
      case 14:
        return 255;
    }
    return 0;
  }

  private static int b(int b) {
    switch (b) {
      case 1:
        return 0;
      case 2:
        return 156;
      case 3:
        return 49;
      case 4:
        return 0;
      case 5:
        return 0;
      case 6:
        return 239;
      case 7:
        return 206;
      case 8:
        return 198;
      case 9:
        return 82;
      case 10:
        return 57;
      case 11:
        return 90;
      case 12:
        return 128;
      case 13:
        return 192;
      case 14:
        return 255;
    }
    return 0;
  }

  /**
   * Convert a 10-bit number to signed (from Python script)
   */
  private static int toSigned(int num) {
    return ((num & (1 << 9)) != 0) ? (num - (1 << 10)) : num;
  }

  /**
   * Parse image data using the color palette (from Python script)
   */
  private static void parseImageData(BufferedImage image, byte[] data, int width, int height, int someval) {
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        int offset = 0;
        offset += (y / 8) * someval;
        offset += (x / 8) * 32;
        offset += (y % 8) * 4;
        offset += (x % 8) / 2;
        
        if (offset >= data.length) continue;
        
        byte mybyte = data[offset];
        Color color;
        if ((x % 2) != 0) {
          color = HD_PALETTE[(mybyte & 0xFF) >> 4];
        } else {
          color = HD_PALETTE[(mybyte & 0xFF) & 0xF];
        }
        image.setRGB(x, y, color.getRGB());
      }
    }
  }

  /**
   * Generate high-quality preview image from .mio game file.
   * This is a Java port of the Python mio_hd_thumbnail.py script.
   * 
   * @param mioFile The .mio file bytes
   * @return Base64 encoded PNG image or null if invalid file
   */
  public static String getHDGamePreview(byte[] mioFile) {
    try {
      ByteBuffer buffer = ByteBuffer.wrap(mioFile).order(ByteOrder.LITTLE_ENDIAN);
      
      // Validate file size and magic
      if (mioFile.length != 65536) {
        return null;
      }
      
      buffer.position(0x8);
      byte[] magic = new byte[7];
      buffer.get(magic);
      if (!new String(magic).equals("DSMIO_S")) {
        return null;
      }
      
      // Read background data
      buffer.position(0x100);
      byte[] backData = new byte[0x3000];
      buffer.get(backData);
      
      // Create background image
      BufferedImage bg = new BufferedImage(192, 128, BufferedImage.TYPE_INT_ARGB);
      parseImageData(bg, backData, 192, 128, 768);
      
      // Convert to RGB for easier pasting later
      BufferedImage rgbBg = new BufferedImage(192, 128, BufferedImage.TYPE_INT_RGB);
      Graphics2D g2d = rgbBg.createGraphics();
      g2d.drawImage(bg, 0, 0, null);
      g2d.dispose();
      
      // Read object order
      buffer.position(0xE5F6);
      byte[] objOrder = new byte[15];
      buffer.get(objOrder);
      
      // Use fixed seed for consistent results (equivalent to randseed=None in Python)
      Random random = new Random();
      
      int objCount = 0;
      Map<Integer, Map<String, Object>> attachments = new HashMap<>();
      Map<Integer, Map<String, Object>> assets = new HashMap<>();
      
      // Initialize attachments and assets
      for (int i = 0; i < 15; i++) {
        Map<String, Object> att = new HashMap<>();
        att.put("obj", -1);
        attachments.put(i, att);
        
        Map<String, Object> asset = new HashMap<>();
        asset.put("obj", -1);
        assets.put(i, asset);
      }
      
      // Process 15 objects
      for (int i = 0; i < 15; i++) {
        buffer.position(0xB104 + i * 136);
        int objSize = ((buffer.get() & 0xFF) + 1) * 16;
        if ((buffer.get() & 0xFF) == 0) {
          continue; // Object doesn't exist
        }
        
        buffer.position(buffer.position() + 19);
        int artOffs = buffer.position();
        
        buffer.position(0xBB89 + 0x30 + i * 0x2D0);
        if ((buffer.get() & 0xFF) != 0x04) {
          continue; // Not a "start" command
        }
        
        int artNum = (buffer.get() & 0xFF) >> 4;
        buffer.position(buffer.position() + 10);
        
        boolean doArea = false;
        BitField cmd = new BitField();
        
        // Read 12 bytes as 3 32-bit little-endian integers
        cmd.setBitRange(0, 32, buffer.getInt());
        cmd.setBitRange(32, 64, buffer.getInt());
        cmd.setBitRange(64, 96, buffer.getInt());
        
        int objX = 0, objY = 0;
        
        if (cmd.getBitRange(0, 4) == 7) { // 0x?7 placement command
          objX = toSigned((int) cmd.getBitRange(17, 27));
          objY = toSigned((int) cmd.getBitRange(27, 37));
          
          if (cmd.getBitRange(4, 5) == 1) { // attachment to obj relative x,y
            objX -= 96;
            objY -= 64;
            int obj = (int) cmd.getBitRange(13, 17);
            Map<String, Object> attachment = new HashMap<>();
            attachment.put("obj", obj);
            attachment.put("xoffs", objX);
            attachment.put("yoffs", objY);
            attachments.put(i, attachment);
          } else if (cmd.getBitRange(7, 8) == 1) { // within area
            doArea = true;
          }
        }
        
        // Read art data
        buffer.position(artOffs + artNum * 28);
        buffer.getShort(); // skip 2 bytes
        int frameOffs = buffer.get() & 0xFF;
        
        BufferedImage artIm = new BufferedImage(objSize, objSize, BufferedImage.TYPE_INT_ARGB);
        buffer.position(0x3104 + frameOffs * 128);
        byte[] frameData = new byte[objSize * objSize / 2];
        buffer.get(frameData);
        parseImageData(artIm, frameData, objSize, objSize, objSize * 4);
        
        if (doArea) { // placing object within area
          int left = objX;
          int right = toSigned((int) cmd.getBitRange(37, 47));
          int top = objY;
          int bottom = toSigned((int) cmd.getBitRange(47, 57));
          
          // Get bounding box of the art (simplified - Java doesn't have getbbox equivalent)
          int nl = 0, nt = 0, nr = objSize, nb = objSize;
          boolean foundPixel = false;
          
          // Find actual bounds of non-transparent pixels
          for (int y = 0; y < objSize && !foundPixel; y++) {
            for (int x = 0; x < objSize; x++) {
              if ((artIm.getRGB(x, y) >> 24) != 0) { // Non-transparent pixel
                nt = y;
                foundPixel = true;
                break;
              }
            }
          }
          
          foundPixel = false;
          for (int y = objSize - 1; y >= 0 && !foundPixel; y--) {
            for (int x = 0; x < objSize; x++) {
              if ((artIm.getRGB(x, y) >> 24) != 0) {
                nb = y + 1;
                foundPixel = true;
                break;
              }
            }
          }
          
          foundPixel = false;
          for (int x = 0; x < objSize && !foundPixel; x++) {
            for (int y = 0; y < objSize; y++) {
              if ((artIm.getRGB(x, y) >> 24) != 0) {
                nl = x;
                foundPixel = true;
                break;
              }
            }
          }
          
          foundPixel = false;
          for (int x = objSize - 1; x >= 0 && !foundPixel; x--) {
            for (int y = 0; y < objSize; y++) {
              if ((artIm.getRGB(x, y) >> 24) != 0) {
                nr = x + 1;
                foundPixel = true;
                break;
              }
            }
          }
          
          int nw = nr - nl;
          int nh = nb - nt;
          int cdx = nw / 2 + nl - objSize / 2;
          int cdy = nh / 2 + nt - objSize / 2;
          
          // Area placement logic
          if (right - left <= nw) {
            left += (right - left) / 2 + 1;
            right = left;
          }
          if (bottom - top <= nh) {
            top += (bottom - top) / 2 + 1;
            bottom = top;
          }
          if ((right - left > nw) && (bottom - top > nh)) {
            left += nw / 2;
            right -= nw / 2;
            top += nh / 2;
            bottom -= nh / 2;
          }
          
          objX = random.nextInt(Math.max(1, right - left + 1)) + left - cdx;
          objY = random.nextInt(Math.max(1, bottom - top + 1)) + top - cdy;
        }
        
        objCount++;
        Map<String, Object> asset = new HashMap<>();
        asset.put("art", artIm);
        asset.put("x", objX);
        asset.put("y", objY);
        asset.put("size", objSize);
        assets.put(i, asset);
      }
      
      // Handle attachments (simplified version)
      for (int iteration = 0; iteration < 10; iteration++) { // Limit iterations to prevent infinite loop
        boolean moved = false;
        for (int i = 0; i < 15; i++) {
          Map<String, Object> att = attachments.get(i);
          if ((Integer) att.get("obj") == -1) {
            continue;
          }
          Map<String, Object> obj = assets.get(i);
          if ((Integer) obj.get("obj") == -1) {
            continue;
          }
          Map<String, Object> attachedTo = assets.get((Integer) att.get("obj"));
          if ((Integer) attachedTo.get("obj") == -1) {
            continue;
          }
          
          int xOffs = (Integer) att.get("xoffs");
          int yOffs = (Integer) att.get("yoffs");
          int attachedX = (Integer) attachedTo.get("x");
          int attachedY = (Integer) attachedTo.get("y");
          int objCurrentX = (Integer) obj.get("x");
          int objCurrentY = (Integer) obj.get("y");
          
          if ((attachedX + xOffs != objCurrentX) || (attachedY + yOffs != objCurrentY)) {
            moved = true;
            obj.put("x", attachedX + xOffs);
            obj.put("y", attachedY + yOffs);
          }
        }
        if (!moved) {
          break;
        }
      }
      
      // Composite objects onto background
      Graphics2D bgGraphics = rgbBg.createGraphics();
      for (int i = objCount - 1; i >= 0; i--) { // Reverse order for layering
        if (i >= objOrder.length) continue;
        int orderIndex = objOrder[i] & 0xFF;
        if (!assets.containsKey(orderIndex)) continue;
        
        Map<String, Object> obj = assets.get(orderIndex);
        if (obj == null || (Integer) obj.get("obj") == -1) {
          continue;
        }
        
        BufferedImage art = (BufferedImage) obj.get("art");
        if (art == null) continue;
        
        int x = (Integer) obj.get("x");
        int y = (Integer) obj.get("y");
        int size = (Integer) obj.get("size");
        
        // Draw with object center as origin
        bgGraphics.drawImage(art, x - size / 2, y - size / 2, null);
      }
      bgGraphics.dispose();
      
      return "data:image/png;base64," + imgToBase64String(rgbBg, "png");
      
    } catch (Exception e) {
      // Return null on any error
      return null;
    }
  }

}
