package ru.yandex.direct.core.entity.feed.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.aggregatedstatuses.repository.AggregatedStatusesAdGroupRepository
import ru.yandex.direct.core.aggregatedstatuses.repository.AggregatedStatusesCampaignRepository
import ru.yandex.direct.core.entity.adgroup.aggrstatus.AggregatedStatusAdGroup
import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum
import ru.yandex.direct.core.entity.aggregatedstatuses.adgroup.AggregatedStatusAdGroupData
import ru.yandex.direct.core.entity.aggregatedstatuses.campaign.AggregatedStatusCampaignData
import ru.yandex.direct.core.entity.campaign.aggrstatus.AggregatedStatusCampaign
import ru.yandex.direct.core.entity.feed.model.FeedUsageType
import ru.yandex.direct.core.entity.feed.repository.FeedRepository
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.FeedInfo
import ru.yandex.direct.core.testing.steps.Steps
import java.util.EnumSet

@CoreTest
@RunWith(SpringJUnit4ClassRunner::class)
class FeedUsageServiceGetActualFeedUsageTest {

    private lateinit var clientInfo: ClientInfo
    private lateinit var feedInfo: FeedInfo

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var feedRepository: FeedRepository

    @Mock
    private lateinit var aggregatedStatusesCampaignRepository: AggregatedStatusesCampaignRepository

    @Mock
    private lateinit var aggregatedStatusesAdGroupRepository: AggregatedStatusesAdGroupRepository

    private lateinit var feedUsageService: FeedUsageService

    @Before
    fun init() {
        MockitoAnnotations.openMocks(this)
        feedUsageService = FeedUsageService(
            feedRepository,
            aggregatedStatusesCampaignRepository,
            aggregatedStatusesAdGroupRepository
        )
        clientInfo = steps.clientSteps().createDefaultClient()
        feedInfo = steps.feedSteps().createDefaultSyncedFeed(clientInfo)
    }

