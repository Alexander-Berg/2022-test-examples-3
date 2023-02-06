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
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.utils.getNextSegmentOrNull
import ru.yandex.market.logistics.mqm.utils.joinInOrder
import java.time.Instant
import java.time.LocalDate

class PartnerRelationToAndDate19ProducerTest {

    private val converter = AggregationEntityConverter()
    private val producer = PartnerRelationToAndDate19Producer(converter)

    @Test
    @DisplayName("Проверка успешной применимости для подходящего типа аггрегации")
    fun isEligibleTest() {
        val planFact = preparePlanFact();
        producer.isEligible(planFact) shouldBe true
    }

    @Test
    @DisplayName("Проверка AggregationEntity с датой на текущий день")
    fun createAggregationEntityTodayTest() {
        val planFact = preparePlanFact()
        val waybillSegment = planFact.entity as WaybillSegment
        val nextWaybillSegment = waybillSegment.getNextSegmentOrNull()
        producer.produceEntity(planFact) shouldBe AggregationEntity(
            date = LocalDate.ofInstant(planFact.expectedStatusDatetime, DateTimeUtils.MOSCOW_ZONE),
            partner = converter.toPartnerAggregationEntity(waybillSegment),
            partnerTo = converter.toPartnerAggregationEntity(nextWaybillSegment)
        )
    }

    @Test
    @DisplayName("Проверка AggregationEntity с датой на следующий день")
    fun createAggregationEntityNextDayTest() {
        val planFact = preparePlanFact(
            expectedStatusDateTime = Instant.parse("2021-01-01T16:00:00.00Z")
        )
        val waybillSegment = planFact.entity as WaybillSegment
        val nextWaybillSegment = waybillSegment.getNextSegmentOrNull()
        producer.produceEntity(planFact) shouldBe AggregationEntity(
            date = LocalDate.ofInstant(planFact.expectedStatusDatetime, DateTimeUtils.MOSCOW_ZONE).plusDays(1),
            partner = converter.toPartnerAggregationEntity(waybillSegment),
            partnerTo = converter.toPartnerAggregationEntity(nextWaybillSegment)
        )
    }


    @Test
    @DisplayName("Проверка AggregationEntity без следующего сегмента")
    fun createAggregationNoNextSegmentTest() {
        val planFact = preparePlanFactNoNextSegment()
        producer.produceEntity(planFact) shouldBe null
    }

    private fun preparePlanFact(
        expectedStatusDateTime: Instant = Instant.parse("2021-01-01T08:00:00.00Z")
    ): PlanFact {
        val nextSegment = WaybillSegment(
            partnerId = 1L,
            segmentType = SegmentType.SORTING_CENTER
        )
        val currentSegment = WaybillSegment(
            partnerId = 2L,
            partnerType = PartnerType.FULFILLMENT,
            segmentType = SegmentType.FULFILLMENT
        )
        joinInOrder(listOf(currentSegment, nextSegment))
        return PlanFact(
            entityType = EntityType.LOM_WAYBILL_SEGMENT,
            expectedStatusDatetime = expectedStatusDateTime
        ).apply { entity = currentSegment }
    }

    private fun preparePlanFactNoNextSegment(
        expectedStatusDateTime: Instant = Instant.parse("2021-01-01T08:00:00.00Z")
    ): PlanFact {
        val currentSegment = WaybillSegment(
            partnerId = 2L,
            partnerType = PartnerType.FULFILLMENT,
            segmentType = SegmentType.FULFILLMENT,
        )
        currentSegment.order = LomOrder()
        return PlanFact(
            entityType = EntityType.LOM_WAYBILL_SEGMENT,
            expectedStatusDatetime = expectedStatusDateTime
        ).apply { entity = currentSegment }
    }

}
