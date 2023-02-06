package ru.yandex.market.core.fulfillment.report;

import java.io.IOException;
import java.time.YearMonth;
import java.util.List;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.web.client.RestTemplate;

import ru.yandex.common.util.IOUtils;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.fulfillment.report.oebs.OebsSendingService;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@Disabled("https://st.yandex-team.ru/MBI-68291#613231f7935ef517a9b653e8")
@DbUnitDataSet(before = "OebsSendingServiceTest.before.csv")
class OebsSendingServiceTest extends FunctionalTest {
    private static final YearMonth REPORT_GENERATION_DATE = YearMonth.of(2018, 7);
    private static final String OEBS_PUSH_MARKET_REPORT_STATISTIC_URL =
            "https://oebsapi-test.mba.yandex-team.ru/rest/pushMarketReportStatistics";
    private static final String EXPECTED_CONTENT_TYPE = "application/json;charset=UTF-8";
    private static final String FAIL_RESPONSE_MESSAGE_FILE = "OebsSendingServiceTest.failResponseMessage.json";
    private static final String FAIL_RESPONSE_DETAIL_FILE = "OebsSendingServiceTest.failResponseDetail.json";
    private static final String SUCCESS_RESPONSE_OK_FILE = "OebsSendingServiceTest.successResponse.json";
    private static final String EXPECTED_JSON_RESPONSE_FILE = "OebsSendingServiceTest.expectedResponseJson.json";

    @Autowired
    private OebsSendingService monthlyOebsSendingService;

    @Value("${oebs.push.market.report.statistics.url}")
    private String oebsMarketReportStatisticsUrl;

    @Value("${mbi.robot.oebs.api.token}")
    private String oebsApiToken;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private MockWebServer oebsReportApiMockWebServer;

    private MockRestServiceServer mockRestServiceServer;

