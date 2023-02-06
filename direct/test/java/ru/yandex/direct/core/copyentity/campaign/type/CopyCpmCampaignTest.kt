package ru.yandex.direct.core.copyentity.campaign.type

import junitparams.JUnitParamsRunner
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.direct.core.copyentity.campaign.BaseCopyCampaignTest
import ru.yandex.direct.core.copyentity.testing.CopyAssert
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefectIds
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.campaign.TestCpmBannerCampaigns
import ru.yandex.direct.feature.FeatureName

@CoreTest
@RunWith(JUnitParamsRunner::class)
class CopyCpmCampaignTest : BaseCopyCampaignTest() {

    @Autowired
    private lateinit var copyAssert: CopyAssert

    @Before
    fun before() {
        client = steps.clientSteps().createDefaultClient()
    }

    @Test
    fun copyDefaultCpmCampaign() {
        val campaign = steps.cpmBannerCampaignSteps().createCampaign(
            client,
            TestCpmBannerCampaigns.fullCpmBannerCampaign()
        )
        val result = sameClientCampaignCopyOperation(campaign).copy()
        copyAssert.assertCampaignIsCopied(campaign.id, result)
    }


    @Test
    fun copyDefaultCpmCampaign_withDisabledFeature() {
        steps.featureSteps().addClientFeature(client.clientId, FeatureName.IS_CPM_BANNER_CAMPAIGN_DISABLED, true)
        val campaign = steps.cpmBannerCampaignSteps().createCampaign(
            client,
            TestCpmBannerCampaigns.fullCpmBannerCampaign()
        )
        val result = sameClientCampaignCopyOperation(campaign).copy()
        copyAssert.assertResultContainsAllDefects(result, listOf(CampaignDefectIds.Gen.CAMPAIGN_TYPE_NOT_SUPPORTED))
    }
}
