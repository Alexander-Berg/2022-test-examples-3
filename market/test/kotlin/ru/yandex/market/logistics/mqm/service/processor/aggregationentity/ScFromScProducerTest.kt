package ru.yandex.market.logistics.mqm.service.processor.aggregationentity

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import ru.yandex.market.logistics.mqm.AbstractTest
import ru.yandex.market.logistics.mqm.converter.AggregationEntityConverter
import ru.yandex.market.logistics.mqm.entity.PlanFact
import ru.yandex.market.logistics.mqm.entity.aggregationentity.AggregationEntity
import ru.yandex.market.logistics.mqm.entity.enums.EntityType
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerType
import ru.yandex.market.logistics.mqm.entity.lom.enums.PlatformClient
import ru.yandex.market.logistics.mqm.utils.getPreviousSegment
import ru.yandex.market.logistics.mqm.utils.joinInOrder
import ru.yandex.market.logistics.mqm.utils.toInstant
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ScFromScProducerTest : AbstractTest() {
    val converter = AggregationEntityConverter()
    val producer = ScFromScProducer(converter)

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

    @ParameterizedTest
    @EnumSource(
        value = PartnerType::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["SORTING_CENTER"]
    )
    @DisplayName("Проверка, что группировка не применяется, если предыдущий партнёр не SORTING_CENTER")
    fun isEligibleReturnFalseIfPreviousIsNotSortingCenter(unsupportedPartnerType: PartnerType) {
        val planFact = preparePlanFact(previousPartnerType = unsupportedPartnerType)
        producer.isEligible(planFact) shouldBe false
    }

    @ParameterizedTest
    @EnumSource(
        value = PartnerType::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["SORTING_CENTER"]
    )
    @DisplayName("Проверка, что группировка не применяется, если текущий партнёр не SORTING_CENTER")
    fun isEligibleReturnFalseIfCurrentIsNotSortingCenter(unsupportedPartnerType: PartnerType) {
        val planFact = preparePlanFact(currentPartnerType = unsupportedPartnerType)
        producer.isEligible(planFact) shouldBe false
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @EnumSource(
        value = PlatformClient::class,
        mode = EnumSource.Mode.INCLUDE,
        names = ["BERU", "YANDEX_GO"]
    )
    @DisplayName("Проверка на корректность AggregationEntity c датой на текущий день до 13.")
    fun aggregationEntityTodayBefore13(platformClient: PlatformClient) {
        val testDate = LocalDate.of(2021, 10, 10)
        val testTime = LocalTime.of(12, 59)
        val planFact = preparePlanFact(
            expectedTime = LocalDateTime.of(testDate, testTime).toInstant(),
            platformClient = platformClient
        )
        val waybillSegment = planFact.entity as WaybillSegment

        producer.produceEntity(planFact) shouldBe AggregationEntity(
            date = testDate,
            time = LocalTime.of(13, 0),
            partnerFrom = converter.toPartnerAggregationEntity(waybillSegment.getPreviousSegment()),
            partner = converter.toPartnerAggregationEntity(waybillSegment),
            platformClient = if (platformClient == PlatformClient.YANDEX_GO) platformClient.id else null,
        )
    }

    @Test
    @DisplayName("Проверка на корректность AggregationEntity c датой на текущий день между 13 и 17.")
    fun aggregationEntityTodayBetween13And17() {
        val testDate = LocalDate.of(2021, 10, 10)
        val testTime = LocalTime.of(16, 59)
        val planFact = preparePlanFact(expectedTime = LocalDateTime.of(testDate, testTime).toInstant())
        val waybillSegment = planFact.entity as WaybillSegment

        producer.produceEntity(planFact) shouldBe AggregationEntity(
            date = testDate,
            time = LocalTime.of(17, 0),
            partnerFrom = converter.toPartnerAggregationEntity(waybillSegment.getPreviousSegment()),
            partner = converter.toPartnerAggregationEntity(waybillSegment),
        )
    }

    @Test
    @DisplayName("Проверка на корректность AggregationEntity с переносом даты на следующий день.")
    fun aggregationEntityNextDay() {
        val testDate = LocalDate.of(2021, 10, 10)
        val testTime = LocalTime.of(17, 1)
        val planFact = preparePlanFact(expectedTime = LocalDateTime.of(testDate, testTime).toInstant())
        val waybillSegment = planFact.entity as WaybillSegment

        producer.produceEntity(planFact) shouldBe AggregationEntity(
            date = testDate.plusDays(1),
            time = LocalTime.of(13, 0),
            partnerFrom = converter.toPartnerAggregationEntity(waybillSegment.getPreviousSegment()),
            partner = converter.toPartnerAggregationEntity(waybillSegment),
        )
    }

    private fun preparePlanFact(
        previousPartnerType: PartnerType = PartnerType.SORTING_CENTER,
        currentPartnerType: PartnerType = PartnerType.SORTING_CENTER,
        expectedTime: Instant = Instant.parse("2021-01-01T12:59:00.00Z"),
        platformClient: PlatformClient = PlatformClient.BERU,
    ): PlanFact {
        val previousSegment = WaybillSegment(
            partnerType = previousPartnerType,
            partnerId = 1L,
            partnerName = "TEST_PARTNER_1"
        )
        val currentSegment = WaybillSegment(
            partnerType = currentPartnerType,
            partnerId = 2L,
            partnerName = "TEST_PARTNER_2"
        )
        joinInOrder(listOf(previousSegment, currentSegment)).apply { platformClientId = platformClient.id }

        return PlanFact(
            entityType = EntityType.LOM_WAYBILL_SEGMENT,
            expectedStatusDatetime = expectedTime
        ).apply { entity = currentSegment }
    }
}
