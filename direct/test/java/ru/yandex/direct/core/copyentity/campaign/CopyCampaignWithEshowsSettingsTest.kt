package ru.yandex.direct.core.copyentity.campaign

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.core.entity.campaign.model.EshowsSettings
import ru.yandex.direct.core.entity.campaign.model.EshowsVideoType
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.campaign.TestCpmBannerCampaigns

@CoreTest
@RunWith(JUnitParamsRunner::class)
class CopyCampaignWithEshowsSettingsTest : BaseCopyCampaignTest() {

    @Before
    fun before() {
        client = steps.clientSteps().createDefaultClient()
    }

    private fun eshowsVideoTypes() = listOf(
        EshowsVideoType.LONG_CLICKS,
        EshowsVideoType.COMPLETES,
    )

    @Test
    @Parameters(method = "eshowsVideoTypes")
    fun copyCpmCampaignWithAllEshowsVideoTypes(eshowsVideoType: EshowsVideoType?) {
        val campaign = steps.cpmBannerCampaignSteps().createCampaign(
            client,
            TestCpmBannerCampaigns.fullCpmBannerCampaign()
                .withEshowsSettings(EshowsSettings().withVideoType(eshowsVideoType))
        )
        val copiedCampaign = copyValidCampaign(campaign)
        assertThat(copiedCampaign.eshowsSettings.videoType).isEqualTo(eshowsVideoType)
    }

    @Test
    fun copyCpmCampaignWithEshowsVideoTypeNull() {
        val campaign = steps.cpmBannerCampaignSteps().createCampaign(
            client,
            TestCpmBannerCampaigns.fullCpmBannerCampaign()
                .withEshowsSettings(EshowsSettings().withVideoType(null))
        )
        val copiedCampaign = copyValidCampaign(campaign)
        // CampaignWithEshowsSettingsAndStrategyAddOperationSupport
        assertThat(copiedCampaign.eshowsSettings.videoType).isEqualTo(EshowsVideoType.COMPLETES)
    }
}
