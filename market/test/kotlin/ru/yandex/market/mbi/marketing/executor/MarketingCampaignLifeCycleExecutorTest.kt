package ru.yandex.market.mbi.marketing.executor

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.mbi.marketing.FunctionalTest
import ru.yandex.market.mbi.marketing.toInstantAtUtc3
import java.time.Clock
import java.time.LocalDateTime
import java.time.Month

class MarketingCampaignLifeCycleExecutorTest(
    @Autowired val marketingCampaignLifeCycleExecutor: MarketingCampaignLifeCycleExecutor,
    @Autowired val mbiMock: WireMockServer,
    @Autowired val clock: Clock
) : FunctionalTest() {

    @BeforeEach
    internal fun setUp() {
        mbiMock.stubFor(WireMock.get(WireMock.anyUrl())
            .willReturn(WireMock.okXml(getStringResource("/mbiApiResponse.xml"))))
    }


    @DbUnitDataSet(
        before = ["/ru/yandex/market/mbi/marketing/executor/MarketingCampaignLifeCycleExecutorTest/test_move_campaigns_by_lifecycle_to_stale/before.csv"],
        after = ["/ru/yandex/market/mbi/marketing/executor/MarketingCampaignLifeCycleExecutorTest/test_move_campaigns_by_lifecycle_to_stale/after.csv"]
    )
    @Test
    fun `test move campaigns by lifecycle to stale` () {
        Mockito.`when`(clock.instant())
            .thenReturn(LocalDateTime.of(2020, Month.MAY, 26, 0, 0, 1).toInstantAtUtc3())
        marketingCampaignLifeCycleExecutor.doJob(null)
    }

    @DbUnitDataSet(
        before = ["/ru/yandex/market/mbi/marketing/executor/MarketingCampaignLifeCycleExecutorTest/test_move_campaigns_by_lifecycle_to_active/before.csv"],
        after = ["/ru/yandex/market/mbi/marketing/executor/MarketingCampaignLifeCycleExecutorTest/test_move_campaigns_by_lifecycle_to_active/after.csv"]
    )
    @Test
    fun `test move campaigns by lifecycle to active` () {
        Mockito.`when`(clock.instant())
            .thenReturn(LocalDateTime.of(2021, Month.JUNE, 9, 12, 0, 0).toInstantAtUtc3())
        marketingCampaignLifeCycleExecutor.doJob(null)
    }

    @DbUnitDataSet(
        before = ["/ru/yandex/market/mbi/marketing/executor/MarketingCampaignLifeCycleExecutorTest/test_move_campaigns_by_lifecycle_to_finished/before.csv"],
        after = ["/ru/yandex/market/mbi/marketing/executor/MarketingCampaignLifeCycleExecutorTest/test_move_campaigns_by_lifecycle_to_finished/after.csv"]
    )
    @Test
    fun `test move campaigns by lifecycle to finished` () {
        Mockito.`when`(clock.instant())
            .thenReturn(LocalDateTime.of(2021, Month.JUNE, 10, 12, 30, 0).toInstantAtUtc3())
        marketingCampaignLifeCycleExecutor.doJob(null)
    }

}
