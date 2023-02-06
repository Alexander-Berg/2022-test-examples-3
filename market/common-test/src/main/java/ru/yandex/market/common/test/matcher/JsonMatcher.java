package ru.yandex.market.common.test.matcher;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.annotation.Nonnull;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONCompareResult;

/**
 * Матчер сравнивает на соответствие <i>JSON</i> строки.
 */
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
        mismatchDescription
            .appendText("was ")
            .appendValue(item)
            .appendText(" Problems: ")
            .appendValue(compareJson(item));
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("Json string equals: " + expected);
    }

    @Nonnull
    private JSONCompareResult compareJson(String item) {
        try {
            return JSONCompare.compareJSON(expected, item, mode);
        } catch (JSONException e) {
            JSONCompareResult result = new JSONCompareResult();
            result.fail(stackTraceAsString(e));
            return result;
        }
    }

    @Nonnull
    private String stackTraceAsString(Throwable e) {
        StringWriter stringWriter = new StringWriter();
        e.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }
}
