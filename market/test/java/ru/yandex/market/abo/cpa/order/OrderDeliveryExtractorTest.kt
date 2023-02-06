package ru.yandex.market.abo.cpa.order

import java.time.LocalDateTime
import java.util.Date
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ru.yandex.common.util.date.DateUtil
import ru.yandex.market.abo.util.kotlin.toDate
import ru.yandex.market.checkout.checkouter.delivery.Delivery
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates
import ru.yandex.market.checkout.checkouter.delivery.DeliveryFeature
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent
import ru.yandex.market.checkout.checkouter.order.Order

/**
 * @author komarovns
 */
class OrderDeliveryExtractorTest {
    private val orderDeliveryExtractor = OrderDeliveryExtractor()

    private val now = Date()
    private val hourAgo = DateUtil.addDay(now, -1)

    @Test
    fun `take only changed`() {
        val events = listOf(
            event(0, 0, hourAgo, now),
            event(1, 1, hourAgo, hourAgo)
        )
        assertEquals(listOf(now), orderDeliveryExtractor.buildDeliveries(events).map { it.byOrder })
    }

    @Test
    fun `take only last event`() {
        val events = listOf(
            event(0, 0, null, hourAgo),
            event(1, 0, null, now)
        )
        assertEquals(listOf(now), orderDeliveryExtractor.buildDeliveries(events).map { it.byOrder })
    }

    @Test
    fun `set all fields`() {
        val orderId = 1L
        val byOrder = Date()
        val byShipment = LocalDateTime.now()
        val partnerType = DeliveryPartnerType.YANDEX_MARKET
        val features = setOf(DeliveryFeature.EXPRESS_DELIVERY)
        val order = order(
            orderId = orderId,
            byOrder = byOrder,
            byShipment = byShipment,
            partnerType = partnerType,
            deliveryFeatures = features
        )
        val events = listOf(event(0, order))
        val dbDelivery = orderDeliveryExtractor.buildDeliveries(events).first()

        assertEquals(orderId, dbDelivery.orderId)
        assertEquals(byOrder, dbDelivery.byOrder)
        assertEquals(byShipment.toDate(), dbDelivery.byShipment)
        assertEquals(partnerType, dbDelivery.deliveryPartnerType)
        assertEquals(true, dbDelivery.express)
    }
}

private fun event(eventId: Long, after: Order, before: Order? = null) =
    OrderHistoryEvent().apply {
        id = eventId
        orderBefore = before
        orderAfter = after
    }

private fun event(eventId: Long, orderId: Long, byOrderBefore: Date?, byOrderAfter: Date) =
    event(eventId, order(orderId, byOrderAfter), order(orderId, byOrderBefore))

private fun order(orderId: Long,
                  byOrder: Date?,
                  byShipment: LocalDateTime? = null,
                  partnerType: DeliveryPartnerType? = null,
                  deliveryFeatures: Set<DeliveryFeature>? = null
) = Order().apply {
    id = orderId
    delivery = Delivery().apply {
        deliveryDates = DeliveryDates(null, byOrder)
        parcels = listOf(Parcel().apply {
            shipmentDateTimeBySupplier = byShipment
        })
        deliveryPartnerType = partnerType
        features = deliveryFeatures
    }
}
