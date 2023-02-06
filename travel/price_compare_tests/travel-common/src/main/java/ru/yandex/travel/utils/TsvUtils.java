package ru.yandex.travel.utils;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TsvUtils {

    private static final String COLUMN_DELIMITER = "\t";

    private TsvUtils() {
    }

    public static List<Map<String, String>> read(final InputStream stream) throws IOException {
        return parseLines(IOUtils.readLines(stream, StandardCharsets.UTF_8));
    }

    public static List<Map<String, String>> parseLines(final List<String> input) {
        final List<Map<String, String>> searches = new ArrayList<>();
        final List<String[]> lines = input.stream().map(TsvUtils::parseRow).collect(Collectors.toList());
        lines.stream().findFirst().ifPresent(header ->
                lines.stream().skip(1).map(row -> TsvUtils.parseRow(header, row)).forEach(searches::add));
        return searches;

    }

    public static Map<String, String> parseRow(final String[] header, final String[] row) {
        assert header.length == row.length;
        return IntStream.range(0, header.length).boxed()
                .collect(Collectors.toMap(i -> header[i], i -> row[i]));
    }

    public static String[] parseRow(final String row) {
        return row.split(COLUMN_DELIMITER);
    }
}
