package ru.yandex.market.logistics.mqm.service.processor.planfact

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.entity.lom.LomOrder
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegmentStatusHistory
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerType
import ru.yandex.market.logistics.mqm.entity.lom.enums.PlatformClient
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.entity.lom.enums.WaybillSegmentTag
import ru.yandex.market.logistics.mqm.utils.TestableSettingsService
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.Collections

class ExpressReturnPlanFactProcessorTest {

    companion object {
        private val FIXED_TIME = Instant.parse("2021-01-02T09:00:00.00Z")
    }

    private val settingService = TestableSettingsService()
    private val processor = ExpressReturnPlanFactProcessor(settingService,
        Clock.fixed(Instant.parse("2020-02-14T11:15:25.00Z"), DateTimeUtils.MOSCOW_ZONE))

    @Test
    @DisplayName("Расчет ожидаемого времени.")
    fun calculateExpectedDatetime() {
        val expectedDeadline = FIXED_TIME.plus(Duration.ofDays(1))
        val waybillSegment = mockWaybillSegment()
        processor.calculateExpectedDatetime(waybillSegment) shouldBe expectedDeadline
    }

    @Test
    @DisplayName("Расчет ожидаемого времени по легаси методу.")
    fun calculateExpectedDatetimeLegacy() {
        val processorLegacy = ExpressRecipientTransmitPlanFactProcessor(settingService)
        val expectedDeadline = FIXED_TIME.plus(ExpressRecipientTransmitPlanFactProcessor.TIMEOUT)
        val waybillSegment = mockWaybillSegment()
        processorLegacy.calculateExpectedDatetime(waybillSegment) shouldBe expectedDeadline
    }

    @Test
    @DisplayName("Проверка применимости процессора")
    fun isEligible() {
        val waybillSegment = mockWaybillSegment()
        processor.isEligible(waybillSegment) shouldBe true
    }

    @Test
    @DisplayName("Процессор не применим, если нет тега CALL_COURIER")
    fun isNotEligibleIfNoTag() {
        val waybillSegment = mockWaybillSegment(tags = Collections.emptySet())
        processor.isEligible(waybillSegment) shouldBe false
    }

    @Test
    @DisplayName("Процессор не применим, если нет нужного чекпоинта в истории")
    fun isNotEligibleIfCheckpointInHistory() {
        val waybillSegment = mockWaybillSegment(history = Collections.emptySet())
        processor.isEligible(waybillSegment) shouldBe false
    }

    private fun mockWaybillSegment(
        tags: MutableSet<WaybillSegmentTag> = mutableSetOf(WaybillSegmentTag.CALL_COURIER),
        history: MutableSet<WaybillSegmentStatusHistory> = mutableSetOf(
            WaybillSegmentStatusHistory(
                status = SegmentStatus.TRANSIT_COURIER_RECEIVED,
                date = FIXED_TIME,
            )
        )
    ): WaybillSegment {
        val order = LomOrder()
        order.platformClientId = PlatformClient.BERU.id
        val segment = WaybillSegment(
            waybillSegmentIndex = 1,
            partnerId = 1L,
            externalId = "externalId",
            partnerType = PartnerType.DELIVERY,
            segmentType = SegmentType.COURIER,
        )

        segment.order = order

        segment.waybillSegmentTags = tags
        segment.waybillSegmentStatusHistory = history
        return segment
    }
}
