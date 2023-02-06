package ru.yandex.direct.core.entity.feed.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.container.CampaignIdAndAdGroupIdPair
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.FeedInfo
import ru.yandex.direct.core.testing.steps.Steps

@CoreTest
@RunWith(SpringJUnit4ClassRunner::class)
class FeedUsageServiceGetCampaignIdAdGroupIdByFeedTest {

    private lateinit var clientInfo: ClientInfo
    private lateinit var feedInfo: FeedInfo

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var feedUsageService: FeedUsageService

    @Before
    fun init() {
        clientInfo = steps.clientSteps().createDefaultClient()
        feedInfo = steps.feedSteps().createDefaultSyncedFeed(clientInfo)
    }

    @Test
    fun feedUnused() {
        val actual = feedUsageService.getCampaignIdAdGroupIdByFeedIds(clientInfo.shard, listOf(feedInfo.feedId))
        val expected = emptyList<Pair<CampaignIdAndAdGroupIdPair, Long>>()
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun oneFeedUsedInPerformance() {
        val adGroupPerformance = steps.adGroupSteps().createActivePerformanceAdGroup(feedInfo.feedId)
        val actual = feedUsageService.getCampaignIdAdGroupIdByFeedIds(clientInfo.shard, listOf(feedInfo.feedId))
        val expected = listOf(
            CampaignIdAndAdGroupIdPair()
                .withCampaignId(adGroupPerformance.campaignId)
                .withAdGroupId(adGroupPerformance.adGroupId) to feedInfo.feedId
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun oneFeedUsedInDynamic() {
        val adGroupDynamic = steps.adGroupSteps().createActiveDynamicFeedAdGroup(feedInfo)
        val actual = feedUsageService.getCampaignIdAdGroupIdByFeedIds(clientInfo.shard, listOf(feedInfo.feedId))
        val expected = listOf(
            CampaignIdAndAdGroupIdPair()
                .withCampaignId(adGroupDynamic.campaignId)
                .withAdGroupId(adGroupDynamic.adGroupId) to feedInfo.feedId
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun oneFeedUsedInTwoPerformanceGroupsInSameCampaign() {
        val adGroupPerformance = steps.adGroupSteps().createActivePerformanceAdGroup(feedInfo.feedId)
        val campaignInfo = adGroupPerformance.campaignInfo
        val adGroupPerformanceSameCampaign =
            steps.adGroupSteps().createActivePerformanceAdGroup(campaignInfo, feedInfo.feedId)
        val actual = feedUsageService.getCampaignIdAdGroupIdByFeedIds(clientInfo.shard, listOf(feedInfo.feedId))
        val expected = listOf(
            CampaignIdAndAdGroupIdPair()
                .withCampaignId(campaignInfo.campaignId)
                .withAdGroupId(adGroupPerformance.adGroupId) to feedInfo.feedId,
            CampaignIdAndAdGroupIdPair()
                .withCampaignId(campaignInfo.campaignId)
                .withAdGroupId(adGroupPerformanceSameCampaign.adGroupId) to feedInfo.feedId
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun oneFeedUsedInTwoDynamicGroupsInSameCampaign() {
        val adGroupDynamic = steps.adGroupSteps().createActiveDynamicFeedAdGroup(feedInfo)
        val campaignInfo = adGroupDynamic.campaignInfo
        val adGroupDynamicSameCampaign = steps.adGroupSteps().createActiveDynamicFeedAdGroup(campaignInfo, feedInfo)
        val actual = feedUsageService.getCampaignIdAdGroupIdByFeedIds(clientInfo.shard, listOf(feedInfo.feedId))
        val expected = listOf(
            CampaignIdAndAdGroupIdPair()
                .withCampaignId(campaignInfo.campaignId)
                .withAdGroupId(adGroupDynamic.adGroupId) to feedInfo.feedId,
            CampaignIdAndAdGroupIdPair()
                .withCampaignId(campaignInfo.campaignId)
                .withAdGroupId(adGroupDynamicSameCampaign.adGroupId) to feedInfo.feedId
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun oneFeedUsedInTwoDynamicGroupsInTwoCampaigns() {
        val adGroupDynamic = steps.adGroupSteps().createActiveDynamicFeedAdGroup(feedInfo)
        val adGroupDynamicOtherCampaign = steps.adGroupSteps().createActiveDynamicFeedAdGroup(feedInfo)
        val actual = feedUsageService.getCampaignIdAdGroupIdByFeedIds(clientInfo.shard, listOf(feedInfo.feedId))
        val expected = listOf(
            CampaignIdAndAdGroupIdPair()
                .withCampaignId(adGroupDynamic.campaignId)
                .withAdGroupId(adGroupDynamic.adGroupId) to feedInfo.feedId,
            CampaignIdAndAdGroupIdPair()
                .withCampaignId(adGroupDynamicOtherCampaign.campaignId)
                .withAdGroupId(adGroupDynamicOtherCampaign.adGroupId) to feedInfo.feedId
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun oneFeedUsedInDynamicGroupAndPerformanceGroup() {
        val adGroupDynamic = steps.adGroupSteps().createActiveDynamicFeedAdGroup(feedInfo)
        val adGroupPerformance = steps.adGroupSteps().createActivePerformanceAdGroup(feedInfo.feedId)
        val actual = feedUsageService.getCampaignIdAdGroupIdByFeedIds(clientInfo.shard, listOf(feedInfo.feedId))
        val expected = listOf(
            CampaignIdAndAdGroupIdPair()
                .withCampaignId(adGroupDynamic.campaignId)
                .withAdGroupId(adGroupDynamic.adGroupId) to feedInfo.feedId,
            CampaignIdAndAdGroupIdPair()
                .withCampaignId(adGroupPerformance.campaignId)
                .withAdGroupId(adGroupPerformance.adGroupId) to feedInfo.feedId
        )
        assertThat(actual).containsExactlyInAnyOrderElementsOf(expected)
    }

    @Test
    fun twoFeedUsedInOneCampaign() {
        val otherFeedInfo = steps.feedSteps().createDefaultSyncedFeed(clientInfo)
        val adGroupDynamic = steps.adGroupSteps().createActiveDynamicFeedAdGroup(feedInfo)
        val campaignInfo = adGroupDynamic.campaignInfo
        val adGroupDynamicSameCampaignOtherFeed =
            steps.adGroupSteps().createActiveDynamicFeedAdGroup(campaignInfo, otherFeedInfo)
        val actual = feedUsageService.getCampaignIdAdGroupIdByFeedIds(
            clientInfo.shard,
            listOf(feedInfo.feedId, otherFeedInfo.feedId)
        )
        val expected = listOf(
            CampaignIdAndAdGroupIdPair()
                .withCampaignId(campaignInfo.campaignId)
                .withAdGroupId(adGroupDynamic.adGroupId) to feedInfo.feedId,
            CampaignIdAndAdGroupIdPair()
                .withCampaignId(campaignInfo.campaignId)
                .withAdGroupId(adGroupDynamicSameCampaignOtherFeed.adGroupId) to otherFeedInfo.feedId
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun twoFeedUsedInTwoCampaignsOfOneClient() {
        val otherFeedInfo = steps.feedSteps().createDefaultSyncedFeed(clientInfo)
        val adGroupDynamic = steps.adGroupSteps().createActiveDynamicFeedAdGroup(feedInfo)
        val adGroupDynamicOtherCampaignOtherFeed = steps.adGroupSteps().createActiveDynamicFeedAdGroup(otherFeedInfo)
        val actual = feedUsageService.getCampaignIdAdGroupIdByFeedIds(
            clientInfo.shard,
            listOf(feedInfo.feedId, otherFeedInfo.feedId)
        )
        val expected = listOf(
            CampaignIdAndAdGroupIdPair()
                .withCampaignId(adGroupDynamic.campaignId)
                .withAdGroupId(adGroupDynamic.adGroupId) to feedInfo.feedId,
            CampaignIdAndAdGroupIdPair()
                .withCampaignId(adGroupDynamicOtherCampaignOtherFeed.campaignId)
                .withAdGroupId(adGroupDynamicOtherCampaignOtherFeed.adGroupId) to otherFeedInfo.feedId
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun twoFeedUsedInTwoCampaignsOfTwoClients() {
        val otherClientInfo = steps.clientSteps().createDefaultClient()
        val otherClientsFeedInfo = steps.feedSteps().createDefaultSyncedFeed(otherClientInfo)
        val adGroupDynamic = steps.adGroupSteps().createActiveDynamicFeedAdGroup(feedInfo)
        val adGroupDynamicOtherClient = steps.adGroupSteps().createActivePerformanceAdGroup(otherClientsFeedInfo.feedId)
        val actual = feedUsageService.getCampaignIdAdGroupIdByFeedIds(
            clientInfo.shard,
            listOf(feedInfo.feedId, otherClientsFeedInfo.feedId)
        )
        val expected = listOf(
            CampaignIdAndAdGroupIdPair()
                .withCampaignId(adGroupDynamic.campaignId)
                .withAdGroupId(adGroupDynamic.adGroupId) to feedInfo.feedId,
            CampaignIdAndAdGroupIdPair()
                .withCampaignId(adGroupDynamicOtherClient.campaignId)
                .withAdGroupId(adGroupDynamicOtherClient.adGroupId) to otherClientsFeedInfo.feedId
        )
        assertThat(actual).containsExactlyInAnyOrderElementsOf(expected)
    }

}
