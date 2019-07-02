package com.difegue.doujinsoft.wc24;

import com.difegue.doujinsoft.templates.BaseMio;
import com.difegue.doujinsoft.utils.MioCompress;
import com.difegue.doujinsoft.utils.MioUtils;
import com.xperia64.diyedit.FileByteOperations;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class MailItem {

    public String sender, recipient, wc24Server, base64EncodedAttachment;

    /**
     * Create a WC24 mail containing DIY data to send to Showcase.
     * @param wiiCode Friend Code to send the mail to
     * @param diyData DIY file to unzip and send
     * @param type type of the file
     * @param dataDir doujinsoft data directory to pull .mios from
     * @throws Exception
     */
    public MailItem(String wiiCode, BaseMio diyData, int type, String dataDir) throws Exception {

        recipient = wiiCode;
        InitializeFromEnvironment();

        String mioPath = dataDir;

        switch (type) {
            case MioUtils.Types
                    .GAME:   mioPath += "/mio/game/";
            case MioUtils.Types
                    .MANGA:  mioPath += "/mio/manga/";
            case MioUtils.Types
                    .RECORD: mioPath += "/mio/record/";

        }
        mioPath += diyData.hash + ".miozip";

        File uncompressedMio = MioCompress.uncompressMio(new File(mioPath));
        byte[] mioData = FileByteOperations.read(uncompressedMio.getAbsolutePath());

        // Compress the bytes with LZ10


        // Base64 encode 'em and we're good
        base64EncodedAttachment = Base64.getEncoder().encodeToString(mioData);
    }

    /**
     * Create a WC24 recap mail to send to the Wii Message Board.
     * @param wiiCode Friend Code to send the mail to
     * @param contentNames DIY content to enumerate in the mail
     */
    public MailItem(String wiiCode, String[] contentNames) throws Exception {

        recipient = wiiCode;
        InitializeFromEnvironment();

        String message = RECAP_HEADER;

        for (String s: contentNames) {
            message += "* "+ s + "\n";
        }

        message += RECAP_FOOTER;

        // Encode the message in UTF-16BE as expected by the Wii, then wrap it in base64
        byte[] utf16 = StandardCharsets.UTF_16BE.encode(message).array();
        base64EncodedAttachment = Base64.getEncoder().encodeToString(utf16);
    }

    private void InitializeFromEnvironment() throws Exception {

        if (!System.getenv().containsKey("WII_NUMBER"))
            throw new Exception("Wii sender friend number not specified. Please set the WII_NUMBER environment variable.");

        if (!System.getenv().containsKey("WC24_SERVER"))
            throw new Exception("WiiConnect24 server url not specified. Please set the WC24_SERVER environment variable.");

        sender = System.getenv("WII_NUMBER");
        wc24Server = System.getenv("WC24_SERVER");

    }

    private static String RECAP_HEADER =
            "Thank you for using DoujinSoft!\n" +
            "\n" +
            "The following content has been sent to your Wii alongside this message:\n\n";

    private static String RECAP_FOOTER =
            "~~~~~ Service provided for fun ~~~~~\n" +
            "~~~~~   by RiiConnect24 and  ~~~~~\n" +
            "~~~~~  https://tvc-16.science  ~~~~~";

}
