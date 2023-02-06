package ru.yandex.market.logistics.mqm.service.processor.aggregationentity

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.converter.AggregationEntityConverter
import ru.yandex.market.logistics.mqm.entity.PlanFact
import ru.yandex.market.logistics.mqm.entity.aggregationentity.AggregationEntity
import ru.yandex.market.logistics.mqm.entity.enums.EntityType
import ru.yandex.market.logistics.mqm.entity.lom.LomOrder
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerType
import ru.yandex.market.logistics.mqm.utils.getPreviousSegment
import ru.yandex.market.logistics.mqm.utils.joinInOrder
import java.time.Instant
import java.time.LocalDate

class PartnerFromAndDateProducerTest {

    val converter = AggregationEntityConverter()
    val producer = PartnerFromAndDateProducer(converter)

    @Test
    @DisplayName("Проверка применимости")
    fun isEligible() {
        val planFact = preparePlanFact()
        producer.isEligible(planFact) shouldBe true
    }

    @Test
    @DisplayName("Проверка на корректность AggregationEntity для текущего дня.")
    fun aggregationEntity() {
        val planFact = preparePlanFact()
        val waybillSegment = planFact.entity as WaybillSegment
        val previousWaybillSegment = waybillSegment.getPreviousSegment()

        producer.produceEntity(planFact) shouldBe AggregationEntity(
            date = LocalDate.ofInstant(planFact.expectedStatusDatetime, DateTimeUtils.MOSCOW_ZONE),
            partnerFrom = converter.toPartnerAggregationEntity(previousWaybillSegment)
        )
    }

    @Test
    @DisplayName("Проверка что AggregationEntity без предшествующего сегмента будет null.")
    fun aggregationEntityIsNullIfNoPrevSegment() {
        val planFact = preparePlanFactNoPrevSegment()
        producer.produceEntity(planFact) shouldBe null
    }


    private fun preparePlanFact(
        currentPartnerType: PartnerType = PartnerType.DELIVERY,
        previousSegment: WaybillSegment = WaybillSegment(partnerType = PartnerType.FULFILLMENT),
        expectedTime: Instant = Instant.parse("2021-01-01T12:59:00.00Z")
    ): PlanFact {
        val currentSegment = WaybillSegment(
            partnerType = currentPartnerType,
        )
        joinInOrder(listOf(previousSegment, currentSegment))
        val planfact = PlanFact(
            entityType = EntityType.LOM_WAYBILL_SEGMENT,
            expectedStatusDatetime = expectedTime
        )
        planfact.entity = currentSegment
        return planfact
    }

    private fun preparePlanFactNoPrevSegment(): PlanFact {
        val currentSegment = WaybillSegment(
            partnerType = PartnerType.DELIVERY,
        )
        currentSegment.order = LomOrder()
        val planfact = PlanFact(entityType = EntityType.LOM_WAYBILL_SEGMENT)
        planfact.entity = currentSegment
        return planfact
    }
}
