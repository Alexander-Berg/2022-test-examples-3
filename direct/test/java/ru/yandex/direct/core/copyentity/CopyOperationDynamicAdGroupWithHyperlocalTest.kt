package ru.yandex.direct.core.copyentity

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.copyentity.model.CopyCampaignFlags
import ru.yandex.direct.core.entity.campaign.model.BaseCampaign
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.TestCampaigns
import ru.yandex.direct.core.testing.data.TestGroups
import ru.yandex.direct.core.testing.info.FeedInfo

@CoreTest
@RunWith(SpringJUnit4ClassRunner::class)
class CopyOperationDynamicAdGroupWithHyperlocalTest : CopyOperationAdGroupWithHyperlocalBaseTest() {

    lateinit var feedInfo: FeedInfo

    @Before
    fun setUp() {
        super.init()

        campaignInfo = steps.campaignSteps().createCampaign(
            TestCampaigns.activeDynamicCampaign(clientId, clientInfo!!.uid)
                .withEmail("test1@yandex-team.ru"), clientInfo)
        campaignId = campaignInfo!!.campaignId

        campaignInfoSameClient = steps.campaignSteps().createCampaign(
            TestCampaigns.activeDynamicCampaign(clientId, clientInfo.uid)
                .withEmail("test2@yandex-team.ru"), clientInfo)
        campaignIdSameClient = campaignInfoSameClient.campaignId

        campaignInfoOtherClient = steps.campaignSteps().createCampaign(
            TestCampaigns.activeDynamicCampaign(otherClientId, otherClientInfo.uid)
                .withEmail("test3@yandex-team.ru"), otherClientInfo)
        campaignIdOtherClient = campaignInfoOtherClient.campaignId

        feedInfo = steps.feedSteps().createDefaultFeed(clientInfo)

        val adGroupWithOneSegment = TestGroups.activeDynamicFeedAdGroup(campaignId, feedInfo.feedId)
            .withHyperGeoId(hyperGeo.id)
            .withHyperGeoSegmentIds(hyperGeo.hyperGeoSegments
                .map { it.id })
        adGroupInfoWithOneSegment = steps.adGroupSteps().createAdGroup(adGroupWithOneSegment, campaignInfo)

        val adGroupWithMultipleSegments = TestGroups.activeDynamicFeedAdGroup(campaignId, feedInfo.feedId)
            .withHyperGeoId(hyperGeoWithMultipleSegments.id)
            .withHyperGeoSegmentIds(hyperGeoWithMultipleSegments.hyperGeoSegments
                .map { it.id })
        adGroupInfoWithMultipleSegments = steps.adGroupSteps().createAdGroup(adGroupWithMultipleSegments, campaignInfo)
    }

    /**
     * Проверка копирования группы с гипергео между клиентами
     */
    @Test(expected = IllegalStateException::class)
    fun adGroupWithHypergeo_CopyToOtherClient() {
        val xerox = factory.build(clientInfo.shard, clientInfo.client!!,
            otherClientInfo.shard, otherClientInfo.client!!,
            otherClientInfo.uid,
            BaseCampaign::class.java,
            listOf(campaignId),
            CopyCampaignFlags(isCopyNotificationSettings = true))

        xerox.copy()
    }
}

