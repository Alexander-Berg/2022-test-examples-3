package ru.yandex.market.wms.receiving.controller;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import javax.servlet.http.Cookie;
import javax.swing.text.html.parser.DTD;
import javax.swing.text.html.parser.Parser;

import net.sf.jasperreports.engine.JasperPrint;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MvcResult;

import ru.yandex.market.wms.common.model.enums.AuthenticationParam;
import ru.yandex.market.wms.common.service.DbConfigService;
import ru.yandex.market.wms.receiving.ReceivingIntegrationTest;
import ru.yandex.market.wms.receiving.exception.ReportGenerationException;
import ru.yandex.market.wms.receiving.model.dto.report.BaseDiscrepanciesReportData;
import ru.yandex.market.wms.receiving.model.dto.report.DiscrepanciesPerBoxReportData;
import ru.yandex.market.wms.receiving.model.dto.report.DiscrepancyReportType;
import ru.yandex.market.wms.receiving.model.dto.report.ReportFormat;
import ru.yandex.market.wms.receiving.service.report.impl.DiscrepanciesReportDataService;
import ru.yandex.market.wms.receiving.service.report.impl.ReportExporterFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.receiving.service.report.impl.DiscrepanciesReportDataService.GENERATION_PER_PALLET_CONFIG;

class ReportControllerTest extends ReceivingIntegrationTest {

    @MockBean
    @Autowired
    private DiscrepanciesReportDataService reportDataService;
    @MockBean
    @Autowired
    private ReportExporterFactory exporterFactory;
    @MockBean
    @Autowired
    private DbConfigService configService;

