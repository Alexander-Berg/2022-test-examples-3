package ru.yandex.market.checkout.checkouter.order.getOrder;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.allure.Epics;
import ru.yandex.market.checkout.allure.Stories;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.OptionalOrderPart;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderSearchRequest;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequest;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestStatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.TestChangeRequestPayload;
import ru.yandex.market.checkout.checkouter.storage.OrderHistoryDao;
import ru.yandex.market.checkout.checkouter.storage.changerequest.ChangeRequestDao;
import ru.yandex.market.checkout.helpers.OrderControllerTestHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static ru.yandex.market.checkout.checkouter.event.HistoryEventType.ORDER_CHANGE_REQUEST_CREATED;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class OrderControllerGetPartialsTest extends AbstractWebTestBase {

    private static final long SHOP_ID = 4545L;

    @Autowired
    private OrderControllerTestHelper orderControllerTestHelper;
    @Autowired
    private ChangeRequestDao changeRequestDao;
    @Autowired
    private OrderHistoryDao orderHistoryDao;

    private Order order;
    private TestChangeRequestPayload expectedPayload;

    @BeforeAll
    public void setUp() {
        order = orderControllerTestHelper.createOrderWithPartnerDelivery(SHOP_ID);

        expectedPayload = new TestChangeRequestPayload();

        transactionTemplate.execute(ts -> {
            changeRequestDao.save(order, expectedPayload, ChangeRequestStatus.NEW, ClientInfo.SYSTEM,
                    orderHistoryDao.insertOrderHistory(order.getId(), ORDER_CHANGE_REQUEST_CREATED, ClientInfo.SYSTEM));
            return null;
        });
    }

    @Override
    @AfterEach
    public void tearDownBase() {
    }

    @AfterAll
    public void cleanUp() {
        tearDownBase();
    }

    @Epic(Epics.GET_ORDER)
    @Story(Stories.ORDERS_ORDER_ID)
    @DisplayName("Возвращается список заявок на изменение заказа")
    @Test
    public void testShowChangeRequests() throws Exception {
        Order orderWithChangeRequests = client.getOrder(order.getId(), ClientRole.SYSTEM, 0L,
                EnumSet.of(OptionalOrderPart.CHANGE_REQUEST));

        List<ChangeRequest> changeRequests = orderWithChangeRequests.getChangeRequests();

        assertNotNull(changeRequests);
        assertThat(changeRequests, hasSize(1));
        ChangeRequest changeRequest = changeRequests.iterator().next();

        assertEquals(expectedPayload, changeRequest.getPayload());
    }

    @Epic(Epics.GET_ORDER)
    @Story(Stories.ORDERS)
    @DisplayName("Возвращается список заявок на изменение заказа при поиске через OrderSearchRequest")
    @Test
    public void getOrdersByOrderSearchRequest() throws Exception {
        OrderSearchRequest request = OrderSearchRequest.builder()
                .withRgbs(Color.BLUE)
                .build();
        request.orderIds = Collections.singletonList(order.getId());
        request.setPartials(new OptionalOrderPart[]{OptionalOrderPart.CHANGE_REQUEST});

        Collection<Order> orders = client.getOrders(request, ClientRole.SYSTEM, 0L).getItems();

        Order orderWithChangeRequests = orders.iterator().next();

        List<ChangeRequest> changeRequests = orderWithChangeRequests.getChangeRequests();

        assertNotNull(changeRequests);

        assertThat(changeRequests, hasSize(1));
        ChangeRequest changeRequest = changeRequests.iterator().next();

        assertEquals(expectedPayload, changeRequest.getPayload());
    }
}

