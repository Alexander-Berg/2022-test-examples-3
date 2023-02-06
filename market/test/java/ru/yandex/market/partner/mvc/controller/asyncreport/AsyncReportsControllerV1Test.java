package ru.yandex.market.partner.mvc.controller.asyncreport;

import java.time.OffsetDateTime;
import java.util.stream.Stream;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.asyncreport.model.ReportsType;
import ru.yandex.market.core.balance.BalanceContactService;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * Тесты для {@link AsyncReportsControllerV1}.
 */
@ParametersAreNonnullByDefault
@DbUnitDataSet(before = "AsyncReportsControllerTest.common.before.csv")
class AsyncReportsControllerV1Test extends FunctionalTest {

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private BalanceContactService balanceContactService;

    @BeforeEach
    void init() {
        //возвращаем то, что передали
        when(balanceContactService.getClientIdByUid(anyLong()))
                .thenAnswer((Answer<Long>) invocation -> (long) invocation.getArguments()[0]);
    }

    @Test
    @DisplayName("Проверяем, что заявка на генерацию отчёта будет сохранена в базе V1")
    @DbUnitDataSet(
            before = "AsyncReportsControllerTest.reports.before.csv",
            after = "AsyncReportsControllerTest.requestReportGeneration.after.csv"
    )
    void requestReportGenerationV1() {
        String url = getUrl(1, "/request-report-generation?campaign_id=21&reportType=FULFILLMENT_ORDERS");
        checkReportGeneration(url);
    }

    @Test
    @DisplayName("Проверяем заявку на генерацию отчёта по user id")
    @DbUnitDataSet(after = "AsyncReportsControllerTest.requestReportGenerationForClient.after.csv")
    void requestReportGenerationForClient() {
        String url = getUrl(1, "/request-report-generation?_user_id=4&reportType=AGENCY_CHECKER");
        HttpEntity request = JsonTestUtil.getJsonHttpEntity(
                getClass(),
                "request-report-generation-uid.request.json"
        );
        ResponseEntity<String> response = FunctionalTestHelper.post(url, request);
        JsonTestUtil.assertEquals(
                response,
                JsonTestUtil.fromJsonTemplate(getClass(), "request-report-generation-uid.expected-response.json")
                        .withVariable("zoneOffset", OffsetDateTime.now().getOffset().toString())
                        .toString()
        );
    }

    @Test
    @DisplayName("Проверяем заявку на перегенерацию отчёта по user id")
    @DbUnitDataSet(before = "AsyncReportsControllerTest.reports.before.csv",
            after = "AsyncReportsControllerTest.requestReportRegenerationForClient.after.csv")
    void requestReportRegenerationForClient() {
        String url = getUrl(1, "/request-report-regeneration?_user_id=4&reportId=66");
        ResponseEntity<String> response = FunctionalTestHelper.post(url);
        JsonTestUtil.assertEquals(
                response,
                JsonTestUtil.fromJsonTemplate(getClass(), "request-report-regeneration-uid.expected-response.json")
                        .withVariable("zoneOffset", OffsetDateTime.now().getOffset().toString())
                        .toString()
        );
    }

    @Test
    @DisplayName("Проверяем генерацию отчета DSBS_ORDERS")
    @DbUnitDataSet(after = "AsyncReportsControllerTest.reports.dsbs.after.csv")
    void requestDSBSOrders() {
        String url = getUrl(1, "/request-report-generation?campaign_id=21&reportType=DSBS_ORDERS");
        HttpEntity request = JsonTestUtil.getJsonHttpEntity(
                getClass(),
                "request-report-generation-uid.request.json"
        );
        ResponseEntity<String> response = FunctionalTestHelper.post(url, request);
        JsonTestUtil.assertEquals(
                response,
                JsonTestUtil.fromJsonTemplate(getClass(), "request-report-generation-uid.dsbs.response.json")
                        .withVariable("zoneOffset", OffsetDateTime.now().getOffset().toString())
                        .toString()
        );
    }

