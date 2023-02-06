package ru.yandex.market.delivery.partnerapimock.component;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonMatcherTest {

    private final JsonMatcher matcher = new JsonMatcher();

    @Test
    void simpleJsonMatches() {
        assertTrue(matcher.isJsonEquals("{\"yandexId\":\"10\"}", "{\n \"yandexId\": \"10\"\n}"));
    }

    @Test
    void simpleJsonNotMatches() {
        assertFalse(matcher.isJsonEquals("{\"yandexId\":\"10\"}", "{\n \"yandexId\": \"11\"\n}"));
    }

    @Test
    void jsonWithObjectInsideMatches() {
        assertTrue(matcher.isJsonEquals("{\"orderId\":{\"yandexId\":\"10\"}}",
            "{\n \"orderId\": {\n \"yandexId\": \"10\"\n}\n}"));
    }

    @Test
    void jsonWithObjectInsideNotMatches() {
        assertFalse(matcher.isJsonEquals("{\"orderId\":{\"yandexId\":\"10\"}}",
            "{\n \"orderId\": {\n \"yandexId\": \"11\"\n}\n}"));
    }

    @Test
    void jsonWithArrayInsideMatches() {
        assertTrue(matcher.isJsonEquals("{\"partners\":[{\"id\":146}, {\"id\":147}]}",
            "{\n \"partners\": [{\n \"id\":147\n}, {\n \"id\":146\n}]\n}"));
    }

    @Test
    void jsonWithArrayInsideNotMatches() {
        assertFalse(matcher.isJsonEquals("{\"partners\":[{\"id\":146}, {\"id\":147}]}",
            "{\n \"partners\": [{\n \"id\":146\n}, {\n \"id\":148\n}]\n}"));
    }
}