    @Test
    public void getPdfDiscrepanciesReport() throws Exception {
        //given
        when(configService.getConfigAsBoolean(GENERATION_PER_PALLET_CONFIG)).thenReturn(false);
        when(reportDataService.getReportData(eq("0000054321"))).thenReturn(discrepancy());
        when(exporterFactory.getExporter(ReportFormat.PDF)).thenReturn(getExporter(ReportFormat.PDF));

        //when
        MvcResult mvcResult = mockMvc.perform(get("/report/discrepancies/0000054321?format=PDF")
                        .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        //then
        MockHttpServletResponse response = mvcResult.getResponse();
        assertThat("Content type", response.getContentType(), equalTo("application/pdf"));
        PDDocument document = PDDocument.load(response.getContentAsByteArray());
        assertThat("Document generated correctly", document.getNumberOfPages(), equalTo(5));
    }

    @Test
    public void getXlsDiscrepanciesReport() throws Exception {
        //given
        when(configService.getConfigAsBoolean(GENERATION_PER_PALLET_CONFIG)).thenReturn(false);
        when(reportDataService.getReportData(eq("0000054321"))).thenReturn(discrepancy());
        when(exporterFactory.getExporter(ReportFormat.XLS)).thenReturn(getExporter(ReportFormat.XLS));

        //when
        MvcResult mvcResult = mockMvc.perform(get("/report/discrepancies/0000054321?format=XLS")
                        .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        //then
        MockHttpServletResponse response = mvcResult.getResponse();
        assertThat("Content type", response.getContentType(),
                equalTo("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(response.getContentAsByteArray()));
        assertThat("Xls workbook generated correctly",
                workbook.sheetIterator().next().getSheetName(), equalTo("Page 1"));
    }

    @Test
    public void getHtmlDiscrepanciesReport() throws Exception {
        //given
        when(configService.getConfigAsBoolean(GENERATION_PER_PALLET_CONFIG)).thenReturn(false);
        when(reportDataService.getReportData(eq("0000054321"))).thenReturn(discrepancy());
        when(exporterFactory.getExporter(ReportFormat.HTML)).thenReturn(getExporter(ReportFormat.HTML));

        //when
        MvcResult mvcResult = mockMvc.perform(get("/report/discrepancies/0000054321?format=HTML")
                        .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        //then
        MockHttpServletResponse response = mvcResult.getResponse();
        assertThat("Content type", response.getContentType(), equalTo("text/html"));
        Parser parser = new Parser(DTD.getDTD("html32"));
        parser.parse(new InputStreamReader(new ByteArrayInputStream(response.getContentAsByteArray())));
        assertThat("Html document is parsed", true);
    }

    @Test
    public void getHtmlDiscrepanciesReportByName() throws Exception {
        //given
        when(configService.getConfigAsBoolean(GENERATION_PER_PALLET_CONFIG)).thenReturn(false);
        when(reportDataService.getReportData(eq("0000054321"), eq(DiscrepancyReportType.INITIAL)))
                .thenReturn(discrepancy());
        when(exporterFactory.getExporter(ReportFormat.HTML)).thenReturn(getExporter(ReportFormat.HTML));

        //when
        MvcResult mvcResult = mockMvc.perform(get("/report/discrepancies/0000054321/INITIAL?format=HTML")
                        .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        //then
        MockHttpServletResponse response = mvcResult.getResponse();
        assertThat("Content type", response.getContentType(), equalTo("text/html"));
        Parser parser = new Parser(DTD.getDTD("html32"));
        parser.parse(new InputStreamReader(new ByteArrayInputStream(response.getContentAsByteArray())));
        assertThat("Html document is parsed", true);
    }

    @Test
    public void getDiscrepanciesReport_emptyReceiptKey() throws Exception {
        //given
        var reportData = discrepancy();
        reportData.setActNumber(null);
        when(configService.getConfigAsBoolean(GENERATION_PER_PALLET_CONFIG)).thenReturn(false);
        when(reportDataService.getReportData(eq("0000054321"))).thenReturn(reportData);

        //when
        MvcResult result = mockMvc.perform(get("/report/discrepancies/0000054321/?format=PDF")
                        .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andReturn();

        //then
        String content = result.getResponse().getContentAsString();
        assertThat("Content", content, containsString("actNumber: must not be blank"));
    }

    @Test
    public void getDiscrepanciesReport_unsupportableFormat() throws Exception {
        mockMvc.perform(get("/report/discrepancies/0000054321?format=XML")
                        .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void getDiscrepanciesReport_generationException() throws Exception {
        //given
        when(configService.getConfigAsBoolean(GENERATION_PER_PALLET_CONFIG)).thenReturn(false);
        when(reportDataService.getReportData(eq("0000054321"))).thenReturn(discrepancy());
        when(exporterFactory.getExporter(any()))
                .thenReturn((jasperPrint, outputStream) -> {
                    throw new ReportGenerationException("Error generation report");
                });

        //when
        MvcResult mvcResult = mockMvc.perform(get("/report/discrepancies/0000054321?format=HTML")
                        .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), "TEST"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is5xxServerError()).andReturn();

        //then
        String content = mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat("Content", content, containsString(
                "{\"message\":\"500 INTERNAL_SERVER_ERROR \\\"Error generation " +
                "report\\\"\",\"status\":\"INTERNAL_SERVER_ERROR\"," +
                        "\"wmsErrorCode\":\"UNDEFINED\"}"));
    }

    private DiscrepanciesPerBoxReportData discrepancy() {
        DiscrepanciesPerBoxReportData discrepanciesReportData = new DiscrepanciesPerBoxReportData();
        discrepanciesReportData.setActNumber("Ю123");
        discrepanciesReportData.setSupplyNumber("012376");
        discrepanciesReportData.setCarrierName("ООО Поставщик товара");
        discrepanciesReportData.setRegistryNumber("reg000555000");
        discrepanciesReportData.setShipFromAddress("Послано с адреса");
        discrepanciesReportData.setReceivingDate(LocalDateTime.of(2021, 1, 2, 3, 3, 4));
        discrepanciesReportData.setOrderDate(LocalDate.of(2021, 1, 1));
        discrepanciesReportData.setWarehouseAddress(
                "Сософьино, Омсковская область, Районный район, ул. Нестроителей 2");
        discrepanciesReportData.setAgentName("ООО Шире вселенной горе моё");
        List<BaseDiscrepanciesReportData.Discrepancy> discrepancies = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            discrepancies.add(discrepancy(i));
        }
        discrepanciesReportData.setDiscrepancyList(discrepancies);
        discrepanciesReportData.setPrintingDate(LocalDateTime.now(ZoneId.systemDefault()));
        fillSummaries(discrepanciesReportData);
        return discrepanciesReportData;
    }

    private void fillSummaries(DiscrepanciesPerBoxReportData discrepanciesReportData) {
        discrepanciesReportData.getDiscrepancySummaries()
                .add(summary("Итого: не принято (некорректный склад возврата)", 1));
        discrepanciesReportData.getDiscrepancySummaries()
                .add(summary("Итого: не принято (статус заказа «50 Доставлен»)", 2));
        discrepanciesReportData.getDiscrepancySummaries()
                .add(summary("Итого: не принято (превышен срок возврата заказа)", 3));
        discrepanciesReportData.getDiscrepancySummaries()
                .add(summary("Итого: недостача (заявлен в реестре, но отсутствует)", 4));
        discrepanciesReportData.getDiscrepancySummaries()
                .add(summary("Итого: дропшип (не обрабатывается на ФФЦ Яндекс.Маркета)", 5));
    }

    private BaseDiscrepanciesReportData.Discrepancy discrepancy(int i) {
        return BaseDiscrepanciesReportData.Discrepancy.builder()
                .number(i)
                .externalNumber("ext12345" + i)
                .dispatchNumber("sending8" + i)
                .discrepancyType("Недостача" + i)
                .value(5 + i)
                .build();
    }

    private BaseDiscrepanciesReportData.Summary summary(String name, int value) {
        return new BaseDiscrepanciesReportData.Summary(name, value);
    }

    private BiConsumer<JasperPrint, OutputStream> getExporter(ReportFormat reportFormat) {
        return new ReportExporterFactory().getExporter(reportFormat);
    }

}
