package ru.yandex.market.deepdive.utils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import javax.annotation.Nonnull;

import org.apache.commons.io.IOUtils;

import static java.lang.ClassLoader.getSystemResourceAsStream;

public class IntegrationTestUtils {
    private IntegrationTestUtils() {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    public static InputStream inputStreamFromResource(String relativePath) {
        return Objects.requireNonNull(getSystemResourceAsStream(relativePath));
    }

    @Nonnull
    public static String extractFileContent(String relativePath) {
        try (InputStream inputStream = inputStreamFromResource(relativePath)) {
            return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Error during reading from file " + relativePath, e);
        }
    }
}
