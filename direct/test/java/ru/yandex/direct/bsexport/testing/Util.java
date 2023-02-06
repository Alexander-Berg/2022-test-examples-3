package ru.yandex.direct.bsexport.testing;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import com.google.common.io.Resources;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

public class Util {
    private Util() {
    }

    public static String getFromClasspath(String resourceName) {
        URL resource = Resources.getResource(resourceName);
        try {
            return Resources.toString(resource, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getCompactJsonFromClasspath(String resourceName) {
        String json = getFromClasspath(resourceName);
        Gson gson = new GsonBuilder()
                .disableHtmlEscaping()
                .create();
        JsonElement jsonElement = gson.fromJson(json, JsonElement.class);
        return gson.toJson(jsonElement);
    }
}

