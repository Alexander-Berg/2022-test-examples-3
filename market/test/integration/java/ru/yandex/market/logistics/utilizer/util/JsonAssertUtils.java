package ru.yandex.market.logistics.utilizer.util;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.json.JSONException;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;

import ru.yandex.market.logistics.utilizer.util.matcher.IgnoreValueMatcher;

@ParametersAreNonnullByDefault
public final class JsonAssertUtils {

    private JsonAssertUtils() {
        throw new AssertionError();
    }

    public static void assertFileNonExtensibleEquals(String expectedJsonFileName, String actualJson) {
        try {
            JSONAssert.assertEquals(FileContentUtils.getFileContent(expectedJsonFileName), actualJson,
                    JSONCompareMode.NON_EXTENSIBLE);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public static void assertFileEquals(String expectedJsonFileName,
                                        String actualJson,
                                        JSONCompareMode mode) {
        try {
            JSONAssert.assertEquals(FileContentUtils.getFileContent(expectedJsonFileName), actualJson, mode);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public static void assertFileNonExtensibleEquals(String expectedJsonFileName,
                                                     String actualJson,
                                                     List<String> ignoreFields) {
        try {
            Customization[] customizations = ignoreFields.stream()
                    .map(ignoreField -> new Customization(ignoreField, new IgnoreValueMatcher()))
                    .toArray(Customization[]::new);

            JSONAssert.assertEquals(
                    FileContentUtils.getFileContent(expectedJsonFileName),
                    actualJson,
                    new CustomComparator(JSONCompareMode.NON_EXTENSIBLE, customizations)
            );
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}
