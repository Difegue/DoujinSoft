package com.difegue.doujinsoft.utils;

import com.difegue.doujinsoft.templates.Collection;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import java.io.*;

public class CollectionUtils {

    public static Collection GetCollectionFromFile(String path) throws FileNotFoundException {
        Gson gson = new Gson();
        JsonReader jsonReader = new JsonReader(new FileReader(path));
        //Auto bind the json to a class
        Collection c = gson.fromJson(jsonReader, Collection.class);
        return c;
    }

    public static void SaveCollectionToFile(Collection c, String path) throws IOException {
        Gson gson = new Gson();
        try (Writer writer = new FileWriter(path)) {
            gson.toJson(c, writer);
        }
    }

}
