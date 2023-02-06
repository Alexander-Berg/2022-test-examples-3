package ru.yandex.market.logistics.cte.service;

import java.io.IOException;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import ru.yandex.market.logistics.cte.base.IntegrationTest;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public abstract class ExcelFilesComparingBaseTest extends IntegrationTest {

    public void assertXlsx(final Workbook workbook, String fileName) throws IOException {
        Workbook actualWorkbook = workbook;
        Workbook expectedWorkbook = new XSSFWorkbook(getSystemResourceAsStream("service/xlsx-report/" + fileName));
        assertThat("Asserting that the number of sheets in workbook is valid",
            actualWorkbook.getNumberOfSheets(), equalTo(expectedWorkbook.getNumberOfSheets()));

        Sheet actualSheet = actualWorkbook.getSheetAt(0);
        Sheet expectedSheet = expectedWorkbook.getSheetAt(0);

        assertThat("Asserting that the number of the last row in sheet is valid",
            actualSheet.getLastRowNum(), equalTo(expectedSheet.getLastRowNum()));

        for (int i = 0; i <= actualSheet.getLastRowNum(); i++) {
            Row actualRow = actualSheet.getRow(i);
            Row expectedRow = expectedSheet.getRow(i);

            assertRowSoftly(actualRow, expectedRow);
        }
    }

    private void assertRowSoftly(Row actualRow, Row expectedRow) {
        short actualLastCellNum = actualRow.getLastCellNum();
        short expectedLastCellNum = expectedRow.getLastCellNum();

        assertions.assertThat(actualLastCellNum)
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
            assertions.assertThat(actualCell == null && expectedCell == null)
                .as("Asserting that both cells are null")
                .isTrue();
            return;
        }

        CellType actualCellType = actualCell.getCellType();
        CellType expectedCellType = expectedCell.getCellType();

        switch (actualCellType) {
            case NUMERIC:
                assertions.assertThat(actualCell.getNumericCellValue())
                    .as("Asserting that the numeric value of the cell is valid for cell: " + actualCell.getAddress())
                    .isEqualTo(expectedCell.getNumericCellValue());
                break;
            case STRING:
                assertions.assertThat(actualCell.getStringCellValue())
                    .as("Asserting that the string value of the cell is valid for cell: " + actualCell.getAddress())
                    .isEqualTo(expectedCell.getStringCellValue());
                break;
            case BOOLEAN:
                assertions.assertThat(actualCell.getBooleanCellValue())
                    .as("Asserting that the boolean value of the cell is valid for cell: " + actualCell.getAddress())
                    .isEqualTo(expectedCell.getBooleanCellValue());
                break;
            case ERROR:
                assertions.assertThat(actualCell.getErrorCellValue())
                    .as("Asserting that the error value of the cell is valid for cell: " + actualCell.getAddress())
                    .isEqualTo(expectedCell.getErrorCellValue());
                break;
            default:
        }
    }
}
