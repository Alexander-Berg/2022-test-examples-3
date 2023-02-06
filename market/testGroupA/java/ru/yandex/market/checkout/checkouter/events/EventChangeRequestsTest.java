package ru.yandex.market.checkout.checkouter.events;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.CheckouterOrderHistoryEventsApi;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.OrderFilter;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvents;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.OptionalOrderPart;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderService;
import ru.yandex.market.checkout.checkouter.order.changerequest.AbstractChangeRequestPayload;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequest;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestStatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.TestChangeRequestPayload;
import ru.yandex.market.checkout.checkouter.storage.OrderHistoryDao;
import ru.yandex.market.checkout.checkouter.storage.changerequest.ChangeRequestDao;
import ru.yandex.market.checkout.helpers.OrderControllerTestHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.event.HistoryEventType.ORDER_CHANGE_REQUEST_CREATED;

public class EventChangeRequestsTest extends AbstractWebTestBase {

    private static final long SHOP_ID = 4545L;
    @Autowired
    private OrderControllerTestHelper orderControllerTestHelper;
    @Autowired
    private ChangeRequestDao changeRequestDao;
    @Autowired
    private OrderService orderService;
    @Autowired
    private OrderHistoryDao orderHistoryDao;

    @Test
    public void shouldReturnListOfChangeRequests() {
        CheckouterOrderHistoryEventsApi checkouterOrderHistoryEventsApi = client.orderHistoryEvents();

        Order order = orderControllerTestHelper.createBlueOrderWithPartnerDelivery(SHOP_ID);

        TestChangeRequestPayload expectedPayload = new TestChangeRequestPayload();

        transactionTemplate.execute(ts -> {
            long historyId = orderHistoryDao.insertOrderHistory(order.getId(), ORDER_CHANGE_REQUEST_CREATED,
                    ClientInfo.SYSTEM);
            changeRequestDao.save(order, expectedPayload, ChangeRequestStatus.NEW, ClientInfo.SYSTEM, historyId);
            return null;
        });

        OrderHistoryEvents events = checkouterOrderHistoryEventsApi.getOrderHistoryEvents(
                0,
                10,
                null,
                false,
                null,
                OrderFilter.builder().setRgb(Color.BLUE).build(),
                EnumSet.of(OptionalOrderPart.CHANGE_REQUEST));

        assertFalse(events.getContent().isEmpty());

        Optional<OrderHistoryEvent> optionalOrderHistoryEvent = events.getContent()
                .stream()
                .filter(e -> e.getType() == HistoryEventType.ORDER_CHANGE_REQUEST_CREATED)
                .findFirst();

        assertTrue(optionalOrderHistoryEvent.isPresent());

        OrderHistoryEvent changeRequestHistoryEvent = optionalOrderHistoryEvent.get();

        assertListOfChangeRequestsReturned(changeRequestHistoryEvent, expectedPayload);

        OrderHistoryEvent event = checkouterOrderHistoryEventsApi.getOrderHistoryEvent(
                changeRequestHistoryEvent.getId(), EnumSet.of(OptionalOrderPart.CHANGE_REQUEST));
        assertNotNull(event);

        assertListOfChangeRequestsReturned(event, expectedPayload);
    }

    private void assertListOfChangeRequestsReturned(OrderHistoryEvent changeRequestHistoryEvent,
                                                    AbstractChangeRequestPayload expectedPayload) {
        List<ChangeRequest> changeRequests = changeRequestHistoryEvent.getOrderAfter().getChangeRequests();

        assertNotNull(changeRequests);

        assertThat(changeRequests,
                containsInAnyOrder(
                        hasProperty("payload", equalTo(expectedPayload))));
    }
}
