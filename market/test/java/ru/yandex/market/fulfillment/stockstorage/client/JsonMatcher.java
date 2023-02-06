package ru.yandex.market.fulfillment.stockstorage.client;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonMatcher extends TypeSafeMatcher<String> {

    private static final Logger logger = LoggerFactory.getLogger(JsonMatcher.class);
    private final String expectedJson;
    private final JSONCompareMode compareMode;

    public JsonMatcher(String expectedJson) {
        this(expectedJson, JSONCompareMode.STRICT);
    }

    public JsonMatcher(String expectedJson, JSONCompareMode compareMode) {
        this.expectedJson = expectedJson;
        this.compareMode = compareMode;
    }

    @Override
    protected boolean matchesSafely(String actualJson) {
        try {
            JSONAssert.assertEquals(expectedJson, actualJson, compareMode);
            return true;
        } catch (JSONException e) {
            logger.error("JSON Match failed", e);
            return false;
        }
    }

    @Override
    public void describeTo(Description description) {

    }
}
