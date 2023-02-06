package ru.yandex.market.pricingmgmt.util.promo

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import ru.yandex.market.pricingmgmt.util.promo.PromoUtils.formatPromoId

internal class PromoUtilsTest {

    @ParameterizedTest
    @CsvSource(value = ["100001,cf_100001", "1000000,cf_1000000", "999999999,cf_999999999"])
    fun formatPromoId_ok(promoId: Int, expected: String) {
        assertEquals(expected, formatPromoId(promoId))
    }

    @ParameterizedTest
    @ValueSource(ints = [-1000, -1, 0])
    fun formatPromoId_negativeOrZero_throws(promoId: Int) {
        val e = assertThrows<RuntimeException> { formatPromoId(promoId) }
        assertEquals("promoId should be positive", e.message)
    }

    @ParameterizedTest
    @ValueSource(ints = [1, 100000])
    fun formatPromoId_longString_throws(promoId: Int) {
        val longPrefix = "x".repeat(255)
        val e = assertThrows<RuntimeException> {
            formatPromoId(
                promoId = promoId,
                prefix = longPrefix,
                maxLength = 255
            )
        }
        assertEquals("Идентификатор промо должен быть не пустым и не длиннее 255 символов", e.message)
    }
}
