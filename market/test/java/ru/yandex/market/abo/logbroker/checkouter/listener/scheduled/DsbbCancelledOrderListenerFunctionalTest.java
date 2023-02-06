package ru.yandex.market.abo.logbroker.checkouter.listener.scheduled;

import java.util.List;

import one.util.streamex.StreamEx;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.core.hiding.rules.blue.BlueOfferHidingRuleRepo;
import ru.yandex.market.abo.logbroker.checkouter.ScheduledCpaEventListenersManager;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Context;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.abo.api.entity.offer.hidden.blue.BlueOfferHidingReason.CANCELLED_ORDER;
import static ru.yandex.market.abo.logbroker.checkouter.listener.scheduled.DropshipCancelledOrderListenerTest.ORDER_ID;
import static ru.yandex.market.abo.logbroker.checkouter.listener.scheduled.DropshipCancelledOrderListenerTest.PARTNER_ID;
import static ru.yandex.market.abo.logbroker.checkouter.listener.scheduled.DropshipCancelledOrderListenerTest.SHOP_SKU;

/**
 * @author komarovns
 */
public class DsbbCancelledOrderListenerFunctionalTest extends EmptyTest {
    @Autowired
    ScheduledCpaEventListenersManager scheduledListenersManager;
    @Autowired
    BlueOfferHidingRuleRepo hidingRuleRepo;

    @Test
    void listenerTest() throws Exception {
        var item = new OrderItem();
        item.setShopSku(SHOP_SKU);
        item.setSupplierId(PARTNER_ID);

        var delivery = new Delivery();
        delivery.setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);

        var order = new Order();
        order.setItems(List.of(item));
        order.setDelivery(delivery);
        order.setId(ORDER_ID);
        order.setShopId(PARTNER_ID);
        order.setFulfilment(false);
        order.setStatus(OrderStatus.CANCELLED);
        order.setSubstatus(OrderSubstatus.PROCESSING_EXPIRED);
        order.setContext(Context.MARKET);
        order.setRgb(Color.BLUE);

        var event = new OrderHistoryEvent();
        event.setOrderAfter(order);
        event.setType(HistoryEventType.ORDER_STATUS_UPDATED);

        scheduledListenersManager.schedule(List.of(event));
        flushAndClear();
        scheduledListenersManager.runEvents(100);
        flushAndClear();

        var ruleSaved = StreamEx.of(hidingRuleRepo.findAllByDeletedFalse()).anyMatch(rule ->
                PARTNER_ID == rule.getSupplierId()
                        && SHOP_SKU.equals(rule.getShopSku())
                        && CANCELLED_ORDER == rule.getHidingReason()
        );
        assertTrue(ruleSaved);
    }
}
