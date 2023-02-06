package ru.yandex.direct.core.grut.api.utils

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.yandex.direct.currency.CurrencyCode

@RunWith(Parameterized::class)
class CurrencyIsoCodeConverterTest(
    private val currency: CurrencyCode
) {

    private val expectedCurrencyToIsoCodeMap = mapOf(
        CurrencyCode.YND_FIXED to 999,
        CurrencyCode.RUB to 643,
        CurrencyCode.EUR to 978,
        CurrencyCode.USD to 840,
        CurrencyCode.BYN to 933,
        CurrencyCode.KZT to 398,
        CurrencyCode.CHF to 756,
        CurrencyCode.TRY to 949,
        CurrencyCode.UAH to 980,
        CurrencyCode.GBP to 826,
    )

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "currency={0}")
        fun testData() = CurrencyCode.values()
    }

    @Test
    fun testCurrencyToIsoCodeConversion() {
        val expectedCode = expectedCurrencyToIsoCodeMap[currency]
        val gotCode = toIsoCode(currency)
        assertThat(expectedCode).withFailMessage("No expected currency iso code for currency $currency, calculated code is $gotCode").isNotNull
        assertThat(expectedCode).isEqualTo(gotCode)
    }
}
