package ru.yandex.direct.logicprocessor.processors.feeds.usagetypes

import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.any
import org.mockito.MockitoAnnotations
import ru.yandex.direct.binlog.model.Operation
import ru.yandex.direct.core.entity.feed.model.Feed
import ru.yandex.direct.core.entity.feed.model.FeedSimple
import ru.yandex.direct.core.entity.feed.model.FeedUsageType
import ru.yandex.direct.core.entity.feed.repository.FeedRepository
import ru.yandex.direct.core.entity.feed.service.FeedService
import ru.yandex.direct.core.entity.feed.service.FeedUsageService
import ru.yandex.direct.dbschema.ppc.enums.CampaignsType
import ru.yandex.direct.ess.logicobjects.feeds.usagetypes.FeedUsageTypesObject
import ru.yandex.direct.logicprocessor.common.EssLogicProcessorContext
import java.util.EnumSet

internal class RecalculateFeedUsageTypeProcessorGetFeedsToNewUsageTest {
    companion object {
        private const val FEED_ID_1 = 1L
        private const val FEED_ID_2 = 2L
        private const val CAMPAIGN_ID_1 = 3L
        private const val AD_GROUP_ID_1_FROM_CAMPAIGN_1 = 4L
        private const val AD_GROUP_ID_1_FROM_CAMPAIGN_2 = 5L
        private const val SHARD = 6
    }

    private lateinit var feedUsageTypesObjects: List<FeedUsageTypesObject>
    private val feedRepository = mock<FeedRepository>()
    private val feedService = mock<FeedService>()
    private val feedUsageService = mock<FeedUsageService>()
    private val essLogicProcessorContext = mock<EssLogicProcessorContext>()
    private val recalculateFeedUsageTypeProcessor = RecalculateFeedUsageTypeProcessor(
        feedRepository, feedService, feedUsageService, essLogicProcessorContext
    ).withShard(SHARD) as RecalculateFeedUsageTypeProcessor

    @BeforeEach
    fun init() {
        MockitoAnnotations.openMocks(this)
        mockGetFeedIdsByAdGroupIds(emptyMap())
        mockGetFeedIdsByCampaignIds(emptyMap())
        mockGetFeedsSimple(emptyList())
    }

    @Test
    fun changeWithFeedIdInsertOnUnusedFeed() {
        mockGetFeedsSimple(
            listOf(
                Feed()
                    .withId(FEED_ID_1)
                    .withUsageTypes(EnumSet.noneOf(FeedUsageType::class.java))
            )
        )
        feedUsageTypesObjects = listOf(
            FeedUsageTypesObject(
                feedId = FEED_ID_1,
                campaignsType = CampaignsType.dynamic,
                campaign_id = null,
                adgroup_id = null,
                operation = Operation.INSERT
            )
        )

        check(listOf(FEED_ID_1))
    }

    @Test
    fun changeWithFeedIdInsertOnUsedFeed() {
        mockGetFeedsSimple(
            listOf(
                Feed()
                    .withId(FEED_ID_1)
                    .withUsageTypes(EnumSet.of(FeedUsageType.GOODS_ADS))
            )
        )
        feedUsageTypesObjects = listOf(
            FeedUsageTypesObject(
                feedId = FEED_ID_1,
                campaignsType = CampaignsType.text,
                campaign_id = null,
                adgroup_id = null,
                operation = Operation.INSERT
            )
        )
        check(emptyList())
    }

    @Test
    fun changeWithCampaignIdUpdateOnUsedFeed() {
        mockGetFeedsSimple(
            listOf(
                Feed()
                    .withId(FEED_ID_1)
                    .withUsageTypes(EnumSet.of(FeedUsageType.GOODS_ADS))
            )
        )
        mockGetFeedIdsByCampaignIds(
            mapOf(CAMPAIGN_ID_1 to setOf(FEED_ID_1))
        )
        feedUsageTypesObjects = listOf(
            FeedUsageTypesObject(
                feedId = null,
                campaignsType = null,
                campaign_id = CAMPAIGN_ID_1,
                adgroup_id = null,
                operation = Operation.UPDATE
            )
        )

        check(listOf(FEED_ID_1))
    }

    @Test
    fun changeWithCampaignIdUpdateOnUnusedFeed() {
        mockGetFeedsSimple(
            listOf(
                Feed()
                    .withId(FEED_ID_1)
                    .withUsageTypes(EnumSet.noneOf(FeedUsageType::class.java))
            )
        )
        mockGetFeedIdsByCampaignIds(
            mapOf(CAMPAIGN_ID_1 to setOf(FEED_ID_1))
        )
        feedUsageTypesObjects = listOf(
            FeedUsageTypesObject(
                feedId = null,
                campaignsType = null,
                campaign_id = CAMPAIGN_ID_1,
                adgroup_id = null,
                operation = Operation.UPDATE
            )
        )

        check(listOf(FEED_ID_1))
    }

    @Test
    fun changeWithAdGroupIdUpdateOnUsedFeed() {
        mockGetFeedsSimple(
            listOf(
                Feed()
                    .withId(FEED_ID_1)
                    .withUsageTypes(EnumSet.of(FeedUsageType.GOODS_ADS))
            )
        )
        mockGetFeedIdsByAdGroupIds(
            mapOf(AD_GROUP_ID_1_FROM_CAMPAIGN_1 to FEED_ID_1)
        )
        feedUsageTypesObjects = listOf(
            FeedUsageTypesObject(
                feedId = null,
                campaignsType = null,
                campaign_id = null,
                adgroup_id = AD_GROUP_ID_1_FROM_CAMPAIGN_1,
                operation = Operation.UPDATE
            )
        )

        check(listOf(FEED_ID_1))
    }

