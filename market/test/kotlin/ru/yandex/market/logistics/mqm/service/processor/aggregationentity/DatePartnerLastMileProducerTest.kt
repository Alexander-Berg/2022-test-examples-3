package ru.yandex.market.logistics.mqm.service.processor.aggregationentity

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.converter.AggregationEntityConverter
import ru.yandex.market.logistics.mqm.entity.PlanFact
import ru.yandex.market.logistics.mqm.entity.aggregationentity.AggregationEntity
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.enums.DeliveryType
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerSubtype
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerType
import ru.yandex.market.logistics.mqm.entity.lom.enums.PlatformClient
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.service.processor.planfact.waybillsegment.CourierLastMileRecipientPlanFactProcessor
import ru.yandex.market.logistics.mqm.service.processor.planfact.waybillsegment.PickupOrPostLastMileRecipientPlanFactProcessor
import ru.yandex.market.logistics.mqm.utils.joinInOrder
import java.time.Instant
import java.time.LocalDate

internal class DatePartnerLastMileProducerTest {

    private val converter = AggregationEntityConverter()
    private val producer = DatePartnerLastMileProducer(converter)

    companion object {
        val TEST_EXPECTED_TIME: Instant = Instant.parse("2021-01-01T06:00:00.00Z")
    }

    @EnumSource(
        value = PlatformClient::class,
        mode = EnumSource.Mode.INCLUDE,
        names = ["BERU", "YANDEX_GO"]
    )
    @DisplayName("Проверка успешной применимости")
    fun isEligible(platformClient: PlatformClient) {
        val planFact = preparePlanFact(platformClient = platformClient)
        Assertions.assertThat(producer.isEligible(planFact)).isTrue
    }

    @Test
    @DisplayName("Проверка применимости для план-факт процессоров передачи на последнюю милю")
    fun isEligibleForCourierLastMile() {
        val courierPlanFact = preparePlanFact()
            .apply {
                producerName = CourierLastMileRecipientPlanFactProcessor::class.simpleName
            }
        val pickupPlanFact = preparePlanFact()
            .apply {
                producerName = PickupOrPostLastMileRecipientPlanFactProcessor::class.simpleName
            }
        assertSoftly {
            producer.isEligible(courierPlanFact) shouldBe true
            producer.isEligible(pickupPlanFact) shouldBe true
        }
    }

    @DisplayName("Группировка применима при SegmentType POST")
    @ParameterizedTest
    @EnumSource(
        value = SegmentType::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["POST"]
    )
    fun isEligibleSegmentTypePost(segmentType: SegmentType) {
        val planFact = preparePlanFact(
            segmentType = segmentType,
        )
        Assertions.assertThat(producer.isEligible(planFact)).isTrue
    }

    @DisplayName("Группировка применима при PartnerSubtype PARTNER_CONTRACT_DELIVERY")
    @ParameterizedTest
    @EnumSource(
        value = PartnerSubtype::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["PARTNER_CONTRACT_DELIVERY"]
    )
    fun isNotEligibleWrongPArtnerSubtype(partnerSubtype: PartnerSubtype) {
        val planFact = preparePlanFact(
            partnerSubtype = partnerSubtype,
        )
        Assertions.assertThat(producer.isEligible(planFact)).isTrue
    }

    @Test
    @DisplayName("Проверка созданной AggregationEntity")
    fun produceEntity() {
        val planFact = preparePlanFact()
        Assertions.assertThat(producer.produceEntity(planFact)).isEqualTo(
            AggregationEntity(
                date = LocalDate.ofInstant(TEST_EXPECTED_TIME, DateTimeUtils.MOSCOW_ZONE),
                partner = converter.toPartnerAggregationEntity(planFact.entity as WaybillSegment),
            )
        )
    }

    private fun preparePlanFact(
        segmentType: SegmentType = SegmentType.POST,
        partnerSubtype: PartnerSubtype = PartnerSubtype.PARTNER_CONTRACT_DELIVERY,
        expectedStatusDatetime: Instant = TEST_EXPECTED_TIME,
        deliveryType: DeliveryType = DeliveryType.COURIER,
        platformClient: PlatformClient = PlatformClient.BERU,
    ): PlanFact {
        val lastMileSegment = WaybillSegment(partnerType = PartnerType.DELIVERY, partnerSubtype = partnerSubtype)
        val order = joinInOrder(listOf(lastMileSegment))
        order.deliveryType = deliveryType
        order.platformClientId = platformClient.id

        return PlanFact(
            waybillSegmentType = segmentType,
            expectedStatusDatetime = expectedStatusDatetime,
        ).apply {
            entity = lastMileSegment
        }
    }
}
