package com.difegue.doujinsoft.wc24;

/**
 * Dumb extension of MailItem that doesn't perform any templating whatsoever and sends the given string as-is.
 * Used to forward mails in MailItemParser.
 */
public class RawMailItem extends MailItem {

    private String mail;

    public RawMailItem(String raw) throws Exception {
        super("0");
        mail = raw;
    }

    @Override
    public String renderString(String templatePath) {
        return mail;
    }

}