package ru.yandex.market.fulfillment.wrap.core.scenario.builder;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONAssert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.market.fulfillment.wrap.core.scenario.BuildableFunctionalTestScenario;

public class JsonMatcher extends TypeSafeMatcher<String> {

    private final String expectedJson;
    private static final Logger log = LoggerFactory.getLogger(BuildableFunctionalTestScenario.class);

    public JsonMatcher(String expectedJson) {
        this.expectedJson = expectedJson;
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
