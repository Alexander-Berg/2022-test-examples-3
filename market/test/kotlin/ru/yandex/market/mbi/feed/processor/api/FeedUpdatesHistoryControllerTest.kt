package ru.yandex.market.mbi.feed.processor.api

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.common.test.spring.FunctionalTestHelper
import ru.yandex.market.common.test.util.JsonTestUtil
import ru.yandex.market.mbi.feed.processor.test.ApiUrl
import ru.yandex.market.mbi.feed.processor.test.getString
import java.time.Instant
import java.time.ZoneOffset

internal class FeedUpdatesHistoryControllerTest : AbstractFeedHistoryControllerTest() {

    private val getFeedUpdatesHistoryUrlPost: String by ApiUrl("/history")

    private val nowUTC = Instant.now().atOffset(ZoneOffset.UTC).toString()
    private val nowMinusNineDaysUTC = Instant.now().atOffset(ZoneOffset.UTC).minusDays(9).toString()

    @Test
    @DbUnitDataSet(before = ["history/FeedUpdatesHistoryControllerTest.getFeedUpdatesHistory.before.csv"])
    fun `feed's updates history`() {
        checkFeedUpdatesHistory(
            parsingRecordsPath = "history/feedParsingRecord.json",
            requestPath = "history/requestUpdateHistory.json",
            responsePath = "history/getFeedUpdatesHistory.json",
            assertQuery = {
                assertThat(it).contains("businessId = 7654321")
                assertThat(it).contains("partnerId in (666999,111)")
            }
        )
    }

    @Test
    @DbUnitDataSet(before = ["history/FeedUpdatesHistoryControllerTest.getFeedUpdatesHistory.before.csv"])
    fun `feed's updates history include business`() {
        checkFeedUpdatesHistory(
            parsingRecordsPath = "history/feedParsingWithBusinessRecord.json",
            requestPath = "history/requestUpdateHistoryIncludeBusiness.json",
            responsePath = "history/getFeedUpdatesHistoryIncludeBusiness.json",
            assertQuery = {
                assertThat(it).contains("businessId = 7654321")
                assertThat(it).contains("partnerId in (666999,111,7654321)")
            }
        )
    }

    @Test
    @DbUnitDataSet(before = ["history/FeedUpdatesHistoryControllerTest.getFeedUpdatesHistory.before.csv"])
    fun `feed's updates history with filters`() {
        checkFeedUpdatesHistory(
            parsingRecordsPath = "history/feedParsingRecord.json",
            requestPath = "history/requestUpdateHistoryWithFilters.json",
            responsePath = "history/getFeedUpdatesHistory.json",
            assertQuery = {
                assertThat(it).contains("feedType = 'ASSORTMENT_FEED'")
                assertThat(it).contains("status in ('INTERNAL_ERROR','FEED_NOT_MODIFIED')")
                assertThat(it).contains("feedId = 4444")
                assertThat(it).contains("isUpload = true")
            }
        )
    }

    @Test
    @DbUnitDataSet(before = ["history/FeedUpdatesHistoryControllerTest.getFeedUpdatesHistory.before.csv"])
    fun `paging page 1`() {
        checkFeedUpdatesHistory(
            parsingRecordsPath = "history/feedParsingRecord.json",
            requestPath = "history/requestUpdateHistory.json",
            pageSize = 10,
            assertQuery = {
                assertThat(it).contains("offset 0")
                assertThat(it).contains("limit 11")
            }
        )
    }

    @Test
    @DbUnitDataSet(before = ["history/FeedUpdatesHistoryControllerTest.getFeedUpdatesHistory.before.csv"])
    fun `paging page 2`() {
        checkFeedUpdatesHistory(
            parsingRecordsPath = "history/feedParsingRecord.json",
            requestPath = "history/requestUpdateHistory.json",
            pageSize = 10,
            page = 2,
            assertQuery = {
                assertThat(it).contains("offset 10")
                assertThat(it).contains("limit 11")
            }
        )
    }

    @Test
    @DbUnitDataSet(before = ["history/FeedUpdatesHistoryControllerTest.getFeedUpdatesHistory.before.csv"])
    fun `paging page incorrect`() {
        checkFeedUpdatesHistory(
            parsingRecordsPath = "history/feedParsingRecord.json",
            requestPath = "history/requestUpdateHistory.json",
            pageSize = 10,
            page = -348,
            assertQuery = {
                assertThat(it).contains("offset 0")
                assertThat(it).contains("limit 11")
            }
        )
    }

    private fun checkFeedUpdatesHistory(
        parsingRecordsPath: String,
        requestPath: String,
        responsePath: String? = null,
        pageSize: Int? = null,
        page: Int? = null,
        assertQuery: ((String) -> Unit)? = null
    ) {
        val captorQuery = mockFeedParsingYtClient(parsingRecordsPath)
        val request = getString<FeedUpdatesHistoryControllerTest>(
            requestPath,
            mapOf(
                "dateFrom" to nowMinusNineDaysUTC,
                "dateTo" to nowUTC,
            )
        )
        val url = getFeedUpdatesHistoryUrlPost +
            listOfNotNull(
                pageSize?.let { "&pageSize=$it" },
                page?.let { "&nextToken=$it" }
            ).joinToString(separator = "&", prefix = "?")

        FunctionalTestHelper.postForJson(url, request).apply {
            responsePath?.let {
                JsonTestUtil.assertEquals(
                    getString<FeedUpdatesHistoryControllerTest>(it),
                    this
                        .replace(Regex("\"historyFeedFileId\":\"[^\"]*\""), "\"historyFeedFileId\": \"$recordId\"")
                )
            }
        }
        assertQuery?.invoke(captorQuery.firstValue)
    }
}
