package ru.yandex.market.mbi.feed.processor

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import ru.yandex.market.common.test.spring.FunctionalTestHelper

/**
 * Тесты на ручку /ping.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
internal class PingTest : FunctionalTest() {

    @Test
    fun `test ping controller`() {
        val response = FunctionalTestHelper.get("$baseUrl/ping")
        Assertions.assertThat(response)
            .matches { it.statusCode.is2xxSuccessful }
            .extracting { it.body }
            .isEqualTo("0;OK")
    }
}
