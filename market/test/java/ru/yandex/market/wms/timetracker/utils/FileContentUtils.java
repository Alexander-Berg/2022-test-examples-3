package ru.yandex.market.wms.timetracker.utils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import org.apache.commons.io.IOUtils;

import static java.lang.ClassLoader.getSystemResourceAsStream;

public class FileContentUtils {
    private FileContentUtils() {
        throw new AssertionError();
    }

    public static String getFileContent(String fileName) {
        try {
            final InputStream resourceAsStream = getSystemResourceAsStream(fileName);
            Objects.requireNonNull(resourceAsStream);
            return IOUtils.toString(resourceAsStream, StandardCharsets.UTF_8.name()).trim();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