    @Test
    fun unusedFeed() {
        val actual = feedUsageService.getActualFeedUsageTypeByFeedId(clientInfo.shard, listOf(feedInfo.feedId))
        val expected = mapOf(feedInfo.feedId to EnumSet.noneOf(FeedUsageType::class.java))
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun feedInOneRunningGroupOfOneRunningCampaign() {
        val adGroup = steps.adGroupSteps().createActivePerformanceAdGroup(clientInfo, feedInfo.feedId)
        mockGroupStatus(adGroup.adGroupId, adGroup.campaignId, GdSelfStatusEnum.RUN_OK)
        mockCampaignStatus(adGroup.campaignId, GdSelfStatusEnum.RUN_OK)
        val actual = feedUsageService.getActualFeedUsageTypeByFeedId(clientInfo.shard, listOf(feedInfo.feedId))
        val expected = mapOf(feedInfo.feedId to EnumSet.of(FeedUsageType.GOODS_ADS))
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun feedInOneRunningGroupOfOneStoppedCampaign() {
        val adGroup = steps.adGroupSteps().createActivePerformanceAdGroup(clientInfo, feedInfo.feedId)
        mockGroupStatus(adGroup.adGroupId, adGroup.campaignId, GdSelfStatusEnum.STOP_CRIT)
        mockCampaignStatus(adGroup.campaignId, GdSelfStatusEnum.RUN_OK)
        val actual = feedUsageService.getActualFeedUsageTypeByFeedId(clientInfo.shard, listOf(feedInfo.feedId))
        val expected = mapOf(feedInfo.feedId to EnumSet.noneOf(FeedUsageType::class.java))
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun feedInOneStoppedGroupOfOneRunningCampaign() {
        val adGroup = steps.adGroupSteps().createActivePerformanceAdGroup(clientInfo, feedInfo.feedId)
        mockGroupStatus(adGroup.adGroupId, adGroup.campaignId, GdSelfStatusEnum.RUN_OK)
        mockCampaignStatus(adGroup.campaignId, GdSelfStatusEnum.STOP_CRIT)
        val actual = feedUsageService.getActualFeedUsageTypeByFeedId(clientInfo.shard, listOf(feedInfo.feedId))
        val expected = mapOf(feedInfo.feedId to EnumSet.noneOf(FeedUsageType::class.java))
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun feedInTwoStoppedGroupsOfOneRunningCampaign() {
        val adGroup = steps.adGroupSteps().createActivePerformanceAdGroup(clientInfo, feedInfo.feedId)
        val campaignInfo = adGroup.campaignInfo
        val otherAdGroup = steps.adGroupSteps().createActivePerformanceAdGroup(campaignInfo, feedInfo.feedId)
        mockGroupStatus(
            mapOf(
                adGroup.adGroupId to AggregatedStatusAdGroup()
                    .withId(adGroup.adGroupId)
                    .withCampaignId(campaignInfo.campaignId)
                    .withAggregatedStatus(AggregatedStatusAdGroupData(GdSelfStatusEnum.STOP_CRIT)),
                otherAdGroup.adGroupId to AggregatedStatusAdGroup()
                    .withId(otherAdGroup.adGroupId)
                    .withCampaignId(campaignInfo.campaignId)
                    .withAggregatedStatus(AggregatedStatusAdGroupData(GdSelfStatusEnum.STOP_CRIT))
            )
        )
        mockCampaignStatus(campaignInfo.campaignId, GdSelfStatusEnum.RUN_OK)
        val actual = feedUsageService.getActualFeedUsageTypeByFeedId(clientInfo.shard, listOf(feedInfo.feedId))
        val expected = mapOf(feedInfo.feedId to EnumSet.noneOf(FeedUsageType::class.java))
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun feedInOneRunningAndOneStoppedGroupsOfOneRunningCampaign() {
        val adGroup = steps.adGroupSteps().createActivePerformanceAdGroup(clientInfo, feedInfo.feedId)
        val campaignInfo = adGroup.campaignInfo
        val otherAdGroup = steps.adGroupSteps().createActivePerformanceAdGroup(campaignInfo, feedInfo.feedId)
        mockGroupStatus(
            mapOf(
                adGroup.adGroupId to AggregatedStatusAdGroup()
                    .withId(adGroup.adGroupId)
                    .withCampaignId(campaignInfo.campaignId)
                    .withAggregatedStatus(AggregatedStatusAdGroupData(GdSelfStatusEnum.STOP_CRIT)),
                otherAdGroup.adGroupId to AggregatedStatusAdGroup()
                    .withId(otherAdGroup.adGroupId)
                    .withCampaignId(campaignInfo.campaignId)
                    .withAggregatedStatus(AggregatedStatusAdGroupData(GdSelfStatusEnum.RUN_OK))
            )
        )
        mockCampaignStatus(campaignInfo.campaignId, GdSelfStatusEnum.RUN_OK)
        val actual = feedUsageService.getActualFeedUsageTypeByFeedId(clientInfo.shard, listOf(feedInfo.feedId))
        val expected = mapOf(feedInfo.feedId to EnumSet.of(FeedUsageType.GOODS_ADS))
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun feedInTwoRunningGroupsOfOneStoppedCampaign() {
        val adGroup = steps.adGroupSteps().createActivePerformanceAdGroup(clientInfo, feedInfo.feedId)
        val campaignInfo = adGroup.campaignInfo
        val otherAdGroup = steps.adGroupSteps().createActivePerformanceAdGroup(campaignInfo, feedInfo.feedId)
        mockGroupStatus(
            mapOf(
                adGroup.adGroupId to AggregatedStatusAdGroup()
                    .withId(adGroup.adGroupId)
                    .withCampaignId(campaignInfo.campaignId)
                    .withAggregatedStatus(AggregatedStatusAdGroupData(GdSelfStatusEnum.RUN_OK)),
                otherAdGroup.adGroupId to AggregatedStatusAdGroup()
                    .withId(otherAdGroup.adGroupId)
                    .withCampaignId(campaignInfo.campaignId)
                    .withAggregatedStatus(AggregatedStatusAdGroupData(GdSelfStatusEnum.RUN_OK))
            )
        )
        mockCampaignStatus(campaignInfo.campaignId, GdSelfStatusEnum.STOP_CRIT)
        val actual = feedUsageService.getActualFeedUsageTypeByFeedId(clientInfo.shard, listOf(feedInfo.feedId))
        val expected = mapOf(feedInfo.feedId to EnumSet.noneOf(FeedUsageType::class.java))
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun feedInTwoRunningGroupsOfTwoStoppedCampaigns() {
        val adGroup = steps.adGroupSteps().createActivePerformanceAdGroup(clientInfo, feedInfo.feedId)
        val otherAdGroup = steps.adGroupSteps().createActivePerformanceAdGroup(clientInfo, feedInfo.feedId)
        mockGroupStatus(
            mapOf(
                adGroup.adGroupId to AggregatedStatusAdGroup()
                    .withId(adGroup.adGroupId)
                    .withCampaignId(adGroup.campaignId)
                    .withAggregatedStatus(
                        AggregatedStatusAdGroupData(GdSelfStatusEnum.RUN_OK)
                    ),
                otherAdGroup.adGroupId to AggregatedStatusAdGroup()
                    .withId(otherAdGroup.adGroupId)
                    .withCampaignId(otherAdGroup.campaignId)
                    .withAggregatedStatus(
                        AggregatedStatusAdGroupData(GdSelfStatusEnum.RUN_OK)
                    )
            )
        )
        mockCampaignStatus(
            mapOf(
                adGroup.campaignId to AggregatedStatusCampaign()
                    .withId(adGroup.campaignId)
                    .withAggregatedStatus(
                        AggregatedStatusCampaignData(GdSelfStatusEnum.STOP_CRIT)
                    ),
                otherAdGroup.campaignId to AggregatedStatusCampaign()
                    .withId(otherAdGroup.campaignId)
                    .withAggregatedStatus(
                        AggregatedStatusCampaignData(GdSelfStatusEnum.STOP_CRIT)
                    )
            )
        )
        val actual = feedUsageService.getActualFeedUsageTypeByFeedId(clientInfo.shard, listOf(feedInfo.feedId))
        val expected = mapOf(feedInfo.feedId to EnumSet.noneOf(FeedUsageType::class.java))
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun feedInTwoRunningGroupsOfStoppedCampaignAndCampaignWithoutAggrStatus() {
        val adGroup = steps.adGroupSteps().createActivePerformanceAdGroup(clientInfo, feedInfo.feedId)
        val otherAdGroup = steps.adGroupSteps().createActivePerformanceAdGroup(clientInfo, feedInfo.feedId)
        mockGroupStatus(
            mapOf(
                adGroup.adGroupId to AggregatedStatusAdGroup()
                    .withId(adGroup.adGroupId)
                    .withCampaignId(adGroup.campaignId)
                    .withAggregatedStatus(
                        AggregatedStatusAdGroupData(GdSelfStatusEnum.RUN_OK)
                    ),
                otherAdGroup.adGroupId to AggregatedStatusAdGroup()
                    .withId(otherAdGroup.adGroupId)
                    .withCampaignId(otherAdGroup.campaignId)
                    .withAggregatedStatus(
                        AggregatedStatusAdGroupData(GdSelfStatusEnum.RUN_OK)
                    )
            )
        )
        mockCampaignStatus(
            mapOf(
                adGroup.campaignId to AggregatedStatusCampaign()
                    .withId(adGroup.campaignId)
                    .withAggregatedStatus(null),
                otherAdGroup.campaignId to AggregatedStatusCampaign()
                    .withId(otherAdGroup.campaignId)
                    .withAggregatedStatus(
                        AggregatedStatusCampaignData(GdSelfStatusEnum.STOP_CRIT)
                    )
            )
        )
        val actual = feedUsageService.getActualFeedUsageTypeByFeedId(clientInfo.shard, listOf(feedInfo.feedId))
        val expected = mapOf(feedInfo.feedId to EnumSet.of(FeedUsageType.GOODS_ADS))
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun feedInRunningGroupAndGroupWithoutAggrStatusOfTwoStoppedCampaigns() {
        val adGroup = steps.adGroupSteps().createActivePerformanceAdGroup(clientInfo, feedInfo.feedId)
        val otherAdGroup = steps.adGroupSteps().createActivePerformanceAdGroup(clientInfo, feedInfo.feedId)
        mockGroupStatus(
            mapOf(
                adGroup.adGroupId to AggregatedStatusAdGroup()
                    .withId(adGroup.adGroupId)
                    .withCampaignId(adGroup.campaignId)
                    .withAggregatedStatus(null),
                otherAdGroup.adGroupId to AggregatedStatusAdGroup()
                    .withId(otherAdGroup.adGroupId)
                    .withCampaignId(otherAdGroup.campaignId)
                    .withAggregatedStatus(
                        AggregatedStatusAdGroupData(GdSelfStatusEnum.RUN_OK)
                    )
            )
        )
        mockCampaignStatus(
            mapOf(
                adGroup.campaignId to AggregatedStatusCampaign()
                    .withId(adGroup.campaignId)
                    .withAggregatedStatus(
                        null
                    ),
                otherAdGroup.campaignId to AggregatedStatusCampaign()
                    .withId(otherAdGroup.campaignId)
                    .withAggregatedStatus(
                        AggregatedStatusCampaignData(GdSelfStatusEnum.STOP_CRIT)
                    )
            )
        )
        val actual = feedUsageService.getActualFeedUsageTypeByFeedId(clientInfo.shard, listOf(feedInfo.feedId))
        val expected = mapOf(feedInfo.feedId to EnumSet.noneOf(FeedUsageType::class.java))
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun feedInTwoRunningGroupsOfOneRunningAndOneStoppedCampaigns() {
        val adGroup = steps.adGroupSteps().createActivePerformanceAdGroup(clientInfo, feedInfo.feedId)
        val otherAdGroup = steps.adGroupSteps().createActivePerformanceAdGroup(clientInfo, feedInfo.feedId)
        mockGroupStatus(
            mapOf(
                adGroup.adGroupId to AggregatedStatusAdGroup()
                    .withId(adGroup.adGroupId)
                    .withCampaignId(adGroup.campaignId)
                    .withAggregatedStatus(
                        AggregatedStatusAdGroupData(GdSelfStatusEnum.RUN_OK)
                    ), otherAdGroup.adGroupId to AggregatedStatusAdGroup()
                    .withId(otherAdGroup.adGroupId)
                    .withCampaignId(otherAdGroup.campaignId)
                    .withAggregatedStatus(
                        AggregatedStatusAdGroupData(GdSelfStatusEnum.RUN_OK)
                    )
            )
        )
        mockCampaignStatus(
            mapOf(
                adGroup.campaignId to AggregatedStatusCampaign()
                    .withId(adGroup.campaignId)
                    .withAggregatedStatus(
                        AggregatedStatusCampaignData(GdSelfStatusEnum.RUN_OK)
                    ), otherAdGroup.adGroupId to AggregatedStatusCampaign()
                    .withId(otherAdGroup.adGroupId)
                    .withAggregatedStatus(
                        AggregatedStatusCampaignData(GdSelfStatusEnum.STOP_CRIT)
                    )
            )
        )
        val actual = feedUsageService.getActualFeedUsageTypeByFeedId(clientInfo.shard, listOf(feedInfo.feedId))
        val expected = mapOf(feedInfo.feedId to EnumSet.of(FeedUsageType.GOODS_ADS))
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun feedInTwoStoppedGroupsOfTwoRunningCampaigns() {
        val adGroup = steps.adGroupSteps().createActivePerformanceAdGroup(clientInfo, feedInfo.feedId)
        val otherAdGroup = steps.adGroupSteps().createActivePerformanceAdGroup(clientInfo, feedInfo.feedId)
        mockGroupStatus(
            mapOf(
                adGroup.adGroupId to AggregatedStatusAdGroup()
                    .withId(adGroup.adGroupId)
                    .withCampaignId(adGroup.campaignId)
                    .withAggregatedStatus(
                        AggregatedStatusAdGroupData(GdSelfStatusEnum.STOP_CRIT)
                    ), otherAdGroup.adGroupId to AggregatedStatusAdGroup()
                    .withId(otherAdGroup.adGroupId)
                    .withCampaignId(otherAdGroup.campaignId)
                    .withAggregatedStatus(
                        AggregatedStatusAdGroupData(GdSelfStatusEnum.STOP_CRIT)
                    )
            )
        )
        mockCampaignStatus(
            mapOf(
                adGroup.campaignId to AggregatedStatusCampaign()
                    .withId(adGroup.campaignId)
                    .withAggregatedStatus(
                        AggregatedStatusCampaignData(GdSelfStatusEnum.RUN_OK)
                    ), otherAdGroup.adGroupId to AggregatedStatusCampaign()
                    .withId(otherAdGroup.adGroupId)
                    .withAggregatedStatus(
                        AggregatedStatusCampaignData(GdSelfStatusEnum.RUN_OK)
                    )
            )
        )
        val actual = feedUsageService.getActualFeedUsageTypeByFeedId(clientInfo.shard, listOf(feedInfo.feedId))
        val expected = mapOf(feedInfo.feedId to EnumSet.noneOf(FeedUsageType::class.java))
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun twoFeedInOneRunningAndOneStoppedGroupsOfOneRunningCampaign() {
        val otherFeedInfo = steps.feedSteps().createDefaultSyncedFeed(clientInfo)
        val adGroup = steps.adGroupSteps().createActivePerformanceAdGroup(clientInfo, feedInfo.feedId)
        val campaignInfo = adGroup.campaignInfo
        val otherAdGroup = steps.adGroupSteps().createActivePerformanceAdGroup(campaignInfo, otherFeedInfo.feedId)
        mockGroupStatus(
            mapOf(
                adGroup.adGroupId to AggregatedStatusAdGroup()
                    .withId(adGroup.adGroupId)
                    .withCampaignId(campaignInfo.campaignId)
                    .withAggregatedStatus(
                        AggregatedStatusAdGroupData(GdSelfStatusEnum.RUN_OK)
                    ), otherAdGroup.adGroupId to AggregatedStatusAdGroup()
                    .withId(otherAdGroup.adGroupId)
                    .withCampaignId(campaignInfo.campaignId)
                    .withAggregatedStatus(
                        AggregatedStatusAdGroupData(GdSelfStatusEnum.STOP_CRIT)
                    )
            )
        )
        mockCampaignStatus(campaignInfo.campaignId, GdSelfStatusEnum.RUN_OK)
        val actual = feedUsageService.getActualFeedUsageTypeByFeedId(
            clientInfo.shard,
            listOf(feedInfo.feedId, otherFeedInfo.feedId)
        )
        val expected = mapOf(
            feedInfo.feedId to EnumSet.of(FeedUsageType.GOODS_ADS),
            otherFeedInfo.feedId to EnumSet.noneOf(FeedUsageType::class.java)
        )
        assertThat(actual).isEqualTo(expected)
    }

    private fun mockGroupStatus(result: Map<Long, AggregatedStatusAdGroup>) =
        `when`(aggregatedStatusesAdGroupRepository.getAdGroupsWithStatusById(eq(clientInfo.shard), any())).thenReturn(
            result
        )

    private fun mockGroupStatus(
        adGroupId: Long,
        campaignId: Long,
        adGroupStatus: GdSelfStatusEnum
    ) {
        mockGroupStatus(
            mapOf(
                adGroupId to AggregatedStatusAdGroup()
                    .withId(adGroupId)
                    .withCampaignId(campaignId)
                    .withAggregatedStatus(AggregatedStatusAdGroupData(adGroupStatus))
            )
        )
    }

    private fun mockCampaignStatus(result: Map<Long, AggregatedStatusCampaign>) =
        `when`(aggregatedStatusesCampaignRepository.getCampaignById(eq(clientInfo.shard), any())).thenReturn(result)

    private fun mockCampaignStatus(
        campaignId: Long,
        campaignStatus: GdSelfStatusEnum,
    ) {
        mockCampaignStatus(
            mapOf(
                campaignId to AggregatedStatusCampaign()
                    .withId(campaignId)
                    .withAggregatedStatus(AggregatedStatusCampaignData(campaignStatus))
            )
        )
    }
}
