package com.difegue.doujinsoft.templates;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class Cart {

    private JsonArray games = new JsonArray();
    private JsonArray manga = new JsonArray();
    private JsonArray records = new JsonArray();

    private File saveFile = null;
    private String recipientFriendCode;

    public Cart(HttpServletRequest request) throws IOException, ServletException {

        if (request.getParameterMap().containsKey("save")) {

            // Get byte[] save from request parameters
            Part filePart = request.getPart("save");
            String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString(); // MSIE fix.
            InputStream fileContent = filePart.getInputStream();

            // Drop the file in a temp file
            saveFile = File.createTempFile(fileName + System.currentTimeMillis(), "bin");

            Files.copy(
                    fileContent,
                    saveFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
        } else {
            recipientFriendCode = request.getParameter("recipient");
        }

        // Get the cart data and deserialize it.
        JsonElement a = JsonParser.parseString(request.getParameter("games"));
        if (a.isJsonArray())
            games = a.getAsJsonArray();

        a = JsonParser.parseString(request.getParameter("manga"));
        if (a.isJsonArray())
            manga = a.getAsJsonArray();

        a = JsonParser.parseString(request.getParameter("records"));
        if (a.isJsonArray())
            records = a.getAsJsonArray();
    }

    public JsonArray getGames() {
        return games;
    }

    public JsonArray getManga() {
        return manga;
    }

    public JsonArray getRecords() {
        return records;
    }

    public File getSaveFile() {
        return saveFile;
    }

    public String getRecipientCode() {
        return recipientFriendCode;
    }

}
