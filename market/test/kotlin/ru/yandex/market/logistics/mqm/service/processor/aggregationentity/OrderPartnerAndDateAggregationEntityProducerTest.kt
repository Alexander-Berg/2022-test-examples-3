package ru.yandex.market.logistics.mqm.service.processor.aggregationentity

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import ru.yandex.market.logistics.mqm.converter.AggregationEntityConverter
import ru.yandex.market.logistics.mqm.entity.PlanFact
import ru.yandex.market.logistics.mqm.entity.aggregationentity.AggregationEntity
import ru.yandex.market.logistics.mqm.entity.aggregationentity.PartnerAggregationEntity
import ru.yandex.market.logistics.mqm.entity.enums.EntityType
import ru.yandex.market.logistics.mqm.entity.lom.LomOrder
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.enums.PlatformClient
import ru.yandex.market.logistics.mqm.service.processor.planfact.OrderWithoutRequiredCISPlanFactProcessor
import ru.yandex.market.logistics.mqm.utils.toLocalDateTime
import java.time.Instant

class OrderPartnerAndDateAggregationEntityProducerTest {

    var producer = OrderPartnerAndDateAggregationEntityProducer(AggregationEntityConverter())

    @Test
    @DisplayName("Проверка применимости")
    fun isEligible() {
        Assertions.assertThat(
            producer.isEligible(mockLomOrderPlanFact()),
        ).isTrue
    }

    @ParameterizedTest
    @EnumSource(
        value = EntityType::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["LOM_ORDER"]
    )
    @DisplayName("Проверка, что группировка не применятся для других entity")
    fun isNonEligibleReturnFalseIfNotLomOrder(unsupportedEntity: EntityType) {
        Assertions.assertThat(
            producer.isEligible(mockLomOrderPlanFact(unsupportedEntity))
        ).isFalse
    }

    @Test
    @DisplayName("Проверка, что группировка не применятся для неправильного процессора")
    fun isNonEligibleReturnFalseIfUnsupportedProcessor() {
        Assertions.assertThat(
            producer.isEligible(mockLomOrderPlanFact(producerName = "unsupported_processor"))
        ).isFalse
    }

    @Test
    @DisplayName("Проверка создаваемой агрегации")
    fun produceEntity() {
        assertSoftly {
            producer.produceEntity(mockLomOrderPlanFact()) shouldBe AggregationEntity(
                date = EXPECTED_DATE_TIME.toLocalDateTime().toLocalDate(),
                partner = PartnerAggregationEntity(id = PARTNER_ID)
            )
        }
    }

    private fun mockLomOrderPlanFact(
        entity: EntityType = EntityType.LOM_ORDER,
        producerName: String = OrderWithoutRequiredCISPlanFactProcessor::class.simpleName!!,
    ): PlanFact {
        val planFact = PlanFact(
            entityType = entity,
            producerName = producerName,
            expectedStatusDatetime = EXPECTED_DATE_TIME
        )
        planFact.entity = LomOrder(platformClientId = PlatformClient.BERU.id).apply {
            waybill = mutableListOf(WaybillSegment(id = 1, partnerId = PARTNER_ID, waybillSegmentIndex = 0))
        }
        return planFact
    }

    companion object{
        private val EXPECTED_DATE_TIME = Instant.parse("2022-06-20T10:00:01.00Z")
        const val PARTNER_ID = 123L
    }
}
