package ru.yandex.direct.core.copyentity.campaign.type

import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.direct.core.copyentity.campaign.BaseCopyCampaignTest
import ru.yandex.direct.core.copyentity.testing.CopyAssert
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.info.NewMcBannerInfo
import ru.yandex.direct.core.testing.info.campaign.McBannerCampaignInfo

@CoreTest
class CopyMcBannerCampaignTest : BaseCopyCampaignTest() {

    @Autowired
    private lateinit var copyAssert: CopyAssert

    private lateinit var campaign: McBannerCampaignInfo

    @Before
    fun before() {
        client = steps.clientSteps().createDefaultClient()
        steps.featureSteps().setCurrentClient(client.clientId)
        campaign = steps.mcBannerCampaignSteps().createDefaultCampaign(client)
    }

    @Test
    fun `copy mcbanner campaign`() {
        val copiedCampaign = copyValidCampaign(campaign)
        copyAssert.assertCampaignIsCopied(copiedCampaign, campaign.typedCampaign)
    }

    @Test
    fun `copy mcbanner campaign with adgroup`() {
        val adGroup = steps.adGroupSteps().createActiveMcBannerAdGroup(campaign)
        val result = sameClientCampaignCopyOperation(campaign).copy()
        copyAssert.assertAdGroupIsCopied(adGroup.adGroupId, result)
    }

    @Test
    fun `copy mcbanner campaign with banner`() {
        val banner = steps.mcBannerSteps().createMcBanner(NewMcBannerInfo()
            .withCampaignInfo(campaign))
        val result = sameClientCampaignCopyOperation(campaign).copy()
        copyAssert.assertBannerIsCopied(banner.bannerId, result)
    }

    @Test
    fun `copy mcbanner campaign with multiple groups and banners`() {
        val adGroup1 = steps.adGroupSteps().createActiveMcBannerAdGroup(campaign)
        val adGroup2 = steps.adGroupSteps().createActiveMcBannerAdGroup(campaign)

        val banners = listOf(adGroup1, adGroup2).zip(0..2).map { (adGroup, _) ->
            steps.mcBannerSteps().createMcBanner(NewMcBannerInfo()
                .withAdGroupInfo(adGroup))
        }

        val result = sameClientCampaignCopyOperation(campaign).copy()

        val bannerIds = banners.map { it.bannerId }
        copyAssert.assertBannersAreCopied(bannerIds, result)
    }

    @Test
    fun `copy mcbanner campaign with campaign bid modifier`() {
        steps.bidModifierSteps().createDefaultCampBidModifierGeo(campaign)
        val result = sameClientCampaignCopyOperation(campaign).copy()
        copyAssert.assertCampaignIsCopied(campaign.campaignId, result)
    }

    @Test
    fun `copy mcbanner campaign with ad group bid modifier`() {
        val adGroup = steps.adGroupSteps().createActiveMcBannerAdGroup(campaign)
        val bidModifier = steps.bidModifierSteps()
            .createDefaultAdGroupBidModifierDemographics(adGroup)

        val result = sameClientCampaignCopyOperation(campaign).copy()

        copyAssert.assertBidModifierIsCopied(bidModifier.bidModifierId, result)
    }

}
