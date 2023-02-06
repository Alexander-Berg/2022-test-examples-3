package ru.yandex.direct.core.entity.feed.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies
import ru.yandex.direct.core.entity.feed.model.BusinessType
import ru.yandex.direct.core.entity.feed.model.Feed
import ru.yandex.direct.core.entity.feed.model.FeedCategory
import ru.yandex.direct.core.entity.feed.model.FeedHistoryItem
import ru.yandex.direct.core.entity.feed.model.FeedVendor
import ru.yandex.direct.core.entity.feed.model.MasterSystem
import ru.yandex.direct.core.entity.feed.model.Source
import ru.yandex.direct.core.entity.feed.model.UpdateStatus
import ru.yandex.direct.core.entity.feed.repository.FeedRepository
import ru.yandex.direct.core.entity.feed.repository.FeedSupplementaryDataRepository
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.CATEGORY_1
import ru.yandex.direct.core.testing.data.CATEGORY_2
import ru.yandex.direct.core.testing.data.CATEGORY_3
import ru.yandex.direct.core.testing.data.CATEGORY_ID_1
import ru.yandex.direct.core.testing.data.CATEGORY_ID_2
import ru.yandex.direct.core.testing.data.CATEGORY_ID_3
import ru.yandex.direct.core.testing.data.FEED_TYPE
import ru.yandex.direct.core.testing.data.INITIAL_OFFERS_AMOUNT
import ru.yandex.direct.core.testing.data.INITIAL_RESPONSE
import ru.yandex.direct.core.testing.data.INITIAl_DOMAIN
import ru.yandex.direct.core.testing.data.NEW_DOMAIN
import ru.yandex.direct.core.testing.data.OFFERS_EXAMPLES
import ru.yandex.direct.core.testing.data.PARENT_CATEGORY
import ru.yandex.direct.core.testing.data.PARENT_CATEGORY_ID
import ru.yandex.direct.core.testing.data.SECONDARY_OFFERS_AMOUNT
import ru.yandex.direct.core.testing.data.SECOND_RESPONSE
import ru.yandex.direct.core.testing.data.TestFeeds.defaultFeed
import ru.yandex.direct.core.testing.data.VENDOR_1
import ru.yandex.direct.core.testing.data.VENDOR_2
import ru.yandex.direct.core.testing.data.VENDOR_3
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.FeedInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.test.utils.assertj.Conditions
import java.math.BigInteger

