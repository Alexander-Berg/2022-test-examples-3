package ru.yandex.market.vendor.stats;

import java.io.InputStream;
import java.util.function.Function;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Assertions;

public final class StatsTestUtils {

    private StatsTestUtils() {
    }

    public static void verifyGeneratedReport(InputStream expectedReport, InputStream actualReport)
            throws Exception {
        try (Workbook expected = new XSSFWorkbook(expectedReport);
             Workbook actual = new XSSFWorkbook(actualReport)) {

            Sheet expectedSheet = getFirstSheet(expected);
            Sheet actualSheet = getFirstSheet(actual);

            verifySheets(expectedSheet, actualSheet, 0);
        }
    }

    public static void verifyGeneratedMultiSheetReport(InputStream expectedReport, InputStream actualReport)
            throws Exception {
        try (Workbook expected = new XSSFWorkbook(expectedReport);
             Workbook actual = new XSSFWorkbook(actualReport)) {
            verifyHasSameNumberOfSheets(expected, actual);

            for (int currentSheet = 0; currentSheet < actual.getNumberOfSheets(); ++currentSheet) {
                Sheet expectedSheet = getSheetAt(expected, currentSheet);
                Sheet actualSheet = getSheetAt(actual, currentSheet);

                verifyHasSameSheetName(expectedSheet, actualSheet, currentSheet);
                verifySheets(expectedSheet, actualSheet, currentSheet);
            }
        }
    }

    private static void verifyHasSameSheetName(Sheet expected, Sheet actual, int sheetNumber) {
        String expectedSheetName = expected.getSheetName();
        String actualSheetName = actual.getSheetName();
        Assertions.assertEquals(
                expectedSheetName,
                actualSheetName,
                String.format(
                        "Expected sheet name = %s, actual sheet name = %s at sheet %d",
                        expectedSheetName,
                        actualSheetName,
                        sheetNumber
                )
        );
    }

    private static void verifyHasSameNumberOfSheets(Workbook expected, Workbook actual) {
        int expectedNumberOfSheets = expected.getNumberOfSheets();
        int actualNumberOfSheets = actual.getNumberOfSheets();
        if (expectedNumberOfSheets != actualNumberOfSheets) {
            throw new IllegalStateException(String.format(
                      "Expected number of sheets = %d not equal actual number of sheets = %d",
                      expectedNumberOfSheets,
                      actualNumberOfSheets
            ));
        }
    }

    private static void verifySheets(Sheet expectedSheet, Sheet actualSheet, int sheetNumber) {
        verifyRowsCount(expectedSheet.getPhysicalNumberOfRows(), actualSheet.getPhysicalNumberOfRows(), sheetNumber);
        for (int rowNum = 0; rowNum < actualSheet.getPhysicalNumberOfRows(); ++rowNum) {
            Row expectedRow = expectedSheet.getRow(rowNum);
            Row actualRow = actualSheet.getRow(rowNum);

            int expectedCellCount = expectedRow.getPhysicalNumberOfCells();
            int actualCellCount = actualRow.getPhysicalNumberOfCells();
            Assertions.assertEquals(
                    expectedCellCount,
                    actualCellCount,
                    String.format("Different cell count on row = %d of sheet = %d", rowNum, sheetNumber)
            );

            for (int columnNum = 0; columnNum < actualCellCount; ++columnNum) {
                verifyCellValue(expectedRow.getCell(columnNum), actualRow.getCell(columnNum), sheetNumber);
            }
        }
    }

    private static Sheet getSheetAt(Workbook workbook, int index) {
        if (index < 0) {
            throw new IllegalArgumentException("Sheet index cannot be negative");
        }
        int numberOfSheets = workbook.getNumberOfSheets() - 1;
        if (numberOfSheets < index) {
            throw new IllegalArgumentException(
                    String.format("Index = %d cannot be greater then number of sheets = %d", numberOfSheets, index)
            );
        }
        return workbook.getSheetAt(index);
    }

    private static Sheet getFirstSheet(Workbook workbook) {
        return workbook.getSheetAt(0);
    }

    private static void verifyRowsCount(int expectedValue, int actualValue, int sheetNumber) {
        Assertions.assertEquals(
                expectedValue,
                actualValue,
                String.format("Row count is not equal at sheet %d", sheetNumber)
        );
    }


    private static void verifyCellValue(Cell expectedCell, Cell actualCell, int sheetNumber) {
        CellType expectedCellType = expectedCell.getCellType();
        CellType actualCellType = actualCell.getCellType();

        Assertions.assertEquals(
                expectedCellType,
                actualCellType,
                String.format(
                        "Different cell type on (row, column)=(%d,%d) at sheet %d",
                        actualCell.getRowIndex(),
                        actualCell.getColumnIndex(),
                        sheetNumber
                )
        );

        Function<Cell, Object> valueGetter;
        switch (actualCellType) {
            case NUMERIC:
                valueGetter = Cell::getNumericCellValue;
                break;
            case STRING:
            case BLANK:
                valueGetter = Cell::getStringCellValue;
                break;
            case BOOLEAN:
                valueGetter = Cell::getBooleanCellValue;
                break;
            default:
                throw new IllegalStateException();
        }

        Object expectedValue = valueGetter.apply(expectedCell);
        Object actualValue = valueGetter.apply(actualCell);
        Assertions.assertEquals(
                expectedValue,
                actualValue,
                String.format(
                        "Different cell values on (row, column)=(%d,%d) at sheet %d",
                        actualCell.getRowIndex(),
                        actualCell.getColumnIndex(),
                        sheetNumber
                )
        );
    }
}
