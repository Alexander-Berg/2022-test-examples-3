package ru.yandex.market.logistics.mqm.service.processor.planfact.order

import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import ru.yandex.market.logistics.mqm.configuration.properties.NotifyDeliveryDateProperties
import ru.yandex.market.logistics.mqm.service.ChangeOrderRequestService
import ru.yandex.market.logistics.mqm.service.geobase.GeoBaseClientService
import ru.yandex.market.logistics.mqm.service.PlanFactService
import ru.yandex.market.logistics.mqm.service.processor.settings.PlanFactProcessorSettingService
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.Clock

@ExtendWith(MockitoExtension::class)
class OrderNotifyInDdPlanFactProcessorTest: BaseOrderNotifyDdPlanFactProcessorTest() {

    override fun mockProcessor(
        settingService: PlanFactProcessorSettingService,
        clock: Clock,
        planFactService: PlanFactService,
        geoBaseClientService: GeoBaseClientService,
        properties: NotifyDeliveryDateProperties,
        changeOrderRequestService: ChangeOrderRequestService,
    ) = OrderNotifyInDdPlanFactProcessor(
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
            .plus(DELIVERY_DEFAULT_TIMEOUT)
            .toInstant()
    }

    override fun getProducerName(): String = PRODUCER_NAME

    override fun getFixedTime(): Instant = FIXED_TIME

    override fun getPlannedShipmentDatetime(): Instant = DEFAULT_PLANNED_SHIPMENT_DATETIME

    override fun getDeliveryDate(): LocalDate = DELIVERY_DATE

    companion object {
        private val FIXED_TIME = Instant.parse("2021-10-01T02:00:00.00Z")
        private val DEFAULT_PLANNED_SHIPMENT_DATETIME = Instant.parse("2021-10-01T10:00:00.00Z")
        private val DELIVERY_DATE = LocalDate.of(2021, 10, 1)
        private const val TIME_ZONE = "Asia/Yekaterinburg"
        private val DELIVERY_DEFAULT_TIMEOUT = Duration.ofHours(9)
        private const val PRODUCER_NAME = "OrderNotifyInDdPlanFactProcessor"
    }
}