    @Test
    @DisplayName("Проверяем генерацию отчета ORDERS_RETURNS для DBS-партнёров")
    @DbUnitDataSet(after = "AsyncReportsControllerTest.reports.dsbs_orders_returns.after.csv")
    void requestDSBSOrderReturns() {
        String url = getUrl(1, "/request-report-generation?campaign_id=11&reportType=ORDERS_RETURNS");
        HttpEntity request = JsonTestUtil.getJsonHttpEntity(
                getClass(),
                "request-report-generation-uid.request.json"
        );
        ResponseEntity<String> response = FunctionalTestHelper.post(url, request);
        JsonTestUtil.assertEquals(
                response,
                JsonTestUtil.fromJsonTemplate(getClass(), "request-report-generation-uid.dsbs_orders_returns.response.json")
                        .withVariable("zoneOffset", OffsetDateTime.now().getOffset().toString())
                        .toString()
        );
    }

    @Test
    @DisplayName("Проверяем генерацию отчета SKU_MOVEMENTS")
    @DbUnitDataSet(after = "AsyncReportsControllerTest.reports.sku_movements.after.csv")
    void requestSkuMovements() {
        String url = getUrl(1, "/request-report-generation?campaign_id=21&reportType=" + ReportsType.SKU_MOVEMENTS);
        HttpEntity<?> request = JsonTestUtil.getJsonHttpEntity(
                getClass(),
                "request-report-generation-sku-movements.request.json"
        );
        ResponseEntity<String> response = FunctionalTestHelper.post(url, request);
        JsonTestUtil.assertEquals(
                response,
                JsonTestUtil.fromJsonTemplate(getClass(), "request-report-generation-sku-movements.response.json")
                        .withVariable("zoneOffset", OffsetDateTime.now().getOffset().toString())
                        .toString()
        );
    }

    @Test
    @DisplayName("Проверяем генерацию отчета SALES_DYNAMICS")
    @DbUnitDataSet(after = "AsyncReportsControllerTest.reports.sales_dynamics.after.csv")
    void requestSalesDynamics() {
        String url = getUrl(1, "/request-report-generation?campaign_id=21&reportType=" + ReportsType.SALES_DYNAMICS);
        HttpEntity<?> request = JsonTestUtil.getJsonHttpEntity(
                getClass(),
                "request-report-generation-sales-dynamics.request.json"
        );
        ResponseEntity<String> response = FunctionalTestHelper.post(url, request);
        JsonTestUtil.assertEquals(
                response,
                JsonTestUtil.fromJsonTemplate(getClass(), "request-report-generation-sales-dynamics.response.json")
                        .withVariable("zoneOffset", OffsetDateTime.now().getOffset().toString())
                        .toString()
        );
    }

    @Test
    @DisplayName("Проверяем генерацию отчета INVENTORY_TURNOVER")
    @DbUnitDataSet(after = "AsyncReportsControllerTest.reports.turnover.after.csv")
    void requestTurnoverReport() {
        String url = getUrl(1, "/request-report-generation?campaign_id=21&reportType=" + ReportsType.INVENTORY_TURNOVER);
        HttpEntity<?> request = JsonTestUtil.getJsonHttpEntity(
                getClass(),
                "request-report-generation-turnover.request.json"
        );
        ResponseEntity<String> response = FunctionalTestHelper.post(url, request);
        JsonTestUtil.assertEquals(
                response,
                JsonTestUtil.fromJsonTemplate(getClass(), "request-report-generation-turnover.response.json")
                        .withVariable("zoneOffset", OffsetDateTime.now().getOffset().toString())
                        .toString()
        );
    }

    static Stream<Arguments> stockRequestsJsons() {
        return Stream.of(
                Arguments.of(
                        "request-report-generation-stocks-by-supply.request.json",
                        "request-report-generation-stocks-by-supply.response.json"),
                Arguments.of(
                        "request-report-generation-stocks-by-supply-with-warehouses.request.json",
                        "request-report-generation-stocks-by-supply-with-warehouses.response.json"),
                Arguments.of(
                        "request-report-generation-stocks-by-supply-with-multiply-warehouses.request.json",
                        "request-report-generation-stocks-by-supply-with-multiply-warehouses.response.json"),
                Arguments.of(
                        "request-report-generation-stocks-by-supply-without-warehouses.request.json",
                        "request-report-generation-stocks-by-supply-without-warehouses.response.json"));
    }

