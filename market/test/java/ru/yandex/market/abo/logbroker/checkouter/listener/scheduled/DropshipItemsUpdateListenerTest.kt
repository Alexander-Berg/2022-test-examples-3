package ru.yandex.market.abo.logbroker.checkouter.listener.scheduled

import java.time.LocalDateTime
import java.time.Month.MAY
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.yandex.market.abo.core.exception.ExceptionalShopsService
import ru.yandex.market.abo.core.hiding.rules.blue.BlueOfferHidingRule
import ru.yandex.market.abo.cpa.order.service.DropshipItemsUpdateService
import ru.yandex.market.abo.util.db.batch.PgBatchUpdater
import ru.yandex.market.abo.util.db.toggle.ToggleService
import ru.yandex.market.abo.util.kotlin.toDate
import ru.yandex.market.checkout.checkouter.delivery.Delivery
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType.YANDEX_MARKET
import ru.yandex.market.checkout.checkouter.event.HistoryEventReason.DELIVERY_SERVICE_PROBLEM
import ru.yandex.market.checkout.checkouter.event.HistoryEventReason.ITEMS_NOT_SUPPLIED
import ru.yandex.market.checkout.checkouter.event.HistoryEventType.ITEMS_UPDATED
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent
import ru.yandex.market.checkout.checkouter.order.Color.BLUE
import ru.yandex.market.checkout.checkouter.order.Context.MARKET
import ru.yandex.market.checkout.checkouter.order.Order
import ru.yandex.market.checkout.checkouter.order.OrderItem

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 17.05.2021
 */
class DropshipItemsUpdateListenerTest {
    private val dropshipItemsUpdateService: DropshipItemsUpdateService = mock()
    private val bySskuHidingBatchUpdater: PgBatchUpdater<BlueOfferHidingRule> = mock()
    private val exceptionalShopsService: ExceptionalShopsService = mock()
    private val toggleService: ToggleService = mock()

    private val dropshipItemsUpdateListener: DropshipItemsUpdateListener = DropshipItemsUpdateListener(
        dropshipItemsUpdateService, bySskuHidingBatchUpdater, exceptionalShopsService, toggleService
    )

    private val delivery: Delivery = mock {
        on { deliveryPartnerType } doReturn YANDEX_MARKET
    }

    private val orderItem: OrderItem = mockItem(ITEM_ID, ITEM_COUNT)
    private val anotherOrderItem: OrderItem = mockItem(ANOTHER_ITEM_ID, ANOTHER_ITEM_COUNT)

    private val orderBefore: Order = mock {
        on { id } doReturn ORDER_ID
        on { isFulfilment } doReturn false
        on { items } doReturn listOf(orderItem, anotherOrderItem)
        on { delivery } doReturn delivery
    }
    private val orderAfter: Order = mock {
        on { id } doReturn ORDER_ID
        on { isFake } doReturn false
        on { context } doReturn MARKET
        on { rgb } doReturn BLUE
        on { isFulfilment } doReturn false
        on { delivery } doReturn delivery
    }

    private val event: OrderHistoryEvent = mock {
        on { fromDate } doReturn EVENT_TIME
        on { type } doReturn ITEMS_UPDATED
        on { reason } doReturn ITEMS_NOT_SUPPLIED
        on { orderBefore } doReturn orderBefore
        on { orderAfter } doReturn orderAfter
    }

    @Test
    fun `before order not found`() {
        whenever(event.orderBefore).thenReturn(null)
        assertFalse(dropshipItemsUpdateListener.isEventValid(event))
    }

    @Test
    fun `wrong event reason`() {
        whenever(event.reason).thenReturn(DELIVERY_SERVICE_PROBLEM)
        assertFalse(dropshipItemsUpdateListener.isEventValid(event))
    }

    @Test
    fun `order is not dropship`() {
        whenever(orderBefore.isFulfilment).thenReturn(true)
        assertFalse(dropshipItemsUpdateListener.isEventValid(event))
    }

    @Test
    fun `old items not changed`() {
        whenever(orderAfter.items).thenReturn(listOf(orderItem, anotherOrderItem))
        assertNull(dropshipItemsUpdateListener.prepareListenerTaskBody(event))
    }

    @Test
    fun `item removed from order`() {
        whenever(orderAfter.items).thenReturn(listOf(orderItem))

        val taskBody = dropshipItemsUpdateListener.prepareListenerTaskBody(event)

        assertNotNull(taskBody)
        assertEquals(ORDER_ID, taskBody?.orderId)
    }

    @Test
    fun `item count decreased`() {
        val decreasedCountItem = mockItem(ANOTHER_ITEM_ID, ANOTHER_ITEM_COUNT - 2)
        whenever(orderAfter.items).thenReturn(listOf(orderItem, decreasedCountItem))

        val taskBody = dropshipItemsUpdateListener.prepareListenerTaskBody(event)

        assertNotNull(taskBody)
        assertEquals(ORDER_ID, taskBody?.orderId)
    }

    private fun mockItem(id: Long, count: Int): OrderItem = mock {
        on { getId() } doReturn id
        on { getCount() } doReturn count
        on { shopSku } doReturn "sku"
    }

    companion object {
        private val EVENT_TIME = LocalDateTime.of(2021, MAY, 27, 10, 5).toDate()

        private const val ORDER_ID = 2131234L

        private const val ITEM_ID = 32152345L
        private const val ANOTHER_ITEM_ID = 32158945L

        private const val ITEM_COUNT = 5
        private const val ANOTHER_ITEM_COUNT = 7
    }
}
