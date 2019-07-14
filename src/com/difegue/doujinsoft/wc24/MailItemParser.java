package com.difegue.doujinsoft.wc24;

import java.util.logging.Level;
import java.util.logging.Logger;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.servlet.ServletContext;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Address;
import javax.mail.Message.RecipientType;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.InternetAddress;

import java.util.ArrayList;
import java.util.Properties;
import java.util.regex.*;
import java.util.List;

public class MailItemParser {

    
    /***
     * Handles emails received from a WC24 server or a data file. </br> 
     * </br>
     * - "WC24 Cmd Message" emails (Friend requests) are replied to using WiiConnect24Api </br>
     * - "QUESTION" emails (DIY Showcase survey box) are added to the matching SQLite table </br>
     * - "G"/"RR"/"MMM" (DIY Showcase content) are converted to .mio and added to the database </br>
     * - Other emails are sent to a backup address belonging to a real Wii for safekeeping. </br>
     * 
     * @param emailData emails recovered from a WC24 server
     * @param application servletContext for WiiConnect24Api
     */
    public static void consumeEmails(String emailData, ServletContext application) {

        Logger log = Logger.getLogger("WC24 Mail Parsing");
        ArrayList<MailItem> mailsToSend = new ArrayList<>();

        // Let's get started. First line of the mail response is our delimiter.
        String delimiter = emailData.split("\\R", 2)[0];
        log.log(Level.INFO, "Delimiter for this maildata is " + delimiter);

        String[] mailItems = emailData.split(delimiter);

        for (String content : mailItems) {

            // analyzeMail returns a MailItem if there's something to send out
            try {
                MailItem toSend = analyzeMail(content);

                if (toSend != null)
                    mailsToSend.add(toSend);
            } catch (Exception e) {
                log.log(Level.WARNING, "Couldn't analyze this mail, skipping: " + e);
            }
        }

        // Send out reply mails, if there are any.
        try {
            new WiiConnect24Api(application).sendMails(mailsToSend);
        } catch (Exception e) {
            log.log(Level.SEVERE, "Couldn't send out mails: " + e);
        }

    }

    private static MailItem analyzeMail(String mail) throws Exception {

        Logger log = Logger.getLogger("WC24 Mail Parsing");

        // Use JavaMail to parse the email string
        Session s = Session.getInstance(new Properties());
        InputStream is = new ByteArrayInputStream(mail.getBytes());
        MimeMessage message = new MimeMessage(s, is);

        // Reject mails that don't come from a Wii
        String wiiCode = getWiiCode(message.getFrom()[0].toString());
        if (wiiCode == null) {
            log.log(Level.INFO, "Mail doesn't come from a Wii - Skipping.");
            return null;
        }

        // Friend request 
        if (message.getSubject().equals("WC24 Cmd Message")) {
            return new MailItem(wiiCode);
        }

        // Survey box
        if (message.getSubject().equals("QUESTION")) {


            // No mail to send
            return null;
        }

        // Games
        if (message.getSubject().equals("G")) {


            // Send thank-you mail
            return new MailItem(wiiCode, List.of("A game"));
        }

        // Records

        // Manga

        // Other
        // Change From: and To: to the server's wii address and the backup wii address respectively
        String sender = System.getenv("WII_NUMBER");
        message.setFrom(new InternetAddress("w"+sender+"@rc24.xyz"));
        message.setRecipients(RecipientType.TO, new Address[]{new InternetAddress("w7475328617225276@rc24.xyz")});
        // Output a string and pipe that into a RawMailItem
        return new RawMailItem(message.toString());
    }

    private static String getWiiCode(String address) {

        // Don't throw an exception here if WC24_SERVER doesn't exist - other parts of the code will warn the user just fine.
        if (!System.getenv().containsKey("WC24_SERVER"))
            return null;

        String wc24Server = System.getenv("WC24_SERVER");

        // Get wii code by stripping "w" and "@wii.com" from the sender's address
        Pattern pattern = Pattern.compile("w([0-9]*)@"+ Pattern.quote(wc24Server));
        Matcher matcher = pattern.matcher(address);
        while (matcher.find()) {
            return matcher.group(1);
        }
        // No match = not a Wii
        return null;
    }

}
