package ru.yandex.travel.hotels.searcher;

import lombok.SneakyThrows;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.function.Consumer;

public class TestUtils {

    public static InputStream resourceAsStream(String path) {
        return TestUtils.class.getClassLoader().getResourceAsStream(path);
    }

    @SneakyThrows
    public static void loadCsvResource(String path, String[] headers, Consumer<CSVRecord> rowConsumer) {
        try (
                InputStream stream = resourceAsStream(path);
                InputStreamReader reader = new InputStreamReader(stream)
        ) {
            CSVFormat.DEFAULT
                    .withHeader(headers)
                    .withFirstRecordAsHeader()
                    .parse(reader)
                    .forEach(rowConsumer);
        }
    }
}
