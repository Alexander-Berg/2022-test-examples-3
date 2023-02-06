package ru.yandex.market.logistics.mqm.service.processor.aggregationentity

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.converter.AggregationEntityConverter
import ru.yandex.market.logistics.mqm.entity.PlanFact
import ru.yandex.market.logistics.mqm.entity.aggregationentity.AggregationEntity
import ru.yandex.market.logistics.mqm.entity.enums.EntityType
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerType
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.utils.getPreviousSegment
import ru.yandex.market.logistics.mqm.utils.joinInOrder
import java.time.Instant
import java.time.LocalDate

class DropoffScIntakePartnerRelationProducerTest{
    private val converter = AggregationEntityConverter()
    private val producer = DropoffScIntakePartnerRelationProducer(converter)

    @Test
    @DisplayName("Проверка применимости")
    fun isEligible() {
        val planFact = preparePlanFact()
        Assertions.assertThat(producer.isEligible(planFact)).isTrue
    }

    @ParameterizedTest
    @EnumSource(
        value = EntityType::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["LOM_WAYBILL_SEGMENT"]
    )
    @DisplayName("Не агрегируется если план-факт не для сегмента")
    fun isNonEligibleIfNotLomSegment(entityType: EntityType) {
        val planFact = PlanFact(entityType = entityType)
        Assertions.assertThat(producer.isEligible(planFact)).isFalse
    }

    @Test
    @DisplayName("Не агрегируется если предыдущий сегмент не дропофф")
    fun isNonEligibleIfWrongCurrentPartnerType() {
        val planFact = preparePlanFact(isPreviousDropoff = false)
        Assertions.assertThat(producer.isEligible(planFact)).isFalse
    }

    @ParameterizedTest
    @EnumSource(
        value = PartnerType::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["SORTING_CENTER"]
    )
    @DisplayName("Не агрегируется если неправильный тип партнера")
    fun isNonEligibleIfWrongPreviousPartnerType(curPartnerType: PartnerType) {
        val planFact = preparePlanFact(curPartnerType = curPartnerType)
        Assertions.assertThat(producer.isEligible(planFact)).isFalse
    }

    @Test
    @DisplayName("Проверка созданной AggegationEntity")
    fun produceEntity() {
        val planFact = preparePlanFact().apply {
            expectedStatusDatetime = Instant.parse("2021-01-01T08:00:00.00Z")
        }
        val currentSegment = planFact.entity as WaybillSegment
        val previousSegment = currentSegment.getPreviousSegment()
        Assertions.assertThat(producer.produceEntity(planFact)).isEqualTo(
            AggregationEntity(
                date = LocalDate.ofInstant(planFact.expectedStatusDatetime, DateTimeUtils.MOSCOW_ZONE),
                partner = converter.toPartnerAggregationEntity(currentSegment),
                partnerFrom = converter.toPartnerAggregationEntity(previousSegment)
            )
        )
    }

    private fun preparePlanFact(
        isPreviousDropoff: Boolean = true,
        curPartnerType: PartnerType = PartnerType.SORTING_CENTER,
    ): PlanFact {
        val previousSegment = WaybillSegment(
            partnerId = 1L
        )
        val currentSegment = WaybillSegment(
            externalId = "123",
            partnerId = 2L,
            partnerType = curPartnerType
        )
        joinInOrder(listOf(previousSegment, currentSegment))

        if (isPreviousDropoff) {
            previousSegment.partnerType = PartnerType.DELIVERY
            previousSegment.segmentType = SegmentType.SORTING_CENTER
        }

        return PlanFact(entityType = EntityType.LOM_WAYBILL_SEGMENT).apply { entity = currentSegment }
    }
}
