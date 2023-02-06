package ru.yandex.direct.core.copyentity.campaign.type

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.direct.core.copyentity.campaign.BaseCopyCampaignTest
import ru.yandex.direct.core.copyentity.testing.CopyAssert
import ru.yandex.direct.core.copyentity.testing.TestCampMetrikaGoals.defaultCampMetrikaGoal
import ru.yandex.direct.core.entity.banner.model.TextBanner
import ru.yandex.direct.core.entity.campaign.model.TextCampaign
import ru.yandex.direct.core.entity.retargeting.service.CampMetrikaGoalService
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.TestNewTextBanners.fullTextBanner
import ru.yandex.direct.core.testing.data.campaign.TestTextCampaigns.fullTextCampaign
import ru.yandex.direct.core.testing.info.NewTextBannerInfo
import ru.yandex.direct.core.testing.info.campaign.TextCampaignInfo
import ru.yandex.direct.core.testing.stub.OrganizationsClientStub
import ru.yandex.direct.operation.Applicability
import ru.yandex.direct.test.utils.randomPositiveInt
import ru.yandex.direct.test.utils.randomPositiveLong

@CoreTest
class CopyTextCampaignTest : BaseCopyCampaignTest() {

    @Autowired
    private lateinit var copyAssert: CopyAssert

    @Autowired
    private lateinit var campMetrikaGoalService: CampMetrikaGoalService

    @Autowired
    private lateinit var organizationsClientStub: OrganizationsClientStub

    private lateinit var campaign: TextCampaignInfo

    @Before
    fun before() {
        client = steps.clientSteps().createDefaultClient()
        campaign = steps.textCampaignSteps().createDefaultCampaign(client)
    }

    @Test
    fun `copy text campaign`() {
        val copiedCampaign = copyValidCampaign(campaign)
        copyAssert.assertCampaignIsCopied(copiedCampaign, campaign.typedCampaign)
    }

    @Test
    fun `copy text campaign with adgroup`() {
        val adGroup = steps.adGroupSteps().createActiveTextAdGroup(campaign)
        val result = sameClientCampaignCopyOperation(campaign).copy()
        copyAssert.assertAdGroupIsCopied(adGroup.adGroupId, result)
    }

    @Test
    fun `copy text campaign with banner`() {
        val banner = steps.textBannerSteps().createBanner(NewTextBannerInfo()
            .withCampaignInfo(campaign))
        val result = sameClientCampaignCopyOperation(campaign).copy()
        copyAssert.assertBannerIsCopied(banner.bannerId, result)
    }

    @Test
    fun `copy text campaign with multiple groups and banners`() {
        val adGroup1 = steps.adGroupSteps().createActiveTextAdGroup(campaign)
        val adGroup2 = steps.adGroupSteps().createActiveTextAdGroup(campaign)

        val banners = listOf(adGroup1, adGroup2).zip(0..2).map { (adGroup, _) ->
            steps.textBannerSteps().createBanner(NewTextBannerInfo()
                .withAdGroupInfo(adGroup)
                .withCampaignInfo(campaign))
        }

        val result = sameClientCampaignCopyOperation(campaign).copy()

        val bannerIds = banners.map { it.bannerId }
        copyAssert.assertBannersAreCopied(bannerIds, result)
    }

    @Test
    fun `copy text campaign with vcard`() {
        val vcard = steps.vcardSteps().createVcard(campaign)
        val result = sameClientCampaignCopyOperation(campaign).copy()
        copyAssert.assertVcardIsCopied(vcard.vcardId, result)
    }

    @Test
    fun `copy text campaign with sitelink set`() {
        val sitelinkSet = steps.sitelinkSetSteps().createDefaultSitelinkSet(client)
        val banner = steps.textBannerSteps().createBanner(NewTextBannerInfo()
            .withCampaignInfo(campaign)
            .withBanner(fullTextBanner()
                .withSitelinksSetId(sitelinkSet.sitelinkSetId)))

        val result = sameClientCampaignCopyOperation(campaign).copy()
        val copiedBanner: TextBanner = copyAssert.getCopiedEntity(banner.bannerId, result)

        assertThat(copiedBanner.sitelinksSetId).isEqualTo(sitelinkSet.sitelinkSetId)
    }

    @Test
    fun `copy text campaign with campaign bid modifier`() {
        steps.bidModifierSteps().createDefaultCampBidModifierGeo(campaign)
        val result = sameClientCampaignCopyOperation(campaign).copy()
        copyAssert.assertCampaignIsCopied(campaign.id, result)
    }

    @Test
    fun `copy text campaign with ad group bid modifier`() {
        val adGroup = steps.adGroupSteps().createActiveTextAdGroup(campaign)
        val bidModifier = steps.bidModifierSteps()
            .createDefaultAdGroupBidModifierDemographics(adGroup)

        val result = sameClientCampaignCopyOperation(campaign).copy()

        copyAssert.assertBidModifierIsCopied(bidModifier.bidModifierId, result)
    }

