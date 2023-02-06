package ru.yandex.market.logistics.mqm.service.processor.planfact

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.embedded.WaybillShipment
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerType
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.entity.lom.enums.WaybillSegmentTag
import ru.yandex.market.logistics.mqm.utils.TestableSettingsService
import ru.yandex.market.logistics.mqm.utils.joinInOrder
import ru.yandex.market.logistics.mqm.utils.writeWaybillSegmentCheckpoint
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

internal class AutoCancelAfterDropshipPlanFactProcessorTest {

    private val settingService = TestableSettingsService()
    private val processor = AutoCancelAfterDropshipPlanFactProcessor(settingService)

    @ParameterizedTest
    @EnumSource(
        value = PartnerType::class,
        names = [
            "DELIVERY",
        ],
        mode = EnumSource.Mode.EXCLUDE
    )
    @DisplayName("Проверка применимости процессора")
    fun isEligible(supportedType: PartnerType) {
        processor.isEligible(prepareSegments(partnerType = supportedType)) shouldBe true
    }

    @Test
    @DisplayName("Проверка, что процессор не применяется, если неподходящий тип текущего сегмента")
    fun isEligibleReturnFalseIfWrongCurrentSegment() {
        processor.isEligible(prepareSegments(partnerType = PartnerType.DELIVERY)) shouldBe false
    }

    @Test
    @DisplayName("Проверка, что процессор не применяется, если экспресс сегмент")
    fun isEligibleReturnFalseIfExpress() {
        processor.isEligible(prepareSegments(isExpress = true)) shouldBe false
    }

    @Test
    @DisplayName("Проверка, что процессор не применяется, если не второй сегмент")
    fun isEligibleReturnFalseIfNotSecond() {
        processor.isEligible(prepareSegments(asSecond = false)) shouldBe false
    }

    @ParameterizedTest
    @EnumSource(
        value = PartnerType::class,
        names = [
            "DROPSHIP",
        ],
        mode = EnumSource.Mode.EXCLUDE
    )
    @DisplayName("Проверка, что процессор не применяется, если неправильный тип предыдущего партнёра")
    fun isEligibleReturnFalseIfWrongPreviousPartnerType(unsupportedType: PartnerType) {
        assertSoftly {
            processor.isEligible(prepareSegments(previousPartnerType = unsupportedType)) shouldBe false
        }
    }

    @Test
    @DisplayName("Проверка, что процессор не применяется, если нет даты отгрузки")
    fun isEligibleReturnFalseIfNotShipmentDate() {
        processor.isEligible(prepareSegments(shipment = null)) shouldBe false
    }

    @Test
    @DisplayName("Проверка, что процессор не применяется, если нет чекпоинта на предыдущем сегменте")
    fun isEligibleReturnFalseIfNotCheckpoint() {
        processor.isEligible(prepareSegments(dropshipCheckpoint = SegmentStatus.OUT)) shouldBe false
    }

    @Test
    @DisplayName("Проверка расчёта дедлайна")
    fun calculateExpectedDatetime() {
        processor.calculateExpectedDatetime(prepareSegments(shipment = SHIPMENT_DATE)) shouldBe
            LocalDateTime.of(SHIPMENT_DATE, LocalTime.MAX)
                .atZone(DateTimeUtils.MOSCOW_ZONE)
                .toInstant()
                .plus(Duration.ofHours(48))
    }

    private fun prepareSegments(
        asSecond: Boolean = true,
        isExpress: Boolean = false,
        previousPartnerType: PartnerType = PartnerType.DROPSHIP,
        partnerType: PartnerType = PartnerType.DELIVERY,
        shipment: LocalDate? = SHIPMENT_DATE,
        dropshipCheckpoint: SegmentStatus = SegmentStatus.TRACK_RECEIVED,
    ): WaybillSegment {
        val segment = WaybillSegment(
            partnerType = partnerType,
            shipment = WaybillShipment(
                date = shipment,
            ),
        )
        if (isExpress) {
            segment.segmentType = SegmentType.COURIER
            segment.waybillSegmentTags!!.add(WaybillSegmentTag.CALL_COURIER)
        }
        val previousSegment = WaybillSegment(
            partnerType = previousPartnerType,
        )
        writeWaybillSegmentCheckpoint(previousSegment, dropshipCheckpoint, (Instant.parse("2021-01-01T15:00:00.00Z")))
        joinInOrder(if (asSecond) listOf(previousSegment, segment) else listOf(segment))
        return segment
    }

    companion object {
        private val SHIPMENT_DATE =
            LocalDate.ofInstant(Instant.parse("2021-01-01T15:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE)
    }
}
