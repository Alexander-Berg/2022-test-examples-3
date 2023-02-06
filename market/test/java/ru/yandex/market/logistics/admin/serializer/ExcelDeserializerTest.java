package ru.yandex.market.logistics.admin.serializer;

import java.io.InputStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.experimental.Accessors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.inputStreamFromResource;

@DisplayName("Парсер excel файла")
class ExcelDeserializerTest extends AbstractTest {
    private final GridDataParser gridDataParser = new ExcelParser(new ObjectMapper());

    @Data
    @Accessors(chain = true)
    public static class ThreeColumnsDto {
        @ru.yandex.market.logistics.front.library.annotation.DisplayName("Колонка Character")
        private Character characterColumn;

        @ru.yandex.market.logistics.front.library.annotation.DisplayName("Колонка Long")
        private Long longColumn;

        @ru.yandex.market.logistics.front.library.annotation.DisplayName("Колонка String")
        private String stringColumn;
    }

    @Data
    @Accessors(chain = true)
    public static class TwoColumnsDto {
        @ru.yandex.market.logistics.front.library.annotation.DisplayName("Колонка Character")
        private Character characterColumn;

        @ru.yandex.market.logistics.front.library.annotation.DisplayName("Колонка Long")
        private Long longColumn;
    }

    @Test
    @DisplayName("Парсинг валидного excel файла")
    void parseValidExcelFile() {
        InputStream inputStream = inputStreamFromResource("excel/valid_excel.xlsx");
        softly.assertThat(gridDataParser.parse(inputStream, ThreeColumnsDto.class)).containsExactly(
            new ThreeColumnsDto().setCharacterColumn('A').setLongColumn(1921L).setStringColumn("test-string-1"),
            new ThreeColumnsDto().setCharacterColumn('B').setLongColumn(1922L).setStringColumn("test-string-2"),
            new ThreeColumnsDto().setCharacterColumn('C').setLongColumn(1923L).setStringColumn("test-string-3")
        );
    }

    @Test
    @DisplayName("Неизвестное название заголовка колонки")
    void unknownHeaderTitle() {
        InputStream inputStream = inputStreamFromResource("excel/valid_excel.xlsx");
        softly.assertThat(gridDataParser.parse(inputStream, TwoColumnsDto.class)).containsExactly(
            new TwoColumnsDto().setCharacterColumn('A').setLongColumn(1921L),
            new TwoColumnsDto().setCharacterColumn('B').setLongColumn(1922L),
            new TwoColumnsDto().setCharacterColumn('C').setLongColumn(1923L)
        );
    }

    @Test
    @DisplayName("Значение в ячейке колонки без заголовка")
    void noHeaderTitleAtColumn() {
        InputStream inputStream = inputStreamFromResource("excel/no_header_title_at_column.xlsx");
        softly.assertThat(gridDataParser.parse(inputStream, TwoColumnsDto.class)).containsExactly(
            new TwoColumnsDto().setCharacterColumn('A').setLongColumn(1921L),
            new TwoColumnsDto().setCharacterColumn('B').setLongColumn(1922L),
            new TwoColumnsDto().setCharacterColumn('C').setLongColumn(1923L)
        );
    }

    @Test
    @DisplayName("Значения в столбце были удалены, но ячейки остались пустыми")
    void emptyColumn() {
        InputStream inputStream = inputStreamFromResource("excel/column_with_empty_values.xlsx");
        softly.assertThat(gridDataParser.parse(inputStream, TwoColumnsDto.class)).containsExactly(
            new TwoColumnsDto().setCharacterColumn('A').setLongColumn(1921L)
        );
    }

    @Test
    @DisplayName("Значения в строках были удалены, но ячейки остались пустыми")
    void emptyRows() {
        InputStream inputStream = inputStreamFromResource("excel/rows_with_empty_values.xlsx");
        softly.assertThat(gridDataParser.parse(inputStream, TwoColumnsDto.class)).containsExactly(
            new TwoColumnsDto().setCharacterColumn('A').setLongColumn(1921L)
        );
    }
}
