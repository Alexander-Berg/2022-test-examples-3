package ru.yandex.market.wms.common.spring.utils;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;

import ru.yandex.market.wms.common.spring.matchers.IgnoreValueMatcher;

import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

public final class JsonAssertUtils {

    private JsonAssertUtils() {
        throw new AssertionError();
    }

    public static void assertFileNonExtensibleEquals(@Nonnull String expectedJsonFileName, @Nonnull String actualJson) {
        try {
            JSONAssert.assertEquals(getFileContent(expectedJsonFileName), actualJson, JSONCompareMode.NON_EXTENSIBLE);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public static void assertFileNonExtensibleEquals(@Nonnull String expectedJsonFileName, @Nonnull String actualJson,
                                                     JSONCompareMode compareMode) {
        try {
            JSONAssert.assertEquals(getFileContent(expectedJsonFileName), actualJson, compareMode);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public static void assertFileEquals(@Nonnull String expectedJsonFileName,
                                        @Nonnull String actualJson,
                                        @Nonnull JSONCompareMode mode) {
        try {
            String expectedJson = getFileContent(expectedJsonFileName);
            if (StringUtils.isBlank(expectedJson) && StringUtils.isBlank(actualJson)) {
                return;
            }

            JSONAssert.assertEquals(expectedJson, actualJson, mode);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public static void assertFileNonExtensibleEquals(
            @Nonnull String expectedJsonFileName, @Nonnull String actualJson, @Nonnull List<String> ignoreFields) {
        try {
            Customization[] customizations = ignoreFields.stream().map(ignoreField -> new Customization(ignoreField,
                    new IgnoreValueMatcher())).toArray(Customization[]::new);
            JSONAssert.assertEquals(
                    getFileContent(expectedJsonFileName),
                    actualJson,
                    new CustomComparator(JSONCompareMode.LENIENT,
                            customizations)
            );
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public static void assertFileNonExtensibleEqualsExceptId(
            @Nonnull String expectedJsonFileName, @Nonnull String actualJson) {
        assertFileNonExtensibleEquals(expectedJsonFileName, actualJson, Collections.singletonList("[*].id"));
    }

    public static void assertFileLenientEquals(
            @Nonnull String expectedJsonFileName, @Nonnull String actualJson, @Nonnull List<String> ignoreFields) {
        try {
            Customization[] customizations = ignoreFields.stream().map(ignoreField -> new Customization(ignoreField,
                    new IgnoreValueMatcher())).toArray(Customization[]::new);
            JSONAssert.assertEquals(
                    getFileContent(expectedJsonFileName),
                    actualJson,
                    new CustomComparator(JSONCompareMode.LENIENT,
                            customizations)
            );
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}
