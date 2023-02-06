package ru.yandex.market.common.test.util;

import org.dbunit.dataset.Column;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.ITable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Class for generate pretty ( and informative) string representation of {@link ITable}.
 */
public final class ITablePrettyGenerator {
    private ITablePrettyGenerator() { }

    /**
     * Generate pretty string representation of {@link ITable}. <br>
     * If table is empty ( columns are empty), this method will return "Table is empty". <br>
     * If {@link DataSetException} occurs during generation, this method return exception message.
     *
     * @return pretty string representation of {@link ITable}
     */
    public static String generate(ITable table) {
        try {
            List<List<String>> data = convert(table);
            if (data.isEmpty()) {
                return "Table is empty";
            }
            int[] columnsWidth = maxWidths(data);
            int totalWidth = Arrays.stream(columnsWidth)
                    .map(width -> width + 2)            // +2 = for symbols ' |' ( space and delimiter) after value
                    .sum() + 1;                         // +1 = for first symbol '|'

            return data.stream()
                    .map(values -> {
                        String line = IntStream.range(0, values.size())
                                .mapToObj(j -> String.format("%-" + columnsWidth[j] + "s ", values.get(j)))
                                .collect(Collectors.joining("|", "|", "|"));
                        return printDelimiter(totalWidth) + "\n" + line;
                    })
                    .collect(Collectors.joining("\n")) +
                    "\n" + printDelimiter(totalWidth);
        } catch (DataSetException ex) {
            return String.format("Unable to get actual content: %s: %s", ex.getClass().getName(), ex.getMessage());
        }
    }

    /**
     * Convert {@link ITable} to two-dimensional array of String (represent as List of List). <br>
     * If header is empty, will return empty list
     */
    private static List<List<String>> convert(ITable table) throws DataSetException {
        List<List<String>> data = new ArrayList<>();

        List<String> header = Arrays.stream(table.getTableMetaData().getColumns())
                .map(Column::getColumnName)
                .collect(Collectors.toList());
        if (header.isEmpty()) {
            return Collections.emptyList();
        }
        data.add(header);

        for (int i = 0; i < table.getRowCount(); i++) {
            List<String> values = new ArrayList<>();
            for (String columnName : header) {
                Object value = table.getValue(i, columnName);
                values.add(String.valueOf(value));
            }
            data.add(values);
        }
        return data;
    }

    /**
     * Calculate max width for each column from {@code data}.
     */
    private static int[] maxWidths(List<List<String>> data) {
        int width = data.get(0).size();
        int[] maxWidths = new int[width];
        for (int i = 0; i < width; i++) {
            for (List<String> values : data) {
                String value = values.get(i);
                maxWidths[i] = Math.max(maxWidths[i], value.length());
            }
            maxWidths[i] = Math.max(maxWidths[i], 1); // avoid 0 width
        }
        return maxWidths;
    }

    private static String printDelimiter(int totalWidth) {
        return Stream.iterate("-", UnaryOperator.identity())
                .limit(totalWidth - 2)
                .collect(Collectors.joining("", "+", "+"));
    }
}
