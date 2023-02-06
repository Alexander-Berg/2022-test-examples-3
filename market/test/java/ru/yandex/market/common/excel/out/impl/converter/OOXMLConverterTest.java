package ru.yandex.market.common.excel.out.impl.converter;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.common.excel.converter.impl.apache.OOXMLConverter;

@DisplayName("Тесты на конвертацию excel-документов с помощью OOXMLConverter")
class OOXMLConverterTest {

    @DisplayName("Обработка excel-документа без добавления управляющих последовательностей. " +
            "Проверка ссылок на изображения.")
    @Test
    void convert_excelDocument_correctProcessing() throws Exception {
        InputStream is = getClass().getClassLoader()
                .getResourceAsStream("xls/mbi_50710.xlsm");

        List<List<String>> csvRows = new ArrayList<>();
        OOXMLConverter.convert(is, csvRows::add, false);

        List<String> firstRow = csvRows.get(1);
        Assertions.assertThat(firstRow.get(1))
                .isEqualTo("Телевизор \\ LG \\n 43LJ515V 42.5\"");
        Assertions.assertThat(firstRow.get(4))
                .isEqualTo("http://example-shop.ru/item59.png," +
                        "http://example-shop.ru/item59_2.png,http://example-shop.ru/item59_3.png");
    }

    @DisplayName("Проверка конвертации цены в обновленном шаблоне.")
    @Test
    void convert_excelDocument_priceIsConverted() throws Exception {
        InputStream is = getClass().getClassLoader()
                .getResourceAsStream("xls/mbi_64264.xlsm");

        List<List<String>> csvRows = new ArrayList<>();
        OOXMLConverter.convert(is, csvRows::add, false);

        int columnIndex = csvRows.get(0).indexOf("price");
        List<String> firstRow = csvRows.get(1);
        Assertions.assertThat(firstRow.get(columnIndex))
                .isEqualTo("2400.00");
    }

    private static Stream<Arguments> booksAndAudiobooksExcelFiles() {
        return Stream.of(
                Arguments.of("книг", 20, "book", "xls/mbi_65288_books.xlsx"),
                Arguments.of("аудиокниг", 20, "audiobook", "xls/mbi_65288_audiobooks.xlsx"),
                Arguments.of("книг2", 11, "book", "xls/mbi_65682_suppliers_book.xlsx")
        );
    }

    @DisplayName("Проверка конвертации книг и аудиокниг.")
    @ParameterizedTest(name = "Проверка конвертации {0}.")
    @MethodSource("booksAndAudiobooksExcelFiles")
    void convert_excelDocument_bookAndAudibook(
            String name,
            int expectedColumnNum,
            String expectedCsvValue,
            String excelFilePath) throws Exception {
        InputStream is = getClass().getClassLoader()
                .getResourceAsStream(excelFilePath);

        List<List<String>> csvRows = new ArrayList<>();
        OOXMLConverter.convert(is, csvRows::add, false);

        List<String> firstRow = csvRows.get(1);
        Assertions.assertThat(firstRow.get(expectedColumnNum))
                .isEqualTo(expectedCsvValue);
    }

    @Test
    @DisplayName("Проверка конвертации с возрастной категорией.")
    void convert_excelDocument_ageUnits() throws Exception {
        InputStream is = getClass().getClassLoader()
                .getResourceAsStream("xls/mbi_64980_age.xlsx");

        List<List<String>> csvRows = new ArrayList<>();
        OOXMLConverter.convert(is, csvRows::add, false);

        List<Map<Integer, String>> expectedValues = ImmutableList.of(
                Collections.emptyMap(),
                ImmutableMap.of(12, "1", 13, "month"),
                ImmutableMap.of(12, "2", 13, "month"),
                ImmutableMap.of(12, "10", 13, "month"),
                ImmutableMap.of(12, "1", 13, "year"),
                ImmutableMap.of(12, "3", 13, "year"),
                ImmutableMap.of(12, "18", 13, "year"),
                ImmutableMap.of(12, "", 13, ""),
                ImmutableMap.of(12, "12", 13, "year")
        );

        for (int rowIndex = 0; rowIndex < expectedValues.size(); rowIndex++) {
            int finalRowIndex = rowIndex;
            expectedValues.get(rowIndex).forEach((colId, expectedValue) ->
                    Assertions.assertThat(csvRows.get(finalRowIndex).get(colId))
                            .withFailMessage("Row " + finalRowIndex + " differs.\nExpected: %s\nActual: %s",
                                    expectedValue,
                                    csvRows.get(finalRowIndex).get(colId))
                            .isEqualTo(expectedValue));
        }
    }
}
