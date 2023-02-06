package ru.yandex.market.logistics.mqm.service.processor.planfact

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.embedded.DeliveryInterval
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerType
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.entity.lom.enums.WaybillSegmentTag
import ru.yandex.market.logistics.mqm.utils.TestableSettingsService
import ru.yandex.market.logistics.mqm.utils.joinInOrder
import ru.yandex.market.logistics.mqm.utils.writeWaybillSegmentCheckpoint
import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime

class OnDemandIntakePlanFactProcessorTest {

    private val settingSegment = TestableSettingsService()
    private val processor = OnDemandIntakePlanFactProcessor(settingSegment)

    @Test
    @DisplayName("Расчет ожидаемого времени после получения 30 чекпоинта")
    fun calculateExpectedDatetime() {
        val waybillSegment = createSegment()
        val deliveryDate = waybillSegment.order!!.deliveryInterval.deliveryDateMax!!
        val expectedDeadline = ZonedDateTime.of(
            deliveryDate,
            OnDemandIntakePlanFactProcessor.TIMEOUT,
            DateTimeUtils.MOSCOW_ZONE
        ).toInstant()

        processor.calculateExpectedDatetime(waybillSegment) shouldBe expectedDeadline
    }

    @Test
    @DisplayName("Проверка применимости процессора при типе сегмента PICKUP")
    fun isEligibleIfSegmentTypePickup() {
        processor.isEligible(createSegment()) shouldBe true
    }

    @Test
    @DisplayName("Проверка применимости процессора при типе сегмента COURIER")
    fun isEligibleIfSegmentTypeCourier() {
        processor.isEligible(createSegment(segmentType = SegmentType.COURIER)) shouldBe true
    }

    @Test
    @DisplayName("Процессор неприменим если тип сегмента неверный")
    fun isNonEligibleIfSegmentTypeIsWrong() {
        processor.isEligible(createSegment(segmentType = SegmentType.SUPPLIER)) shouldBe false
    }

    @Test
    @DisplayName("Процессор неприменим если тип передыдущего сегмента неверный")
    fun isNonEligibleIfPreviousSegmentTypeIsWrong() {
        processor.isEligible(createSegment(previousSegmentType = SegmentType.SUPPLIER)) shouldBe false
    }

    @Test
    @DisplayName("Процессор неприменим, если нет тега ON_DEMAND")
    fun isNotEligibleIfNoTag() {
        processor.isEligible(createSegment(withTag = false)) shouldBe false
    }

    @Test
    @DisplayName("Процессор неприменим, если есть тег DEFERRED_COURIER")
    fun isNotEligibleIfHasDeferredCourierTag() {
        processor.isEligible(createSegment(withWrongTag = true)) shouldBe false
    }

    @Test
    @DisplayName("Процессор неприменим если нет 30 чекпоинта в истории предыдущего сегмента")
    fun isNonEligibleWithoutCheckpointInHistory() {
        processor.isEligible(createSegment(previousSegmentStatus = SegmentStatus.OUT)) shouldBe false
    }

    @Test
    @DisplayName("Процессор не применим, если нет предыдущего сегмента")
    fun isNotEligibleWithoutPreviousSegment() {
        processor.isEligible(createSegment(hasPreviousSegment = false)) shouldBe false
    }

    private fun createSegment(
        segmentType: SegmentType = SegmentType.PICKUP,
        previousSegmentType: SegmentType = SegmentType.MOVEMENT,
        previousSegmentStatus: SegmentStatus = SegmentStatus.TRANSIT_DELIVERY_TRANSPORTATION,
        withTag: Boolean = true,
        withWrongTag: Boolean = false,
        hasPreviousSegment: Boolean = true
    ): WaybillSegment {
        val currentSegment = WaybillSegment(
            partnerType = PartnerType.DELIVERY,
            segmentType = segmentType
        )
        val previousSegment = WaybillSegment(
            partnerType = PartnerType.DELIVERY,
            segmentType = previousSegmentType
        )

        val waybillSegmentTags = mutableSetOf<WaybillSegmentTag>()
        if (withTag) waybillSegmentTags.add(WaybillSegmentTag.ON_DEMAND)
        if (withWrongTag) waybillSegmentTags.add(WaybillSegmentTag.DEFERRED_COURIER)
        currentSegment.waybillSegmentTags = waybillSegmentTags

        writeWaybillSegmentCheckpoint(
            previousSegment,
            previousSegmentStatus,
            Instant.parse("2021-01-01T00:00:00.00Z")
        )

        val segments = if (hasPreviousSegment) listOf(previousSegment, currentSegment) else listOf(currentSegment)
        joinInOrder(segments).apply {
            deliveryInterval = DeliveryInterval(deliveryDateMax = LocalDate.of(2021, 1, 2))
        }
        return currentSegment
    }
}
