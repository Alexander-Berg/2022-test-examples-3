package ru.yandex.market.vendor;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.cs.placement.tms.paidOpinions.PaidOpinionsCollectedSyncExecutor;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;

public class PaidOpinionsCollectedSyncExecutorTest extends AbstractCsPlacementTmsFunctionalTest {
    @Autowired
    private PaidOpinionsCollectedSyncExecutor paidOpinionsCollectedSyncExecutor;
    @Autowired
    private WireMockServer reportMock;

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/PaidOpinionsCollectedSyncExecutorTest/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/PaidOpinionsCollectedSyncExecutorTest/before.vendors.csv",
            after = "/ru/yandex/market/vendor/PaidOpinionsCollectedSyncExecutorTest/after.csv",
            dataSource = "vendorDataSource"
    )
    void testJob() {
        reportMock.stubFor(get(anyUrl())
                .withQueryParam("place", WireMock.equalTo("brand_products"))
                .withQueryParam("hyperId", WireMock.absent())
                .willReturn(aResponse().withBody(getStringResource("/brandProducts.json"))));

        paidOpinionsCollectedSyncExecutor.doJob(null);
    }
}
