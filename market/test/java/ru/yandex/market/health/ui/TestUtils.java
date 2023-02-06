package ru.yandex.market.health.ui;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.google.common.io.Resources;

public class TestUtils {

    private TestUtils() {
    }

    public static String loadFromClasspath(String path) {
        try {
            return Resources.toString(Resources.getResource(path), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
