package com.difegue.doujinsoft.wc24;

import com.mitchellbosecke.pebble.template.PebbleTemplate;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.StringJoiner;

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

    public String CraftMail(MailItem item, PebbleTemplate mailTemplate) {



    }

    public boolean SendMails(String[] mails) throws IOException {

        URL url = new URL("https://mtw."+wc24Server+"/cgi-bin/send.cgi?mlid=w"+sender+"&passwd="+wc24Pass);
        URLConnection con = url.openConnection();
        HttpURLConnection http = (HttpURLConnection)con;
        http.setRequestMethod("POST"); // PUT is another valid option
        http.setDoOutput(true);

        // m1, m2, etc

        StringJoiner sj = new StringJoiner("&");
        for(Map.Entry<String,String> entry : arguments.entrySet())
            sj.add(URLEncoder.encode(entry.getKey(), "UTF-8") + "="
                    + URLEncoder.encode(entry.getValue(), "UTF-8"));
        byte[] out = sj.toString().getBytes(StandardCharsets.UTF_8);
        int length = out.length;

    }

}
