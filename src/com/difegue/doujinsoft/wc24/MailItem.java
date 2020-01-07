package com.difegue.doujinsoft.wc24;

import com.difegue.doujinsoft.utils.MioUtils;
import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.error.PebbleException;
import com.mitchellbosecke.pebble.template.PebbleTemplate;
import com.xperia64.diyedit.metadata.Metadata;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.servlet.ServletContext;

public class MailItem {

    public String sender, recipient, wc24Server, base64EncodedAttachment, wiiFace;
    public int attachmentType;

    /**
     * Create a WC24 mail containing DIY data to send to Showcase.
     * @param wiiCode Friend Code to send the mail to
     * @param diyData DIY file to send
     * @param type type of the file
     * @param context servletContext to find the folder where the lzss executables are located this is absolutely disgusting holy fuck spare him his life from this monstrosity
     * @throws Exception
     */
    public MailItem(String wiiCode, Metadata diyData, int type, ServletContext context) throws Exception {

        attachmentType = type;
        initializeFromEnvironment(wiiCode);

        Path compressedMio = Files.createTempFile("mio",".lz10");
        try (FileOutputStream fos = new FileOutputStream(compressedMio.toFile())) {
            // Re-write the diy data to a file that we'll compress with LZSS native
            fos.write(diyData.file);
        }

        // Compress the bytes with LZ10/LZSS
        String filePath = compressedMio.toFile().getAbsolutePath();
        new LZSS(context).LZS_Encode(filePath, filePath+"d");
        byte[] mioData = Files.readAllBytes(new File(filePath+"d").toPath());

        // Base64 encode 'em and we're good.
        // Add a linebreak every 76 characters for MIME compliancy (The Wii doesn't care but it looks nicer)
        base64EncodedAttachment = Base64.getEncoder().encodeToString(mioData).replaceAll("(.{76})", "$1\n");
    }

    /**
     * Create a WC24 recap mail to send to the Wii Message Board.
     * @param wiiCode Friend Code to send the mail to
     * @param contentNames DIY content to enumerate in the mail
     * @param incoming Whether this recap is for incoming or outgoing content.
     */
    public MailItem(String wiiCode, List<String> contentNames, boolean incoming) throws Exception {

        String message;

        if (incoming) {
            attachmentType = 2;
            message = RECAP_HEADER_INCOMING;
        }
        else {
            attachmentType = 1;
            message = RECAP_HEADER_OUTGOING;
        }

        initializeFromEnvironment(wiiCode);

        for (String s: contentNames) {
            message += "* "+ s + "\n";
        }

        message += "\n" + RECAP_FOOTER;

        // Encode the message in UTF-16BE as expected by the Wii, then wrap it in base64
        byte[] utf16 = StandardCharsets.UTF_16BE.encode(message).array();
        base64EncodedAttachment = Base64.getEncoder().encodeToString(utf16).replaceAll("(.{76})", "$1\n");

        // Pick a random Mii
        int idx = new Random().nextInt(LETTERHEAD_MIIS.length);
        wiiFace = LETTERHEAD_MIIS[idx];
    }

    /**
     * Create a Wii Mail with the specified message. Uses the custom DoujinSoft mail template.
     * @param wiiCode
     * @param message 35 columns wide for best display on Wiis.
     * @throws Exception
     */
    public MailItem(String wiiCode, String message) throws Exception {
        
        attachmentType = 3; 
        initializeFromEnvironment(wiiCode);

        // Encode the message in UTF-16BE as expected by the Wii, then wrap it in base64
        byte[] utf16 = StandardCharsets.UTF_16BE.encode(message).array();
        base64EncodedAttachment = Base64.getEncoder().encodeToString(utf16).replaceAll("(.{76})", "$1\n");

    }

    /**
     * Create a WC24 friend request mail.
     * @param wiiCode
     */
    public MailItem(String wiiCode) throws Exception {
        attachmentType = 0;
        initializeFromEnvironment(wiiCode);
    }

