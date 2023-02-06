package ru.yandex.market.logistics.mqm.service.processor.aggregationentity

import io.kotest.matchers.shouldBe
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
import ru.yandex.market.logistics.mqm.service.processor.planfact.DropoffShipmentPlanFactProcessor
import ru.yandex.market.logistics.mqm.utils.joinInOrder
import java.time.Instant
import java.time.LocalDate

class DropoffPartnerProducerTest : AbstractTest() {

    private val converter = AggregationEntityConverter()
    private val producer = DropoffPartnerProducer(converter)

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
    @DisplayName("Не агрегируется, если план-факт от неподходящего продьюсера")
    fun isNonEligibleIfFromWrongProducer() {
        val planFact = PlanFact(producerName = "unsupported_producer")
        producer.isEligible(planFact) shouldBe false
    }

    @Test
    @DisplayName("Не агрегируется, если не дропофф")
    fun isNonEligibleIfWrongCurrentPartnerType() {
        val planFact = preparePlanFact(isDropoff = false)
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
        val currentSegment = WaybillSegment(
            partnerId = 2,
            partnerType = PartnerType.SORTING_CENTER,
            partnerName = "dropoff sc"
        )
        val planFact = preparePlanFact(currentSegment, platformClient = platformClient).apply {
            expectedStatusDatetime = Instant.parse("2021-01-01T06:00:00.00Z")
        }
        Assertions.assertThat(producer.produceEntity(planFact)).isEqualTo(
            AggregationEntity(
                date = LocalDate.ofInstant(planFact.expectedStatusDatetime, DateTimeUtils.MOSCOW_ZONE),
                partner = converter.toPartnerAggregationEntity(currentSegment),
                locationTo = converter.toLocationAggregationEntity(currentSegment.shipment.locationTo),
                platformClient = if (platformClient == PlatformClient.YANDEX_GO) platformClient.id else null
            )
        )
    }

    private fun preparePlanFact(
        segment: WaybillSegment? = null,
        producerName: String = DropoffShipmentPlanFactProcessor::class.simpleName!!,
        isDropoff: Boolean = true,
        platformClient: PlatformClient = PlatformClient.BERU
    ): PlanFact {
        val currentSegment = segment ?: WaybillSegment(partnerId = 1, externalId = "123")
        joinInOrder(listOf(currentSegment)).apply { platformClientId = platformClient.id }

        if (isDropoff) {
            currentSegment.partnerType = PartnerType.DELIVERY
            currentSegment.segmentType = SegmentType.SORTING_CENTER
        }

        return PlanFact(
            entityType = EntityType.LOM_WAYBILL_SEGMENT,
            producerName = producerName,
        ).apply { entity = currentSegment }
    }
}
