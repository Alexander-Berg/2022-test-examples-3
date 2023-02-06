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
import ru.yandex.market.logistics.mqm.utils.todayOrNextDayGroup
import java.time.Instant
import java.time.LocalDate

class PickupPostFinalStatusProducerTest: AbstractTest() {
    val converter = AggregationEntityConverter()
    val producer = PickupPostFinalStatusProducer(converter)

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
        val planFact = preparePlanFact()
        producer.produceEntity(planFact) shouldBe AggregationEntity(
            date = todayOrNextDayGroup(planFact, AggregationEntityProducer.GROUP_CHANGE_TIME_19),
            partner = converter.toPartnerAggregationEntity(planFact.entity as? WaybillSegment),
            locationTo = converter.toLocationAggregationEntity(
                (planFact.entity as? WaybillSegment)?.shipment?.locationTo
            )
        )
    }

    @Test
    @DisplayName("Проверка на корректность AggregationEntity для следующего дня.")
    fun aggregationEntityNextDay() {
        val planFact = preparePlanFact(expectedStatusDateTime = EXPECTED_NEXT_TIME)
        producer.produceEntity(planFact) shouldBe AggregationEntity(
            date = LocalDate.ofInstant(planFact.expectedStatusDatetime, DateTimeUtils.MOSCOW_ZONE).plusDays(1),
            partner = converter.toPartnerAggregationEntity(planFact.entity as? WaybillSegment),
            locationTo = converter.toLocationAggregationEntity(
                (planFact.entity as? WaybillSegment)?.shipment?.locationTo
            )
        )
    }

    private fun preparePlanFact(
        entityType: EntityType = EntityType.LOM_WAYBILL_SEGMENT,
        expectedStatusDateTime: Instant = EXPECTED_TIME,
        platformClient: PlatformClient = PlatformClient.BERU,
    ): PlanFact {
        return PlanFact(
            entityType = entityType,
            expectedStatusDatetime = expectedStatusDateTime,
        ).apply {
            entity = WaybillSegment(
                segmentType = SegmentType.FULFILLMENT,
                partnerType = PartnerType.DROPSHIP,
            ).apply {
                order = LomOrder(platformClientId = platformClient.id)
            }
        }
    }

    companion object {
        val EXPECTED_TIME: Instant = Instant.parse("2021-01-01T12:00:00.00Z")
        val EXPECTED_NEXT_TIME: Instant = Instant.parse("2021-01-01T16:00:00.00Z")
    }
}
