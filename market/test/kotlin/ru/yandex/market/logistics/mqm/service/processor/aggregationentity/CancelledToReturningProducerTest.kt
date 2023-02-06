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
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegmentStatusHistory
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerType
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentStatus
import ru.yandex.market.logistics.mqm.service.processor.planfact.order.CancelledToReturningLomOrderPlanFactProcessor
import ru.yandex.market.logistics.mqm.utils.joinInOrder
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

class CancelledToReturningProducerTest {
    private val converter = AggregationEntityConverter()
    private val producer = CancelledToReturningProducer(converter)

    @Test
    @DisplayName("Проверка применимости")
    fun isEligible() {
        val planFact = preparePlanFact()
        producer.isEligible(planFact) shouldBe true
    }

    @Test
    @DisplayName("Проверка созданной AggregationEntity")
    fun produceEntity() {
        val planFact = preparePlanFact()
        val order = planFact.entity as LomOrder

        val expectedAggregationEntity = AggregationEntity(
            date = LocalDate.ofInstant(EXPECTED_TIME, DateTimeUtils.MOSCOW_ZONE),
            partner = converter.toPartnerAggregationEntity(order.waybill[0]),
            partnerTo = converter.toPartnerAggregationEntity(order.waybill[1]),
        )
        producer.produceEntity(planFact) shouldBe expectedAggregationEntity
    }

    @Test
    @DisplayName("Проверка переноса времени на следующий день, если время плана больше 10ч")
    fun produceNextDayEntity() {
        val planFact = preparePlanFact(expectedDateTime = EXPECTED_TIME.plus(Duration.ofHours(2)))
        val order = planFact.entity as LomOrder

        val expectedAggregationEntity = AggregationEntity(
            date = LocalDate.ofInstant(EXPECTED_TIME, DateTimeUtils.MOSCOW_ZONE).plusDays(1),
            partner = converter.toPartnerAggregationEntity(order.waybill[0]),
            partnerTo = converter.toPartnerAggregationEntity(order.waybill[1]),
        )
        producer.produceEntity(planFact) shouldBe expectedAggregationEntity
    }

    @Test
    @DisplayName("Не переносить на следующий день, если время плана равно 10ч")
    fun produceTodayEntityIfPlan10() {
        val planFact = preparePlanFact(expectedDateTime = Instant.parse("2021-12-22T07:00:00.00Z"))
        val order = planFact.entity as LomOrder

        val expectedAggregationEntity = AggregationEntity(
            date = LocalDate.ofInstant(EXPECTED_TIME, DateTimeUtils.MOSCOW_ZONE),
            partner = converter.toPartnerAggregationEntity(order.waybill[0]),
            partnerTo = converter.toPartnerAggregationEntity(order.waybill[1]),
        )
        producer.produceEntity(planFact) shouldBe expectedAggregationEntity
    }

    private fun preparePlanFact(
        expectedDateTime: Instant = EXPECTED_TIME,
        order: LomOrder = mockOrder()
    ): PlanFact {
        return PlanFact(
            entityType = EntityType.LOM_ORDER,
            producerName = CancelledToReturningLomOrderPlanFactProcessor::class.simpleName!!,
            expectedStatusDatetime = expectedDateTime,
        ).apply { entity = order }
    }

    private fun mockOrder(): LomOrder {
        val firstSegment = mockSegment(
            1,
            listOf(
                Instant.parse("2021-12-19T12:00:00.00Z"),
                Instant.parse("2021-12-20T13:00:00.00Z"),
                Instant.parse("2021-12-21T12:00:00.00Z"),
            )
        )
        val secondSegment = mockSegment(
            2,
            listOf(
                Instant.parse("2021-12-20T12:00:00.00Z"),
                Instant.parse("2021-12-20T13:00:00.00Z"),
                Instant.parse("2021-12-20T14:00:00.00Z"),
            )
        )
        val lateReturnPrepared = WaybillSegmentStatusHistory(
            date = Instant.parse("2021-12-22T14:00:00.00Z"),
            status = SegmentStatus.RETURN_PREPARING,
        )
            .apply { this.waybillSegment = secondSegment }
        secondSegment.waybillSegmentStatusHistory.add(lateReturnPrepared)
        return joinInOrder(listOf(firstSegment, secondSegment))
    }

    private fun mockSegment(
        partnerId: Long,
        statusDates: List<Instant>
    ): WaybillSegment {
        val waybillSegment = WaybillSegment(
            partnerId = partnerId,
            partnerType = PartnerType.DELIVERY,
            partnerName = "Partner$partnerId",
        )
        val segmentStatuses = statusDates.map {
            WaybillSegmentStatusHistory(date = it)
                .apply { this.waybillSegment = waybillSegment }
        }
        waybillSegment.waybillSegmentStatusHistory.addAll(segmentStatuses)
        return waybillSegment
    }

    companion object {
        private val EXPECTED_TIME = Instant.parse("2021-12-22T06:00:00.00Z")
    }
}