    @Test
    fun changeWithAdGroupIdUpdateOnUnusedFeed() {
        mockGetFeedsSimple(
            listOf(
                Feed()
                    .withId(FEED_ID_1)
                    .withUsageTypes(EnumSet.noneOf(FeedUsageType::class.java))
            )
        )
        mockGetFeedIdsByAdGroupIds(
            mapOf(AD_GROUP_ID_1_FROM_CAMPAIGN_1 to FEED_ID_1)
        )
        feedUsageTypesObjects = listOf(
            FeedUsageTypesObject(
                feedId = null,
                campaignsType = null,
                campaign_id = null,
                adgroup_id = AD_GROUP_ID_1_FROM_CAMPAIGN_1,
                operation = Operation.UPDATE
            )
        )

        check(listOf(FEED_ID_1))
    }

    @Test
    fun changeWithAdGroupIdDeleteOnUsedFeed() {
        mockGetFeedsSimple(
            listOf(
                Feed()
                    .withId(FEED_ID_1)
                    .withUsageTypes(EnumSet.of(FeedUsageType.GOODS_ADS))
            )
        )
        mockGetFeedIdsByAdGroupIds(
            mapOf(AD_GROUP_ID_1_FROM_CAMPAIGN_1 to FEED_ID_1)
        )
        feedUsageTypesObjects = listOf(
            FeedUsageTypesObject(
                feedId = null,
                campaignsType = null,
                campaign_id = null,
                adgroup_id = AD_GROUP_ID_1_FROM_CAMPAIGN_1,
                operation = Operation.DELETE
            )
        )

        check(listOf(FEED_ID_1))
    }

    @Test
    fun changeWithAdGroupIdDeleteOnUnusedFeed() {
        mockGetFeedsSimple(
            listOf(
                Feed()
                    .withId(FEED_ID_1)
                    .withUsageTypes(EnumSet.noneOf(FeedUsageType::class.java))
            )
        )
        mockGetFeedIdsByAdGroupIds(
            mapOf(AD_GROUP_ID_1_FROM_CAMPAIGN_1 to FEED_ID_1)
        )
        feedUsageTypesObjects = listOf(
            FeedUsageTypesObject(
                feedId = null,
                campaignsType = null,
                campaign_id = null,
                adgroup_id = AD_GROUP_ID_1_FROM_CAMPAIGN_1,
                operation = Operation.DELETE
            )
        )

        check(emptyList())
    }

    @Test
    fun complexChangeOnSeveralFeeds() {
        mockGetFeedsSimple(
            listOf(
                Feed()
                    .withId(FEED_ID_1)
                    .withUsageTypes(EnumSet.noneOf(FeedUsageType::class.java)),
                Feed()
                    .withId(FEED_ID_2)
                    .withUsageTypes(EnumSet.of(FeedUsageType.GOODS_ADS))
            )
        )
        mockGetFeedIdsByAdGroupIds(
            mapOf(
                AD_GROUP_ID_1_FROM_CAMPAIGN_1 to FEED_ID_1,
                AD_GROUP_ID_1_FROM_CAMPAIGN_2 to FEED_ID_1
            )
        )
        mockGetFeedIdsByCampaignIds(mapOf(CAMPAIGN_ID_1 to setOf(FEED_ID_2)))

        feedUsageTypesObjects = listOf(
            FeedUsageTypesObject(
                feedId = null,
                campaignsType = null,
                campaign_id = null,
                adgroup_id = AD_GROUP_ID_1_FROM_CAMPAIGN_1,
                operation = Operation.DELETE
            ),
            FeedUsageTypesObject(
                feedId = null,
                campaignsType = null,
                campaign_id = null,
                adgroup_id = AD_GROUP_ID_1_FROM_CAMPAIGN_2,
                operation = Operation.DELETE
            ),
            FeedUsageTypesObject(
                feedId = null,
                campaignsType = null,
                campaign_id = CAMPAIGN_ID_1,
                adgroup_id = null,
                operation = Operation.UPDATE
            ),
            FeedUsageTypesObject(
                feedId = FEED_ID_2,
                campaignsType = null,
                campaign_id = null,
                adgroup_id = null,
                operation = Operation.INSERT
            ),
        )

        check(listOf(FEED_ID_2))
    }

    private fun check(expectedFeedIds: List<Long>) {
        recalculateFeedUsageTypeProcessor.getFeedsToNewUsage(feedUsageTypesObjects)
        verify(feedUsageService).getActualFeedUsageTypeByFeedId(eq(SHARD), eq(expectedFeedIds))
    }

    private fun mockGetFeedIdsByCampaignIds(result: Map<Long, Set<Long>>) =
        `when`(feedService.getFeedIdsByCampaignIds(eq(SHARD), any())).thenReturn(result)

    private fun mockGetFeedIdsByAdGroupIds(result: Map<Long, Long>) =
        `when`(feedService.getFeedIdsByAdGroupIds(eq(SHARD), any())).thenReturn(result)

    private fun mockGetFeedsSimple(result: List<FeedSimple>) =
        `when`(feedService.getFeedsSimple(eq(SHARD), any())).thenReturn(result)
}
