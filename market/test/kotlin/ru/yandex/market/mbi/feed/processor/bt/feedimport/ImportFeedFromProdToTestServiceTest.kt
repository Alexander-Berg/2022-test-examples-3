package ru.yandex.market.mbi.feed.processor.bt.feedimport

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.stubbing
import org.mockito.kotlin.times
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import retrofit2.Response
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.mbi.feed.processor.FunctionalTest
import ru.yandex.market.mbi.feed.processor.yt.reader.TestYtTableReader
import ru.yandex.market.mbi.feed.processor.yt.reader.factory.YtTableReaderFactory
import ru.yandex.market.mbi.open.api.client.model.FeedType
import ru.yandex.market.mbi.open.api.client.model.PartnerFeedSourceType
import ru.yandex.market.mbi.open.api.client.model.RefreshPartnerFeedRequestDTO
import ru.yandex.market.mbi.open.api.client.model.RefreshPartnerFeedResponseDTO
import ru.yandex.market.mbi.open.api.client.api.FeedApi as MbiFeedApi

/**
 * Тесты для [ImportFeedFromProdToTestService].
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
internal class ImportFeedFromProdToTestServiceTest : FunctionalTest() {

    @Autowired
    private lateinit var importFeedFromProdToTestService: ImportFeedFromProdToTestService

    @Autowired
    private lateinit var mbiFeedApiClient: MbiFeedApi

    @Autowired
    private lateinit var ytTableReaderFactory: YtTableReaderFactory

    @AfterEach
    fun checkMocks() {
        verifyNoMoreInteractions(mbiFeedApiClient)
    }

    @Test
    fun `no partner mappings`() {
        mockYt(emptyList())
        importFeedFromProdToTestService.importFeeds()
        verifyNoMoreInteractions(ytTableReaderFactory)
    }

    @Test
    @DbUnitDataSet(after = ["csv/empty.mapping.after.csv"])
    fun `no feeds in yt`() {
        mockYt(emptyList())
        importFeedFromProdToTestService.importFeeds()
    }

    @Test
    @DbUnitDataSet(
        before = ["csv/mappings.before.csv"],
        after = ["csv/single-partner.after.csv"]
    )
    fun `all feeds from yt are new, add them`() {
        mockYt(
            listOf(
                AutotestFeed(
                    feedId = 300,
                    partnerId = 100,
                    "http://url300.local",
                    login = null,
                    password = null
                ),
                AutotestFeed(
                    feedId = 301,
                    partnerId = 100,
                    "http://url301.local",
                    login = "login123",
                    password = "pass123"
                ),
            )
        )
        mockApi(addFeedIds = listOf(10300L, 10301L))
        importFeedFromProdToTestService.importFeeds()

        val captor: KArgumentCaptor<RefreshPartnerFeedRequestDTO> = argumentCaptor()
        verifyBlocking(mbiFeedApiClient, times(2)) {
            addPartnerFeed(
                eq(200L),
                captor.capture(),
                eq(FeedType.ASSORTMENT_FEED)
            )
        }
        Assertions.assertThat(captor.allValues)
            .containsExactlyInAnyOrder(
                RefreshPartnerFeedRequestDTO(
                    url = "http://url300.local",
                    feedSourceType = PartnerFeedSourceType.EXTERNAL_REFRESHEABLE_FILE,
                ),
                RefreshPartnerFeedRequestDTO(
                    url = "http://url301.local",
                    feedSourceType = PartnerFeedSourceType.EXTERNAL_REFRESHEABLE_FILE,
                    login = "login123",
                    password = "pass123"
                )
            )
    }

    @Test
    @DbUnitDataSet(
        before = ["csv/mappings.before.csv", "csv/feed-mappings.before.csv"],
        after = ["csv/single-partner.after.csv"]
    )
    fun `1 - new feed, 2 - exists`() {
        mockYt(
            listOf(
                AutotestFeed(
                    feedId = 300,
                    partnerId = 100,
                    "http://url300.local",
                    login = null,
                    password = null
                ),
                AutotestFeed(
                    feedId = 301,
                    partnerId = 100,
                    "http://url301.local",
                    login = "login123",
                    password = "pass123"
                ),
            )
        )
        mockApi(addFeedIds = listOf(10300L), updateFeedIds = listOf(10301L))
        importFeedFromProdToTestService.importFeeds()

        val captor: KArgumentCaptor<RefreshPartnerFeedRequestDTO> = argumentCaptor()
        verifyBlocking(mbiFeedApiClient) {
            addPartnerFeed(
                eq(200L),
                captor.capture(),
                eq(FeedType.ASSORTMENT_FEED)
            )
        }
        verifyBlocking(mbiFeedApiClient) {
            updatePartnerFeed(
                eq(200L),
                eq(10301L),
                eq(FeedType.ASSORTMENT_FEED),
                captor.capture()
            )
        }
        Assertions.assertThat(captor.allValues)
            .containsExactlyInAnyOrder(
                RefreshPartnerFeedRequestDTO(
                    url = "http://url300.local",
                    feedSourceType = PartnerFeedSourceType.EXTERNAL_REFRESHEABLE_FILE,
                ),
                RefreshPartnerFeedRequestDTO(
                    url = "http://url301.local",
                    feedSourceType = PartnerFeedSourceType.EXTERNAL_REFRESHEABLE_FILE,
                    login = "login123",
                    password = "pass123"
                )
            )
    }

    @Test
    @DbUnitDataSet(
        before = ["csv/mappings.before.csv", "csv/feed-mappings.before.csv"],
        after = ["csv/delete-feed.after.csv"]
    )
    fun `1 - add new feed, 2 - delete`() {
        checkDelete()
    }

    @Test
    @DbUnitDataSet(
        before = ["csv/mappings.before.csv", "csv/unknown-feed.before.csv"],
        after = ["csv/delete-feed.after.csv"]
    )
    fun `delete unknown feeds`() {
        checkDelete()
    }

    private fun checkDelete() {
        mockYt(
            listOf(
                AutotestFeed(
                    feedId = 300,
                    partnerId = 100,
                    "http://url300.local",
                    login = null,
                    password = null
                ),
            )
        )
        mockApi(addFeedIds = listOf(10300L), deleteFeedIds = listOf(10301L))
        importFeedFromProdToTestService.importFeeds()

        val captor = argumentCaptor<RefreshPartnerFeedRequestDTO>()
        verifyBlocking(mbiFeedApiClient) {
            addPartnerFeed(
                eq(200L),
                captor.capture(),
                eq(FeedType.ASSORTMENT_FEED)
            )
        }
        verifyBlocking(mbiFeedApiClient) {
            deletePartnerFeed(
                eq(200L),
                eq(10301L),
                eq(FeedType.ASSORTMENT_FEED)
            )
        }
        Assertions.assertThat(captor.allValues)
            .containsExactlyInAnyOrder(
                RefreshPartnerFeedRequestDTO(
                    url = "http://url300.local",
                    feedSourceType = PartnerFeedSourceType.EXTERNAL_REFRESHEABLE_FILE,
                )
            )
    }

    @Test
    @DbUnitDataSet(
        before = ["csv/mappings.before.csv", "csv/deleted-unknown-feed.before.csv"],
        after = ["csv/delete-feed.after.csv"]
    )
    @ExperimentalCoroutinesApi
    fun `deleted feed will be ignored`() = runBlockingTest {
        mockYt(
            listOf(
                AutotestFeed(
                    feedId = 300,
                    partnerId = 100,
                    "http://url300.local",
                    login = null,
                    password = null
                ),
            )
        )
        mockApi(addFeedIds = listOf(10300L))
        importFeedFromProdToTestService.importFeeds()

        val captor: KArgumentCaptor<RefreshPartnerFeedRequestDTO> = argumentCaptor()
        verifyBlocking(mbiFeedApiClient) {
            addPartnerFeed(
                eq(200L),
                captor.capture(),
                eq(FeedType.ASSORTMENT_FEED)
            )
        }
        Assertions.assertThat(captor.allValues)
            .containsExactlyInAnyOrder(
                RefreshPartnerFeedRequestDTO(
                    url = "http://url300.local",
                    feedSourceType = PartnerFeedSourceType.EXTERNAL_REFRESHEABLE_FILE,
                )
            )
    }

    private fun mockYt(data: List<AutotestFeed>) {
        whenever(ytTableReaderFactory.createYQLPreparedReader<AutotestFeed>(any(), any(), any(), any()))
            .thenReturn(TestYtTableReader(data))
    }

    private fun mockApi(
        addFeedIds: List<Long> = emptyList(),
        updateFeedIds: List<Long> = emptyList(),
        deleteFeedIds: List<Long> = emptyList(),
    ) {
        mockApi(addFeedIds) { addPartnerFeed(any(), any(), anyOrNull()) }
        mockApi(updateFeedIds) { updatePartnerFeed(any(), any(), any(), any()) }
        mockApi(deleteFeedIds) { deletePartnerFeed(any(), any(), any()) }
    }

    private fun mockApi(
        feedIds: List<Long>,
        apiAction: suspend MbiFeedApi.() -> Response<RefreshPartnerFeedResponseDTO>
    ) {
        fun <T> Iterator<T>.nextOrNull() =
            if (hasNext()) next()
            else null

        val feedsIterator = feedIds.iterator()
        stubbing(mbiFeedApiClient) {
            onBlocking {
                apiAction()
            }.doAnswer { Response.success(RefreshPartnerFeedResponseDTO(feedId = feedsIterator.nextOrNull())) }
        }
    }
}