    /**
     * Craft the string version of the mail, using templates.
     * @return
     * @throws PebbleException
     * @throws IOException
     */
    public String renderString(String templatePath) throws PebbleException, IOException {

        PebbleEngine engine = new PebbleEngine.Builder().build();
        PebbleTemplate template = null;

        switch (attachmentType) {
            case 0:          template = engine.getTemplate(templatePath + ("/friend_request.eml"));
                             break;
            case 1:          template = engine.getTemplate(templatePath + ("/recap_mail.eml"));
                             break;
            case 2:          template = engine.getTemplate(templatePath + ("/recap_mail_blue.eml"));
                             break;         
            case 3:          template = engine.getTemplate(templatePath + ("/doujinsoft_mail.eml"));
                             break;         
            case MioUtils.Types
                    .GAME:   template = engine.getTemplate(templatePath + ("/game_mail.eml"));
                             break;
            case MioUtils.Types
                    .MANGA:  template = engine.getTemplate(templatePath + ("/manga_mail.eml"));
                             break;
            case MioUtils.Types
                    .RECORD: template = engine.getTemplate(templatePath + ("/record_mail.eml"));
                             break;
        }

        Map<String, Object> context = new HashMap<>();
        context.put("mail", this);

        Writer writer = new StringWriter();
        template.evaluate(writer, context);
        return writer.toString();
    }

    private void initializeFromEnvironment(String recipientCode) throws Exception {

        if (!System.getenv().containsKey("WII_NUMBER"))
            throw new Exception("Wii sender friend number not specified. Please set the WII_NUMBER environment variable.");

        if (!System.getenv().containsKey("WC24_SERVER"))
            throw new Exception("WiiConnect24 server url not specified. Please set the WC24_SERVER environment variable.");

        if (!validateFriendCode(recipientCode))
            throw new Exception("Invalid Wii Friend Code.");

        sender = System.getenv("WII_NUMBER");
        wc24Server = System.getenv("WC24_SERVER");
        recipient = recipientCode;
    }

    private boolean validateFriendCode(String code) {

        if (code.length() != 16)
            return false;

        return code.chars().allMatch(x -> Character.isDigit(x));
    }

    private static String RECAP_HEADER_INCOMING =
        "Thank you for using DoujinSoft!\n" +
        "\n" +
        "You're receiving this message to      acknowledge that we've received and  added the following to our database:\n\n";

    private static String RECAP_HEADER_OUTGOING =
        "Thank you for using DoujinSoft!\n" +
        "\n" +
        "The following content has been sent  to your Wii alongside this message:\n\n";

    private static String RECAP_FOOTER =
        "~~~~~ Service provided for fun ~~~~~\n" +
        "~~~~~   by RiiConnect24 and  ~~~~~\n" +
        "~~~~~    Difegue @ TVC-16    ~~~~~";

    private static String[] LETTERHEAD_MIIS = {
        "gBYAQwAuAE4AbwByAHIAaQBzAAAAAFtAgZX3KDb4KCzG7AuQaKuokFxM\n BmloSrjSAIr9DiUEAFcAbwBsAHYAZQByAGkAbgBlAAA=\n", // Chuck Norris
        "gAAATQBpAHkAYQBtAG8AdABvAAAAAFtAhl8LZ29KhAglQIQAsj0EolyN\n BEBCSYitAIoAiiUEAE0AYQB0AHkAegB1AHoAdQAAAAA=\n", // Miyamoto
        "gAAASQB3AGEAdABhAAAAAAAAAAAAAGVUhl8KkW9KhAjlwFggSb0Kwlxu\n CGBzWUCuSowAiiUEAE0AYQB0AHkAegB1AHoAdQAAAAA=\n", // Iwata
        "UWYAbQBlAHkAbwBuAAAAAAAAAAAAAAALgRz5xwO2IGWARApAkP0wTISQ\n DoQAWEAOChEAiiUFMNcwyzCqAAAAAAAAAAAAAAAAAAA=\n", // meyon
        "QBQAbwBzAGEAcQBhAAAAAAAAAAAAAAAAgb1lBlRWMF6ARI7AQT0JFRhg\n DwQAWJgOAIoAiiUFAEEALgBKJgYAAAAAAAAAAAAAAAA=\n", // osaqa
        "FuQAVAAuACAATQBlAGkAagBpAG4AAExggkpBim9KhAgkBDxAoX0oolxs\n CERTSHiNAIoAiiUFAFQALgAgAE0AZQBpAGoAaQBuAAA=\n", // Meijin
        "AAQAVwBhAHIAaQBvAAAAAAAAAAAAAE9/gHv6E8I+IRitwHIAsUAREKyq\n ikBWGHEKAEAAgEYQAFQAbwBjAGMAaQAAAAAAAAAAAAA=\n", // Wario
        "ABAAVwBhAGwAdQBpAGcAaQAAAAAAAH8AgOynWMI+IRhABHJAeQAREBjJ\n iiA2KDEMAIoAiiUEAFQAbwBjAGMAaQAAAAAAAAAAAAA=\n"  // Waluigi
    };
}
