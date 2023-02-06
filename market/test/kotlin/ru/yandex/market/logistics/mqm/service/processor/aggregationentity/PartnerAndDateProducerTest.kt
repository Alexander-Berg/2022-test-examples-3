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
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerType
import ru.yandex.market.logistics.mqm.entity.lom.enums.PlatformClient
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import java.time.Instant
import java.time.LocalDate

class PartnerAndDateProducerTest {

    private val converter = AggregationEntityConverter()
    private val producer = PartnerAndDateProducer(converter)

    @Test
    fun isEligibleInternal() {
        val planFact = preparePlanFact()
        producer.isEligible(planFact) shouldBe true
    }

    @ParameterizedTest
    @EnumSource(
        value = PartnerType::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["DROPSHIP"]
    )
    @DisplayName("Проверка AggregationEntity для всех валидных партнеров")
    fun validAggregationEntity(partnerType: PartnerType) {
        val planFact = preparePlanFact(partnerType = partnerType)
        producer.produceEntity(planFact) shouldBe AggregationEntity(
            date = LocalDate.ofInstant(planFact.expectedStatusDatetime, DateTimeUtils.MOSCOW_ZONE),
            partner = converter.toPartnerAggregationEntity(planFact.entity as? WaybillSegment)
        )
    }

    private fun preparePlanFact(
        currentSegmentType: SegmentType = SegmentType.FULFILLMENT,
        partnerType: PartnerType = PartnerType.DELIVERY,
        entityType: EntityType = EntityType.LOM_WAYBILL_SEGMENT
    ): PlanFact {
        val currentSegment = WaybillSegment(
            segmentType = currentSegmentType,
            partnerType = partnerType
        )
        currentSegment.order = LomOrder(platformClientId = PlatformClient.BERU.id)
        val planFact = PlanFact(
            entityType = entityType,
            expectedStatusDatetime = Instant.parse("2021-01-01T17:00:00.00Z"),
        )
        planFact.entity = currentSegment
        return planFact
    }
}
