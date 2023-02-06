package ru.yandex.market.checkout.checkouter.events;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.event.PagedEvents;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.helpers.EventsGetHelper;
import ru.yandex.market.checkout.providers.BlueParametersProvider;

public class EventReceiptTest extends AbstractWebTestBase {

    @Autowired
    private EventsGetHelper eventsGetHelper;

    @Test
    public void shouldNotReturnReceiptWithIdEqZero() throws Exception {
        Order order = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());

        PagedEvents orderHistoryEvents = eventsGetHelper.getOrderHistoryEvents(order.getId());

        Assertions.assertTrue(orderHistoryEvents.getItems().stream().allMatch(ohe -> ohe.getReceipt() == null),
                "no receipts");
    }
}
