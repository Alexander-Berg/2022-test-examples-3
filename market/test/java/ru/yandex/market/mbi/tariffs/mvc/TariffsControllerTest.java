package ru.yandex.market.mbi.tariffs.mvc;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.common.test.spring.RestTemplateFactory;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.mbi.tariffs.Constants;
import ru.yandex.market.mbi.tariffs.FunctionalTest;
import ru.yandex.market.mbi.tariffs.TestUtils;
import ru.yandex.market.mbi.tariffs.Utils;
import ru.yandex.market.mbi.tariffs.model.CommonJsonSchema;
import ru.yandex.market.mbi.tariffs.model.DistributionJsonSchema;
import ru.yandex.market.mbi.tariffs.model.ModelType;
import ru.yandex.market.mbi.tariffs.model.PagerResponseInfo;
import ru.yandex.market.mbi.tariffs.model.PartnersFilter;
import ru.yandex.market.mbi.tariffs.model.ServiceTypeEnum;
import ru.yandex.market.mbi.tariffs.model.TariffDTO;
import ru.yandex.market.mbi.tariffs.model.TariffFilterSummary;
import ru.yandex.market.mbi.tariffs.model.TariffFilterSummaryResult;
import ru.yandex.market.mbi.tariffs.model.TariffFindQuery;
import ru.yandex.market.mbi.tariffs.model.TariffHistoryDTO;
import ru.yandex.market.partner.error.info.model.ErrorInfo;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.common.test.spring.FunctionalTestHelper.get;
import static ru.yandex.market.common.test.spring.FunctionalTestHelper.post;
import static ru.yandex.market.mbi.tariffs.TestUtils.getErrors;
import static ru.yandex.market.mbi.tariffs.TestUtils.parseListResults;
import static ru.yandex.market.mbi.tariffs.TestUtils.parsePagerResponse;
import static ru.yandex.market.mbi.tariffs.matcher.TariffDTOMatcher.hasApprovalTicket;
import static ru.yandex.market.mbi.tariffs.matcher.TariffDTOMatcher.hasDateFrom;
import static ru.yandex.market.mbi.tariffs.matcher.TariffDTOMatcher.hasDateTo;
import static ru.yandex.market.mbi.tariffs.matcher.TariffDTOMatcher.hasDraftId;
import static ru.yandex.market.mbi.tariffs.matcher.TariffDTOMatcher.hasId;
import static ru.yandex.market.mbi.tariffs.matcher.TariffDTOMatcher.hasIsActive;
import static ru.yandex.market.mbi.tariffs.matcher.TariffDTOMatcher.hasMetaSize;
import static ru.yandex.market.mbi.tariffs.matcher.TariffDTOMatcher.hasModelType;
import static ru.yandex.market.mbi.tariffs.matcher.TariffDTOMatcher.hasPartner;
import static ru.yandex.market.mbi.tariffs.matcher.TariffDTOMatcher.hasServiceType;
import static ru.yandex.market.mbi.tariffs.matcher.TariffDTOMatcher.hasTags;
import static ru.yandex.market.mbi.tariffs.matcher.TariffDTOMatcher.hasUpdatedBy;
import static ru.yandex.market.mbi.tariffs.matcher.TariffDTOMatcher.hasUpdatedTime;

/**
 * Тесты для {@link ru.yandex.market.mbi.tariffs.mvc.controller.TariffsController}
 */
public class TariffsControllerTest extends FunctionalTest {

    @ParameterizedTest(name = "{5}")
    @MethodSource("getTariffsWithException")
    @DisplayName("Тест на получение клиентской ошибки")
    void testGetTariffsWithException(
            String sortBy, String sortType,
            Integer pageNum, Integer pageSize,
            String expectedExceptionMsg,
            String testName
    ) {
        String parameters = "sortBy=" + sortBy + "&sortType=" + sortType + "&page=" + pageNum + "&pageSize=" + pageSize;
        HttpClientErrorException actualException = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.get(baseUrl() + "/tariffs?" + parameters)
        );

