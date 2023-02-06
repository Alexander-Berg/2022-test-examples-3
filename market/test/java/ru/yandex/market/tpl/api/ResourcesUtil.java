package ru.yandex.market.tpl.api;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;

import static java.lang.ClassLoader.getSystemResourceAsStream;

public class ResourcesUtil {
    private ResourcesUtil() {
        // utility class
    }

    @SneakyThrows
    public static String getFileContent(String filename) {
        return IOUtils.toString(Objects.requireNonNull(getSystemResourceAsStream(filename)), StandardCharsets.UTF_8);
    }
}
