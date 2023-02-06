package ru.yandex.market.logistics.mqm.converter

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.junit.Test
import ru.yandex.market.logistics.mqm.entity.aggregationentity.PartnerAggregationEntity
import ru.yandex.market.logistics.mqm.entity.lom.Address
import ru.yandex.market.logistics.mqm.entity.lom.Location
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.embedded.WaybillShipment


class AggregationEntityConverterTest {

    private val converter = AggregationEntityConverter()

    @Test
    fun toPartnerAggregationEntityIfNoLocation() {
        assertSoftly {
            converter.toPartnerAggregationEntity(
                WaybillSegment(shipment = WaybillShipment())
            ) shouldBe PartnerAggregationEntity()
        }
    }

    @Test
    fun toPartnerAggregationEntityIfNoAddress() {
        assertSoftly {
            converter.toPartnerAggregationEntity(
                WaybillSegment(shipment = WaybillShipment(locationFrom = Location()))
            ) shouldBe PartnerAggregationEntity()
        }
    }

    @Test
    fun toPartnerAggregationEntity() {
        assertSoftly {
            converter.toPartnerAggregationEntity(segmentWithAddress(Address())) shouldBe
                PartnerAggregationEntity(addressFrom = "")

            converter.toPartnerAggregationEntity(
                segmentWithAddress(
                    Address(
                        country = "Россия",
                        settlement = "Москва",
                    )
                )
            ) shouldBe PartnerAggregationEntity(
                addressFrom = "Россия, Москва"
            )

            converter.toPartnerAggregationEntity(
                segmentWithAddress(
                    Address(
                        country = "Россия",
                        settlement = "Москва",
                        street = "Новинский бульвар",
                    )
                )
            ) shouldBe PartnerAggregationEntity(
                addressFrom = "Россия, Москва, Новинский бульвар"
            )

            converter.toPartnerAggregationEntity(
                segmentWithAddress(
                    Address(
                        country = "Россия",
                        settlement = "Москва",
                        street = "Новинский бульвар",
                        house = "8"
                    )
                )
            ) shouldBe PartnerAggregationEntity(
                addressFrom = "Россия, Москва, Новинский бульвар, 8"
            )

            converter.toPartnerAggregationEntity(
                segmentWithAddress(
                    Address(
                        country = "Россия",
                        settlement = "Москва",
                        street = "Новинский бульвар",
                        house = "8",
                        building = "1",
                        housing = "2"
                    )
                )
            ) shouldBe PartnerAggregationEntity(
                addressFrom = "Россия, Москва, Новинский бульвар, 8, cт 1, к 2"
            )
        }
    }

    private fun segmentWithAddress(address: Address) = WaybillSegment(
        shipment = WaybillShipment(
            locationFrom = Location(
                address = address
            )
        )
    )
}