    @ParameterizedTest
    @MethodSource("stockRequestsJsons")
    @DisplayName("Проверяем генерацию отчета STOCKS_BY_SUPPLY,  проверка ответа на запрос")
    void requestStocksBySupplyDBChangeTest(String requestJson, String responseJson) {
        String url = getUrl(1, "/request-report-generation?campaign_id=21&reportType=" + ReportsType.STOCKS_BY_SUPPLY);
        HttpEntity<?> request = JsonTestUtil.getJsonHttpEntity(
                getClass(),
                requestJson
        );
        ResponseEntity<String> response = FunctionalTestHelper.post(url, request);
        JsonTestUtil.assertEquals(
                response,
                JsonTestUtil.fromJsonTemplate(getClass(), responseJson)
                        .withVariable("zoneOffset", OffsetDateTime.now().getOffset().toString())
                        .toString()
        );
    }

    @Test
    @DisplayName("Проверяем генерацию отчета STOCKS_BY_SUPPLY, проверка изменения БД")
    @DbUnitDataSet(after = "AsyncReportsControllerTest.reports.stocks_by_supply.after.csv")
    void requestStocksBySupplyResponseTest() {
        String url = getUrl(1, "/request-report-generation?campaign_id=21&reportType=" + ReportsType.STOCKS_BY_SUPPLY);
        HttpEntity<?> request = JsonTestUtil.getJsonHttpEntity(
                getClass(),
                "request-report-generation-stocks-by-supply-with-multiply-warehouses.request.json"
        );
        FunctionalTestHelper.post(url, request);
    }

    @Test
    @DisplayName("Проверяем генерацию отчета ORDERS_CIS_XML")
    @DbUnitDataSet(after = "AsyncReportsControllerTest.reports.orders_cis_xml.after.csv")
    void requestOrdersCISXml() {
        String url = getUrl(1, "/request-report-generation?campaign_id=21&reportType=" + ReportsType.ORDERS_CIS_XML);
        HttpEntity<?> request = JsonTestUtil.getJsonHttpEntity(
                getClass(),
                "request-report-generation-orders-cis.request.json"
        );
        ResponseEntity<String> response = FunctionalTestHelper.post(url, request);
        JsonTestUtil.assertEquals(
                response,
                JsonTestUtil.fromJsonTemplate(getClass(), "request-report-generation-orders-cis-xml.response.json")
                        .withVariable("zoneOffset", OffsetDateTime.now().getOffset().toString())
                        .toString()
        );
    }

    @Test
    @DisplayName("Проверяем генерацию отчета ORDERS_CIS_CSV")
    @DbUnitDataSet(after = "AsyncReportsControllerTest.reports.orders_cis_csv.after.csv")
    void requestOrdersCISCsv() {
        String url = getUrl(1, "/request-report-generation?campaign_id=21&reportType=" + ReportsType.ORDERS_CIS_CSV);
        HttpEntity<?> request = JsonTestUtil.getJsonHttpEntity(
                getClass(),
                "request-report-generation-orders-cis.request.json"
        );
        ResponseEntity<String> response = FunctionalTestHelper.post(url, request);
        JsonTestUtil.assertEquals(
                response,
                JsonTestUtil.fromJsonTemplate(getClass(),
                                "request-report-generation-orders-cis-csv.response.json")
                        .withVariable("zoneOffset", OffsetDateTime.now().getOffset().toString())
                        .toString()
        );
    }

    @Test
    @DisplayName("Проверяем генерацию отчета CLAIM_LOST_ORDERS")
    @DbUnitDataSet(after = "AsyncReportsControllerTest.reports.claim_lost_orders.after.csv")
    void requestClaimLostOrders() {
        String url = getUrl(1, "/request-report-generation?campaign_id=21&reportType=" + ReportsType.CLAIM_LOST_ORDERS);
        HttpEntity<?> request = JsonTestUtil.getJsonHttpEntity(
                getClass(),
                "request-report-generation-claim-lost-orders.json"
        );
        ResponseEntity<String> response = FunctionalTestHelper.post(url, request);
        JsonTestUtil.assertEquals(
                response,
                JsonTestUtil.fromJsonTemplate(getClass(),
                                "request-report-generation-claim-lost-orders.response.json")
                        .withVariable("zoneOffset", OffsetDateTime.now().getOffset().toString())
                        .toString()
        );
    }

