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
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerSubtype
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerType
import ru.yandex.market.logistics.mqm.utils.getPreviousSegment
import ru.yandex.market.logistics.mqm.utils.joinInOrder
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import ru.yandex.market.logistics.mqm.utils.toInstant

class FfScIntakeProducerTest {

    private val converter = AggregationEntityConverter()
    private val producer = FfScIntakeProducer(converter)

    @Test
    @DisplayName("Проверка применимости")
    fun isEligible() {
        val planFact = preparePlanFact()
        producer.isEligible(planFact) shouldBe true
    }

    @ParameterizedTest
    @EnumSource(
        value = PartnerType::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["SORTING_CENTER"]
    )
    @DisplayName("Не агрегируется для планфактов с неподходящим типом партнеров для текущего сегмента.")
    fun isNonEligibleWhenCurrentSegmentWrongPartnerType(unsupportedPartnerType: PartnerType) {
        val planFact = preparePlanFact(currentPartnerType = unsupportedPartnerType)
        producer.isEligible(planFact) shouldBe false
    }


    @ParameterizedTest
    @EnumSource(
        value = PartnerType::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["FULFILLMENT"]
    )
    @DisplayName("Не агрегируется для планфактов с неподходящим типом партнеров для предыдущего сегмента.")
    fun isNonEligibleWhenPreviousSegmentWrongPartnerType(unsupportedPartnerType: PartnerType) {
        val planFact = preparePlanFact(previousSegment = WaybillSegment(partnerType = unsupportedPartnerType))
        producer.isEligible(planFact) shouldBe false
    }

    @ParameterizedTest
    @EnumSource(
        value = PartnerSubtype::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["MARKET_COURIER"]
    )
    @DisplayName("Не агрегируется для планфактов сподтипом партнера для текущего сегмента MARKET_COURIER.")
    fun isNotEligibleWhenWrongPartnerSubtype(partnerSubtype: PartnerSubtype) {
        val planFact = preparePlanFact(currentPartnerSubtype = partnerSubtype)
        producer.isEligible(planFact) shouldBe true
    }

    @Test
    @DisplayName("Не агрегируется для планфактов без предыдущего сегмента.")
    fun isNotEligibleWhenNoPreviousSegment() {
        val planFact = preparePlanFactNoPrevSegment()
        producer.isEligible(planFact) shouldBe false
    }

    @Test
    @DisplayName("Проверка созданного AggregationEntity в текущий день до 13")
    fun produceEntityCurrentDayBefore13() {
        val testDate = LocalDate.of(2021, 10, 10)
        val testTime = LocalTime.of(12, 59)
        val planFact = preparePlanFact(expectedTime = LocalDateTime.of(testDate, testTime).toInstant())
        val waybillSegment = planFact.entity as WaybillSegment
        val previousWaybillSegment = waybillSegment.getPreviousSegment()

        producer.produceEntity(planFact) shouldBe AggregationEntity(
            date = testDate,
            time = LocalTime.of(13, 0),
            partner = converter.toPartnerAggregationEntity(waybillSegment),
            partnerFrom = converter.toPartnerAggregationEntity(previousWaybillSegment)
        )
    }

    @Test
    @DisplayName("Проверка созданного AggregationEntity в текущий день между 13 и 17")
    fun produceEntityCurrentDayBetween13And17() {
        val testDate = LocalDate.of(2021, 10, 10)
        val testTime = LocalTime.of(16, 59)
        val planFact = preparePlanFact(expectedTime = LocalDateTime.of(testDate, testTime).toInstant())
        val waybillSegment = planFact.entity as WaybillSegment
        val previousWaybillSegment = waybillSegment.getPreviousSegment()

        producer.produceEntity(planFact) shouldBe AggregationEntity(
            date = testDate,
            time = LocalTime.of(17, 0),
            partner = converter.toPartnerAggregationEntity(waybillSegment),
            partnerFrom = converter.toPartnerAggregationEntity(previousWaybillSegment)
        )
    }

    @Test
    @DisplayName("Проверка созданного AggregationEntity на следующий день.")
    fun produceEntityNextDay() {
        val testDate = LocalDate.of(2021, 10, 10)
        val testTime = LocalTime.of(17, 1)
        val planFact = preparePlanFact(expectedTime = LocalDateTime.of(testDate, testTime).toInstant())
        val waybillSegment = planFact.entity as WaybillSegment
        val previousWaybillSegment = waybillSegment.getPreviousSegment()

        producer.produceEntity(planFact) shouldBe AggregationEntity(
            date = testDate.plusDays(1),
            time = LocalTime.of(13, 0),
            partner = converter.toPartnerAggregationEntity(waybillSegment),
            partnerFrom = converter.toPartnerAggregationEntity(previousWaybillSegment)
        )
    }

    private fun preparePlanFact(
        currentPartnerType: PartnerType = PartnerType.SORTING_CENTER,
        currentPartnerSubtype: PartnerSubtype = PartnerSubtype.PARTNER_CONTRACT_DELIVERY,
        previousSegment: WaybillSegment = WaybillSegment(partnerType = PartnerType.FULFILLMENT),
        expectedTime: Instant = Instant.parse("2021-01-01T12:59:00.00Z")
    ): PlanFact {
        val currentSegment = WaybillSegment(
            partnerType = currentPartnerType,
            partnerSubtype = currentPartnerSubtype,
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
            partnerType = PartnerType.SORTING_CENTER,
        )
        currentSegment.order = LomOrder()
        val planfact = PlanFact(entityType = EntityType.LOM_WAYBILL_SEGMENT)
        planfact.entity = currentSegment
        return planfact
    }
}
