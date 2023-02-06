package ru.yandex.market.logistics.mqm.service.processor.planfact

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.entity.lom.enums.WaybillSegmentTag
import ru.yandex.market.logistics.mqm.utils.TestableSettingsService
import ru.yandex.market.logistics.mqm.utils.createWaybillSegmentWithCheckpoint
import java.time.Instant

class ExpressFinalStatusPlanFactProcessorTest {

    private val settingSegment = TestableSettingsService()
    private val processor = ExpressFinalStatusPlanFactProcessor(settingSegment)

    @Test
    @DisplayName("Расчет ожидаемого времени после получения 49 чекпоинта")
    fun calculateExpectedDatetime() {
        val expectedDeadline = FIXED_TIME.plus(ExpressFinalStatusPlanFactProcessor.TIMEOUT)
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
    @DisplayName("Процессор неприменим, если нет тега CALL_COURIER")
    fun isNotEligibleIfNoTag() {
        processor.isEligible(createSegment(withTag = false)) shouldBe false
    }

    @Test
    @DisplayName("Процессор неприменим если нет 49 чекпоинта в истории")
    fun isNonEligibleWithoutCheckpointInHistory() {
        processor.isEligible(createSegment(segmentStatus = SegmentStatus.IN)) shouldBe false
    }

    private fun createSegment(
        segmentType: SegmentType = SegmentType.COURIER,
        segmentStatus: SegmentStatus = SegmentStatus.TRANSIT_TRANSMITTED_TO_RECIPIENT,
        withTag: Boolean = true
    ): WaybillSegment {
        val waybillSegment = createWaybillSegmentWithCheckpoint(
            segmentType,
            segmentStatus
        )

        if (withTag) waybillSegment.apply {
            waybillSegmentTags = mutableSetOf(WaybillSegmentTag.CALL_COURIER)
        }
        return waybillSegment
    }

    companion object {
        private val FIXED_TIME = Instant.parse("2021-01-01T15:00:00.00Z")
    }
}
