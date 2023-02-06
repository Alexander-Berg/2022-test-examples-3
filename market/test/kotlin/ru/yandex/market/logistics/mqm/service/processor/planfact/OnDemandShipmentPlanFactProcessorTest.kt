package ru.yandex.market.logistics.mqm.service.processor.planfact

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.entity.lom.enums.WaybillSegmentTag
import ru.yandex.market.logistics.mqm.utils.TestableSettingsService
import ru.yandex.market.logistics.mqm.utils.createWaybillSegmentWithCheckpoint
import java.time.ZonedDateTime

class OnDemandShipmentPlanFactProcessorTest {

    private val settingSegment = TestableSettingsService()
    private val processor = OnDemandShipmentPlanFactProcessor(settingSegment)

    @Test
    @DisplayName("Расчет ожидаемого времени после получения 10 или 110 чекпоинта")
    fun calculateExpectedDatetime() {
        val waybillSegment = createSegment()
        val deliveryDate = waybillSegment.order!!.deliveryInterval.deliveryDateMax!!
        val expectedDeadline = ZonedDateTime.of(
            deliveryDate,
            OnDemandShipmentPlanFactProcessor.TIMEOUT,
            DateTimeUtils.MOSCOW_ZONE
        ).toInstant()

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
    @DisplayName("Процессор неприменим, если нет тега ON_DEMAND")
    fun isNotEligibleIfNoTag() {
        processor.isEligible(createSegment(withTag = false)) shouldBe false
    }

    @Test
    @DisplayName("Процессор неприменим если нет 10 и 110 чекпоинта в истории")
    fun isNonEligibleWithoutCheckpointInHistory() {
        processor.isEligible(createSegment(segmentStatus = SegmentStatus.OUT)) shouldBe false
    }

    private fun createSegment(
        segmentType: SegmentType = SegmentType.MOVEMENT,
        segmentStatus: SegmentStatus = SegmentStatus.IN,
        withTag: Boolean = true
    ): WaybillSegment {
        val waybillSegment = createWaybillSegmentWithCheckpoint(
            segmentType,
            segmentStatus
        )

        if (withTag) waybillSegment.apply {
            waybillSegmentTags = mutableSetOf(WaybillSegmentTag.ON_DEMAND)
        }
        return waybillSegment
    }
}
