package ru.yandex.market.replenishment.autoorder.utils;

import java.io.ByteArrayInputStream;

import lombok.SneakyThrows;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import static org.junit.Assert.assertEquals;

public class ExcelCellValidator {

    private final Workbook workbook;

    @SneakyThrows
    public ExcelCellValidator(byte[] contentAsByteArray) {
        this.workbook = WorkbookFactory.create(new ByteArrayInputStream(contentAsByteArray));
    }

    public void assertThatTextEquals(Integer rowNumber, Integer cellNumber, String expectedMessage) {
        Sheet sheet = workbook.getSheetAt(NumberUtils.INTEGER_ZERO);
        Row row = sheet.getRow(rowNumber);
        Cell cell = row.getCell(cellNumber);
        String stringCellValue = cell.getStringCellValue();
        assertEquals(expectedMessage, stringCellValue);
    }

}
