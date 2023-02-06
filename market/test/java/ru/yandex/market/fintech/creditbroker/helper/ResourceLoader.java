package ru.yandex.market.fintech.creditbroker.helper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import org.apache.commons.io.IOUtils;

public class ResourceLoader {
    private ResourceLoader() {
    }

    public static String loadResourceAsString(String resourceName) {
        return loadResourceAsString(resourceName, StandardCharsets.UTF_8.name());
    }

    public static String loadResourceAsString(String resourceName, String charsetName) {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        InputStream inputStream = classloader.getResourceAsStream(resourceName);
        try {
            return IOUtils.toString(Objects.requireNonNull(inputStream), charsetName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
