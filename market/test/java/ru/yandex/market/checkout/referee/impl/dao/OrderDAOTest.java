package ru.yandex.market.checkout.referee.impl.dao;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvents;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.entity.OrderInfo;
import ru.yandex.market.checkout.referee.EmptyTest;
import ru.yandex.market.checkout.referee.impl.CheckoutRefereeService;
import ru.yandex.market.checkout.referee.impl.RetriableCheckouterService;
import ru.yandex.market.checkout.referee.test.BaseTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author kukabara
 */
public class OrderDAOTest extends EmptyTest {

    @Autowired
    protected CheckoutRefereeService checkoutRefereeService;

    private static OrderInfo generateOrderInfo(Order order) {
        return RetriableCheckouterService.convert(order, new OrderHistoryEvents(Collections.emptyList()));
    }

    @Test
    public void testSaveGetOrder() {
        Order order = BaseTest.getOrder(getId(), getId(), getId());
        order.setRgb(Color.RED);
        OrderInfo info = generateOrderInfo(order);
        checkoutRefereeService.insertOrder(info);

        OrderInfo saved = checkoutRefereeService.getOrder(order.getId());
        assertNotNull(saved);
        assertEquals(order.getRgb(), saved.getRgb());
        assertEquals(order.getId(), saved.getOrderId());
        assertEquals(order.getShopId(), saved.getShopId());
        assertEquals(order.getUid(), saved.getUid());
        assertEquals(order.isGlobal(), saved.isGlobal());
    }
}
