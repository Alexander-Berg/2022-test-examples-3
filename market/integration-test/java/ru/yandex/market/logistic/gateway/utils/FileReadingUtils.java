package ru.yandex.market.logistic.gateway.utils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.io.IOUtils;

import static java.lang.ClassLoader.getSystemResourceAsStream;

@ParametersAreNonnullByDefault
public final class FileReadingUtils {
    private FileReadingUtils() {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    public static String getFileContent(String filename) {
        try {
            InputStream resource = getSystemResourceAsStream(filename);
            if (null == resource) {
                throw new FileNotFoundException("File not found: " + filename);
            }
            return IOUtils.toString(resource, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
