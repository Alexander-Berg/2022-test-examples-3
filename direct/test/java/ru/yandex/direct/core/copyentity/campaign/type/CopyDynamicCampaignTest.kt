package ru.yandex.direct.core.copyentity.campaign.type

import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.direct.core.copyentity.campaign.BaseCopyCampaignTest
import ru.yandex.direct.core.copyentity.testing.CopyAssert
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.campaign.TestDynamicCampaigns.fullDynamicCampaign
import ru.yandex.direct.core.testing.info.NewDynamicBannerInfo
import ru.yandex.direct.core.testing.info.campaign.DynamicCampaignInfo

@CoreTest
class CopyDynamicCampaignTest : BaseCopyCampaignTest() {

    @Autowired
    private lateinit var copyAssert: CopyAssert

    private lateinit var campaign: DynamicCampaignInfo

    @Before
    fun before() {
        client = steps.clientSteps().createDefaultClient()
        campaign = steps.dynamicCampaignSteps().createCampaign(client, fullDynamicCampaign())
    }

    @Test
    fun `copy dynamic campaign`() {
        val operation = sameClientCampaignCopyOperation(campaign)
        val result = operation.copy()
        copyAssert.assertCampaignIsCopied(campaign.id, result)
    }

    @Test
    fun `copy dynamic campaign with text ad group`() {
        val adGroup = steps.adGroupSteps().createActiveDynamicTextAdGroup(campaign)

        val operation = sameClientCampaignCopyOperation(campaign)
        val result = operation.copy()

        copyAssert.assertAdGroupIsCopied(adGroup.adGroupId, result)
    }

    @Test
    fun `copy dynamic campaign with feed ad group`() {
        val adGroup = steps.adGroupSteps().createActiveDynamicFeedAdGroup(campaign)

        val operation = sameClientCampaignCopyOperation(campaign)
        val result = operation.copy()

        copyAssert.assertAdGroupIsCopied(adGroup.adGroupId, result)
    }

    @Test
    fun `copy dynamic campaign with banner`() {
        val banner = steps.dynamicBannerSteps().createDynamicBanner(NewDynamicBannerInfo()
            .withCampaignInfo(campaign))

        val operation = sameClientCampaignCopyOperation(campaign)
        val result = operation.copy()

        copyAssert.assertBannerIsCopied(banner.bannerId, result)
    }

    @Test
    fun `copy dynamic campaign with multiple groups and banners`() {
        val adGroup1 = steps.adGroupSteps().createActiveDynamicTextAdGroup(campaign)
        val adGroup2 = steps.adGroupSteps().createActiveDynamicFeedAdGroup(campaign)
        val banners = listOf(adGroup1, adGroup2).zip(0..2).map { (adGroup, _) ->
            steps.dynamicBannerSteps().createDynamicBanner(adGroup)
        }

        val operation = sameClientCampaignCopyOperation(campaign)
        val result = operation.copy()

        val bannerIds = banners.map { it.bannerId }
        copyAssert.assertBannersAreCopied(bannerIds, result)
    }

    @Test
    fun `copy dynamic campaign with campaign bid modifier`() {
        steps.bidModifierSteps().createDefaultCampBidModifierGeo(campaign)

        val operation = sameClientCampaignCopyOperation(campaign)
        val result = operation.copy()

        copyAssert.assertCampaignIsCopied(campaign.campaignId, result)
    }

    @Test
    fun `copy dynamic campaign with ad group bid modifier`() {
        val adGroup = steps.adGroupSteps().createActiveDynamicTextAdGroup(campaign)
        val bidModifier = steps.bidModifierSteps().createDefaultAdGroupBidModifierDemographics(adGroup)

        val operation = sameClientCampaignCopyOperation(campaign)
        val result = operation.copy()

        copyAssert.assertBidModifierIsCopied(bidModifier.bidModifierId, result)
    }
}
