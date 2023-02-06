package ru.yandex.direct.grid.processing.service.feed

import graphql.ExecutionResult
import org.assertj.core.api.SoftAssertions
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.feed.model.Feed
import ru.yandex.direct.core.entity.feed.model.UpdateStatus
import ru.yandex.direct.core.entity.user.service.UserService
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.FeedInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.grid.model.Order
import ru.yandex.direct.grid.model.feed.GdSource.FILE
import ru.yandex.direct.grid.model.feed.GdSource.URL
import ru.yandex.direct.grid.model.feed.GdUpdateStatus
import ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext
import ru.yandex.direct.grid.processing.model.GdLimitOffset
import ru.yandex.direct.grid.processing.model.feed.GdFeedOrderByField
import ru.yandex.direct.grid.processing.model.feed.GdFeedsFilter
import ru.yandex.direct.grid.processing.model.feed.GdFeedsOrderBy
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider
import ru.yandex.direct.grid.processing.util.ContextHelper.buildContext
import ru.yandex.direct.grid.processing.util.GraphQLUtils.checkErrors
import ru.yandex.direct.grid.processing.util.GraphQLUtils.getDataValue
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner::class)
class FeedsGraphQlServiceSortAndFilterTest {

    companion object {
        private val FEED_REQUEST = """
            {
              client(searchBy:{
                  id:%s
                  }) {
                    feeds%s{
                      totalCount,
                      rowset{
                        id
                    }
                  }
              }
            }
            """.trimIndent()
    }

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var gridContextProvider: GridContextProvider

    @Autowired
    @Qualifier(GridProcessingConfiguration.GRAPH_QL_PROCESSOR)
    private lateinit var processor: GridGraphQLProcessor

    public lateinit var clientInfo: ClientInfo
    private lateinit var firstUrlFeed: Feed
    private lateinit var secondFileFeed: Feed

    @Before
    fun setUp() {
        clientInfo = steps.clientSteps().createDefaultClient()
        firstUrlFeed = steps.feedSteps().createDefaultFeed(clientInfo).feed
        secondFileFeed = steps.feedSteps().createDefaultFileFeed(clientInfo).feed
    }

    @After
    fun afterTest() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun getFeeds_success_sortByIdAsc() {
        val orderBy = GdFeedsOrderBy().apply { field = GdFeedOrderByField.ID; order = Order.ASC }
        val data = sendRequest(ordersBy = listOf(orderBy))

        val firstFeedId = getDataValue<Long>(data, "client/feeds/rowset/0/id")
        val secondFeedId = getDataValue<Long>(data, "client/feeds/rowset/1/id")

        SoftAssertions.assertSoftly {
            it.assertThat(firstFeedId).`as`("firstFeedId").isEqualTo(firstUrlFeed.id)
            it.assertThat(secondFeedId).`as`("secondFeedId").isEqualTo(secondFileFeed.id)
        }
    }

    @Test
    fun getFeeds_success_sortByIdDesc() {
        val orderBy = GdFeedsOrderBy().apply { field = GdFeedOrderByField.ID; order = Order.DESC }
        val data = sendRequest(ordersBy = listOf(orderBy))

        val firstFeedId = getDataValue<Long>(data, "client/feeds/rowset/0/id")
        val secondFeedId = getDataValue<Long>(data, "client/feeds/rowset/1/id")

        SoftAssertions.assertSoftly {
            it.assertThat(firstFeedId).`as`("firstFeedId").isEqualTo(secondFileFeed.id)
            it.assertThat(secondFeedId).`as`("secondFeedId").isEqualTo(firstUrlFeed.id)
        }
    }

    @Test
    fun getFeeds_success_filterSourceUrl() {
        val filter = GdFeedsFilter().apply { sources = listOf(URL) }
        val data = sendRequest(filter = filter)

        val totalCount = getDataValue<Int>(data, "client/feeds/totalCount")
        val feedId = getDataValue<Long>(data, "client/feeds/rowset/0/id")

        SoftAssertions.assertSoftly {
            it.assertThat(totalCount).`as`("totalCount").isEqualTo(1)
            it.assertThat(feedId).`as`("feedId").isEqualTo(firstUrlFeed.id)
        }
    }

    @Test
    fun getFeeds_success_filterSourceFile() {
        val filter = GdFeedsFilter().apply { sources = listOf(FILE) }
        val data = sendRequest(filter = filter)

        val totalCount = getDataValue<Int>(data, "client/feeds/totalCount")
        val feedId = getDataValue<Long>(data, "client/feeds/rowset/0/id")

        SoftAssertions.assertSoftly {
            it.assertThat(totalCount).`as`("totalCount").isEqualTo(1)
            it.assertThat(feedId).`as`("feedId").isEqualTo(secondFileFeed.id)
        }
    }

