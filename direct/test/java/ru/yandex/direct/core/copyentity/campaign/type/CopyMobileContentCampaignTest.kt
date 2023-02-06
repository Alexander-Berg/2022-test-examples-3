package ru.yandex.direct.core.copyentity.campaign.type

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.direct.common.testing.softly
import ru.yandex.direct.core.copyentity.campaign.BaseCopyCampaignTest
import ru.yandex.direct.core.copyentity.testing.CopyAssert
import ru.yandex.direct.core.entity.campaign.model.MobileContentCampaign
import ru.yandex.direct.core.entity.mobileapp.model.MobileApp
import ru.yandex.direct.core.entity.mobilecontent.model.MobileContent
import ru.yandex.direct.core.entity.mobilecontent.service.MobileContentYtHelper
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.TestNewMobileAppBanners.fullMobileBanner
import ru.yandex.direct.core.testing.data.campaign.TestMobileContentCampaigns.fullMobileContentCampaign
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.MobileAppInfo
import ru.yandex.direct.core.testing.info.NewMobileAppBannerInfo
import ru.yandex.direct.core.testing.info.campaign.MobileContentCampaignInfo
import ru.yandex.direct.rbac.RbacRole
import ru.yandex.direct.test.utils.randomPositiveInt

@CoreTest
@RunWith(JUnitParamsRunner::class)
class CopyMobileContentCampaignTest : BaseCopyCampaignTest() {

    private val storeUrl: String =
        "https://play.google.com/store/apps/details?id=com.test.app"

    private val trackingUrlDomain: String =
        "app.appsflyer.com"

    private val trackingUrl: String =
        "https://$trackingUrlDomain/test?logid={logid}&rand=${randomPositiveInt()}"

    private val impressionUrl: String =
        "https://impression.appsflyer.com/test?logid={logid}&rand=${randomPositiveInt()}"

    @Autowired
    lateinit var mobileContentYtHelper: MobileContentYtHelper

    @Autowired
    private lateinit var copyAssert: CopyAssert

    private lateinit var mobileAppInfo: MobileAppInfo
    private lateinit var campaign: MobileContentCampaignInfo

    @Before
    fun before() {
        steps.trustedRedirectSteps().addValidCounters()

        client = steps.clientSteps().createDefaultClient()
        mobileAppInfo = steps.mobileAppSteps().createMobileApp(client, storeUrl)
        campaign = steps.mobileContentCampaignSteps()
            .createCampaign(
                MobileContentCampaignInfo(
                    clientInfo = client,
                    mobileAppInfo = mobileAppInfo,
                )
            )
        Mockito.doReturn(listOf<MobileContent>())
            .`when`(mobileContentYtHelper)
            .getMobileContentFromYt(ArgumentMatchers.anyInt(), ArgumentMatchers.any(), ArgumentMatchers.anyCollection())
    }

    @After
    fun after() {
        steps.trustedRedirectSteps().deleteTrusted()
        Mockito.reset(mobileContentYtHelper)
    }

    @Test
    fun `copy mobile content campaign`() {
        val copiedCampaign: MobileContentCampaign = copyValidCampaign(campaign)
        copyAssert.assertCampaignIsCopied(copiedCampaign, campaign.typedCampaign)
    }

    @Test
    @Parameters(method = "targetClientAllShards")
    fun `copy mobile content campaign between clients`(targetClientShard: Int) {
        targetClient = steps.clientSteps().createClient(ClientInfo(shard = targetClientShard))
        superClient = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPER)

        val result = betweenClientsCampaignCopyOperation(campaign).copy()
        val copiedMobileAppId = copyAssert.getAllCopiedEntityIds(MobileApp::class.java, result).first()

