package ru.yandex.market.yql_test.utils;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class YqlJsonUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private YqlJsonUtils() {
    }

    public static String serializeJson(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("can't serialize json", e);
        }
    }

    public static <T> T deserializeJson(String json, Class<T> type) {
        try {
            return MAPPER.readValue(json, type);
        } catch (IOException e) {
            throw new IllegalStateException("can't deserialize json", e);
        }
    }
}
