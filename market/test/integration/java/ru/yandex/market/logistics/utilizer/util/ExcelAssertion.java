package ru.yandex.market.logistics.utilizer.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.assertj.core.api.SoftAssertions;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class ExcelAssertion {

    private final SoftAssertions softly;

    public ExcelAssertion(SoftAssertions softly) {
        this.softly = softly;
    }

    public void assertXlsx(InputStream actual, String expected) throws IOException {
        Workbook actualWorkbook = new XSSFWorkbook(actual);
        Workbook expectedWorkbook = new XSSFWorkbook(
                Objects.requireNonNull(getSystemResourceAsStream(expected),
                        "Could not find expected file: " + expected)
        );

        assertThat("Asserting that the number of sheets in workbook is valid",
                actualWorkbook.getNumberOfSheets(), equalTo(expectedWorkbook.getNumberOfSheets()));

        Sheet actualSheet = actualWorkbook.getSheetAt(0);
        Sheet expectedSheet = expectedWorkbook.getSheetAt(0);

        assertThat("Asserting that the number of the last row in sheet is valid",
                actualSheet.getLastRowNum(), equalTo(expectedSheet.getLastRowNum()));

        for (int i = 0; i < actualSheet.getLastRowNum(); i++) {
            Row actualRow = actualSheet.getRow(i);
            Row expectedRow = expectedSheet.getRow(i);

            assertRowSoftly(actualRow, expectedRow);
        }
    }

    private void assertRowSoftly(Row actualRow, Row expectedRow) {
        short actualLastCellNum = actualRow.getLastCellNum();
        short expectedLastCellNum = expectedRow.getLastCellNum();

        softly.assertThat(actualLastCellNum)
                .as("Asserting that the number of the last cell in row is valid for row: " + actualRow.getRowNum())
                .isEqualTo(expectedLastCellNum);

        if (actualLastCellNum != expectedLastCellNum) {
            return;
        }

        for (int i = 0; i < actualLastCellNum; i++) {
            Cell actualCell = actualRow.getCell(i);
            Cell expectedCell = expectedRow.getCell(i);

            assertCellSoftly(actualCell, expectedCell);
        }
    }

    private void assertCellSoftly(Cell actualCell, Cell expectedCell) {
        if (actualCell == null || expectedCell == null) {
            softly.assertThat(actualCell == null && expectedCell == null)
                    .as("Asserting that both cells are null")
                    .isTrue();
            return;
        }

        CellType actualCellType = actualCell.getCellType();
        CellType expectedCellType = expectedCell.getCellType();

        softly.assertThat(actualCellType)
                .as("Asserting that the type of the cell is valid for cell: " + actualCell.getAddress())
                .isEqualTo(expectedCellType);
        softly.assertThat(actualCell.getCellStyle())
                .as("Asserting that the style of the cell is valid for cell: " + actualCell.getAddress())
                .isEqualTo(expectedCell.getCellStyle());

        if (actualCellType != expectedCellType) {
            return;
        }

        switch (actualCellType) {
            case NUMERIC:
                softly.assertThat(actualCell.getNumericCellValue())
                        .as("Asserting that the numeric value of the cell is valid for cell: " +
                                actualCell.getAddress())
                        .isEqualTo(expectedCell.getNumericCellValue());
                break;
            case STRING:
                softly.assertThat(actualCell.getStringCellValue())
                        .as("Asserting that the string value of the cell is valid for cell: " + actualCell.getAddress())
                        .isEqualTo(expectedCell.getStringCellValue());
                break;
            case BOOLEAN:
                softly.assertThat(actualCell.getBooleanCellValue())
                        .as("Asserting that the boolean value of the cell is valid for cell: " +
                                actualCell.getAddress())
                        .isEqualTo(expectedCell.getBooleanCellValue());
                break;
            case ERROR:
                softly.assertThat(actualCell.getErrorCellValue())
                        .as("Asserting that the error value of the cell is valid for cell: " + actualCell.getAddress())
                        .isEqualTo(expectedCell.getErrorCellValue());
                break;
            default:
        }
    }
}
