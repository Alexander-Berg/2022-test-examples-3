package ru.yandex.market.checkout.referee.entity;

import java.text.ParseException;

import org.springframework.test.util.JsonPathExpectationsHelper;

/**
 * @author kukabara
 */
public abstract class JsonTest {
    private JsonTest() {
        throw new UnsupportedOperationException();
    }

    public static void checkJson(String json, String path, Object value) throws ParseException {
        new JsonPathExpectationsHelper(path).assertValue(json, value);
    }
}
