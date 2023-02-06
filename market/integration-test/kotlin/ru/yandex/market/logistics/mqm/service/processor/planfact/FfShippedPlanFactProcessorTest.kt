package ru.yandex.market.logistics.mqm.service.processor.planfact

import com.nhaarman.mockitokotlin2.whenever
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.ArgumentMatchers.any
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.management.client.LMSClient
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse
import ru.yandex.market.logistics.management.entity.response.schedule.CalendarHolidaysResponse
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.entity.lom.PartnerSettings
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.embedded.WaybillShipment
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerType
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.exception.planfact.ShipmentDatetimeNotFoundException
import ru.yandex.market.logistics.mqm.utils.joinInOrder
import ru.yandex.market.logistics.mqm.utils.writeWaybillSegmentCheckpoint
import java.time.Instant
import java.time.LocalDate
import java.util.Optional

@DisplayName("Тесты процессора расчета план-фактов для отрузки ФФ")
class FfShippedPlanFactProcessorTest: AbstractContextualTest() {

    @Autowired
    private lateinit var lmsClient: LMSClient

    @Autowired
    private lateinit var processor: FfShippedPlanFactProcessor

    @Test
    @DisplayName("Применимость процессора")
    fun shouldBeEligible() {
        val waybillSegment = prepareWaybillSegment()
        processor.isEligible(waybillSegment) shouldBe true
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Процессор не применяется, если неправильный тип партнёра")
    @EnumSource(
        value = PartnerType::class,
        names = ["FULFILLMENT"],
        mode = EnumSource.Mode.EXCLUDE
    )
    fun isEligibleReturnFalseIfWrongPartnerType(unsupportedPartnerType: PartnerType) {
        val waybillSegment = prepareWaybillSegment(partnerType = unsupportedPartnerType)
        processor.isEligible(waybillSegment) shouldBe false
    }

    @Test
    @DisplayName("Процессор не применяется для возвратного сегмента")
    fun isEligibleReturnFalseForReturn() {
        val waybillSegment = prepareWaybillSegment(isReturn = true)
        processor.isEligible(waybillSegment) shouldBe false
    }

    @Test
    @DisplayName("Процессор не применяется для экспресса")
    fun isEligibleReturnFalseForExpress() {
        val waybillSegment = prepareWaybillSegment(isFromExpress = true)
        processor.isEligible(waybillSegment) shouldBe false
    }

    @Test
    @DisplayName("Процессор не применяется, если нет даты отгрузки")
    fun isEligibleReturnFalseIfNoShipmentDate() {
        val waybillSegment = prepareWaybillSegment(plannedShipmentDate = null)
        processor.isEligible(waybillSegment) shouldBe false
    }

    @Test
    @DisplayName("Расчет дедлайна для сегментов у которых заполнен shipment.dateTime")
    fun calculationFromShipmentDateTime() {
        val waybillSegment = prepareWaybillSegment(shipmentDateTime = DEFAULT_SHIPMENT_DATE_TIME)
        processor.calculateExpectedDatetime(waybillSegment) shouldBe DEFAULT_SHIPMENT_DATE_TIME
    }

    @Test
    @DisplayName("Исключение при расчете дедлайна для сегмента без shipmentDatetime")
    fun exceptionOnCalculatingDeadlineWithoutShipmentDatetime() {
        val waybillSegment = prepareWaybillSegment()
        assertThrows<ShipmentDatetimeNotFoundException> { processor.calculateExpectedDatetime(waybillSegment) }
    }

    private fun prepareWaybillSegment(
        partnerType: PartnerType = PartnerType.FULFILLMENT,
        isFromExpress: Boolean = false,
        isReturn: Boolean = false,
        plannedShipmentDate: LocalDate? = DEFAULT_PLANNED_SHIPMENT_DATE,
        shipmentDateTime: Instant? = null,
        checkpoint120Time: Instant? = Instant.parse("2021-09-10T10:00:00.00Z"),
        holidays: List<LocalDate> = listOf()
    ): WaybillSegment {
        val waybillShipment = WaybillShipment(date = plannedShipmentDate, dateTime = shipmentDateTime)
        val waybillSegment = WaybillSegment(
            partnerId = 1,
            partnerType = partnerType,
            segmentType = SegmentType.FULFILLMENT,
            shipment = waybillShipment,
            partnerSettings = PartnerSettings(dropshipExpress = isFromExpress)
        )
        checkpoint120Time?.let { writeWaybillSegmentCheckpoint(waybillSegment, SegmentStatus.TRANSIT_PREPARED, it) }
        val deliverySegment = WaybillSegment(
            partnerId = 2,
            partnerType = PartnerType.DELIVERY
        )
        if (isReturn) {
            joinInOrder(listOf(deliverySegment, waybillSegment))
        } else {
            joinInOrder(listOf(waybillSegment, deliverySegment))
        }
        mockLmsClient(
            partnerFrom = waybillSegment.partnerId!!,
            holidays = holidays
        )
        return waybillSegment
    }

    private fun mockLmsClient(
        partnerFrom: Long,
        holidays: List<LocalDate>
    ) {
        whenever(lmsClient.getPartner(partnerFrom))
            .thenReturn(Optional.of(PartnerResponse.newBuilder().calendarId(1L).build()))
        whenever(lmsClient.getHolidays(any()))
            .thenReturn(listOf(CalendarHolidaysResponse.builder().days(holidays).build()))
    }

    companion object {
        private val DEFAULT_PLANNED_SHIPMENT_DATE: LocalDate = LocalDate.of(2021, 9, 20)
        private val DEFAULT_SHIPMENT_DATE_TIME = Instant.parse("2021-10-27T14:00:00Z")
    }
}
