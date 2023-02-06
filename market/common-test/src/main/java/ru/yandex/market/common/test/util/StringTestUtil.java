package ru.yandex.market.common.test.util;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Утилиты для работы со строками в тестах.
 * Чтение в {@code UTF_8}.
 *
 * @author ivmelnik
 * @since 22.11.17
 */
public final class StringTestUtil {

    private StringTestUtil() {
        throw new UnsupportedOperationException("Cannot instantiate an util class");
    }

    /**
     * Получить строку из файла.
     *
     * @param contextClass класс, чей класслоадер может загрузить файл.
     * @param fileName     имя файла
     */
    @Nonnull
    public static String getString(Class contextClass, String fileName) {
        try (InputStream in = contextClass.getResourceAsStream(fileName)) {
            if (in == null) {
                throw new IllegalArgumentException(
                        String.format("Failed to find file with name: \"%s\" at classpath for \"%s\"",
                                fileName,
                                contextClass.getSimpleName()
                        )
                );
            }
            return getString(in);
        } catch (final IOException ex) {
            throw new UncheckedIOException("Could not read file " + fileName, ex);
        }
    }

    /**
     * Получить строку из входного потока.
     *
     * @param inputStream входной поток
     */
    @Nonnull
    public static String getString(InputStream inputStream) {
        try {
            return StringUtils.trimToNull(IOUtils.toString(inputStream, StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
