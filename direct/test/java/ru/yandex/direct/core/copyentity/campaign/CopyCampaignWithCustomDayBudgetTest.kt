package ru.yandex.direct.core.copyentity.campaign

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.core.entity.campaign.model.CampaignDayBudgetNotificationStatus
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.campaign.TestCpmBannerCampaigns
import java.time.LocalDateTime

@CoreTest
@RunWith(JUnitParamsRunner::class)
class CopyCampaignWithCustomDayBudgetTest : BaseCopyCampaignTest() {

    @Before
    fun before() {
        client = steps.clientSteps().createDefaultClient()
    }

    private fun dayBudgetNotificationStatuses() = listOf(
        CampaignDayBudgetNotificationStatus.READY,
        CampaignDayBudgetNotificationStatus.SENT,
        null // важно чтобы был последним, иначе тест не стартанет
    )

    @Test
    @Parameters(method = "dayBudgetNotificationStatuses")
    fun copyCpmCampaignWithAllDayBudget(dayBudgetNotificationStatus: CampaignDayBudgetNotificationStatus?) {
        val campaign = steps.cpmBannerCampaignSteps().createCampaign(
            client,
            TestCpmBannerCampaigns.fullCpmBannerCampaign()
                .withDayBudgetNotificationStatus(dayBudgetNotificationStatus)
        )
        val copiedCampaign = copyValidCampaign(campaign)
        // CampaignWithCustomDayBudgetAddOperationSupport
        assertThat(copiedCampaign.dayBudgetNotificationStatus).isEqualTo(CampaignDayBudgetNotificationStatus.READY)
    }

    @Test
    fun `day budget stop time is cleared on copy`() {
        val campaign = steps.cpmBannerCampaignSteps().createCampaign(
            client,
            TestCpmBannerCampaigns.fullCpmBannerCampaign()
                .withDayBudgetStopTime(LocalDateTime.now())
        )
        val copiedCampaign = copyValidCampaign(campaign)
        assertThat(copiedCampaign.dayBudgetStopTime).isNull()
    }
}
