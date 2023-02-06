package ru.yandex.market.logistics.mqm.service.processor.planfact

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import ru.yandex.market.logistics.mqm.entity.lom.LomOrder
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegmentStatusHistory
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerType
import ru.yandex.market.logistics.mqm.entity.lom.enums.PlatformClient
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.entity.lom.enums.WaybillSegmentTag
import ru.yandex.market.logistics.mqm.utils.TestableSettingsService
import java.time.Instant

//TODO DELIVERY-36218 удалить ExpressRecipientTransmitPlanFactProcessor и тест
@Deprecated("Заменен на NewExpressRecipientTransmitPlanFactProcessorTest")
class ExpressRecipientTransmitPlanFactProcessorTest {

    companion object {
        private val FIXED_TIME = Instant.parse("2021-01-01T15:00:00.00Z")
    }

    private val settingSegment = TestableSettingsService()
    private val processor = ExpressRecipientTransmitPlanFactProcessor(settingSegment)

    @Test
    @DisplayName("Расчет ожидаемого времени вручения заказа курьером после получения 35 чекпоинта с новым дедлайном")
    fun calculateExpectedDatetime() {
        val expectedDeadline = FIXED_TIME.plus(ExpressRecipientTransmitPlanFactProcessor.TIMEOUT)
        val waybillSegment = mockWaybillSegment()
        waybillSegment.waybillSegmentTags = mutableSetOf(WaybillSegmentTag.CALL_COURIER)
        waybillSegment.waybillSegmentStatusHistory = mutableSetOf(
            WaybillSegmentStatusHistory(
                status = SegmentStatus.TRANSIT_COURIER_RECEIVED,
                date = FIXED_TIME,
            )
        )
        Assertions.assertThat(processor.calculateExpectedDatetime(waybillSegment)).isEqualTo(expectedDeadline)
    }

    @Test
    @DisplayName("Расчет ожидаемого времени вручения заказа курьером после получения 35 чекпоинта")
    fun calculateExpectedDatetimeLegacy() {
        val processorLegacy = ExpressRecipientTransmitPlanFactProcessor(settingSegment)
        val expectedDeadline = FIXED_TIME.plus(ExpressRecipientTransmitPlanFactProcessor.TIMEOUT)
        val waybillSegment = mockWaybillSegment()
        waybillSegment.waybillSegmentTags = mutableSetOf(WaybillSegmentTag.CALL_COURIER)
        waybillSegment.waybillSegmentStatusHistory = mutableSetOf(
            WaybillSegmentStatusHistory(
                status = SegmentStatus.TRANSIT_COURIER_RECEIVED,
                date = FIXED_TIME,
            )
        )
        Assertions.assertThat(processorLegacy.calculateExpectedDatetime(waybillSegment)).isEqualTo(expectedDeadline)
    }

    @Test
    @DisplayName("Проверка применимости процессора")
    fun isEligible() {
        val waybillSegment = mockWaybillSegment()
        waybillSegment.waybillSegmentTags = mutableSetOf(WaybillSegmentTag.CALL_COURIER)
        waybillSegment.waybillSegmentStatusHistory = mutableSetOf(
            WaybillSegmentStatusHistory(
                status = SegmentStatus.TRANSIT_COURIER_RECEIVED,
                date = FIXED_TIME,
            )
        )
        Assertions.assertThat(processor.isEligible(waybillSegment)).isTrue
    }

    @Test
    @DisplayName("Процессор не применим, если нет тега CALL_COURIER")
    fun isNotEligibleIfNoTag() {
        val waybillSegment = mockWaybillSegment()
        waybillSegment.waybillSegmentStatusHistory = mutableSetOf(
            WaybillSegmentStatusHistory(
                status = SegmentStatus.TRANSIT_COURIER_RECEIVED,
                date = FIXED_TIME,
            )
        )
        Assertions.assertThat(processor.isEligible(waybillSegment)).isFalse
    }

    @Test
    @DisplayName("Процессор не применим, если нет нужного чекпоинта в истории")
    fun isNotEligibleIfCheckpointInHistory() {
        val waybillSegment = mockWaybillSegment()
        waybillSegment.waybillSegmentTags = mutableSetOf(WaybillSegmentTag.CALL_COURIER)
        val eligible = processor.isEligible(waybillSegment)
        Assertions.assertThat(eligible).isFalse
    }

    private fun mockWaybillSegment(): WaybillSegment {
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
        return segment
    }
}
