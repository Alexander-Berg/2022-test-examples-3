package ru.yandex.market.partner.status.flow

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Тесты для расширений [Flow].
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class FlowExtensionsTest {

    @Test
    fun `chunk with remainder`() {
        val actual = runBlocking {
            flowOf(1, 2, 3)
                .chunked(2)
                .toList()
        }

        Assertions.assertThat(actual)
            .contains(
                listOf(1, 2),
                listOf(3)
            )
    }

    @Test
    fun `chunk without remainder`() {
        val actual = runBlocking {
            flowOf(1, 2, 3, 4)
                .chunked(2)
                .toList()
        }

        Assertions.assertThat(actual)
            .contains(
                listOf(1, 2),
                listOf(3, 4)
            )
    }

    @Test
    fun `empty flow`() {
        val actual = runBlocking {
            flowOf<Int>()
                .chunked(2)
                .toList()
        }

        Assertions.assertThat(actual)
            .isEmpty()
    }
}
