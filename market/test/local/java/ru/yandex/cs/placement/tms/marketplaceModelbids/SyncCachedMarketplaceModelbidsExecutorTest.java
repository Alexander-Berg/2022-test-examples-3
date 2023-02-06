package ru.yandex.cs.placement.tms.marketplaceModelbids;

import java.time.Clock;
import java.time.LocalDateTime;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.matching.EqualToJsonPattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.cs.billing.util.TimeUtil;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendor.AbstractCsPlacementTmsFunctionalTest;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.post;

@DbUnitDataSet(
        before = "/ru/yandex/cs/placement/tms/marketplaceModelbids/SyncCachedMarketplaceModelbidsExecutorTest/before.cs_billing.csv",
        dataSource = "csBillingDataSource"
)
@DbUnitDataSet(
        before = "/ru/yandex/cs/placement/tms/marketplaceModelbids/SyncCachedMarketplaceModelbidsExecutorTest/before.vendors.csv",
        dataSource = "vendorDataSource"
)
public class SyncCachedMarketplaceModelbidsExecutorTest extends AbstractCsPlacementTmsFunctionalTest {
    private final SyncCachedMarketplaceModelbidsExecutor executor;
    private final WireMockServer pricelabsMock;
    private final Clock clock;

    @Autowired
    public SyncCachedMarketplaceModelbidsExecutorTest(SyncCachedMarketplaceModelbidsExecutor executor,
                                                      WireMockServer pricelabsMock,
                                                      Clock clock) {
        this.executor = executor;
        this.pricelabsMock = pricelabsMock;
        this.clock = clock;
    }

    @DisplayName("Ставки вендоров с активной услугой отправляются в PL, с катофом - тоже, нулевые - отправляются и сбрасывается autostrategyId")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/placement/tms/marketplaceModelbids/SyncCachedMarketplaceModelbidsExecutorTest/syncBids/before.csv",
            after = "/ru/yandex/cs/placement/tms/marketplaceModelbids/SyncCachedMarketplaceModelbidsExecutorTest/syncBids/after.csv",
            dataSource = "vendorDataSource"
    )
    void syncBids() {
        LocalDateTime testCaseNow = LocalDateTime.of(2020, 7, 3, 0, 0, 0);
        Mockito.doReturn(TimeUtil.toInstant(testCaseNow)).when(clock).instant();


        pricelabsMock.stubFor(post("/autostrategy/batch?shopId=321&autoStrategyTarget=vendorBlue")
                .withRequestBody(new EqualToJsonPattern(getStringResource("/syncBids/pricelabs_autostrategy_post_body_321.json"), true, false))
                .willReturn(aResponse().withBody(
                        getStringResource("/syncBids/pricelabs_response_321.json"))));

        pricelabsMock.stubFor(post("/autostrategy/batch?shopId=322&autoStrategyTarget=vendorBlue")
                .withRequestBody(new EqualToJsonPattern(getStringResource("/syncBids/pricelabs_autostrategy_post_body_322.json"), true, false))
                .willReturn(aResponse().withBody(
                        getStringResource("/syncBids/pricelabs_response_322.json"))));

        pricelabsMock.stubFor(post("/autostrategy/batch?shopId=323&autoStrategyTarget=vendorBlue")
                .withRequestBody(new EqualToJsonPattern(getStringResource("/syncBids/pricelabs_autostrategy_post_body_323.json"), true, false))
                .willReturn(aResponse().withBody(
                        getStringResource("/syncBids/pricelabs_response_323.json"))));

        pricelabsMock.stubFor(delete(anyUrl())
                .willReturn(aResponse().withBody(
                        getStringResource("/syncBids/pricelabs_delete_response.json"))));

        executor.doJob(null);
    }

    @DisplayName("Обнулять только нулевую ставку")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/placement/tms/marketplaceModelbids/SyncCachedMarketplaceModelbidsExecutorTest/syncZeroBids/before.csv",
            after = "/ru/yandex/cs/placement/tms/marketplaceModelbids/SyncCachedMarketplaceModelbidsExecutorTest/syncZeroBids/after.csv",
            dataSource = "vendorDataSource"
    )
    void syncZeroBids() {
        LocalDateTime testCaseNow = LocalDateTime.of(2020, 11, 1, 0, 0, 0);
        Mockito.doReturn(TimeUtil.toInstant(testCaseNow)).when(clock).instant();

        executor.doJob(null);
    }
}
