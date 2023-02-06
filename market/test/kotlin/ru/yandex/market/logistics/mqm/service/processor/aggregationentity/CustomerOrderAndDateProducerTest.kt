package ru.yandex.market.logistics.mqm.service.processor.aggregationentity

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import ru.yandex.market.logistics.mqm.converter.AggregationEntityConverter
import ru.yandex.market.logistics.mqm.entity.PlanFact
import ru.yandex.market.logistics.mqm.entity.enums.EntityType
import java.time.Instant

class CustomerOrderAndDateProducerTest {
    private val converter = AggregationEntityConverter()
    private val producer = CustomerOrderAndDateProducer(converter)

    @Test
    @DisplayName("Проверка применимости")
    fun isEligible() {
        val planFact = preparePlanFact()
        producer.isEligible(planFact) shouldBe true
    }

    private fun preparePlanFact(
        expectedTime: Instant = Instant.parse("2021-01-01T12:59:00.00Z")
    ) = PlanFact(
        entityType = EntityType.CUSTOMER_ORDER,
        expectedStatusDatetime = expectedTime,
    )
}
