package ru.yandex.market.abo.cpa.order;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.market.abo.cpa.order.model.CpaOrderStat;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;

/**
 * @author imelnikov
 * @since 25.03.2021
 */
public class OrderStatCreatorTest {

    @ParameterizedTest
    @EnumSource(OrderStatus.class)
    public void parseEvent(OrderStatus status) {
        if (OrderStatus.UNKNOWN == status) {
            return;
        }

        OrderHistoryEvent event = new OrderHistoryEvent();
        Order orderAfter = new Order();
        orderAfter.setStatus(status);
        event.setOrderAfter(orderAfter);

        event.setAuthor(new ClientInfo(ClientRole.SHOP, 1L));
        new CpaOrderStat().addStatusHistory(event);
    }
}
