package ru.yandex.market.logshatter.parser.checkout.common;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.internal.util.collections.Sets;

public class KeyValueParseTest {

    private static final Byte DEF_N = -1;
    private static final ObjectMapper J_MAPPER = new ObjectMapper();
    private static final Gson G_MAPPER = new GsonBuilder().create();

    @Test
    public void gsonNumberFormatTest() {
        Assertions.assertThrows(AssertionError.class, () -> {
            Map<String, Object> map = new HashMap<>();
            map.put("bigIntValue", Integer.MAX_VALUE);
            map.put("normalIntValue", ThreadLocalRandom.current().nextInt(-256000, 256000));
            map.put("bigLongValue", Long.MAX_VALUE);
            map.put("normalLongValue", ThreadLocalRandom.current().nextLong(-256000, 256000));
            map.put("bigDecimalValue", BigInteger.valueOf(-9223372036854775808L));

            String strData = G_MAPPER.toJson(map);

            Map<String, Object> parsed = G_MAPPER.fromJson(strData, new TypeToken<Map<String, Object>>() {
            }.getType());

            testValue(parsed, "bigIntValue", Integer.class);
            testValue(parsed, "normalIntValue", Integer.class);
            testValue(parsed, "bigLongValue", Long.class);
            testValue(parsed, "normalLongValue", Integer.class, Long.class);
            testValue(parsed, "bigDecimalValue", Long.class, BigInteger.class);

            testValuesStringRepresentation(parsed.values());
        });
    }

    @Test
    public void jacksonNumberFormatTest() throws IOException {
        Map<String, Object> map = new HashMap<>();
        map.put("bigIntValue", Integer.MAX_VALUE);
        map.put("normalIntValue", ThreadLocalRandom.current().nextInt(-256000, 256000));
        map.put("bigLongValue", Long.MAX_VALUE);
        map.put("normalLongValue", ThreadLocalRandom.current().nextLong(-256000, 256000));
        map.put("bigDecimalValue", BigInteger.valueOf(-9223372036854775808L));

        String strData = J_MAPPER.writeValueAsString(map);

        Map<String, Object> parsed = J_MAPPER.readValue(strData, new TypeReference<Map<String, Object>>() {
        });

        testValue(parsed, "bigIntValue", Integer.class);
        testValue(parsed, "normalIntValue", Integer.class);
        testValue(parsed, "bigLongValue", Long.class);
        testValue(parsed, "normalLongValue", Integer.class, Long.class);
        testValue(parsed, "bigDecimalValue", Long.class, BigInteger.class);

        testValuesStringRepresentation(parsed.values());
    }

    private void testValue(Map<String, Object> source, String name, Class... acceptableTypes) {
        Object sourceValue = source.getOrDefault(name, DEF_N);
        Assertions.assertNotNull(sourceValue);
        Assertions.assertTrue(
            Sets.newSet(acceptableTypes).contains(sourceValue.getClass()),
            String.format("value %s has wrong type %s",
                String.valueOf(sourceValue), sourceValue.getClass().getSimpleName())
        );
    }

    private void testValuesStringRepresentation(Collection<Object> values) {
        Assertions.assertFalse(values.stream()
            .map(String::valueOf)
            .anyMatch(str ->
                String.valueOf(str).contains("E")
                    || String.valueOf(str).contains(".")));
    }
}
