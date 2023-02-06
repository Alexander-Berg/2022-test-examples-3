package ru.yandex.direct.core.copyentity.campaign

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.core.entity.campaign.model.ImpressionStandardTime
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.campaign.TestCpmBannerCampaigns

@CoreTest
@RunWith(JUnitParamsRunner::class)
class CopyCampaignWithImpressionStandardTimeTest : BaseCopyCampaignTest() {

    @Before
    fun before() {
        client = steps.clientSteps().createDefaultClient()
        steps.featureSteps().setCurrentClient(client.clientId)
    }

    private fun impressionStandardTimes() = listOf(
        ImpressionStandardTime.MRC,
        ImpressionStandardTime.YANDEX,
        null, // важно чтобы был последним, иначе тест не стартанет
    )

    @Test
    @Parameters(method = "impressionStandardTimes")
    fun copyCpmCampaignWithImpressionStandardTimes(impressionStandardTime: ImpressionStandardTime?) {
        val campaign = steps.cpmBannerCampaignSteps().createCampaign(
            client,
            TestCpmBannerCampaigns.fullCpmBannerCampaign()
                .withImpressionStandardTime(impressionStandardTime)
        )
        val copiedCampaign = copyValidCampaign(campaign)
        assertThat(copiedCampaign.impressionStandardTime).isEqualTo(impressionStandardTime)
    }

}
