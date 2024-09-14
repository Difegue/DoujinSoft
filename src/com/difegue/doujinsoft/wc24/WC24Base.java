package com.difegue.doujinsoft.wc24;

import javax.servlet.ServletContext;

/***
 * Base class for stuff using the WC24 environment variables.
 */
public class WC24Base {

    protected String sender, wc24Server, wc24Pass;
    protected Boolean debugLogging;
    protected ServletContext application;

    public WC24Base(ServletContext application) throws Exception {

        if (!System.getenv().containsKey("WII_NUMBER"))
            throw new Exception(
                    "Wii sender friend number not specified. Please set the WII_NUMBER environment variable.");

        if (!System.getenv().containsKey("WC24_SERVER"))
            throw new Exception(
                    "WiiConnect24 server url not specified. Please set the WC24_SERVER environment variable.");

        if (!System.getenv().containsKey("WC24_PASSWORD"))
            throw new Exception(
                    "WiiConnect24 account password not specified. Please set the WC24_PASSWORD environment variable.");

        if (!System.getenv().containsKey("WC24_DEBUG")) {
            debugLogging = false;
        } else {
            debugLogging = Boolean.parseBoolean(System.getenv("WC24_DEBUG"));
        }

        sender = System.getenv("WII_NUMBER");
        wc24Server = System.getenv("WC24_SERVER");
        wc24Pass = System.getenv("WC24_PASSWORD");

        this.application = application;
    }

}