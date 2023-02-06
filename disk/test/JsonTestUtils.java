package ru.yandex.chemodan.util.test;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import ru.yandex.commune.json.jackson.ObjectMapperX;

/**
 * Utils for processing JSON in unit tests. These utils are not expected to be used in production code, so they
 * can be exception unsafe, or constructed to allow easier debugging at the expense of performance.
 *
 * @author buberman
 */
public class JsonTestUtils {

    private JsonTestUtils() {}

    /**
     * Parse byte array containing a JSON to a map. Can throw a runtime exception if any IOException emerges
     * during parsing.
     *
     * @param json byte array containing JSON data.
     * @return JSON top-level object parsed as a map.
     * @throws ru.yandex.misc.io.RuntimeIoException if an IOException is encountered while parsing.
     */
    @SuppressWarnings("unchecked")
    public static <T> Map<String, T> parseJsonToMap(byte[] json) {
        ObjectMapperX objectMapper = new ObjectMapperX(new ObjectMapper());
        return objectMapper.readValue(Map.class, json);
    }
}
