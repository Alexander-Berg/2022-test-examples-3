package ru.yandex.market.logistics.mqm.service.processor.planfact

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.junit.jupiter.MockitoExtension
import ru.yandex.market.logistics.mqm.entity.lom.PartnerSettings
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.embedded.WaybillShipment
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerType
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.entity.lom.enums.WaybillSegmentTag
import ru.yandex.market.logistics.mqm.exception.planfact.ShipmentDatetimeNotFoundException
import ru.yandex.market.logistics.mqm.utils.TestableSettingsService
import ru.yandex.market.logistics.mqm.utils.joinInOrder
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@ExtendWith(MockitoExtension::class)
class FfPreparedPlanFactProcessorTest {
    private val settingService = TestableSettingsService()

    private lateinit var processor: FfPreparedPlanFactProcessor

    @BeforeEach
    fun setUp() {
        processor = FfPreparedPlanFactProcessor(settingService)
    }

    @Test
    @DisplayName("Проверка применимости процессора")
    fun isEligible() {
        val current = createSegments()
        processor.isEligible(current) shouldBe true
    }

    @Test
    @DisplayName("Процессор не применим, если есть экспресс")
    fun isNonEligibleIfExpress() {
        val current = createSegments(isDropshipExpress = true)
        processor.isEligible(current) shouldBe false
    }

    @Test
    @DisplayName("Процессор не применим, если RETURN")
    fun isNonEligibleIfNoReturn() {
        val current = createSegments(hasReturn = true)
        processor.isEligible(current) shouldBe false
    }

    @Test
    @DisplayName("Процессор не применим, если нет следующего сегмента")
    fun isNonEligibleIfNoNext() {
        val current = createSegments(hasNextSegment = false)
        processor.isEligible(current) shouldBe false
    }

    @ParameterizedTest
    @EnumSource(
        value = PartnerType::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["FULFILLMENT"]
    )
    @DisplayName("Процессор не применим, если тип текущего сегмента не FULFILLMENT")
    fun isNonEligibleIfNoFf(currentPartnerType: PartnerType) {
        val current = createSegments(currentPartnerType = currentPartnerType)
        processor.isEligible(current) shouldBe false
    }

    @Test
    @DisplayName("Процессор не применим, если нет barcode")
    fun isNonEligibleIfNoOrderBarcode() {
        val current = createSegments(hasOrderBarcode = false)
        processor.isEligible(current) shouldBe false
    }

    @Test
    @DisplayName("Расчет дедлайна c shipment.dateTime")
    fun calculationPlanFromShipmentDateTime() {
        val current = createSegments(shipmentDateTime = SHIPMENT_DATE_TIME)
        val deadline = processor.calculateExpectedDatetime(current)
        deadline shouldBe SHIPMENT_DATE_TIME.minus(1, ChronoUnit.HOURS)
    }

    @Test
    @DisplayName("Расчет дедлайна для сегмента без shipmentDatetime")
    fun calculateDeadlineWithoutShipmentDatetime() {
        val current = createSegments()
        assertThrows<ShipmentDatetimeNotFoundException> { processor.calculateExpectedDatetime(current) }
    }

    private fun createSegments(
        currentPartnerType: PartnerType = PartnerType.FULFILLMENT,
        isDropshipExpress: Boolean = false,
        hasReturn: Boolean = false,
        hasNextSegment: Boolean = true,
        shipmentDateTime: Instant? = null,
        hasOrderBarcode: Boolean = true
    ): WaybillSegment {
        val waybillSegmentTags = if (hasReturn) mutableSetOf(WaybillSegmentTag.RETURN) else mutableSetOf()
        val currentSegment = WaybillSegment(
            partnerId = 111L,
            segmentType = SegmentType.FULFILLMENT,
            partnerType = currentPartnerType,
            partnerSettings = PartnerSettings(dropshipExpress = isDropshipExpress),
            waybillSegmentTags = waybillSegmentTags,
            shipment = WaybillShipment(date = SHIPMENT_TIME, dateTime = shipmentDateTime)
        )
        val nextSegment = WaybillSegment(
            partnerId = 222L,
        )
        val order = if (hasNextSegment) {
            joinInOrder(listOf(currentSegment, nextSegment))
        } else {
            joinInOrder(listOf(currentSegment))
        }
        if (hasOrderBarcode) {
            order.apply { barcode = ORDER_BARCODE }
        }
        return currentSegment
    }

    companion object {
        private val SHIPMENT_TIME = LocalDate.of(2021, 3, 10)
        private val SHIPMENT_DATE_TIME = Instant.parse("2021-10-27T14:00:00Z")
        private const val ORDER_BARCODE = "BC"
    }
}
