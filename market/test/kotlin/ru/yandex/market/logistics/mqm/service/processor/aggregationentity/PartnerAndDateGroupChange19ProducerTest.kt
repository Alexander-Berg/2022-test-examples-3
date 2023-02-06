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
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import java.time.Instant
import java.time.LocalDate

class PartnerAndDateGroupChange19ProducerTest: AbstractTest() {

    val converter = AggregationEntityConverter()
    val producer = PartnerAndDateGroupChange19Producer(converter)

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
    @DisplayName("Проверка на корректность AggregationEntity для текущего дня.")
    fun aggregationEntityToday() {
        val planFact = preparePlanFact(
            expectedStatusDateTime = Instant.parse("2021-01-01T12:00:00.00Z")
        )
        producer.produceEntity(planFact) shouldBe AggregationEntity(
            date = LocalDate.ofInstant(planFact.expectedStatusDatetime, DateTimeUtils.MOSCOW_ZONE),
            partner = converter.toPartnerAggregationEntity(planFact.entity as? WaybillSegment)
        )
    }

    @Test
    @DisplayName("Проверка на корректность AggregationEntity для следующего дня.")
    fun aggregationEntityNextDay() {
        val planFact = preparePlanFact(
            expectedStatusDateTime = Instant.parse("2021-01-01T16:00:00.00Z")
        )
        producer.produceEntity(planFact) shouldBe AggregationEntity(
            date = LocalDate.ofInstant(planFact.expectedStatusDatetime, DateTimeUtils.MOSCOW_ZONE).plusDays(1),
            partner = converter.toPartnerAggregationEntity(planFact.entity as? WaybillSegment)
        )
    }

    private fun preparePlanFact(
        currentSegmentType: SegmentType = SegmentType.FULFILLMENT,
        partnerType: PartnerType = PartnerType.DROPSHIP,
        entityType: EntityType = EntityType.LOM_WAYBILL_SEGMENT,
        expectedStatusDateTime: Instant = Instant.parse("2021-01-01T12:00:00.00Z"),
        platformClient: PlatformClient = PlatformClient.BERU
    ): PlanFact {
        val currentSegment = WaybillSegment(
            segmentType = currentSegmentType,
            partnerType = partnerType
        )
        currentSegment.order = LomOrder(platformClientId = platformClient.id)
        val planFact = PlanFact(
            entityType = entityType,
            expectedStatusDatetime = expectedStatusDateTime,
        )
        planFact.entity = currentSegment
        return planFact
    }
}
