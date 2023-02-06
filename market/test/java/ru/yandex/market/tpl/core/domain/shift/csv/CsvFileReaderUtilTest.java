package ru.yandex.market.tpl.core.domain.shift.csv;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CsvFileReaderUtilTest {

    private static final String SEPARATOR_RN = "\r\n";
    private static final String SEPARATOR_R = "\r";
    private static final String SEPARATOR_N = "\n";

    @DisplayName("Проверка чтения из csv файла с \\r\\n")
    @Test
    void createCsvAndReadWithSeparatorRN() throws IOException {
        String newFileName = "/csv/temp-shifManager-rn";
        File newFile = File.createTempFile(newFileName, ".csv");
        BufferedWriter writer = new BufferedWriter(new FileWriter(newFile));
        List<String> data = List.of("1", "2");
        for (String s : data) {
            writer.write(s);
            writer.write(SEPARATOR_RN);
        }
        writer.close();
        byte[] bytes = FileUtils.readFileToByteArray(newFile);

        List<String[]> filesData = CsvFileReaderUtil.readAll(bytes);

        Assertions.assertEquals(filesData.size(), data.size());
    }

    @DisplayName("Проверка чтения из csv файла с \\r")
    @Test
    void createCsvAndReadWithSeparatorR() throws IOException {
        String newFileName = "/csv/temp-shifManager-r";
        File newFile = File.createTempFile(newFileName, ".csv");
        BufferedWriter writer = new BufferedWriter(new FileWriter(newFile));
        List<String> data = List.of("1", "2");
        for (String s : data) {
            writer.write(s);
            writer.write(SEPARATOR_R);
        }
        writer.close();
        byte[] bytes = FileUtils.readFileToByteArray(newFile);

        List<String[]> filesData = CsvFileReaderUtil.readAll(bytes);

        Assertions.assertEquals(filesData.size(), data.size());
    }

    @DisplayName("Проверка чтения из csv файла с \\n")
    @Test
    void createCsvAndReadWithSeparatorN() throws IOException {
        String newFileName = "/csv/temp-shifManager-n";
        File newFile = File.createTempFile(newFileName, ".csv");
        BufferedWriter writer = new BufferedWriter(new FileWriter(newFile));
        List<String> data = List.of("1", "2");
        for (String s : data) {
            writer.write(s);
            writer.write(SEPARATOR_N);
        }
        writer.close();
        byte[] bytes = FileUtils.readFileToByteArray(newFile);

        List<String[]> filesData = CsvFileReaderUtil.readAll(bytes);

        Assertions.assertEquals(filesData.size(), data.size());
    }

    @DisplayName("UTF8 with BOM. Файл из тикета с проблемой домаршрутизацией на Нижнем Новгороде MARKETTPL-4764")
    @Test
    void caseMARKETTPL4764() throws URISyntaxException, IOException {
        URI path = Objects.requireNonNull(getClass()
                .getResource("/csv/utf8-with-bom-NN-MARKETTPL-4764.csv"))
                .toURI();
        File file = Paths.get(path).toFile();
        byte[] bytes = FileUtils.readFileToByteArray(file);

        List<String[]> strings = CsvFileReaderUtil.readAll(bytes);

        String[] firstLine = strings.iterator().next();
        String firstOrderId = firstLine[0];
        Assertions.assertEquals(firstOrderId, "56343253");
        Assertions.assertEquals(strings.size(), 268);
    }

    @DisplayName("UTF8. Чтение из файла")
    @Test
    void readSimpleFileTest() throws URISyntaxException, IOException {
        URI path = Objects.requireNonNull(getClass()
                .getResource("/csv/simple.csv"))
                .toURI();
        File file = Paths.get(path).toFile();
        byte[] bytes = FileUtils.readFileToByteArray(file);

        List<String[]> strings = CsvFileReaderUtil.readAll(bytes);

        String[] firstLine = strings.iterator().next();
        String firstItem = firstLine[0];
        Assertions.assertEquals(firstItem, "123123123");
        Assertions.assertEquals(strings.size(), 1);
    }

    @DisplayName("Проверка чтения из пустого csv файла")
    @Test
    void createEmptyCsvAndRead() throws IOException {
        String newFileName = "/csv/temp-shifManager-empty";
        File newFile = File.createTempFile(newFileName, ".csv");
        BufferedWriter writer = new BufferedWriter(new FileWriter(newFile));
        writer.close();
        byte[] bytes = FileUtils.readFileToByteArray(newFile);

        List<String[]> filesData = CsvFileReaderUtil.readAll(bytes);

        Assertions.assertEquals(filesData.size(), 0);
    }

}
