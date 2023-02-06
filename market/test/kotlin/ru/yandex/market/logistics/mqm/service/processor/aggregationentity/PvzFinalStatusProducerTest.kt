package ru.yandex.market.logistics.mqm.service.processor.aggregationentity

import io.kotest.matchers.shouldBe
import org.assertj.core.api.Assertions.assertThat
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
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.service.processor.planfact.DeliveredPlanFactProcessor
import ru.yandex.market.logistics.mqm.utils.getPreviousSegment
import ru.yandex.market.logistics.mqm.utils.joinInOrder
import java.time.Instant
import java.time.LocalDate

class PvzFinalStatusProducerTest {

    private val converter = AggregationEntityConverter()
    private val producer = PvzFinalStatusProducer(converter)

    @ParameterizedTest
    @EnumSource(
        value = PartnerSubtype::class,
        mode = EnumSource.Mode.INCLUDE,
        names = ["MARKET_OWN_PICKUP_POINT", "PARTNER_PICKUP_POINT_IP"]
    )
    @DisplayName("Проверка применимости")
    fun isEligible(partnerSubtype: PartnerSubtype) {
        val planFact = createPlanFact(partnerSubtype = partnerSubtype)
        assertThat(producer.isEligible(planFact)).isTrue
    }

    @ParameterizedTest
    @EnumSource(
        value = EntityType::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["LOM_WAYBILL_SEGMENT"]
    )
    @DisplayName("Не агрегируется если план-факт не для сегмента")
    fun isNonEligibleIfNotLomSegment(entityType: EntityType) {
        val planFact = createPlanFact(entityType = entityType)
        assertThat(producer.isEligible(planFact)).isFalse
    }

    @ParameterizedTest
    @EnumSource(
        value = PartnerSubtype::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["MARKET_OWN_PICKUP_POINT", "PARTNER_PICKUP_POINT_IP"]
    )
    @DisplayName("Не агрегируется если неправильный подтип партнера на текущем сегменте")
    fun isNonEligibleIfWrongCurrentPartnerType(partnerSubtype: PartnerSubtype) {
        val planFact = createPlanFact(partnerSubtype = partnerSubtype)
        assertThat(producer.isEligible(planFact)).isFalse
    }

    @Test
    @DisplayName("Проверка созданной AggegationEntity")
    fun produceEntity() {
        val planFact = createPlanFact()
        val currentSegment = planFact.entity as WaybillSegment
        val previousSegment = currentSegment.getPreviousSegment()
        producer.produceEntity(planFact) shouldBe AggregationEntity(
            date = LocalDate.ofInstant(FIXED_TIME, DateTimeUtils.MOSCOW_ZONE),
            partnerSubtype = currentSegment.partnerSubtype,
            partnerFrom = converter.toPartnerAggregationEntity(previousSegment)
        )
    }

    private fun createPlanFact(
        entityType: EntityType = EntityType.LOM_WAYBILL_SEGMENT,
        partnerSubtype: PartnerSubtype = PartnerSubtype.MARKET_OWN_PICKUP_POINT,
    ): PlanFact {
        val currentSegment = WaybillSegment(
            segmentType = SegmentType.PICKUP,
            partnerSubtype = partnerSubtype
        )
        val previousSegment = WaybillSegment(
            partnerId = 1,
            partnerName = "СД1"
        )
        joinInOrder(listOf(previousSegment, currentSegment))

        return PlanFact(
            entityType = entityType,
            expectedStatusDatetime = FIXED_TIME,
            producerName = DeliveredPlanFactProcessor::class.simpleName!!
        ).apply { entity = currentSegment }
    }

    companion object {
        private val FIXED_TIME = Instant.parse("2021-01-01T06:00:00.00Z")
    }
}

