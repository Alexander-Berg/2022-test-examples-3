package ru.yandex.market.logistics.cs;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import javax.annotation.Nonnull;

import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Condition;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.json.JSONException;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static org.junit.jupiter.params.ParameterizedTest.INDEX_PLACEHOLDER;

@ExtendWith(SoftAssertionsExtension.class)
@ExtendWith(MockitoExtension.class)
public abstract class AbstractTest {
    public static final String TUPLE_PARAMETERIZED_DISPLAY_NAME = "[" + INDEX_PLACEHOLDER + "] {0}";

    @InjectSoftAssertions
    protected SoftAssertions softly;

    @Nonnull
    public String extractFileContent(String relativePath) {
        try (InputStream inputStream = Objects.requireNonNull(getSystemResourceAsStream(relativePath))) {
            return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Error during reading from file " + relativePath, e);
        }
    }

    public Condition<? super String> getCondition(String expectedJson, JSONCompareMode compareMode) {
        return new Condition<>() {
            @Override
            public boolean matches(String actualJson) {
                try {
                    JSONAssert.assertEquals(expectedJson, actualJson, compareMode);
                    return true;
                } catch (JSONException e) {
                    return false;
                }
            }
        };
    }
}
