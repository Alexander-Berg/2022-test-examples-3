package ru.yandex.market.mbi.feed.processor.api

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.of
import org.junit.jupiter.params.provider.MethodSource
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.common.test.spring.FunctionalTestHelper
import ru.yandex.market.common.test.util.JsonTestUtil
import ru.yandex.market.mbi.feed.processor.FunctionalTest
import ru.yandex.market.mbi.feed.processor.model.FeedTypeForParsingFields
import ru.yandex.market.mbi.feed.processor.model.FeedTypeForParsingFields.BUSINESS
import ru.yandex.market.mbi.feed.processor.model.FeedTypeForParsingFields.SERVICE
import ru.yandex.market.mbi.feed.processor.model.ParsingFieldsResponse
import ru.yandex.market.mbi.feed.processor.model.ShopTypeForParsingFields
import ru.yandex.market.mbi.feed.processor.model.ShopTypeForParsingFields.DBS
import ru.yandex.market.mbi.feed.processor.model.ShopTypeForParsingFields.FBY
import ru.yandex.market.mbi.feed.processor.test.ApiUrl
import ru.yandex.market.mbi.feed.processor.test.getString

internal class FeedParsingFieldsControllerTest : FunctionalTest() {

    private val getBusinessFeedsUrl: String by ApiUrl("/feeds/parsing-fields")

    companion object {
        @JvmStatic
        fun arg() = listOf(
            of(null, null, "fields/json/response/All.json"),
            of(BUSINESS, null, "fields/json/response/Business.json"),
            of(BUSINESS, FBY, "fields/json/response/Business.json"),
            of(SERVICE, null, "fields/json/response/Service.json"),
            of(SERVICE, DBS, "fields/json/response/Service.json"),
            of(SERVICE, FBY, "fields/json/response/ServiceAndFby.json"),
        )
    }

    @ParameterizedTest
    @MethodSource("arg")
    @DbUnitDataSet(before = ["fields/csv/FeedParsingFieldsControllerTest.csv"])
    fun `check fields`(
        type: FeedTypeForParsingFields?,
        shopType: ShopTypeForParsingFields?,
        response: String
    ) {
        val urlParams = setOfNotNull(
            type?.let { "type=$it" },
            shopType?.let { "shopType=$it" },
        )
        val url = getBusinessFeedsUrl.plus("?").plus(urlParams.joinToString("&"))
        FunctionalTestHelper.get(url, ParsingFieldsResponse::class).body.let {
            JsonTestUtil.assertEquals(
                getString<FeedParsingFieldsControllerTest>(response), it
            )
        }
    }
}
