package ru.yandex.market.logistics.mqm.service.processor.aggregationentity

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.converter.AggregationEntityConverter
import ru.yandex.market.logistics.mqm.entity.PlanFact
import ru.yandex.market.logistics.mqm.entity.aggregationentity.AggregationEntity
import ru.yandex.market.logistics.mqm.entity.enums.EntityType
import ru.yandex.market.logistics.mqm.entity.enums.PlanFactStatus
import ru.yandex.market.logistics.mqm.entity.enums.ProcessingStatus
import ru.yandex.market.logistics.mqm.entity.lom.LomOrder
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.enums.PlatformClient
import java.time.Instant
import java.time.LocalDate

class FfReturnPreparingSenderProducerTest {
    private val converter = AggregationEntityConverter()
    private val producer = FfReturnPreparingSenderProducer(converter)

    @Test
    @DisplayName("Проверка применимости")
    fun isEligible() {
        val planFact = preparePlanFact()
        producer.isEligible(planFact) shouldBe true
    }

    @Test
    @DisplayName("Проверка созданной AggregationEntity")
    fun produceEntity() {
        val planFact = preparePlanFact()
        val waybillSegment = planFact.entity as WaybillSegment

        val expectedAggregationEntity = AggregationEntity(
            date = LocalDate.ofInstant(EXPECTED_TIME, DateTimeUtils.MOSCOW_ZONE),
            partner = converter.toPartnerAggregationEntity(waybillSegment),
        )
        producer.produceEntity(planFact) shouldBe expectedAggregationEntity
    }

    private fun preparePlanFact(): PlanFact {
        return PlanFact(
            id = 1,
            entityType = EntityType.LOM_WAYBILL_SEGMENT,
            entityId = 2,
            expectedStatus = "RETURN_PREPARING_SENDER",
            producerName = "FfReturnPreparingSenderPlanFactProcessor",
            expectedStatusDatetime = EXPECTED_TIME,
            processingStatus = ProcessingStatus.ENQUEUED,
            planFactStatus = PlanFactStatus.ACTIVE,
        ).apply {
            entity = WaybillSegment(partnerId = 321, partnerName = PARTNER_NAME)
                .apply { order = LomOrder(platformClientId = PlatformClient.BERU.id) }
        }
    }

    companion object {
        val EXPECTED_TIME: Instant = Instant.parse("2021-12-21T17:00:00.00Z")
        const val PARTNER_NAME = "testPartner"
    }
}
