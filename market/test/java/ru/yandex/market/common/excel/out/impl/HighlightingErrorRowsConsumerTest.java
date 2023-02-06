package ru.yandex.market.common.excel.out.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import com.google.common.collect.ImmutableList;
import org.apache.poi.hssf.util.HSSFColor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.excel.XlsSheet;
import ru.yandex.market.common.excel.out.ExcelWriter;
import ru.yandex.market.common.excel.out.WriterBuilder;
import ru.yandex.market.common.excel.wrapper.PoiSheet;
import ru.yandex.market.common.excel.wrapper.PoiWorkbook;

/**
 * Тест на {@link HighlightingErrorRowsConsumer раскраску}.
 *
 * @author fbokovikov
 */
class HighlightingErrorRowsConsumerTest {

    @DisplayName("Проверка расскраски ошибочной строки в красный цвет")
    @Test
    void accept_setError_coloredRowToRed() throws IOException {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("xls/supplier-feed.xls")) {
            ExcelWriter writer = new WriterBuilder()
                    .withWorkbookOpener(() -> PoiWorkbook.load(Objects.requireNonNull(inputStream)))
                    .withProcessor(new HighlightingErrorRowsConsumer(ImmutableList.of(1)))
                    .build();
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            writer.write(buf);
            PoiWorkbook sheets = PoiWorkbook.load(new ByteArrayInputStream(buf.toByteArray()));
            PoiSheet sheet = sheets.getSheet(XlsSheet.newBuilder().withName("Ассортимент").build());
            ExcelAssert.assertRowStyle(sheet, 4, HSSFColor.HSSFColorPredefined.RED.getIndex());
        }
    }
}
