package ru.yandex.direct.core.entity.feed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FeedUtilsKtUrlTest {

    @Suppress("SpellCheckingInspection")
    private fun urlParameters() =
        listOf(
            "https://моё-добро.рф/someting/else/123.xml",
            "https://umarket.feed/someting/else/123.xml",
            "http://feed.tools.domaun.yandex/o.cgi?source=mvideo2_moscow&set_utm_source=rtg_yandex&set_utm_medium=cpc&set_utm_content=[id]&set_utm_source=rtg_yandex&set_utm_campaign=DynRmkt_moscow_[%20utm_campaign%20]_mgcom_&set_utm_term=[utm_term]&where_price=%3C3000000"
        )

    @ParameterizedTest
    @MethodSource("urlParameters")
    fun test_url_converters(srcUrl: String) {
        val fakeUrl = createFakeFeedUrl(1L, 2L, 3L, srcUrl)
        val repairedUrl = tryGetSrcUrl(fakeUrl)
        assertThat(repairedUrl).isEqualTo(srcUrl)
    }

    @ParameterizedTest
    @MethodSource("urlParameters")
    fun test_unFakeUrl(srcUrl: String) {
        val fakeUrl = createFakeFeedUrl(1L, 2L, 3L, srcUrl)
        val repairedUrl = unFakeUrlIfNeeded(fakeUrl)
        assertThat(repairedUrl).isEqualTo(srcUrl)
    }

    @ParameterizedTest
    @MethodSource("urlParameters")
    fun test_unFakeDeepFakedUrl(srcUrl: String) {
        val fakeUrl =
            createFakeFeedUrl(1L, 2L, 3L, createFakeFeedUrl(1L, 2L, 3L, (createFakeFeedUrl(1L, 2L, 3L, srcUrl))))
        val repairedUrl = unFakeUrlIfNeeded(fakeUrl)
        assertThat(repairedUrl).isEqualTo(srcUrl)
    }

}
