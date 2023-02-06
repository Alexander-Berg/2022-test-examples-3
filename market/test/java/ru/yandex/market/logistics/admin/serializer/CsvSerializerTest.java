package ru.yandex.market.logistics.admin.serializer;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import lombok.Value;
import org.apache.commons.lang.ArrayUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.front.library.annotation.FieldOrder;
import ru.yandex.market.logistics.front.library.dto.Mode;
import ru.yandex.market.logistics.front.library.dto.grid.GridData;
import ru.yandex.market.logistics.front.library.util.ViewUtils;

import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContentInBytes;

class CsvSerializerTest extends AbstractTest {
    private static final byte[] BOM_BYTES = new byte[]{-17, -69, -65};

    private static final GridData GRID_DATA = ViewUtils.getGridView(
        Arrays.asList(
            new TestFrontDto(1L, "name1", Collections.singletonList(1L), 1, true),
            new TestFrontDto(2L, "name2", Arrays.asList(1L, 2L), 2, false)
        ),
        2L,
        Mode.VIEW
    );

    private final GridDataSerializer<CsvSerializationSettings> csvSerializer = new CsvSerializer();

    @Test
    @DisplayName("Получить csv-файл из GridData")
    void formatOrderGridDtoTest() throws IOException {
        softly.assertThat(csvSerializer.serialize(GRID_DATA, TestFrontDto.class))
            .isEqualTo(prependFileWithBom("csv/data.csv"));
    }

    @Test
    @DisplayName("Получить csv-файл из GridData без BOM")
    void formatOrderGridDtoWithoutBomTest() throws IOException {
        softly.assertThat(csvSerializer.serialize(
            GRID_DATA,
            TestFrontDto.class,
            CsvSerializationSettings.builder().bomMark(false).build()
        ))
            .isEqualTo(extractFileContentInBytes("csv/data.csv"));
    }

    @Test
    @DisplayName("Использование запятой в качестве разделителя")
    void formatWithComma() throws IOException {
        softly.assertThat(csvSerializer.serialize(
            GRID_DATA,
            TestFrontDto.class,
            CsvSerializationSettings.builder().separator(CsvSerializationSettings.COMMA).build()
        ))
            .isEqualTo(prependFileWithBom("csv/data_comma_separated.csv"));
    }

    @Test
    @DisplayName("Без использования кавычек")
    void formatWithoutQuotes() throws IOException {
        softly.assertThat(csvSerializer.serialize(
            GRID_DATA,
            TestFrontDto.class,
            CsvSerializationSettings.builder().quote(CsvSerializationSettings.NO_SYMBOL).build()
        ))
            .isEqualTo(prependFileWithBom("csv/data_without_escape.csv"));
    }

    @Test
    @DisplayName("Получить csv-файл с именами полей в заголовках")
    void useFieldsNames() throws IOException {
        softly.assertThat(csvSerializer.serialize(
                GRID_DATA,
                TestFrontDto.class,
                CsvSerializationSettings.builder().usedColumnName(CsvSerializationSettings.UsedColumnName.NAME).build()
            ))
            .isEqualTo(prependFileWithBom("csv/data_field_names_headers.csv"));
    }

    @Nonnull
    private byte[] prependFileWithBom(String filePath) {
        return ArrayUtils.addAll(BOM_BYTES, extractFileContentInBytes(filePath));
    }

    @Value
    @FieldOrder({"name", "active", "length"})
    private static class TestFrontDto {
        @ru.yandex.market.logistics.front.library.annotation.DisplayName("Идентификатор")
        Long id;
        @ru.yandex.market.logistics.front.library.annotation.DisplayName("Имя")
        String name;
        @ru.yandex.market.logistics.front.library.annotation.DisplayName("Какой-то список")
        List<Long> list;
        @ru.yandex.market.logistics.front.library.annotation.DisplayName("Длина")
        int length;
        @ru.yandex.market.logistics.front.library.annotation.DisplayName("Признак активности")
        boolean active;
    }
}
