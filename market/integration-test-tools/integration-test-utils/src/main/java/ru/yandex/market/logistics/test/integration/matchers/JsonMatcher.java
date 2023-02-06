package ru.yandex.market.logistics.test.integration.matchers;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONCompareResult;

import ru.yandex.market.logistics.test.integration.utils.ComparsionUtils;

public class JsonMatcher extends TypeSafeMatcher<String> {

    private final String expected;
    private JSONCompareMode mode = JSONCompareMode.STRICT;

    public JsonMatcher(String expected) {
        this.expected = expected;
    }

    public JsonMatcher(String expected, JSONCompareMode mode) {
        this(expected);
        this.mode = mode;
    }

    @Override
    protected boolean matchesSafely(String actual) {
        return compareJson(actual)
            .passed();
    }


    @Override
    protected void describeMismatchSafely(String item, Description mismatchDescription) {
        mismatchDescription.appendText("was ")
            .appendValue(item)
            .appendText(" Problems : ").appendValue(compareJson(item));
    }

    private JSONCompareResult compareJson(String item) {
        return ComparsionUtils
            .compareJson(expected, item, mode);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("Json string equals: " + expected);
    }
}
