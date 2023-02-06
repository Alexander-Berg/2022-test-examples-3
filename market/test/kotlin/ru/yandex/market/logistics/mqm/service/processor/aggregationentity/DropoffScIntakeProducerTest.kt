package ru.yandex.market.logistics.mqm.service.processor.aggregationentity

import org.assertj.core.api.Assertions
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
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerType
import ru.yandex.market.logistics.mqm.entity.lom.enums.PlatformClient
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.utils.getPreviousSegment
import ru.yandex.market.logistics.mqm.utils.joinInOrder
import java.time.Instant
import java.time.LocalDate


class DropoffScIntakeProducerTest : AbstractTest() {
    private val converter = AggregationEntityConverter()
    private val producer = DropoffScIntakeProducer(converter)

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @EnumSource(
        value = PlatformClient::class,
        mode = EnumSource.Mode.INCLUDE,
        names = ["BERU", "YANDEX_GO"]
    )
    @DisplayName("Проверка применимости")
    fun isEligible(platformClient: PlatformClient) {
        val planFact = preparePlanFact(platformClient = platformClient)
        Assertions.assertThat(producer.isEligible(planFact)).isTrue
    }

    @ParameterizedTest
    @EnumSource(
        value = EntityType::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["LOM_WAYBILL_SEGMENT"]
    )
    @DisplayName("Не агрегируется, если план-факт не для сегмента")
    fun isNonEligibleIfNotLomSegment(entityType: EntityType) {
        val planFact = PlanFact(entityType = entityType)
        Assertions.assertThat(producer.isEligible(planFact)).isFalse
    }

    @Test
    @DisplayName("Не агрегируется, если предыдущий сегмент не дропофф")
    fun isNonEligibleIfWrongCurrentPartnerType() {
        val planFact = preparePlanFact(isPreviousDropoff = false)
        Assertions.assertThat(producer.isEligible(planFact)).isFalse
    }

    @ParameterizedTest
    @EnumSource(
        value = PartnerType::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["SORTING_CENTER"]
    )
    @DisplayName("Не агрегируется, если неправильный тип партнера")
    fun isNonEligibleIfWrongPreviousPartnerType(curPartnerType: PartnerType) {
        val planFact = preparePlanFact(curPartnerType = curPartnerType)
        Assertions.assertThat(producer.isEligible(planFact)).isFalse
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @EnumSource(
        value = PlatformClient::class,
        mode = EnumSource.Mode.INCLUDE,
        names = ["BERU", "YANDEX_GO"]
    )
    @DisplayName("Проверка созданной AggregationEntity")
    fun produceEntity(platformClient: PlatformClient) {
        val planFact = preparePlanFact(platformClient = platformClient).apply {
            expectedStatusDatetime = Instant.parse("2021-01-01T08:00:00.00Z")
        }
        val currentSegment = planFact.entity as WaybillSegment
        val previousSegment = currentSegment.getPreviousSegment()
        Assertions.assertThat(producer.produceEntity(planFact)).isEqualTo(
            AggregationEntity(
                date = LocalDate.ofInstant(planFact.expectedStatusDatetime, DateTimeUtils.MOSCOW_ZONE),
                partnerFrom = converter.toPartnerAggregationEntity(previousSegment),
                locationTo = converter.toLocationAggregationEntity(currentSegment.shipment.locationTo),
                platformClient = if (platformClient == PlatformClient.YANDEX_GO) platformClient.id else null,
            )
        )
    }

    private fun preparePlanFact(
        isPreviousDropoff: Boolean = true,
        curPartnerType: PartnerType = PartnerType.SORTING_CENTER,
        platformClient: PlatformClient = PlatformClient.BERU
    ): PlanFact {
        val previousSegment = WaybillSegment(
            partnerId = 1L
        )
        val currentSegment = WaybillSegment(
            externalId = "123",
            partnerId = 2L,
            partnerType = curPartnerType
        )
        joinInOrder(listOf(previousSegment, currentSegment)).apply { platformClientId = platformClient.id }

        if (isPreviousDropoff) {
            previousSegment.partnerType = PartnerType.DELIVERY
            previousSegment.segmentType = SegmentType.SORTING_CENTER
        }

        return PlanFact(entityType = EntityType.LOM_WAYBILL_SEGMENT).apply { entity = currentSegment }
    }
}
