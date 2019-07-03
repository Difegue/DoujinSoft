package com.difegue.doujinsoft.wc24;

import com.mitchellbosecke.pebble.error.PebbleException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;

import javax.servlet.ServletContext;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WiiConnect24Api {

    private String sender, wc24Server, wc24Pass;

    public WiiConnect24Api() throws Exception {

        if (!System.getenv().containsKey("WII_NUMBER"))
            throw new Exception("Wii sender friend number not specified. Please set the WII_NUMBER environment variable.");

        if (!System.getenv().containsKey("WC24_SERVER"))
            throw new Exception("WiiConnect24 server url not specified. Please set the WC24_SERVER environment variable.");

        if (!System.getenv().containsKey("WC24_PASSWORD"))
            throw new Exception("WiiConnect24 account password not specified. Please set the WC24_PASSWORD environment variable.");

        sender = System.getenv("WII_NUMBER");
        wc24Server = System.getenv("WC24_SERVER");
        wc24Pass = System.getenv("WC24_PASSWORD");
    }

    /**
     * Craft a request with all the mails we have to send, and fire it over to the WC24 server.
     *
     * @param mails
     * @return
     * @throws IOException
     */
    public String sendMails(List<MailItem> mails, ServletContext application) throws IOException {

        Logger log = Logger.getLogger("WC24");

        HttpClient httpclient = HttpClients.createDefault();
        HttpPost httppost = new HttpPost("https://mtw." + wc24Server + "/cgi-bin/send.cgi?mlid=w" + sender + "&passwd=" + wc24Pass);

        // Request parameters and other properties.
        List<NameValuePair> params = new ArrayList<>();
        String templatePath = application.getRealPath("/WEB-INF/wiiconnect24");

        int count = 1;
        for (MailItem mail : mails) {
            // m1, m2, etc
            try {
                String renderedMail = mail.renderString(templatePath);
                log.log(Level.INFO, renderedMail);
                params.add(new BasicNameValuePair("m" + count, renderedMail));
                count++;
            } catch (PebbleException e) {
                log.log(Level.SEVERE, "Couldn't add mailitem to the sendbox: " + e.getPebbleMessage());
            }

        }

        httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

        //Execute and get the response.
        HttpResponse response = httpclient.execute(httppost);
        HttpEntity entity = response.getEntity();

        if (entity != null) {
            try (InputStream inStream = entity.getContent()) {

                // Just return the server response as-is for the time being.
                String wc24rep = new String(inStream.readAllBytes(), StandardCharsets.UTF_8);
                log.log(Level.INFO, wc24rep);
                return wc24rep;
            }
        }
        return null;
    }
}


