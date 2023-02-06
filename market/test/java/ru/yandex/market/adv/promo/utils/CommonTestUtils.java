package ru.yandex.market.adv.promo.utils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;

public final class CommonTestUtils {
    private static final ObjectMapper MAPPER =
            new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private CommonTestUtils() {
    }

    /**
     * Получает ресурс для теста. Добавляет имя класса в путь то файла с ресурсом
     *
     * @param testClass класс с тестами, для которого нужно получить ресурс
     * @param filename путь до файла с ресурсом (без имени класса в пути)
     */
    public static String getResource(Class<?> testClass, String filename) {
        try {
            return IOUtils.toString(
                    testClass.getResourceAsStream(testClass.getSimpleName() + "/" + filename),
                    StandardCharsets.UTF_8
            );
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to get test resource: " + filename, e);
        }
    }

    /**
     * Вощвращает объект из json представления из файла.
     *
     * @param testClass    класс с тестами, для которого нужно получить ресурс
     * @param jsonFilePath пусть до json-файла (без имени класса в пути)
     * @param tClass       класс объекта, который ожидается на выходе
     */
    public static <T> T readJson(Class<?> testClass, String jsonFilePath, Class<T> tClass) throws IOException {
        return MAPPER.readValue(testClass.getResourceAsStream(testClass.getSimpleName() + "/" + jsonFilePath), tClass);
    }
}
