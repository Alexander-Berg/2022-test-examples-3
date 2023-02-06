package ru.yandex.market.mapi.client.fapi.util

import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.client.fapi.enums.FapiPromoType
import ru.yandex.market.mapi.client.fapi.model.FapiPicture
import ru.yandex.market.mapi.client.fapi.model.FapiPrice
import ru.yandex.market.mapi.client.fapi.model.FapiPromo
import java.math.BigDecimal
import kotlin.test.assertEquals

/**
 * @author Ilya Kislitsyn / ilyakis@ / 30.03.2022
 */
class FapiUtilsTest {

    @Test
    fun testPictureFormat() {
        assertEquals("https://pic/orig", testUrlPicture("//pic/").original?.format())

        assertEquals(
            listOf(
                "https://pic/orig",
                "https://pic2/orig"
            ),
            listOf(
                testUrlPicture(url = "//pic/"),
                testUrlPicture(url = "//pic2/"),
                testUrlPicture(url = "//pic3/")
            ).slicePictures(2)
        )
    }

    @Test
    fun testCurrency() {
        val testPrice = FapiPrice.build(BigDecimal.valueOf(42.5), "KZT")
        assertEquals("42.5 тнг.", testPrice.format())
        assertEquals("от 42.5 тнг.", testPrice.format(isMedicine = true))
    }

    @Test
    fun testPromoBadge() {
        assertEquals(null, FapiUtils.formatPromocodeBadge(null))
        assertEquals(null, FapiUtils.formatPromocodeBadge(emptyList()))
        assertEquals(
            null,
            FapiUtils.formatPromocodeBadge(
                listOf(
                    testPromo(
                        type = FapiPromoType.CASHBACK,
                        discount = FapiPromo.DiscountType.ABSOLUTE,
                        absolute = 12.0
                    )
                )
            )
        )
        assertEquals(
            "12 ₽",
            FapiUtils.formatPromocodeBadge(
                listOf(
                    testPromo(
                        type = FapiPromoType.PROMO_CODE,
                        discount = FapiPromo.DiscountType.ABSOLUTE,
                        absolute = 12.0
                    )
                )
            )
        )
        assertEquals(
            "–42.5%",
            FapiUtils.formatPromocodeBadge(
                listOf(
                    testPromo(
                        type = FapiPromoType.PROMO_CODE,
                        discount = FapiPromo.DiscountType.PERCENT,
                        percent = 42.5
                    )
                )
            )
        )
        assertEquals(
            null,
            FapiUtils.formatPromocodeBadge(
                listOf(
                    testPromo(
                        type = FapiPromoType.PROMO_CODE,
                        discount = FapiPromo.DiscountType.UNKNOWN,
                        percent = 42.5
                    )
                )
            )
        )
    }

    fun testPromo(
        type: FapiPromoType,
        discount: FapiPromo.DiscountType,
        absolute: Double? = null,
        percent: Double? = null
    ): FapiPromo {
        return FapiPromo().also { promo ->
            promo.id = "test"
            promo.type = type
            promo.itemsInfo = FapiPromo.PromoItemsInfo().also { promoInfo ->
                promoInfo.discountType = discount
                if (discount == FapiPromo.DiscountType.PERCENT && percent != null) {
                    promoInfo.discount = FapiPromo.PromoDiscount().also { disc ->
                        disc.value = percent.toBigDecimal().stripTrailingZeros()
                    }
                }
                promoInfo.priceWithTotalDiscount = FapiPromo.PromoPrice().also { ds ->
                    if (absolute != null) {
                        ds.absolute = FapiPrice.build(absolute.toBigDecimal().stripTrailingZeros(), "RUB")
                    }
                    if (percent != null) {
                        // specially ruin, prevent reading from this field
                        ds.percent = (percent + 1).toBigDecimal().stripTrailingZeros()
                    }
                }
            }
        }
    }

    private fun testUrlPicture(url: String): FapiPicture {
        return FapiPicture().apply {
            original = FapiPicture.Data().also {
                it.url = url
            }
        }
    }
}
