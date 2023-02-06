package ru.yandex.market.partner.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.IOUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.hamcrest.CoreMatchers;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Утилитные методы для грубой проверки xls vs csv.
 * В идеале тут бы конечно не строки сравнивать а с учетом типа xls поля и указание проблемной ячейки
 * не только номер строки + номер колонки а как ячеек а как-то более user-friendly.
 *
 * @author vbudnev
 */
public class CsvUtils {
    private static final String UNKNOWN_CELL_TYPE = "<unknown_data_type>";
    private static final Set<CellType> SUPPORTED_CELL_TYPES = ImmutableSet.of(
            CellType.BOOLEAN,
            CellType.BLANK,
            CellType.NUMERIC,
            CellType.STRING
    );

    /**
     * Сверка представления xls vs csv для конкретного номера листа. Используется номер, так как в зависимости от локали
     * неявно заданный лист будет генериться либо как Sheet1 либо как Лист1, а именования для листов мы не везде задаем.
     */
    public static void assertXlsEqCsv(InputStream xlsInputStream,
                                      InputStream csvInputStream,
                                      int sheetIndex,
                                      boolean dumpActual
    ) throws IOException {

        final List<List<String>> xlsSheet = xlsLoadSheet(xlsInputStream, sheetIndex);
        final List<List<String>> csv = csvLoad(csvInputStream);

        if (dumpActual) {
            dump(xlsSheet);
        }

        assertThat("xls sheet and csv have different row number", xlsSheet.size(), CoreMatchers.is(csv.size()));

        for (int rowNum = 0; rowNum < csv.size(); rowNum++) {
            final List<String> csvExpectedRow = csv.get(rowNum);
            final List<String> xlsActualRow = xlsSheet.get(rowNum);

            assertThat(String.format("Row #%s has different size for csv and xls", rowNum),
                    csvExpectedRow.size(),
                    CoreMatchers.is(xlsActualRow.size())
            );

            for (int colNum = 0; colNum < csvExpectedRow.size(); colNum++) {
                final String expectedCsvColumn = csvExpectedRow.get(colNum);
                final String actualXlsColumn = xlsActualRow.get(colNum);

                assertThat(
                        String.format("Column #%s for row #%s has different expected csv value then actual xls", colNum,
                                rowNum
                        ),
                        actualXlsColumn,
                        CoreMatchers.is(expectedCsvColumn)
                );

            }
        }
    }

    private static List<List<String>> xlsLoadSheet(InputStream inputStream, int sheetIndex) throws IOException {
        final Workbook wb = WorkbookFactory.create(inputStream);
        final Sheet sheet = wb.getSheetAt(sheetIndex);
        final List<List<String>> rows = new ArrayList<>();
        for (int rowId = 0; rowId <= sheet.getLastRowNum(); rowId++) {
            final Row row = sheet.getRow(rowId);
            final List<String> parsedRow = new ArrayList<>();
            for (int cellId = 0; cellId < row.getLastCellNum(); cellId++) {
                final Cell cell = row.getCell(cellId);
                if (SUPPORTED_CELL_TYPES.contains(cell.getCellType())) {
                    parsedRow.add(cell.toString());
                } else {
                    parsedRow.add(UNKNOWN_CELL_TYPE);
                }
            }
            rows.add(parsedRow);
        }
        return rows;
    }

    private static List<List<String>> csvLoad(InputStream inputStream) throws IOException {
        final String csvStr = IOUtils.toString(inputStream, UTF_8);
        return CSVParser.parse(csvStr, CSVFormat.DEFAULT)
                .getRecords()
                .stream()
                .map(CsvUtils::simpleCsvRowMapper)
                .collect(Collectors.toList());
    }

    private static List<String> simpleCsvRowMapper(CSVRecord record) {
        final List<String> rowCols = new ArrayList<>();
        for (int i = 0; i < record.size(); ++i) {
            rowCols.add(record.get(i));
        }
        return rowCols;
    }

    /**
     * Дамп для удобства отладки.
     */
    private static void dump(List<List<String>> data) {
        final String res = data.stream()
                .map(row -> String.join(",", row))
                .collect(Collectors.joining("\n"));
        System.out.println(res);
    }
}