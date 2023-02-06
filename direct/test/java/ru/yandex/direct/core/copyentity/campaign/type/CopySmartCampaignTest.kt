package ru.yandex.direct.core.copyentity.campaign.type

import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.direct.core.copyentity.campaign.BaseCopyCampaignTest
import ru.yandex.direct.core.copyentity.testing.CopyAssert
import ru.yandex.direct.core.copyentity.testing.TestCampMetrikaGoals.defaultCampMetrikaGoal
import ru.yandex.direct.core.entity.campaign.model.SmartCampaign
import ru.yandex.direct.core.entity.retargeting.service.CampMetrikaGoalService
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.campaign.TestSmartCampaigns.fullSmartCampaign
import ru.yandex.direct.core.testing.info.NewPerformanceBannerInfo
import ru.yandex.direct.core.testing.info.campaign.SmartCampaignInfo
import ru.yandex.direct.core.testing.stub.MetrikaClientStub
import ru.yandex.direct.operation.Applicability
import ru.yandex.direct.test.utils.randomPositiveInt

@CoreTest
class CopySmartCampaignTest : BaseCopyCampaignTest() {

    @Autowired
    private lateinit var metrikaClientStub: MetrikaClientStub

    @Autowired
    private lateinit var campMetrikaGoalService: CampMetrikaGoalService

    @Autowired
    private lateinit var copyAssert: CopyAssert

    private val metrikaCounterId: Int = randomPositiveInt()

    private lateinit var campaign: SmartCampaignInfo

    @Before
    fun before() {
        client = steps.clientSteps().createDefaultClient()
        campaign = steps.smartCampaignSteps().createCampaign(client, fullSmartCampaign()
            .withMetrikaCounters(listOf(metrikaCounterId.toLong())))
        metrikaClientStub.addUserCounter(client.uid, metrikaCounterId)
    }

    @Test
    fun `copy smart campaign`() {
        val copiedCampaign: SmartCampaign = copyValidCampaign(campaign)
        copyAssert.assertCampaignIsCopied(copiedCampaign, campaign.typedCampaign)
    }

    @Test
    fun `copy smart campaign with ad group`() {
        val adGroup = steps.adGroupSteps().createDefaultPerformanceAdGroup(campaign)

        val operation = sameClientCampaignCopyOperation(campaign)
        val result = operation.copy()

        copyAssert.assertAdGroupIsCopied(adGroup.adGroupId, result)
    }

    @Test
    fun `copy smart campaign with two ad groups`() {
        val adGroup1 = steps.adGroupSteps().createDefaultPerformanceAdGroup(campaign)
        val adGroup2 = steps.adGroupSteps().createDefaultPerformanceAdGroup(campaign)

        val operation = sameClientCampaignCopyOperation(campaign)
        val result = operation.copy()

        copyAssert.assertAdGroupsAreCopied(listOf(adGroup1.adGroupId, adGroup2.adGroupId), result)
    }

    @Test
    fun `copy smart campaign with banner`() {
        val banner = steps.performanceBannerSteps().createPerformanceBanner(NewPerformanceBannerInfo()
            .withCampaignInfo(campaign))

        val operation = sameClientCampaignCopyOperation(campaign)
        val result = operation.copy()

        copyAssert.assertBannerIsCopied(banner.bannerId, result)
    }

    @Test
    fun `copy smart campaign with vcard`() {
        val vcard = steps.vcardSteps().createVcard(campaign)

        val operation = sameClientCampaignCopyOperation(campaign)
        val result = operation.copy()

        copyAssert.assertVcardIsCopied(vcard.vcardId, result)
    }

    @Test
    fun `copy smart campaign with campaign bid modifier`() {
        steps.bidModifierSteps().createDefaultCampBidModifierGeo(campaign)

        val operation = sameClientCampaignCopyOperation(campaign)
        val result = operation.copy()

        copyAssert.assertCampaignIsCopied(campaign.id, result)
    }

    @Test
    fun `copy smart campaign with ad group bid modifier`() {
        val adGroup = steps.adGroupSteps().createDefaultPerformanceAdGroup(campaign)
        val bidModifier = steps.bidModifierSteps().createDefaultAdGroupBidModifierDemographics(adGroup)

        val operation = sameClientCampaignCopyOperation(campaign)
        val result = operation.copy()

        copyAssert.assertBidModifierIsCopied(bidModifier.bidModifierId, result)
    }

    @Test
    fun `copy smart campaign with metrika goal`() {
        val goal = defaultCampMetrikaGoal(campaign.id)
        campMetrikaGoalService.add(campaign.clientId, campaign.uid, listOf(goal), Applicability.PARTIAL)

        val operation = sameClientCampaignCopyOperation(campaign)
        val result = operation.copy()

        copyAssert.assertCampMetrikaGoalIsCopied(campaign.id, goal.goalId, result)
    }

    @Test
    fun `copy smart campaign with multiple metrika goals`() {
        val goals = (0 until 2).map { defaultCampMetrikaGoal(campaign.id) }
        campMetrikaGoalService.add(campaign.clientId, campaign.uid, goals, Applicability.PARTIAL)

        val result = sameClientCampaignCopyOperation(campaign).copy()

        val goalIds = goals.map { it.goalId }
        copyAssert.assertCampMetrikaGoalsAreCopied(campaign.id, goalIds, result)
    }
}
