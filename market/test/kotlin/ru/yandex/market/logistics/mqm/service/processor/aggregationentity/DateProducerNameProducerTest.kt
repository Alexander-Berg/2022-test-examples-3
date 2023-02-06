package ru.yandex.market.logistics.mqm.service.processor.aggregationentity

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.converter.AggregationEntityConverter
import ru.yandex.market.logistics.mqm.entity.PlanFact
import ru.yandex.market.logistics.mqm.entity.aggregationentity.AggregationEntity
import ru.yandex.market.logistics.mqm.entity.lom.LomOrder
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.enums.PlatformClient
import ru.yandex.market.logistics.mqm.service.processor.planfact.AutoCancelAfterDropshipPlanFactProcessor
import java.time.Instant
import java.time.LocalDate

class DateProducerNameProducerTest {

    private val producer = DateProducerNameProducer(AggregationEntityConverter())

    @Test
    @DisplayName("Проверка применимости")
    fun isEligible() {
        val planFact = preparePlanFact()
        Assertions.assertThat(producer.isEligible(planFact)).isTrue
    }

    @Test
    @DisplayName("Не агрегируется, если нет producer_name")
    fun isNonEligibleIfUnsupportedProducerName() {
        val planFact = preparePlanFact(producerName = "test_name")
        Assertions.assertThat(producer.isEligible(planFact)).isFalse
    }

    @Test
    @DisplayName("Проверка созданной AggegationEntity")
    fun produceEntity() {
        val planFact = preparePlanFact()
        Assertions.assertThat(producer.produceEntity(planFact)).isEqualTo(
            AggregationEntity(
                date = LocalDate.ofInstant(planFact.expectedStatusDatetime, DateTimeUtils.MOSCOW_ZONE),
                producerName = planFact.producerName,
            )
        )
    }

    private fun preparePlanFact(
        deadline: Instant = Instant.parse("2021-01-01T06:00:00.00Z"),
        producerName: String? = AutoCancelAfterDropshipPlanFactProcessor::class.simpleName,
    ) = PlanFact(
        expectedStatusDatetime = deadline,
        producerName = producerName,
    ).apply {
        entity = WaybillSegment().apply { order = LomOrder(platformClientId = PlatformClient.BERU.id) }
    }
}