    @Test
    fun getFeeds_success_filterUpdateStatus() {
        val feedInfo = FeedInfo().apply {
            this.clientInfo = this@FeedsGraphQlServiceSortAndFilterTest.clientInfo
            this.feed = secondFileFeed
        }
        steps.feedSteps().setFeedProperty(feedInfo, Feed.UPDATE_STATUS, UpdateStatus.OUTDATED)

        val filter = GdFeedsFilter().apply { statuses = listOf(GdUpdateStatus.OUTDATED) }
        val data = sendRequest(filter = filter)

        val totalCount = getDataValue<Int>(data, "client/feeds/totalCount")
        val feedId = getDataValue<Long>(data, "client/feeds/rowset/0/id")

        SoftAssertions.assertSoftly {
            it.assertThat(totalCount).`as`("totalCount").isEqualTo(1)
            it.assertThat(feedId).`as`("feedId").isEqualTo(secondFileFeed.id)
        }
    }

    @Test
    fun getFeeds_success_filterSearchByName() {
        val partName = "Абра-Швабра-Кадабра"

        val feedInfo = FeedInfo().apply {
            this.clientInfo = this@FeedsGraphQlServiceSortAndFilterTest.clientInfo
            this.feed = secondFileFeed
        }
        steps.feedSteps().setFeedProperty(feedInfo, Feed.NAME, "test $partName test")

        val filter = GdFeedsFilter().apply { searchBy = partName }
        val data = sendRequest(filter = filter)

        val totalCount = getDataValue<Int>(data, "client/feeds/totalCount")
        val feedId = getDataValue<Long>(data, "client/feeds/rowset/0/id")

        SoftAssertions.assertSoftly {
            it.assertThat(totalCount).`as`("totalCount").isEqualTo(1)
            it.assertThat(feedId).`as`("feedId").isEqualTo(secondFileFeed.id)
        }
    }

    @Test
    fun getFeeds_success_filterSearchByFeedId() {
        val filter = GdFeedsFilter().apply { searchBy = secondFileFeed.id.toString() }
        val data = sendRequest(filter = filter)

        val totalCount = getDataValue<Int>(data, "client/feeds/totalCount")
        val feedId = getDataValue<Long>(data, "client/feeds/rowset/0/id")

        SoftAssertions.assertSoftly {
            it.assertThat(totalCount).`as`("totalCount").isEqualTo(1)
            it.assertThat(feedId).`as`("feedId").isEqualTo(secondFileFeed.id)
        }
    }

    @Test
    fun getFeeds_success_filterWithLimit() {
        val limitOffset = GdLimitOffset().apply { limit = 1; offset = 0 }
        val data = sendRequest(limitOffset = limitOffset)

        val totalCount = getDataValue<Int>(data, "client/feeds/totalCount")
        val feeds = getDataValue<List<Any>>(data, "client/feeds/rowset")

        SoftAssertions.assertSoftly {
            it.assertThat(totalCount).`as`("totalCount").isEqualTo(2)
            it.assertThat(feeds).`as`("feeds").hasSize(1)
        }
    }

    @Test
    fun getFeeds_success_filterOffsetTooMuch() {
        val limitOffset = GdLimitOffset().apply { limit = 1; offset = 2 }
        val data = sendRequest(limitOffset = limitOffset)

        val totalCount = getDataValue<Int>(data, "client/feeds/totalCount")
        val feeds = getDataValue<List<Any>>(data, "client/feeds/rowset")

        SoftAssertions.assertSoftly {
            it.assertThat(totalCount).`as`("totalCount").isEqualTo(2)
            it.assertThat(feeds).`as`("feeds").isEmpty()
        }
    }

    private fun sendRequest(filter: GdFeedsFilter? = null,
                            ordersBy: List<GdFeedsOrderBy>? = null,
                            limitOffset: GdLimitOffset? = null): Map<String, Any> {
        val args = mapOf("filter" to filter, "ordersBy" to ordersBy, "limitOffset" to limitOffset)
                .filterValues { it != null }
                .map { "${it.key}:${graphQlSerialize(it.value)}" }
                .joinToString(prefix = "( ", separator = ", ", postfix = " )")
                .takeIf { it.length > 3 } ?: ""
        val query = String.format(FEED_REQUEST, clientInfo.clientId, args)
        val context = createContext()
        val result: ExecutionResult = processor.processQuery(null, query, null, context)
        checkErrors(result.errors)
        return result.getData()
    }

    private fun createContext(): GridGraphQLContext {
        val user = userService.getUser(clientInfo.uid)
        val gridContext = buildContext(user)
                .withFetchedFieldsReslover(null)
        gridContextProvider.gridContext = gridContext
        return gridContext
    }

}
