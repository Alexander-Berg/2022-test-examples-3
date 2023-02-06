package ru.yandex.market.logistics.mqm.service.processor.aggregationentity

import org.assertj.core.api.Assertions
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
import ru.yandex.market.logistics.mqm.entity.lom.enums.PlatformClient
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.entity.lom.enums.WaybillSegmentTag
import ru.yandex.market.logistics.mqm.utils.joinInOrder
import java.time.Instant
import java.time.LocalDate

class DatePartnerRelationFromReturnProducerTest {

    private val converter = AggregationEntityConverter()
    private val producer = DatePartnerRelationFromReturnProducer(converter)

    @Test
    @DisplayName("Проверка применимости")
    fun isEligible() {
        val planFact = preparePlanFact()
        Assertions.assertThat(producer.isEligible(planFact)).isTrue
    }

    @Test
    @DisplayName("Проверка созданной AggegationEntity для ФФ (фф (прямой+обратный) + сд)")
    fun produceEntityWithFf() {
        val dsSegment = WaybillSegment(
            segmentType = SegmentType.POST,
            partnerId = 1,
            partnerType = PartnerType.DELIVERY,
            partnerName = "ds"
        )
        val ffSegment = WaybillSegment(
            segmentType = SegmentType.FULFILLMENT,
            partnerId = 2,
            partnerType = PartnerType.FULFILLMENT,
            partnerName = "ff",
            waybillSegmentTags = mutableSetOf(WaybillSegmentTag.RETURN, WaybillSegmentTag.DIRECT),
        )
        val planFact = preparePlanFact(ffSegment, dsSegment).apply {
            entity = ffSegment
            expectedStatusDatetime = Instant.parse("2021-01-01T06:00:00.00Z")
        }
        Assertions.assertThat(producer.produceEntity(planFact)).isEqualTo(
            AggregationEntity(
                date = LocalDate.ofInstant(planFact.expectedStatusDatetime, DateTimeUtils.MOSCOW_ZONE),
                partner = converter.toPartnerAggregationEntity(ffSegment),
                partnerFrom = converter.toPartnerAggregationEntity(dsSegment)
            )
        )
    }

    @Test
    @DisplayName("Проверка созданной AggegationEntity для ФФ (фф (прямой) + сд + фф (обратный))")
    fun produceEntityWithFfDs() {
        val ffSegment = WaybillSegment(
            segmentType = SegmentType.FULFILLMENT,
            partnerId = 2,
            partnerType = PartnerType.FULFILLMENT,
            partnerName = "ff1",
            waybillSegmentTags = mutableSetOf(WaybillSegmentTag.DIRECT),
        )
        val dsSegment = WaybillSegment(
            segmentType = SegmentType.POST,
            partnerId = 1,
            partnerType = PartnerType.DELIVERY,
            partnerName = "ds"
        )
        val ffReturnSegment = WaybillSegment(
            segmentType = SegmentType.FULFILLMENT,
            partnerId = 2,
            partnerType = PartnerType.FULFILLMENT,
            partnerName = "ff2",
            waybillSegmentTags = mutableSetOf(WaybillSegmentTag.RETURN),
        )
        val planFact = preparePlanFact(ffSegment, dsSegment, ffReturnSegment).apply {
            entity = ffReturnSegment
            expectedStatusDatetime = Instant.parse("2021-01-01T06:00:00.00Z")
        }
        Assertions.assertThat(producer.produceEntity(planFact)).isEqualTo(
            AggregationEntity(
                date = LocalDate.ofInstant(planFact.expectedStatusDatetime, DateTimeUtils.MOSCOW_ZONE),
                partner = converter.toPartnerAggregationEntity(ffReturnSegment),
                partnerFrom = converter.toPartnerAggregationEntity(dsSegment)
            )
        )
    }

    private fun preparePlanFact(
        first: WaybillSegment,
        second: WaybillSegment,
        third: WaybillSegment? = null
    ): PlanFact {
        if (third != null) {
            joinInOrder(listOf(first, second, third))
        } else {
            joinInOrder(listOf(first, second))
        }
        return preparePlanFact()
    }

    private fun preparePlanFact() = PlanFact(entityType = EntityType.LOM_WAYBILL_SEGMENT)
        .apply {
            entity = WaybillSegment().apply { order = LomOrder(platformClientId = PlatformClient.BERU.id) }
        }
}
