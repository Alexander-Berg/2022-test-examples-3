package ru.yandex.market.mapi.client.fapi.util

import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.client.fapi.enums.DeliveryPartnerType
import ru.yandex.market.mapi.client.fapi.enums.FapiPromoType
import ru.yandex.market.mapi.client.fapi.enums.OfferSpecsType
import ru.yandex.market.mapi.client.fapi.enums.ServiceColor
import ru.yandex.market.mapi.client.fapi.model.FapiDiscount
import ru.yandex.market.mapi.client.fapi.model.FapiOffer
import ru.yandex.market.mapi.client.fapi.model.FapiOfferDelivery
import ru.yandex.market.mapi.client.fapi.model.FapiPrice
import ru.yandex.market.mapi.client.fapi.model.FapiPromo
import ru.yandex.market.mapi.client.fapi.model.FapiTitles
import java.math.BigDecimal
import kotlin.test.assertEquals

/**
 * @author Ilya Kislitsyn / ilyakis@ / 30.03.2022
 */
class OfferUtilsTest {

    @Test
    fun testIsMedical() {
        // various non-medical cases
        assertEquals(false, testOffer().isMedical())
        assertEquals(false, testOffer(specs = testSpecs(null)).isMedical())
        assertEquals(false, testOffer(specs = testSpecs(emptyList())).isMedical())
        assertEquals(
            false,
            testOffer(
                specs = testSpecs(listOf(OfferSpecsType.UNKNOWN))
            ).isMedical()
        )

        // check any medical spec
        assertEquals(
            true,
            testOffer(
                specs = testSpecs(listOf(OfferSpecsType.MEDICINE))
            ).isMedical()
        )
    }

    @Test
    fun testDsbs() {
        assertEquals(
            true,
            testOffer(
                offerColor = ServiceColor.WHITE,
                isFulfilment = false,
                delivery = testDelivery(
                    partnerTypes = arrayListOf(DeliveryPartnerType.SHOP.code)
                )
            ).isDsbs()
        )

        assertEquals(
            false,
            testOffer(
                offerColor = ServiceColor.BLUE,
                isFulfilment = true,
                delivery = testDelivery(
                    partnerTypes = arrayListOf(DeliveryPartnerType.YANDEX_MARKET.code)
                )
            ).isDsbs()
        )
    }

    @Test
    fun testClickAndCollect() {
        assertEquals(
            true,
            testOffer(
                offerColor = ServiceColor.BLUE,
                isFulfilment = false,
                delivery = testDelivery(
                    partnerTypes = arrayListOf(DeliveryPartnerType.SHOP.code)
                )
            ).isClickAndCollect()
        )

        assertEquals(
            false,
            testOffer(
                offerColor = ServiceColor.WHITE,
                isFulfilment = true,
                delivery = testDelivery(
                    partnerTypes = arrayListOf(DeliveryPartnerType.YANDEX_MARKET.code)
                )
            ).isClickAndCollect()
        )
    }

    @Test
    fun testFormatDeliveryText() {
        assertEquals(null, testOffer().formatDeliveryText())
        assertEquals(null, testOffer(delivery = testDelivery()).formatDeliveryText())
        assertEquals(
            null, testOffer(
                delivery = testDelivery(
                    options = emptyList()
                )
            ).formatDeliveryText()
        )

        // non-express - ignore
        assertEquals(
            null, testOffer(
                delivery = testDelivery(
                    options = listOf(
                        FapiOfferDelivery.DeliveryOption().apply {
                            dayFrom = 1
                            dayTo = 1
                        }
                    )
                )
            ).formatDeliveryText()
        )

        assertEquals(
            "завтра", testOffer(
                delivery = testDelivery(
                    isExpress = true,
                    options = listOf(
                        FapiOfferDelivery.DeliveryOption().apply {
                            dayFrom = 1
                            dayTo = 1
                        }
                    )
                )
            ).formatDeliveryText()
        )

        assertEquals(
            "от 1 часа", testOffer(
                delivery = testDelivery(
                    isExpress = true,
                    options = listOf(
                        FapiOfferDelivery.DeliveryOption().apply {
                            dayFrom = 0
                            dayTo = 0
                        }
                    )
                )
            ).formatDeliveryText()
        )

        assertEquals(
            "с some text", testOffer(
                delivery = testDelivery(
                    isExpress = true,
                    options = listOf(
                        FapiOfferDelivery.DeliveryOption().apply {
                            dayFrom = 0
                            dayTo = 0
                            timeIntervals = listOf(
                                FapiOfferDelivery.TimeInterval().apply { from = "some text" }
                            )
                        }
                    )
                )
            ).formatDeliveryText()
        )
    }

    private fun testSpecs(items: List<OfferSpecsType>?): FapiOffer.Specs {
        if (items == null) {
            return FapiOffer.Specs()
        }

        return FapiOffer.Specs().apply {
            internal = items.map { specValue ->
                FapiOffer.SpecsInternal().apply { value = specValue.code }
            }
        }
    }

    companion object {
        @JvmStatic
        fun testOffer(
            offerColor: ServiceColor = ServiceColor.BLUE,
            isFulfilment: Boolean = false,
            discount: FapiDiscount? = null,
            promos: List<FapiPromo>? = null,
            delivery: FapiOfferDelivery? = null,
            specs: FapiOffer.Specs? = null,
        ): FapiOffer {
            return FapiOffer().also { offer ->
                offer.id = "offer-test-id"
                offer.offerColor = offerColor
                offer.isFulfilment = isFulfilment
                offer.titles = FapiTitles().apply { raw = "test" }
                offer.price = testPrice()

                offer.discount = discount
                offer.promos = promos
                offer.delivery = delivery
                offer.specs = specs
            }
        }

        @JvmStatic
        public fun testDelivery(
            partnerTypes: List<String>? = null,
            options: List<FapiOfferDelivery.DeliveryOption>? = null,
            courierOptions: List<FapiOfferDelivery.CourierOption>? = null,
            pickupOptions: List<FapiOfferDelivery.PickupOption>? = null,
            postStats: FapiOfferDelivery.PostStats? = null,
            onDemandStats: FapiOfferDelivery.OnDemandStats? = null,
            isExpress: Boolean = false,
            isEda: Boolean = false,
            isCourierAvailable: Boolean = false,
            isDownloadable: Boolean = false,
            inStock: Boolean = false,
        ): FapiOfferDelivery {
            return FapiOfferDelivery().also {
                it.partnerTypes = partnerTypes
                it.options = options
                it.courierOptions = courierOptions
                it.pickupOptions = pickupOptions
                it.postStats = postStats
                it.onDemandStats = onDemandStats
                it.isExpress = isExpress
                it.isEda = isEda
                it.isCourierAvailable = isCourierAvailable
                it.isDownloadable = isDownloadable
                it.inStock = inStock
            }
        }

        @JvmStatic
        public fun testPrice(value: Double = 1.0): FapiPrice {
            return FapiPrice.build(BigDecimal.valueOf(value).stripTrailingZeros(), "RUB")
        }

        public fun testPromo(
            type: FapiPromoType,
            isPersonal: Boolean = false,
        ): FapiPromo {
            return FapiPromo().also {
                it.type = type
                it.isPersonal = isPersonal
            }
        }
    }
}
