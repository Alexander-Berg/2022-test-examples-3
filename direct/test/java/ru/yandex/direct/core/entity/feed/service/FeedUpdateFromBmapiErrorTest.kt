package ru.yandex.direct.core.entity.feed.service

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies
import ru.yandex.direct.bmapi.client.model.BmApiErrorCode
import ru.yandex.direct.common.testing.softly
import ru.yandex.direct.core.entity.feed.model.BusinessType
import ru.yandex.direct.core.entity.feed.model.Feed
import ru.yandex.direct.core.entity.feed.model.FeedHistoryItem
import ru.yandex.direct.core.entity.feed.model.FeedHistoryItemParseResults
import ru.yandex.direct.core.entity.feed.model.FeedHistoryItemParseResultsDefect
import ru.yandex.direct.core.entity.feed.model.MasterSystem
import ru.yandex.direct.core.entity.feed.model.Source
import ru.yandex.direct.core.entity.feed.model.UpdateStatus
import ru.yandex.direct.core.entity.feed.repository.FeedRepository
import ru.yandex.direct.core.entity.feed.repository.FeedSupplementaryDataRepository
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.ANOTHER_TYPE_RESPONSE
import ru.yandex.direct.core.testing.data.BMAPI_ERROR_CODE
import ru.yandex.direct.core.testing.data.BMAPI_ERROR_MESSAGE
import ru.yandex.direct.core.testing.data.ERROR_RESPONSE
import ru.yandex.direct.core.testing.data.FEED_TYPE
import ru.yandex.direct.core.testing.data.INITIAL_RESPONSE
import ru.yandex.direct.core.testing.data.TestFeeds
import ru.yandex.direct.core.testing.data.UNKNOWN_TYPE_RESPONSE
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.FeedInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.test.utils.assertj.Conditions

@CoreTest
@RunWith(SpringJUnit4ClassRunner::class)
class FeedUpdateFromBmapiErrorTest {
    private lateinit var feedInfo: FeedInfo
    private lateinit var clientInfo: ClientInfo

    @Autowired
    private lateinit var feedRepository: FeedRepository

    @Autowired
    private lateinit var feedSupplementaryDataRepository: FeedSupplementaryDataRepository

    @Autowired
    private lateinit var feedService: FeedService

    @Autowired
    private lateinit var steps: Steps

    @Before
    fun init() {
        clientInfo = steps.clientSteps().createDefaultClient()
        feedInfo = FeedInfo().withClientInfo(clientInfo)
            .withFeed(
                TestFeeds.defaultFeed(clientInfo.clientId)
                    .withSource(Source.URL)
                    .withMasterSystem(MasterSystem.DIRECT)
                    .withName("Маркет")
                    .withUrl("https://somedomain.com/kindafeed.xml")
                    .withFeedType(FEED_TYPE)
                    .withBusinessType(BusinessType.RETAIL)
                    .withClientId(clientInfo.clientId!!.asLong())
                    .withIsRemoveUtm(false)
                    .withUpdateStatus(UpdateStatus.UPDATING)
            )
        feedInfo = steps.feedSteps().createFeed(feedInfo)
    }

