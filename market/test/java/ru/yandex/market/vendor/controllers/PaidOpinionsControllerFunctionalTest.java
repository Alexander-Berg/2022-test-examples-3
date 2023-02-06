package ru.yandex.market.vendor.controllers;

import java.time.Clock;
import java.time.LocalDateTime;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.cs.billing.util.TimeUtil;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendor.AbstractVendorPartnerFunctionalTest;
import ru.yandex.market.vendor.util.FunctionalTestHelper;
import ru.yandex.vendor.util.NettyRestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;

@DbUnitDataSet(
        before = "/ru/yandex/market/vendor/controllers/PaidOpinionsControllerFunctionalTest/before.cs_billing.csv",
        dataSource = "csBillingDataSource"
)
@DbUnitDataSet(
        before = "/ru/yandex/market/vendor/controllers/PaidOpinionsControllerFunctionalTest/before.vendors.csv",
        dataSource = "vendorDataSource"
)
public class PaidOpinionsControllerFunctionalTest
        extends AbstractVendorPartnerFunctionalTest {

    private final WireMockServer reportMock;
    private final NettyRestClient persPayRestClient;
    private final Clock clock;

    @Autowired
    public PaidOpinionsControllerFunctionalTest(WireMockServer reportMock,
                                                NettyRestClient persPayRestClient,
                                                Clock clock) {
        this.reportMock = reportMock;
        this.persPayRestClient = persPayRestClient;
        this.clock = clock;
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/PaidOpinionsControllerFunctionalTest/testGetPaidModelOpinionsAll/before.csv",
            dataSource = "vendorDataSource"
    )
    void testGetPaidModelOpinionsAll() {

        LocalDateTime testCaseNow = LocalDateTime.of(2020, 6, 1, 0, 0);
        doReturn(TimeUtil.toInstant(testCaseNow)).when(clock).instant();

        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", WireMock.equalTo("brand_products"))
                .withQueryParam("hyperId", WireMock.absent())
                .willReturn(aResponse().withBody(getStringResource("/testGetPaidModelOpinions/report_products.json"))));

        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", WireMock.equalTo("brand_products"))
                .withQueryParam("hyperId", WireMock.absent())
                .withQueryParam("check-models", WireMock.equalTo("1"))
                .willReturn(aResponse().withBody(
                        getStringResource("/testGetPaidModelOpinions/report_check_models_in_marketplace.json"))));

        doAnswer(invocation -> getInputStreamResource("/testGetPaidModelOpinions/persGrade_response.json"))
                .when(persPayRestClient)
                .getForObject(any());

        assertHttpGet("/vendors/1/paidOpinions/opinions?uid=1",
                "/testGetPaidModelOpinions/expected.json");
        assertHttpGet("/vendors/1/paidOpinions/opinions?uid=1&enabled=false",
                "/testGetPaidModelOpinions/expected_disabled.json");
        assertHttpGet("/vendors/1/paidOpinions/opinions?uid=1&enabled=true",
                "/testGetPaidModelOpinions/expected_enabled.json");
    }

    @Test
    void testGetPaidModelOpinionsCategoryFilterAll() {
        LocalDateTime testCaseNow = LocalDateTime.of(2020, 6, 1, 0, 0);
        doReturn(TimeUtil.toInstant(testCaseNow)).when(clock).instant();

        doAnswer(invocation -> getInputStreamResource("/testGetPaidModelOpinions/persGrade_response.json"))
                .when(persPayRestClient)
                .getForObject(any());

        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", WireMock.equalTo("brand_products"))
                .withQueryParam("hid", WireMock.equalTo("3000"))
                .withQueryParam("hyperId", WireMock.absent())
                .willReturn(aResponse().withBody(getStringResource("/testGetPaidModelOpinions/report_products.json"))));

        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", WireMock.equalTo("brand_products"))
                .withQueryParam("hyperId", WireMock.absent())
                .withQueryParam("check-models", WireMock.equalTo("1"))
                .willReturn(aResponse().withBody(
                        getStringResource("/testGetPaidModelOpinions/report_check_models_in_marketplace.json"))));

        FunctionalTestHelper.post(baseUrl + "/vendors/1/paidOpinions/models?uid=1",
                getStringResource("/testGetPaidModelOpinions/saved_models.json"));

        assertHttpGet("/vendors/1/paidOpinions/opinions?uid=1&categoryId=3000",
                "/testGetPaidModelOpinions/expected.json");
    }

    @Test
    void testGetPaidModelsWithCategoryEnabled() {
        LocalDateTime testCaseNow = LocalDateTime.of(2020, 6, 1, 0, 0);
        doReturn(TimeUtil.toInstant(testCaseNow)).when(clock).instant();

        doAnswer(invocation -> getInputStreamResource("/testGetPaidModelOpinions/persGrade_response.json"))
                .when(persPayRestClient)
                .getForObject(any());

        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", WireMock.equalTo("brand_products"))
                .withQueryParam("hid", WireMock.equalTo("3000"))
                .withQueryParam("hyperId", WireMock.absent())
                .willReturn(aResponse().withBody(getStringResource("/testGetPaidModelOpinions/report_products_enabled.json"))));

        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", WireMock.equalTo("brand_products"))
                .withQueryParam("hyperId", WireMock.absent())
                .withQueryParam("check-models", WireMock.equalTo("1"))
                .willReturn(aResponse().withBody(
                        getStringResource("/testGetPaidModelOpinions/report_check_models_in_marketplace.json"))));

        FunctionalTestHelper.post(baseUrl + "/vendors/1/paidOpinions/models?uid=1",
                getStringResource("/testGetPaidModelOpinions/saved_models.json"));

        assertHttpGet("/vendors/1/paidOpinions/opinions?uid=1&categoryId=3000&enabled=1",
                "/testGetPaidModelsWithCategoryEnabled/expected_targets.json");
    }

    @Test
    void testGetPaidModelOpinionsCategoryFilterEnabled() {
        doAnswer(invocation -> getInputStreamResource("/testGetPaidModelOpinions/persGrade_response.json"))
                .when(persPayRestClient)
                .getForObject(any());

        LocalDateTime testCaseNow = LocalDateTime.of(2020, 6, 1, 0, 0);
        doReturn(TimeUtil.toInstant(testCaseNow)).when(clock).instant();

        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", WireMock.equalTo("brand_products"))
                .withQueryParam("hid", WireMock.equalTo("3000"))
                .withQueryParam("hyperid", WireMock.equalTo("955288,955287"))
                .willReturn(aResponse()
                        .withBody(getStringResource("/testGetPaidModelOpinions/report_products_enabled.json"))));

        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", WireMock.equalTo("brand_products"))
                .withQueryParam("hyperId", WireMock.absent())
                .withQueryParam("check-models", WireMock.equalTo("1"))
                .willReturn(aResponse().withBody(
                        getStringResource("/testGetPaidModelOpinions/report_check_models_in_marketplace.json"))));

        FunctionalTestHelper.post(baseUrl + "/vendors/1/paidOpinions/models?uid=1",
                getStringResource("/testGetPaidModelOpinions/saved_models.json"));

        assertHttpGet("/vendors/1/paidOpinions/opinions?uid=1&enabled=true&categoryId=3000&enabled=true",
                "/testGetPaidModelOpinions/expected_enabled_report.json");
    }

    @Test
    void testValidateSave() {
        LocalDateTime testCaseNow = LocalDateTime.of(2020, 6, 1, 0, 0);
        doReturn(TimeUtil.toInstant(testCaseNow)).when(clock).instant();

        saveModelExpectError("[{}]");
        saveModelExpectError("[{\"modelId\":955288, \"cashBack\": 2}]");
        saveModelExpectError("[{\"modelId\":955288, \"cashBack\": 1001}]");

        // all above should fail
        FunctionalTestHelper.postForEntity(
                baseUrl + "/vendors/1/paidOpinions/models?uid=1",
                "[{\"modelId\":955288, \"cashBack\": 50}]");

        FunctionalTestHelper.postForEntity(
                baseUrl + "/vendors/1/paidOpinions/models?uid=1",
                "[{\"modelId\":955288}]");
    }

    private void saveModelExpectError(String request) {
        try {
            FunctionalTestHelper.postForEntity(
                    baseUrl + "/vendors/1/paidOpinions/models?uid=1",
                    request);
        } catch (HttpClientErrorException e) {
            assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
        }
    }

    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/PaidOpinionsControllerFunctionalTest/testGetPaidModelOpinions/before.csv",
            after = "/ru/yandex/market/vendor/controllers/PaidOpinionsControllerFunctionalTest/testGetPaidModelOpinions/after.csv",
            dataSource = "vendorDataSource"
    )
    @Test
    void testSaveRemove() {

        doAnswer(invocation -> getInputStreamResource("/testGetPaidModelOpinions/persGrade_response.json"))
                .when(persPayRestClient)
                .getForObject(any());

        LocalDateTime testCaseNow = LocalDateTime.of(2020, 6, 1, 0, 0);
        doReturn(TimeUtil.toInstant(testCaseNow)).when(clock).instant();

        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", WireMock.equalTo("brand_products"))
                .willReturn(aResponse().withBody(getStringResource("/testGetPaidModelOpinions/report_products.json"))));

        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", WireMock.equalTo("brand_products"))
                .withQueryParam("hyperId", WireMock.absent())
                .withQueryParam("check-models", WireMock.equalTo("1"))
                .willReturn(aResponse().withBody(
                        getStringResource("/testGetPaidModelOpinions/report_check_models_in_marketplace.json"))));

        FunctionalTestHelper.post(baseUrl + "/vendors/1/paidOpinions/models?uid=1",
                getStringResource("/testGetPaidModelOpinions/saved_models_clean.json"));

    }

    @Test
    void testModelNameFilter() {
        LocalDateTime testCaseNow = LocalDateTime.of(2020, 6, 1, 0, 0);
        doReturn(TimeUtil.toInstant(testCaseNow)).when(clock).instant();

        doAnswer(invocation -> getInputStreamResource("/testGetPaidModelOpinions/persGrade_response.json"))
                .when(persPayRestClient)
                .getForObject(any());

        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", WireMock.equalTo("brand_products"))
                .withQueryParam("text", WireMock.equalTo("modelname"))
                .willReturn(aResponse().withBody(getStringResource("/testGetPaidModelOpinions/report_products.json"))));

        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", WireMock.equalTo("brand_products"))
                .withQueryParam("hyperId", WireMock.absent())
                .withQueryParam("check-models", WireMock.equalTo("1"))
                .willReturn(aResponse().withBody(
                        getStringResource("/testGetPaidModelOpinions/report_check_models_in_marketplace.json"))));

        FunctionalTestHelper.post(baseUrl + "/vendors/1/paidOpinions/models?uid=1",
                getStringResource("/testGetPaidModelOpinions/saved_models.json"));

        assertHttpGet("/vendors/1/paidOpinions/opinions?uid=1&modelName=modelname",
                "/testGetPaidModelOpinions/expected.json");
    }

    @Test
    void testHidFilter() {
        LocalDateTime testCaseNow = LocalDateTime.of(2020, 6, 1, 0, 0);
        doReturn(TimeUtil.toInstant(testCaseNow)).when(clock).instant();

        doAnswer(invocation -> getInputStreamResource("/testGetPaidModelOpinions/persGrade_response.json"))
                .when(persPayRestClient)
                .getForObject(any());

        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", WireMock.equalTo("brand_products"))
                .withQueryParam("hid", WireMock.equalTo("3000"))
                .withQueryParam("hid", WireMock.equalTo("3001"))
                .willReturn(aResponse().withBody(getStringResource("/testGetPaidModelOpinions/report_products.json"))));

        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", WireMock.equalTo("brand_products"))
                .withQueryParam("hyperId", WireMock.absent())
                .withQueryParam("check-models", WireMock.equalTo("1"))
                .willReturn(aResponse().withBody(
                        getStringResource("/testGetPaidModelOpinions/report_check_models_in_marketplace.json"))));

        FunctionalTestHelper.post(baseUrl + "/vendors/1/paidOpinions/models?uid=1",
                getStringResource("/testGetPaidModelOpinions/saved_models.json"));

        assertHttpGet("/vendors/1/paidOpinions/opinions?uid=1&categoryId=3000&categoryId=3001",
                "/testGetPaidModelOpinions/expected.json");
    }

    @Test
    void testPagingReport() {

        LocalDateTime testCaseNow = LocalDateTime.of(2020, 6, 1, 0, 0);
        doReturn(TimeUtil.toInstant(testCaseNow)).when(clock).instant();

        doAnswer(invocation -> getInputStreamResource("/testGetPaidModelOpinions/persGrade_response.json"))
                .when(persPayRestClient)
                .getForObject(any());

        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", WireMock.equalTo("brand_products"))
                .withQueryParam("numdoc", WireMock.equalTo("20"))
                .withQueryParam("page", WireMock.equalTo("2"))
                .withQueryParam("hyperid", WireMock.absent())
                .willReturn(aResponse().withBody(getStringResource("/testGetPaidModelOpinions/report_products.json"))));

        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", WireMock.equalTo("brand_products"))
                .withQueryParam("hyperId", WireMock.absent())
                .withQueryParam("check-models", WireMock.equalTo("1"))
                .willReturn(aResponse().withBody(
                        getStringResource("/testGetPaidModelOpinions/report_check_models_in_marketplace.json"))));

        FunctionalTestHelper.post(baseUrl + "/vendors/1/paidOpinions/models?uid=1",
                getStringResource("/testGetPaidModelOpinions/saved_models.json"));

        assertHttpGet("/vendors/1/paidOpinions/opinions?uid=1&page=2&pageSize=20",
                "/testGetPaidModelOpinions/expected_2_page.json");
    }

    @Test
    void testPagingDbAndReport() {
        doAnswer(invocation -> getInputStreamResource("/testGetPaidModelOpinions/persGrade_response.json"))
                .when(persPayRestClient)
                .getForObject(any());

        LocalDateTime testCaseNow = LocalDateTime.of(2020, 6, 1, 0, 0);
        doReturn(TimeUtil.toInstant(testCaseNow)).when(clock).instant();

        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", WireMock.equalTo("brand_products"))
                .withQueryParam("numdoc", WireMock.equalTo("20"))
                .withQueryParam("page", WireMock.equalTo("2"))
                .withQueryParam("hyperid", WireMock.equalTo("955288,955287"))
                .willReturn(aResponse().withBody(getStringResource("/testGetPaidModelOpinions/report_products.json"))));

        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", WireMock.equalTo("brand_products"))
                .withQueryParam("hyperId", WireMock.absent())
                .withQueryParam("check-models", WireMock.equalTo("1"))
                .willReturn(aResponse().withBody(
                        getStringResource("/testGetPaidModelOpinions/report_check_models_in_marketplace.json"))));

        FunctionalTestHelper.post(baseUrl + "/vendors/1/paidOpinions/models?uid=1",
                getStringResource("/testGetPaidModelOpinions/saved_models.json"));

        assertHttpGet("/vendors/1/paidOpinions/opinions?uid=1&enabled=true&categoryId=3000&page=2&pageSize=20",
                "/testGetPaidModelOpinions/expected_2_page.json");
    }

    @Test
    void testPagingDb() {

        LocalDateTime testCaseNow = LocalDateTime.of(2020, 6, 1, 0, 0);
        doReturn(TimeUtil.toInstant(testCaseNow)).when(clock).instant();

        doAnswer(invocation -> getInputStreamResource("/testGetPaidModelOpinions/persGrade_response.json"))
                .when(persPayRestClient)
                .getForObject(any());

        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", WireMock.equalTo("brand_products"))
                .withQueryParam("numdoc", WireMock.equalTo("2"))
                .withQueryParam("page", WireMock.equalTo("1"))
                .withQueryParam("hyperid", WireMock.equalTo("955288,955287"))
                .willReturn(aResponse().withBody(getStringResource("/testGetPaidModelOpinions/report_products.json"))));

        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", WireMock.equalTo("brand_products"))
                .withQueryParam("hyperId", WireMock.absent())
                .withQueryParam("check-models", WireMock.equalTo("1"))
                .willReturn(aResponse().withBody(
                        getStringResource("/testGetPaidModelOpinions/report_check_models_in_marketplace.json"))));

        FunctionalTestHelper.post(baseUrl + "/vendors/1/paidOpinions/models?uid=1",
                getStringResource("/testGetPaidModelOpinions/saved_models.json"));

        String actual = FunctionalTestHelper.get(baseUrl + "/vendors/1/paidOpinions/opinions?uid=1&enabled=true&page=1&pageSize=3");
        String expected = getStringResource("/testGetPaidModelOpinions/expected_enabled_2_page.json");
        JsonAssert.assertJsonEquals(expected, actual);
    }

    @Test
    @DbUnitDataSet(
            after = "/ru/yandex/market/vendor/controllers/PaidOpinionsControllerFunctionalTest/testEmptyTargetCountSave/after.csv",
            dataSource = "vendorDataSource"
    )
    void testEmptyTargetCountSave() {
        LocalDateTime testCaseNow = LocalDateTime.of(2020, 6, 1, 0, 0);
        doReturn(TimeUtil.toInstant(testCaseNow)).when(clock).instant();

        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", WireMock.equalTo("brand_products"))
                .willReturn(aResponse().withBody(getStringResource("/testGetPaidModelOpinions/report_products.json"))));

        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", WireMock.equalTo("brand_products"))
                .withQueryParam("hyperId", WireMock.absent())
                .withQueryParam("check-models", WireMock.equalTo("1"))
                .willReturn(aResponse().withBody(
                        getStringResource("/testGetPaidModelOpinions/report_check_models_in_marketplace.json"))));

        FunctionalTestHelper.post(baseUrl + "/vendors/1/paidOpinions/models?uid=1",
                getStringResource("/testGetPaidModelOpinions/saved_model_empty_target.json"));
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/PaidOpinionsControllerFunctionalTest/testGetEnabledPaidOpinionsTargetNo/before.csv",
            dataSource = "vendorDataSource"
    )
    void testGetEnabledPaidOpinionsTargetNo() {

        LocalDateTime testCaseNow = LocalDateTime.of(2020, 6, 1, 0, 0);
        doReturn(TimeUtil.toInstant(testCaseNow)).when(clock).instant();


        doAnswer(invocation -> getInputStreamResource("/testGetPaidModelOpinions/persGrade_response.json"))
                .when(persPayRestClient)
                .getForObject(any());

        reportMock.stubFor(get(anyUrl())
                .willReturn(aResponse().withBody(getStringResource("/testGetPaidModelOpinions/report_products.json"))));

        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", WireMock.equalTo("brand_products"))
                .withQueryParam("hyperId", WireMock.absent())
                .withQueryParam("check-models", WireMock.equalTo("1"))
                .willReturn(aResponse().withBody(
                        getStringResource("/testGetPaidModelOpinions/report_check_models_in_marketplace.json"))));

        assertHttpGet("/vendors/1/paidOpinions/opinions?uid=1" +
                        "&enabled=true&target=false",
                "/testGetEnabledPaidOpinionsTargetNo/expected.json");
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/PaidOpinionsControllerFunctionalTest/testAgitationStats/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/PaidOpinionsControllerFunctionalTest/testAgitationStats/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    void testAgitationStats() {
        LocalDateTime testCaseNow = LocalDateTime.of(2020, 6, 1, 0, 0);
        doReturn(TimeUtil.toInstant(testCaseNow)).when(clock).instant();

        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", WireMock.equalTo("brand_products"))
                .willReturn(aResponse().withBody(getStringResource("/testAgitationStats/report_products.json"))));

        assertHttpGet("/vendors/1/paidOpinions/stats/agitation?uid=1",
                "/testAgitationStats/expected.json");
    }

    @Test
    @DbUnitDataSet(
            after = "/ru/yandex/market/vendor/controllers/PaidOpinionsControllerFunctionalTest/testGenerateAsyncReportTask/after.csv",
            dataSource = "vendorDataSource"
    )
    void testGenerateAsyncReportTask() {

        LocalDateTime testCaseNow = LocalDateTime.of(2020, 6, 1, 0, 0);
        doReturn(TimeUtil.toInstant(testCaseNow)).when(clock).instant();

        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", WireMock.equalTo("brand_products"))
                .willReturn(aResponse().withBody(getStringResource("/testGetPaidModelOpinions/report_products.json"))));

        FunctionalTestHelper.post(baseUrl + "/vendors/1/paidOpinions/stats/async?uid=1", "{}");
    }

    @Test
    void testShowCashBack() {
        assertHttpGet("/vendors/1/paidOpinions/showCharge?selected=10&cashBack=100&uid=1",
                "/testShowCashBack/expected.json");
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/PaidOpinionsControllerFunctionalTest/testGetPaidOpinionsProductView/before.csv",
            dataSource = "csBillingDataSource"
    )
    void testGetPaidOpinionsProductView() {
        assertHttpGet("/vendors/1/paidOpinions?uid=1",
                "/testGetPaidOpinionsProductView/expected.json");
    }

}
