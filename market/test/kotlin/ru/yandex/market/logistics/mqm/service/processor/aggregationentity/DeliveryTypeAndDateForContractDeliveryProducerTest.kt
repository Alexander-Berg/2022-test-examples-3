package ru.yandex.market.logistics.mqm.service.processor.aggregationentity

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.converter.AggregationEntityConverter
import ru.yandex.market.logistics.mqm.entity.PlanFact
import ru.yandex.market.logistics.mqm.entity.aggregationentity.AggregationEntity
import ru.yandex.market.logistics.mqm.entity.enums.EntityType
import ru.yandex.market.logistics.mqm.entity.lom.LomOrder
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.enums.DeliveryType
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerSubtype
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerType
import ru.yandex.market.logistics.mqm.entity.lom.enums.PlatformClient
import java.time.Instant
import java.time.LocalDate

class DeliveryTypeAndDateForContractDeliveryProducerTest {

    private val converter = AggregationEntityConverter()
    private val producer = DeliveryTypeAndDateForContractDeliveryProducer(converter)

    @Test
    @DisplayName("Проверка применимости")
    fun isEligible() {
        val planFact = preparePlanFact()
        producer.isEligible(planFact) shouldBe true
    }

    @ParameterizedTest
    @EnumSource(
        value = PartnerSubtype::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["PARTNER_CONTRACT_DELIVERY"]
    )
    @DisplayName("Проверка, что группировка не применяется для неподходящих PartnerSubtype")
    fun isNonEligibleIfWrongPartnerSubtype(unsupportedPartnerSubtype: PartnerSubtype) {
        val planFact = preparePlanFact(currentPartnerSubtype = unsupportedPartnerSubtype)
        producer.isEligible(planFact) shouldBe false
    }

    @Test
    @DisplayName("Проверка создания валидного AggregationEntity")
    fun validAgggregationEntity() {
        val planFact = preparePlanFact()
        val waybillSegment = planFact.entity as? WaybillSegment
        val order = waybillSegment?.order

        producer.produceEntity(planFact) shouldBe AggregationEntity(
            partner = converter.toPartnerAggregationEntity(waybillSegment),
            date = LocalDate.ofInstant(planFact.expectedStatusDatetime, DateTimeUtils.MOSCOW_ZONE),
            deliveryType = order?.deliveryType
        )
    }

    @Test
    @DisplayName("Проверка создания валидного AggregationEntity при отсутствующем поле LomOrder")
    fun validAggregationEntityIfNoLomOrder() {
        val planFact = preparePlanFact(lomOrder = null)
        val waybillSegment = planFact.entity as? WaybillSegment

        producer.produceEntity(planFact) shouldBe AggregationEntity(
            partner = converter.toPartnerAggregationEntity(waybillSegment),
            date = LocalDate.ofInstant(planFact.expectedStatusDatetime, DateTimeUtils.MOSCOW_ZONE),
            deliveryType = null
        )
    }

    private fun preparePlanFact(
        currentPartnerType: PartnerType = PartnerType.DELIVERY,
        currentPartnerSubtype: PartnerSubtype = PartnerSubtype.PARTNER_CONTRACT_DELIVERY,
        lomOrder: LomOrder? = LomOrder(platformClientId = PlatformClient.BERU.id, deliveryType = DeliveryType.COURIER),
        expectedTime: Instant = Instant.parse("2021-01-01T12:59:00.00Z")
    ): PlanFact {
        val currentSegment = WaybillSegment(
            partnerType = currentPartnerType,
            partnerSubtype = currentPartnerSubtype
        )
        if (lomOrder != null) {
            currentSegment.order = lomOrder
        }
        val planfact = PlanFact(
            entityType = EntityType.LOM_WAYBILL_SEGMENT,
            expectedStatusDatetime = expectedTime
        )
        planfact.entity = currentSegment
        return planfact
    }

}
