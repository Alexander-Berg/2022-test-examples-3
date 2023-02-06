package ru.yandex.market.logistics.mqm.service.processor.aggregationentity

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import ru.yandex.market.logistics.mqm.entity.PlanFact
import ru.yandex.market.logistics.mqm.entity.aggregationentity.AggregationEntity
import ru.yandex.market.logistics.mqm.entity.enums.AggregationType
import ru.yandex.market.logistics.mqm.entity.enums.EntityType
import ru.yandex.market.logistics.mqm.entity.lom.LomOrder
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.enums.PlatformClient

class BaseAggregationEntityProducerTest {

    @DisplayName("Проверка применимости")
    @Test
    fun isEligibleReturnTrue() {
        SimpleTestEntityProducer().isEligible(mockWaybillSegmentPlanFact()) shouldBe true
    }

    @DisplayName("Проверка применимости для старых план-фактов без producerName")
    @Test
    fun isEligibleReturnTrueIfPlanFactWithoutProducerName() {
        SimpleTestEntityProducer(setOf("test_producer")).isEligible(mockWaybillSegmentPlanFact()) shouldBe true
    }

    @DisplayName("Producer не применяется, если producerName, а список поддерживаемых продьюсеров пустой")
    @Test
    fun isEligibleReturnFalseIfPlanFactWithProducerName() {
        SimpleTestEntityProducer().isEligible(mockWaybillSegmentPlanFact(producerName = "test_producer")) shouldBe false
    }

    @DisplayName("Проверка применимости, если план-факт и группировка содержат producerName")
    @Test
    fun isEligibleReturnTrueIfPlanFactAndEntityProducerWithProducerName() {
        SimpleTestEntityProducer(setOf("test_producer", "another")).isEligible(
            mockWaybillSegmentPlanFact(producerName = "test_producer")
        ) shouldBe true
    }

    @DisplayName("Producer не применяется, если не совпадает EntityType")
    @Test
    fun isEligibleReturnFalseIfWrongEntityType() {
        SimpleTestEntityProducer().isEligible(mockOrderPlanFact()) shouldBe false
    }

    @DisplayName("Producer не применяется, если не прошла внутрення проверка применимости")
    @Test
    fun isEligibleReturnFalseIfNotInternal() {
        SimpleTestEntityProducer(isEligibleInternal = false).isEligible(mockWaybillSegmentPlanFact()) shouldBe false
    }

    @DisplayName("Producer не применяется, если не совпадают producerName")
    @Test
    fun isEligibleReturnFalseIfProducerNotMatched() {
        SimpleTestEntityProducer(setOf("another_producer"))
            .isEligible(mockWaybillSegmentPlanFact(producerName = "test_producer")) shouldBe false
    }

    private fun mockWaybillSegmentPlanFact(
        entityType: EntityType = EntityType.LOM_WAYBILL_SEGMENT,
        producerName: String? = null,
    ) = PlanFact(
        entityType = entityType,
        producerName = producerName
    ).apply {
        entity = WaybillSegment().apply { order = LomOrder(platformClientId = PlatformClient.BERU.id) }
    }

    private fun mockOrderPlanFact(): PlanFact {
        val planFact = PlanFact(entityType = EntityType.LOM_ORDER)
        planFact.entity = LomOrder(platformClientId = PlatformClient.BERU.id)
        return planFact
    }
}

class SimpleTestEntityProducer(
    supportedProducers: Set<String> = setOf(),
    private val isEligibleInternal: Boolean = true,
) : BaseAggregationEntityProducer(
    EntityType.LOM_WAYBILL_SEGMENT,
    AggregationType.DATE,
    supportedProducers,
) {
    override fun produceEntity(planFact: PlanFact): AggregationEntity? = null

    override fun isEligibleInternal(planFact: PlanFact) = isEligibleInternal
}
