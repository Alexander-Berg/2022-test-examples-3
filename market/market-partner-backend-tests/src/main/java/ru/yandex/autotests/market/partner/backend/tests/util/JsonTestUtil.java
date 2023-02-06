package ru.yandex.autotests.market.partner.backend.tests.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;

import javax.annotation.Nonnull;

import com.google.common.io.CharStreams;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.apache.commons.lang3.StringUtils;

import ru.yandex.autotests.market.common.http.response.BackendResponse;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class JsonTestUtil {
    private static final JsonParser PARSER = new JsonParser();

    private JsonTestUtil() {
        throw new UnsupportedOperationException("Cannot instantiate an util class");
    }

    public static void assertEquals(BackendResponse response, Class contextClass, String jsonFileName) {
        assertThat(response.getStatusCode(), equalTo(200));
        JsonElement actual = parseJson(new String(response.getBodyAsByteArray(), Charset.defaultCharset()))
                .getAsJsonObject()
                .get("result");
        JsonElement expectedResult = parseJson(contextClass, jsonFileName);
        assertThat(actual, equalTo(expectedResult));
    }

    @Nonnull
    public static JsonElement parseJson(Class contextClass, String jsonFileName) {
        return PARSER.parse(getString(contextClass, jsonFileName));
    }

    @Nonnull
    public static String getString(Class contextClass, String fileName) {
        try (InputStream in = contextClass.getResourceAsStream(fileName)) {
            if (in == null) {
                throw new IllegalArgumentException(
                        String.format("Failed to find file with name: \"%s\" at classpath for \"%s\"",
                                fileName,
                                contextClass.getSimpleName()));
            }
            return getString(in);
        } catch (final IOException ex) {
            throw new UncheckedIOException("Could not read file " + fileName, ex);
        }
    }

    @Nonnull
    public static JsonElement parseJson(String json) {
        return PARSER.parse(json);
    }

    @Nonnull
    public static String getString(InputStream inputStream) {
        try (Reader reader = new InputStreamReader(inputStream)) {
            return StringUtils.trimToNull(CharStreams.toString(reader));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
