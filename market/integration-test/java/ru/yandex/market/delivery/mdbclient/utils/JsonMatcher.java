package ru.yandex.market.delivery.mdbclient.utils;

import java.util.function.Function;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONAssert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonMatcher extends TypeSafeMatcher<String> {

    private static final Logger log = LoggerFactory.getLogger(JsonMatcher.class);

    private final String expectedJson;

    public JsonMatcher(String expectedJson) {
        this.expectedJson = expectedJson;
    }

    public static Function<String, BaseMatcher<? super String>> getMatcherFunction() {
        return JsonMatcher::new;
    }

    @Override
    protected boolean matchesSafely(String actualJson) {
        try {
            JSONAssert.assertEquals(expectedJson, actualJson, true);
            return true;
        } catch (JSONException e) {
            log.error("JSON Match failed", e);
            return false;
        }
    }

    @Override
    public void describeTo(Description description) {
    }
}
