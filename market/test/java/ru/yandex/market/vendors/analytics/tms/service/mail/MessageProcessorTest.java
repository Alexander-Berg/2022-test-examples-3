package ru.yandex.market.vendors.analytics.tms.service.mail;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.vendors.analytics.tms.FunctionalTest;

/**
 * Тест на обработчик {@link MessageProcessor}
 *
 * @author sergeymironov
 */
public class MessageProcessorTest extends FunctionalTest {

    @Autowired
    MessageProcessor messageProcessor;

    private final Date msgReceivedTimeDate = java.util.Date.from(LocalDate
            .of(2014, 2, 11)
            .atStartOfDay(ZoneId.of("Europe/Moscow"))
            .toInstant());

    @Test
    @DisplayName("Отказ в дальнейшей обработке файла при попытке распарсить имя файла")
    void getParseError() {
        String filenameError1 = "domain.ru1.5.2019.xLsx";
        String filenameError2 = "domainru_1.5.2019.xlS";
        String filenameError3 = "domain.ru_1.5.2019.exe";
        String filenameError4 = "domain.ru_1.15.2019.csv";
        byte[] content = new byte[]{1, 2, 3};
        var actual = new ArrayList<String>();

        messageProcessor.processFile(filenameError1, content, msgReceivedTimeDate, actual);
        messageProcessor.processFile(filenameError2, content, msgReceivedTimeDate, actual);
        messageProcessor.processFile(filenameError3, content, msgReceivedTimeDate, actual);
        messageProcessor.processFile(filenameError4, content, msgReceivedTimeDate, actual);

        var expected = new ArrayList<String>();
        expected.add(filenameError1 + ". Error in filename. can't parse with _");
        expected.add(filenameError2 + ". Error in filename. can't parse domain: domainru");
        expected.add(filenameError3 + ". Error in filename. cannot find enum of type FileType by id exe");
        expected.add(filenameError4 + ". Invalid date. Invalid value for MonthOfYear (valid values 1 - 12): 15");

        JsonAssert.assertJsonEquals(expected, actual);
    }

    @Test
    @DisplayName("Успешное чтение обязательных хедеров и содержание файла CSV")
    void readCSV() {
        validateHeadersAndContent("correct.content_1.5.2019.csv");
    }

    @Test
    @DisplayName("Успешное чтение обязательных хедеров и содержание файла с BOM")
    void readFileWithBOM() {
        validateHeadersAndContent("correct.with.bom_31.03.2019.csv");
    }

    @Test
    @DisplayName("Успешное чтение обязательных хедеров и содержание файла XLS")
    void readXLS() {
        validateHeadersAndContent("correct.content_02.12.2019.xls");
    }

    @Test
    @DisplayName("Успешное чтение обязательных хедеров и содержание файла XLSX")
    void readXLSX() {
        validateHeadersAndContent("correct.content_19.11.2019.xlsx");
    }

    @Test
    @DisplayName("Отсутствие обязательных хедеров в файле CSV")
    void getCSVHeadersError() {
        var actual = getActual("error-headers.ru_1.5.2019.csv");
        var expected = Collections.singletonList("error-headers.ru_1.5.2019.csv. Error in content. wrong headers.");

        JsonAssert.assertJsonEquals(expected, actual);
    }

    @Test
    @DisplayName("Ошибка чтения числа в содержании файла")
    void getNumberFormatError() {
        var actual = getActual("error.in.content_25.12.2019.csv");
        var expected = Collections.singletonList("error.in.content_25.12.2019.csv. Error in content. For input string: \"не число\"");

        JsonAssert.assertJsonEquals(expected, actual);
    }

    private void validateHeadersAndContent(String fileName) {
        int actual = getActual(fileName).size();
        int expected = 0;

        JsonAssert.assertJsonEquals(expected, actual);
    }

    private List<String> getActual(String fileName) {
        var nonvalidFiles = new ArrayList<String>();

        Class contextClass = getClass();
        try (InputStream in = contextClass.getResourceAsStream(fileName)) {
            byte[] content = in.readAllBytes();
            messageProcessor.processFile(fileName, content, msgReceivedTimeDate, nonvalidFiles);
        } catch (final IOException ex) {
            throw new UncheckedIOException("Could not read file " + fileName, ex);
        }
        return nonvalidFiles;
    }
}
