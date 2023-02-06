package ru.yandex.market.checkout.pushapi.web;

import org.springframework.test.util.JsonPathExpectationsHelper;

public abstract class JsonPathUtils {
    private JsonPathUtils() {
        throw new UnsupportedOperationException();
    }

    public static JsonPathExpectationsHelper jpath(String expression) {
        return new JsonPathExpectationsHelper(expression);
    }
}
