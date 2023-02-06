package ru.yandex.market.ff.service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.model.entity.ShopRequest;
import ru.yandex.market.ff.repository.ShopRequestRepository;
import ru.yandex.market.ff.service.implementation.drivers.DaasServiceImpl;
import ru.yandex.market.ff.service.implementation.drivers.DriversBookletActGenerationService;
import ru.yandex.market.ff.service.implementation.drivers.parsers.PlaceholdersParser;
import ru.yandex.market.ff.service.implementation.drivers.pdf.PdfGenerator;
import ru.yandex.market.ff.service.implementation.drivers.processors.CssProcessor;
import ru.yandex.market.ff.service.implementation.drivers.processors.HtmlPageProcessor;
import ru.yandex.market.ff.util.FileContentUtils;
import ru.yandex.market.logistics.calendaring.client.dto.BookingListResponseV2;
import ru.yandex.market.logistics.calendaring.client.dto.BookingResponseV2;
import ru.yandex.market.logistics.calendaring.client.dto.enums.BookingStatus;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DriversBookletActGenerationServiceTest extends ActGenerationServiceTest {

    private static final String DAAS_RESPONSE_JSON = "service/drivers/json/response_with_remove.json";
    private static final String DAAS_RESPONSE_EMPTY_JSON = "service/drivers/json/response_empty.json";
    private static final String CSS = "service/drivers/css/css.css";

    private static final String BOOKLET_HTML = "service/drivers/html/booklet.html";
    private static final String BOOKLET_NO_TIME_HTML = "service/drivers/html/booklet_no_time.html";
    private static final String BOOKLET_NO_TIME_WITHDRAW_HTML = "service/drivers/html/booklet_no_time_withdraw.html";
    private static final String BOOKLET_EMPTY_HTML = "service/drivers/html/booklet_empty.html";

    @Autowired
    private ShopRequestRepository shopRequestRepository;

    @Autowired
    private HtmlPageProcessor htmlPageProcessor;

    @Autowired
    private PlaceholdersParser placeholdersParser;

    @Autowired
    ConcreteEnvironmentParamService concreteEnvironmentParamService;

    private DaasServiceImpl mockDaasService;
    private CssProcessor mockCssProcessor;
    private DriversBookletActGenerationService driversBookletActGenerationService;

    @BeforeEach
    public void setFields() {
        mockCssProcessor = mock(CssProcessor.class);
        mockDaasService = mock(DaasServiceImpl.class);
        PdfGenerator pdfGenerator = mock(PdfGenerator.class);
        driversBookletActGenerationService = new DriversBookletActGenerationService(
                concreteEnvironmentParamService, mockDaasService, mockCssProcessor,
                htmlPageProcessor, placeholdersParser, pdfGenerator);
    }

    @Test
    @DatabaseSetup("classpath:service/drivers/requests-driver-booklet.xml")
    public void generateReport() throws IOException {
        when(csClient.getSlotByExternalIdentifiersV2(any(), any(), any())).thenReturn(
                new BookingListResponseV2(List.of(
                        new BookingResponseV2(1, "FFWF", "1", null, 1,
                                ZonedDateTime.of(2020, 7, 6, 12, 0, 0, 0, ZoneId.of("UTC")),
                                ZonedDateTime.of(2020, 7, 6, 13, 0, 0, 0, ZoneId.of("UTC")),
                                BookingStatus.ACTIVE, null, 172)
                ))
        );
        mockCss();
        setDaasServiceMockResponse(DAAS_RESPONSE_JSON);
        String htmlDocument = getBooklet(shopRequestRepository.findById(1L));
//        writeHtmlContentToFile(htmlDocument, "" + BOOKLET_HTML);
        assertions.assertThat(htmlDocument).isEqualToIgnoringWhitespace(loadHtml(BOOKLET_HTML));
    }

    @Test
    @DatabaseSetup("classpath:service/drivers/requests-driver-booklet.xml")
    public void generateReportForSupplyNoTime() throws IOException {
        mockCss();
        setDaasServiceMockResponse(DAAS_RESPONSE_JSON);
        String htmlDocument = getBooklet(shopRequestRepository.findById(2L));
//        writeHtmlContentToFile(htmlDocument, "" + BOOKLET_NO_TIME_HTML);
        assertions.assertThat(htmlDocument).isEqualToIgnoringWhitespace(loadHtml(BOOKLET_NO_TIME_HTML));
    }

    @Test
    @DatabaseSetup("classpath:service/drivers/requests-driver-booklet.xml")
    public void generateReportForCrossdockNoTime() throws IOException {
        mockCss();
        setDaasServiceMockResponse(DAAS_RESPONSE_JSON);
        String htmlDocument = getBooklet(shopRequestRepository.findById(4L));
//        writeHtmlContentToFile(htmlDocument, "" + BOOKLET_NO_TIME_HTML);
        assertions.assertThat(htmlDocument).isEqualToIgnoringWhitespace(loadHtml(BOOKLET_NO_TIME_HTML));
    }

    @Test
    @DatabaseSetup("classpath:service/drivers/requests-withdrawal.xml")
    public void generateReportForWithdrawNoTime() throws IOException {
        mockCss();
        setDaasServiceMockResponse(DAAS_RESPONSE_JSON);
        String htmlDocument = getBooklet(shopRequestRepository.findById(2L));
//        writeHtmlContentToFile(htmlDocument, "" + BOOKLET_NO_TIME_WITHDRAW_HTML);
        assertions.assertThat(htmlDocument).isEqualToIgnoringWhitespace(loadHtml(BOOKLET_NO_TIME_WITHDRAW_HTML));
    }

    @Test
    @DatabaseSetup("classpath:service/drivers/requests-driver-booklet.xml")
    public void generateReportNoSupplierError() throws IOException {
        mockCss();
        setDaasServiceMockResponse(DAAS_RESPONSE_JSON);
        assertThrows(RuntimeException.class, () -> getBooklet(shopRequestRepository.findById(5L)));
    }

    @Test
    @DatabaseSetup("classpath:service/drivers/requests-driver-booklet.xml")
    public void generateReportWithDaasError() {
        doThrow(RuntimeException.class).when(mockDaasService).getHtmlDocumentComponent(anyString(), anyString());
        assertThrows(RuntimeException.class,
                () -> driversBookletActGenerationService.generateReport(shopRequestRepository.findById(1L)));
    }

    @Test
    @DatabaseSetup("classpath:service/drivers/requests-driver-booklet.xml")
    public void generateEmptyReportWithUnknownWarehouse() throws IOException {
        mockCss();
        setDaasServiceMockResponse(DAAS_RESPONSE_EMPTY_JSON);
        String htmlDocument = getBooklet(shopRequestRepository.findById(3L));
//        writeHtmlContentToFile(htmlDocument, "" + BOOKLET_EMPTY_HTML);
        assertEquals(loadHtml(BOOKLET_EMPTY_HTML), htmlDocument);
    }

    @Test
    @DatabaseSetup("classpath:service/drivers/requests-driver-booklet.xml")
    public void generateEmptyReportWithWrongRequestType() throws IOException {
        mockCss();
        setDaasServiceMockResponse(DAAS_RESPONSE_EMPTY_JSON);
        String htmlDocument = getBooklet(shopRequestRepository.findById(6L));
        assertEquals(loadHtml(BOOKLET_EMPTY_HTML), htmlDocument);
    }

    @Test
    @DatabaseSetup("classpath:service/drivers/requests-driver-booklet.xml")
    public void parseTemplatesNamesEnvVar() {
        Map<Long, String> supplyTemplatesUrlForDriverBooklets =
                concreteEnvironmentParamService.getSupplyTemplatesUrlForDriverBooklets();
        assertTrue(supplyTemplatesUrlForDriverBooklets.size() > 0);
    }

    @Test
    public void notAbleToParseTemplatesNamesEnvVar() {
        Map<Long, String> supplyTemplatesUrlForDriverBooklets =
                concreteEnvironmentParamService.getSupplyTemplatesUrlForDriverBooklets();
        assertTrue(supplyTemplatesUrlForDriverBooklets.isEmpty());
    }

    private void setDaasServiceMockResponse(String jsonFile) throws IOException {
        when(mockDaasService.getHtmlDocumentComponent(anyString(), anyString()))
                .thenReturn(FileContentUtils.getFileContent(jsonFile));
    }

    private void mockCss() throws IOException {
        when(mockCssProcessor.loadCss(any(ShopRequest.class), anyString()))
                .thenReturn(FileContentUtils.getFileContent(CSS));
    }

    private String getBooklet(ShopRequest request) {
        String daasTemplate = mockDaasService.getHtmlDocumentComponent("", "");
        String css = mockCssProcessor.loadCss(request, daasTemplate);
        Document htmlDocument = htmlPageProcessor.generateHtml(daasTemplate, css);
        return placeholdersParser.getParsedHtml(htmlDocument, request, false).html();
    }

    private String loadHtml(String fileName) throws IOException {
        return IOUtils.toString(Objects.requireNonNull(getSystemResourceAsStream(fileName)),
                StandardCharsets.UTF_8).trim();
    }

    private void writeHtmlContentToFile(String html, String filePathAndName) throws IOException {
        FileUtils.writeStringToFile(new File(filePathAndName), html, StandardCharsets.UTF_8);
    }
}
