package ru.yandex.market.logistics.mqm.service.processor.planfact

import com.nhaarman.mockitokotlin2.mock
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerType
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.utils.createWaybillSegmentWithCheckpoint
import java.time.Instant

class CteFfIntakeReturnPlanFactProcessorTest {

    private val processor = CteFfIntakeReturnPlanFactProcessor(mock())

    @Test
    @DisplayName("Расчет ожидаемого времени после получения 170 чекпоинта")
    fun calculateExpectedDatetime() {
        val expectedDeadline = FIXED_TIME.plus(CteFfIntakeReturnPlanFactProcessor.TIMEOUT)
        processor.calculateExpectedDatetime(createSegment()) shouldBe expectedDeadline
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
    @DisplayName("Процессор неприменим, если partnerType неверный")
    fun isNotEligibleIfPartnerTypeIsWrong() {
        processor.isEligible(createSegment(partnerType = PartnerType.OWN_DELIVERY)) shouldBe false
    }

    @Test
    @DisplayName("Процессор неприменим если нет 170 чекпоинта в истории")
    fun isNonEligibleWithoutCheckpointInHistory() {
        processor.isEligible(createSegment(segmentStatus = SegmentStatus.IN)) shouldBe false
    }

    private fun createSegment(
        partnerType: PartnerType = PartnerType.FULFILLMENT,
        segmentType: SegmentType = SegmentType.FULFILLMENT,
        segmentStatus: SegmentStatus = SegmentStatus.RETURN_ARRIVED,
    ): WaybillSegment {
        val waybillSegment = createWaybillSegmentWithCheckpoint(segmentType, segmentStatus)
        waybillSegment.partnerType = partnerType
        return waybillSegment
    }

    companion object {
        private val FIXED_TIME = Instant.parse("2021-01-01T15:00:00.00Z")
    }
}