    @Test
    fun checkFeedErrorResponse() {
        feedService.saveFeedFromBmApiResponse(clientInfo.shard, mapOf(feedInfo.feedId to INITIAL_RESPONSE))
        feedInfo.feed.updateStatus = UpdateStatus.DONE
        steps.feedSteps().setFeedProperty(feedInfo, Feed.UPDATE_STATUS, UpdateStatus.UPDATING)
        feedService.saveFeedFromBmApiResponse(clientInfo.shard, mapOf(feedInfo.feedId to ERROR_RESPONSE))

        val expectedFeed = feedInfo.feed
            .withFetchErrorsCount(1)
            .withUpdateStatus(UpdateStatus.ERROR)
        val expectedFeedHistory = FeedHistoryItem()
            .withFeedId(feedInfo.feedId)
            .withParseResults(
                FeedHistoryItemParseResults()
                    .withErrors(
                        listOf(
                            FeedHistoryItemParseResultsDefect()
                                .withCode(BMAPI_ERROR_CODE.code.toLong())
                                .withMessageEn(BMAPI_ERROR_MESSAGE)
                        )
                    )
            )
        val actualFeed = feedRepository.get(
            clientInfo.shard, listOf(
                feedInfo.feedId
            )
        )[0]
        val actualFeedHistory =
            feedSupplementaryDataRepository.getLatestFeedHistoryItems(
                clientInfo.shard,
                listOf(feedInfo.feedId)
            )[feedInfo.feedId]!!

        softly {
            assertThat(actualFeed).`is`(
                Conditions.matchedBy(
                    BeanDifferMatcher.beanDiffer(expectedFeed)
                        .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())
                )
            )
            assertThat(actualFeedHistory).`is`(
                Conditions.matchedBy(
                    BeanDifferMatcher.beanDiffer(expectedFeedHistory)
                        .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())
                )
            )
        }
    }

    @Test
    fun checkFeedUnknownTypeResponse() {
        feedService.saveFeedFromBmApiResponse(clientInfo.shard, mapOf(feedInfo.feedId to UNKNOWN_TYPE_RESPONSE))

        val expectedFeed = feedInfo.feed
            .withFetchErrorsCount(1)
            .withUpdateStatus(UpdateStatus.ERROR)
        val expectedFeedHistory = FeedHistoryItem()
            .withFeedId(feedInfo.feedId)
            .withParseResults(
                FeedHistoryItemParseResults()
                    .withErrors(
                        listOf(
                            FeedHistoryItemParseResultsDefect()
                                .withMessageEn(BmApiErrorCode.FEED_TYPE_ERROR_MESSAGE)
                                .withCode(BmApiErrorCode.BL_ERROR_FEED_TYPE_MISMATCH.code.toLong())
                        )
                    )
            )
        val actualFeed = feedRepository.get(
            clientInfo.shard, listOf(
                feedInfo.feedId
            )
        )[0]
        val actualFeedHistory =
            feedSupplementaryDataRepository.getLatestFeedHistoryItems(
                clientInfo.shard,
                listOf(feedInfo.feedId)
            )[feedInfo.feedId]!!

        softly {
            assertThat(actualFeed).`is`(
                Conditions.matchedBy(
                    BeanDifferMatcher.beanDiffer(expectedFeed)
                        .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())
                )
            )
            assertThat(actualFeedHistory).`is`(
                Conditions.matchedBy(
                    BeanDifferMatcher.beanDiffer(expectedFeedHistory)
                        .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())
                )
            )
        }
    }

    @Test
    fun checkFeedChangeTypeResponse() {
        feedService.saveFeedFromBmApiResponse(clientInfo.shard, mapOf(feedInfo.feedId to ANOTHER_TYPE_RESPONSE))

        val expectedFeed = feedInfo.feed
            .withFetchErrorsCount(1)
            .withUpdateStatus(UpdateStatus.ERROR)
        val expectedFeedHistory = FeedHistoryItem()
            .withFeedId(feedInfo.feedId)
            .withParseResults(
                FeedHistoryItemParseResults()
                    .withErrors(
                        listOf(
                            FeedHistoryItemParseResultsDefect()
                                .withMessageEn(BmApiErrorCode.FEED_TYPE_ERROR_MESSAGE)
                                .withCode(BmApiErrorCode.BL_ERROR_FEED_TYPE_MISMATCH.code.toLong())
                        )
                    )
            )
        val actualFeed = feedRepository.get(
            clientInfo.shard, listOf(
                feedInfo.feedId
            )
        )[0]
        val actualFeedHistory =
            feedSupplementaryDataRepository.getLatestFeedHistoryItems(
                clientInfo.shard,
                listOf(feedInfo.feedId)
            )[feedInfo.feedId]!!

        softly {
            assertThat(actualFeed).`is`(
                Conditions.matchedBy(
                    BeanDifferMatcher.beanDiffer(expectedFeed)
                        .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())
                )
            )
            assertThat(actualFeedHistory).`is`(
                Conditions.matchedBy(
                    BeanDifferMatcher.beanDiffer(expectedFeedHistory)
                        .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())
                )
            )
        }
    }
}
