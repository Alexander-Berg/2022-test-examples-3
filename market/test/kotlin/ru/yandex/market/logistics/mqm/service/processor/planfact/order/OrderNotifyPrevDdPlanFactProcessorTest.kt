package ru.yandex.market.logistics.mqm.service.processor.planfact.order

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import ru.yandex.market.logistics.mqm.configuration.properties.NotifyDeliveryDateProperties
import ru.yandex.market.logistics.mqm.service.ChangeOrderRequestService
import ru.yandex.market.logistics.mqm.service.PlanFactService
import ru.yandex.market.logistics.mqm.service.geobase.GeoBaseClientService
import ru.yandex.market.logistics.mqm.service.processor.settings.PlanFactProcessorSettingService
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

@ExtendWith(MockitoExtension::class)
class OrderNotifyPrevDdPlanFactProcessorTest: BaseOrderNotifyDdPlanFactProcessorTest() {
    override fun mockProcessor(
        settingService: PlanFactProcessorSettingService,
        clock: Clock,
        planFactService: PlanFactService,
        geoBaseClientService: GeoBaseClientService,
        properties: NotifyDeliveryDateProperties,
        changeOrderRequestService: ChangeOrderRequestService,
    ) = OrderNotifyPrevDdPlanFactProcessor(
        settingService,
        clock,
        planFactService,
        geoBaseClientService,
        properties,
        changeOrderRequestService,
    )

    override fun expectedStatusDatetime(): Instant {
        val timeZoneId = ZoneId.of(TIME_ZONE)
        val localTime = LocalTime.of(0, 0)
        val localDateTime = LocalDateTime.of(DELIVERY_DATE, localTime)
        return localDateTime
            .atZone(timeZoneId)
            .minusDays(1)
            .plus(DELIVERY_DEFAULT_TIMEOUT)
            .toInstant()
    }

    override fun getProducerName(): String = PRODUCER_NAME

    override fun getFixedTime(): Instant = FIXED_TIME

    override fun getPlannedShipmentDatetime(): Instant = DEFAULT_PLANNED_SHIPMENT_DATETIME

    override fun getDeliveryDate(): LocalDate = DELIVERY_DATE

    @Test
    @DisplayName("План-факт не создается, если доставка на следующий день от создания")
    fun doNotCreatePlanFactIfNextDayDelivery() {
        val orderCreatedDate = DEFAULT_PLANNED_SHIPMENT_DATETIME.minus(Duration.ofDays(1))
        val lomOrder = createLomOrder(orderCreatedDate = orderCreatedDate)

        val context = mockLomOrderEventContext(lomOrder = lomOrder)

        setupProcessor().lomOrderStatusChanged(context)

        verify(planFactService, never()).save(any())
    }

    companion object {
        private val FIXED_TIME = Instant.parse("2021-10-01T02:00:00.00Z")
        private val DEFAULT_PLANNED_SHIPMENT_DATETIME = Instant.parse("2021-10-02T10:00:00.00Z")
        private val DELIVERY_DATE = LocalDate.of(2021, 10, 2)
        private const val TIME_ZONE = "Asia/Yekaterinburg"
        private val DELIVERY_DEFAULT_TIMEOUT = Duration.ofHours(9)
        private const val PRODUCER_NAME = "OrderNotifyPrevDdPlanFactProcessor"
    }
}