    @Test
    @DisplayName("Проверяем генерацию отчета ORDERS_RETURNS для диапазона дат")
    @DbUnitDataSet(after = "AsyncReportsControllerTest.reports.orders_returns.after.1.csv")
    void requestOrdersReturnsDateRange() {
        String url = getUrl(1, "/request-report-generation?campaign_id=21&reportType="
                + ReportsType.ORDERS_RETURNS);
        HttpEntity<?> request = JsonTestUtil.getJsonHttpEntity(
                getClass(),
                "request-report-generation-orders-returns.request.1.json"
        );
        ResponseEntity<String> response = FunctionalTestHelper.post(url, request);
        JsonTestUtil.assertEquals(
                response,
                JsonTestUtil.fromJsonTemplate(getClass(),
                                "request-report-generation-orders-returns.response.1.json")
                        .withVariable("zoneOffset", OffsetDateTime.now().getOffset().toString())
                        .toString()
        );
    }

    @Test
    @DisplayName("Проверяем генерацию отчета SALES_STATISTICS для диапазона дат")
    @DbUnitDataSet(after = "AsyncReportsControllerTest.reports.sales_statistics.after.1.csv")
    void salesStatisticsTest() {
        String url = getUrl(1, "/request-report-generation?campaign_id=21&reportType="
                + ReportsType.SALES_STATISTICS);
        HttpEntity<?> request = JsonTestUtil.getJsonHttpEntity(
                getClass(),
                "request-report-generation-sales-statistics.request.1.json"
        );
        ResponseEntity<String> response = FunctionalTestHelper.post(url, request);
        JsonTestUtil.assertEquals(
                response,
                JsonTestUtil.fromJsonTemplate(getClass(),
                                "request-report-generation-sales-statistics.response.1.json")
                        .withVariable("zoneOffset", OffsetDateTime.now().getOffset().toString())
                        .toString()
        );
    }

    @Test
    @DisplayName("Проверяем генерацию отчета STOCKS_ON_WAREHOUSES")
    @DbUnitDataSet(after = "AsyncReportsControllerTest.reports.stocks_on_warehouses.after.csv")
    void requestStocksOnWarehouses() {
        String url = getUrl(1,
                "/request-report-generation?campaign_id=21&reportType=" + ReportsType.STOCKS_ON_WAREHOUSES);
        HttpEntity<?> request = JsonTestUtil.getJsonHttpEntity(
                getClass(),
                "request-report-generation-stocks-on-warehouses.request.json"
        );
        ResponseEntity<String> response = FunctionalTestHelper.post(url, request);
        JsonTestUtil.assertEquals(
                response,
                JsonTestUtil.fromJsonTemplate(getClass(),
                                "request-report-generation-stocks-on-warehouses.response.json")
                        .withVariable("zoneOffset", OffsetDateTime.now().getOffset().toString())
                        .toString()
        );
    }

    @Test
    @DisplayName("Проверяем генерацию отчета ORDERS_RETURNS для заказов, готовых к выдаче партнеру")
    @DbUnitDataSet(after = "AsyncReportsControllerTest.reports.orders_returns.after.2.csv")
    void requestOrdersReturnsReadyForPickup() {
        String url = getUrl(1, "/request-report-generation?campaign_id=21&reportType=" + ReportsType.ORDERS_RETURNS);
        HttpEntity<?> request = JsonTestUtil.getJsonHttpEntity(
                getClass(),
                "request-report-generation-orders-returns.request.2.json"
        );
        ResponseEntity<String> response = FunctionalTestHelper.post(url, request);
        JsonTestUtil.assertEquals(
                response,
                JsonTestUtil.fromJsonTemplate(getClass(), "request-report-generation-orders-returns.response.2.json")
                        .withVariable("zoneOffset", OffsetDateTime.now().getOffset().toString())
                        .toString()
        );
    }

    void checkReportGeneration(String url) {
        HttpEntity request = JsonTestUtil.getJsonHttpEntity(
                getClass(),
                "request-report-generation.request.json"
        );
        ResponseEntity<String> response = FunctionalTestHelper.post(url, request);
        JsonTestUtil.assertEquals(
                response,
                JsonTestUtil.fromJsonTemplate(getClass(), "request-report-generation.expected-response.json")
                        .withVariable("zoneOffset", OffsetDateTime.now().getOffset().toString())
                        .toString()
        );
    }

