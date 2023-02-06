package ru.yandex.cs.placement.tms.abc;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.Month;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.matching.EqualToJsonPattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.cs.billing.util.TimeUtil;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendor.AbstractCsPlacementTmsFunctionalTest;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.mockito.Mockito.when;

@DbUnitDataSet(
        before = "/ru/yandex/cs/placement/tms/abc/AbcResourceSyncExecutorFunctionalTest/before.csv",
        dataSource = "vendorDataSource"
)
public class AbcResourceSyncExecutorFunctionalTest extends AbstractCsPlacementTmsFunctionalTest {
    @Autowired
    private AbcResourceSyncExecutor abcResourceSyncExecutor;
    @Autowired
    private WireMockServer abcMock;
    @Autowired
    private Clock clock;

    @Test
    @DbUnitDataSet(
            after = "/ru/yandex/cs/placement/tms/abc/AbcResourceSyncExecutorFunctionalTest/sendNotSyncedResourcesToAbc/after.csv",
            dataSource = "vendorDataSource"
    )
    void sendNotSyncedResourcesToAbc() {
        LocalDateTime now = LocalDateTime.of(2020, Month.JUNE, 16, 10, 23);
        when(clock.instant()).thenReturn(TimeUtil.toInstant(now));

        abcMock.stubFor(post(urlPathEqualTo("/v4/resources/financial_resources/"))
                .withRequestBody(new EqualToJsonPattern(getStringResource("/sendNotSyncedResourcesToAbc/abc_request_1006540.json"), true, false))
                .willReturn(aResponse().withBody(getStringResource("/sendNotSyncedResourcesToAbc/abc_request_1006540.json")).withStatus(201).withHeader("Content-Type", "application/json")));

        abcMock.stubFor(post(urlPathEqualTo("/v4/resources/financial_resources/"))
                .withRequestBody(new EqualToJsonPattern(getStringResource("/sendNotSyncedResourcesToAbc/abc_request_1006541.json"), true, false))
                .willReturn(aResponse().withBody(getStringResource("/sendNotSyncedResourcesToAbc/abc_request_1006541.json")).withStatus(201).withHeader("Content-Type", "application/json")));

        abcMock.stubFor(post(urlPathEqualTo("/v4/resources/financial_resources/"))
                .withRequestBody(new EqualToJsonPattern(getStringResource("/sendNotSyncedResourcesToAbc/abc_request_1006542.json"), true, false))
                .willReturn(aResponse().withBody(getStringResource("/sendNotSyncedResourcesToAbc/abc_request_1006542.json")).withStatus(201).withHeader("Content-Type", "application/json")));

        abcResourceSyncExecutor.doJob(null);
    }
}
