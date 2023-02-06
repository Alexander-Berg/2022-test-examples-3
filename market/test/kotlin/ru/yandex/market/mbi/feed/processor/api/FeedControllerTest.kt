package ru.yandex.market.mbi.feed.processor.api

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.stubbing
import org.mockito.kotlin.verifyBlocking
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpMethod
import retrofit2.Response
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.common.test.spring.FunctionalTestHelper
import ru.yandex.market.common.test.util.JsonTestUtil
import ru.yandex.market.mbi.feed.processor.FunctionalTest
import ru.yandex.market.mbi.feed.processor.model.FeedType
import ru.yandex.market.mbi.feed.processor.test.ApiUrl
import ru.yandex.market.mbi.feed.processor.test.getString
import ru.yandex.market.mbi.feed.processor.test.isEqualTo
import ru.yandex.market.mbi.open.api.client.model.RefreshPartnerFeedRequestDTO
import ru.yandex.market.mbi.open.api.client.model.RefreshPartnerFeedResponseDTO
import ru.yandex.market.mbi.open.api.client.api.FeedApi as MbiFeedApi

/**
 * Тесты для [FeedController].
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
internal class FeedControllerTest : FunctionalTest() {

    private val partnerId: Long = 100L
    private val feedId: Long = 200L
    private val feedType: FeedType = FeedType.ASSORTMENT_FEED

    private val deleteUrl: String by ApiUrl("/feed?partner_id=%d&feed_id=%d&feed_type=%s&updated_at=2021-01-01T10:00:30Z")
    private val addUrl: String by ApiUrl("/feed")

    @Autowired
    private lateinit var mbiFeedApiClient: MbiFeedApi

    @Test
    @ExperimentalCoroutinesApi
    fun `proxy feed creation to mbi`() = runBlockingTest {
        stubbing(mbiFeedApiClient) {
            onBlocking {
                addPartnerFeed(any(), any(), anyOrNull())
            }.thenReturn(Response.success(RefreshPartnerFeedResponseDTO(feedId = 123L)))
        }

        val request = getString<FeedControllerTest>("feed/add.request.json")
        FunctionalTestHelper.postForJson(addUrl, request).apply {
            JsonTestUtil.assertEquals(getString<FeedControllerTest>("feed/add.response.json"), this)
        }

        val captor: KArgumentCaptor<RefreshPartnerFeedRequestDTO> = argumentCaptor()

        verifyBlocking(mbiFeedApiClient) { addPartnerFeed(eq(1001L), captor.capture(), anyOrNull()) }

        with(captor.allValues.single()) {
            Assertions.assertThat(url).isEqualTo("http://feed.url")
            Assertions.assertThat(login).isEqualTo("login1")
            Assertions.assertThat(password).isEqualTo("pass1")
        }
    }

    @Test
    @DbUnitDataSet(after = ["feed/FeedControllerTest.deleted_new.after.csv"])
    fun `create new empty feed, if it didn't exist before`() {
        FunctionalTestHelper.exchange(deleteUrl.format(partnerId, feedId, feedType), null, HttpMethod.DELETE)
            .isEqualTo<MigrationControllerTest>("feed/deleted.response.json")
    }

    @Test
    @DbUnitDataSet(
        before = ["feed/FeedControllerTest.old_feed.before.csv"],
        after = ["feed/FeedControllerTest.deleted.after.csv"]
    )
    fun `update existing feed`() {
        FunctionalTestHelper.exchange(deleteUrl.format(partnerId, feedId, feedType), null, HttpMethod.DELETE)
            .isEqualTo<MigrationControllerTest>("feed/deleted.response.json")
    }

    @Test
    @DbUnitDataSet(
        before = ["feed/FeedControllerTest.new_feed.before.csv"],
        after = ["feed/FeedControllerTest.new_feed.before.csv"]
    )
    fun `don't update existing feed, because it has newer updated_at date`() {
        FunctionalTestHelper.exchange(deleteUrl.format(partnerId, feedId, feedType), null, HttpMethod.DELETE)
            .isEqualTo<MigrationControllerTest>("feed/not_deleted.response.json")
    }
}
