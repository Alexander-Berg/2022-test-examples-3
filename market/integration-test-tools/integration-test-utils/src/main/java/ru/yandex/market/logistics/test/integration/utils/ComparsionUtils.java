package ru.yandex.market.logistics.test.integration.utils;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONCompareResult;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.ElementSelectors;

public final class ComparsionUtils {

    private ComparsionUtils() {
        throw new UnsupportedOperationException();
    }

    public static Diff compareXml(String expected, String actual) {
        return createDiffBuilder(expected).withTest(actual).build();
    }

    public static boolean isXmlEquals(String expected, String actual) {
        return !createDiffBuilder(expected).withTest(actual).build().hasDifferences();
    }

    public static boolean isJsonEquals(String expected, String actual) {
        return compareJsonStrictly(expected, actual).passed();
    }

    public static DiffBuilder createDiffBuilder(String expected) {
        return DiffBuilder.compare(expected)
            .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndText))
            .ignoreWhitespace()
            .ignoreComments()
            .checkForSimilar();
    }

    public static JSONCompareResult compareJson(String expected, String actual, JSONCompareMode mode) {
        try {
            return JSONCompare.compareJSON(expected, actual, mode);
        } catch (JSONException e) {
            JSONCompareResult result = new JSONCompareResult();
            result.fail(stackTraceAsString(e));
            return result;
        }
    }

    private static String stackTraceAsString(Throwable e) {
        StringWriter stringWriter = new StringWriter();
        e.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }

    public static JSONCompareResult compareJsonStrictly(String expected, String actual) {
        return compareJson(expected, actual, JSONCompareMode.STRICT);
    }
}
