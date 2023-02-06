package ru.yandex.market.logistics.mqm.service.processor.planfact

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegmentStatusHistory
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerType
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.entity.lom.enums.WaybillSegmentTag
import ru.yandex.market.logistics.mqm.utils.TestableSettingsService
import ru.yandex.market.logistics.mqm.utils.joinInOrder
import java.time.Duration
import java.time.Instant

class ExpressArrivingPlanFactProcessorTest {

    private val settingService = TestableSettingsService()
    private val processor = ExpressArrivingPlanFactProcessor(settingService)

    @Test
    @DisplayName("Проверка применимости процессора")
    fun isEligible() {
        processor.isEligible(createSegments()) shouldBe true
    }

    @Test
    @DisplayName("Процессор не применим, если не Экспресс заказ")
    fun isNonEligibleIfCurrentIsNotExpress() {
        processor.isEligible(createSegments(tag = WaybillSegmentTag.ON_DEMAND)) shouldBe false
    }

    @Test
    @DisplayName("Процессор не применим, если не сегмент курьера")
    fun isNonEligibleIfCurrentIsNotCourier() {
        processor.isEligible(createSegments(segmentType = SegmentType.FULFILLMENT)) shouldBe false
    }

    @Test
    @DisplayName("Процессор не применим, если не нужного чекпоинта")
    fun isNonEligibleIfNoCheckpoint() {
        processor.isEligible(createSegments(checkpoint = SegmentStatus.INFO_RECEIVED)) shouldBe false
    }

    @Test
    @DisplayName("Расчет ожидаемого времени")
    fun calculateDateTime() {
        processor.calculateExpectedDatetime(createSegments()) shouldBe
            CHECKPOINT_TIME.plus(Duration.ofHours(1))
    }

    private fun createSegments(
        segmentType: SegmentType = SegmentType.COURIER,
        tag: WaybillSegmentTag = WaybillSegmentTag.CALL_COURIER,
        checkpoint: SegmentStatus = SegmentStatus.TRANSIT_COURIER_FOUND,
        checkpointTime: Instant = CHECKPOINT_TIME,
    ): WaybillSegment {
        val segment = WaybillSegment(
            segmentType = segmentType,
            partnerType = PartnerType.DELIVERY,
            waybillSegmentTags = mutableSetOf(tag),
        ).apply {
            waybillSegmentStatusHistory = mutableSetOf(
                WaybillSegmentStatusHistory(
                    status = checkpoint,
                    date = checkpointTime,
                )
            )

        };
        joinInOrder(listOf(segment))
        return segment
    }

    companion object {
        private val CHECKPOINT_TIME = Instant.parse("2021-01-02T09:00:00.00Z")
    }
}
