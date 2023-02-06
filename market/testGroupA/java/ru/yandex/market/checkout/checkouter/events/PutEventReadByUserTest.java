package ru.yandex.market.checkout.checkouter.events;

import java.util.List;

import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.allure.Epics;
import ru.yandex.market.checkout.allure.Stories;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.event.OrderEventPublishService;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderNotFoundException;
import ru.yandex.market.checkout.checkouter.tasks.Partition;
import ru.yandex.market.checkout.common.rest.ErrorCodeException;
import ru.yandex.market.checkout.helpers.OrderHistoryEventsTestHelper;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.util.services.OrderServiceHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author : poluektov
 * date: 08.08.17.
 */

//https://testpalm.yandex-team.ru/checkouter/testcases?testcase=78
public class PutEventReadByUserTest extends AbstractWebTestBase {

    @Autowired
    private OrderServiceHelper orderServiceHelper;
    @Autowired
    private OrderHistoryEventsTestHelper eventsHelper;
    @Autowired
    private OrderEventPublishService orderEventPublishService;

    private long orderId;
    private List<OrderHistoryEvent> events;
    private Order order;


    @BeforeEach
    public void createOrder() {
        order = orderServiceHelper.createOrder();
        orderId = order.getId();
        events = eventsHelper.getAllEvents(orderId);
    }

    @Epic(Epics.GET_ORDER)
    @Story(Stories.EVENTS_READ_BY_USER)
    @DisplayName("Проверяем, что для прочитанного пользователем события проставляется флаг readByUser")
    @Test
    public void setReadByUserTrue() throws Exception {
        long eventId = events.get(0).getId().longValue();
        orderUpdateService.setEventReadByUser(orderId, eventId, ClientInfo.SYSTEM, true);
        OrderHistoryEvent event = eventsHelper.getOrderEventById(orderId, eventId);
        assertThat("wrong ReadByUser value", event.getReadByUser(), equalTo(true));
    }

    @Epic(Epics.GET_ORDER)
    @Story(Stories.ORDERS_ORDER_ID)
    @DisplayName("Проверяем, работу поля unreadEventsCount")
    @Test
    public void unreadImportantEventsTest() throws Exception {
        checkUnreadEventsCount(0);

        Delivery newDelivery = DeliveryProvider.getGlobalDelivery();
        newDelivery.setShopAddress(newDelivery.getAddress());
        orderUpdateService.updateOrderDelivery(orderId, newDelivery,
                new ClientInfo(ClientRole.SHOP, OrderServiceHelper.DEFAULT_SHOP_ID));
        checkUnreadEventsCount(1);

        events = eventsHelper.getAllEvents(orderId);
        Long lastEventId = events.get(0).getId();
        orderUpdateService.setEventReadByUser(orderId, lastEventId, ClientInfo.SYSTEM, true);
        checkUnreadEventsCount(0);
    }

    @Test
    public void systemRoleFilterTest() throws Exception {
        checkUnreadEventsCount(0);

        orderUpdateService.updateOrderDelivery(orderId, DeliveryProvider.getGlobalDelivery(), ClientInfo.SYSTEM);
        checkUnreadEventsCount(0);
    }

    @Test
    public void wrongEventId() {
        Assertions.assertThrows(ErrorCodeException.class, () -> {
            orderUpdateService.setEventReadByUser(orderId, 12345677L, ClientInfo.SYSTEM, true);
        });
    }

    @Test
    public void wrongUid() {
        Assertions.assertThrows(OrderNotFoundException.class, () -> {
            orderUpdateService.setEventReadByUser(orderId, events.get(0).getId(), new ClientInfo(ClientRole.USER,
                    1111111L), true);
        });
    }

    private void checkUnreadEventsCount(Integer expected) {
        orderEventPublishService.publishEventsBatch(Integer.MAX_VALUE, Partition.of(0, 10));
        orderEventPublishService.publishEventsBatch(Integer.MAX_VALUE, Partition.NULL);
        order = orderService.getOrder(orderId);
        assertThat("", order.getBuyer().getUnreadImportantEvents().intValue(), equalTo(expected));
    }
}
