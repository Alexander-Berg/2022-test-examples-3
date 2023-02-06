package ru.yandex.market.jmf.entity.test.assertions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.google.common.collect.Multimap;

public class EntityAssertDiffFormatter {

    private static final List<String> HEADER = List.of("ATTRIBUTE", "EXPECTED", "ACTUAL");
    private static final int MAX_WIDTH = 30;
    private static final List<String> EMPTY_ROW = Collections.emptyList();

    public static String formatDiff(Multimap<String, Object> expected, Multimap<String, Object> mismatch) {
        List<List<String>> data = prepareData(toRegularMap(expected), toRegularMap(mismatch));
        List<List<String>> table = prepareTable(data);
        return formatTable(table);
    }

    private static List<List<String>> prepareData(Map<String, Object> expected, Map<String, Object> mismatch) {
        List<List<String>> data = new ArrayList<>();

        data.add(HEADER);
        expected.forEach((key, value) -> {
            boolean isMismatch = mismatch.containsKey(key);
            data.add(List.of(
                    isMismatch ? key + " (MISMATCH)" : key,
                    nullToString(value),
                    nullToString(mismatch.getOrDefault(key, "EQUAL TO EXPECTED"))
            ));
        });
        return data;
    }

    private static List<List<String>> prepareTable(List<List<String>> data) {
        List<List<String>> rows = new ArrayList<>();
        for (List<String> dataRow : data) {
            boolean needExtraRow;
            int splitRow = 0;
            do {
                needExtraRow = false;
                List<String> newRow = new ArrayList<>(dataRow.size());
                for (String column : dataRow) {
                    if (column.length() < MAX_WIDTH) {
                        newRow.add(splitRow == 0 ? column : "");
                    } else if (column.length() > (splitRow * MAX_WIDTH)) {
                        // If data is more than max width, then crop data at maxwidth.
                        // Remaining cropped data will be part of next dataRow.
                        int end = Math.min(column.length(), ((splitRow * MAX_WIDTH) + MAX_WIDTH));
                        newRow.add(column.substring((splitRow * MAX_WIDTH), end));
                        needExtraRow = true;
                    } else {
                        newRow.add("");
                    }
                }
                rows.add(newRow);
                if (needExtraRow) {
                    splitRow++;
                } else {
                    rows.add(EMPTY_ROW);
                }
            } while (needExtraRow);
        }
        return rows;
    }

    private static String formatTable(List<List<String>> table) {
        Map<Integer, Integer> columnLengths = getColumnLengths(table);
        String formatString = getFormatString(columnLengths);
        String rowDelimiter = getRowDelimiter(columnLengths);
        StringBuilder builder = new StringBuilder(rowDelimiter);
        for (List<String> row : table) {
            if (EMPTY_ROW.equals(row)) {
                builder.append(rowDelimiter);
            } else {
                builder.append(String.format(formatString, row.toArray()));
            }
        }
        return builder.toString();
    }

    private static String getRowDelimiter(Map<Integer, Integer> columnLengths) {
        return columnLengths.values()
                .stream()
                .map("-"::repeat)
                .collect(Collectors.joining("-+-", "+-", "-+\n"));
    }

    private static String getFormatString(Map<Integer, Integer> columnLengths) {
        final StringBuilder builder = new StringBuilder();
        columnLengths.forEach((key, value) -> builder.append("| %" + "-").append(value).append("s "));
        builder.append("|\n");
        return builder.toString();
    }

    private static Map<Integer, Integer> getColumnLengths(List<List<String>> table) {
        Map<Integer, Integer> columnLengths = new HashMap<>();
        for (List<String> row : table) {
            for (int i = 0; i < row.size(); i++) {
                columnLengths.putIfAbsent(i, 0);
                String column = row.get(i);
                if (columnLengths.get(i) < column.length()) {
                    columnLengths.put(i, column.length());
                }
            }
        }
        return columnLengths;
    }

    private static String nullToString(Object obj) {
        return obj == null ? "NULL" : obj.toString();
    }

    private static Map<String, Object> toRegularMap(Multimap<String, Object> multimap) {
        return multimap.entries()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> nullToString(entry.getValue()),
                        (a, b) -> Objects.equals(a, b) ? a : String.join("; ", a.toString(), b.toString())
                ));
    }
}
