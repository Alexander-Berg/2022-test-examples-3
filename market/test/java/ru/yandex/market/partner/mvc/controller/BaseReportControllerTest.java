package ru.yandex.market.partner.mvc.controller;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.http.HttpMethod;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import ru.yandex.common.util.csv.CSVReader;
import ru.yandex.market.partner.PartnerReportGeneratorTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

public abstract class BaseReportControllerTest extends PartnerReportGeneratorTest {

    protected Sheet queryForSheet(String url) {
        return queryForSheet(url, false);
    }

    protected Sheet queryForSheet(String url, boolean saveToTemporaryFile) {
        return queryForWorkbook(url, saveToTemporaryFile).getSheetAt(0);
    }

    protected Workbook queryForWorkbook(String url) {
        return queryForWorkbook(url, false);
    }

    protected Workbook queryForWorkbook(String url, boolean saveToTemporaryFile) {
        return queryForWorkbook(URI.create(baseUrl + url), saveToTemporaryFile);
    }

    protected Workbook queryForWorkbook(URI uri) {
        return queryForWorkbook(uri, false);
    }

    protected Workbook queryForWorkbook(URI uri, boolean saveToTemporaryFile) {
        return queryForReport(uri, saveToTemporaryFile, WorkbookFactory::create, ".xls");
    }

    protected CSVReader queryForCsv(String url) {
        return queryForCsv(url, false);
    }

    protected CSVReader queryForCsv(String url, boolean saveToTemporaryFile) {
        return queryForCsv(URI.create(baseUrl + url), saveToTemporaryFile);
    }

    protected CSVReader queryForCsv(URI uri) {
        return queryForCsv(uri, false);
    }

    protected CSVReader queryForCsv(URI uri, boolean saveToTemporaryFile) {
        return queryForReport(uri, saveToTemporaryFile, CSVReader::new, ".csv");
    }

    protected Document queryForXml(String url) {
        return queryForXml(url, false);
    }

    protected Document queryForXml(String url, boolean saveToTemporaryFile) {
        return queryForXml(URI.create(baseUrl + url), saveToTemporaryFile);
    }

    protected Document queryForXml(URI uri) {
        return queryForXml(uri, false);
    }

    protected Document queryForXml(URI uri, boolean saveToTemporaryFile) {
        return queryForReport(uri, saveToTemporaryFile, this::getDocument, ".csv");
    }

    protected <T> T queryForCustom(String url, ReportReader<T> builder) {
        return queryForCustom(url, false, builder);
    }

    protected <T> T queryForCustom(String url, boolean saveToTemporaryFile, ReportReader<T> builder) {
        return queryForCustom(URI.create(baseUrl + url), saveToTemporaryFile, builder);
    }

    protected <T> T queryForCustom(URI uri, ReportReader<T> builder) {
        return queryForCustom(uri, false, builder);
    }

    protected <T> T queryForCustom(URI uri, boolean saveToTemporaryFile, ReportReader<T> builder) {
        return queryForReport(uri, saveToTemporaryFile, builder, ".csv");
    }

    private Document getDocument(InputStream input) throws IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        factory.setIgnoringComments(true);
        factory.setIgnoringElementContentWhitespace(true);
        factory.setValidating(false);

        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(new InputSource(input));
        } catch (ParserConfigurationException | SAXException parserConfigurationException) {
            throw new IOException("Cannot parse xml " + parserConfigurationException.getMessage());
        }
    }

    private <T> T queryForReport(URI uri,
                                 boolean saveToTemporaryFile,
                                 ReportReader<T> builder,
                                 String extension) {
        return FunctionalTestHelper.execute(
                uri,
                HttpMethod.GET,
                null,
                response -> {
                    InputStream result;
                    if (!saveToTemporaryFile) {
                        result = response.getBody();
                    } else {
                        Path tmp = Files.createTempFile("tmp", extension);
                        Files.copy(response.getBody(), tmp, StandardCopyOption.REPLACE_EXISTING);
                        System.out.println("Generated report saved to " + tmp);
                        result = Files.newInputStream(tmp);
                    }
                    return builder.apply(result);
                });
    }

    @FunctionalInterface
    public interface ReportReader<T> {
        T apply(InputStream inputStream) throws IOException;
    }
}
