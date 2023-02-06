package ru.yandex.market.core.fulfillment.report.excel;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import com.google.common.collect.Iterables;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Assertions;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Утилитный класс для проверки excel-отчетов.
 */
public final class ExcelTestUtils {

    private ExcelTestUtils() {
        throw new UnsupportedOperationException();
    }

    public static void write(Workbook workbook) {
        try (OutputStream output = new FileOutputStream("src/test/resources/testReport.xlsx")) {
            workbook.write(output);
            workbook.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void assertCellValues(List<List<Object>> expectedValues, Sheet sheet, int rowNum, int colNum) {
        for (int i = 0; i < expectedValues.size(); i++) {
            List<Object> rowValues = expectedValues.get(i);
            for (int j = 0; j < rowValues.size(); j++) {
                Object value = rowValues.get(j);
                assertCellValue(String.format("error in row %s, column %s", rowNum + i, colNum + j),
                        sheet.getRow(rowNum + i).getCell(colNum + j), value);
            }
        }
    }

    public static void assertCellValue(Cell cell, Object expectedValue) {
        assertCellValue("", cell, expectedValue);
    }

    private static void assertCellValue(String reason, Cell cell, Object expectedValue) {
        switch (cell.getCellType()) {
            case NUMERIC:
                assertThat(reason, cell.getNumericCellValue(), closeTo(((Number) expectedValue).doubleValue(), 0));
                break;
            case BLANK:
                assertThat(reason, expectedValue, nullValue());
                break;
            default:
                if (expectedValue instanceof Matcher) {
                    assertThat(reason, cell.getStringCellValue(), (Matcher<String>) expectedValue);
                } else {
                    assertThat(reason, cell.getStringCellValue(), is(expectedValue));
                }
                break;
        }
    }

    public static void assertEquals(XSSFWorkbook expected, XSSFWorkbook result) {
        assertEquals(expected, result, Set.of());
    }

    public static void assertEquals(XSSFWorkbook expected, XSSFWorkbook result, Set<Integer> ignoredColumns) {
        Assertions.assertEquals(expected.getNumberOfSheets(), result.getNumberOfSheets());

        IntStream.range(0, expected.getNumberOfSheets()).forEach(sheetIndex -> {
            XSSFSheet expectedSheet = expected.getSheetAt(sheetIndex);
            XSSFSheet resultSheet = result.getSheetAt(sheetIndex);
            Row[] expectedRows = Iterables.toArray(expectedSheet, Row.class);
            Row[] resultRows = Iterables.toArray(resultSheet, Row.class);
            Assertions.assertEquals(expectedRows.length, resultRows.length);
            IntStream.range(0, expectedRows.length).forEach(rowIndex -> {
                Row expectedRow = expectedRows[rowIndex];
                Row resultRow = resultRows[rowIndex];
                Assertions.assertEquals(expectedRow.getRowNum(), resultRow.getRowNum());
                Cell[] expectedCells = Iterables.toArray(expectedRow, Cell.class);
                Cell[] resultCells = Iterables.toArray(resultRow, Cell.class);
                IntStream.range(0, Math.max(expectedCells.length, resultCells.length))
                        .filter(cellIndex -> !ignoredColumns.contains(cellIndex))
                        .forEach(cellIndex -> {
                            String msg = "Sheet index: [" + sheetIndex + ", row index: [" + rowIndex + "]";
                            Cell expectedCell = cellIndex < expectedCells.length ? expectedCells[cellIndex] : null;
                            Cell resultCell = cellIndex < resultCells.length ? resultCells[cellIndex] : null;
                            if (expectedCell != null && resultCell != null) {
                                Assertions.assertEquals(
                                        expectedCell.getColumnIndex(), resultCell.getColumnIndex(), msg
                                );
                            }
                            CellType expectedCellType =
                                    expectedCell == null ? CellType.BLANK : expectedCell.getCellType();
                            CellType resultCellType = resultCell == null ? CellType.BLANK : resultCell.getCellType();
                            Assertions.assertEquals(
                                    expectedCellType, resultCellType,
                                    "assertion fails in row " + rowIndex + ", column " + cellIndex
                            );
                            switch (expectedCellType) {
                                case BLANK:
                                    break;
                                case STRING:
                                    Assertions.assertEquals(
                                            expectedCell.getStringCellValue(),
                                            resultCell.getStringCellValue(),
                                            getMessageForCellAssertion(
                                                    expectedCell.getStringCellValue(),
                                                    resultCell.getStringCellValue(),
                                                    expectedCell,
                                                    resultCell,
                                                    expectedSheet)
                                    );
                                    break;
                                case BOOLEAN:
                                    Assertions.assertEquals(
                                            expectedCell.getBooleanCellValue(),
                                            resultCell.getBooleanCellValue(),
                                            getMessageForCellAssertion(
                                                    expectedCell.getBooleanCellValue(),
                                                    resultCell.getBooleanCellValue(),
                                                    expectedCell,
                                                    resultCell,
                                                    expectedSheet)
                                    );
                                    break;
                                case NUMERIC:
                                    Assertions.assertEquals(
                                            expectedCell.getNumericCellValue(),
                                            resultCell.getNumericCellValue(),
                                            getMessageForCellAssertion(
                                                    expectedCell.getNumericCellValue(),
                                                    resultCell.getNumericCellValue(),
                                                    expectedCell,
                                                    resultCell,
                                                    expectedSheet)
                                    );
                                    break;
                                case FORMULA:
                                    String expectedValue = expectedCell.getCellFormula()
                                            .replaceAll("\\s|=|'", "").toLowerCase();
                                    String actualValue = resultCell.getCellFormula()
                                            .replaceAll("\\s|=|'", "").toLowerCase();
                                    Assertions.assertEquals(
                                            expectedValue,
                                            actualValue,
                                            getMessageForCellAssertion(
                                                    expectedValue,
                                                    actualValue,
                                                    expectedCell,
                                                    resultCell,
                                                    expectedSheet)
                                    );
                                    break;
                                default:
                                    Assertions.fail("unknown cell type: " + expectedCell.getCellType());
                            }
                        });
            });
        });
    }

    private static String getMessageForCellAssertion(Object expectedValue,
                                                     Object actualValue,
                                                     Cell expectedCell,
                                                     Cell actualCell,
                                                     XSSFSheet expectedSheet) {
        return String.format("Expected \"%s\", actual \"%s\", for cell on \"%s\" sheet," +
                        " with coordinates (row=%s,column=%s)",
                expectedValue,
                actualValue,
                expectedSheet.getSheetName(),
                expectedCell.getRowIndex(),
                expectedCell.getColumnIndex()
        );
    }
}