        softly {
            assertThat(copiedMobileAppId)
                .describedAs("copied mobile app id should not be equal to original")
                .isNotEqualTo(campaign.typedCampaign.mobileAppId)
            copyAssert.assertMobileAppIsCopied(campaign.typedCampaign.mobileAppId, result, this)
        }
    }

    @Test
    @Parameters(method = "targetClientAllShards")
    fun `copy mobile content campaign between clients when target client already has same mobile app`(
        targetClientShard: Int,
    ) {
        targetClient = steps.clientSteps().createClient(ClientInfo(shard = targetClientShard))
        superClient = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPER)
        val targetMobileAppInfo = steps.mobileAppSteps().createMobileApp(targetClient, storeUrl)

        val copiedCampaign: MobileContentCampaign = copyValidCampaignBetweenClients(campaign)

        assertThat(copiedCampaign.mobileAppId)
            .describedAs("copied mobile app id should be equal to target client mobile app id")
            .isEqualTo(targetMobileAppInfo.mobileAppId)
    }

    @Test
    @Parameters(method = "targetClientAllShards")
    fun `copy mobile content campaign between clients twice`(
        targetClientShard: Int,
    ) {
        targetClient = steps.clientSteps().createClient(ClientInfo(shard = targetClientShard))
        superClient = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPER)

        val fistCopiedCampaign: MobileContentCampaign = copyValidCampaignBetweenClients(campaign)
        val secondCopiedCampaign: MobileContentCampaign = copyValidCampaignBetweenClients(campaign)
        softly {
            assertThat(fistCopiedCampaign.mobileAppId)
                .describedAs("first copied mobile app id should not be equal to original")
                .isNotEqualTo(campaign.typedCampaign.mobileAppId)

            assertThat(secondCopiedCampaign.mobileAppId)
                .describedAs("second copied mobile app id must be equal to first copied id")
                .isEqualTo(fistCopiedCampaign.mobileAppId)

        }
    }

    @Test
    fun `copy mobile content campaign with null mobileAppId`() {
        val campaign = steps.mobileContentCampaignSteps()
            .createCampaign(
                client, fullMobileContentCampaign(0)
                    .withDeviceTypeTargeting(null)
                    .withNetworkTargeting(null)
            )

        val result = sameClientCampaignCopyOperation(campaign).copy()

        copyAssert.assertCampaignIsCopied(campaign.id, result)
    }

    @Test
    fun `copy mobile content campaign with ad group`() {
        val adGroup = steps.adGroupSteps()
            .createActiveMobileContentAdGroup(campaign, mobileAppInfo.mobileContentInfo)

        val operation = sameClientCampaignCopyOperation(campaign)
        val result = operation.copy()

        copyAssert.assertAdGroupIsCopied(adGroup.adGroupId, result)
    }

    @Test
    fun `copy mobile content campaign with banner`() {
        val adGroup = steps.adGroupSteps()
            .createActiveMobileContentAdGroup(campaign, mobileAppInfo.mobileContentInfo)
        val banner = steps.mobileAppBannerSteps().createMobileAppBanner(
            NewMobileAppBannerInfo()
                .withAdGroupInfo(adGroup)
                .withBanner(fullMobileAppBannerWithTrackingUrl())
        )

        val operation = sameClientCampaignCopyOperation(campaign)
        val result = operation.copy()

        copyAssert.assertBannerIsCopied(banner.bannerId, result)
    }

    @Test
    fun `copy mobile content campaign with multiple groups and banners`() {
        val adGroup1 = steps.adGroupSteps()
            .createActiveMobileContentAdGroup(campaign, mobileAppInfo.mobileContentInfo)
        val adGroup2 = steps.adGroupSteps()
            .createActiveMobileContentAdGroup(campaign, mobileAppInfo.mobileContentInfo)

        val banners = listOf(adGroup1, adGroup2).zip(0..2).map { (adGroup, _) ->
            steps.mobileAppBannerSteps().createMobileAppBanner(
                NewMobileAppBannerInfo()
                    .withAdGroupInfo(adGroup)
                    .withBanner(fullMobileAppBannerWithTrackingUrl())
            )
        }

        val operation = sameClientCampaignCopyOperation(campaign)
        val result = operation.copy()

        val bannerIds = banners.map { it.bannerId }
        copyAssert.assertBannersAreCopied(bannerIds, result)
    }

    @Test
    fun `copy mobile content campaign with campaign bid modifier`() {
        steps.bidModifierSteps().createDefaultCampBidModifierGeo(campaign)

        val operation = sameClientCampaignCopyOperation(campaign)
        val result = operation.copy()

        copyAssert.assertCampaignIsCopied(campaign.id, result)
    }

    @Test
    fun `copy mobile content campaign with ad group bid modifier`() {
        val adGroup = steps.adGroupSteps()
            .createActiveMobileContentAdGroup(campaign, mobileAppInfo.mobileContentInfo)
        val bidModifier = steps.bidModifierSteps()
            .createDefaultAdGroupBidModifierDemographics(adGroup)

        val operation = sameClientCampaignCopyOperation(campaign)
        val result = operation.copy()

        copyAssert.assertBidModifierIsCopied(bidModifier.bidModifierId, result)
    }

    @Test
    fun `copy campaign with retargeting`() {
        val adGroup = steps.adGroupSteps()
            .createActiveMobileContentAdGroup(campaign, mobileAppInfo.mobileContentInfo)
        val retargeting = steps.retargetingSteps().createDefaultRetargeting(adGroup)

        val operation = sameClientCampaignCopyOperation(campaign)
        val result = operation.copy()

        copyAssert.assertRetargetingIsCopied(retargeting.retargetingId, result)
    }

    private fun fullMobileAppBannerWithTrackingUrl() = fullMobileBanner()
        .withHref(trackingUrl)
        .withDomain(trackingUrlDomain)
        .withImpressionUrl(impressionUrl)
}
