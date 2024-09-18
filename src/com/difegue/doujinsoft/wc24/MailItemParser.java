package com.difegue.doujinsoft.wc24;

import java.util.logging.Level;
import java.util.logging.Logger;

import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.ServletContext;

import com.difegue.doujinsoft.utils.CollectionUtils;
import com.difegue.doujinsoft.utils.MioStorage;
import com.xperia64.diyedit.metadata.*;

import javax.mail.*;
import javax.mail.Message.RecipientType;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.InternetAddress;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import java.util.regex.*;
import java.util.List;
import java.util.Base64;

import com.difegue.doujinsoft.templates.Collection;

public class MailItemParser extends WC24Base {

    private final Pattern pattern = Pattern.compile("w([0-9]*)@" + Pattern.quote(wc24Server));
    private String dataDir;
    private String mailFallbackCode;

    public MailItemParser(ServletContext application) throws Exception {
        super(application);
        dataDir = application.getInitParameter("dataDirectory");

        if (!System.getenv().containsKey("WII_FALLBACK"))
            throw new Exception(
                    "Fallback Wii number not specified. Please set the WII_FALLBACK environment variable.");

        mailFallbackCode = System.getenv("WII_FALLBACK");
    }

    /***
     * Handles emails received from a WC24 server or a data file. </br>
     * </br>
     * - "WC24 Cmd Message" emails (Friend requests) are replied to using
     * WiiConnect24Api </br>
     * - "QUESTION" emails (DIY Showcase survey box) are added to the matching
     * SQLite table </br>
     * - "G"/"RR"/"MMM" (DIY Showcase content) are converted to .mio and added to
     * the database </br>
     * - Other emails are sent to a backup address belonging to a real Wii for
     * safekeeping. </br>
     * 
     * @param emailData emails recovered from a WC24 server
     */
    public void consumeEmails(String emailData) {

        Logger log = Logger.getLogger("WC24 Mail Parsing");
        ArrayList<MailItem> mailsToSend = new ArrayList<>();

        // Let's get started. First line of the mail response is our delimiter.
        String delimiter = emailData.split("\\R", 2)[0];
        log.log(Level.INFO, "Delimiter for this maildata is " + delimiter);

        String[] mailItems = emailData.split(Pattern.quote(delimiter));

        for (String content : mailItems) {
            try {

                if (content.contains("This part is ignored")) {
                    // What it says on the tin!
                    continue;
                }

                if (content.length() < 30) {
                    // Skip empty (or mostly-empty) items
                    // The mails end with "--202409141517/6360705--" whereas the delimiter is
                    // "--202409141517/6360705" so you'll have some noise at the end
                    continue;
                }

                // Strip "Content-Type: text/plain" at the beginning of the item
                content = content.substring(30);
                MailItem toSend = analyzeMail(content);

                // analyzeMail returns a MailItem if there's something to send out
                if (toSend != null)
                    mailsToSend.add(toSend);
            } catch (Exception e) {
                log.log(Level.WARNING, "Couldn't analyze this mail, skipping: " + e);
            }
        }

        // Add recovered .mios to the database
        try {
            MioStorage.ScanForNewMioFiles(dataDir, log);
        } catch (SQLException e) {
            log.log(Level.SEVERE, "Couldn't add obtained mios: " + e);
        }

        // Send out reply mails, if there are any.
        try {
            new WiiConnect24Api(application).sendMails(mailsToSend);
        } catch (Exception e) {
            log.log(Level.SEVERE, "Couldn't send out mails: " + e);
        }

    }

