package com.difegue.doujinsoft.wc24;

import com.mitchellbosecke.pebble.error.PebbleException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClients;

import javax.servlet.ServletContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.concurrent.atomic.AtomicInteger;

public class WiiConnect24Api extends WC24Base {

    public WiiConnect24Api(ServletContext application) throws Exception {
        super(application);
    }

    /**
     * Craft a request with all the mails we have to send, and fire it over to the
     * WC24 server.
     *
     * @param mails
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public String sendMails(List<MailItem> mails) throws IOException, InterruptedException {

        String output = "";

        // If the mail list is too long it'll likely overload the WC24 endpoint
        // Split the list into 15s (max amount) and perform an equal number of requests
        final AtomicInteger counter = new AtomicInteger();
        final java.util.Collection<List<MailItem>> chunkedMails = mails.stream()
                .collect(Collectors.groupingBy(it -> counter.getAndIncrement() / 15))
                .values();

        for (List<MailItem> chunk : chunkedMails) {
            output += sendMailsInternal(chunk);
            // Sleep between chunks to avoid murdering the RC24 server :|
            Thread.sleep(1000);
            output += "----------------\n";
        }

        return output;
    }

    private String sendMailsInternal(List<MailItem> mails) throws IOException {
        Logger log = Logger.getLogger("WC24");

        HttpClient httpclient = HttpClients.createDefault();
        HttpPost request = new HttpPost("http://mtw." + wc24Server + "/cgi-bin/send.cgi");

        // Request parameters and other properties.
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        String templatePath = application.getRealPath("/WEB-INF/wiiconnect24");

        // Add the authentication string as mlid
        // See spec on https://wiibrew.org/wiki/WiiConnect24/Mail
        String authString = "mlid=w" + sender + "\r\n" + "passwd=" + wc24Pass;
        builder.addTextBody("mlid", authString);

        int count = 1;
        for (MailItem mail : mails) {
            // m1, m2, etc
            try {
                String renderedMail = mail.renderString(templatePath);
                log.log(Level.FINE, renderedMail);
                builder.addTextBody("m" + count, renderedMail);
                count++;
            } catch (PebbleException e) {
                log.log(Level.SEVERE, "Couldn't add mailitem to the sendbox: " + e.getPebbleMessage());
            }

        }

        HttpEntity formDataEntity = builder.build();
        request.setEntity(formDataEntity);

        // Log full multipart request, if thou must
        // It makes the logs gigantic
        if (debugLogging) {

            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                builder.build().writeTo(baos);

                log.log(Level.INFO, "Executing request:" + System.lineSeparator()
                        + request.getRequestLine() + System.lineSeparator()
                        + baos.toString());
            } catch (Exception e) {
                log.log(Level.INFO, e.getMessage());
            }
        }

        // Execute and get the response.
        HttpResponse response = httpclient.execute(request);
        HttpEntity entity = response.getEntity();

        if (entity != null) {
            try (InputStream inStream = entity.getContent()) {

                // Just return the server response as-is for the time being.
                String wc24rep = new String(inStream.readAllBytes(), StandardCharsets.UTF_8);
                log.log(Level.INFO, "Reply from WC24 server: " + wc24rep);
                return wc24rep;
            }
        }
        return null;
    }

    /***
     * Phones up the WC24 server to grab mails, and consume them. See
     * MailItemParser.
     * 
     * @throws Exception
     */
    public String receiveMails() throws Exception {

        HttpClient httpclient = HttpClients.createDefault();
        HttpPost request = new HttpPost("http://mtw." + wc24Server + "/cgi-bin/receive.cgi");

        // For receiving, the syntax is different and the creds are form parameters.
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.addTextBody("mlid", "w" + sender);
        builder.addTextBody("passwd", wc24Pass);
        builder.addTextBody("maxsize", "2000000");

        HttpEntity formDataEntity = builder.build();
        request.setEntity(formDataEntity);

        // Execute and get the response.
        HttpResponse response = httpclient.execute(request);
        HttpEntity entity = response.getEntity();

        if (entity != null)
            try (InputStream inStream = entity.getContent()) {

                String responseText = new BufferedReader(new InputStreamReader(inStream)).lines()
                        .collect(Collectors.joining("\n"));

                if (debugLogging) {
                    Logger log = Logger.getLogger("WC24 Debug");
                    log.log(Level.INFO, "Reponse from WC24: \n" + responseText);
                }

                new MailItemParser(application).consumeEmails(responseText);
                return responseText;
            }

        return "No mails received.";
    }
}
