package ru.yandex.market.logistics.mqm.service.processor.planfact;

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import ru.yandex.market.logistics.mqm.entity.lom.LomOrder
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegmentStatusHistory
import ru.yandex.market.logistics.mqm.entity.lom.enums.DeliveryType
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerSubtype
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerType
import ru.yandex.market.logistics.mqm.entity.lom.enums.PlatformClient
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.entity.lom.enums.WaybillSegmentTag
import ru.yandex.market.logistics.mqm.service.processor.planfact.DeliveredPlanFactProcessor.DeadlineCalculator
import java.time.Duration
import java.time.Instant

@Deprecated("Заменен на CourierFinalStatusPlanFactProcessorTest и PickupOrPostFinalStatusPlanFactProcessorTest")
class DeliveredPlanFactProcessorTest {
    private val processor = DeliveredPlanFactProcessor()

    @ParameterizedTest
    @EnumSource(
        value = PartnerSubtype::class,
        mode = EnumSource.Mode.INCLUDE,
        names = [
            "TAXI_EXPRESS",
            "TAXI_LAVKA",
            "MARKET_LOCKER",
            "MARKET_OWN_PICKUP_POINT",
            "MARKET_COURIER",
            "PARTNER_CONTRACT_DELIVERY",
            "PARTNER_PICKUP_POINT_IP"]
    )
    @DisplayName(
        "Проверка, что среди всех чекпоинтов будет выбираться самый недавний с валидным статусом" +
            " для всех подтипов партнеров."
    )
    fun checkDeadlineValid(partnerSubtype: PartnerSubtype) {
        val segment = mockProcessorWaybillSegment(
            partnerSubtype = partnerSubtype,
        )
        val deliveryType: DeliveryType = segment.order!!.deliveryType!!
        val timeAdded = partnerSubtypeDeadlines[partnerSubtype]?.calculate(deliveryType)

        processor.calculateExpectedDatetime(segment) shouldBe DEFAULT_LATEST_TIME.plus(timeAdded)
    }

    @Test
    @DisplayName("Проверка, что может подсчитывать с момента получения чекпоинта 45")
    fun validWhen45Checkpoint() {
        val segment = mockProcessorWaybillSegment(
            history = DEFAULT_SEGMENT_45
        )
        val partnerSubtype = segment.partnerSubtype
        val deliveryType: DeliveryType = segment.order!!.deliveryType!!
        val timeAdded = partnerSubtypeDeadlines[partnerSubtype]?.calculate(deliveryType)

        processor.calculateExpectedDatetime(segment) shouldBe DEFAULT_LATEST_TIME.plus(timeAdded)
    }

    @Test
    @DisplayName("Проверка, что может подсчитывать с момента получения чекпоинта 48")
    fun validWhen48Checkpoint() {
        val segment = mockProcessorWaybillSegment(
            history = DEFAULT_SEGMENT_48
        )
        val partnerSubtype = segment.partnerSubtype
        val deliveryType: DeliveryType = segment.order!!.deliveryType!!
        val timeAdded = partnerSubtypeDeadlines[partnerSubtype]?.calculate(deliveryType)

        processor.calculateExpectedDatetime(segment) shouldBe DEFAULT_LATEST_TIME.plus(timeAdded)
    }

    @ParameterizedTest
    @EnumSource(
        value = SegmentType::class,
        mode = EnumSource.Mode.INCLUDE,
        names = ["COURIER", "FULFILLMENT"]
    )
    @DisplayName("Проверка, что валидна обработка 45 чекпоинта")
    fun isEligibleOnlyCheckpoint45(segmentType: SegmentType) {
        val segment = mockProcessorWaybillSegment(
            history = DEFAULT_SEGMENT_45
        )
        segment.segmentType = segmentType

        processor.isEligible(segment) shouldBe true
    }

    @ParameterizedTest
    @EnumSource(
        value = SegmentType::class,
        mode = EnumSource.Mode.INCLUDE,
        names = ["COURIER", "FULFILLMENT"]
    )
    @DisplayName("Проверка, что валидна обработка 48 чекпоинта")
    fun isEligibleOnlyCheckpoint48(segmentType: SegmentType) {
        val segment = mockProcessorWaybillSegment(
            history = DEFAULT_SEGMENT_48
        )
        segment.segmentType = segmentType

        processor.isEligible(segment) shouldBe true
    }

    @Test
    @DisplayName("Проверка, что сегмент не обрабатывается, если нет 45 или48 чекпоинта.")
    fun isNonEligibleWhenNoProperCheckpoint() {
        val segment = mockProcessorWaybillSegment(
            history = mutableSetOf(
                WaybillSegmentStatusHistory(
                    id = 1L,
                    status = SegmentStatus.TRACK_RECEIVED,
                    date = DEFAULT_LATEST_TIME,
                    created = DEFAULT_LATEST_TIME,
                )
            )
        )
        processor.isEligible(segment) shouldBe false
    }

    @ParameterizedTest
    @EnumSource(
        value = PartnerSubtype::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = [
            "TAXI_EXPRESS",
            "TAXI_LAVKA",
            "MARKET_LOCKER",
            "MARKET_OWN_PICKUP_POINT",
            "MARKET_COURIER",
            "PARTNER_CONTRACT_DELIVERY",
            "PARTNER_PICKUP_POINT_IP"]
    )
    @DisplayName(
        "Проверка, что сегмент не обрабатывается, если нет возможности подсчитать дедлайн " +
            "(нет валидного подтипа партнера и неявляется почтой)."
    )
    fun isNonEligibleWhenWrongPartnerSubtype(unsupportedPartnerSubtype: PartnerSubtype) {
        val segment = mockProcessorWaybillSegment(
            partnerSubtype = unsupportedPartnerSubtype
        )
        processor.isEligible(segment) shouldBe false
    }

