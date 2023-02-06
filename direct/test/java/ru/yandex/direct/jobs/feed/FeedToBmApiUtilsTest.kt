package ru.yandex.direct.jobs.feed

import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies
import ru.yandex.direct.asynchttp.Result
import ru.yandex.direct.bmapi.client.model.BmApiError
import ru.yandex.direct.bmapi.client.model.BmApiFeedInfoResponse
import ru.yandex.direct.bmapi.client.model.BmApiWarning
import ru.yandex.direct.bmapi.client.model.Category
import ru.yandex.direct.core.entity.feed.model.Feed
import ru.yandex.direct.core.entity.feed.model.FeedOfferExamples
import ru.yandex.direct.core.entity.feed.model.FeedSimple
import ru.yandex.direct.core.entity.feed.model.FeedType
import ru.yandex.direct.core.entity.feed.model.UpdateStatus
import ru.yandex.direct.core.entity.feed.repository.FeedRepository
import ru.yandex.direct.core.entity.feed.service.FeedService
import ru.yandex.direct.core.testing.data.TestFeeds.defaultFeed
import ru.yandex.direct.core.testing.info.FeedInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.stub.BmapiClientStub
import ru.yandex.direct.jobs.configuration.JobsTest
import ru.yandex.direct.jobs.feed.service.SendFeedsToBannerLandJobService
import ru.yandex.direct.test.utils.assertj.Conditions.matchedBy

@JobsTest
@ExtendWith(SpringExtension::class)
class FeedToBmApiUtilsTest {
    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var feedService: FeedService

    @Autowired
    private lateinit var feedRepository: FeedRepository

    private lateinit var bmapiClientStub: BmapiClientStub
    private lateinit var sendFeedsToBannerLandJobService: SendFeedsToBannerLandJobService
    private lateinit var feedInfo: FeedInfo
    private lateinit var anotherFeedInfo: FeedInfo

    @BeforeEach
    fun initData() {
        bmapiClientStub = BmapiClientStub()
        feedInfo =
            steps.feedSteps().createFeed(
                FeedInfo()
                    .withFeed(
                        defaultFeed()
                            .withUpdateStatus(UpdateStatus.NEW)
                            .withUrl("https://somedomain.com/kindafeed.xml")
                    )
            )
        anotherFeedInfo =
            steps.feedSteps().createFeed(
                FeedInfo()
                    .withClientInfo(feedInfo.clientInfo)
                    .withFeed(
                        defaultFeed()
                            .withUpdateStatus(UpdateStatus.NEW)
                            .withUrl("https://anotherdomain.com/kindafeed.xml")
                    )
            )
        bmapiClientStub.resultsByFeedUrl["https://somedomain.com/kindafeed.xml"] = getResult(
            "somedomain.com",
            "someVendor",
            "12",
            "someCategory",
            500
        )
        bmapiClientStub.resultsByFeedUrl["https://anotherdomain.com/kindafeed.xml"] = getResult(
            "anotherdomain.com",
            "anotherVendor",
            "13",
            "anotherCategory",
            400
        )
        sendFeedsToBannerLandJobService = SendFeedsToBannerLandJobService(
            feedService, feedRepository, bmapiClientStub, LoggerFactory.getLogger("TEST")
        )
    }

    @Test
    fun check() {
        sendFeedsToBannerLandJobService.sendFeedsToBmApi(
            feedInfo.shard,
            listOf(anotherFeedInfo.feed as FeedSimple, feedInfo.feed as FeedSimple),
            mapOf(feedInfo.clientId.asLong() to 1000000)
        )
        val actualFeeds = feedService.getFeeds(feedInfo.clientId, listOf(feedInfo.feedId, anotherFeedInfo.feedId))
        val firstActualFeed = actualFeeds.find { feed -> feed.id == feedInfo.feedId }
        val anotherActualFeed = actualFeeds.find { feed -> feed.id == anotherFeedInfo.feedId }
        val expectedFeed = Feed()
            .withId(feedInfo.feedId)
            .withFeedType(FeedType.YANDEX_MARKET)
            .withOffersCount(500)
            .withTargetDomain("somedomain.com")
            .withUpdateStatus(UpdateStatus.DONE)
        val anotherExpectedFeed = Feed()
            .withId(anotherFeedInfo.feedId)
            .withFeedType(FeedType.YANDEX_MARKET)
            .withOffersCount(400)
            .withTargetDomain("anotherdomain.com")
            .withUpdateStatus(UpdateStatus.DONE)
        SoftAssertions.assertSoftly {
            it.assertThat(actualFeeds).hasSize(2)
            it.assertThat(firstActualFeed).isNotNull
            it.assertThat(anotherActualFeed).isNotNull
            it.assertThat(firstActualFeed)
                .`is`(
                    matchedBy(
                        beanDiffer(expectedFeed)
                            .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())
                    )
                )
            it.assertThat(anotherActualFeed)
                .`is`(
                    matchedBy(
                        beanDiffer(anotherExpectedFeed)
                            .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())
                    )
                )

        }
    }

    private fun getResult(
        domain: String,
        vendor: String,
        categoryId: String,
        categoryName: String,
        offersCount: Int
    ): Result<BmApiFeedInfoResponse> {
        val result: Result<BmApiFeedInfoResponse> = Result(0)
        val categoryIdsToOffersCount: Map<String, Int> = mapOf(categoryId to offersCount)
        val warnings: List<BmApiWarning>? = null
        val errors: List<BmApiError>? = null
        val feedType: String = FeedType.YANDEX_MARKET.typedValue
        val totalOffersAmount: Long = offersCount.toLong()
        val domainToOffersCount: Map<String, Int> = mapOf(domain to offersCount)
        val categories: List<Category> = listOf(Category(categoryId, null, categoryName))
        val vendorsToOffersCount: Map<String, Int> = mapOf(vendor to offersCount)
        val feedOfferExamples: FeedOfferExamples? = null

        result.success = BmApiFeedInfoResponse(
            categoryIdsToOffersCount,
            warnings,
            errors,
            feedType,
            totalOffersAmount,
            domainToOffersCount,
            categories,
            vendorsToOffersCount,
            feedOfferExamples
        )
        return result
    }
}
