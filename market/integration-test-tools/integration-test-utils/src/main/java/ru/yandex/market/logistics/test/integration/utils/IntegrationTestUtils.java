package ru.yandex.market.logistics.test.integration.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

import javax.annotation.Nonnull;

import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Assertions;
import org.json.JSONException;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static org.mockito.ArgumentMatchers.refEq;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

public final class IntegrationTestUtils {

    private IntegrationTestUtils() {
        throw new UnsupportedOperationException();
    }

    /**
     * Nonnull {@link org.mockito.ArgumentMatchers#refEq(Object, String...)}.
     */
    @Nonnull
    public static <T> T safeRefEq(T value, String... excludeFields) {
        refEq(value, excludeFields);
        return value;
    }

    @Nonnull
    public static ResultMatcher errorMessage(String value) {
        return jsonPath("message").value(value);
    }

    @Nonnull
    public static ResultMatcher missingParameter(String field, String type) {
        return errorMessage(String.format(
            "Required request parameter '%s' for method parameter type %s is not present",
            field,
            type
        ));
    }

    @Nonnull
    public static ResultMatcher nullParameter(String field, String type) {
        return errorMessage(String.format(
            "Required request parameter '%s' for method parameter type %s is present but converted to null",
            field,
            type
        ));
    }

    @Nonnull
    public static ResultMatcher jsonContent(String path) {
        return jsonContent(path, true);
    }

    @Nonnull
    public static ResultMatcher jsonContent(String path, boolean strict) {
        return MockMvcResultMatchers.content().json(extractFileContent(path), strict);
    }

    @Nonnull
    public static RequestMatcher jsonRequestContent(String path) {
        return MockRestRequestMatchers.content().json(extractFileContent(path), true);
    }

    @Nonnull
    public static ResultMatcher jsonContent(String expectedPath, String... ignoringFields) {
        return jsonContent(expectedPath, JSONCompareMode.STRICT, ignoringFields);
    }

    @Nonnull
    public static ResultMatcher jsonContent(String expectedPath, JSONCompareMode mode, String... ignoringFields) {
        return r -> assertJson(
            expectedPath,
            new String(r.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8),
            mode,
            ignoringFields
        );
    }

    @Nonnull
    public static RequestMatcher queryParam(final String name, final String... expectedValues) {
        return request -> {
            MultiValueMap<String, String> params = getQueryParams(request);
            Assertions.assertThat(params.get(name)).containsExactlyInAnyOrder(expectedValues);
        };
    }

    @Nonnull
    public static MultiValueMap<String, String> getQueryParams(ClientHttpRequest request) {
        return UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams();
    }

    public static void assertJson(String expectedPath, String content, String... ignoringFields) throws JSONException {
        assertJson(expectedPath, content, JSONCompareMode.STRICT, ignoringFields);
    }

    public static void assertJson(
        String expectedPath,
        String content,
        JSONCompareMode mode,
        String... ignoringFields
    ) throws JSONException {
        JSONAssert.assertEquals(
            extractFileContent(expectedPath),
            content,
            new CustomComparator(
                mode,
                Arrays.stream(ignoringFields)
                    .map(fieldName -> new Customization(fieldName, (o1, o2) -> true))
                    .toArray(Customization[]::new)
            )
        );
    }

    @Nonnull
    public static ResultMatcher noContent() {
        return MockMvcResultMatchers.content().string("");
    }

    @Nonnull
    public static String extractFileContent(String relativePath) {
        try (InputStream inputStream = inputStreamFromResource(relativePath)) {
            return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Error during reading from file " + relativePath, e);
        }
    }

    @Nonnull
    public static byte[] extractFileContentInBytes(String relativePath) {
        try (InputStream inputStream = inputStreamFromResource(relativePath)) {
            return IOUtils.toByteArray(inputStream);
        } catch (Exception e) {
            throw new RuntimeException("Error during reading from file " + relativePath, e);
        }
    }

    @Nonnull
    public static InputStream inputStreamFromResource(String relativePath) {
        return Objects.requireNonNull(getSystemResourceAsStream(relativePath));
    }

    @Nonnull
    public static ResultMatcher binaryContent(String path) throws IOException {
        return MockMvcResultMatchers.content()
            .bytes(IOUtils.toByteArray(Objects.requireNonNull(getSystemResourceAsStream(path))));
    }

    @Nonnull
    public static StatusResultMatchersExtended status() {
        return new StatusResultMatchersExtended();
    }
}
