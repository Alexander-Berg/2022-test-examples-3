package ru.yandex.market.mbi.feed.processor.api.feeds

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.common.test.spring.FunctionalTestHelper
import ru.yandex.market.common.test.util.JsonTestUtil
import ru.yandex.market.mbi.feed.processor.FunctionalTest
import ru.yandex.market.mbi.feed.processor.api.FeedControllerTest
import ru.yandex.market.mbi.feed.processor.test.ApiUrl
import ru.yandex.market.mbi.feed.processor.test.getString

@DbUnitDataSet(before = ["csv/FeedsControllerTest.before.csv"])
internal class FeedsControllerTest : FunctionalTest() {

    private val getBusinessFeedsUrl: String by ApiUrl("/feeds/list")

    @ParameterizedTest
    @CsvSource(
        value = [
            "feeds/json/request/byBusinessRequest.json, feeds/json/response/byBusinessResponse.json, 6, 1",
            "feeds/json/request/byBusinessAndTypeRequest.json, feeds/json/response/byBusinessAndTypeResponse.json, 5, 1",
            "feeds/json/request/byBusinessAndPartnerRequest.json, feeds/json/response/byBusinessAndPartnerResponse.json, 5,1",
            "feeds/json/request/byBusinessRequest.json, feeds/json/response/byBusinessResponsePage1.json, 1,1",
            "feeds/json/request/byBusinessRequest.json, feeds/json/response/byBusinessResponsePage2.json, 1,2",
            "feeds/json/request/byBusinessRequestFilterStatus.json, feeds/json/response/byBusinessResponseFilterStatus.json, 5, 1",
            "feeds/json/request/byBusinessRequestFilterUpdateType.json, feeds/json/response/byBusinessResponseFilterUpdateType.json, 5, 1",
            "feeds/json/request/byBusinessRequestFilterIsDefault.json, feeds/json/response/byBusinessResponseFilterIsDefault.json, 5, 1",
        ]
    )
    fun `get business feeds with params`(
        requestBodyPath: String,
        responseBodyPath: String,
        pageSize: Int,
        pageNumber: Int
    ) {
        val urlParams = mutableSetOf<String>()
        urlParams.add("pageSize=$pageSize")
        urlParams.add("pageNumber=$pageNumber")
        val url = getBusinessFeedsUrl.plus("?").plus(urlParams.joinToString("&"))
        val request = getString<FeedControllerTest>(requestBodyPath)
        FunctionalTestHelper.postForJson(url, request).apply {
            JsonTestUtil.assertEquals(
                getString<FeedControllerTest>(responseBodyPath), this
            )
        }
    }
}
