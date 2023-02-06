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
import ru.yandex.market.logistics.mqm.entity.lom.Location
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.embedded.WaybillShipment
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerType
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.utils.joinInOrder
import java.time.Instant
import java.time.LocalDate

class DropshipDropoffIntakeProducerTest {

    private val converter = AggregationEntityConverter()
    private val producer = DropshipDropoffIntakeProducer(converter)

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
    @DisplayName("Не агрегируется если текущий сегмент не дропофф")
    fun isNonEligibleIfWrongCurrentPartnerType() {
        val planFact = preparePlanFact(isDropoff = false)
        Assertions.assertThat(producer.isEligible(planFact)).isFalse
    }

    @ParameterizedTest
    @EnumSource(
        value = PartnerType::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["DROPSHIP"]
    )
    @DisplayName("Не агрегируется если неправильный тип партнера на предыдущем сегменте")
    fun isNonEligibleIfWrongPreviousPartnerType(previousType: PartnerType) {
        val planFact = preparePlanFact(previousType = previousType)
        Assertions.assertThat(producer.isEligible(planFact)).isFalse
    }

    @Test
    @DisplayName("Проверка созданной AggegationEntity")
    fun produceEntity() {
        val previousSegment = WaybillSegment(
            partnerId = 1,
            partnerType = PartnerType.DROPSHIP,
            partnerName = "dropship"
        )
        val currentSegment = WaybillSegment(
            partnerId = 2,
            partnerType = PartnerType.DELIVERY,
            segmentType = SegmentType.SORTING_CENTER,
            partnerName = "dropoff sc",
            shipment = WaybillShipment(locationFrom = Location(warehouseId = 11))
        )
        val planFact = preparePlanFact(previousSegment, currentSegment)
        Assertions.assertThat(producer.produceEntity(planFact)).isEqualTo(
            AggregationEntity(
                date = LocalDate.ofInstant(planFact.expectedStatusDatetime, DateTimeUtils.MOSCOW_ZONE),
                partnerFrom = converter.toPartnerAggregationEntity(previousSegment),
                locationTo = converter.toLocationAggregationEntity(currentSegment.shipment.locationFrom)
            )
        )
    }

    private fun preparePlanFact(
        previousType: PartnerType = PartnerType.DROPSHIP,
        isDropoff: Boolean = true
    ): PlanFact {
        val previousSegment = WaybillSegment(partnerType = previousType)
        val currentSegment = WaybillSegment(partnerId = 1, externalId = "123")
        joinInOrder(listOf(previousSegment, currentSegment))

        if (isDropoff) {
            currentSegment.partnerType = PartnerType.DELIVERY
            currentSegment.segmentType = SegmentType.SORTING_CENTER
        }

        return PlanFact(entityType = EntityType.LOM_WAYBILL_SEGMENT).apply { entity = currentSegment }
    }

    private fun preparePlanFact(
        previous: WaybillSegment,
        current: WaybillSegment,
        expectedStatusDatetime: Instant = Instant.parse("2021-01-01T06:00:00.00Z")
    ): PlanFact {
        joinInOrder(listOf(previous, current))
        return PlanFact(
            entityType = EntityType.LOM_WAYBILL_SEGMENT,
            expectedStatusDatetime = expectedStatusDatetime,
        ).apply { entity = current }
    }
}
