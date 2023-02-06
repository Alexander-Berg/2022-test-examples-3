package ru.yandex.vendor.modelbids.statistic.report.impl.xlsx;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ru.yandex.vendor.statistic.report.impl.xlsx.XlsxPoiFormatters;
import ru.yandex.vendor.statistic.report.impl.xlsx.XlsxReportContext;
import ru.yandex.vendor.statistic.report.impl.xlsx.XlsxReportGenerator;
import ru.yandex.vendor.statistic.report.model.Report;
import ru.yandex.vendor.statistic.report.model.ReportData;
import ru.yandex.vendor.statistic.report.model.ReportRow;

@Disabled
class XlsxReportGeneratorTest {

    private static final String TEMP_REPORT_NAME = "test.xlsx";

    private final XlsxReportGenerator xlsxReportGenerator;

    XlsxReportGeneratorTest() {
        this.xlsxReportGenerator = new XlsxReportGenerator();
    }

    @Test
    void testXlsxReportGeneratorTest(@TempDir final Path tempDir) throws IOException {
        final Path reportFile = tempDir.resolve(TEMP_REPORT_NAME);
        final Report report = createReportWithHeaderAndData();
        try (final var os = new FileOutputStream(reportFile.toFile())) {
            final var reportContext = XlsxReportContext.createContext(os);
            reportContext.setRowDataFormatter(2, XlsxPoiFormatters.DECIMAL_TWO_DECIMAL_PLACES.value());
            reportContext.setRowDataFormatter(4, XlsxPoiFormatters.DATE_YEAR.value());
            xlsxReportGenerator.generate(report, reportContext);
            verifyGeneratedReport(report, reportFile);
        }
    }

    @Test
    void testXlsxReportGeneratorTestWithHugeData(@TempDir final Path tempDir) throws IOException {
        final Path reportFile = tempDir.resolve(TEMP_REPORT_NAME);
        final Report report = createReportWithHeaderAndHugeData();
        try (final var os = new FileOutputStream(reportFile.toFile())) {
            final var reportContext = XlsxReportContext.createContext(os);
            reportContext.setRowDataFormatter(2, XlsxPoiFormatters.DECIMAL_TWO_DECIMAL_PLACES.value());
            reportContext.setRowDataFormatter(4, XlsxPoiFormatters.DATE_YEAR.value());
            xlsxReportGenerator.generate(report, reportContext);
            verifyGeneratedReport(report, reportFile);
        }
    }

    private void verifyGeneratedReport(Report report, Path reportPath) throws IOException {
        try (var is = new FileInputStream(reportPath.toFile())) {
            Workbook workbook = new XSSFWorkbook(is);
            Sheet sheet = workbook.getSheetAt(workbook.getNumberOfSheets() - 1);
            List<ReportRow> headerRows = report.getHeader().getRows();
            List<ReportRow> dataRows = report.getData().getRows();

            verifyReportRowsCount(headerRows.size() + dataRows.size(), sheet.getPhysicalNumberOfRows());
            verifyReportData(0, sheet, headerRows);
            verifyReportData(headerRows.size(), sheet, dataRows);
        }
    }

    private void verifyReportRowsCount(int expectedValue, int actualValue) {
        Assertions.assertEquals(expectedValue, actualValue);
    }

    private void verifyReportData(int startRow, Sheet sheet, List<ReportRow> data) {
        for (int rowNum = startRow, dataNum = 0; rowNum < data.size(); ++rowNum, ++dataNum) {
            Row row = sheet.getRow(rowNum);
            int cellCount = row.getPhysicalNumberOfCells();
            ReportRow reportRow = data.get(dataNum);
            Assertions.assertEquals(cellCount, reportRow.size());
            for (int columnNum = 0; columnNum < cellCount; ++columnNum) {
                verifyValue(row.getCell(columnNum), reportRow.getValueAt(columnNum));
            }
        }
    }

    private void verifyValue(Cell currentCell, Object expectedValue) {
        boolean isNumber = expectedValue instanceof Number;
        boolean isBoolean = expectedValue instanceof Boolean;
        boolean isLocalDate = expectedValue instanceof LocalDate;

        Object actualValue;
        if (isNumber) {
            expectedValue = ((Number) expectedValue).doubleValue();
            actualValue = currentCell.getNumericCellValue();
        } else if (isBoolean) {
            actualValue = currentCell.getBooleanCellValue();
        } else if (isLocalDate) {
            actualValue = currentCell.getLocalDateTimeCellValue().toLocalDate();
        } else {
            expectedValue = expectedValue.toString();
            actualValue = currentCell.getStringCellValue();
        }

        Assertions.assertEquals(expectedValue, actualValue);
    }

    private Report createReportWithHeaderAndData() {
        ReportData header = new ReportData(Arrays.asList(createReportHeader(), createTableHeader()));

        final ReportRow testDataRow = createTestRow();
        ReportData data = new ReportData(Collections.nCopies(3, testDataRow));

        return new Report(header, data);
    }

    private Report createReportWithHeaderAndHugeData() {
        ReportData header = new ReportData(Arrays.asList(createReportHeader(), createTableHeader()));

        final ReportRow testDataRow = createTestRow();
        ReportData data = new ReportData(Collections.nCopies(100_000, testDataRow));

        return new Report(header, data);
    }

    private ReportRow createReportHeader() {
        return ReportRow.create()
                .addValue("Report test");
    }

    private ReportRow createTableHeader() {
        return ReportRow.create()
                .addValue("1 column")
                .addValue("2 column")
                .addValue("3 column")
                .addValue("4 column")
                .addValue("5 column");
    }

    private ReportRow createTestRow() {
        return ReportRow.create()
                .addValue("test")
                .addValue(10L)
                .addValue(10.558)
                .addValue(true)
                .addValue(LocalDate.of(2020, 1, 16));
    }
}