        List<ErrorInfo> errors = TestUtils.getErrors(actualException.getResponseBodyAsString());
        assertThat(errors, hasSize(1));
        assertThat(errors.get(0).getMessage(), startsWith(expectedExceptionMsg));
    }

    private static Stream<Arguments> getTariffsWithException() {
        return Stream.of(
                Arguments.of("id", "123", 0, 10, "Unknown sortType. Available are : ", "Невалидный sortType"),
                Arguments.of("id123", "asc", 0, 10, "Unknown sortBy. Available are : ", "Невалидный sortBy"),
                Arguments.of("id", "asc", -1, 10, "getTariffs.page: must be greater than or equal to 0", "Page = -1"),
                Arguments.of("id", "asc", 0, 0, "getTariffs.pageSize: must be greater than or equal to 1", "PageSize " +
                        "= 0"),
                Arguments.of("id", "asc", 0, 501, "getTariffs.pageSize: must be less than or equal to 500", "PageSize" +
                        " = 501")
        );
    }

    @Test
    @DisplayName("Тест на получение тарифов")
    @DbUnitDataSet(
            before = "tariff/getTariffs.before.csv"
    )
    void testGetTariffs() {
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl() + "/tariffs?sortBy=id");
        assertOk(response);

        PagerResponseInfo pagerResponse = TestUtils.parsePagerResponse(response.getBody(), TariffDTO.class);
        assertEquals(2L, (long) pagerResponse.getTotalCount());
        TariffDTO tariff = (TariffDTO) pagerResponse.getItems().get(0);
        TariffDTO secondTariff = (TariffDTO) pagerResponse.getItems().get(1);
        assertThat(tariff, allOf(
                hasId(1L),
                hasDraftId(2L),
                hasModelType(ModelType.FULFILLMENT_BY_YANDEX),
                hasApprovalTicket("MBI-51594"),
                hasServiceType(ServiceTypeEnum.DISTRIBUTION),
                hasDateFrom(LocalDate.of(2020, 9, 1)),
                hasDateTo(LocalDate.of(2020, 10, 1)),
                hasTags(List.of("tag1", "tag2")),
                hasPartner(Constants.Partners.VALID_PARTNER_SHOP),
                hasUpdatedTime(OffsetDateTime.of(LocalDateTime.of(2020, 8, 10, 20, 0, 0), ZoneOffset.UTC)),
                hasUpdatedBy("andreybystrov"),
                hasIsActive(true),
                hasMetaSize(1)
        ));
        List<DistributionJsonSchema> meta = TestUtils.convert(tariff.getMeta(), DistributionJsonSchema.class);
        List<DistributionJsonSchema> secondTariffJson = TestUtils.convert(secondTariff.getMeta(),
                DistributionJsonSchema.class);
        assertThat(meta, hasSize(1));
        DistributionJsonSchema firstMeta = meta.get(0);
        DistributionJsonSchema secondMeta = secondTariffJson.get(0);
        assertEquals(new BigDecimal("1.80"), firstMeta.getAmount());
        assertEquals(new BigDecimal("18.00"), secondMeta.getAmount());
        assertEquals(CommonJsonSchema.TypeEnum.RELATIVE, firstMeta.getType());
        assertEquals(198119L, (long) firstMeta.getCategoryId());
        assertEquals("CEHAC", firstMeta.getTariffName());
        assertEquals("closer-cashbacks", firstMeta.getPartnerSegmentTariffKey());
    }

    @Test
    @DisplayName("Тест на получение тарифов")
    @DbUnitDataSet(
            before = "tariff/getTariffsWithoutTags.before.csv"
    )
    void testGetTariffsWithoutTags() {
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl() + "/tariffs");
        assertOk(response);

        PagerResponseInfo pagerResponse = TestUtils.parsePagerResponse(response.getBody(), TariffDTO.class);
        long id = ((TariffDTO) pagerResponse.getItems().get(0)).getId();
        assertEquals(1L, id);
    }

    @ParameterizedTest(name = "[{index}] {displayName}")
    @MethodSource("findTariffs")
    @DisplayName("Тест на поиск тарифов с разными фильтрами")
    @DbUnitDataSet(before = "tariff/find.before.csv")
    void testFindTariffs(
            long expectedTotalCount,
            List<Long> expectedIds,
            TariffFindQuery findQuery
    ) {
        HttpEntity<String> bodyEntity = createHttpRequestEntity(findQuery);
        ResponseEntity<String> response = post(baseUrl() + "/tariffs/find?page=0&pageSize=100&sortBy=id", bodyEntity);
        assertOk(response);

        PagerResponseInfo pagerResponse = parsePagerResponse(response.getBody(), TariffDTO.class);
        assertEquals(expectedTotalCount, (long) pagerResponse.getTotalCount());
        assertThat(
                pagerResponse.getItems().stream()
                        .map(obj -> (TariffDTO) obj)
                        .map(TariffDTO::getId)
                        .collect(Collectors.toList()),
                is(expectedIds)
        );
    }

    private static Stream<Arguments> findTariffs() {
        return Stream.of(
                // 1
                Arguments.of(8, LongStream.iterate(1, i -> i + 1).limit(8).boxed().collect(Collectors.toList()),
                        new TariffFindQuery()),
                // 2
                Arguments.of(1, List.of(2L), new TariffFindQuery().approvalTicket("MBI-12345")),
                // 3
                Arguments.of(0, List.of(), new TariffFindQuery().approvalTicket("MBI-12345").updatedBy("robot")),
                // 4
                Arguments.of(2, List.of(7L, 8L), new TariffFindQuery().serviceType(ServiceTypeEnum.FF_PROCESSING)),
                // 5
                Arguments.of(1, List.of(4L),
                        new TariffFindQuery().partner(Constants.Partners.VALID_PARTNER_SHOP)),
                // 6
                Arguments.of(7, List.of(1L, 2L, 4L, 5L, 6L, 7L, 8L),
                        new TariffFindQuery().dateToLower(LocalDate.of(2020, 10, 1))),
                // 7
                Arguments.of(1, List.of(4L), new TariffFindQuery().dateToLower(LocalDate.of(2020, 10, 2))),
                // 8
                Arguments.of(
                        2,
                        List.of(1L, 2L),
                        new TariffFindQuery().tags(List.of("tag1"))
                ),
                // 9
                Arguments.of(
                        1,
                        List.of(1L),
                        new TariffFindQuery().tags(List.of("tag1", "tag2"))
                ),
                // 10
                Arguments.of(
                        1,
                        List.of(4L),
                        new TariffFindQuery()
                                .serviceType(ServiceTypeEnum.DISTRIBUTION)
                                .dateFromLower(LocalDate.of(2020, 9, 1))
                                .dateFromUpper(LocalDate.of(2020, 10, 1))
                                .dateToLower(LocalDate.of(2020, 10, 1))
                                .dateToUpper(LocalDate.of(2020, 12, 2))
                                .partner(Constants.Partners.VALID_PARTNER_SHOP)
                                .updatedTimeFrom(Utils.toOffsetDateTime(LocalDateTime.of(2020, 10, 10, 20, 0, 0)))
                                .updatedTimeTo(Utils.toOffsetDateTime(LocalDateTime.of(2020, 10, 10, 20, 1, 0)))
                                .updatedBy("andreybystrov")
                                .isActive(true)
                                .approvalTicket("MBI-51594")
                ),
                // 11
                Arguments.of(
                        2,
                        List.of(3L, 4L),
                        new TariffFindQuery().targetDate(LocalDate.of(2020, 10, 1))
                ),
                // 12
                Arguments.of(
                        3,
                        List.of(1L, 2L, 3L),
                        new TariffFindQuery().modelType(ModelType.FULFILLMENT_BY_YANDEX)
                ),
                // 13
                Arguments.of(
                        4,
                        List.of(5L, 6L, 7L, 8L),
                        new TariffFindQuery().serviceTypes(List.of(ServiceTypeEnum.FEE, ServiceTypeEnum.FF_PROCESSING))
                ),
                // 14
                Arguments.of(
                        8,
                        LongStream.iterate(1, i -> i + 1).limit(8).boxed().collect(Collectors.toList()),
                        new TariffFindQuery().serviceTypes(null)
                ),
                // 15
                Arguments.of(
                        0,
                        List.of(),
                        new TariffFindQuery().serviceTypes(List.of())
                ),
                //16
                Arguments.of(
                        6,
                        List.of(1L, 2L, 3L, 5L, 6L, 7L),
                        new TariffFindQuery().filterByPartners(PartnersFilter.GENERAL)
                ),
                //17
                Arguments.of(
                        2,
                        List.of(4L, 8L),
                        new TariffFindQuery().filterByPartners(PartnersFilter.CUSTOM)
                ),
                //18
                Arguments.of(
                        6,
                        List.of(1L, 2L, 3L, 5L, 6L, 7L),
                        new TariffFindQuery()
                                .filterByPartners(PartnersFilter.GENERAL)
                                .partner(Constants.Partners.VALID_PARTNER_SHOP)
                ),
                //19
                Arguments.of(
                        2,
                        List.of(4L, 8L),
                        new TariffFindQuery()
                                .filterByPartners(PartnersFilter.CUSTOM)
                                .partner(Constants.Partners.VALID_PARTNER_SUPPLIER)
                ),
                //20
                Arguments.of(
                        1,
                        List.of(1L),
                        new TariffFindQuery().id(1L)
                ),
                //21
                Arguments.of(
                        6,
                        List.of(1L, 2L, 3L, 5L, 6L, 7L),
                        new TariffFindQuery()
                                .filterByPartners(PartnersFilter.GENERAL)
                                .partner(Constants.Partners.VALID_PARTNER_SHOP)
                ),
                //22
                Arguments.of(
                        2,
                        List.of(4L, 8L),
                        new TariffFindQuery()
                                .filterByPartners(PartnersFilter.CUSTOM)
                                .partner(Constants.Partners.VALID_PARTNER_SHOP)
                ),
                //23
                Arguments.of(
                        1,
                        List.of(4L),
                        new TariffFindQuery()
                                .partner(Constants.Partners.VALID_PARTNER_SHOP)
                )
        );
    }

    @Test
    @DisplayName("Тест на получение тарифов с пагинацией")
    @DbUnitDataSet(
            before = "tariff/getTariffsPaging.csv"
    )
    void testGetTariffsPaging() {
        ResponseEntity<String> response = get(baseUrl() + "/tariffs?sortBy=id&page=0&pageSize=3");
        assertOk(response);
        PagerResponseInfo pagerResponse = parsePagerResponse(response.getBody(), TariffDTO.class);
        assertThat(pagerResponse.getTotalCount(), is(7L));
        assertThat(pagerResponse.getItems()
                        .stream()
                        .map(o -> ((TariffDTO) o).getId())
                        .collect(Collectors.toList()),
                contains(1L, 2L, 3L)
        );
    }

    @Test
    @DisplayName("Тест на получение саммари по тарифам")
    @DbUnitDataSet(
            before = "tariff/getTariffsSummary.csv"
    )
    void testGetTariffsSummary() {
        List<TariffFilterSummary> filters = List.of(
                /*2*/ new TariffFilterSummary().serviceType(ServiceTypeEnum.FEE).modelType(ModelType.FULFILLMENT_BY_YANDEX).isActive(true),
                /*2*/ new TariffFilterSummary().serviceType(ServiceTypeEnum.DISTRIBUTION).modelType(ModelType.FULFILLMENT_BY_YANDEX).isActive(true),
                /*2*/ new TariffFilterSummary().serviceType(ServiceTypeEnum.FF_STORAGE_BILLING).modelType(ModelType.FULFILLMENT_BY_YANDEX).isActive(true),
                /*1*/ new TariffFilterSummary().serviceType(ServiceTypeEnum.FF_STORAGE_BILLING).modelType(ModelType.FULFILLMENT_BY_YANDEX).isActive(false),
                /*2*/ new TariffFilterSummary().serviceType(ServiceTypeEnum.FEE).modelType(ModelType.FULFILLMENT_BY_YANDEX_PLUS).isActive(true),
                /*3*/ new TariffFilterSummary().serviceType(ServiceTypeEnum.FF_PROCESSING).modelType(ModelType.FULFILLMENT_BY_YANDEX).isActive(true)
        );
        HttpEntity<String> bodyEntity = createHttpRequestEntity(filters);
        ResponseEntity<String> response = post(baseUrl() + "/tariffs/summary", bodyEntity);
        assertOk(response);

        List<TariffFilterSummaryResult> summaryResults = parseListResults(
                response.getBody(),
                TariffFilterSummaryResult.class
        );

        assertThat(summaryResults, hasSize(6));
        assertThat(
                summaryResults.stream()
                        .map(TariffFilterSummaryResult::getCountTariff)
                        .collect(Collectors.toList()),
                contains(2L, 2L, 2L, 1L, 2L, 3L)
        );
    }

    @Test
    @DisplayName("Тест на ручку по изменению тегов")
    @DbUnitDataSet(
            before = "tariff/updateTags.before.csv",
            after = "tariff/updateTags.after.csv"
    )
    void testUpdateTags() {
        HttpEntity<String> bodyEntity = createHttpRequestEntity("[\"anotherTag1\", \"anotherTag2\"]");
        ResponseEntity<String> response = post(baseUrl() + "/tariffs/1/tags", bodyEntity);
        assertOk(response);
    }

    @Test
    @DisplayName("Тест на ручку по получение ошибки по изменению тегов")
    @DbUnitDataSet()
    void testUpdateTagsFailed() {
        HttpEntity<String> bodyEntity = createHttpRequestEntity("[\"anotherTag1\", \"anotherTag2\"]");
        HttpClientErrorException exception = assertThrows(
                HttpClientErrorException.class,
                () -> post(baseUrl() + "/tariffs/101/tags", bodyEntity)
        );
        List<ErrorInfo> errors = getErrors(exception.getResponseBodyAsString());
        assertThat(errors, hasSize(1));
        assertThat(errors.get(0).getMessage(), is("The tariff with id 101 is not found"));
    }

    @Test
    @DbUnitDataSet(before = "tariff/getHistory.before.csv")
    @DisplayName("Тест на получение истории для тарифа")
    void testGetTariffHistory() {
        ResponseEntity<String> response = get(baseUrl() + "/tariffs/4/history");
        assertOk(response);
        List<TariffHistoryDTO> results = parseListResults(response.getBody(), TariffHistoryDTO.class);
        assertThat(results, hasSize(3));

        TariffHistoryDTO first = results.get(0);
        assertThat(first.getTariffBefore(), hasId(3L));
        assertThat(first.getTariffAfter(), hasId(4L));
        assertThat(first.getChanges().getChangedFields(), containsInAnyOrder("dateFrom", "dateTo", "isActive", "meta"));

        TariffHistoryDTO second = results.get(1);
        assertThat(second.getTariffBefore(), hasId(2L));
        assertThat(second.getTariffAfter(), hasId(3L));
        assertThat(second.getChanges().getChangedFields(), containsInAnyOrder("serviceType", "partner", "meta",
                "modelType"));

        TariffHistoryDTO third = results.get(2);
        assertThat(third.getTariffBefore(), hasId(1L));
        assertThat(third.getTariffAfter(), hasId(2L));
        assertThat(third.getChanges().getChangedFields(), containsInAnyOrder("serviceType", "partner", "meta"));
    }

    @Test
    @DisplayName("Тест на получение ошибки, что тариф не найден, при запросе истории тарифа")
    void testGetHistoryWithException() {
        HttpClientErrorException exception = assertThrows(
                HttpClientErrorException.class,
                () -> get(baseUrl() + "/tariffs/4/history")
        );
        List<ErrorInfo> errors = getErrors(exception.getResponseBodyAsString());
        assertThat(errors, hasSize(1));
        assertThat(errors.get(0).getMessage(), is("The tariff with id 4 is not found"));
    }

    @Test
    @DbUnitDataSet(before = "tariff/getEmptyHistory.before.csv")
    @DisplayName("Тест на получение пустой истории")
    void testGetEmptyHistory() {
        ResponseEntity<String> response = get(baseUrl() + "/tariffs/1/history");
        assertOk(response);

        assertNull(JsonTestUtil.parseJson(response.getBody()).getAsJsonObject().get("result"));
    }

    @DisplayName("Тест на сортировку тарифов")
    @ParameterizedTest
    @ValueSource(strings = {"serviceType", "dateFrom", "dateTo", "tags", "partnerId", "updatedTime", "updatedBy", "id"
            , "approvalTicket", "modelType", "status"})
    void testSortBy(String sortBy) {
        HttpEntity<String> bodyEntity = createHttpRequestEntity(new TariffFindQuery());
        ResponseEntity<String> response = post(baseUrl() + "/tariffs/find?page=0&pageSize=100&sortBy=" + sortBy,
                bodyEntity);
        assertOk(response);
    }

    @Test
    @DbUnitDataSet(before = "tariff/getDump.before.csv")
    @DisplayName("Тест на получение дампа в CSV")
    void testGetTariffCsvDump() {
        HttpEntity<String> bodyEntity = createHttpRequestEntity(new TariffFindQuery());
        ResponseEntity<String> response = post(
                baseUrl() + "/tariffs/download?format=CSV&sortBy=id&sortType=asc",
                bodyEntity);
        assertOk(response);
        Assertions.assertNotNull(response.getBody());
        String[] lines = response.getBody().split("\n");

        Assertions.assertEquals(3, lines.length);
        Assertions.assertEquals("id;draftId;isActive;serviceType;dateFrom;dateTo;tags;partner;updatedTime;updatedBy;" +
                "modelType;meta;approvalTicketId", lines[0]);
        Assertions.assertEquals("1;null;true;distribution;2020-09-01;2020-10-01;[];{\"id\":12345,\"type\":\"shop\"};" +
                "2020-08-10T23:00+03:00;andreybystrov;fulfillment_by_yandex;[{\"amount\":1.80,\"type\":\"relative\"," +
                "\"currency\":\"RUB\",\"billingUnit\":\"order\",\"categoryId\":198119," +
                "\"tariffName\":\"CEHAC\"}];" +
                "MBI-51594", lines[1]);
        Assertions.assertEquals("2;null;true;distribution;2020-10-01;2020-11-01;[];null;2020-09-01T23:00+03:00;" +
                "andreybystrov;fulfillment_by_yandex;[{\"amount\":18.00,\"type\":\"relative\",\"currency\":\"RUB\"," +
                "\"billingUnit\":\"order\",\"categoryId\":198119,\"tariffName\":\"CEHAC\"" +
                "}];MBI-51594", lines[2]);
    }

    @Test
    @DbUnitDataSet(before = "tariff/getDump.before.csv")
    @DisplayName("Тест на получение дампа в XLS")
    void testGetTariffXlsDump() {
        HttpEntity<String> bodyEntity = createHttpRequestEntity(new TariffFindQuery());
        RestTemplate restTemplate = RestTemplateFactory.createRestTemplate();
        ResponseEntity<Resource> response = restTemplate.exchange(
                baseUrl() + "/tariffs/download?format=EXCEL&sortBy=id&sortType=asc",
                HttpMethod.POST,
                bodyEntity,
                Resource.class);

        assertOk(response);
        Assertions.assertNotNull(response.getBody());

        List<List<String>> dumpContent = List.of(
                List.of("id", "draftId", "isActive", "serviceType", "dateFrom", "dateTo", "tags", "partner",
                        "updatedTime", "updatedBy", "modelType", "meta", "approvalTicketId"),
                List.of("1", "null", "true", "distribution", "2020-09-01", "2020-10-01", "[]", "{\"id\":12345," +
                        "\"type\":" +
                        "\"shop\"}", "2020-08-10T23:00+03:00", "andreybystrov", "fulfillment_by_yandex", "[{\"amount" +
                        "\":1.80,\"type\":\"relative\",\"currency\":\"RUB\",\"billingUnit\":\"order\"," +
                        "\"categoryId\":198119,\"tariffName\":\"CEHAC\"}]", "MBI-51594"),
                List.of("2", "null", "true", "distribution", "2020-10-01", "2020-11-01", "[]", "null", "2020-09-01T23" +
                        ":00+03:00", "andreybystrov", "fulfillment_by_yandex", "[{\"amount\":18.00," +
                        "\"type\":\"relative\",\"currency\":\"RUB\",\"billingUnit\":\"order\"," +
                        "\"categoryId\":198119,\"tariffName\":\"CEHAC\"}]", "MBI-51594")
        );

        try (Workbook book = new HSSFWorkbook(response.getBody().getInputStream())
        ) {
            Sheet sheet = book.getSheetAt(0);
            Assertions.assertEquals(3, sheet.getPhysicalNumberOfRows());

            IntStream.range(0, sheet.getPhysicalNumberOfRows())
                    .mapToObj(rowIndex -> new Pair<>(rowIndex, sheet.getRow(rowIndex)))
                    .forEach(p -> IntStream
                            .range(0, p.second.getPhysicalNumberOfCells())
                            .forEach(index -> Assertions.assertEquals(dumpContent.get(p.first).get(index),
                                    p.second.getCell(index).getStringCellValue())));


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    @DisplayName("Тест на 4хх ошибку при запросе дампа в неизвестном формате")
    void testGetUnknownFormatDump() {
        Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(baseUrl() + "/tariffs/download?format=PLAIN",
                        createHttpRequestEntity(new TariffFindQuery()))
        );
    }
}
