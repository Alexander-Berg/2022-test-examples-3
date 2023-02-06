package ru.yandex.market.pricelabs.misc;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ToJsonStringTest {

    @Test
    void testString() {
        assertEquals("\"\"", ToJsonString.wrap("").toJsonString());
    }

    @Test
    void testMap() {
        assertEquals("{}", ToJsonString.wrap(Map.of()).toJsonString());
    }

    @Test
    void testStableMap() {
        var object = Utils.stableMap("name", "test");
        assertEquals("{\"name\":\"test\"}", ToJsonString.wrap(object).toJsonString());
    }

    @Test
    void testStableMap2() {
        var object = Utils.stableMap("name", "test", "key", 2);
        assertEquals("{\"name\":\"test\",\"key\":2}", ToJsonString.wrap(object).toJsonString());
    }

}
