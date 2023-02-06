package ru.yandex.market.vendors.analytics.tms.jobs.partner.approved;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendors.analytics.core.security.AnalyticsTvmClient;
import ru.yandex.market.vendors.analytics.core.utils.dbunit.ClickhouseDbUnitDataSet;
import ru.yandex.market.vendors.analytics.tms.FunctionalTest;

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

/**
 * Функциональный тест для джобы {@link UpdateApprovedApplicationExecutor}.
 *
 * @author sergeymironov
 */
class UpdateApprovedApplicationExecutorTest extends FunctionalTest {

    private static final String HEADER_SERVICE_TICKET = "X-Ya-Service-Ticket";

    @Autowired
    private UpdateApprovedApplicationExecutor updateApprovedApplicationExecutor;
    @Autowired
    AnalyticsTvmClient analyticsTvmClient;
    @Autowired
    RestTemplate metricsRestTemplate;

    private MockRestServiceServer mockRestServiceServer;

    @BeforeEach
    void resetMocks() {
        reset(analyticsTvmClient);
        mockRestServiceServer = MockRestServiceServer.createServer(metricsRestTemplate);
    }

    @Test
    @DbUnitDataSet(
            before = "UpdateApprovedApplicationExecutorTest.before.csv",
            after = "UpdateApprovedApplicationExecutorTest.after.csv"
    )
    @ClickhouseDbUnitDataSet(before = "UpdateApprovedApplicationExecutorTest.before.clickhouse.csv")
    @DisplayName("Тестирует обновление заявок со статусом APPROVED")
    void updateApprovedApplicationExecutorTest() {
        mockTvmAndMetrika();
        mockTvmAndAppMetrika();
        updateApprovedApplicationExecutor.doJob(null);
    }

    private void mockTvmAndMetrika() {
        String url = "http://internalapi-test.metrika.yandex.ru:8096/market_analytics/check_access";
        String serviceTicketValue = "service_ticket_for_test";
        when(analyticsTvmClient.getServiceTicketForYandexMetrika()).thenReturn(serviceTicketValue);

        String response = "{\n" +
                "   \"result\":[\n" +
                "      {\n" +
                "         \"counter_id\": 1001 ,\n" +
                "         \"uids\":[]\n" +
                "      },\n" +
                "      {\n" +
                "         \"counter_id\": 1002,\n" +
                "         \"uids\":[2]\n" +
                "      },\n" +
                "      {\n" +
                "         \"counter_id\": 1003,\n" +
                "         \"uids\":[123]\n" +
                "      }\n" +
                "   ]\n" +
                "}";

        mockRestServiceServer.expect(
                ExpectedCount.once(),
                requestTo(url))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(header(HEADER_SERVICE_TICKET, serviceTicketValue))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(response)
                );
    }

    private void mockTvmAndAppMetrika() {
        String url = "http://mobmet-intapi-test.metrika.yandex.net/market_analytics/check_access";
        String serviceTicketValue = "service_ticket_for_test";
        when(analyticsTvmClient.getServiceTicketForAppMetrika()).thenReturn(serviceTicketValue);
        String response = "{\n" +
                "   \"result\":[\n" +
                "      {\n" +
                "         \"application_id\": 2000 ,\n" +
                "         \"uids\":[]\n" +
                "      }\n" +
                "   ]\n" +
                "}";
        mockRestServiceServer.expect(
                ExpectedCount.once(),
                requestTo(url))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(header(HEADER_SERVICE_TICKET, serviceTicketValue))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(response)
                );
    }
}
