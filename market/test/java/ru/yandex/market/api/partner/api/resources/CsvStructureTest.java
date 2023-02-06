package ru.yandex.market.api.partner.api.resources;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;

/**
 * Тест проверяет, что в csv-файлах количество столбцов в каждой строке совпадает с количеством полей заголовка.
 *
 * @author serenitas
 */
class CsvStructureTest {
    private static final String API_RESOURCES_CSV = "shops_web/api_resources/api_resources.csv";
    private static final String API_RESOURCES_GROUP_CSV = "shops_web/api_resources/api_resources_group.csv";

    private static Stream<Arguments> arguments() {
        return Stream.of(
                Arguments.of(API_RESOURCES_CSV, ","),
                Arguments.of(API_RESOURCES_GROUP_CSV, ";")
        );
    }

    @ParameterizedTest
    @MethodSource("arguments")
    void testCsvStructure(String filepath, String delimiter) {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new ClassPathResource(filepath).getInputStream()))) {
            String line;
            int fieldsCount = -1;
            int lineNumber = 1;
            while ((line = br.readLine()) != null) {
                int count = StringUtils.countOccurrencesOf(line, delimiter) + 1;
                if (fieldsCount < 0) {
                    fieldsCount = count;
                } else if (fieldsCount != count) {
                    Assertions.fail(String.format("Количество полей в строке %d файла %s не совпадает с количеством столбцов в заголовке! Ожидалось %d, получено %d",
                            lineNumber, FilenameUtils.getName(filepath), fieldsCount, count));
                }
                lineNumber++;
            }
        } catch (IOException e) {
            Assertions.fail("Unable to load csv file " + FilenameUtils.getName(filepath));
        }
    }
}