    @Test
    @DisplayName("Проверяет запрос на перегенерацию отчета. Создается новая заявка V1")
    @DbUnitDataSet(
            before = "AsyncReportsControllerTest.reports.before.csv",
            after = "AsyncReportsControllerTest.requestReportRegeneration.after.csv"
    )
    void requestReportRegenerationV1() {
        String url = getUrl(1, "/request-report-regeneration?campaign_id=31&reportId=55");
        checkReportRegeneration(url);
    }

    void checkReportRegeneration(String url) {
        ResponseEntity<String> response = FunctionalTestHelper.post(url);
        JsonTestUtil.assertEquals(
                response,
                JsonTestUtil.fromJsonTemplate(getClass(), "request-report-regeneration.expected-response.json")
                        .withVariable("zoneOffset", OffsetDateTime.now().getOffset().toString())
                        .toString()
        );
    }

    @Test
    @DisplayName("Проверяет, что получим все отчёты поставщика определённого типа V1")
    @DbUnitDataSet(
            before = "AsyncReportsControllerTest.reports.before.csv",
            after = "AsyncReportsControllerTest.reports.before.csv"
    )
    void getReportInfosV1() {
        String url = getUrl(1, "/get-reports?campaign_id=21&reportType=FULFILLMENT_ORDERS");
        checkReportInfos(url, "get-report-infos.expected-response.json");
    }

    @Test
    @DisplayName("Проверяет, что получим все отчёты поставщика нескольких определённых типов V1")
    @DbUnitDataSet(
            before = "AsyncReportsControllerTest.reports.before.csv",
            after = "AsyncReportsControllerTest.reports.before.csv"
    )
    void getReportInfosV1_multi() {
        String url = getUrl(1, "/get-reports?campaign_id=21&reportType=FULFILLMENT_ORDERS,DSBS_ORDERS");
        checkReportInfos(url, "get-report-infos-multi.expected-response.json");
    }

    void checkReportInfos(String url, String jsonExcepted) {
        // Довести до ума в рамках MBI-74727
        environmentService.setValue("AsyncReportsController.get-reports.countLastDays", "99999");
        ResponseEntity<String> response = FunctionalTestHelper.get(url);
        JsonTestUtil.assertEquals(
                response,
                JsonTestUtil.fromJsonTemplate(getClass(), jsonExcepted)
                        .withVariable("zoneOffset", OffsetDateTime.now().getOffset().toString())
                        .toString()
        );
    }

    @Test
    @DisplayName("Проверяет, что получим отчёт поставщика по его id поставщика")
    @DbUnitDataSet(
            before = "AsyncReportsControllerTest.reports.before.csv",
            after = "AsyncReportsControllerTest.reports.before.csv"
    )
    void getReportV1() {
        String url = getUrl(1, "/get-report?campaign_id=21&reportId=33");
        checkGetReport(url);
    }

    void checkGetReport(String url) {
        ResponseEntity<String> response = FunctionalTestHelper.get(url);
        JsonTestUtil.assertEquals(
                response,
                JsonTestUtil.fromJsonTemplate(getClass(), "get-report-info.expected-response.json")
                        .withVariable("zoneOffset", OffsetDateTime.now().getOffset().toString())
                        .toString()
        );
    }

    @Test
    @DisplayName("Удалить отчет V1")
    @DbUnitDataSet(
            before = "AsyncReportsControllerTest.reports.before.csv",
            after = "AsyncReportsControllerTest.deleteReport.after.csv"
    )
    void deleteReportV1() {
        String url = getUrl(1, "/delete-report?campaign_id=21&reportId=22");
        checkDeleteReport(url);
    }

    void checkDeleteReport(String url) {
        Assertions.assertDoesNotThrow(() -> FunctionalTestHelper.delete(url));
    }

    @Test
    @DisplayName("Попытаться удалить несуществующий отчет V1")
    @DbUnitDataSet(
            before = "AsyncReportsControllerTest.reports.before.csv",
            after = "AsyncReportsControllerTest.reports.before.csv"
    )
    void deleteReportWrongReportIdV1() {
        String url = getUrl(1, "/delete-report?campaign_id=21&reportId=9999");
        checkDeleteReportWrongReportId(url);
    }

