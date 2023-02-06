package ru.yandex.market.logistics.mqm.service.processor.aggregationentity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.AbstractTest
import ru.yandex.market.logistics.mqm.converter.AggregationEntityConverter
import ru.yandex.market.logistics.mqm.entity.PlanFact
import ru.yandex.market.logistics.mqm.entity.aggregationentity.AggregationEntity
import ru.yandex.market.logistics.mqm.entity.enums.EntityType
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerType
import ru.yandex.market.logistics.mqm.entity.lom.enums.PlatformClient
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.entity.lom.enums.WaybillSegmentTag
import ru.yandex.market.logistics.mqm.service.processor.planfact.waybillsegment.MkLastMileIntakePlanFactProcessor
import ru.yandex.market.logistics.mqm.utils.getPreviousSegment
import ru.yandex.market.logistics.mqm.utils.joinInOrder
import java.time.Instant
import java.time.LocalDate

class LastMileIntakeProducerTest {
    private val converter = AggregationEntityConverter()
    private val producer = LastMileIntakeProducer(converter)

    @ParameterizedTest(name = AbstractTest.TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @EnumSource(
        value = PlatformClient::class,
        mode = EnumSource.Mode.INCLUDE,
        names = ["BERU", "YANDEX_GO"]
    )
    @DisplayName("Проверка применимости")
    fun isEligible(platformClient: PlatformClient) {
        val planFact = preparePlanFact(platformClient = platformClient)
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
        val planFact = preparePlanFact().apply { this.entityType = entityType }
        assertThat(producer.isEligible(planFact)).isFalse
    }

    @Test
    @DisplayName("Проверка созданной AggegationEntity")
    fun produceEntity() {
        val planFact = preparePlanFact()
        val currentSegment = planFact.entity as WaybillSegment
        val previousSegment = currentSegment.getPreviousSegment()
        assertThat(producer.produceEntity(planFact)).isEqualTo(
            AggregationEntity(
                date = LocalDate.ofInstant(planFact.expectedStatusDatetime, DateTimeUtils.MOSCOW_ZONE),
                partner = converter.toPartnerAggregationEntity(currentSegment),
                partnerFrom = converter.toPartnerAggregationEntity(previousSegment)
            )
        )
    }

    private fun preparePlanFact(
        currentPartnerType: PartnerType = PartnerType.DELIVERY,
        previousSegmentType: SegmentType = SegmentType.MOVEMENT,
        isPreviousOnDemand: Boolean = true,
        platformClient: PlatformClient = PlatformClient.BERU,
    ): PlanFact {
        val previousSegment = WaybillSegment(
            partnerId = 1L,
            segmentType = previousSegmentType,
            waybillSegmentTags = if (isPreviousOnDemand) mutableSetOf(WaybillSegmentTag.ON_DEMAND) else mutableSetOf(),
        )
        val currentSegment = WaybillSegment(
            partnerId = 2L,
            partnerType = currentPartnerType
        )
        joinInOrder(listOf(previousSegment, currentSegment)).apply { platformClientId = platformClient.id }
        return PlanFact(
            entityType = EntityType.LOM_WAYBILL_SEGMENT,
            expectedStatusDatetime = Instant.parse("2021-01-01T08:00:00.00Z"),
            producerName = MkLastMileIntakePlanFactProcessor::class.simpleName,
        ).apply { entity = currentSegment }
    }
}
