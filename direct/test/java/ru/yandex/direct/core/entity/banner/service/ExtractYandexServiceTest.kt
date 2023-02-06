package ru.yandex.direct.core.entity.banner.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.rules.SpringMethodRule
import ru.yandex.direct.core.entity.banner.type.href.BannersUrlHelper
import ru.yandex.direct.core.testing.configuration.CoreTest

@CoreTest
@RunWith(Parameterized::class)
class ExtractYandexServiceTest(
    private val href: String,
    private val expectedService: String?,
) {
    @get:Rule
    var springMethodRule = SpringMethodRule()

    @Autowired
    private lateinit var bannersUrlHelper: BannersUrlHelper

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "href={0}, expected service={1}")
        fun testData() = arrayOf(
            arrayOf("https://market.yandex.ru", "market"),
            arrayOf("https://m.market.yandex.ru", "market"),
            arrayOf("market.yandex.ru", "market"),
            arrayOf("yandex.ru/market", "market"),
            arrayOf("https://m.yandex.ru/market", "market"),
            arrayOf("http://www.market.yandex.com", "market"),
            arrayOf("https://market.yandex.ru/store--marvel-kt?businessId=860533", "market"),
            arrayOf("https://pokupki.market.yandex.ru", "pokupki.market"),
            arrayOf("https://market.neyandex.ru", null),
            arrayOf("https://yandex.ru", null),
            arrayOf("https://ozon.ru", null),
        )
    }

    @Test
    fun test() {
        val domain = bannersUrlHelper.extractHostFromHrefWithoutWwwOrNull(href)
        val actualService = bannersUrlHelper.extractYandexServiceFromDomain(domain)
        assertThat(actualService).isEqualTo(expectedService)
    }
}
