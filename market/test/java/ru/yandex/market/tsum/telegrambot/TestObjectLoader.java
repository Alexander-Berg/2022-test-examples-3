package ru.yandex.market.tsum.telegrambot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.net.URL;

public class TestObjectLoader {
    private static final Gson GSON = new GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .create();

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static <T> T getTestUpdateObject(String name, Class<T> targetClass,
                                            SerializerType serializerType) throws IOException {

        URL url = Resources.getResource(name);
        String result = Resources.toString(url, Charsets.UTF_8);

        if (serializerType == SerializerType.JACKSON) {
            return MAPPER.readValue(result.toString(), targetClass);
        } else {
            return GSON.fromJson(result.toString(), targetClass);
        }
    }

    public enum SerializerType {
        JACKSON,
        GSON
    }
}
