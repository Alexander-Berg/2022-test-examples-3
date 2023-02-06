package ru.yandex.market.logistics.mqm.service.processor.aggregationentity

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import ru.yandex.market.logistics.mqm.entity.PlanFact
import ru.yandex.market.logistics.mqm.entity.aggregationentity.AggregationEntity
import ru.yandex.market.logistics.mqm.entity.enums.EntityType
import ru.yandex.market.logistics.mqm.entity.lom.LomOrder
import ru.yandex.market.logistics.mqm.entity.lom.PartnerSettings
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.enums.PlatformClient
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.entity.lom.enums.WaybillSegmentTag

class OrderExpressProducerTest {

    var producer = OrderExpressProducer()

    @Test
    @DisplayName("Проверка применимости на сегменте дропшипа")
    fun isEligibleOnDropship() {
        Assertions.assertThat(
            producer.isEligible(mockDropshipExpressPlanFact()),
        ).isTrue
    }

    @Test
    @DisplayName("Проверка применимости на сегменте курьера")
    fun isEligibleOnCourier() {
        Assertions.assertThat(
            producer.isEligible(mockCourierExpressPlanFact())
        ).isTrue
    }

    @Test
    @DisplayName("Проверка, что группировка не применятся, если не Express")
    fun isNonEligibleReturnFalseIfNotExpress() {
        val testPlanFact = PlanFact().apply {
            entity = WaybillSegment(segmentType = SegmentType.PICKUP)
        }
        Assertions.assertThat(
            producer.isEligible(testPlanFact)
        ).isFalse
    }

    @ParameterizedTest
    @EnumSource(
        value = EntityType::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["LOM_WAYBILL_SEGMENT"]
    )
    @DisplayName("Проверка, что группировка не применятся для других entity")
    fun isNonEligibleReturnFalseIfNotLomSegment(unsupportedEntity: EntityType) {
        Assertions.assertThat(
            producer.isEligible(mockDropshipExpressPlanFact(unsupportedEntity))
        ).isFalse
    }

    @Test
    @DisplayName("Проверка создаваемой агрегации")
    fun produceEntityNextDay() {
        assertSoftly {
            producer.produceEntity(mockDropshipExpressPlanFact()) shouldBe AggregationEntity()
        }
    }

    private fun mockDropshipExpressPlanFact(
        entity: EntityType = EntityType.LOM_WAYBILL_SEGMENT
    ): PlanFact {
        val planFact = PlanFact(entityType = entity)
        planFact.entity = WaybillSegment(
            segmentType = SegmentType.FULFILLMENT,
            partnerSettings = PartnerSettings(dropshipExpress = true)
        ).apply { order = LomOrder(platformClientId = PlatformClient.BERU.id) }
        return planFact
    }

    private fun mockCourierExpressPlanFact(): PlanFact {
        val planFact = PlanFact(entityType = EntityType.LOM_WAYBILL_SEGMENT)
        planFact.entity = WaybillSegment(
            segmentType = SegmentType.COURIER,
            waybillSegmentTags = mutableSetOf(WaybillSegmentTag.CALL_COURIER)
        ).apply { order = LomOrder(platformClientId = PlatformClient.BERU.id) }
        return planFact
    }
}
