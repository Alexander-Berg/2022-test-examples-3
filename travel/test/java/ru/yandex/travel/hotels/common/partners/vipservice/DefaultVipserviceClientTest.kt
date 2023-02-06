package ru.yandex.travel.hotels.common.partners.vipservice

import org.junit.Assert.*
import org.junit.Test
import ru.yandex.travel.commons.proto.ProtoCurrencyUnit
import ru.yandex.travel.hotels.common.partners.vipservice.model.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime


class DefaultVipserviceClientTest {
    private fun load(path: String) = javaClass.classLoader.getResourceAsStream(path)!!

    companion object {
        private val offersReader = DefaultVipserviceClient.mapper.readerFor(Array<Offer>::class.java)
    }

    @Test
    fun `read single offer`() {
        val offers: Array<Offer> = offersReader.readValue(load("vipservice/searchSingleOffer.json"))

        assertArrayEquals(
            arrayOf(
                Offer(
                    hash = "3869_24706_6",
                    currency = ProtoCurrencyUnit.RUB,
                    price = BigDecimal.valueOf(5940),
                    priceBreakdown = mapOf(
                        LocalDate.of(2022, 9, 29) to BigDecimal.valueOf(5940),
                    ),
                    isVatIncluded = false,
                    vatAmount = BigDecimal.ZERO,
                    cityId = 90005,
                    hotelId = 870482,
                    providerId = 17,
                    roomName = "семейный 3-местный",
                    cancelConditions = CancelConditions(
                        freeCancellationBefore = LocalDateTime.of(2022, 9, 28, 12, 0, 0),
                        policies = listOf(
                            CancelConditionsPolicy(
                                penalty = CancelConditionsPolicyPenalty(
                                    amount = BigDecimal.ZERO,
                                    percent = 0,
                                    currencyCode = ProtoCurrencyUnit.RUB,
                                ),
                                startAt = LocalDateTime.of(2022, 7, 27, 14, 54, 8),
                                endAt = LocalDateTime.of(2022, 9, 28, 12, 0, 0),
                            ),
                            CancelConditionsPolicy(
                                penalty = CancelConditionsPolicyPenalty(
                                    amount = BigDecimal.valueOf(5940),
                                    percent = 0,
                                    currencyCode = ProtoCurrencyUnit.RUB,
                                ),
                                startAt = LocalDateTime.of(2022, 9, 28, 12, 0, 0),
                                endAt = null,
                            ),
                        ),
                    ),
                    availableRooms = null,
                    extras = emptyList(),
                    meals = listOf(
                        Meal(
                            currency = ProtoCurrencyUnit.RUB,
                            price = BigDecimal.ZERO,
                            name = "завтрак",
                            code = "1",
                            included = true,
                        )
                    ),
                    description = "",
                    bookingInfo = emptySet(),
                    taxes = null,
                    infoForGuest = "Информируем вас о том, что в соответствии с Постановлением Правительства РФ",
                )
            ),
            offers
        )
    }

    @Test
    fun `read multiple offers`() {
        val offers: Array<Offer> = offersReader.readValue(load("vipservice/searchMultipleOffers.json"))

        assertEquals(34, offers.size)
        assertEquals(
            Offer(
                hash = "1887153",
                currency = ProtoCurrencyUnit.RUB,
                price = BigDecimal.valueOf(3476),
                priceBreakdown = emptyMap(),
                isVatIncluded = false,
                vatAmount = BigDecimal.ZERO,
                cityId = 91276,
                hotelId = 373584,
                providerId = 3,
                roomName = "СТАНДАРТ Эконом",
                availableRooms = 1,
                cancelConditions = CancelConditions(
                    freeCancellationBefore = LocalDateTime.of(2022, 8, 1, 18, 30),
                    policies = listOf(
                        CancelConditionsPolicy(
                            penalty = CancelConditionsPolicyPenalty(
                                amount = BigDecimal.ZERO,
                                percent = 0,
                                currencyCode = ProtoCurrencyUnit.RUB
                            ),
                            startAt = null,
                            endAt = LocalDateTime.of(2022, 8, 1, 18, 30)
                        ),
                        CancelConditionsPolicy(
                            penalty = CancelConditionsPolicyPenalty(
                                amount = BigDecimal.valueOf(1738),
                                percent = 50,
                                currencyCode = ProtoCurrencyUnit.RUB
                            ),
                            startAt = LocalDateTime.of(2022, 8, 1, 18, 30),
                            endAt = null
                        )
                    )
                ),
                extras = emptyList(),
                meals = emptyList(),
                description = "",
                bookingInfo = null,
                taxes = null,
                infoForGuest = null,
            ),
            offers[0]
        )
    }
}
