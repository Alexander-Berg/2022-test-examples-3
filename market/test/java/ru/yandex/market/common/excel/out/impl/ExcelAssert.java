package ru.yandex.market.common.excel.out.impl;

import java.util.Map;
import java.util.Optional;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellReference;
import org.assertj.core.api.Assertions;

import ru.yandex.market.common.excel.wrapper.PoiCell;
import ru.yandex.market.common.excel.wrapper.PoiRow;
import ru.yandex.market.common.excel.wrapper.PoiSheet;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
@SuppressWarnings("SameParameterValue")
@ParametersAreNonnullByDefault
final class ExcelAssert {

    private ExcelAssert() {
        throw new UnsupportedOperationException();
    }

    static void assertCells(Workbook workbook, Map<String, String> expected) {
        for (Map.Entry<String, String> entry : expected.entrySet()) {
            String addr = entry.getKey();
            String expectedValue = entry.getValue();

            CellReference ref = new CellReference(addr);
            DataFormatter formatter = new DataFormatter();
            String actual = Optional.ofNullable(workbook.getSheet(ref.getSheetName()))
                    .flatMap(s -> Optional.ofNullable(s.getRow(ref.getRow())))
                    .flatMap(r -> Optional.ofNullable(r.getCell(ref.getCol())))
                    .map(formatter::formatCellValue)
                    .orElse("");

            Assertions.assertThat(actual)
                    .withFailMessage("\nCell: %s\nExpected: %s\nActual:", addr, expectedValue, actual)
                    .isEqualTo(expectedValue);
        }
    }

    static void assertRowStyle(PoiSheet sheet, int rowNum, short color) {
        PoiRow row = sheet.getRow(rowNum);
        short lastCellNum = row.getLastCellNum();
        for (int i = 0; i < lastCellNum; i++) {
            PoiCell cell = row.getCell(i);
            assertCellStyle(cell.getCellStyle(), color);
        }
    }

    static void assertColumnFontStyle(Workbook workbook, Sheet sheet, int firstRow,
                                      int columnPosition, byte underline, short color) {
        for (int i = firstRow; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            Cell cell = row.getCell(columnPosition);
            if (cell != null && cell.getHyperlink() != null) {
                CellStyle cellStyle = cell.getCellStyle();
                Font font = workbook.getFontAt(cellStyle.getFontIndexAsInt());

                Assertions.assertThat(font.getColor())
                        .isEqualTo(color);
                Assertions.assertThat(font.getUnderline())
                        .isEqualTo(underline);
            }
        }
    }

    private static void assertCellStyle(CellStyle cellStyle, short color) {
        Assertions.assertThat(cellStyle.getBottomBorderColor())
                .isEqualTo(color);
        Assertions.assertThat(cellStyle.getTopBorderColor())
                .isEqualTo(color);
        Assertions.assertThat(cellStyle.getLeftBorderColor())
                .isEqualTo(color);
        Assertions.assertThat(cellStyle.getRightBorderColor())
                .isEqualTo(color);
    }
}
