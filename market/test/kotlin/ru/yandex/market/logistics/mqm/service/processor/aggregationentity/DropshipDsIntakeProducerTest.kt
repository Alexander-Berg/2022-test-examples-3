package ru.yandex.market.logistics.mqm.service.processor.aggregationentity

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.converter.AggregationEntityConverter
import ru.yandex.market.logistics.mqm.entity.PlanFact
import ru.yandex.market.logistics.mqm.entity.aggregationentity.AggregationEntity
import ru.yandex.market.logistics.mqm.entity.enums.EntityType
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerType
import ru.yandex.market.logistics.mqm.utils.getPreviousSegment
import ru.yandex.market.logistics.mqm.utils.joinInOrder
import java.time.Instant
import java.time.LocalDate

class DropshipDsIntakeProducerTest {

    private val converter = AggregationEntityConverter()
    private val producer = DropshipDsIntakeProducer(converter)

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
        val previousWaybillSegment = waybillSegment.getPreviousSegment()

        producer.produceEntity(planFact) shouldBe AggregationEntity(
            date = LocalDate.ofInstant(planFact.expectedStatusDatetime, DateTimeUtils.MOSCOW_ZONE),
            partner = converter.toPartnerAggregationEntity(waybillSegment),
            locationFrom = converter.toLocationAggregationEntity(previousWaybillSegment.shipment.locationFrom)
        )
    }

    @Test
    @DisplayName("Проверка AggregationEntity с датой на следующий день")
    fun createAggregationEntityNextDayTest() {
        val planFact = preparePlanFact(
            expectedStatusDateTime = Instant.parse("2021-01-01T16:00:00.00Z")
        )
        val waybillSegment = planFact.entity as WaybillSegment
        val previousWaybillSegment = waybillSegment.getPreviousSegment()

        producer.produceEntity(planFact) shouldBe AggregationEntity(
            date = LocalDate.ofInstant(planFact.expectedStatusDatetime, DateTimeUtils.MOSCOW_ZONE).plusDays(1),
            partner = converter.toPartnerAggregationEntity(waybillSegment),
            locationFrom = converter.toLocationAggregationEntity(previousWaybillSegment.shipment.locationFrom)
        )
    }

    private fun preparePlanFact(
        expectedStatusDateTime: Instant = Instant.parse("2021-01-01T08:00:00.00Z"),
        currentSegment: WaybillSegment = WaybillSegment(
            partnerId = 2L,
            partnerType = PartnerType.DELIVERY,
        ),
    ): PlanFact {
        val previousSegment = WaybillSegment(
            partnerId = 1L,
            partnerType = PartnerType.DROPSHIP,
        )
        joinInOrder(listOf(previousSegment, currentSegment))
        return PlanFact(
            entityType = EntityType.LOM_WAYBILL_SEGMENT,
            expectedStatusDatetime = expectedStatusDateTime
        ).apply { entity = currentSegment }
    }

}
