package ru.yandex.market.ff4shops.util;

import javax.annotation.Nonnull;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class JsonTestUtil {
    private static final JsonParser PARSER = new JsonParser();

    /**
     * Получить {@link JsonElement} из строки.
     *
     * @param json json-строка
     */
    @Nonnull
    public static JsonElement parseJson(String json) {
        return PARSER.parse(json);
    }
}
