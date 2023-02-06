package ru.yandex.market.deliverycalculator.test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.apache.commons.io.IOUtils;

import static java.lang.ClassLoader.getSystemResourceAsStream;

public final class TestUtils {
    private TestUtils() {
        throw new UnsupportedOperationException();
    }

    public static String extractFileContent(Class<?> clazz, String relativePath) {
        return extractContent(relativePath, clazz.getResourceAsStream(relativePath));
    }

    public static String extractFileContent(String relativePath) {
        return extractContent(relativePath, getSystemResourceAsStream(relativePath));
    }

    private static String getString(String relativePath, InputStream stream) {
        try {
            return IOUtils.toString(stream, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Error during reading from file " + relativePath, e);
        }
    }

    private static String extractContent(String path, InputStream resourceAsStream) {
        try {
            return Optional.ofNullable(resourceAsStream)
                    .map(stream -> getString(path, stream))
                    .orElseThrow(() -> new RuntimeException("Error during reading from file " + path));
        } catch (Exception e) {
            throw new RuntimeException("Error during reading from file " + path, e);
        }
    }

}
