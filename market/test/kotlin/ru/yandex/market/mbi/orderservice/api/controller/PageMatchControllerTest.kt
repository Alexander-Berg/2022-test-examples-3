package ru.yandex.market.mbi.orderservice.api.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import ru.yandex.market.common.test.spring.FunctionalTestHelper
import ru.yandex.market.mbi.orderservice.api.ApiUrl
import ru.yandex.market.mbi.orderservice.api.FunctionalTest

/**
 * Тесты для [PageMatchController].
 */
internal class PageMatchControllerTest : FunctionalTest() {

    private val url: String by ApiUrl("/pagematch")

    @Test
    fun `test pagematch format`() {
        val response = FunctionalTestHelper.get(url)

        assertThat(response.statusCode)
            .isEqualTo(HttpStatus.OK)
        assertThat(response.body)
            .contains("ping\t/ping\torder-service")
    }
}