@CoreTest
@RunWith(SpringJUnit4ClassRunner::class)
class FeedUpdateFromBmapiSuccessTest {
    companion object {
        @JvmStatic
        var needToInit = true

        @JvmStatic
        private lateinit var feedInfo: FeedInfo

        @JvmStatic
        private lateinit var clientInfo: ClientInfo

        @JvmStatic
        private lateinit var actualFeedVendorsFirstCall: List<FeedVendor>

        @JvmStatic
        private lateinit var actualFeedHistoryFirstCall: FeedHistoryItem

        @JvmStatic
        private lateinit var actualFeedCategoriesFirstCall: List<FeedCategory>

        @JvmStatic
        private lateinit var actualFeedFirstCall: Feed

        @JvmStatic
        private lateinit var actualFeedVendorsSecondCall: List<FeedVendor>

        @JvmStatic
        private lateinit var actualFeedHistorySecondCall: FeedHistoryItem

        @JvmStatic
        private lateinit var actualFeedCategoriesSecondCall: List<FeedCategory>

        @JvmStatic
        private lateinit var actualFeedSecondCall: Feed
    }

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
        if (needToInit) {
            clientInfo = steps.clientSteps().createDefaultClient()
            feedInfo = FeedInfo().withClientInfo(clientInfo)
                .withFeed(
                    defaultFeed(clientInfo.clientId)
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
            feedService.saveFeedFromBmApiResponse(clientInfo.shard, mapOf(feedInfo.feedId to INITIAL_RESPONSE))
            actualFeedFirstCall = feedRepository.get(clientInfo.shard, listOf(feedInfo.feedId))[0]
            actualFeedCategoriesFirstCall =
                feedSupplementaryDataRepository.getFeedCategories(clientInfo.shard, listOf(feedInfo.feedId))
            actualFeedHistoryFirstCall =
                feedSupplementaryDataRepository.getLatestFeedHistoryItems(
                    clientInfo.shard,
                    listOf(feedInfo.feedId)
                )[feedInfo.feedId]!!
            actualFeedVendorsFirstCall =
                feedSupplementaryDataRepository.getFeedVendorsByFeedId(
                    clientInfo.shard,
                    listOf(feedInfo.feedId)
                )[feedInfo.feedId]!!

            feedInfo.feed.updateStatus = UpdateStatus.DONE
            steps.feedSteps().setFeedProperty(feedInfo, Feed.UPDATE_STATUS, UpdateStatus.UPDATING)

            feedService.saveFeedFromBmApiResponse(clientInfo.shard, mapOf(feedInfo.feedId to SECOND_RESPONSE))

            actualFeedSecondCall = feedRepository.get(clientInfo.shard, listOf(feedInfo.feedId))[0]
            actualFeedCategoriesSecondCall =
                feedSupplementaryDataRepository.getFeedCategories(clientInfo.shard, listOf(feedInfo.feedId))
            actualFeedVendorsSecondCall =
                feedSupplementaryDataRepository.getFeedVendorsByFeedId(
                    clientInfo.shard,
                    listOf(feedInfo.feedId)
                )[feedInfo.feedId]!!
            actualFeedHistorySecondCall =
                feedSupplementaryDataRepository.getLatestFeedHistoryItems(
                    clientInfo.shard,
                    listOf(feedInfo.feedId)
                )[feedInfo.feedId]!!
            needToInit = false
        }
    }

