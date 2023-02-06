package ru.yandex.market.logistics.mqm.service.processor.aggregationentity

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.Mockito
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
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.service.lms.LmsPartnerService
import ru.yandex.market.logistics.mqm.service.processor.planfact.ScScShipmentPlanFactProcessor
import ru.yandex.market.logistics.mqm.utils.joinInOrder
import java.time.Instant
import java.time.LocalDate

class ScScShipmentProducerTest : AbstractTest() {
    val converter = AggregationEntityConverter()
    val partnerService: LmsPartnerService = Mockito.mock(LmsPartnerService::class.java)
    val producer = ScScShipmentProducer(converter)

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

    @Test
    @DisplayName("Проверка, что группировка не применяется для неподходящих продьюсера")
    fun isNonEligibleIfWrongProducerName() {
        val planFact = preparePlanFact(producerName = "unsupported_producer")
        producer.isEligible(planFact) shouldBe false
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
        val waybillSegment = planFact.entity as? WaybillSegment

        producer.produceEntity(planFact) shouldBe AggregationEntity(
            date = LocalDate.ofInstant(planFact.expectedStatusDatetime, DateTimeUtils.MOSCOW_ZONE),
            partner = converter.toPartnerAggregationEntity(waybillSegment),
            platformClient = if (platformClient == PlatformClient.YANDEX_GO) platformClient.id else null
        )
    }

    @Test
    @DisplayName("Проверка на корректность AggregationEntity с переносом даты на следующий день.")
    fun aggregationEntityNextDay() {
        val planFact = preparePlanFact(expectedTime = Instant.parse("2021-01-01T16:01:00.00Z"))
        val waybillSegment = planFact.entity as? WaybillSegment

        producer.produceEntity(planFact) shouldBe AggregationEntity(
            date = LocalDate.ofInstant(planFact.expectedStatusDatetime, DateTimeUtils.MOSCOW_ZONE).plusDays(1),
            partner = converter.toPartnerAggregationEntity(waybillSegment),
        )
    }

    @Test
    @DisplayName("Проверка что AggregationEntity без последующего сегмента не обрабатывается.")
    fun aggregationEntityIsNullIfNoNextSegment() {
        val planFact = preparePlanFact(nextSegment = null)
        producer.isEligible(planFact) shouldBe false
    }

    @Test
    @DisplayName("Проверка что AggregationEntity для сегмента типа Dropoff не обрабатываются.")
    fun aggregationEntityIsNullIfIsDropoff() {
        val planFact = preparePlanFact(isDropoff = true)
        producer.isEligible(planFact) shouldBe false
    }

    private fun preparePlanFact(
        currentPartnerType: PartnerType = PartnerType.SORTING_CENTER,
        nextSegment: WaybillSegment? = WaybillSegment(partnerType = PartnerType.SORTING_CENTER),
        expectedTime: Instant = Instant.parse("2021-01-01T12:59:00.00Z"),
        producerName: String = ScScShipmentPlanFactProcessor::class.simpleName!!,
        isDropoff: Boolean = false,
        platformClient: PlatformClient = PlatformClient.BERU,
    ): PlanFact {
        val currentSegment = WaybillSegment(
            partnerType = currentPartnerType,
            partnerId = 1L,
            partnerName = "dropoff sc"
        )

        if (nextSegment != null) {
            joinInOrder(listOf(currentSegment, nextSegment)).apply { platformClientId = platformClient.id }
        } else {
            currentSegment.order = LomOrder().apply { platformClientId = platformClient.id }
        }
        if (isDropoff) {
            currentSegment.partnerType = PartnerType.DELIVERY
            currentSegment.segmentType = SegmentType.SORTING_CENTER
        }
        val planfact = PlanFact(
            entityType = EntityType.LOM_WAYBILL_SEGMENT,
            expectedStatusDatetime = expectedTime,
            producerName = producerName,
        )
        planfact.entity = currentSegment
        return planfact
    }
}
