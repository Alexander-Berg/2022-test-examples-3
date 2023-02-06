package ru.yandex.market.logistics.mqm.service.processor.aggregationentity

import io.kotest.matchers.shouldBe
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
import ru.yandex.market.logistics.mqm.entity.lom.LomOrder
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerType
import ru.yandex.market.logistics.mqm.entity.lom.enums.PlatformClient
import ru.yandex.market.logistics.mqm.utils.getPreviousSegment
import ru.yandex.market.logistics.mqm.utils.joinInOrder
import java.time.Instant
import java.time.LocalDate

class ScDsIntakeProducerTest : AbstractTest() {
    val converter = AggregationEntityConverter()
    val producer = ScDsIntakeProducer(converter)

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @EnumSource(
        value = PlatformClient::class,
        mode = EnumSource.Mode.INCLUDE,
        names = ["BERU", "YANDEX_GO"]
    )
    @DisplayName("Проверка применимости")
    fun isEligible(platformClient: PlatformClient) {
        val planFact = preparePlanFact(platformClient = platformClient)
        producer.isEligible(planFact) shouldBe true
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @EnumSource(
        value = PlatformClient::class,
        mode = EnumSource.Mode.INCLUDE,
        names = ["BERU", "YANDEX_GO"]
    )
    @DisplayName("Проверка на корректность AggregationEntity c датой на текущий день.")
    fun aggregationEntityToday(platformClient: PlatformClient) {
        val planFact = preparePlanFact(platformClient = platformClient)
        val waybillSegment = planFact.entity as WaybillSegment
        val previousWaybillSegment = waybillSegment.getPreviousSegment()

        producer.produceEntity(planFact) shouldBe AggregationEntity(
            date = LocalDate.ofInstant(planFact.expectedStatusDatetime, DateTimeUtils.MOSCOW_ZONE),
            partner = converter.toPartnerAggregationEntity(waybillSegment),
            partnerFrom = converter.toPartnerAggregationEntity(previousWaybillSegment),
            platformClient = if (platformClient == PlatformClient.YANDEX_GO) platformClient.id else null
        )
    }

    @Test
    @DisplayName("Проверка на корректность AggregationEntity с переносом даты на следующий день.")
    fun aggregationEntityNextDay() {
        val planFact = preparePlanFact(expectedTime = Instant.parse("2021-01-01T16:01:00.00Z"))
        val waybillSegment = planFact.entity as WaybillSegment
        val previousWaybillSegment = waybillSegment.getPreviousSegment()

        producer.produceEntity(planFact) shouldBe AggregationEntity(
            date = LocalDate.ofInstant(planFact.expectedStatusDatetime, DateTimeUtils.MOSCOW_ZONE).plusDays(1),
            partner = converter.toPartnerAggregationEntity(waybillSegment),
            partnerFrom = converter.toPartnerAggregationEntity(previousWaybillSegment)
        )
    }

    @Test
    @DisplayName("Проверка что AggregationEntity без предшествующего не обрабатывается.")
    fun aggregationEntityIsNullIfNoPrevSegment() {
        val planFact = preparePlanFact(previousSegment = null)
        producer.isEligible(planFact) shouldBe false
    }

    private fun preparePlanFact(
        currentPartnerType: PartnerType = PartnerType.DELIVERY,
        previousSegment: WaybillSegment? = WaybillSegment(partnerType = PartnerType.SORTING_CENTER),
        expectedTime: Instant = Instant.parse("2021-01-01T12:59:00.00Z"),
        platformClient: PlatformClient = PlatformClient.BERU,
    ): PlanFact {
        val currentSegment = WaybillSegment(
            partnerType = currentPartnerType,
        )
        if (previousSegment != null) {
            joinInOrder(listOf(previousSegment, currentSegment)).apply { platformClientId = platformClient.id }
        } else {
            currentSegment.order = LomOrder().apply { platformClientId = platformClient.id }
        }
        val planfact = PlanFact(
            entityType = EntityType.LOM_WAYBILL_SEGMENT,
            expectedStatusDatetime = expectedTime
        )
        planfact.entity = currentSegment
        return planfact
    }
}