    @Test
    fun `copy text campaign with retargeting`() {
        val retargeting = steps.retargetingSteps().createDefaultRetargeting(campaign)
        val result = sameClientCampaignCopyOperation(campaign).copy()
        copyAssert.assertRetargetingIsCopied(retargeting.retargetingId, result)
    }

    @Test
    fun `copy text campaign with metrika goal`() {
        val goal = defaultCampMetrikaGoal(campaign.id)
        campMetrikaGoalService.add(campaign.clientId, campaign.uid, listOf(goal), Applicability.PARTIAL)

        val result = sameClientCampaignCopyOperation(campaign).copy()

        copyAssert.assertCampMetrikaGoalIsCopied(campaign.id, goal.goalId, result)
    }

    @Test
    fun `copy text campaign with metrika counters`() {
        val counterIds = (0 until 3).map { randomPositiveInt().toLong() }
        val campaign = steps.textCampaignSteps().createCampaign(client, fullTextCampaign()
            .withMetrikaCounters(counterIds))

        val result = sameClientCampaignCopyOperation(campaign).copy()

        copyAssert.assertCampaignIsCopied(campaign.id, result)
    }

    @Test
    fun `copy text campaign with multiple metrika goals`() {
        val goals = (0 until 2).map { defaultCampMetrikaGoal(campaign.id) }
        campMetrikaGoalService.add(campaign.clientId, campaign.uid, goals, Applicability.PARTIAL)

        val result = sameClientCampaignCopyOperation(campaign).copy()

        val goalIds = goals.map { it.goalId }
        copyAssert.assertCampMetrikaGoalsAreCopied(campaign.id, goalIds, result)
    }

    @Test
    fun `copy text campaign with broad match`() {
        val campaign = steps.textCampaignSteps().createCampaign(client, fullTextCampaign()
            .apply { broadMatch.broadMatchGoalId = randomPositiveLong() })

        val result = sameClientCampaignCopyOperation(campaign).copy()
        val copiedCampaign: TextCampaign = copyAssert.getCopiedEntity(campaign.id, result)

        assertThat(copiedCampaign.broadMatch.broadMatchGoalId).isEqualTo(0)
    }

    @Test
    fun `copy text campaign with callouts`() {
        val callouts = (0 until 2).map { steps.calloutSteps().createDefaultCallout(client) }
        val calloutIds = callouts.map { it.id }

        val banner = steps.textBannerSteps().createBanner(NewTextBannerInfo()
            .withCampaignInfo(campaign)
            .withBanner(fullTextBanner()
                .withCalloutIds(calloutIds)))

        val result = sameClientCampaignCopyOperation(campaign).copy()
        val copiedBanner: TextBanner = copyAssert.getCopiedEntity(banner.bannerId, result)

        assertThat(copiedBanner.calloutIds).containsExactlyElementsOf(calloutIds)
    }

    @Test
    fun `copy text campaign with promo extension`() {
        val promoExtension = steps.promoExtensionSteps().createDefaultPromoExtension(client)
        val campaign = steps.textCampaignSteps().createCampaign(client, fullTextCampaign()
            .withPromoExtensionId(promoExtension.promoExtensionId))

        val result = sameClientCampaignCopyOperation(campaign).copy()
        val copiedCampaign: TextCampaign = copyAssert.getCopiedEntity(campaign.id, result)

        assertThat(copiedCampaign.promoExtensionId).isEqualTo(promoExtension.promoExtensionId)
    }

    @Test
    fun `copy text campaign with phone`() {
        val organization = steps.organizationSteps().createClientOrganization(client)
        organizationsClientStub.addUidsByPermalinkId(organization.permalinkId, listOf(client.uid))

        val phone = steps.clientPhoneSteps().addDefaultClientManualPhone(client.clientId)

        val campaign = steps.textCampaignSteps().createCampaign(client, fullTextCampaign()
            .withDefaultPermalinkId(organization.permalinkId)
            .withDefaultTrackingPhoneId(phone.id))

        val result = sameClientCampaignCopyOperation(campaign).copy()

        copyAssert.assertCampaignIsCopied(campaign.campaignId, result)
    }

    @Test
    fun `copy text campaign with phone on banner`() {
        val organization = steps.organizationSteps().createClientOrganization(client)
        organizationsClientStub.addUidsByPermalinkId(organization.permalinkId, listOf(client.uid))

        val phone = steps.clientPhoneSteps().addDefaultClientManualPhone(client.clientId)

        val banner = steps.textBannerSteps().createBanner(NewTextBannerInfo()
            .withCampaignInfo(campaign)
            .withBanner(fullTextBanner()
                .withPermalinkId(organization.permalinkId)
                .withPreferVCardOverPermalink(false)
                .withPhoneId(phone.id)))

        val result = sameClientCampaignCopyOperation(campaign).copy()

        copyAssert.assertBannerIsCopied(banner.bannerId, result)
    }

}
