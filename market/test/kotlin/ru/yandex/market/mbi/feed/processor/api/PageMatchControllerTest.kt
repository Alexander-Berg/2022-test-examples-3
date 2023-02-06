package ru.yandex.market.mbi.feed.processor.api

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import ru.yandex.market.common.test.spring.FunctionalTestHelper
import ru.yandex.market.mbi.feed.processor.FunctionalTest
import ru.yandex.market.mbi.feed.processor.test.ApiUrl

/**
 * Тесты для [PageMatchController].
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
internal class PageMatchControllerTest : FunctionalTest() {

    private val url: String by ApiUrl("/pagematch")

    @Test
    fun `test pagematch format`() {
        val response = FunctionalTestHelper.get(url)

        assertThat(response.statusCode)
            .isEqualTo(HttpStatus.OK)
        assertThat(response.body)
            .contains("ping\t/ping\tfeed-processor")
    }
}