    @Test
    fun checkFeedSuccessFirstResponse_feed() {
        val expectedFeed = feedInfo.feed
            .withFetchErrorsCount(0)
            .withTargetDomain(INITIAl_DOMAIN)
            .withUpdateStatus(UpdateStatus.DONE)
            .withOffersCount(INITIAL_OFFERS_AMOUNT.toLong())
            .withOfferExamples(OFFERS_EXAMPLES)
        assertThat(actualFeedFirstCall).`is`(
            Conditions.matchedBy(
                beanDiffer(expectedFeed)
                    .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())
            )
        )
    }

    @Test
    fun checkFeedSuccessFirstResponse_categories() {
        val expectedFeedCategories = listOf(
            FeedCategory()
                .withCategoryId(BigInteger(PARENT_CATEGORY_ID))
                .withFeedId(feedInfo.feedId)
                .withName(PARENT_CATEGORY)
                .withIsDeleted(false)
                .withOfferCount(null)
                .withParentCategoryId(BigInteger.ZERO),
            FeedCategory()
                .withCategoryId(BigInteger(CATEGORY_ID_1))
                .withFeedId(feedInfo.feedId)
                .withName(CATEGORY_1)
                .withIsDeleted(false)
                .withOfferCount((INITIAL_OFFERS_AMOUNT - 1).toLong())
                .withParentCategoryId(BigInteger(PARENT_CATEGORY_ID)),
            FeedCategory()
                .withCategoryId(BigInteger(CATEGORY_ID_2))
                .withFeedId(feedInfo.feedId)
                .withName(CATEGORY_2)
                .withIsDeleted(false)
                .withOfferCount(1)
                .withParentCategoryId(BigInteger(CATEGORY_ID_1)),
        )
        assertThat(actualFeedCategoriesFirstCall).`is`(
            Conditions.matchedBy(
                beanDiffer(expectedFeedCategories)
                    .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())
            )
        )
    }

    @Test
    fun checkFeedSuccessFirstResponse_vendors() {
        val expectedFeedVendors = listOf(
            FeedVendor()
                .withFeedId(feedInfo.feedId)
                .withName(VENDOR_1),
            FeedVendor()
                .withFeedId(feedInfo.feedId)
                .withName(VENDOR_2),
        )
        assertThat(actualFeedVendorsFirstCall).`is`(
            Conditions.matchedBy(
                beanDiffer(expectedFeedVendors)
                    .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())
            )
        )
    }

    @Test
    fun checkFeedSuccessFirstResponse_history() {
        val expectedFeedHistory = FeedHistoryItem()
            .withFeedId(feedInfo.feedId)
            .withOfferCount(INITIAL_OFFERS_AMOUNT.toLong())
        assertThat(actualFeedHistoryFirstCall).`is`(
            Conditions.matchedBy(
                beanDiffer(expectedFeedHistory)
                    .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())
            )
        )
    }

    @Test
    fun checkFeedSuccessSecondResponse_feed() {
        val expectedFeed = feedInfo.feed
            .withFetchErrorsCount(0)
            .withTargetDomain(NEW_DOMAIN)
            .withUpdateStatus(UpdateStatus.DONE)
            .withOffersCount(SECONDARY_OFFERS_AMOUNT.toLong())
            .withOfferExamples(OFFERS_EXAMPLES)
        assertThat(actualFeedSecondCall).`is`(
            Conditions.matchedBy(
                beanDiffer(expectedFeed)
                    .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())
            )
        )
    }

    @Test
    fun checkFeedSuccessSecondResponse_categories() {
        val expectedFeedCategories = listOf(
            FeedCategory()
                .withCategoryId(BigInteger(PARENT_CATEGORY_ID))
                .withFeedId(feedInfo.feedId)
                .withName(PARENT_CATEGORY)
                .withIsDeleted(false)
                .withOfferCount(null)
                .withParentCategoryId(BigInteger.ZERO),
            FeedCategory()
                .withCategoryId(BigInteger(CATEGORY_ID_1))
                .withFeedId(feedInfo.feedId)
                .withName(CATEGORY_1)
                .withIsDeleted(true)
                .withOfferCount((INITIAL_OFFERS_AMOUNT - 1).toLong())
                .withParentCategoryId(BigInteger(PARENT_CATEGORY_ID)),
            FeedCategory()
                .withCategoryId(BigInteger(CATEGORY_ID_2))
                .withFeedId(feedInfo.feedId)
                .withName(CATEGORY_2)
                .withIsDeleted(false)
                .withOfferCount(1)
                .withParentCategoryId(BigInteger(PARENT_CATEGORY_ID)),
            FeedCategory()
                .withCategoryId(BigInteger(CATEGORY_ID_3))
                .withFeedId(feedInfo.feedId)
                .withName(CATEGORY_3)
                .withIsDeleted(false)
                .withOfferCount((SECONDARY_OFFERS_AMOUNT - 1).toLong())
                .withParentCategoryId(BigInteger(PARENT_CATEGORY_ID)),
        )
        assertThat(actualFeedCategoriesSecondCall).`is`(
            Conditions.matchedBy(
                beanDiffer(expectedFeedCategories)
                    .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())
            )
        )
    }

    @Test
    fun checkFeedSuccessSecondResponse_vendors() {
        val expectedFeedVendors = listOf(
            FeedVendor()
                .withFeedId(feedInfo.feedId)
                .withName(VENDOR_3),
        )
        assertThat(actualFeedVendorsSecondCall).`is`(
            Conditions.matchedBy(
                beanDiffer(expectedFeedVendors)
                    .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())
            )
        )
    }

    @Test
    fun checkFeedSuccessSecondResponse_history() {
        val expectedFeedHistory = FeedHistoryItem()
            .withFeedId(feedInfo.feedId)
            .withOfferCount(SECONDARY_OFFERS_AMOUNT.toLong())
        assertThat(actualFeedHistorySecondCall).`is`(
            Conditions.matchedBy(
                beanDiffer(expectedFeedHistory)
                    .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())
            )
        )
    }
}
