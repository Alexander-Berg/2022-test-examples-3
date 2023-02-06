package ru.yandex.market.api.util.fs;

import com.google.common.io.ByteStreams;
import org.apache.commons.csv.CSVFormat;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.util.ApiStrings;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author zoom
 */
public class CsvHelperTest extends UnitTestBase {

    /**
     * 1)  Читает данные из файла
     * <p>
     * 2) Записывает данные в буфер
     * <p>
     * 3) Читает данные из буфера
     * <p>
     * 4) Сравнивает результаты шага 1 и шага 3
     */
    @Test
    public void shouldReadWriteAndReadAgainConsistency() throws IOException {
        byte[] originalBytes;
        try (InputStream in = CsvHelperTest.class.getResourceAsStream("CsvHelperTest_sample.csv.xml")) {
            originalBytes = ByteStreams.toByteArray(in);
        }

        List<List<String>> originalValues = new ArrayList<>();

        try (ByteArrayInputStream originalFile = new ByteArrayInputStream(originalBytes)) {
            try (InputStreamReader originalReader = new InputStreamReader(originalFile, ApiStrings.UTF8);) {
                CsvHelper.readData(originalReader, values -> originalValues.add(Arrays.asList(values)));
            }
        }

        byte[] actualBytes;
        try (ByteArrayOutputStream writtenBuffer = new ByteArrayOutputStream()) {
            CsvHelper.writeData(new OutputStreamWriter(writtenBuffer, ApiStrings.UTF8), writer -> {
                for (List<String> values : originalValues) {
                    writer.accept(values.toArray(new String[values.size()]));
                }
            });
            actualBytes = writtenBuffer.toByteArray();
        }

        List<List<String>> actualValues = new ArrayList<>();

        try (InputStreamReader reader = new InputStreamReader(new ByteArrayInputStream(actualBytes),
            ApiStrings.UTF8)) {
            CsvHelper.readData(reader, values -> actualValues.add(Arrays.asList(values)));
        }

        Assert.assertEquals(originalValues, actualValues);
    }

    /**
     * Проверяем, что {@code null} значения правильно сериализуются и десериализуются
     *
     * @throws IOException
     */
    @Test
    public void shouldWriteAndReadNullValue() throws IOException {
        CSVFormat format = CSVFormat.DEFAULT.withNullString("!NULL!");
        String[] data = new String[]{"1", null, "346", null};

        try (ByteArrayOutputStream writtenBuffer = new ByteArrayOutputStream()) {
            CsvHelper.writeData(new OutputStreamWriter(writtenBuffer, ApiStrings.UTF8), format, writer -> {
                writer.accept(data);
            });
            try (InputStreamReader reader = new InputStreamReader(new ByteArrayInputStream(writtenBuffer.toByteArray()), ApiStrings.UTF8)) {
                CsvHelper.readData(reader, format, values -> Assert.assertArrayEquals(data, values));
            }
        }

    }

}