    void checkDeleteReportWrongReportId(String url) {
        Assertions.assertThrows(HttpClientErrorException.NotFound.class, () -> FunctionalTestHelper.delete(url));
    }

    @Test
    @DisplayName("Попытаться отменить чужой отчет V1")
    @DbUnitDataSet(
            before = "AsyncReportsControllerTest.reports.before.csv",
            after = "AsyncReportsControllerTest.reports.before.csv"
    )
    void deleteReportWrongPartnerIdV1() {
        String url = getUrl(1, "/delete-report?campaign_id=9999&reportId=22");
        checkDeleteReportWrongPartnerId(url);
    }

    void checkDeleteReportWrongPartnerId(String url) {
        Assertions.assertThrows(HttpServerErrorException.class, () -> FunctionalTestHelper.delete(url));
    }

    @Test
    @DisplayName("Попытаться создать больше, чем можно отчетов V1")
    @DbUnitDataSet(
            before = "AsyncReportsControllerTest.manyReports.before.csv",
            after = "AsyncReportsControllerTest.manyReports.after.csv"
    )
    void testTooManyReportsV1() {
        String url = getUrl(1, "/request-report-generation?campaign_id=21&reportType=FULFILLMENT_ORDERS");
        checkTestTooManyReports(url);
    }

    @Test
    @DisplayName("Получение фильтров для единых отчетов")
    @DbUnitDataSet(
            before = "AsyncReportsControllerTest.unitedInformation.before.csv"
    )
    void testUnitedInformation() {
        String url = getUrl(1, "/united-info?businessId=1000");
        ResponseEntity<String> response = FunctionalTestHelper.get(url);
        JsonTestUtil.assertEquals(
                response,
                JsonTestUtil.fromJsonTemplate(getClass(),
                                "request-report-generation-united-info.response.json")
                        .withVariable("zoneOffset", OffsetDateTime.now().getOffset().toString())
                        .toString()
        );
    }

    void checkTestTooManyReports(String url) {
        HttpEntity request = JsonTestUtil.getJsonHttpEntity(
                JsonTestUtil.fromJsonTemplate(getClass(), "request-report-generation.request.json")
                        .withVariable("zoneOffset", OffsetDateTime.now().getOffset().toString())
                        .toString()
        );

        ResponseEntity<String> response = FunctionalTestHelper.post(url, request);
        JsonTestUtil.assertEquals(
                response,
                JsonTestUtil.fromJsonTemplate(getClass(), "request-report-generation.expected-response.json")
                        .withVariable("zoneOffset", OffsetDateTime.now().getOffset().toString())
                        .toString()
        );
    }

    @Test
    @DisplayName("Если сгенерено 10 отчетов, при генерации нового самый старый должен быть удален V1")
    @DbUnitDataSet(
            before = "AsyncReportsControllerTest.manyGenerated.before.csv",
            after = "AsyncReportsControllerTest.manyGenerated.after.csv"
    )
    void testDeleteReportWhenOverLimitGeneratedV1() {
        String url = getUrl(1, "/request-report-generation?campaign_id=21&reportType=FULFILLMENT_ORDERS");
        checkTestDeleteReportWhenOverLimitGenerated(url);
    }

    void checkTestDeleteReportWhenOverLimitGenerated(String url) {
        HttpEntity request = JsonTestUtil.getJsonHttpEntity(
                JsonTestUtil.fromJsonTemplate(getClass(), "request-report-generation.request.json")
                        .withVariable("zoneOffset", OffsetDateTime.now().getOffset().toString())
                        .toString()
        );

        ResponseEntity<String> response = FunctionalTestHelper.post(url, request);
        JsonTestUtil.assertEquals(
                response,
                JsonTestUtil.fromJsonTemplate(getClass(), "request-report-generation.expected-response.json")
                        .withVariable("zoneOffset", OffsetDateTime.now().getOffset().toString())
                        .toString()
        );
    }

    private String getUrl(int v, String relatedPath) {
        return baseUrl + "/v" + v + "/async-reports" + relatedPath;
    }
}
