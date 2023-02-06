package ru.yandex.market.logistics.mqm.entity.aggregationentity

import java.time.LocalDate
import java.time.LocalTime
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import ru.yandex.market.logistics.mqm.entity.lom.enums.DeliveryType
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerSubtype
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerType
import ru.yandex.market.logistics.mqm.entity.lom.enums.PlatformClient

class AggregationEntityTest {
    @Test
    @DisplayName("Проверка применимости, если заполнено все")
    fun toKey() {
        val entity = AggregationEntity(
            date = LocalDate.of(2021, 9, 12),
            time = LocalTime.of(17, 6, 30, 21),
            order = OrderAggregationEntity(barcode = "test_barcode", directFlow = true),
            partner = PartnerAggregationEntity(
                id = 1,
                name = "test_partner_name",
                type = PartnerType.DROPSHIP,
                subtype = PartnerSubtype.MARKET_COURIER,
                dropshipExpress = true,
            ),
            partnerFrom = PartnerAggregationEntity(
                id = 2,
                name = "test_partner_from_name",
                type = PartnerType.DELIVERY,
                subtype = PartnerSubtype.GO_PLATFORM,
                dropshipExpress = true,
            ),
            partnerTo = PartnerAggregationEntity(
                id = 3,
                name = "test_partner_to_name",
                type = PartnerType.SORTING_CENTER,
                subtype = PartnerSubtype.MARKET_COURIER,
                dropshipExpress = true,
            ),
            locationFrom = LocationAggregationEntity(region = "test_from_region", address = "test_from_address"),
            locationTo = LocationAggregationEntity(region = "test_to_region", address = "test_to_address"),
            partnerSubtype = PartnerSubtype.TAXI_AIR,
            deliveryType = DeliveryType.COURIER,
            clientReturnAdditionalDataType = "test_client_return",
            producerName = "test_producer",
            platformClient = PlatformClient.YANDEX_GO.id
        )
        Assertions.assertThat(entity.toKey()).isEqualTo(
            "date:${entity.date};" +
                "time:${entity.time};" +
                "order:${entity.order!!.toKey()};" +
                "partner:${entity.partner!!.toKey()};" +
                "partnerFrom:${entity.partnerFrom!!.toKey()};" +
                "partnerTo:${entity.partnerTo!!.toKey()};" +
                "locationFrom:${entity.locationFrom!!.toKey()};" +
                "locationTo:${entity.locationTo!!.toKey()};" +
                "partnerSubtype:${entity.partnerSubtype};" +
                "deliveryType:${entity.deliveryType};" +
                "clientReturnAdditionalDataType:${entity.clientReturnAdditionalDataType};" +
                "producerName:${entity.producerName};" +
                "platformClient:${entity.platformClient};"
        )
    }

    @Test
    @DisplayName("Проверка применимости, если заполнен только locationTo")
    fun toKeyOnlyLocationTo() {
        val entity = AggregationEntity(
            locationTo = LocationAggregationEntity(region = "test_to_region", address = "test_to_address")
        )
        Assertions.assertThat(entity.toKey()).isEqualTo(
            "locationTo:${entity.locationTo!!.toKey()};"
        )
    }
}
