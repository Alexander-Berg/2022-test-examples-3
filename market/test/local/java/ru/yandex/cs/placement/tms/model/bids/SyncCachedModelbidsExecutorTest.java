package ru.yandex.cs.placement.tms.model.bids;

import java.time.Clock;
import java.time.LocalDateTime;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.cs.billing.util.TimeUtil;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendor.AbstractCsPlacementTmsFunctionalTest;

import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okForContentType;
import static com.github.tomakehurst.wiremock.client.WireMock.put;

@DbUnitDataSet(
        before = "/ru/yandex/cs/placement/tms/model/bids/SyncCachedModelbidsExecutorTest/before.cs_billing.csv",
        dataSource = "csBillingDataSource"
)
@DbUnitDataSet(
        before = "/ru/yandex/cs/placement/tms/model/bids/SyncCachedModelbidsExecutorTest/before.vendors.csv",
        dataSource = "vendorDataSource"
)
class SyncCachedModelbidsExecutorTest extends AbstractCsPlacementTmsFunctionalTest {

    private final SyncCachedModelbidsExecutor syncCachedModelbidsExecutor;
    private final Clock clock;
    private final WireMockServer mbiBiddingMock;
    private final WireMockServer reportMock;

    @Autowired
    SyncCachedModelbidsExecutorTest(SyncCachedModelbidsExecutor syncCachedModelbidsExecutor,
                                    Clock clock,
                                    WireMockServer mbiBiddingMock,
                                    WireMockServer reportMock) {
        this.syncCachedModelbidsExecutor = syncCachedModelbidsExecutor;
        this.clock = clock;
        this.mbiBiddingMock = mbiBiddingMock;
        this.reportMock = reportMock;
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/placement/tms/model/bids/SyncCachedModelbidsExecutorTest/testSendModelbidsSuccessfully/before.csv",
            after = "/ru/yandex/cs/placement/tms/model/bids/SyncCachedModelbidsExecutorTest/testSendModelbidsSuccessfully/after.csv",
            dataSource = "vendorDataSource"
    )
    void testSendModelbidsSuccessfully() {
        Mockito.when(clock.instant())
                .thenReturn(TimeUtil.toInstant(LocalDateTime.of(2020, 1, 10, 0, 0, 0)));

        mbiBiddingMock.stubFor(put("/market/bidding/vendors/actions/action?uid=100500")
                .willReturn(okForContentType("application/json", "10000"))
        );

        mbiBiddingMock.stubFor(put("/market/bidding/vendors/actions/action?uid=100501")
                .willReturn(okForContentType("application/json", "10000"))
        );

        mbiBiddingMock.stubFor(put("/market/bidding/vendors/actions/action?uid=100502")
                .willReturn(okForContentType("application/json", "10000"))
        );

        mbiBiddingMock.stubFor(put("/market/bidding/vendors/1010/model-bids")
                .willReturn(okForContentType("application/json", "10000"))
        );

        mbiBiddingMock.stubFor(put("/market/bidding/vendors/1011/model-bids")
                .willReturn(okForContentType("application/json", "10000"))
        );

        syncCachedModelbidsExecutor.doJob(null);
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/cs/placement/tms/model/bids/SyncCachedModelbidsExecutorTest/testResetObsoleteBids/before.csv",
            after = "/ru/yandex/cs/placement/tms/model/bids/SyncCachedModelbidsExecutorTest/testResetObsoleteBids/after.csv",
            dataSource = "vendorDataSource"
    )
    void testResetObsoleteBids() {
        Mockito.when(clock.instant())
                .thenReturn(TimeUtil.toInstant(LocalDateTime.of(2020, 1, 10, 0, 0, 0)));


        mbiBiddingMock.stubFor(put("/market/bidding/vendors/actions/action?uid=100500")
                .willReturn(okForContentType("application/json", "10000"))
        );

        mbiBiddingMock.stubFor(put("/market/bidding/vendors/actions/action?uid=100501")
                .willReturn(okForContentType("application/json", "10000"))
        );

        mbiBiddingMock.stubFor(put("/market/bidding/vendors/actions/action?uid=100502")
                .willReturn(okForContentType("application/json", "10000"))
        );

        mbiBiddingMock.stubFor(put("/market/bidding/vendors/actions/action?uid=1")
                .willReturn(okForContentType("application/json", "10000"))
        );

        mbiBiddingMock.stubFor(put("/market/bidding/vendors/1010/model-bids")
                .willReturn(okForContentType("application/json", "10000"))
        );

        mbiBiddingMock.stubFor(put("/market/bidding/vendors/1011/model-bids")
                .willReturn(okForContentType("application/json", "10000"))
        );

        mbiBiddingMock.stubFor(put("/market/bidding/vendors/1012/model-bids")
                .willReturn(okForContentType("application/json", "10000"))
        );


        String reportResponse = getStringResource("/testResetObsoleteBids/reportResponse.json");
        reportMock.stubFor(get(anyUrl())
                .willReturn(okForContentType("application/json", reportResponse)));

        syncCachedModelbidsExecutor.doJob(null);
    }

}
