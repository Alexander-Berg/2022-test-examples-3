package ru.yandex.market.cocon.util;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.cocon.util.RequestUtils.mapToQueryString;

class RequestUtilsTest {

    @Test
    void testMapToQueryString() {
        String actual = mapToQueryString(Map.of("key1", new String[]{"value1", "value2"}, "key2", "value3"));
        String expected = "key1=value1&key1=value2&key2=value3";
        assertEquals(expected, actual);
    }

    @Test
    void testEmptyMapToQueryString() {
        String actual = mapToQueryString(Collections.emptyMap());
        String expected = "";
        assertEquals(expected, actual);
    }
}
