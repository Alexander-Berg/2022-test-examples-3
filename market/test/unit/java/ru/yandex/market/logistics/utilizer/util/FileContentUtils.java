package ru.yandex.market.logistics.utilizer.util;

import java.io.IOException;
import java.io.UncheckedIOException;
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
    public static String getFileContent(@Nonnull String fileName) {
        try {
            return IOUtils.toString(
                    Objects.requireNonNull(
                            getSystemResourceAsStream(fileName)),
                    StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
