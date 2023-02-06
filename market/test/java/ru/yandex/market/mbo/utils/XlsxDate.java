package ru.yandex.market.mbo.utils;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Класс позволяет в тестах проверить содержимое excel таблиц.
 * Конструктор XlsxDate(String csv) используется для описания ожидаемого содержимого таблицы.
 * Конструктор XlsxDate(Workbook workbook) используется для реального excel файла.
 *
 * Сравниваются только значения, без учета какого-либо форматирования.
 *
 * Ограничения реализации:
 * 1. Поддерживаются только типы: строка, целое число и пустая ячейка
 * 2. Значения не должны содержать "," так как это разделитель в csv формате
 * 3. При сравнение значений, пробелы в начале и конце значения игнорируются
 *
 * Использование:
 * XlsxDate expected = new XlsxDate(
 *     "Колонка 1, Колонка 2, Колонка 3\n" +
 *     "Значение 1_1,Значение 1_2, Значение 1_3\n" +
 *     "Значение 2_1,, Значение 2_3"
 * );
 *
 * org.apache.poi.ss.usermodel.Workbook workbook = ...
 * XlsxDate actual = new XlsxDate(workbook);
 * org.junit.Assert.assertEquals(expected, actual);
 *
 * @author ayratgdl
 * @since 03.10.18
 */
public class XlsxDate {
    private List<List<String>> table;

    /**
     * @param csv описание таблицы в csv формате (разделитель ",")
     */
    public XlsxDate(String csv) {
        table = new ArrayList<>();
        for (String line : csv.split("\n")) {
            List<String> row = new ArrayList<>();
            table.add(row);
            for (String cellValue : line.split(",")) {
                row.add(cellValue.trim());
            }
        }
    }

    public XlsxDate(Workbook workbook) {
        table = new ArrayList<>();
        Sheet sheet = workbook.getSheetAt(0);
        for (int rowIndex = 0; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            List<String> tableRow = new ArrayList<>();
            table.add(tableRow);
            Row xlsxRow = sheet.getRow(rowIndex);
            for (int colIndex = 0; colIndex < xlsxRow.getLastCellNum(); colIndex++) {
                Cell cell = xlsxRow.getCell(colIndex);
                if (cell == null) {
                    tableRow.add("");
                    continue;
                }
                String value;
                switch (cell.getCellTypeEnum()) {
                    case STRING:
                        value = cell.getStringCellValue();
                        break;
                    case NUMERIC:
                        value = Long.toString(Math.round(cell.getNumericCellValue()));
                        break;
                    case BLANK:
                        value = "";
                        break;
                    default:
                        throw new RuntimeException(
                            "Xlsx contains cell with unsupported type '" + cell.getCellTypeEnum() + "'");
                }
                tableRow.add(value.trim());
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        XlsxDate dataExcel = (XlsxDate) o;
        return Objects.equals(table, dataExcel.table);
    }

    @Override
    public int hashCode() {
        return Objects.hash(table);
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        for (List<String> row : table) {
            StringJoiner rowJoiner = new StringJoiner(", ");
            for (String cell : row) {
                rowJoiner.add(cell);
            }
            buffer.append(rowJoiner.toString());
            buffer.append("\n");
        }
        return buffer.toString();
    }
}
