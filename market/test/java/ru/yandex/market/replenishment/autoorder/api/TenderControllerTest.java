package ru.yandex.market.replenishment.autoorder.api;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import com.amazonaws.services.s3.AmazonS3;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellAddress;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.client.RestTemplate;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.common.test.db.DbUnitDataBaseConfig;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbo.excel.ExcelFileAssertions;
import ru.yandex.market.mbo.excel.StreamExcelParser;
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.market.mboc.http.MboMappingsService;
import ru.yandex.market.replenishment.autoorder.api.dto.EditItemsCountInTenderResultDTO;
import ru.yandex.market.replenishment.autoorder.api.dto.SendLettersRequest;
import ru.yandex.market.replenishment.autoorder.api.dto.user_fillters.recommendation.RecommendationFilters;
import ru.yandex.market.replenishment.autoorder.api.dto.user_fillters.recommendation.RecommendationUserFilter;
import ru.yandex.market.replenishment.autoorder.api.dto.user_fillters.tender_result.TenderResultField;
import ru.yandex.market.replenishment.autoorder.api.dto.user_fillters.tender_result.TenderResultUserFilter;
import ru.yandex.market.replenishment.autoorder.config.ControllerTest;
import ru.yandex.market.replenishment.autoorder.config.ExcelTestingHelper;
import ru.yandex.market.replenishment.autoorder.model.AxHandlerResponse;
import ru.yandex.market.replenishment.autoorder.model.PriceSpecificationStatus;
import ru.yandex.market.replenishment.autoorder.model.RecommendationFilter;
import ru.yandex.market.replenishment.autoorder.model.SskuStatus;
import ru.yandex.market.replenishment.autoorder.model.TenderStatus;
import ru.yandex.market.replenishment.autoorder.model.dto.SupplierRecommendationExcelDTO;
import ru.yandex.market.replenishment.autoorder.openapi.autoorder.model.AxaptaPriceSpecStatus;
import ru.yandex.market.replenishment.autoorder.openapi.autoorder.model.PriceSpecificationAxaptaRequest;
import ru.yandex.market.replenishment.autoorder.openapi.autoorder.model.PriceSpecificationAxaptaState;
import ru.yandex.market.replenishment.autoorder.repository.postgres.TenderRepository;
import ru.yandex.market.replenishment.autoorder.security.WithMockLogin;
import ru.yandex.market.replenishment.autoorder.service.TimeService;
import ru.yandex.market.replenishment.autoorder.service.client.Cabinet1PClient;
import ru.yandex.market.replenishment.autoorder.service.excel.SupplierSskuTemplateWriter;
import ru.yandex.market.replenishment.autoorder.service.excel.core.reader.BaseExcelReader;
import ru.yandex.market.replenishment.autoorder.service.user_filters.base.UserFilterFieldPredicate;
import ru.yandex.market.replenishment.autoorder.utils.TestUtils;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.replenishment.autoorder.api.dto.user_fillters.recommendation.RecommendationField.MSKU;
import static ru.yandex.market.replenishment.autoorder.service.excel.core.reader.BaseExcelReader.getMapper;
import static ru.yandex.market.replenishment.autoorder.utils.AxHandlerResponseUtils.getMockedResponseEntity;

@DbUnitDataBaseConfig({@DbUnitDataBaseConfig.Entry(
    name = "tableType",
    value = "TABLE,VIEW"
)})
@WithMockLogin
@MockBean(Cabinet1PClient.class)
public class TenderControllerTest extends ControllerTest {

    @Value("${ax.handlers}")
    private String axaptaServerUrl;

    @Autowired
    @Qualifier("axRestTemplate")
    private RestTemplate axRestTemplate;

    @Autowired
    SqlSessionFactory sqlSessionFactory;

    @Autowired
    JavaMailSender javaMailSender;

    @Autowired
    private MboMappingsService mboMappingService;

    @Autowired
    private AmazonS3 s3Client;

    @Autowired
    private TimeService timeService;

    @Autowired
    private Cabinet1PClient cabinet1PClient;

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    private final ExcelTestingHelper excelTestingHelper = new ExcelTestingHelper(this);

    private static final List<String> HEADERS_EXPECTED = List.of(
        "Бренд", "Категория 1", "Категория 2", "Категория 3",
        "Название товара", "MSKU", "SSKU", "Артикул", "Штрихкоды",
        "Потребность", "кол-во", "цена", "НДС, %", "валюта",
        "комментарий\n(здесь вы можете указать ваш комментарий по товару. Наличие других цветов, доступность и др.)"
    );

    private static final List<String> HEADERS_EXPECTED_EXPORT_SUPPLIER_RESPONSES = List.of(
        "MSKU", "SSKU", "Название", "Итоговая цена закупки", "Количество",
        "Склад", "ЗП", "Номер счета", "Привезем", "Комментарий", "Demand id", "Тип поставки"
    );

    @Before
    public void clearUsersCacheManger() {
        setTestTime(LocalDateTime.of(2020, 9, 6, 0, 0));
    }

    @Before
    public void setMboAndS3Mocks() throws MalformedURLException {
        long requestId = 1L;
        when(mboMappingService.uploadExcelFile(any())).thenReturn(
            MboMappings.OfferExcelUpload.Response.newBuilder()
                .setRequestId(requestId)
                .build()
        );
        when(s3Client.getUrl(any(), any())).thenReturn(new URL("http://localhost:65432/test_url"));
    }

