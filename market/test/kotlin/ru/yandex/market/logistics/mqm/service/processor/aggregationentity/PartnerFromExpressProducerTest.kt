package ru.yandex.market.logistics.mqm.service.processor.aggregationentity

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import ru.yandex.market.logistics.mqm.converter.AggregationEntityConverter
import ru.yandex.market.logistics.mqm.entity.PlanFact
import ru.yandex.market.logistics.mqm.entity.aggregationentity.AggregationEntity
import ru.yandex.market.logistics.mqm.entity.enums.EntityType
import ru.yandex.market.logistics.mqm.entity.lom.Address
import ru.yandex.market.logistics.mqm.entity.lom.Location
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.embedded.WaybillShipment
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerSubtype
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.entity.lom.enums.WaybillSegmentTag
import ru.yandex.market.logistics.mqm.utils.joinInOrder

class PartnerFromExpressProducerTest {

    private val converter = AggregationEntityConverter()
    private val producer = PartnerFromExpressProducer(converter)

    @Test
    @DisplayName("Проверка применимости")
    fun isEligible() {
        val planFact = createPlanFact()
        producer.isEligible(planFact) shouldBe true
    }

    @Test
    @DisplayName("Проверка, что группировка не применятся, если это не экспресс")
    fun isNonEligibleWithoutPreviousSegment() {
        val planFact = createPlanFact(isFromExpress = false)
        producer.isEligible(planFact) shouldBe false
    }

    @Test
    @DisplayName("Проверка, что группировка не применятся без предыдущего сегмета")
    fun isNonEligibleNotFromExpress() {
        val planFact = createPlanFact(addPreviousSegment = false)
        producer.isEligible(planFact) shouldBe false
    }

    @Test
    @DisplayName("Проверка полученной AggregationEntity")
    fun produceEntity() {
        val previousSegment = WaybillSegment(
            shipment = WaybillShipment(locationFrom = Location(address = Address(
                settlement = "Париж",
                street = "улица",
                house = "1",
                building = "2",
                housing = "3"
            )))
        )
        val currentSegment = WaybillSegment(
            segmentType = SegmentType.COURIER,
            waybillSegmentTags = mutableSetOf(WaybillSegmentTag.CALL_COURIER),
        )
        val planFact = createPlanFactWithPreviousSegment(currentSegment, previousSegment)
        val expectedAggregationEntity = AggregationEntity(
            partner = converter.toPartnerAggregationEntity(currentSegment),
            partnerFrom = converter.toPartnerAggregationEntity(previousSegment),
        )
        producer.produceEntity(planFact) shouldBe expectedAggregationEntity

    }

    private fun createPlanFact(
        isFromExpress: Boolean = true,
        addPreviousSegment: Boolean = true
    ): PlanFact {
        val currentSegment = WaybillSegment(segmentType = SegmentType.COURIER)
        currentSegment.partnerSubtype = PartnerSubtype.MARKET_COURIER
        if (isFromExpress) {
            currentSegment.waybillSegmentTags = mutableSetOf(WaybillSegmentTag.CALL_COURIER)
        }
        if (addPreviousSegment) {
            val previousSegment = WaybillSegment()
            joinInOrder(listOf(previousSegment, currentSegment))
        } else {
            joinInOrder(listOf(currentSegment))
        }
        val planFact = PlanFact(entityType = EntityType.LOM_WAYBILL_SEGMENT)
        planFact.entity = currentSegment
        return planFact
    }

    private fun createPlanFactWithPreviousSegment(
        current: WaybillSegment,
        previous: WaybillSegment
    ): PlanFact {
        joinInOrder(listOf(previous, current))
        val planFact = PlanFact(entityType = EntityType.LOM_WAYBILL_SEGMENT)
        planFact.entity = current
        return planFact
    }
}
