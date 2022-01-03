package com.difegue.doujinsoft.utils;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Base64;
import java.time.*;
import java.time.format.DateTimeFormatter;

import javax.imageio.ImageIO;

import com.xperia64.diyedit.editors.GameEdit;
import com.xperia64.diyedit.editors.MangaEdit;

/*
 * Static class containing base methods for interacting with .mio files.
 * Basically an abstraction layer for DIYEdit.
 * 
 */

public class MioUtils {

  // Constants for timestamp printing
  private static final ZonedDateTime date = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
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
    ZonedDateTime dateMio = date.plusDays(timestamp);
    return dateMio.format(formatter);
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

}
