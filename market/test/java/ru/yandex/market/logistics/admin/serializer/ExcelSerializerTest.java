package ru.yandex.market.logistics.admin.serializer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import lombok.SneakyThrows;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class ExcelSerializerTest extends AbstractTest {

    private static final ExcelSerializer EXCEL_SERIALIZER = new ExcelSerializer();

    @Test
    @SneakyThrows
    @DisplayName("В файле лист с кастомным названием")
    void checkSheetCustomName() {
        byte[] generatedBytes = EXCEL_SERIALIZER.serialize(
            ImmutableMap.of("header_1", "Заголовок 1"),
            ImmutableList.of(ImmutableMap.of("header_1", "Заголовок 1")),
            ExcelSerializerSettings.builder().sheetName("Sheet1").build()
        );
        Workbook generatedWorkbook = new XSSFWorkbook(new ByteArrayInputStream(generatedBytes));
        softly.assertThat(generatedWorkbook.getSheetName(0)).isEqualTo("Sheet1");
    }

    @Test
    @SneakyThrows
    @DisplayName("В файле лист с названием по умолчанию")
    void checkSheetDefaultName() {
        byte[] generatedBytes = EXCEL_SERIALIZER.serialize(
            ImmutableMap.of("header_1", "Заголовок 1"),
            ImmutableList.of(ImmutableMap.of("header_1", "Заголовок 1"))
        );
        Workbook generatedWorkbook = new XSSFWorkbook(new ByteArrayInputStream(generatedBytes));
        softly.assertThat(generatedWorkbook.getSheetName(0)).isEqualTo("Data");
    }

    @Nonnull
    private static Workbook generateWorkbook(SheetContentSettings sheetContentSettings) throws IOException {
        return generateWorkbook(ImmutableList.of(sheetContentSettings));
    }

    @Nonnull
    private static Workbook generateWorkbook(List<SheetContentSettings> sheetContentSettings) throws IOException {
        byte[] generatedBytes = EXCEL_SERIALIZER.serialize(sheetContentSettings);
        return new XSSFWorkbook(new ByteArrayInputStream(generatedBytes));
    }


    @Test
    @SneakyThrows
    @DisplayName("В файле лист с заданным SheetContentSettings")
    void checkCustomSheetContentSettings() {
        Workbook generatedWorkbook = generateWorkbook(
            SheetContentSettings.builder()
                .sheetName("Стресс тест")
                .headers(ImmutableMap.of("header_1", "Тестовый столбец 1"))
                .rows(ImmutableList.of(ImmutableMap.of("header_1", "Тестовое значение 1")))
                .build()
        );
        softly.assertThat(generatedWorkbook.getNumberOfSheets()).isEqualTo(1);
        softly.assertThat(generatedWorkbook.getSheetName(0)).isEqualTo("Стресс тест");
        softly.assertThat(generatedWorkbook.getSheet("Стресс тест").getLastRowNum()).isEqualTo(1);
    }

    @Test
    @DisplayName("В файле два листа с одинаковым названием")
    void twoSheetsWithSameNames() {
        softly.assertThatThrownBy(
            () -> EXCEL_SERIALIZER.serialize(
                ImmutableList.of(
                    SheetContentSettings.builder()
                        .sheetName("Abacababa")
                        .headers(ImmutableMap.of("header_1", "Тестовый столбец 1"))
                        .rows(ImmutableList.of(ImmutableMap.of("header_1", "Тестовое значение 1")))
                        .build(),
                    SheetContentSettings.builder()
                        .sheetName("Abacababa")
                        .headers(ImmutableMap.of("header_1", "Тестовый столбец 1"))
                        .rows(ImmutableList.of(ImmutableMap.of("header_1", "Тестовое значение 1")))
                        .build()
                )
            ),
            "The workbook already contains a sheet named 'Abacababa'",
            IllegalArgumentException.class
        );
    }

    @Test
    @SneakyThrows
    @DisplayName("В файле два листа с разными названиями и разным содержимым")
    void twoSheetsWithDifferentNames() {
        Workbook generatedWorkbook = generateWorkbook(
            ImmutableList.of(
                SheetContentSettings.builder()
                    .sheetName("TestName1")
                    .headers(ImmutableMap.of("header_1", "Тестовый столбец 1", "header_2", "Тестовый столбец two"))
                    .rows(
                        ImmutableList.of(
                            ImmutableMap.of("header_1", "Тестовое значение 1"),
                            ImmutableMap.of("header_2", "Тестовое значение два")))
                    .build(),
                SheetContentSettings.builder()
                    .sheetName("TestName2")
                    .headers(ImmutableMap.of("header_1", "Тестовый столбец 1"))
                    .rows(ImmutableList.of(ImmutableMap.of("header_1", "Тестовое значение 1")))
                    .build()
            )
        );
        softly.assertThat(generatedWorkbook.getSheetName(0)).isEqualTo("TestName1");
        softly.assertThat(generatedWorkbook.getSheetName(1)).isEqualTo("TestName2");
        softly.assertThat(generatedWorkbook.getNumberOfSheets()).isEqualTo(2);
        softly.assertThat(generatedWorkbook.getSheet("TestName1").getLastRowNum()).isEqualTo(2);
        softly.assertThat(generatedWorkbook.getSheet("TestName2").getLastRowNum()).isEqualTo(1);
    }
}
