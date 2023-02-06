package ru.yandex.market.core.tmessage.service.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.core.tmessage.TargetMessageFileFormatException;
import ru.yandex.market.core.tmessage.service.DatasourceIDNotFoundException;
import ru.yandex.market.core.tmessage.service.impl.processors.CsvProcessor;
import ru.yandex.market.core.tmessage.service.impl.processors.JsonProcessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class TargetMessageProcessorTest {
    @Test
    @DisplayName("Тестирует, что файл обработался успешно")
    public void testCsvOk() throws IOException {
        var processor = new CsvProcessor();
        try (var stream = getClass().getResourceAsStream("targetMessage.ok.csv")) {
            var message = processor.process(stream);
            assertThat(message.getData().getVariables()).hasSize(2);
            assertThat(message.getData().getRecipients()).hasSize(3);
            assertThat(message.getData().getTestRecipientIndex()).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("Тестирует, что будет исключение, если файл пустой")
    public void testCsvEmptyFile() {
        assertThatExceptionOfType(TargetMessageFileFormatException.class)
                .isThrownBy(() -> {
                    try (InputStream stream = new ByteArrayInputStream(ArrayUtils.EMPTY_BYTE_ARRAY)) {
                        new CsvProcessor().process(stream);
                    }
                })
                .withMessage("CSV file is empty");
    }

    @Test
    @DisplayName("Тестирует, что будет исключение, если ни одной строки в файле нет")
    public void testCsvEmptyContent() {
        assertThatExceptionOfType(TargetMessageFileFormatException.class)
                .isThrownBy(() -> processCsv("targetMessage.rows.empty.csv"))
                .withMessage("At least one recipient must exist");
    }

    @Test
    @DisplayName("Тестирует, что будет исключение, если длина любой строки не совпадает с кол-вом переменных")
    public void testCsvLinesWithDifferentLength() {
        assertThatExceptionOfType(TargetMessageFileFormatException.class)
                .isThrownBy(() -> processCsv("targetMessage.lines.with.different.length.csv"))
                .withMessage("Line 3 has a different length");
    }

    @Test
    @DisplayName("Тестирует, что будет исключение, если имя любой из переменных пусто")
    public void testCsvVariableHasEmptyValue() {
        assertThatExceptionOfType(TargetMessageFileFormatException.class)
                .isThrownBy(() -> processCsv("targetMessage.too.many.vars.csv"))
                .withMessage("Variable name cannot be empty");
    }

    private void processCsv(String resource) throws IOException {
        try (var stream = getClass().getResourceAsStream(resource)) {
            new CsvProcessor().process(stream);
        }
    }

    @Test
    @DisplayName("Тестирует, что файл при обычных условиях обрабатывается успешно")
    public void testJsonUsually() throws IOException {
        var processor = new JsonProcessor();
        try (var stream = getClass().getResourceAsStream("test.json.good")) {
            var message = processor.process(stream);
            assertThat(message.getData().getVariables()).hasSize(3);
            assertThat(message.getData().getRecipients()).hasSize(3);
            assertThat(message.getData().getTestRecipientIndex()).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("Тестирует, что парсер игнорирует регистр")
    public void testJsonIgnoreCase() throws IOException {
        var processor = new JsonProcessor();
        try (var stream = getClass().getResourceAsStream("test.json.ignoreCase")) {
            var message = processor.process(stream);
            assertThat(message.getData().getVariables()).hasSize(3);
            assertThat(message.getData().getRecipients()).hasSize(3);
            assertThat(message.getData().getTestRecipientIndex()).isEqualTo(0);
        }
    }

    @Test
    @DisplayName("Тестирует, что будет исключение, если файл пустой")
    public void testJsonEmptyFile() {
        assertThatExceptionOfType(TargetMessageFileFormatException.class)
                .isThrownBy(() -> {
                    try (InputStream stream = new ByteArrayInputStream(ArrayUtils.EMPTY_BYTE_ARRAY)) {
                        new JsonProcessor().process(stream);
                    }
                }).withMessage("Json file is empty");
    }

    @Test
    @DisplayName("Тестирует, что будет исключение, если в какой-то из строк нет поля с id")
    public void testJsonNoIdField() {
        assertThatExceptionOfType(DatasourceIDNotFoundException.class)
                .isThrownBy(() -> processJson("test.json.badId"))
                .withMessage("JSON file must contain field DATASOURCE_ID.");
    }

    @Test
    @DisplayName("Тестирует, что будет исключение, если пустой json")
    public void testJsonEmptyJson() {
        assertThatExceptionOfType(DatasourceIDNotFoundException.class)
                .isThrownBy(() -> processJson("test.json.empty"))
                .withMessage("JSON file must contain field DATASOURCE_ID.");
    }

    private void processJson(String resource) throws IOException {
        try (var stream = getClass().getResourceAsStream(resource)) {
            new JsonProcessor().process(stream);
        }
    }

    @Test
    @DisplayName("Тестирует, что процессор правильно выбирает формат, когда подан json")
    public void testCorrectChooseJsonFile() throws IOException {
        var processor = new TargetMessageProcessor();
        try (var stream = getClass().getResourceAsStream("test.json.good")) {
            var message = processor.processStream(stream);
            assertThat(message.getData().getVariables()).hasSize(3);
            assertThat(message.getData().getRecipients()).hasSize(3);
            assertThat(message.getData().getTestRecipientIndex()).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("Тестирует, что процессор правильно выбирает формат, когда подан csv")
    public void testCorrectChooseCSVFile() throws IOException {
        var processor = new TargetMessageProcessor();
        try (var stream = getClass().getResourceAsStream("targetMessage.ok.csv")) {
            var message = processor.processStream(stream);
            assertThat(message.getData().getVariables()).hasSize(2);
            assertThat(message.getData().getRecipients()).hasSize(3);
            assertThat(message.getData().getTestRecipientIndex()).isEqualTo(1);
        }
    }
}
