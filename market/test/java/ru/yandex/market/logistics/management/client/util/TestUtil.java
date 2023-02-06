package ru.yandex.market.logistics.management.client.util;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

import javax.annotation.Nonnull;

import org.apache.commons.io.IOUtils;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.web.util.UriComponentsBuilder;

import static java.lang.ClassLoader.getSystemResourceAsStream;

public final class TestUtil {

    private TestUtil() {
        throw new UnsupportedOperationException();
    }

    public static String jsonResource(String relativePath) {
        try {
            return IOUtils.toString(
                Objects.requireNonNull(getSystemResourceAsStream(relativePath)),
                StandardCharsets.UTF_8
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to read " + relativePath, e);
        }
    }

    @Nonnull
    public static UriComponentsBuilder getBuilder(@Nonnull String uri, @Nonnull String root) {
        return UriComponentsBuilder
            .fromHttpUrl(uri)
            .path(root);
    }

    @Nonnull
    public static RequestMatcher jsonContent(String relativePath, boolean strict) {
        return jsonContent(relativePath, !strict, strict);
    }

    @Nonnull
    public static RequestMatcher jsonContent(String relativePath) {
        return jsonContent(relativePath, true);
    }

    @Nonnull
    public static RequestMatcher jsonContent(String relativePath, boolean extensible, boolean strictOrder) {
        return request -> {
            try {
                String expectedJsonContent = jsonResource(relativePath);
                String actualJsonContent = ((MockClientHttpRequest) request).getBodyAsString();
                JSONCompareMode jsonCompareMode = jsonCompareModeOf(extensible, strictOrder);
                JSONAssert.assertEquals(expectedJsonContent, actualJsonContent, jsonCompareMode);
            } catch (Exception ex) {
                throw new AssertionError("Failed to parse expected or actual JSON request content", ex);
            }
        };
    }

    @Nonnull
    private static JSONCompareMode jsonCompareModeOf(boolean extensible, boolean strictOrder) {
        return Arrays.stream(JSONCompareMode.values())
            .filter(mode -> mode.hasStrictOrder() == strictOrder)
            .filter(mode -> mode.isExtensible() == extensible)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(String.format(
                "No json compare mode for parameters extensible = %s, strictOrder = %s",
                extensible,
                strictOrder
            )));
    }
}
