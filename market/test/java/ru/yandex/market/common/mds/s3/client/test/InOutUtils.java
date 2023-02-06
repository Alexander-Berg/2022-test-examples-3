package ru.yandex.market.common.mds.s3.client.test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.annotation.Nonnull;

import com.amazonaws.util.StringInputStream;
import org.apache.commons.io.FileUtils;

import ru.yandex.market.common.mds.s3.client.util.TempFileUtils;

import static ru.yandex.market.common.mds.s3.client.test.TestUtils.TEST_DATA;

/**
 * Утилитный класс для работы с IO.
 *
 * @author Vladislav Bauer
 */
public final class InOutUtils {

    private InOutUtils() {
        throw new UnsupportedOperationException();
    }


    @Nonnull
    public static InputStream badInputStream() {
        return new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException();
            }
        };
    }

    @Nonnull
    public static InputStream inputStream() {
        try {
            return new StringInputStream(TEST_DATA);
        } catch (final UnsupportedEncodingException ex) {
            throw new RuntimeException("Could not create test input stream", ex);
        }
    }

    @Nonnull
    public static String readFile(@Nonnull final File file) {
        try {
            return FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        } catch (final Exception ex) {
            throw new RuntimeException(String.format("Could not read file \"%s\"", file), ex);
        }
    }

    @Nonnull
    public static String readPath(@Nonnull final Path path) {
        try {
            final byte[] bytes = Files.readAllBytes(path);
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (final Exception ex) {
            throw new RuntimeException(String.format("Could not read path \"%s\"", path), ex);
        }
    }

    @Nonnull
    public static File createTempFile() {
        try {
            final File tempFile = TempFileUtils.createTempFile();
            FileUtils.writeStringToFile(tempFile, TEST_DATA, StandardCharsets.UTF_8);
            return tempFile;
        } catch (final Exception ex) {
            throw new RuntimeException("Could not create temporary file", ex);
        }
    }

}
