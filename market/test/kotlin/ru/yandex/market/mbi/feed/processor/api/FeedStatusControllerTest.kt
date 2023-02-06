package ru.yandex.market.mbi.feed.processor.api

import org.junit.jupiter.api.Test
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.common.test.spring.FunctionalTestHelper
import ru.yandex.market.mbi.feed.processor.FunctionalTest
import ru.yandex.market.mbi.feed.processor.test.ApiUrl
import ru.yandex.market.mbi.feed.processor.test.isEqualTo

@DbUnitDataSet(before = ["status/FeedStatusControllerTest.before.csv"])
internal class FeedStatusControllerTest : FunctionalTest() {

    private val url: String by ApiUrl("/feed/1001/status/")

    @Test
    fun `parsing and download status filled`() {
        FunctionalTestHelper.get("$url?feed_type=ASSORTMENT_FEED")
            .isEqualTo<FeedStatusControllerTest>("status/fullStatus.json")
    }

    @Test
    fun `download status filled`() {
        FunctionalTestHelper.get("$url?feed_type=PRICES_FEED")
            .isEqualTo<FeedStatusControllerTest>("status/downloadStatus.json")
    }
}
