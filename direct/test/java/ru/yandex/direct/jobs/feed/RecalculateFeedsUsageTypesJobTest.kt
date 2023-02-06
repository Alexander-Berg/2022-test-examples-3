package ru.yandex.direct.jobs.feed

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.common.testing.softly
import ru.yandex.direct.core.entity.feed.converter.FeedConverter
import ru.yandex.direct.core.entity.feed.model.FeedSimple
import ru.yandex.direct.core.entity.feed.model.FeedUsageType
import ru.yandex.direct.core.entity.feed.service.FeedService
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.jobs.configuration.JobsTest
import ru.yandex.direct.ytcomponents.service.BlrtDynContextProvider
import ru.yandex.direct.ytwrapper.dynamic.context.YtDynamicContext
import ru.yandex.direct.ytwrapper.dynamic.testutil.RowBuilder.rowBuilder
import ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder
import ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder.rowsetBuilder

@JobsTest
@ExtendWith(SpringExtension::class)
class RecalculateFeedsUsageTypesJobTest {
    @Autowired
    private lateinit var steps: Steps
    @Autowired
    private lateinit var blrtDynContextProvider: BlrtDynContextProvider
    @Autowired
    private lateinit var feedService: FeedService
    @Autowired
    private lateinit var job: RecalculateFeedsUsageTypesJob

    private var feedsInUse: MutableList<FeedSimple> = ArrayList()

    @BeforeEach
    fun setUp() {
        feedsInUse.clear()

        val ytDynamicContext = mock<YtDynamicContext> {
            on { readTable(any(), any()) } doAnswer {
                feedsInUse
                    .asSequence()
                    .mapNotNull(FeedConverter::extractBusinessIdAndShopId)
                    .map {
                        rowBuilder()
                            .withColValue("BusinessID", it.businessId)
                            .withColValue("ShopID", it.shopId)
                    }
                    .fold(rowsetBuilder(), RowsetBuilder::add)
                    .build()
                    .yTreeRows
            }
        }
        doReturn(ytDynamicContext).whenever(blrtDynContextProvider).context
    }

    @Test
    fun test() {
        val clientSteps = steps.clientSteps()
        val feedSteps = steps.feedSteps()

        val unusedFeed = feedSteps.createDefaultFeed()
        val unusedMbiFeed = feedSteps.createDefaultSyncedSiteFeed(clientSteps.createDefaultClient())
        val usedMbiFeed = feedSteps.createDefaultSyncedFeed(clientSteps.createDefaultClient())
        feedsInUse.add(usedMbiFeed.feed)

        job.execute()

        val actualUnusedFeed = feedService.getFeeds(unusedFeed.clientId, listOf(unusedFeed.feedId))[0]
        val actualUnusedMbiFeed = feedService.getFeeds(unusedMbiFeed.clientId, listOf(unusedMbiFeed.feedId))[0]
        val actualUsedMbiFeed = feedService.getFeeds(usedMbiFeed.clientId, listOf(usedMbiFeed.feedId))[0]
        softly {
            assertThat(actualUnusedFeed.usageTypes).isEmpty()
            assertThat(actualUnusedMbiFeed.usageTypes).isEmpty()
            assertThat(actualUsedMbiFeed.usageTypes).containsExactly(FeedUsageType.GOODS_ADS)
        }
    }
}