    private MailItem analyzeMail(String mail) throws Exception {

        Logger log = Logger.getLogger("WC24 Mail Parsing");

        // Use JavaMail to parse the email string
        Session s = Session.getInstance(new Properties());
        InputStream is = new ByteArrayInputStream(mail.getBytes());
        MimeMessage message = new MimeMessage(s, is);

        // Reject mails that don't come from a Wii
        log.log(Level.INFO, "Analyzing mail received from " + message.getFrom()[0].toString());
        String wiiCode = getWiiCode(message.getFrom()[0].toString());
        if (wiiCode == null) {
            log.log(Level.INFO, "Mail doesn't come from a Wii - Skipping.");
            return null;
        }

        String subject = message.getSubject();

        if (subject == null) // The Wii doesn't care about the subject field
            subject = "";

        if (!isFriendCodeSaved(wiiCode)) {

            // If the mail is a friend request, handle it
            if (subject.contains("WC24 Cmd Message")) {
                log.log(Level.INFO, "Friend request from " + wiiCode);
                saveFriendCode(wiiCode);
                return new MailItem(wiiCode);
            } else {
                // Reject mails that don't have their code stored in the DB - with exceptions.
                // Note - This was originally implemented to prevent survey spam, so it might be
                // necessary to re-exclude QUESTION here if that happens again.
                if (subject.equals("QUESTION") || subject.equals("G") || subject.equals("RR")
                        || subject.equals("MMM")) {
                    log.log(Level.INFO, "Unregistered Friend Code yet DIY content - Making an exception...");
                } else {
                    log.log(Level.INFO, "Mail from unregistered Friend Code - Skipping.");
                    return null;
                }

            }
        }

        // Survey box
        if (subject.equals("QUESTION")) {

            // Get survey answer (2nd bodypart) as a byte array
            InputStream attachmentData = ((Multipart) message.getContent()).getBodyPart(1).getInputStream();
            byte[] survey = attachmentData.readAllBytes();

            // 0x19 (25 bytes) for the title, a byte for the type, a byte for how many
            // stars, and a byte for the comment
            String title = new String(Arrays.copyOfRange(survey, 0, 24));

            // Discard surveys with empty titles
            if (title.isEmpty() || title.isBlank())
                return null;

            saveSurveyAnswer(wiiCode, survey[25], title, survey[26], survey[27]);

            log.log(Level.INFO, "Survey for " + title);

            // No mail to send
            return null;
        }

        // Games/Records/Manga
        if (subject.equals("G") || subject.equals("RR") || subject.equals("MMM")) {

            byte[] data = getLZ10BodyPart(message);
            Metadata metadata = new Metadata(data);

            // Store data in a .mio file; Server will pick it up later
            String hash = MioStorage.computeMioHash(data);
            File mio = new File(dataDir + "/mio/" + hash + ".mio");
            try (FileOutputStream fos = new FileOutputStream(mio.getAbsolutePath())) {
                fos.write(data);
            }

            // Store hash in the matching WC24 collection
            addToWC24Collection(subject, hash);

            // Send thank-you mail
            String name = metadata.getName();
            log.log(Level.INFO, "Received DIY content: " + name);
            return new MailItem(wiiCode, List.of(name), true);
        }

        // Other emails...
        // Change From: and To: to the server's wii address and the backup wii address
        // respectively
        message.setFrom(new InternetAddress("w" + sender + "@" + wc24Server));
        message.addHeader("MAIL FROM", "w" + sender + "@" + wc24Server);
        message.setRecipients(RecipientType.TO,
                new Address[] { new InternetAddress("w" + mailFallbackCode + "@" + wc24Server) });
        message.addHeader("RCPT TO", "w" + mailFallbackCode + "@" + wc24Server);

        // Add the original sender's Wii code as an AltName so it appears on the message
        // board
        // This is easier than messing with the message content, and shouldn't fuck up
        // invisible mails (i.e Miis)
        String b64WiiCode = Base64.getEncoder().encodeToString(wiiCode.getBytes(StandardCharsets.UTF_16BE));
        message.addHeader("X-Wii-AltName", b64WiiCode);

        // Output a string and pipe that into a RawMailItem
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        message.writeTo(os);
        log.log(Level.INFO, "Unknown mail with subject " + message.getSubject() + " Forwarding to " + mailFallbackCode);
        return new RawMailItem(mailFallbackCode, new String(os.toByteArray(), StandardCharsets.UTF_8));
    }

    private String getWiiCode(String address) {

        // Get wii code by stripping "w" and "@wii.com" from the sender's address
        Matcher matcher = pattern.matcher(address);
        while (matcher.find()) {
            return matcher.group(1);
        }
        // No match = not a Wii
        return null;
    }

    /**
     * Recover the attachment from a DIY Showcase Message and decode it to a file.
     * 
     * @param m the message to recover the attachment from
     * @return LZ10-decompressed data
     * @throws IOException
     * @throws MessagingException
     */
    private byte[] getLZ10BodyPart(Message m) throws Exception {

        // Get the second bodypart of the message
        Multipart content = (Multipart) m.getContent();

        // Write bodypart to a file
        Path compressedMio = Files.createTempFile("mio", ".lz10");
        Files.copy(content.getBodyPart(1).getInputStream(), compressedMio, StandardCopyOption.REPLACE_EXISTING);

        // LZSS-decode it
        String filePath = compressedMio.toFile().getAbsolutePath();
        new LZSS(application).LZS_Decode(filePath, filePath + "d");

        return Files.readAllBytes(new File(filePath + "d").toPath());
    }

    private boolean saveSurveyAnswer(String sender, byte type, String title, byte stars, byte comment) {

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dataDir + "/mioDatabase.sqlite")) {

            PreparedStatement ret = connection.prepareStatement("INSERT INTO Surveys VALUES (?,?,?,?,?,?)");
            ret.setLong(1, System.currentTimeMillis());
            ret.setInt(2, type & 0xFF);
            ret.setString(3, title);
            ret.setInt(4, stars & 0xFF);
            ret.setInt(5, comment & 0xFF);
            ret.setString(6, sender);

            ret.executeUpdate();

            ret.close();
            connection.close();
            return true;

        } catch (SQLException e) {
            return false;
        }
    }

    private boolean saveFriendCode(String code) {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dataDir + "/mioDatabase.sqlite")) {

            PreparedStatement ret = connection.prepareStatement("INSERT INTO Friends VALUES (?)");
            ret.setString(1, code);
            ret.executeUpdate();
            ret.close();

            connection.close();
            return true;

        } catch (SQLException e) {
            return false;
        }
    }

    private boolean isFriendCodeSaved(String code) {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dataDir + "/mioDatabase.sqlite")) {

            PreparedStatement ret = connection.prepareStatement("select COUNT(*) from Friends WHERE friendcode = ?");
            ret.setString(1, code);

            ResultSet result = ret.executeQuery();
            int res = result.getInt(1);
            return (res == 1);
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Update pre-set WC24 collections with the newly added hash. A bit hacky but
     * it'll do for now...
     * 
     * @param type type (G/RR/MMM)
     * @param hash mio data MD5 hash
     * @throws FileNotFoundException
     * @throws IOException
     */
    private void addToWC24Collection(String type, String hash) throws FileNotFoundException, IOException {

        String collectionFile = dataDir + "/collections/";
        switch (type) {
            case "G":
                collectionFile += "e_wl24_g";
                break;
            case "RR":
                collectionFile += "e_wl24_r";
                break;
            case "MMM":
                collectionFile += "e_wl24_m";
                break;
        }

        collectionFile += ".json";

        // Try opening the matching JSON file
        Collection c = CollectionUtils.GetCollectionFromFile(collectionFile);
        c.addMioHash(hash);
        // Overwrite the collectionfile
        CollectionUtils.SaveCollectionToFile(c, collectionFile);
    }
}
