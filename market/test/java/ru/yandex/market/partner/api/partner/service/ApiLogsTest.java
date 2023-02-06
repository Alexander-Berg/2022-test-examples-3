package ru.yandex.market.partner.api.partner.service;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import ru.yandex.market.core.campaign.CampaignService;
import ru.yandex.market.mbi.logprocessor.client.MbiLogProcessorClient;
import ru.yandex.market.mbi.logprocessor.client.model.ApiLogRecordsResponse;
import ru.yandex.market.mbi.logprocessor.client.model.GetApiLogsRequest;
import ru.yandex.market.mbi.logprocessor.client.model.GetCpaApiLogsRequest;
import ru.yandex.market.partner.api.partner.RowId;
import ru.yandex.market.partner.api.partner.ViewApiLogServantlet;
import ru.yandex.market.partner.api.partner.ViewPullApiLogServantlet;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiLogsTest extends FunctionalTest {

    private static final String VIEW_PULL_API_LOG_URL = "/viewPullApiLog";
    private static final String VIEW_API_LOG_URL = "/viewApiLog";

    @Autowired // mock
    public MbiLogProcessorClient logProcessorClient;
    @Autowired
    public CampaignService campaignService;
    @Autowired
    public ViewPullApiLogServantlet<?> viewPullApiLogServantlet;
    @Autowired
    public ViewApiLogServantlet<?> viewApiLogServantlet;

    private final Pattern okRegex = Pattern.compile("<data.*><pager>.*</pager></data>");
    private final Pattern errorRegex = Pattern.compile("<data.*><errors>.*</errors></data>");

    private void testApiLogs(String requestUrl, Predicate<String> responsePredicate) {
        var response = FunctionalTestHelper.get(baseUrl + requestUrl);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).matches(responsePredicate);
    }

    private void testCpaApiLogs(String requestUrl,
                                GetCpaApiLogsRequest getApiLogsRequest,
                                ApiLogRecordsResponse apiLogRecordsResponse,
                                Predicate<String> responsePredicate) {
        Mockito.when(logProcessorClient.getCpaApiLogs(getApiLogsRequest)).thenReturn(apiLogRecordsResponse);
        testApiLogs(requestUrl, responsePredicate);
    }

    private void testAllApiLogs(String requestUrl,
                                GetApiLogsRequest getApiLogsRequest,
                                ApiLogRecordsResponse apiLogRecordsResponse,
                                Predicate<String> responsePredicate) {
        Mockito.when(logProcessorClient.getAllApiLogs(getApiLogsRequest)).thenReturn(apiLogRecordsResponse);
        testApiLogs(requestUrl, responsePredicate);
    }

    private static OffsetDateTime getOffsetDate(int year, int month, int day, int hour, int minute, int second) {
        return ZonedDateTime.of(
                year, month, day, hour, minute, second, 0, ZoneId.systemDefault()
        ).toOffsetDateTime();
    }

    @Test
    void testSpecialLog() {
        long campaignId = 1001089570;
        RowId rowId = new RowId(10781189L,
                1615390527121L,
                "1615390527120/9a6a070f826f7dd22ee48a6a30bd0500");
        String requestUrl = String.format("?campaignId=%s&rowid=%s", campaignId, rowId);
        GetCpaApiLogsRequest getCpaApiLogsRequest = new GetCpaApiLogsRequest();
        GetApiLogsRequest getApiLogsRequest = new GetApiLogsRequest();
        Stream.of(getCpaApiLogsRequest, getApiLogsRequest).forEach((logsRequest) ->
                logsRequest.campaignId(campaignId)
                        .partnerId(rowId.getPartnerId())
                        .requestDate(rowId.getRequestDate())
                        .traceId(rowId.getTraceId())
        );
        ApiLogRecordsResponse apiLogRecordsResponse = new ApiLogRecordsResponse();
        testCpaApiLogs(VIEW_PULL_API_LOG_URL + requestUrl,
                getCpaApiLogsRequest, apiLogRecordsResponse, okRegex.asMatchPredicate());
        testAllApiLogs(VIEW_API_LOG_URL + requestUrl,
                getApiLogsRequest, apiLogRecordsResponse, okRegex.asMatchPredicate());
    }

    @Test
    void testLogsPeriod() {
        long campaignId = 1001089570;
        String requestParams = "?campaignId=1001089570&fdt=14.04.2021 00:00:00&tdt=16.04.2021 00:00:00";
        OffsetDateTime fromDate = getOffsetDate(2021, 4, 14, 0, 0, 0);
        OffsetDateTime toDate = getOffsetDate(2021, 4, 16, 0, 0, 0);
        GetCpaApiLogsRequest getCpaApiLogsRequest = new GetCpaApiLogsRequest();
        GetApiLogsRequest getApiLogsRequest = new GetApiLogsRequest();
        Stream.of(getCpaApiLogsRequest, getApiLogsRequest).forEach((logsRequest) ->
                logsRequest.campaignId(campaignId)
                        .fromDate(fromDate)
                        .toDate(toDate)
        );
        ApiLogRecordsResponse apiLogRecordsResponse = new ApiLogRecordsResponse();
        testCpaApiLogs(VIEW_PULL_API_LOG_URL + requestParams,
                getCpaApiLogsRequest, apiLogRecordsResponse, okRegex.asMatchPredicate());
        testAllApiLogs(VIEW_API_LOG_URL + requestParams,
                getApiLogsRequest, apiLogRecordsResponse, okRegex.asMatchPredicate());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            VIEW_PULL_API_LOG_URL + "?campaignId=1001089570&fdt=14.04.2021 00:00:00",
            VIEW_API_LOG_URL + "?campaignId=1001089570&fdt=14.04.2021 00:00:00",
            VIEW_PULL_API_LOG_URL + "?fdt=14.04.2021 00:00:00&tdt=15.04.2021 00:00:00",
            VIEW_API_LOG_URL + "?fdt=14.04.2021 00:00:00&tdt=15.04.2021 00:00:00"
    })
    void testLogsNotEnoughFilters(String url) {
        testApiLogs(url, errorRegex.asMatchPredicate());
    }
}
