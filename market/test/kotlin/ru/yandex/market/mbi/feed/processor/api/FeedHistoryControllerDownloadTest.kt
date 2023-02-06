package ru.yandex.market.mbi.feed.processor.api

import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.common.test.spring.FunctionalTestHelper
import ru.yandex.market.mbi.feed.processor.test.ApiUrl
import ru.yandex.market.mbi.feed.processor.test.isEqualTo
import java.net.URL

internal class FeedHistoryControllerDownloadTest : AbstractFeedHistoryControllerTest() {
    @Autowired
    private lateinit var mdsS3Client: MdsS3Client

    private val recordIdEncoded = "9c4518756e2bd8504947b8e248725ae13d862d4fe6dce77e8581bf683c3d792d"

    private val getFeedFromHistoryUrl: String by ApiUrl("/history/feed/$recordIdEncoded")
    private val getFeedFromHistoryUrlAsync: String by ApiUrl(
        "/history/feed/$recordIdEncoded/async?" +
            "partnerId=$partnerId&businessId=$businessId"
    )
    private val getFeedFromHistoryUrlBusinessAsync: String by ApiUrl(
        "/history/feed/$recordIdEncoded/async?" +
            "businessId=$businessId"
    )

    @Test
    @DbUnitDataSet(before = ["history/FeedUpdatesHistoryControllerTest.getFeedUpdatesHistory.before.csv"])
    fun `get downloadable feed`() {
        mockFeedParsingYtClient("history/feedParsingRecord.json")
        mockMdsGetUrl()
        FunctionalTestHelper.get(getFeedFromHistoryUrl)
            .isEqualTo<FeedUpdatesHistoryControllerTest>("history/getFeedFromHistory.json")
    }

    @Test
    @DbUnitDataSet(before = ["history/FeedUpdatesHistoryControllerTest.getFeedUpdatesHistory.before.csv"])
    fun `get downloadable feed with extension`() {
        mockFeedParsingYtClient("history/feedParsingRecordWithExtension.json")
        mockMdsGetUrl()
        FunctionalTestHelper.get(getFeedFromHistoryUrl)
            .isEqualTo<FeedUpdatesHistoryControllerTest>("history/getFeedFromHistoryXlsx.json")
    }

    @Test
    @DbUnitDataSet(
        before = ["history/FeedUpdatesHistoryControllerTest.getFeedUpdatesHistory.before.csv"],
        after = ["history/download/FeedUpdatesHistoryControllerTest.feedDownload.after.csv"]
    )
    fun `get downloadable feed async not finished`() {
        mockFeedParsingYtClient("history/feedParsingRecord.json")
        doAnswer {
            Thread.sleep(5000)
        }.`when`(mdsS3Client)
            .getUrl(any())

        FunctionalTestHelper.get(getFeedFromHistoryUrlAsync)
            .isEqualTo<FeedUpdatesHistoryControllerTest>("history/download/getHistoryAsyncResponse.json")
        // Запросили еще раз
        FunctionalTestHelper.get(getFeedFromHistoryUrlAsync)
            .isEqualTo<FeedUpdatesHistoryControllerTest>("history/download/getHistoryAsyncResponse.json")
    }

    @Test
    @DbUnitDataSet(
        before = ["history/FeedUpdatesHistoryControllerTest.getFeedUpdatesHistory.before.csv"],
        after = ["history/download/FeedUpdatesHistoryControllerTest.feedDownload.business.after.csv"]
    )
    fun `get downloadable business feed async not finished`() {
        mockFeedParsingYtClient("history/feedParsingRecord.json")
        doAnswer {
            Thread.sleep(5000)
        }.`when`(mdsS3Client)
            .getUrl(any())

        FunctionalTestHelper.get(getFeedFromHistoryUrlBusinessAsync)
            .isEqualTo<FeedUpdatesHistoryControllerTest>("history/download/getHistoryAsyncResponse.json")
        // Запросили еще раз
        FunctionalTestHelper.get(getFeedFromHistoryUrlBusinessAsync)
            .isEqualTo<FeedUpdatesHistoryControllerTest>("history/download/getHistoryAsyncResponse.json")
    }

    @Test
    @DbUnitDataSet(
        before = ["history/FeedUpdatesHistoryControllerTest.getFeedUpdatesHistory.before.csv"],
        after = ["history/download/FeedUpdatesHistoryControllerTest.feedDownloadSuccess.after.csv"]
    )
    fun `get downloadable feed async finished`() {
        mockFeedParsingYtClient("history/feedParsingRecord.json")
        mockMdsGetUrl()

        FunctionalTestHelper.get(getFeedFromHistoryUrlAsync)
            .isEqualTo<FeedUpdatesHistoryControllerTest>("history/download/getHistoryAsyncResponse.json")
        // TODO: убрать этот колхоз в пользу CountDownLatch
        Thread.sleep(2000)
        // Теперь успех
        FunctionalTestHelper.get(getFeedFromHistoryUrlAsync)
            .isEqualTo<FeedUpdatesHistoryControllerTest>("history/getFeedFromHistory.json")
    }

    private fun mockMdsGetUrl() {
        doReturn(URL("https://website.ru/yandex.xml"))
            .`when`(mdsS3Client)
            .getUrl(any())
    }
}
