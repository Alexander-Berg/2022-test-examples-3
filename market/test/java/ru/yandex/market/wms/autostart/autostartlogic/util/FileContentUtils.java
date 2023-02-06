package ru.yandex.market.wms.autostart.autostartlogic.util;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import org.apache.commons.io.IOUtils;

import static java.lang.ClassLoader.getSystemResourceAsStream;

public final class FileContentUtils {

    private FileContentUtils() {
        throw new AssertionError();
    }

    public static String getFileContent(String fileName) {
        try {
            return IOUtils.toString(
                    Objects.requireNonNull(
                            getSystemResourceAsStream(fileName)),
                    StandardCharsets.UTF_8).trim();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
