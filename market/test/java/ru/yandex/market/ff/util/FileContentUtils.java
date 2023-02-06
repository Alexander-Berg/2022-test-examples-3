package ru.yandex.market.ff.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import javax.annotation.Nonnull;

import org.apache.commons.io.IOUtils;

import static java.lang.ClassLoader.getSystemResourceAsStream;

public final class FileContentUtils {

    private FileContentUtils() {
        throw new AssertionError();
    }

    @Nonnull
    public static String getFileContent(@Nonnull String fileName) throws IOException {
        return IOUtils.toString(
            Objects.requireNonNull(
                getSystemResourceAsStream(fileName)),
            StandardCharsets.UTF_8).trim();
    }
}
