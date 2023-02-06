package ru.yandex.market.logistics.mqm.service.processor.planfact

import io.kotest.matchers.shouldBe
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.entity.lom.enums.WaybillSegmentTag
import ru.yandex.market.logistics.mqm.utils.TestableSettingsService
import ru.yandex.market.logistics.mqm.utils.joinInOrder
import java.time.LocalDate
import java.time.LocalTime

class DeferredCourierOrderPlanFactProcessorTest {

    private val settingService = TestableSettingsService()
    private val processor = DeferredCourierOrderPlanFactProcessor(settingService)

    @Test
    @DisplayName("Проверка применимости процессора с типом COURIER")
    fun isEligibleWithDefaults() {
        val current = createSegment()
        Assertions.assertThat(processor.isEligible(current)).isTrue
    }

    @Test
    @DisplayName("Проверка применимости процессора с типом GO_PLATFORM")
    fun isEligibleWithGoType() {
        val current = createSegment(currentSegmentType = SegmentType.GO_PLATFORM)
        Assertions.assertThat(processor.isEligible(current)).isTrue
    }

    @Test
    @DisplayName("Процессор не применим, если нет DEFERRED_COURIER")
    fun isNonEligibleIfNoDeferredCourier() {
        val current = createSegment(hasDeferredCourier = false)
        processor.isEligible(current) shouldBe false
    }

    @Test
    @DisplayName("Процессор не применим, если нет DeliveryInterval + FromTime")
    fun isNonEligibleIfNoOrderDeliveryIntervalFromTime() {
        val current = createSegment(orderDeliveryIntervalFromTime = null)
        processor.isEligible(current) shouldBe false
    }

    @Test
    @DisplayName("Процессор не применим, если нет DeliveryInterval + DateMin")
    fun isNonEligibleIfNoOrderDeliveryIntervalDateMin() {
        val current = createSegment(orderDeliveryIntervalDateMin = null)
        processor.isEligible(current) shouldBe false
    }

    @Test
    @DisplayName("Расчет ожидаемого времени приемки заказа")
    fun calculateDateTime() {
        val current = createSegment()
        val expectedDeadline = ORDER_DELIVERY_INTERVAL_DATE_MIN
            .atTime(ORDER_DELIVERY_INTERVAL_FROM_TIME)
            .atZone(DateTimeUtils.MOSCOW_ZONE)
            .minusHours(HOURS_BEFORE_START_TIME)
            .toInstant()
        Assertions.assertThat(processor.calculateExpectedDatetime(current)).isEqualTo(expectedDeadline)
    }

    private fun createSegment(
        currentSegmentType: SegmentType = SegmentType.COURIER,
        hasDeferredCourier: Boolean = true,
        orderDeliveryIntervalFromTime: LocalTime? = ORDER_DELIVERY_INTERVAL_FROM_TIME,
        orderDeliveryIntervalDateMin: LocalDate? = ORDER_DELIVERY_INTERVAL_DATE_MIN
    ): WaybillSegment {
        val waybillSegmentTags =
            if (hasDeferredCourier) mutableSetOf(WaybillSegmentTag.DEFERRED_COURIER) else mutableSetOf()
        val currentSegment = WaybillSegment(
            segmentType = currentSegmentType,
            waybillSegmentTags = waybillSegmentTags,
        )
        val order = joinInOrder(listOf(currentSegment))
        order.deliveryInterval.fromTime = orderDeliveryIntervalFromTime
        order.deliveryInterval.deliveryDateMin = orderDeliveryIntervalDateMin
        return currentSegment
    }

    companion object {
        private val ORDER_DELIVERY_INTERVAL_FROM_TIME = LocalTime.of(15, 0)
        private val ORDER_DELIVERY_INTERVAL_DATE_MIN = LocalDate.of(2021, 1, 2)
        private const val HOURS_BEFORE_START_TIME = 1L
    }
}
