package ru.yandex.market.mbi.orderservice.common.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.Currency

class CurrencyHelperTest {

    @Test
    fun `verify convertToCents`() {
        val rur = BigDecimal.valueOf(200.45)
        val rurCents = rur.convertToCents(Currency.getInstance("RUB"))
        assertThat(rurCents).isEqualTo(20045)

        val jpy = BigDecimal.valueOf(300)
        val jpyCents = jpy.convertToCents(Currency.getInstance("JPY"))
        assertThat(jpyCents).isEqualTo(300) // no fractional part
    }

    @Test
    fun `verify convertFromCents`() {
        val rurCents = 20045L
        val rur = rurCents.convertFromCents(Currency.getInstance("RUB"))
        assertThat(rur).isEqualTo(BigDecimal.valueOf(200.45))

        val jpyCents = 300L
        val jpy = jpyCents.convertFromCents(Currency.getInstance("JPY"))
        assertThat(jpy).isEqualTo(BigDecimal.valueOf(300)) // no fractional part
    }
}
