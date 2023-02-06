package ru.yandex.market.abo.logbroker.checkouter.listener.scheduled;

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.yandex.market.abo.api.entity.offer.hidden.blue.BlueOfferHidingReason
import ru.yandex.market.abo.core.exception.ExceptionalShopReason
import ru.yandex.market.abo.core.exception.ExceptionalShopsService
import ru.yandex.market.abo.core.hiding.rules.blue.BlueOfferHidingRule
import ru.yandex.market.abo.util.db.batch.PgBatchUpdater
import ru.yandex.market.checkout.checkouter.delivery.Delivery
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType
import ru.yandex.market.checkout.checkouter.event.HistoryEventType
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent
import ru.yandex.market.checkout.checkouter.order.Color
import ru.yandex.market.checkout.checkouter.order.Context
import ru.yandex.market.checkout.checkouter.order.Order
import ru.yandex.market.checkout.checkouter.order.OrderItem
import ru.yandex.market.checkout.checkouter.order.OrderStatus
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 02.11.2020
 */
class DropshipCancelledOrderListenerTest {

    private val bySskuHidingBatchUpdater: PgBatchUpdater<BlueOfferHidingRule> = mock()
    private val exceptionalShopsService: ExceptionalShopsService = mock()
    private val event: OrderHistoryEvent = mock()
    private val order: Order = mock()

    private val dsbbCancelledOrderListener = DropshipCancelledOrderListener(
        bySskuHidingBatchUpdater, exceptionalShopsService
    )

    @BeforeEach
    fun init() {
        whenever(event.orderAfter).thenReturn(order)
        whenever(event.type).thenReturn(HistoryEventType.ORDER_STATUS_UPDATED)

        val item: OrderItem = mock()
        whenever(item.supplierId).thenReturn(PARTNER_ID)
        whenever(item.shopSku).thenReturn(SHOP_SKU)

        whenever(order.id).thenReturn(ORDER_ID)
        whenever(order.shopId).thenReturn(PARTNER_ID)
        whenever(order.status).thenReturn(OrderStatus.CANCELLED)
        whenever(order.substatus).thenReturn(OrderSubstatus.PROCESSING_EXPIRED)
        whenever(order.items).thenReturn(listOf(item))
        whenever(order.context).thenReturn(Context.MARKET)
        whenever(order.rgb).thenReturn(Color.BLUE)
        whenever(order.isFulfilment).thenReturn(false)
        val delivery: Delivery = mock()
        whenever(delivery.deliveryPartnerType).thenReturn(DeliveryPartnerType.YANDEX_MARKET)
        whenever(order.delivery).thenReturn(delivery)

        whenever(exceptionalShopsService.loadShops(ExceptionalShopReason.IGNORE_CANCELLED_ORDER)).thenReturn(emptySet())
    }

    @Test
    fun validEvent() {
        assertTrue(dsbbCancelledOrderListener.isEventValid(event))
        val taskBody = dsbbCancelledOrderListener.prepareListenerTaskBody(event)
        dsbbCancelledOrderListener.processListenerTask(taskBody)

        val hidingRulesCaptor = argumentCaptor<List<BlueOfferHidingRule>>()
        verify(bySskuHidingBatchUpdater).insertWithoutUpdate(hidingRulesCaptor.capture())

        val rules = hidingRulesCaptor.firstValue
        assertEquals(1, rules.size)
        val rule = rules.first()
        assertEquals(ORDER_ID.toString(), rule.comment)
        assertEquals(PARTNER_ID, rule.supplierId)
        assertEquals(SHOP_SKU, rule.shopSku)
        assertEquals(BlueOfferHidingReason.CANCELLED_ORDER, rule.hidingReason)
        assertFalse(rule.deleted)
    }

    @Test
    fun orderIsNotDropshipTest() {
        whenever(order.isFulfilment).thenReturn(true)
        assertFalse(dsbbCancelledOrderListener.isEventValid(event))
    }

    @Test
    fun orderNotCancelledTest() {
        whenever(order.status).thenReturn(OrderStatus.PROCESSING)
        assertFalse(dsbbCancelledOrderListener.isEventValid(event))
    }

    @Test
    fun orderCancelledNotBySupplierErrorTest() {
        whenever(order.substatus).thenReturn(OrderSubstatus.USER_CHANGED_MIND)
        assertFalse(dsbbCancelledOrderListener.isEventValid(event))
    }

    @Test
    fun shopHasExceptionTest() {
        whenever(exceptionalShopsService.loadShops(ExceptionalShopReason.IGNORE_CANCELLED_ORDER))
            .thenReturn(setOf(PARTNER_ID))
        assertFalse(dsbbCancelledOrderListener.isEventValid(event))
    }

    /**
     * Если вдруг кто-то решил поменять формат эвента,
     * то проверяем, что существующие в базе джейсонины парсятся
     */
    @Test
    fun deserializeTest() {
        val json = "{\"rules\":[{\"orderId\":1,\"supplierId\":2,\"shopSku\":\"3\"}]}"
        val taskBody = DropshipCancelledOrderListener.TaskBody(listOf(
            DropshipCancelledOrderListener.HidingRule(1L, 2L, "3")
        ))
        assertEquals(taskBody, ObjectMapper().readValue(json, DropshipCancelledOrderListener.TaskBody::class.java))
    }

    companion object {
        const val ORDER_ID = 1242513L
        const val PARTNER_ID = 213L
        const val SHOP_SKU = "test-ssku"
    }
}
