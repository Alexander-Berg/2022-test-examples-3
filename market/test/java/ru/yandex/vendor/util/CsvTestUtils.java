package ru.yandex.vendor.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;

import ru.yandex.common.util.csv.CSVReader;

public final class CsvTestUtils {

    private CsvTestUtils() {
    }

    public static void verifyCsvWithDelimiter(InputStream expected,
                                              InputStream actually,
                                              char delimiter) throws IOException {
        CSVReader expectedCsv = new CSVReader(expected);
        CSVReader actuallyCsv = new CSVReader(actually);
        expectedCsv.setDelimiter(delimiter);
        actuallyCsv.setDelimiter(delimiter);
        verifyList(readAsList(expectedCsv), readAsList(actuallyCsv));
    }

    private static List<List<String>> readAsList(CSVReader reader) throws IOException {
        List<List<String>> csvToList= new ArrayList<>();
        while (reader.readRecord()) {
            csvToList.add(reader.getFields());
        }
        return csvToList;
    }

    private static void verifyList(List<List<String>> expected,
                                   List<List<String>> actually) {
        Assertions.assertEquals(actually, expected);
    }
}
