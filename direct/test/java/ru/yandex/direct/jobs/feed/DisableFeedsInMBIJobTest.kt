package ru.yandex.direct.jobs.feed

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.doThrow
import com.nhaarman.mockitokotlin2.whenever
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit.SECONDS
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.MockitoAnnotations
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.common.testing.softly
import ru.yandex.direct.core.entity.feed.model.Feed
import ru.yandex.direct.core.entity.feed.repository.FeedRepository
import ru.yandex.direct.core.entity.feed.service.MbiService
import ru.yandex.direct.core.testing.info.FeedInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.jobs.configuration.JobsTest
import ru.yandex.direct.market.client.MarketClient
import ru.yandex.direct.market.client.exception.MarketClientException

@JobsTest
@ExtendWith(SpringExtension::class)
internal class DisableFeedsInMBIJobTest {
    private lateinit var mocks: AutoCloseable

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var feedRepository: FeedRepository

    @Autowired
    private lateinit var marketClient: MarketClient

    @Autowired
    private lateinit var job: DisableFeedsInMBIJob

    private lateinit var feedInfo: FeedInfo
    private lateinit var now: LocalDateTime
    private lateinit var lastUsed: LocalDateTime

    @BeforeEach
    fun init() {
        mocks = MockitoAnnotations.openMocks(this)

        val clientInfo = steps.clientSteps().createDefaultClient()
        job.withShard(clientInfo.shard)
        feedInfo = steps.feedSteps().createDefaultSyncedFeed(clientInfo)
        now = Instant.now().atZone(ZoneId.systemDefault()).toLocalDateTime().truncatedTo(SECONDS)
        lastUsed = now.minus(MbiService.FEED_KEEP_MBI_ENABLED_DURATION).truncatedTo(SECONDS)
        steps.feedSteps().setFeedProperty(feedInfo, Feed.LAST_USED, lastUsed)
    }

    @AfterEach
    fun releaseMocks() {
        mocks.close()
    }

    @Test
    fun checkJob_withOutMarketExceptions() {
        var feeds = feedRepository.getSimple(feedInfo.shard, listOf(feedInfo.feedId))
        job.disableFeeds(feeds, now)

        feeds = feedRepository.getSimple(feedInfo.shard, listOf(feedInfo.feedId))

        softly {
            assertThat(feeds.size).isEqualTo(1)
            assertThat(feeds.first()).isNotNull
            assertThat(feeds.first().lastUsed).isEqualTo(lastUsed)
        }
    }

    @Test
    fun checkJob_withMarketExceptions() {
        doThrow(MarketClientException("Error")).whenever(marketClient).sendUrlFeedToMbi(any(), anyOrNull())

        var feeds = feedRepository.getSimple(feedInfo.shard, listOf(feedInfo.feedId))
        job.disableFeeds(feeds, now)

        feeds = feedRepository.getSimple(feedInfo.shard, listOf(feedInfo.feedId))

        softly {
            assertThat(feeds.size).isEqualTo(1)
            assertThat(feeds.first()).isNotNull
            assertThat(feeds.first().lastUsed).isEqualTo(now.plusSeconds(1L))
        }
    }

    @Test
    fun checkJob_freshFeedLastUsedNotUpdated() {
        lastUsed = now.minus(MbiService.FEED_KEEP_MBI_ENABLED_DURATION).plusSeconds(1L).truncatedTo(SECONDS)
        steps.feedSteps().setFeedProperty(feedInfo, Feed.LAST_USED, lastUsed)

        var feeds = feedRepository.getSimple(feedInfo.shard, listOf(feedInfo.feedId))
        job.disableFeeds(feeds, now)

        feeds = feedRepository.getSimple(feedInfo.shard, listOf(feedInfo.feedId))

        softly {
            assertThat(feeds.size).isEqualTo(1)
            assertThat(feeds.first()).isNotNull
            assertThat(feeds.first().lastUsed).isEqualTo(lastUsed)
        }
    }

    @Test
    fun checkJob_ancientFeedLastUsedNotUpdated() {
        lastUsed = LocalDate.EPOCH.atStartOfDay().minus(MbiService.FEED_KEEP_MBI_ENABLED_DURATION).minusSeconds(1L)
        steps.feedSteps().setFeedProperty(feedInfo, Feed.LAST_USED, lastUsed)

        var feeds = feedRepository.getSimple(feedInfo.shard, listOf(feedInfo.feedId))
        job.disableFeeds(feeds, now)

        feeds = feedRepository.getSimple(feedInfo.shard, listOf(feedInfo.feedId))

        softly {
            assertThat(feeds.size).isEqualTo(1)
            assertThat(feeds.first()).isNotNull
            assertThat(feeds.first().lastUsed).isEqualTo(lastUsed)
        }
    }
}
