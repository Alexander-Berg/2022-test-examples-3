package ru.yandex.market.dynamic.feature;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import ru.yandex.market.dynamic.JsonFileGenerator;

/**
 * Вспомогательный класс для чтения dynamic файлов для быстроотключений фич.
 * Ожидается json-файл формата:
 * [
 * 1234,
 * 5678,
 * 9012
 * ]
 *
 * @see JsonFileGenerator
 */
public class DynamicJsonReader {

    private DynamicJsonReader() {
    }

    public static List<Long> readFile(File disabledFeatureFile) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return (List<Long>) objectMapper.readValue(disabledFeatureFile, List.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