    @BeforeEach
    void setUp() {
        mockRestServiceServer = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    @DbUnitDataSet(before = "OebsSendingServiceTest.actualContract.csv")
    void sendReportSuccessWithCompletedContractTest() throws IOException {
        createMockedOebsRestService();

        monthlyOebsSendingService.sendReport(REPORT_GENERATION_DATE, List.of(465826L, 465827L, 465828L));

        mockRestServiceServer.verify();
    }

    @Test
    @DbUnitDataSet(
            before = {"OebsSendingServiceTest.actualContract.csv", "OebsSendingServiceTest.async.before.csv" },
            after = "OebsSendingServiceTest.actualContract.async.after.csv"
    )
    void sendReportSuccessWithCompletedContractAsyncTest() throws IOException {
        createMockedOebsRestService();

        String response = org.apache.commons.io.IOUtils.toString(
                getClass().getResourceAsStream("OebsSendingServiceTest.async.json"), UTF_8);
        oebsReportApiMockWebServer.enqueue(new MockResponse().setBody(response));

        monthlyOebsSendingService.sendReport(REPORT_GENERATION_DATE, List.of(465826L, 465827L, 465828L));
    }

    @Test
    @DbUnitDataSet(
            before = {"OebsSendingServiceTest.contractIsEmpty.csv", "OebsSendingServiceTest.async.before.csv" },
            after = "OebsSendingServiceTest.actualContract.async.after.csv"
    )
    void sendReportWithFailWhenContractsIsEmptyAsync() throws IOException {
        createMockedOebsRestService();

        String response = org.apache.commons.io.IOUtils.toString(
                getClass().getResourceAsStream("OebsSendingServiceTest.async.json"), UTF_8);
        oebsReportApiMockWebServer.enqueue(new MockResponse().setBody(response));

        monthlyOebsSendingService.sendReport(REPORT_GENERATION_DATE, List.of(465826L, 465827L, 465828L));
    }

    @Test
    @DbUnitDataSet(
            before = {"OebsSendingServiceTest.actualContract.csv", "OebsSendingServiceTest.async.before.csv" },
            after = "OebsSendingServiceTest.actualContract.async.fail.after.csv"
    )
    void sendReportSuccessWithCompletedContractAsyncFailTest() throws IOException {
        createMockedOebsRestService();

        String response = org.apache.commons.io.IOUtils.toString(
                getClass().getResourceAsStream("OebsSendingServiceTest.async.fail.json"), UTF_8);
        oebsReportApiMockWebServer.enqueue(new MockResponse().setBody(response));

        monthlyOebsSendingService.sendReport(REPORT_GENERATION_DATE, List.of(465826L, 465827L, 465828L));
    }

    @Test
    @DbUnitDataSet(before = "OebsSendingServiceTest.frozenContract.csv")
    void sendReportSuccessWithFrozenContractTest() throws IOException {
        createMockedOebsRestService();

        monthlyOebsSendingService.sendReport(REPORT_GENERATION_DATE, List.of(465826L, 465827L, 465828L));

        mockRestServiceServer.verify();
    }

    @Test
    @DbUnitDataSet(before = "OebsSendingServiceTest.sendDoNotSendEmptyReport.before.csv",
                    after = "OebsSendingServiceTest.sendDoNotSendEmptyReport.after.csv")
    void sendDoNotSendEmptyReport() {
        mockRestServiceServer.reset();

        List<Long> failedPartners = monthlyOebsSendingService.sendReport(REPORT_GENERATION_DATE, List.of(465829L));

        assertTrue(failedPartners.isEmpty());
        mockRestServiceServer.verify();
    }

    private void createMockedOebsRestService() throws IOException {
        mockRestServiceServer.expect(requestTo(OEBS_PUSH_MARKET_REPORT_STATISTIC_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(EXPECTED_CONTENT_TYPE))
                .andExpect(request -> {
                    MockClientHttpRequest mockRequest = (MockClientHttpRequest) request;
                    final String actualJson = mockRequest.getBodyAsString();

                    String expectedJson = getJsonAsString(EXPECTED_JSON_RESPONSE_FILE);

                    try {
                        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.LENIENT);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                })
                .andRespond(withSuccess(getJsonAsString(SUCCESS_RESPONSE_OK_FILE), MediaType.APPLICATION_JSON));
    }

    @Test
    @DbUnitDataSet(before = "OebsSendingServiceTest.contractIsEmpty.csv")
    void sendReportWithFailWhenContractsIsEmpty() throws IOException {
        testErrorRequest(ExpectedCount.times(3), withSuccess(getJsonAsString(SUCCESS_RESPONSE_OK_FILE), MediaType.APPLICATION_JSON), false);
    }

    @Test
    @DbUnitDataSet(before = "OebsSendingServiceTest.actualContract.csv")
    void sendReportWithFailInMessageTest() throws IOException {
        testErrorRequest(ExpectedCount.times(3), withSuccess(getJsonAsString(FAIL_RESPONSE_MESSAGE_FILE), MediaType.APPLICATION_JSON), true);
    }

    @Test
    @DbUnitDataSet(before = "OebsSendingServiceTest.actualContract.csv")
    void sendReportWithFailInDetailTest() throws IOException {
        testErrorRequest(ExpectedCount.times(3), withSuccess(getJsonAsString(FAIL_RESPONSE_DETAIL_FILE), MediaType.APPLICATION_JSON), true);
    }

    @Test
    @DbUnitDataSet(before = "OebsSendingServiceTest.actualContract.csv")
    void sendReportWithFailHttpStatusTest() {
        testErrorRequest(ExpectedCount.times(3), withStatus(HttpStatus.BAD_REQUEST), true);
    }

    private void testErrorRequest(ExpectedCount expectedCount, ResponseCreator response, boolean hasFailed) {
        mockRestServiceServer.expect(expectedCount, requestTo(OEBS_PUSH_MARKET_REPORT_STATISTIC_URL)).andRespond(response);

        List<Long> failedSuppliersIds = monthlyOebsSendingService.sendReport(REPORT_GENERATION_DATE,
                List.of(465826L, 465827L, 465828L));

        assertThat(failedSuppliersIds, hasFailed ? hasItems(465826L, 465827L, 465828L) : hasSize(0));
    }

    private String getJsonAsString(String successResponseOkFile) throws IOException {
        return IOUtils.readInputStream(
                getClass().getResourceAsStream(successResponseOkFile)
        );
    }

    @Test
    void testValidateCurrentTargetMonth() {
        final YearMonth month = YearMonth.now();
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> monthlyOebsSendingService.sendReport(month, List.of(465826L, 465827L, 465828L))
                );
        assertThat(exception.getMessage(), equalTo("Cannot send reports to OEBS for required month: " + month));
    }
}
