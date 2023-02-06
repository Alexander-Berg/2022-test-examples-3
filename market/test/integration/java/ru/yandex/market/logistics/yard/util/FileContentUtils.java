package ru.yandex.market.logistics.yard.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.annotation.Nonnull;

import org.apache.commons.io.IOUtils;
import org.springframework.util.ResourceUtils;

public final class FileContentUtils {

    private FileContentUtils() {
        throw new AssertionError();
    }

    @Nonnull
    public static String getFileContent(@Nonnull String fileName) throws IOException {
        try (InputStream input = ResourceUtils.getURL(fileName).openStream()) {
            return IOUtils.toString(input, String.valueOf(StandardCharsets.UTF_8)).trim();
        } catch (FileNotFoundException e) {
            if (!fileName.startsWith("classpath:")) {
                throw new FileNotFoundException(e.getMessage() +
                        "\n\nHint: try with prefix \"classpath:" + fileName + "\"");
            } else {
                throw e;
            }
        }
    }
}
