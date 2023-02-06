package ru.yandex.direct.core.copyentity.campaign

import junitparams.JUnitParamsRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.common.testing.softly
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform
import ru.yandex.direct.core.entity.campaign.model.CpmBannerCampaign
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy.defaultAutobudgetMaxReachCustomPeriod
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy.defaultAutobudgetWeekSum
import ru.yandex.direct.core.testing.data.campaign.TestCpmBannerCampaigns
import ru.yandex.direct.core.testing.data.campaign.TestTextCampaigns
import java.time.LocalDateTime

@CoreTest
@RunWith(JUnitParamsRunner::class)
class CopyCampaignWithStrategyFields : BaseCopyCampaignTest() {

    @Before
    fun before() {
        client = steps.clientSteps().createDefaultClient()
    }

    @Test
    fun copyCampaignWithAutobudgetWeekSumStrategyBidderRestartTime() {
        val campaign = steps.textCampaignSteps().createCampaign(
            client,
            TestTextCampaigns.fullTextCampaign()
                .withStrategy(defaultAutobudgetWeekSum(LocalDateTime.now().minusDays(3)))
        )
        val copiedCampaign = copyValidCampaign(campaign)

        assertThat(copiedCampaign.strategy.strategyData.lastBidderRestartTime)
            .isBetween(LocalDateTime.now().minusMinutes(2), LocalDateTime.now())
    }

    @Test
    fun copyCampaignWithAutobudgetWeekSumStrategyNullBidderRestartTime() {
        val expectedCampaign = TestTextCampaigns.fullTextCampaign()
            .withStrategy(defaultAutobudgetWeekSum(LocalDateTime.now()))

        expectedCampaign.strategy.strategyData
            .withLastBidderRestartTime(null)

        val campaign = steps.textCampaignSteps().createCampaign(client, expectedCampaign)

        val copiedCampaign = copyValidCampaign(campaign)

        assertThat(copiedCampaign.strategy.strategyData.lastBidderRestartTime)
            .isBetween(LocalDateTime.now().minusMinutes(15), LocalDateTime.now())
    }

    @Test
    fun copyCpmCampaignWithDaylyChangesCountAndLastUpdateTime() {
        val startDate = LocalDateTime.now().plusDays(1)
        val expectedCampaign: CpmBannerCampaign = TestCpmBannerCampaigns.fullCpmBannerCampaign()
            .withStartDate(startDate.toLocalDate())
            .withEndDate(startDate.toLocalDate().plusDays(1))
            .withStrategy(defaultAutobudgetMaxReachCustomPeriod(startDate))

        expectedCampaign.strategy.strategyData
            .withLastUpdateTime(LocalDateTime.now().minusDays(3))
            .withDailyChangeCount(10)

        expectedCampaign.strategy
            .withPlatform(CampaignsPlatform.CONTEXT)

        val campaign = steps.cpmBannerCampaignSteps().createCampaign(client, expectedCampaign)
        val copiedCampaign = copyValidCampaign(campaign)

        softly {
            assertThat(copiedCampaign.strategy.strategyData.lastUpdateTime)
                .isBetween(LocalDateTime.now().minusMinutes(15), LocalDateTime.now())
            assertThat(copiedCampaign.strategy.strategyData.dailyChangeCount).isEqualTo(1)
        }
    }
}
