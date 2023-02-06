package ru.yandex.market.mbi.orderservice.common.model.yt

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

/**
 * Тесты на [Cis]
 */
class CisTest {

    @Test
    fun `validate cis`() {
        val cis = assertDoesNotThrow { Cis("01046301293048002120370003735599180939") }
        assertThat(cis).satisfies {
            assertThat(it.isFullCis()).isEqualTo(false)
            assertThat(it.getIdentity()).isEqualTo("01046301293048002120370003735599180939")
            assertThat(it.getCryptoTailSafe()).isNull()
        }
    }

    @Test
    fun `validate full cis`() {
        val cis = assertDoesNotThrow {
            Cis("01046301293048002120370003735599180939\u001D910094\u001D92MDAwMzczNTU5OTE4MDkzOQ==")
        }
        assertThat(cis).satisfies {
            assertThat(it.isFullCis()).isEqualTo(true)
            assertThat(it.getIdentity()).isEqualTo("01046301293048002120370003735599180939")
            assertThat(it.getCryptoTailSafe()).isEqualTo("MDAwMzczNTU5OTE4MDkzOQ==")
        }
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "01046301293048002120370003735599180939\u001D910094",
        "0104630129304",
        "01046301293048002120370003735599180939\u001D910094\u001D92PnRxEyQI09uDgaK1J+gXCzRNp+Utd+2kh9zQP5MQu"
    ])
    fun `check invalid cis`(cis: String) {
        assertThrows<IllegalArgumentException> { Cis(cis) }
    }
}
