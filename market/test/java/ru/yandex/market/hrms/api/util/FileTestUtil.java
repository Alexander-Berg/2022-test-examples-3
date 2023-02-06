package ru.yandex.market.hrms.api.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import javax.annotation.Nonnull;

/**
 * @author magomedovgh
 */
public class FileTestUtil {

    /**
     * Получить строку из файла.
     *
     * @param contextClass класс, чей класслоадер может загрузить файл.
     * @param fileName     имя файла
     */
    @Nonnull
    public static byte[] loadFileAsBytes(Class contextClass, String fileName) {
        try (InputStream in = contextClass.getResourceAsStream(fileName)) {
            if (in == null) {
                throw new IllegalArgumentException(
                        String.format("Failed to find file with name: \"%s\" at classpath for \"%s\"",
                                fileName,
                                contextClass.getSimpleName()
                        )
                );
            }
            return in.readAllBytes();
        } catch (final IOException ex) {
            throw new UncheckedIOException("Could not read file " + fileName, ex);
        }
    }
}
