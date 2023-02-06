package ru.yandex.market.common.excel.out.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Objects;

import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.excel.InternalColumnName;
import ru.yandex.market.common.excel.out.ExcelWriter;
import ru.yandex.market.common.excel.out.WriterBuilder;
import ru.yandex.market.common.excel.wrapper.PoiWorkbook;

/**
 * Тесты на {@link HyperlinkColumnsConsumer проставление гиперссылок}.
 *
 * @author fbokovikov
 */
class HyperlinkColumnsConsumerTest {

    @DisplayName("Проверка преобразования market-sku-url в гиперссылку")
    @Test
    void accept_setMarketSkuUrl_hyperlink() throws IOException {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("xls/supplier-feed.xls")) {
            ExcelWriter writer = new WriterBuilder()
                    .withWorkbookOpener(() -> PoiWorkbook.load(Objects.requireNonNull(inputStream)))
                    .withProcessor(
                            new HyperlinkColumnsConsumer(
                                    Collections.singleton(new InternalColumnName("market-sku-url"))
                            )
                    ).build();

            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            writer.write(buf);

            Workbook sheets = WorkbookFactory.create(new ByteArrayInputStream(buf.toByteArray()));
            Sheet sheet = sheets.getSheet("Ассортимент");
            ExcelAssert.assertColumnFontStyle(sheets, sheet, 3, 12, XSSFFont.U_SINGLE,
                    HSSFColor.HSSFColorPredefined.BLUE.getIndex());
        }
    }
}