    private RecommendationFilters createFilters() {
        var filter = new RecommendationFilter();
        filter.setDemandIds(List.of(1L));
        var filters = new RecommendationFilters();
        filters.setFilter(filter);
        return filters;
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest.recommendations-to-supplier.before.csv")
    public void testExportRecommendationsToSupplier() throws Exception {
        byte[] excelData = mockMvc.perform(post("/api/v1/tender-demands/1/excel")
                .contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(createFilters()))
            )
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();

        List<List<Object>> lists = BaseExcelReader.extractFromExcel(
            new ByteArrayInputStream(excelData),
            getMapper(HEADERS_EXPECTED.size()),
            1,
            2
        );

        assertEquals(3, lists.size());
        assertEquals(HEADERS_EXPECTED, lists.get(0));

        lists.remove(0);

        assertEquals(List.of(
            "JobsAndCompany",
            "Пинетки из крокодила",
            "валенки",
            "валенки валенки да не пошиты стареньки",
            "msku_2",
            200.0,
            "",
            "XX000X777",
            "123, 321",
            9.0,
            "",
            "",
            20.0,
            "",
            ""
        ), lists.get(0));
        assertEquals(List.of(
            "JobsAndCompany",
            "all",
            "all",
            "пинетки",
            "msku_3",
            300.0,
            "",
            "XX000X777",
            "300, 500",
            42.0,
            "",
            "",
            20.0,
            "",
            ""
        ), lists.get(1));
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest_testExportSupplierResultXdoc.before.csv")
    public void testExportSupplierResult() throws Exception {
        byte[] excelData = mockMvc.perform(get("/api/v1/tender/3/1/result"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();

        checkSupplierResultXls(excelData, "X-Dock");
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest_testExportSupplierResultMonoXdoc.before.csv")
    public void testExportSupplierResultByPartnerId() throws Exception {
        byte[] excelData = mockMvc.perform(get("/api/v1/tender/partner/3/11/result"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();

        checkSupplierResultXls(excelData, "Mono-Xdock");
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest_testExportSupplierResultNoDemands.before.csv")
    public void testExportSupplierResultNoDemands() throws Exception {
        byte[] excelData = mockMvc.perform(get("/api/v1/tender/3/1/result"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();

        checkSupplierResultNoDemandsXls(excelData);
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest_testExportSupplierResultNoDemands.before.csv")
    public void testExportSupplierResultByPartnerIdNoDemands() throws Exception {
        byte[] excelData = mockMvc.perform(get("/api/v1/tender/partner/3/11/result"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();

        checkSupplierResultNoDemandsXls(excelData);
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest.recommendations-to-supplier.before.csv")
    public void testExportRecommendationsToSupplier_withFilter() throws Exception {
        var filters = createFilters();
        var userFilter = new RecommendationUserFilter();
        userFilter.setField(MSKU);
        userFilter.setPredicate(UserFilterFieldPredicate.EQUAL);
        userFilter.setValue("200");
        filters.setUserFilter(Collections.singletonList(userFilter));

        byte[] excelData = mockMvc.perform(post("/api/v1/tender-demands/1/excel")
                .contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(filters))
            )
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();

        List<List<Object>> lists = BaseExcelReader.extractFromExcel(
            new ByteArrayInputStream(excelData),
            getMapper(HEADERS_EXPECTED.size()),
            1,
            2
        );

        assertEquals(2, lists.size());
        assertEquals(HEADERS_EXPECTED, lists.get(0));

        assertEquals(List.of(
            "JobsAndCompany",
            "Пинетки из крокодила",
            "валенки",
            "валенки валенки да не пошиты стареньки",
            "msku_2",
            200.0,
            "",
            "XX000X777",
            "123, 321",
            9.0,
            "",
            "",
            20.0,
            "",
            ""
        ), lists.get(1));
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest.recommendations-to-supplier2.before.csv")
    public void testExportRecommendationsToSupplier_ignoreZeroQty() throws Exception {
        byte[] excelData = mockMvc.perform(post("/api/v1/tender-demands/1/excel")
                .contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(createFilters()))
            )
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();

        List<List<Object>> lists = BaseExcelReader.extractFromExcel(
            new ByteArrayInputStream(excelData),
            getMapper(HEADERS_EXPECTED.size()),
            1,
            2
        );

        assertEquals(2, lists.size());
        assertEquals(HEADERS_EXPECTED, lists.get(0));
        assertEquals(List.of(
            "JobsAndCompany",
            "Пинетки из крокодила",
            "валенки",
            "валенки валенки да не пошиты стареньки",
            "msku_2",
            200.0,
            "",
            "XX000X777",
            "123, 321",
            9.0,
            "",
            "",
            20.0,
            "",
            ""
        ), lists.get(1));
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest.recommendations-to-supplier3.before.csv")
    public void testExportRecommendationsToSupplier_readAdjustedQty() throws Exception {
        byte[] excelData = mockMvc.perform(post("/api/v1/tender-demands/1/excel")
                .contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(createFilters()))
            )
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();

        List<List<Object>> lists = BaseExcelReader.extractFromExcel(
            new ByteArrayInputStream(excelData),
            getMapper(HEADERS_EXPECTED.size()),
            1,
            2
        );

        assertEquals(3, lists.size());
        assertEquals(HEADERS_EXPECTED, lists.get(0));
        assertEquals(List.of(
            "JobsAndCompany",
            "Пинетки из крокодила",
            "валенки",
            "валенки валенки да не пошиты стареньки",
            "msku_2",
            200.0,
            "",
            "XX000X777",
            "123, 321",
            9.0,
            "",
            "",
            20.0,
            "",
            ""
        ), lists.get(1));
        assertEquals(List.of(
            "JobsAndCompany",
            "all",
            "all",
            "пинетки",
            "msku_3",
            300.0,
            "",
            "XX000X777",
            "300, 500",
            20.0,
            "",
            "",
            20.0,
            "",
            ""
        ), lists.get(2));
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest_importResponseFromSupplier.before.csv",
        after = "TenderControllerTest_importResponseFromSupplier.after.csv")
    public void testImportResponseFromSupplier_isOk_withoutSsku() throws Exception {
        TestUtils.mockTimeService(timeService, LocalDateTime.of(2021, 6, 25, 18, 0, 0));

        excelTestingHelper.upload(
                "POST",
                "/api/v1/tender/2/10/excel",
                "TenderControllerTest_importResponseFromSupplier.xlsx"
            ).andExpect(status().isOk())
            .andExpect(jsonPath("$").value("false"));
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest_importResponseFromSupplier.before.csv")
    public void testImportResponseFromSupplier_isBad_negativePriceCount() throws Exception {
        TestUtils.mockTimeService(timeService, LocalDateTime.of(2021, 6, 25, 18, 0, 0));

        excelTestingHelper.upload(
                "POST",
                "/api/v1/tender/2/10/excel",
                "TenderControllerTest_importResponseFromSupplierNegativeNumbers.xlsx"
            ).andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message")
                .value("В строке 5 указано отрицательное значение в ячейке 11.\\n" +
                    "В строке 7 указано отрицательное значение в ячейке 12."));
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest_importResponseFromSupplier.before.csv",
        after = "TenderControllerTest_importResponseFromSupplier_testOrderId.after.csv")
    public void testImportResponseFromSupplier_orderId() throws Exception {
        TestUtils.mockTimeService(timeService, LocalDateTime.of(2021, 6, 25, 18, 0, 0));

        excelTestingHelper.upload(
                "POST",
                "/api/v1/tender/2/10/excel",
                "TenderControllerTest_importResponseFromSupplier.xlsx"
            ).andExpect(status().isOk())
            .andExpect(jsonPath("$").value("false"));
        try (var session = sqlSessionFactory.openSession()) {
            var tenderRepository = session.getMapper(TenderRepository.class);
            assertTrue(tenderRepository.findAllByDemandIdAndSupplierIds(2L, List.of(10L))
                .get(0).getPriceSpec().contains("\"orderId\": \"2342342423\""));
        }
        excelTestingHelper.upload(
            "POST",
            "/api/v1/tender/2/10/excel",
            "TenderControllerTest_importResponseFromSupplier_testOrderId.xlsx"
        ).andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest_testImportResponseFromSupplier_orderId_noPartner.before.csv",
        after = "TenderControllerTest_testImportResponseFromSupplier_orderId_noPartner.after.csv")
    public void testImportResponseFromSupplier_orderId_noPartner() throws Exception {
        TestUtils.mockTimeService(timeService, LocalDateTime.of(2021, 6, 25, 18, 0, 0));

        excelTestingHelper.upload(
            "POST",
            "/api/v1/tender/2/10/excel",
            "TenderControllerTest_importResponseFromSupplier_testOrderId.xlsx"
        ).andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest_importResponseFromSupplier_noAgreementIdInPdbFail.before.csv")
    public void testImportResponseFromSupplier_noAgreementIdInPdbFail() throws Exception {
        TestUtils.mockTimeService(timeService, LocalDateTime.of(2021, 6, 25, 18, 0, 0));
        // из Аксапты возвращаются номера договоров по данному поставщику, но указанного нет -> ошибка
        excelTestingHelper.upload(
                "POST",
                "/api/v1/tender/2/10/excel",
                "TenderControllerTest_importResponseFromSupplier.xlsx"
            ).andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message")
                .value("Номер договора: 2342342423 не найден в базе для поставщика: 10"));
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest_importResponseFromSupplier_noAgreementIdInPdbOk.before.csv")
    public void testImportResponseFromSupplier_noAgreementIdInPdbOk() throws Exception {
        TestUtils.mockTimeService(timeService, LocalDateTime.of(2021, 6, 25, 18, 0, 0));
        // в Аксапте нет договоров по данному поставщику -> не ошибка
        excelTestingHelper.upload(
            "POST",
            "/api/v1/tender/2/10/excel",
            "TenderControllerTest_importResponseFromSupplier.xlsx"
        ).andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest_importResponseFromSupplier_with_ssku.before.csv",
        after = "TenderControllerTest_importResponseFromSupplier.after.csv")
    public void testImportResponseFromSupplier_isOk_with_ssku() throws Exception {
        TestUtils.mockTimeService(timeService, LocalDateTime.of(2021, 6, 25, 18, 0, 0));

        excelTestingHelper.upload(
                "POST",
                "/api/v1/tender/2/10/excel",
                "TenderControllerTest_importResponseFromSupplier.xlsx"
            ).andExpect(status().isOk())
            .andExpect(jsonPath("$").value("true"));
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest_importResponseFromSupplier_with_ssku.before.csv",
        after = "TenderControllerTest_importResponseFromSupplier.after.csv")
    public void testImportResponseFromSupplierByPartner_isOk_with_ssku() throws Exception {
        TestUtils.mockTimeService(timeService, LocalDateTime.of(2021, 6, 25, 18, 0, 0));

        excelTestingHelper.upload(
                "POST",
                "/api/v1/tender/partner/2/10/excel",
                "TenderControllerTest_importResponseFromSupplier.xlsx"
            ).andExpect(status().isOk())
            .andExpect(jsonPath("$").value("true"));
    }

    @Test
    @DbUnitDataSet(before = "testImportResponseFromAnonSupplierByPartner_isOk.before.csv",
        after = "testImportResponseFromAnonSupplierByPartner_isOk.after.csv")
    public void testImportResponseFromAnonSupplierByPartner_isOk() throws Exception {
        when(cabinet1PClient.getRsId(anyLong())).thenReturn(null);
        TestUtils.mockTimeService(timeService, LocalDateTime.of(2021, 6, 25, 18, 0, 0));

        excelTestingHelper.upload(
                "POST",
                "/api/v1/tender/partner/2/1/excel",
                "TenderControllerTest_importResponseFromSupplier.xlsx"
            ).andExpect(status().isOk())
            .andExpect(jsonPath("$").value("true"));
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest_importResponseFromSupplier_noMskus.before.csv")
    public void testImportResponseFromSupplier_noMskus() throws Exception {
        excelTestingHelper.upload(
                "POST",
                "/api/v1/tender/2/10/excel",
                "TenderControllerTest_importResponseFromSupplier.xlsx"
            ).andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message")
                .value("Товара с msku = 100358093750 нет в системе\\n" +
                    "Товара с msku = 100573868915 нет в системе"));
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest_importResponseFromSupplier.before.csv")
    public void testImportResponseFromSupplier_wrongDemandId() throws Exception {
        excelTestingHelper.upload(
                "POST",
                "/api/v1/tender/123123123/10/excel",
                "TenderControllerTest_importResponseFromSupplier.xlsx"
            ).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message")
                .value("Потребности с типом TENDER и c id 123123123 нет в базе, вероятно произошел реимпорт " +
                    "рекомендаций и у них поменялись id, попробуйте снова найти их на экране с " +
                    "календарем и открыть заново (не переживайте, ваши корректировки не были " +
                    "перезаписаны)"));
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest_importResponseFromSupplier.before.csv")
    public void testImportResponseFromSupplier_wrongSupplierId() throws Exception {
        excelTestingHelper.upload(
                "POST",
                "/api/v1/tender/2/123123123/excel",
                "TenderControllerTest_importResponseFromSupplier.xlsx"
            ).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message")
                .value("Поставщик с указанным id не существует 123123123"));
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest_importResponseFromSupplier.before.csv")
    public void testImportResponseFromSupplier_wrongExcel() throws Exception {
        excelTestingHelper.upload(
                "POST",
                "/api/v1/tender/2/10/excel",
                "TenderControllerTest_importResponseFromSupplier.wrong.xlsx"
            ).andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message")
                .value("Error int value in cell (12, 10): For input string: \"0.5\"."));
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest_importResponseFromSupplier.before.csv")
    public void testImportResponseFromSupplier_emptyExcel() throws Exception {
        excelTestingHelper.upload(
                "POST",
                "/api/v1/tender/2/10/excel",
                "TenderControllerTest_importResponseFromSupplier.empty.xlsx"
            ).andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message")
                .value("Необходимо заполнить шаблон!"));
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest_importResponseFromSupplier_savedSSKU.before.csv",
        after = "TenderControllerTest_importResponseFromSupplier_savedSSKU.after.csv")
    public void testImportResponseFromSupplier_savedSSKU() throws Exception {
        TestUtils.mockTimeService(timeService, LocalDateTime.of(2021, 6, 25, 18, 0, 0));

        excelTestingHelper.upload(
                "POST",
                "/api/v1/tender/2/10/excel",
                "TenderControllerTest_importResponseFromSupplier_withSsku.xlsx"
            ).andExpect(status().isOk())
            .andExpect(jsonPath("$").value("true"));
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest_importResponseFromSupplier_badSSKU.before.csv")
    public void testImportResponseFromSupplier_badSSKU() throws Exception {
        excelTestingHelper.upload(
                "POST",
                "/api/v1/tender/2/10/excel",
                "TenderControllerTest_importResponseFromSupplier_withSsku.xlsx"
            ).andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message")
                .value("Для MSKU 100358093750 и поставщика 10 отсутствуют SSKU '456'"));
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest.calculateTenderTest.before.csv")
    public void calculateTenderWrongDemandIdTest() throws Exception {
        mockMvc.perform(post("/api/v1/tender/123/calculate")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message")
                .value("Потребности с типом TENDER и c id 123 нет в базе, " +
                    "вероятно произошел реимпорт рекомендаций и у них поменялись id, " +
                    "попробуйте снова найти их на экране с календарем и открыть заново " +
                    "(не переживайте, ваши корректировки не были перезаписаны)"));
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest.getTenderResults.before.csv")
    public void getTenderResultsTest() throws Exception {
        mockMvc.perform(post("/api/v2/tender/1/result")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.results.length()").value(1))
            .andExpect(jsonPath("$.userFiltersCount.length()").value(0))
            .andExpect(jsonPath("$.results[0].msku").value(1))
            .andExpect(jsonPath("$.results[0].ssku").value("2.1"))
            .andExpect(jsonPath("$.results[0].supplierId").value(2))
            .andExpect(jsonPath("$.results[0].supplier").value("The Supplier"))
            .andExpect(jsonPath("$.results[0].price").value(41.0))
            .andExpect(jsonPath("$.results[0].comment").value(
                "Цена выше стоимости MSKU на сайте, но меньше минимальной цены конкурентов"))
            .andExpect(jsonPath("$.results[0].items").value(20))
            .andExpect(jsonPath("$.results[0].sskuStatus").value(SskuStatus.ACTIVE.toString()))
            .andExpect(jsonPath("$.results[0].params")
                .value("{\"isCoreFix\": true, \"salePrice\": 38.0, \"competitorPrice\": 42.0}"));
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest.getTenderResultsTest_withFilter.before.csv")
    public void getTenderResultsTest_withFilter() throws Exception {
        List<TenderResultUserFilter> filters = List.of(
            new TenderResultUserFilter(
                TenderResultField.MSKU,
                UserFilterFieldPredicate.EQUAL,
                "100")
        );
        mockMvc.perform(post("/api/v2/tender/1/result")
                .contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(filters)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.results.length()").value(1))
            .andExpect(jsonPath("$.userFiltersCount.length()").value(1));
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest.getTenderResults.before.csv")
    public void getTenderResultsWrongDemandIdTest() throws Exception {
        mockMvc.perform(post("/api/v2/tender/123/result")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message")
                .value("Потребности с типом TENDER и c id 123 нет в базе, " +
                    "вероятно произошел реимпорт рекомендаций и у них поменялись id, " +
                    "попробуйте снова найти их на экране с календарем и открыть заново " +
                    "(не переживайте, ваши корректировки не были перезаписаны)"));
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest_testExportSupplierResponses.before.csv")
    public void testExportSupplierResponsesToExcel() throws Exception {
        byte[] excelData = mockMvc.perform(get("/api/v1/tender/3/1/excel"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();

        ExcelFileAssertions.assertThat(excelData)
            .containsValue(1, "Бренд", "JobsAndCompany")
            .containsValue(1, "Категория 1", "валенки валенки да не пошиты стареньки")
            .containsValue(1, "Категория 2", "валенки")
            .containsValue(1, "Категория 3", "Пинетки из крокодила")
            .containsValue(1, "Название товара", "msku_2")
            .containsValue(1, "MSKU", 200)
            .containsValue(1, "SSKU", "0042.42-42")
            .containsValue(1, "Артикул", "XX000X777")
            .containsValue(1, "Штрихкоды", "123, 321")
            .containsValue(1, "Потребность", 11)
            .containsValue(1, "кол-во", 3)
            .containsValue(1, "цена", 78)
            .containsValue(1, "валюта", "RUB")
            .containsValue(1, "комментарий\n(здесь вы можете указать ваш комментарий по товару. Наличие других " +
                "цветов, доступность и др.)", "Comment for 200 msku")
            .hasSize(1);
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest_testExportSupplierResponses.before.csv")
    public void testExportSupplierResponsesToExcel_wrongDemandId() throws Exception {
        mockMvc.perform(get("/api/v1/tender/123123123/1/excel"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message")
                .value("Потребности с типом TENDER и c id 123123123 нет в базе, вероятно произошел реимпорт " +
                    "рекомендаций и у них поменялись id, попробуйте снова найти их на экране с " +
                    "календарем и открыть заново (не переживайте, ваши корректировки не были " +
                    "перезаписаны)"));
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest_testExportSupplierResponses.before.csv")
    public void testExportSupplierResponsesToExcel_wrongSupplierId() throws Exception {
        mockMvc.perform(get("/api/v1/tender/3/123123123/excel"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message")
                .value("Поставщик с указанным id не существует 123123123"));
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest_testExportPriceSpecs.before.csv")
    public void testExportPriceSpecsBySupplier() throws Exception {
        byte[] excelData = mockMvc.perform(get("/api/v1/tender/123/4202/prices/excel"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();

        var sheet = StreamExcelParser.parse(new ByteArrayInputStream(excelData)).get(0);
        var rows = sheet.getAllRows();
        var updatedRows = List.of(rows.get(25), rows.get(26), rows.get(27));

        Assertions
            .assertThat(updatedRows)
            .containsExactlyInAnyOrder(
                Map.of(0, "1", 1, "234234", 2, "msku_3", 3, "JobsAndCompany", 7, "17"),
                Map.of(0, "2", 1, "234237", 2, "msku_3", 3, "JobsAndCompany", 7, "170"),
                Map.of(0, "3", 1, "234238", 2, "msku_3", 3, "JobsAndCompany", 7, "170")
            );
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest_testExportPriceSpecs.before.csv")
    public void testExportPriceSpecsBySupplierWrongDemandId() throws Exception {
        mockMvc.perform(get("/api/v1/tender/123123123/4201/prices/excel"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message")
                .value("Потребности с типом TENDER и c id 123123123 нет в базе, вероятно произошел реимпорт " +
                    "рекомендаций и у них поменялись id, попробуйте снова найти их на экране с " +
                    "календарем и открыть заново (не переживайте, ваши корректировки не были " +
                    "перезаписаны)"));
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest_testExportPriceSpecs.before.csv")
    public void testExportPriceSpecsBySupplierWrongSupplierId() throws Exception {
        mockMvc.perform(get("/api/v1/tender/123/123123123/prices/excel"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message")
                .value("Поставщик с указанным id не существует 123123123"));
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest_testExportPriceSpecs.before.csv")
    public void testExportPriceSpecsByPartnerId() throws Exception {
        byte[] excelData = mockMvc.perform(get("/api/v1/tender/partner/123/222/prices/excel"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();

        var sheet = StreamExcelParser.parse(new ByteArrayInputStream(excelData)).get(0);
        var rows = sheet.getAllRows();
        var updatedRows = List.of(rows.get(25), rows.get(26), rows.get(27));

        Assertions
            .assertThat(updatedRows)
            .containsExactlyInAnyOrder(
                Map.of(0, "1", 1, "234234", 2, "msku_3", 3, "JobsAndCompany", 7, "17"),
                Map.of(0, "2", 1, "234237", 2, "msku_3", 3, "JobsAndCompany", 7, "170"),
                Map.of(0, "3", 1, "234238", 2, "msku_3", 3, "JobsAndCompany", 7, "170")
            );
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest_testExportPriceSpecs.before.csv")
    public void exportPriceSpecsByPartnerIdWithAbsentDemandIdThrowError() throws Exception {
        mockMvc.perform(get("/api/v1/tender/partner/777/4202/prices/excel"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message")
                .value("Потребности с типом TENDER и c id 777 нет в базе, вероятно произошел реимпорт рекомендаций и " +
                    "у них поменялись id, попробуйте снова найти их на экране с календарем и открыть заново (не " +
                    "переживайте, ваши корректировки не были перезаписаны)"));
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest_testExportPriceSpecs.before.csv")
    public void exportPriceSpecsByPartnerIdWithAbsentXSupplierIdIsOk() throws Exception {
        mockMvc.perform(get("/api/v1/tender/partner/123/222/prices/excel"))
            .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest_testExportPriceSpecs.before.csv")
    public void exportPriceSpecsByPartnerIdWithDismatchedSupplierIdThrowError1() throws Exception {
        mockMvc.perform(get("/api/v1/tender/partner/123/222/prices/excel")
                .header("x-supplier-id", 333))
            .andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message")
                .value("Header x-supplier-id: 333 not equals supplierId: 222"));
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest_testExportSupplierResponses2.before.csv")
    public void testGetAllSupplierResponsesByDemandId() throws Exception {
        mockMvc.perform(get("/api/v1/tender/1/responses")
                .contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$.items.length()").value(2))

            .andExpect(jsonPath("$.deadline").value("2021-06-24T00:00:00"))

            .andExpect(jsonPath("$.items[?(@.supplierId==1)].supplierName").value("'Поставщик 1'"))
            .andExpect(jsonPath("$.items[?(@.supplierId==1)].demand1pId").value(300))
            .andExpect(jsonPath("$.items[?(@.supplierId==1)].rsId").value("rs_id_1"))
            .andExpect(jsonPath("$.items[?(@.supplierId==1)][?(@.priceSpec==null)]").exists())
            .andExpect(jsonPath("$.items[?(@.supplierId==1)][?(@.priceSpecJson==null)]").exists())
            .andExpect(jsonPath("$.items[?(@.supplierId==1)].anonymous").value(false))

            .andExpect(jsonPath("$.items[?(@.supplierId==1000000)].supplierName").value("Анонимный 1"))
            .andExpect(jsonPath("$.items[?(@.supplierId==1000000)][?(@.demand1pId==null)]").exists())
            .andExpect(jsonPath("$.items[?(@.supplierId==1000000)].rsId").value(""))
            .andExpect(jsonPath("$.items[?(@.supplierId==1000000)][?(@.priceSpecJson==null)]").exists())
            .andExpect(jsonPath("$.items[?(@.supplierId==1000000)].priceSpec.id").value("foo"))
            .andExpect(jsonPath("$.items[?(@.supplierId==1000000)].priceSpec.specs[0].id").value("spec1"))
            .andExpect(jsonPath("$.items[?(@.supplierId==1000000)].priceSpec.specs[0].fio").value("login1"))
            .andExpect(jsonPath("$.items[?(@.supplierId==1000000)].priceSpec.specs[0].login").value("login1"))
            .andExpect(jsonPath("$.items[?(@.supplierId==1000000)].priceSpec.specs[0].mdsUrl").value("url1"))
            .andExpect(jsonPath("$.items[?(@.supplierId==1000000)].priceSpec.sskus[0]").value("010.3"))
            .andExpect(jsonPath("$.items[?(@.supplierId==1000000)].priceSpec.sskus[1]").value("010.2"))
            .andExpect(jsonPath("$.items[?(@.supplierId==1000000)].priceSpec.status")
                .value(PriceSpecificationStatus.APPROVED.toString()))
            .andExpect(jsonPath("$.items[?(@.supplierId==1000000)].priceSpec.orderId").value("42"))
            .andExpect(jsonPath("$.items[?(@.supplierId==1000000)].anonymous").value(true));
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest_testExportSupplierResponses.before.csv")
    public void testGetAllSupplierResponsesByDemandId_wrondDemandId() throws Exception {
        mockMvc.perform(get("/api/v1/tender/123123123/responses"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message")
                .value("Потребности с типом TENDER и c id 123123123 нет в базе, вероятно произошел реимпорт " +
                    "рекомендаций и у них поменялись id, попробуйте снова найти их на экране с " +
                    "календарем и открыть заново (не переживайте, ваши корректировки не были " +
                    "перезаписаны)"));
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest.setStatus.before.csv",
        after = "TenderControllerTest.setStatus.after.csv")
    public void setStatusTest() throws Exception {
        mockMvc.perform(post("/api/v1/tender/1/status")
                .contentType(APPLICATION_JSON_UTF8)
                .content(String.format("{\"status\":\"%s\"}", TenderStatus.NEW)))
            .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest.updateStatus.before.csv",
        after = "TenderControllerTest.updateStatus.after.csv")
    public void updateStatusTest() throws Exception {
        mockMvc.perform(post("/api/v1/tender/1/status")
                .contentType(APPLICATION_JSON_UTF8)
                .content(String.format("{\"status\":\"%s\"}", TenderStatus.OFFERS_COLLECTED)))
            .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest.wrongUpdateStatus.before.csv",
        after = "TenderControllerTest.wrongUpdateStatus.before.csv")
    public void wrongUpdateStatusTest() throws Exception {
        mockMvc.perform(post("/api/v1/tender/1/status")
                .contentType(APPLICATION_JSON_UTF8)
                .content(String.format("{\"status\":\"%s\"}", TenderStatus.NEW)))
            .andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message").value("Тендер не может вернуться в состояние NEW"));
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest.setStatusStarted.before.csv",
        after = "TenderControllerTest.setStatusStarted.after.csv")
    public void setStatusStartedTest() throws Exception {
        TestUtils.mockTimeService(timeService, LocalDateTime.of(2021, 7, 21, 16, 41));
        mockMvc.perform(post("/api/v1/tender/1/status")
                .contentType(APPLICATION_JSON_UTF8)
                .content(String.format("{\"status\":\"%s\"}", TenderStatus.STARTED)))
            .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest.checkWrongLogParams.before.csv")
    public void checkWrongLogParams() throws Exception {
        mockMvc.perform(post("/api/v1/tender/1/status")
                .contentType(APPLICATION_JSON_UTF8)
                .content(String.format("{\"status\":\"%s\"}", TenderStatus.OFFERS_COLLECTED)))
            .andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message")
                .value("У вашего тендерного ассортимента разные группы лог-параметров [<empty>, logparam1]," +
                    " это может привести к неожиданному разбиению на заказы, укажите одну группу на" +
                    " ваш ассортимент (или оставьте пустой)"));
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest.inviteSuppliers.before.csv",
        after = "TenderControllerTest.inviteSuppliers.after.csv")
    public void inviteSuppliersTest() throws Exception {
        mockMvc.perform(post("/api/v1/tender/1/invite-suppliers")
                .contentType(APPLICATION_JSON_UTF8)
                .content("{\"supplierIds\":[566631,566632]}"))
            .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest.inviteSuppliers.after.csv")
    public void getParticipantsTest() throws Exception {
        mockMvc.perform(get("/api/v1/tender/1/participants"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[?(@==566631)]").isNotEmpty())
            .andExpect(jsonPath("$[?(@==566632)]").isNotEmpty());
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest.exportTenderResults.before.csv")
    public void testExportTenderResultsByDemandId() throws Exception {
        byte[] excelTenderResults = mockMvc.perform(get("/api/v1/tender/7/excel"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();

        ExcelFileAssertions.assertThat(excelTenderResults)
            .hasSize(5)
            .containsValue(1, "MSKU", 424242)
            .containsValue(1, "SSKU", "fff656565")
            .containsValue(1, "Рекомендация", 200)
            .containsValue(1, "Поставщик", "Haskell")
            .containsValue(1, "Цена", 280)
            .containsValue(1, "Количество", 120)
            .containsValue(1, "Комментарий", "Last price: 300")
            .containsValue(1, "Бренд", "BONAS")
            .containsValue(1, "Vendor Code", "code-Butterfly")
            .containsValue(1, "Категория 1", "Трусы")
            .containsValue(1, "Категория 2", "Нижнее белье")
            .containsValue(1, "Категория 3", "Одежда")
            .containsValue(1, "Core Fix", "")
            .containsValue(1, "Цена продажи на сайте", "")
            .containsValue(1, "Минимальная цена конкурентов", "")
            .containsValue(1, "Был на стоках последние 4 недели", 0)
            .containsValue(1, "Сток 1P", 0)
            .containsValue(1, "Продажи за последнюю неделю", 0)
            .containsValue(1, "Продажи за 2ю неделю", 0)
            .containsValue(1, "Продажи за 3ю неделю", 0)
            .containsValue(1, "Продажи за 4ю неделю", 0)
            .containsValue(1, "Продажи за 2й месяц", 0)
            .containsValue(1, "Продажи за 3й месяц", 0)
            .containsValue(1, "Название товара", "Butterfly")
            .containsValue(1, "Квант", 1)
            .containsValue(1, "Сумма закупки", 33600)
            .containsValue(1, "Время доставки, дни", 1)
            .containsValue(1, "Статус", "")
            .containsValue(1, "Склад", "Томилино")

            .containsValue(2, "MSKU", 424242)
            .containsValue(2, "SSKU", "gg1414")
            .containsValue(2, "Рекомендация", 200)
            .containsValue(2, "Поставщик", "Marvel")
            .containsValue(2, "Цена", 310)
            .containsValue(2, "Количество", 80)
            .containsValue(2, "Комментарий", "Last price: 300")
            .containsValue(2, "Бренд", "BONAS")
            .containsValue(2, "Vendor Code", "code-Butterfly")
            .containsValue(2, "Категория 1", "Трусы")
            .containsValue(2, "Категория 2", "Нижнее белье")
            .containsValue(2, "Категория 3", "Одежда")
            .containsValue(2, "Core Fix", "")
            .containsValue(2, "Цена продажи на сайте", "")
            .containsValue(2, "Минимальная цена конкурентов", "")
            .containsValue(2, "Был на стоках последние 4 недели", 0)
            .containsValue(2, "Сток 1P", 0)
            .containsValue(2, "Продажи за последнюю неделю", 0)
            .containsValue(2, "Продажи за 2ю неделю", 0)
            .containsValue(2, "Продажи за 3ю неделю", 0)
            .containsValue(2, "Продажи за 4ю неделю", 0)
            .containsValue(2, "Продажи за 2й месяц", 0)
            .containsValue(2, "Продажи за 3й месяц", 0)
            .containsValue(2, "Название товара", "Butterfly")
            .containsValue(2, "Квант", 3)
            .containsValue(2, "Сумма закупки", 24800)
            .containsValue(2, "Время доставки, дни", 5)
            .containsValue(2, "Статус", "")
            .containsValue(2, "Склад", "Томилино")

            .containsValue(3, "MSKU", 883366)
            .containsValue(3, "SSKU", "gg8888")
            .containsValue(3, "Рекомендация", 30)
            .containsValue(3, "Поставщик", "Marvel")
            .containsValue(3, "Цена", 48)
            .containsValue(3, "Количество", 200)
            .containsValue(3, "Комментарий", "Last price: 50")
            .containsValue(3, "Бренд", "BONAS")
            .containsValue(3, "Vendor Code", "code-Ant")
            .containsValue(3, "Категория 1", "Носки")
            .containsValue(3, "Категория 2", "Нижнее белье")
            .containsValue(3, "Категория 3", "Одежда")
            .containsValue(3, "Core Fix", "Да")
            .containsValue(3, "Цена продажи на сайте", "48")
            .containsValue(3, "Минимальная цена конкурентов", "42")
            .containsValue(3, "Был на стоках последние 4 недели", "22")
            .containsValue(3, "Сток 1P", 0)
            .containsValue(3, "Продажи за последнюю неделю", 1)
            .containsValue(3, "Продажи за 2ю неделю", 1)
            .containsValue(3, "Продажи за 3ю неделю", 1)
            .containsValue(3, "Продажи за 4ю неделю", 1)
            .containsValue(3, "Продажи за 2й месяц", 0)
            .containsValue(3, "Продажи за 3й месяц", 38)
            .containsValue(3, "Название товара", "Ant")
            .containsValue(3, "Квант", 5)
            .containsValue(3, "Сумма закупки", 9600)
            .containsValue(3, "Время доставки, дни", 5)
            .containsValue(3, "Статус", "INACTIVE")
            .containsValue(3, "Склад", "Томилино")

            .containsValue(4, "MSKU", 883366)
            .containsValue(4, "SSKU", "zzz66666")
            .containsValue(4, "Рекомендация", 30)
            .containsValue(4, "Поставщик", "Haskell")
            .containsValue(4, "Цена", 52)
            .containsValue(4, "Количество", 100)
            .containsValue(4, "Комментарий", "Last price: 50")
            .containsValue(4, "Бренд", "BONAS")
            .containsValue(4, "Vendor Code", "code-Ant")
            .containsValue(4, "Категория 1", "Носки")
            .containsValue(4, "Категория 2", "Нижнее белье")
            .containsValue(4, "Категория 3", "Одежда")
            .containsValue(4, "Core Fix", "")
            .containsValue(4, "Цена продажи на сайте", "")
            .containsValue(4, "Минимальная цена конкурентов", "")
            .containsValue(4, "Был на стоках последние 4 недели", 22)
            .containsValue(4, "Сток 1P", 0)
            .containsValue(4, "Продажи за последнюю неделю", 1)
            .containsValue(4, "Продажи за 2ю неделю", 1)
            .containsValue(4, "Продажи за 3ю неделю", 1)
            .containsValue(4, "Продажи за 4ю неделю", 1)
            .containsValue(4, "Продажи за 2й месяц", 0)
            .containsValue(4, "Продажи за 3й месяц", 38)
            .containsValue(4, "Название товара", "Ant")
            .containsValue(4, "Квант", 1)
            .containsValue(4, "Сумма закупки", 5200)
            .containsValue(4, "Время доставки, дни", 1)
            .containsValue(4, "Статус", "")
            .containsValue(4, "Склад", "Томилино")

            .containsValue(5, "MSKU", 994455)
            .containsValue(5, "SSKU", "www85858")
            .containsValue(5, "Рекомендация", 400)
            .containsValue(5, "Поставщик", "Lego")
            .containsValue(5, "Цена", 82)
            .containsValue(5, "Количество", 300)
            .containsValue(5, "Комментарий", "Last price: 80")
            .containsValue(5, "Бренд", "BONAS")
            .containsValue(5, "Vendor Code", "code-Dragonfly")
            .containsValue(5, "Категория 1", "Пижамы")
            .containsValue(5, "Категория 2", "Нижнее белье")
            .containsValue(5, "Категория 3", "Одежда")
            .containsValue(5, "Core Fix", "")
            .containsValue(5, "Цена продажи на сайте", "")
            .containsValue(5, "Минимальная цена конкурентов", "")
            .containsValue(5, "Был на стоках последние 4 недели", 0)
            .containsValue(5, "Сток 1P", 0)
            .containsValue(5, "Продажи за последнюю неделю", 0)
            .containsValue(5, "Продажи за 2ю неделю", 0)
            .containsValue(5, "Продажи за 3ю неделю", 0)
            .containsValue(5, "Продажи за 4ю неделю", 0)
            .containsValue(5, "Продажи за 2й месяц", 0)
            .containsValue(5, "Продажи за 3й месяц", 0)
            .containsValue(5, "Название товара", "Dragonfly")
            .containsValue(5, "Квант", 7)
            .containsValue(5, "Сумма закупки", 24600)
            .containsValue(5, "Время доставки, дни", 3)
            .containsValue(5, "Статус", "ACTIVE")
            .containsValue(5, "Склад", "Софьино");
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest.exportTenderResults.before.csv")
    public void testExportTenderResultsByDemandId_wrongDemandId() throws Exception {
        mockMvc.perform(get("/api/v1/tender/1/excel"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message")
                .value("Потребности с типом TENDER и c id 1 нет в базе, вероятно произошел реимпорт " +
                    "рекомендаций и у них поменялись id, попробуйте снова найти их на экране с " +
                    "календарем и открыть заново (не переживайте, ваши корректировки не были " +
                    "перезаписаны)"));
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest.updateBySsku.before.csv",
        after = "TenderControllerTest.updateBySsku.after.csv")
    public void testUpdateBySsku() throws Exception {
        String testJson = TestUtils.dtoToString(new EditItemsCountInTenderResultDTO("foo", 180L));
        mockMvc.perform(post("/api/v1/tender/7/result/edit")
                .content(testJson)
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());

        testJson = TestUtils.dtoToString(new EditItemsCountInTenderResultDTO("bar", 0L));
        mockMvc.perform(post("/api/v1/tender/7/result/edit")
                .content(testJson)
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest.updateBySsku.before.csv")
    public void testUpdateBySsku_wrongSsku() throws Exception {
        String testJson = TestUtils.dtoToString(new EditItemsCountInTenderResultDTO("1", 0L));
        mockMvc.perform(post("/api/v1/tender/7/result/edit")
                .content(testJson)
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message")
                .value("SSKU: 1 не найдено"));
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest.exportResults.before.csv")
    public void testExportResults_DemandIdNotFound() throws Exception {
        mockMvc.perform(post("/api/v1/tender/123123123/export")
                .content("[4201]")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message")
                .value("Потребности с типом TENDER и c id 123123123 нет в базе, вероятно произошел реимпорт " +
                    "рекомендаций и у них поменялись id, попробуйте снова найти их на экране с календарем" +
                    " и открыть заново (не переживайте, ваши корректировки не были перезаписаны)"));
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest.exportResults.before.csv")
    public void testExportResults_wrongSupplierId() throws Exception {
        mockMvc.perform(post("/api/v1/tender/123/export")
                .content("[1]")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message")
                .value("Поставщик с указанным id не существует 1"));
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest.delete.before.csv",
        after = "TenderControllerTest.delete.after.csv")
    public void testDeleteIsOk() throws Exception {
        mockMvc.perform(delete("/api/v1/tender/1/10"))
            .andExpect(status().isOk());
    }

    @Test
    public void testFiltersInfo() throws Exception {
        mockMvc.perform(get("/api/v1/tender-results/user-filters")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].field").value("MSKU"))
            .andExpect(jsonPath("$[0].dataType").value("LONG"))
            .andExpect(jsonPath("$[0].label").value("MSKU"))
            .andExpect(jsonPath("$[0].predicates[0].predicate").value("EQUAL"))
            .andExpect(jsonPath("$[0].predicates[0].label").value("="));
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest.testManySskuForOneMsku.before.csv")
    public void testManySskuForOneMsku() {
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest.getPossibleSuppliers.before.csv")
    public void testPossibleSuppliers() throws Exception {
        mockMvc.perform(
                get("/api/v1/tender/24288201/suppliers")
                    .contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk())

            .andExpect(jsonPath("$.length()").value(2))

            .andExpect(jsonPath("$[0].supplierId").value(481612))
            .andExpect(jsonPath("$[0].name").value("ООО Мерлион"))
            .andExpect(jsonPath("$[0].rsId").value("000017"))
            .andExpect(jsonPath("$[0].mskus").value(4))

            .andExpect(jsonPath("$[1].supplierId").value(481618))
            .andExpect(jsonPath("$[1].name").value("ООО Вип Маркет"))
            .andExpect(jsonPath("$[1].rsId").value("000025"))
            .andExpect(jsonPath("$[1].mskus").value(1));
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest.sendLettersToCatman.before.csv")
    public void testSendLettersToCatmanWithWrongSupplierId() throws Exception {
        String testRequest = TestUtils.dtoToString(
            new SendLettersRequest(
                new HashSet<>(Arrays.asList(1L, 4L)),
                createFilters()
            )
        );
        mockMvc.perform(post("/api/v1/tender/1/send-letters")
                .content(testRequest)
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").
                value("Поле sendLettersRequest.supplierIds Поставщик с указанным id не существует 4"));
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest.sendLettersToCatman.before.csv")
    public void testSendLettersToCatman() throws Exception {

        long demandId = 1L;
        String urlApi = "/api/v1/tender/" + demandId + "/send-letters";

        String testRequest = TestUtils.dtoToString(
            new SendLettersRequest(
                new HashSet<>(Arrays.asList(1L, 2L, 3L)),
                createFilters()
            )
        );

        mockMvc.perform(post(urlApi)
                .content(testRequest)
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());

        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);

        Mockito.verify(javaMailSender, times(3)).send(messageCaptor.capture());

        List<MimeMessage> messages = messageCaptor.getAllValues();

        String email = Arrays.stream(messages.get(0).getRecipients(Message.RecipientType.TO))
            .findFirst()
            .get()
            .toString();

        assertEquals("slonwork@yandex-team.ru", email);

        var firstSupplierBarcodeSsku = getSupplierBarcodeSskuFromAttachment(messages.get(0), "AKVARIM_1_1.xlsx");
        var secondSupplierBarcodeSsku = getSupplierBarcodeSskuFromAttachment(messages.get(1), "MINIROUTE_2_1.xlsx");
        var thirdSupplierRequest = getSupplierRequestFromAttachment(messages.get(2), "LEAXOLVER_3_1.xlsx");

        assertEquals(2, firstSupplierBarcodeSsku.size());

        assertEquals(new Pair<>("123, 321", "111"), firstSupplierBarcodeSsku.get(0));
        assertEquals(new Pair<>("300, 500", "111"), firstSupplierBarcodeSsku.get(1));

        assertEquals(2, secondSupplierBarcodeSsku.size());

        assertEquals(new Pair<>(null, null), secondSupplierBarcodeSsku.get(0));
        assertEquals(new Pair<>("300, 400", "300"), secondSupplierBarcodeSsku.get(1));

        assertEquals(2, thirdSupplierRequest.size());

        assertEquals(200, thirdSupplierRequest.get(0).getMsku());
        assertEquals("301", thirdSupplierRequest.get(0).getSsku());
        assertEquals("333, 321", thirdSupplierRequest.get(0).getBarcode());
        assertEquals(300, thirdSupplierRequest.get(1).getMsku());
        assertNull(thirdSupplierRequest.get(1).getSsku());
        assertNull(thirdSupplierRequest.get(1).getBarcode());
    }

    private List<Pair<String, String>> getSupplierBarcodeSskuFromAttachment(
        MimeMessage message, String expectedFilename) throws MessagingException, IOException {

        MimeMultipart multi = (MimeMultipart) message.getContent();

        assertTrue(multi.getCount() > 1);

        BodyPart attachment = multi.getBodyPart(1);

        byte[] excelFromAttachment = attachment.getDataHandler().getDataSource()
            .getInputStream().readAllBytes();

        Assert.assertEquals(expectedFilename, attachment.getDataHandler().getName());

        return BaseExcelReader.extractFromExcel(
            new ByteArrayInputStream(excelFromAttachment),
            (index, row) -> {
                String barcode = BaseExcelReader.extractString(row, 8);
                String ssku = BaseExcelReader.extractString(row, 6);
                return new Pair<>(barcode, ssku);
            },
            2,
            3,
            "Бренд",
            "Категория 1",
            "Категория 2",
            "Категория 3",
            "Название товара",
            "MSKU",
            "SSKU",
            "Артикул",
            "Штрихкоды",
            "Потребность",
            "кол-во",
            "цена",
            "НДС, %",
            "валюта",
            "комментарий\n" +
                "(здесь вы можете указать ваш комментарий по товару." +
                " Наличие других цветов, доступность и др.)"
        );
    }

    private List<SupplierRecommendationExcelDTO> getSupplierRequestFromAttachment(
        MimeMessage message, String expectedFilename) throws MessagingException, IOException {

        MimeMultipart multi = (MimeMultipart) message.getContent();

        assertTrue(multi.getCount() > 1);

        BodyPart attachment = multi.getBodyPart(1);

        byte[] excelFromAttachment = attachment.getDataHandler().getDataSource()
            .getInputStream().readAllBytes();

        Assert.assertEquals(expectedFilename, attachment.getDataHandler().getName());

        return BaseExcelReader.extractFromExcel(
            new ByteArrayInputStream(excelFromAttachment),
            (index, row) -> {
                var dto = new SupplierRecommendationExcelDTO();
                dto.setVendorName(BaseExcelReader.extractString(row, 0));
                dto.setMsku(BaseExcelReader.extractNumeric(row, 5).longValue());
                dto.setSsku(BaseExcelReader.extractString(row, 6));
                dto.setBarcode(BaseExcelReader.extractString(row, 8));
                return dto;
            },
            2,
            3,
            "Бренд",
            "Категория 1",
            "Категория 2",
            "Категория 3",
            "Название товара",
            "MSKU",
            "SSKU",
            "Артикул",
            "Штрихкоды",
            "Потребность",
            "кол-во",
            "цена",
            "НДС, %",
            "валюта",
            "комментарий\n" +
                "(здесь вы можете указать ваш комментарий по товару." +
                " Наличие других цветов, доступность и др.)"
        );
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest_importTenderResults.before.csv",
        after = "TenderControllerTest_importTenderResults.after.csv")
    public void testImportTenderResults() throws Exception {
        excelTestingHelper.upload(
            "POST",
            "/api/v1/tender/1/excel",
            "TenderControllerTest_importTenderResults.xlsx"
        ).andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest_importTenderResults.before.csv")
    public void testImportTenderResults_wrongSsku() throws Exception {
        excelTestingHelper.upload(
            "POST",
            "/api/v1/tender/1/excel",
            "TenderControllerTest_importTenderResults_wrongSsku.xlsx"
        ).andExpect(status().isIAmATeapot()).andExpect(jsonPath("$.message").value(
            "Список SSKU в документе не совпадает со списком тендера"
        ));

        excelTestingHelper.upload(
            "POST",
            "/api/v1/tender/1/excel",
            "TenderControllerTest_importTenderResults_extraSsku.xlsx"
        ).andExpect(status().isIAmATeapot()).andExpect(jsonPath("$.message").value(
            "Список SSKU в документе не совпадает со списком тендера"
        ));
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest.getTenderResults_withZeroItems.before.csv")
    public void testImportTenderResults_resultsDisplayedWithoutSsku() throws Exception {
        byte[] excelData = mockMvc.perform(get("/api/v1/tender/30565527/excel")
                .contentType(APPLICATION_JSON_UTF8)
            )
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();

        ExcelFileAssertions.assertThat(excelData)
            .containsValue(1, "MSKU", "1578356")
            // здесь ssku == <empty>, поэтому в Excel будет
            // подставлено значение из tender_supplier_response (100500)
            .containsValue(1, "SSKU", "")
            .containsValue(1, "Рекомендация", 13)
            .containsValue(1, "Поставщик", "ООО «Мерлион»")
            .containsValue(1, "Цена", 13)
            .containsValue(1, "Количество", "100500")
            .containsValue(1, "Комментарий", "Нет SSKU, но цена хорошая")
            .containsValue(1, "Бренд", "Patriot Memory")
            .containsValue(1, "Vendor Code", "991806")
            .containsValue(1, "Категория 1", "Модули памяти")
            .containsValue(1, "Категория 2", "Комплектующие")
            .containsValue(1, "Категория 3", "Компьютерная техника")
            .containsValue(1, "Core Fix", "Да")
            .containsValue(1, "Цена продажи на сайте", 1329)
            .hasSize(1);
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest.getSskuTemplateExcel.before.csv")
    public void getSskuTemplateExcelIsOk() throws Exception {
        byte[] excelBytes = mockMvc.perform(get("/api/v1/tender/1/1/ssku-template")
                .header("x-supplier-id", 1))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();

        List<List<String>> list = BaseExcelReader.extractFromExcel(
            new ByteArrayInputStream(excelBytes),
            BaseExcelReader.getStringsMapperForColumnWriter(SupplierSskuTemplateWriter.getCOLUMNS_METADATA()),
            SupplierSskuTemplateWriter.START_ROW_IDX,
            SupplierSskuTemplateWriter.START_ROW_IDX,
            SupplierSskuTemplateWriter.ASSORTMENT_SHEET_IDX);

        list.sort(Comparator.comparing(row -> row == null ? null : row.get(0)));

        assertEquals(3, list.size());

        var item = list.get(0);
        assertEquals("msku_2_title", item.get(0));
        assertEquals("валенки валенки да не пошиты стареньки", item.get(1));
        assertEquals("XX000X777", item.get(2));
        assertEquals("200", item.get(3));

        item = list.get(1);
        assertEquals("msku_3_title", item.get(0));
        assertEquals("пинетки", item.get(1));
        assertEquals("XX000X777", item.get(2));
        assertEquals("300", item.get(3));

        item = list.get(2);
        assertEquals("msku_4_title", item.get(0));
        assertEquals("пинетки", item.get(1));
        assertEquals("XX000X777", item.get(2));
        assertEquals("400", item.get(3));
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest.getSskuTemplateExcel.before.csv")
    public void getSskuTemplateExcelWithAbsentDemandIdThrowError() throws Exception {
        mockMvc.perform(get("/api/v1/tender/777/1/ssku-template"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message")
                .value("Потребности с типом TENDER и c id 777 нет в базе, вероятно произошел реимпорт рекомендаций и " +
                    "у них поменялись id, попробуйте снова найти их на экране с календарем и открыть заново (не " +
                    "переживайте, ваши корректировки не были перезаписаны)"));
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest.getSskuTemplateExcel.before.csv")
    public void getSskuTemplateExcelWithAbsentXSupplierIdIsOk() throws Exception {
        mockMvc.perform(get("/api/v1/tender/1/1/ssku-template"))
            .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest.getSskuTemplateExcel.before.csv")
    public void getSskuTemplateExcelWithDismatchedSupplierIdThrowError1() throws Exception {
        mockMvc.perform(get("/api/v1/tender/1/1/ssku-template")
                .header("x-supplier-id", 2))
            .andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message")
                .value("Header x-supplier-id: 2 not equals supplierId: 1"));
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest.getSskuTemplateExcel.before.csv")
    public void importSupplierSskuExcel_isOk() throws Exception {
        excelTestingHelper.uploadWithHeaders(
                "POST",
                "/api/v1/tender/1/1/ssku-template",
                "TenderControllerTest_supplier-ssku-template.xlsm",
                Map.of("x-supplier-id", 1)
            ).andExpect(status().isOk())
            .andExpect(jsonPath("$.requestId")
                .value("SskusForDemand_1_Supplier_1_Date_2020-09-06_00:00:00_000.xlsm###1"));
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest.getSskuTemplateExcel.before.csv")
    public void importSupplierSskuExcelWithAbsentDemandId_throwError() throws Exception {
        excelTestingHelper.uploadWithHeaders(
                "POST",
                "/api/v1/tender/777/1/ssku-template",
                "TenderControllerTest_supplier-ssku-template.xlsm",
                Map.of("x-supplier-id", 1)
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message")
                .value("Потребности с типом TENDER и c id 777 нет в базе, вероятно произошел реимпорт рекомендаций и " +
                    "у них поменялись id, попробуйте снова найти их на экране с календарем и открыть заново (не " +
                    "переживайте, ваши корректировки не были перезаписаны)"));
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest.getSskuTemplateExcel.before.csv")
    public void importSupplierSskuExcelWithAbsentXSupplierId_isOk() throws Exception {
        excelTestingHelper.upload(
                "POST",
                "/api/v1/tender/1/1/ssku-template",
                "TenderControllerTest_supplier-ssku-template.xlsm"
            )
            .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest.getSskuTemplateExcel.before.csv")
    public void importSupplierSskuExcelWithDismatchedSupplierId_throwError1() throws Exception {
        excelTestingHelper.uploadWithHeaders(
                "POST",
                "/api/v1/tender/1/1/ssku-template",
                "TenderControllerTest_supplier-ssku-template.xlsm",
                Map.of("x-supplier-id", 2)
            )
            .andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message")
                .value("Header x-supplier-id: 2 not equals supplierId: 1"));
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest.getSupplierTemplateExcel.before.csv")
    public void getSupplierTemplateExcel_IsOk() throws Exception {
        mockMvc.perform(get("/api/v1/tender/partner/1/1/supplier-template")
                .header("x-supplier-id", 1))
            .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest.getSupplierTemplateExcel.before.csv")
    public void getSupplierTemplateExcel_WithAbsentDemandIdThrowError() throws Exception {
        mockMvc.perform(get("/api/v1/tender/777/1/supplier-template"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message")
                .value("Потребности с типом TENDER и c id 777 нет в базе, вероятно произошел реимпорт рекомендаций и " +
                    "у них поменялись id, попробуйте снова найти их на экране с календарем и открыть заново (не " +
                    "переживайте, ваши корректировки не были перезаписаны)"));
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest.getSupplierTemplateExcel.before.csv")
    public void getSupplierTemplateExcel_WithAbsentXSupplierIdIsOk() throws Exception {
        mockMvc.perform(get("/api/v1/tender/1/1/supplier-template"))
            .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest.getSupplierTemplateExcelWithAgreementIds.before.csv")
    public void getSupplierTemplateExcel_WithAgreementIds() throws Exception {
        byte[] excelData = mockMvc.perform(get("/api/v1/tender/1/1/supplier-template"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();

        Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(excelData));
        var validation = workbook.getSheetAt(0).getDataValidations().stream()
            .filter(v -> v.getRegions().getCellRangeAddress(0).isInRange(new CellAddress("B1")))
            .findFirst()
            .orElse(null);
        Assert.assertEquals("\"Договор-020-1,Договор-020-2,Договор-020-3\"",
            validation.getValidationConstraint().getFormula1());
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest.getSupplierTemplateExcelWithAgreementId.before.csv")
    public void getSupplierTemplateExcel_WithAgreementId() throws Exception {
        byte[] excelData = mockMvc.perform(get("/api/v1/tender/1/1/supplier-template"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();

        Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(excelData));
        var validation = workbook.getSheetAt(0).getDataValidations().stream()
            .filter(v -> v.getRegions().getCellRangeAddress(0).isInRange(new CellAddress("B1")))
            .findFirst()
            .orElse(null);
        Assert.assertEquals("\"Договор-020-1\"",
            validation.getValidationConstraint().getFormula1());
        Assert.assertEquals("Договор-020-1",
            workbook.getSheetAt(0).getRow(0).getCell(1).toString());
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest.getSupplierTemplateExcel.before.csv")
    public void getSupplierTemplateExcel_WithDismatchedSupplierIdThrowError1() throws Exception {
        mockMvc.perform(get("/api/v1/tender/partner/1/1/supplier-template")
                .header("x-supplier-id", 2))
            .andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message")
                .value("Header x-supplier-id: 2 not equals supplierId: 1"));
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest.getPossibleSuppliers.before.csv")
    public void getSupplierTenders_willReturnValidJson() throws Exception {
        when(timeService.getNowDate()).thenReturn(LocalDate.of(2021, 10, 12));
        mockMvc.perform(get("/api/v1/tender/partner/42/tenders")
                .header("x-supplier-id", 42)
                .contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isNotEmpty());
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest.getPossibleSuppliers.before.csv")
    public void getSupplierTenders_supplierHeaderMismatch_willReturnBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/tender/partner/10/tenders")
                .header("x-supplier-id", 481612)
                .contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(status().isIAmATeapot());
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest.apiV1TenderPriceSpecPost.before.csv",
        after = "TenderControllerTest.apiV1TenderPriceSpecPost_getPostedRequest.after.csv")
    public void apiV1TenderPriceSpecPosted_getPostedRequest() throws Exception {
        var priceSpec = new PriceSpecificationAxaptaState();
        priceSpec.setId("spec1");
        priceSpec.setLogin("login1");
        priceSpec.setFio("fio1");
        priceSpec.setMdsUrl("url1");
        priceSpec.setStatus(AxaptaPriceSpecStatus.POSTED);
        priceSpec.setSskus(List.of("010.3", "010.2"));
        var priceSpecRequest = new PriceSpecificationAxaptaRequest();
        priceSpecRequest.setDocumentId("xyz");
        priceSpecRequest.setSpecs(List.of(priceSpec));
        mockMvc.perform(post("/api/v1/tender/price-spec")
                .contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(priceSpecRequest)))
            .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest.apiV1TenderPriceSpecPost.before.csv",
        after = "TenderControllerTest.apiV1TenderPriceSpecPost_getSignedByUsRequest.after.csv")
    public void apiV1TenderPriceSpecPost_getSignedByUsRequest() throws Exception {
        var priceSpec = new PriceSpecificationAxaptaState();
        priceSpec.setId("spec1");
        priceSpec.setLogin("login1");
        priceSpec.setFio("fio1");
        priceSpec.setMdsUrl("url1");
        priceSpec.setStatus(AxaptaPriceSpecStatus.SIGNEDBYUS);
        priceSpec.setSskus(List.of("010.3", "010.2"));
        var priceSpecRequest = new PriceSpecificationAxaptaRequest();
        priceSpecRequest.setDocumentId("xyz");
        priceSpecRequest.setSpecs(List.of(priceSpec));
        mockMvc.perform(post("/api/v1/tender/price-spec")
                .contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(priceSpecRequest)))
            .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest.apiV1TenderPriceSpecPost.before.csv",
        after = "TenderControllerTest.apiV1TenderPriceSpecPost_getErrorRequest.after.csv")
    public void apiV1TenderPriceSpecPost_getErrorRequest() throws Exception {
        var priceSpec = new PriceSpecificationAxaptaState();
        priceSpec.setId("spec1");
        priceSpec.setLogin("login1");
        priceSpec.setFio("fio1");
        priceSpec.setMdsUrl("url1");
        priceSpec.setStatus(AxaptaPriceSpecStatus.POSTED);
        priceSpec.setErrors(List.of("Error1", "Error2"));
        var priceSpecRequest = new PriceSpecificationAxaptaRequest();
        priceSpecRequest.setDocumentId("xyz");
        priceSpecRequest.setSpecs(List.of(priceSpec));
        mockMvc.perform(post("/api/v1/tender/price-spec")
                .contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(priceSpecRequest)))
            .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest.apiV1TenderPriceSpecPost.before.csv")
    public void apiV1TenderPriceSpecPost_getInvalidRequest() throws Exception {
        var priceSpec1 = new PriceSpecificationAxaptaState();
        priceSpec1.setLogin("login1");
        priceSpec1.setFio("fio1");
        priceSpec1.setMdsUrl("url1");
        priceSpec1.setStatus(AxaptaPriceSpecStatus.POSTED);
        var priceSpec2 = new PriceSpecificationAxaptaState();
        priceSpec2.setLogin("login1");
        priceSpec2.setFio("fio1");
        priceSpec2.setMdsUrl("url1");
        priceSpec2.setStatus(AxaptaPriceSpecStatus.POSTED);
        var priceSpecRequest = new PriceSpecificationAxaptaRequest();
        priceSpecRequest.setDocumentId("xyz");
        priceSpecRequest.setSpecs(List.of(priceSpec1, priceSpec2));
        mockMvc.perform(post("/api/v1/tender/price-spec")
                .contentType(APPLICATION_JSON_UTF8)
                .content(dtoToString(priceSpecRequest)))
            .andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message")
                .value("В полях priceSpecificationAxaptaRequest.specs[1, 2] должен быть указан id или errors."));
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest.approvePriceSpec.before.csv",
        after = "TenderControllerTest.approvePriceSpec.after.csv")
    public void approvePriceSpec_isOk() throws Exception {
        String changePurchPriceStatus = axaptaServerUrl + "change_purch_price_status";
        doReturn(getMockedResponseEntity("123")).when(axRestTemplate).exchange(
            eq(changePurchPriceStatus),
            eq(HttpMethod.POST),
            any(),
            eq(AxHandlerResponse.class)
        );
        mockMvc.perform(post("/api/v1/tender/partner/24288201/42/approve-price-specs")
                .header("x-supplier-id", 42)
                .contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest.approvePriceSpec.before.csv")
    public void approvePriceSpec_axaptaHandleError_isIAmATeapot() throws Exception {
        String changePurchPriceStatus = axaptaServerUrl + "change_purch_price_status";
        doReturn(getMockedResponseEntity(null, "error")).when(axRestTemplate).exchange(
            eq(changePurchPriceStatus),
            eq(HttpMethod.POST),
            any(),
            eq(AxHandlerResponse.class)
        );
        mockMvc.perform(post("/api/v1/tender/partner/24288201/42/approve-price-specs")
                .header("x-supplier-id", 42)
                .contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message")
                .value("Price specification id xyz, axapta zhks id spec1 error: " +
                    "AxHandlerCreatePriceSpecError: error"));
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest.approvePriceSpec.before.csv")
    public void approvePriceSpec_badDemandId_isIAmATeapot() throws Exception {
        mockMvc.perform(post("/api/v1/tender/partner/1/42/approve-price-specs")
                .header("x-supplier-id", 42)
                .contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message")
                .value("Потребности с типом TENDER и c id 1 нет в базе, вероятно произошел реимпорт " +
                    "рекомендаций и у них поменялись id, попробуйте снова найти их на экране с календарем " +
                    "и открыть заново (не переживайте, ваши корректировки не были перезаписаны)"));
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest.approvePriceSpec.before.csv")
    public void approvePriceSpec_badXSupplierId_isIAmATeapot() throws Exception {
        mockMvc.perform(post("/api/v1/tender/partner/24288201/42/approve-price-specs")
                .header("x-supplier-id", 481612)
                .contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message")
                .value("Header x-supplier-id: 481612 not equals supplierId: 42"));
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest.declinePriceSpec.before.csv",
        after = "TenderControllerTest.declinePriceSpec.after.csv")
    public void declinePriceSpec_isOk() throws Exception {
        mockMvc.perform(post("/api/v1/tender/partner/24288201/42/decline-price-specs")
                .header("x-supplier-id", 42)
                .contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest.declinePriceSpec.before.csv")
    public void declinePriceSpec_badDemandId_isIAmATeapot() throws Exception {
        mockMvc.perform(post("/api/v1/tender/partner/1/42/decline-price-specs")
                .header("x-supplier-id", 42)
                .contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message")
                .value("Потребности с типом TENDER и c id 1 нет в базе, вероятно произошел реимпорт " +
                    "рекомендаций и у них поменялись id, попробуйте снова найти их на экране с календарем " +
                    "и открыть заново (не переживайте, ваши корректировки не были перезаписаны)"));
    }

    @Test
    @DbUnitDataSet(before = "TenderControllerTest.declinePriceSpec.before.csv")
    public void declinePriceSpec_badXSupplierId_isIAmATeapot() throws Exception {
        mockMvc.perform(post("/api/v1/tender/partner/24288201/42/decline-price-specs")
                .header("x-supplier-id", 481612)
                .contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message")
                .value("Header x-supplier-id: 481612 not equals supplierId: 42"));
    }

    @Test
    @DbUnitDataSet(
        before = "TenderControllerTest.groupTender.before.csv",
        after = "TenderControllerTest.groupTender.after.csv"
    )
    public void groupTender() throws Exception {
        mockMvc.perform(post("/api/v1/tender/start-grouped")
                .content(dtoToString(List.of(11L, 22L)))
                .contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").value("1"));
    }

    private <T> String dtoToString(T dto) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);
        ObjectWriter ow = mapper.writer().withDefaultPrettyPrinter();
        return ow.writeValueAsString(dto);
    }

    private void checkSupplierResultXls(byte[] excelData, String supplyRoute) {
        ExcelFileAssertions.assertThat(excelData)
            .containsHeaders(HEADERS_EXPECTED_EXPORT_SUPPLIER_RESPONSES)
            .containsValuesExactly(1, List.of(100, "100", "msku_1", 1, 4,
                "Яндекс.Маркет (Софьино КГТ)", "123", "4242", "россыпью", "опа", "41741000", supplyRoute))
            .containsValuesExactly(2, List.of(200, "200", "msku_2", 1, 10,
                "Яндекс.Маркет (Софьино КГТ)", "123", "4242", "россыпью", "опа", "41741000", supplyRoute))
            .containsValuesExactly(3, List.of(200, "200", "msku_2", 1, 40,
                "Томилино", "124", "", "на паллетах", "", "41741001", supplyRoute))
            .containsValuesExactly(4, List.of(200, "200", "msku_2", 1, 50,
                "Софьино", "", "", "", "", "41741002", supplyRoute))
            .containsValuesExactly(5, List.of(300, "300", "msku_3", 1, 5,
                "Яндекс.Маркет (Софьино КГТ)", "123", "4242", "россыпью", "опа", "41741000", supplyRoute))
            .hasSize(5);
    }

    private void checkSupplierResultNoDemandsXls(byte[] excelData) {
        ExcelFileAssertions.assertThat(excelData)
            .containsHeaders(HEADERS_EXPECTED_EXPORT_SUPPLIER_RESPONSES)
            .containsValuesExactly(1, List.of(100, "100", "msku_1", 1, 4,
                "Яндекс.Маркет (Софьино КГТ)", "", "4242", "россыпью", "опа", ""))
            .containsValuesExactly(2, List.of(200, "200", "msku_2", 1, 60,
                "Томилино", "", "", "", "", ""))
            .containsValuesExactly(3, List.of(200, "200", "msku_2", 1, 40,
                "Томилино", "", "", "на паллетах", "", ""))
            .containsValuesExactly(4, List.of(300, "300", "msku_3", 1, 5,
                "Софьино", "", "", "", "", ""))
            .hasSize(4);
    }
}
