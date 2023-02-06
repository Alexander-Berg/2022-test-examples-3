package ru.yandex.market.logistics.mqm.service.processor.planfact

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.entity.lom.enums.WaybillSegmentTag
import ru.yandex.market.logistics.mqm.utils.at
import ru.yandex.market.logistics.mqm.utils.createWaybillSegmentWithCheckpoint
import ru.yandex.market.logistics.mqm.utils.joinInOrder
import java.time.LocalTime

@Deprecated("Заменен на waybillSegment.RecipientTransmitPlanFactProcessorTest")
class RecipientTransmitPlanFactProcessorTest {

    private val processor = RecipientTransmitPlanFactProcessor()

    @Test
    @DisplayName("Расчет ожидаемого времени после получения 48 чекпоинта")
    fun calculateExpectedDatetime() {
        val waybillSegment = createSegment()
        val endDate = waybillSegment.order!!.deliveryInterval.deliveryDateMax!!
        val endTime = waybillSegment.order!!.deliveryInterval.toTime ?: LocalTime.MAX
        val expectedDeadline = endDate.at(endTime)
        processor.calculateExpectedDatetime(waybillSegment) shouldBe expectedDeadline
    }

    @Test
    @DisplayName("Проверка применимости процессора")
    fun isEligible() {
        processor.isEligible(createSegment()) shouldBe true
    }

    @Test
    @DisplayName("Процессор неприменим если тип сегмента неверный")
    fun isNonEligibleIfSegmentTypeIsWrong() {
        processor.isEligible(createSegment(segmentType = SegmentType.PICKUP)) shouldBe false
    }

    @Test
    @DisplayName("Процессор неприменим, если есть тег CALL_COURIER")
    fun isNotEligibleWithCallCourierTag() {
        processor.isEligible(createSegment(segmentTag = WaybillSegmentTag.CALL_COURIER)) shouldBe false
    }

    @Test
    @DisplayName("Процессор неприменим, если есть тег ON_DEMAND")
    fun isNotEligibleWithOnDemandTag() {
        processor.isEligible(createSegment(segmentTag = WaybillSegmentTag.ON_DEMAND)) shouldBe false
    }

    @Test
    @DisplayName("Процессор неприменим если нет 48 чекпоинта в истории")
    fun isNonEligibleWithoutCheckpointInHistory() {
        processor.isEligible(createSegment(segmentStatus = SegmentStatus.IN)) shouldBe false
    }

    @Test
    @DisplayName("Процессор неприменим, если это не последний DELIVERY сегмент")
    fun isNotEligibleIfNotLastDeliverySegment() {
        val firstSegment = createSegment().apply { id = 1L }
        val lastSegment = createSegment().apply { id = 2L }
        joinInOrder(listOf(firstSegment, lastSegment))
        processor.isEligible(firstSegment) shouldBe false
    }

    private fun createSegment(
        segmentType: SegmentType = SegmentType.COURIER,
        segmentStatus: SegmentStatus = SegmentStatus.TRANSIT_TRANSPORTATION_RECIPIENT,
        segmentTag: WaybillSegmentTag? = null
    ): WaybillSegment {
        val waybillSegment = createWaybillSegmentWithCheckpoint(
            segmentType,
            segmentStatus
        )

        if (segmentTag != null) waybillSegment.apply {
            waybillSegmentTags = mutableSetOf(segmentTag)
        }
        return waybillSegment
    }
}
