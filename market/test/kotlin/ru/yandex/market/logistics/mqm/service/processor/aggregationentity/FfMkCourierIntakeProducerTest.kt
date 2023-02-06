package ru.yandex.market.logistics.mqm.service.processor.aggregationentity

import org.assertj.core.api.Assertions
import org.joda.time.Hours
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
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerSubtype
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerType
import ru.yandex.market.logistics.mqm.utils.joinInOrder
import java.time.Instant
import java.time.LocalDate

class FfMkCourierIntakeProducerTest {

    var converter = AggregationEntityConverter()
    var producer = FfMkCourierIntakeProducer(converter)

    @Test
    @DisplayName("Проверка применимости")
    fun isEligible() {
        val planFact = mockMKPlanFactWithPreviousSegment(
            PartnerType.FULFILLMENT,
            PartnerType.DELIVERY,
            PartnerSubtype.MARKET_COURIER
        )
        Assertions.assertThat(
            producer.isEligible(planFact)
        ).isTrue
    }

    @ParameterizedTest
    @EnumSource(
        value = EntityType::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["LOM_WAYBILL_SEGMENT"]
    )
    @DisplayName("Проверка, что группировка не применятся для других entity")
    fun isNonEligibleReturnFalseIfNotLomSegment(unsupportedEntity: EntityType) {
        val planFact = PlanFact(entityType = unsupportedEntity)
        Assertions.assertThat(
            producer.isEligible(planFact)
        ).isFalse
    }

    @ParameterizedTest
    @EnumSource(
        value = PartnerType::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["FULFILLMENT"]
    )
    @DisplayName("Проверка, что группировка не подходит для других типов предыдущего партнёра")
    fun isNonEligibleReturnFalseIfWrongPreviousPartner(unsupportedType: PartnerType) {
        val planFact = mockMKPlanFactWithPreviousSegment(
            unsupportedType,
            PartnerType.DELIVERY,
            PartnerSubtype.MARKET_COURIER
        )
        Assertions.assertThat(
            producer.isEligible(planFact)
        ).isFalse
    }

    @ParameterizedTest
    @EnumSource(
        value = PartnerType::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["DELIVERY"]
    )
    @DisplayName("Проверка, что группировка не подходит для других типов текущего партнёра")
    fun isNonEligibleReturnFalseIfWrongCurrentPartner(unsupportedType: PartnerType) {
        val planFact = mockMKPlanFactWithPreviousSegment(
            PartnerType.FULFILLMENT,
            unsupportedType,
            PartnerSubtype.MARKET_COURIER
        )
        Assertions.assertThat(
            producer.isEligible(planFact)
        ).isFalse
    }

    @ParameterizedTest
    @EnumSource(
        value = PartnerSubtype::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["MARKET_COURIER"]
    )
    @DisplayName("Проверка, что группировка не подходит для других подтипов текущего партнёра")
    fun isNonEligibleReturnFalseIfWrongCurrentPartner(unsupportedType: PartnerSubtype) {
        val planFact = mockMKPlanFactWithPreviousSegment(
            PartnerType.FULFILLMENT,
            PartnerType.DELIVERY,
            unsupportedType
        )
        Assertions.assertThat(
            producer.isEligible(planFact)
        ).isFalse
    }

    @Test
    @DisplayName("Проверка полученной AggregationEntity")
    fun produceEntity() {
        val previous = WaybillSegment(
            partnerId = 1,
            partnerType = PartnerType.FULFILLMENT,
            partnerName = "test_partner_from",
        )
        val current = WaybillSegment(
            partnerId = 2,
            partnerType = PartnerType.DELIVERY,
            partnerName = "test_current_partner",
        )
        val planFact = mockMKPlanFactWithPreviousSegment(previous, current)

        planFact.expectedStatusDatetime = Instant.parse("2021-01-01T13:00:59.00Z")
            .minusSeconds(Hours.THREE.toStandardSeconds().seconds.toLong())

        Assertions.assertThat(
            producer.produceEntity(planFact)
        ).isEqualTo(
            AggregationEntity(
                date = LocalDate.ofInstant(
                    planFact.expectedStatusDatetime,
                    DateTimeUtils.MOSCOW_ZONE),
                partnerFrom = converter.toPartnerAggregationEntity(previous),
                partner = converter.toPartnerAggregationEntity(current),
            )
        )
    }

    @Test
    @DisplayName("Проверка полученной AggregationEntity для группы следующего дня")
    fun produceEntityNextDay() {
        val previous = WaybillSegment(
            partnerId = 1,
            partnerType = PartnerType.FULFILLMENT,
            partnerName = "test_partner_from",
        )
        val current = WaybillSegment(
            partnerId = 2,
            partnerType = PartnerType.DELIVERY,
            partnerName = "test_current_partner",
        )
        val planFact = mockMKPlanFactWithPreviousSegment(previous, current)
        planFact.expectedStatusDatetime = Instant.parse("2021-01-01T18:00:00.00Z")

        Assertions.assertThat(
            producer.produceEntity(planFact)
        ).isEqualTo(
            AggregationEntity(
                date = LocalDate.ofInstant(planFact.expectedStatusDatetime, DateTimeUtils.MOSCOW_ZONE)
                    .plusDays(1),
                partnerFrom = converter.toPartnerAggregationEntity(previous),
                partner = converter.toPartnerAggregationEntity(current),
            )
        )
    }

    private fun mockMKPlanFactWithPreviousSegment(
        previousType: PartnerType,
        currentType: PartnerType,
        currentSubtype: PartnerSubtype,
    ): PlanFact {
        val previousSegment = WaybillSegment(
            partnerType = previousType
        )
        val currentSegment = WaybillSegment(
            partnerType = currentType
        )
        currentSegment.partnerSubtype = currentSubtype
        joinInOrder(listOf(previousSegment, currentSegment))

        val planFact = PlanFact(entityType = EntityType.LOM_WAYBILL_SEGMENT)
        planFact.entity = currentSegment
        return planFact
    }

    private fun mockMKPlanFactWithPreviousSegment(
        previous: WaybillSegment,
        current: WaybillSegment,
    ): PlanFact {
        joinInOrder(listOf(previous, current))

        val planFact = PlanFact(entityType = EntityType.LOM_WAYBILL_SEGMENT)
        planFact.entity = current
        return planFact
    }
}
