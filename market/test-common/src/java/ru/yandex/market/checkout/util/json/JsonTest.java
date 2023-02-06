package ru.yandex.market.checkout.util.json;

import java.text.ParseException;

import org.hamcrest.Matcher;
import org.springframework.test.util.JsonPathExpectationsHelper;

public abstract class JsonTest {
    private JsonTest() {
        throw new UnsupportedOperationException();
    }

    public static void checkJson(String json, String path, Object value) {
        new JsonPathExpectationsHelper(path).assertValue(json, value);
    }

    public static <T> void checkJsonMatcher(String json, String path, Matcher<T> matcher) {
        new JsonPathExpectationsHelper(path).assertValue(json, matcher);
    }

    public static void checkJson(String json, String path, JsonConsumer handler) throws ParseException {
        handler.accept(new JsonPathExpectationsHelper(path), json);
    }

    public static void checkJsonNotExist(String json, String path) {
        new JsonPathExpectationsHelper(path).doesNotExist(json);
    }

    public static <T> void checkJsonMatcher(String json, String path, Matcher<T> matcher, Class<T> targetType) {
        new JsonPathExpectationsHelper(path).assertValue(json, matcher, targetType);
    }

    @FunctionalInterface
    public interface JsonConsumer {

        void accept(JsonPathExpectationsHelper helper, String json) throws ParseException;
    }
}