    @Test
    @DisplayName(
        "Проверка, что сегмент не обрабатывается, если нет возможности подсчитать дедлайн " +
            "(отсутсвует подпартнер и не является почтой)"
    )
    fun isNonEligibleWhenPartnerSubtypeNull() {
        val segment = mockProcessorWaybillSegment(
            partnerSubtype = null
        )
        processor.isEligible(segment) shouldBe false
    }

    @Test
    @DisplayName("Проверка, что сегмент обрабатывается, если нет возможности подсчитать дедлайн, но является почтой.")
    fun isEligibleWhenNoSubpartnerAndIsPost() {
        val segment = mockProcessorWaybillSegment(
            partnerSubtype = null,
            deliveryType = DeliveryType.POST
        )

        processor.isEligible(segment) shouldBe true
    }

    @Test
    @DisplayName("Проверка, что сегмент не обрабатывается, если сегмент OnDemand")
    fun notEligibleWhenOnDemand() {
        val segment = mockProcessorWaybillSegment()
        segment.partnerType = PartnerType.DELIVERY
        segment.waybillSegmentTags = mutableSetOf(WaybillSegmentTag.ON_DEMAND)

        processor.isEligible(segment) shouldBe false
    }

    private fun mockProcessorWaybillSegment(
        history: MutableSet<WaybillSegmentStatusHistory> = mutableSetOf(
            WaybillSegmentStatusHistory(
                status = SegmentStatus.TRANSIT_PICKUP,
                date = DEFAULT_LATEST_TIME,
                created = Instant.parse("2021-01-01T10:00:02.00Z"),
            ),
            WaybillSegmentStatusHistory(
                status = SegmentStatus.TRANSIT_PICKUP,
                date = Instant.parse("2021-01-01T15:00:00.00Z"),
                created = Instant.parse("2021-01-01T15:00:02.00Z"),
            ),
            WaybillSegmentStatusHistory(
                status = SegmentStatus.TRANSIT_PICKUP,
                date = Instant.parse("2021-01-01T14:03:00.00Z"),
                created = Instant.parse("2021-01-01T14:10:02.00Z"),
            ),
            WaybillSegmentStatusHistory(
                status = SegmentStatus.TRANSIT_TRANSPORTATION_RECIPIENT,
                date = Instant.parse("2021-01-01T16:00:00.00Z"),
                created = Instant.parse("2021-01-01T13:12:02.00Z"),
            ),
            WaybillSegmentStatusHistory(
                status = SegmentStatus.INFO_RECEIVED,
                date = Instant.parse("2021-01-01T14:00:00.00Z"),
                created = Instant.parse("2021-01-01T12:00:02.00Z"),
            ),
        ),
        partnerSubtype: PartnerSubtype? = PartnerSubtype.MARKET_COURIER,
        deliveryType: DeliveryType = DeliveryType.COURIER,
        segmentType: SegmentType = SegmentType.COURIER,
    ): WaybillSegment {
        val segment = WaybillSegment(
            id = 1L,
            partnerSubtype = partnerSubtype,
            partnerType = PartnerType.DELIVERY,
            segmentType = segmentType
        )
        val order = LomOrder(
            deliveryType = deliveryType,
            platformClientId = PlatformClient.BERU.id
        )
        segment.waybillSegmentStatusHistory = history
        order.waybill = mutableListOf(segment)
        segment.order = order
        return segment
    }

    companion object {
        private val DEFAULT_LATEST_TIME = Instant.parse("2021-01-01T18:00:00.00Z")

        private val DEFAULT_SEGMENT_45 = mutableSetOf(
            WaybillSegmentStatusHistory(
                status = SegmentStatus.TRANSIT_PICKUP,
                date = DEFAULT_LATEST_TIME,
                created = Instant.parse("2021-01-01T12:00:02.00Z"),
            )
        )

        private val DEFAULT_SEGMENT_48 = mutableSetOf(
            WaybillSegmentStatusHistory(
                status = SegmentStatus.TRANSIT_TRANSPORTATION_RECIPIENT,
                date = DEFAULT_LATEST_TIME,
                created = Instant.parse("2021-01-01T12:00:02.00Z"),
            )
        )

        private val partnerSubtypeDeadlines = mapOf(
            PartnerSubtype.TAXI_EXPRESS to DeadlineCalculator { Duration.ofDays(7) },
            PartnerSubtype.TAXI_LAVKA to DeadlineCalculator { Duration.ofDays(7) },
            PartnerSubtype.MARKET_LOCKER to DeadlineCalculator { Duration.ofDays(10) },
            PartnerSubtype.MARKET_OWN_PICKUP_POINT to DeadlineCalculator { Duration.ofDays(20) },
            PartnerSubtype.MARKET_COURIER to DeadlineCalculator { Duration.ofDays(1) },
            PartnerSubtype.PARTNER_CONTRACT_DELIVERY to
                DeadlineCalculator { deliveryType: DeliveryType? ->
                    if (deliveryType == DeliveryType.POST) Duration.ofDays(7)
                    else Duration.ofDays(3)
                },
            PartnerSubtype.PARTNER_PICKUP_POINT_IP to DeadlineCalculator { Duration.ofDays(20) }
        )
    }
}
