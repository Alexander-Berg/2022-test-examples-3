package ru.yandex.market.checkout.checkouter.order.changerequest;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.PagedEvents;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderNotFoundException;
import ru.yandex.market.checkout.checkouter.storage.OrderHistoryDao;
import ru.yandex.market.checkout.checkouter.storage.OrderReadingDao;
import ru.yandex.market.checkout.checkouter.storage.changerequest.ChangeRequestDao;
import ru.yandex.market.checkout.helpers.EventsGetHelper;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.checkout.checkouter.event.HistoryEventType.ORDER_CHANGE_REQUEST_CREATED;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PROCESSING;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.OrderProvider.SHOP_ID_WITH_SORTING_CENTER;

public class ChangeRequestDaoTest extends AbstractWebTestBase {

    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;
    @Autowired
    private ChangeRequestDao changeRequestDao;
    @Autowired
    private OrderReadingDao orderReadingDao;
    @Autowired
    private OrderHistoryDao orderHistoryDao;
    @Autowired
    private EventsGetHelper eventsGetHelper;

    private Order order;
    private TestChangeRequestPayload testChangeRequestPayload;

    @BeforeEach
    public void setUp() throws Exception {
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withColor(Color.BLUE)
                .buildParameters();
        order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, PROCESSING);

        testChangeRequestPayload = new TestChangeRequestPayload();
        testChangeRequestPayload.setName("TEST");
    }

    @Test
    @SuppressWarnings("checkstyle:HiddenField")
    public void readWriteSerializationTest() {
        AnotherTestChangeRequestPayload anotherTestChangeRequestPayload = new AnotherTestChangeRequestPayload();
        anotherTestChangeRequestPayload.setAddress("Москва, Кремль");
        anotherTestChangeRequestPayload.setAnotherField(123L);


        transactionTemplate.execute(tc -> {
            Order order = orderReadingDao.getOrder(this.order.getId(), ClientInfo.SYSTEM)
                    .orElseThrow(() -> new OrderNotFoundException(this.order.getId()));

            changeRequestDao.save(order, testChangeRequestPayload, ChangeRequestStatus.NEW, ClientInfo.SYSTEM,
                    orderHistoryDao.insertOrderHistory(order.getId(), ORDER_CHANGE_REQUEST_CREATED, ClientInfo.SYSTEM));
            changeRequestDao.save(order, anotherTestChangeRequestPayload, ChangeRequestStatus.APPLIED,
                    ClientInfo.SYSTEM,
                    orderHistoryDao.insertOrderHistory(order.getId(), ORDER_CHANGE_REQUEST_CREATED, ClientInfo.SYSTEM));
            return null;
        });

        List<ChangeRequest> changeRequests = changeRequestDao.findByOrder(order);

        assertThat(changeRequests, hasSize(2));

        assertThat(changeRequests,
                containsInAnyOrder(
                        hasProperty("payload", equalTo(testChangeRequestPayload)),
                        hasProperty("payload", equalTo(anotherTestChangeRequestPayload))));

        List<Long> crIds = changeRequestDao.findByStatuses(Collections.singletonList(ChangeRequestStatus.APPLIED));
        Collection<ChangeRequest> applied = changeRequestDao.getChangeRequestsById(crIds);

        assertThat(applied, Matchers.hasSize(1));
        assertThat(applied,
                Matchers.contains(
                        hasProperty("status", equalTo(ChangeRequestStatus.APPLIED))));
    }

    @Test
    @SuppressWarnings("checkstyle:HiddenField")
    public void eventTest() throws Exception {
        transactionTemplate.execute(tc -> {
            Order order = orderReadingDao.getOrder(this.order.getId(), ClientInfo.SYSTEM)
                    .orElseThrow(() -> new OrderNotFoundException(this.order.getId()));

            changeRequestDao.save(order, testChangeRequestPayload, ChangeRequestStatus.APPLIED, ClientInfo.SYSTEM,
                    orderHistoryDao.insertOrderHistory(order.getId(), ORDER_CHANGE_REQUEST_CREATED, ClientInfo.SYSTEM));
            return null;
        });

        PagedEvents events = eventsGetHelper.getOrderHistoryEvents(order.getId());

        assertEquals(HistoryEventType.ORDER_CHANGE_REQUEST_CREATED,
                events.getItems().stream().findFirst().get().getType());
    }
}
