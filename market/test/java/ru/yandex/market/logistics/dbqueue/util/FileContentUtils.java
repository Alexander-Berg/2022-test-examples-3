package ru.yandex.market.logistics.dbqueue.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import org.apache.commons.io.IOUtils;

public class FileContentUtils {

    private FileContentUtils() {

    }

    public static String getFileContent(String fileName) throws IOException {
        return IOUtils.toString(
                Objects.requireNonNull(ClassLoader.getSystemResourceAsStream(fileName)),
                StandardCharsets.UTF_8
        ).trim();
    }
}
