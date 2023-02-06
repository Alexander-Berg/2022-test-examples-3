package ru.yandex.market.checkout.checkouter.order.status;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.CheckouterClient;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderStatusUpdateResult;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;

import static java.util.stream.Collectors.toUnmodifiableList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.DELIVERY;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PROCESSING;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.RESERVED;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.DELIVERY_SERVICE_RECEIVED;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.STARTED;

public class BulkStatusChangeTest extends AbstractWebTestBase {

    @Autowired
    protected OrderPayHelper paymentHelper;
    @Autowired
    private CheckouterClient client;
    private List<Long> orderIds;

    @BeforeEach
    public void setup() {
        List<Order> orders = List.of(createOrder(), createOrder(), createOrder());
        orderIds = orders.stream().map(Order::getId).collect(toUnmodifiableList());
        orders.forEach(order -> {
            assertThat(order.getStatus(), is(PROCESSING));
            assertThat(order.getSubstatus(), is(STARTED));
        });
    }

    @Test
    public void shouldBulkUpdateOrderStatuses() {
        //given:
        var newStatus = DELIVERY;

        //when:
        var updateResults = updateOrdersStatus(orderIds, newStatus, null);

        //then:
        checkSuccessfulUpdateResultsAndOrder(updateResults, orderIds, newStatus, DELIVERY_SERVICE_RECEIVED);
    }

    @Test
    public void unableToChangeStatusToDeliveryTest() {

        OrderItem digitalItem = OrderItemProvider.buildOrderItemDigital("1");
        Parameters digitalParameters = WhiteParametersProvider.digitalOrderPrameters();
        digitalParameters.getOrder().setItems(Collections.singleton(digitalItem));
        Order order = orderCreateHelper.createOrder(digitalParameters);

        paymentHelper.payForOrder(order);
        order = orderService.getOrder(order.getId());

        List<OrderStatusUpdateResult> statusUpdateResults = client.bulkUpdateOrderStatus(
                Collections.singletonList(order.getId()),
                ClientRole.SHOP,
                order.getShopId(),
                order.getShopId(),
                DELIVERY,
                null
        );

        assertEquals(1, statusUpdateResults.size());
        assertEquals(order.getStatus(), statusUpdateResults.get(0).getStatus());
        assertTrue(statusUpdateResults.get(0).getErrorDetails().contains("No permission to set status"));
    }

    @Test
    public void yaPlusAbleToChangeStatusToDeliveryTest() {

        OrderItem digitalItem = OrderItemProvider.buildOrderItemDigital("1");
        Parameters digitalParameters = WhiteParametersProvider.digitalOrderPrameters();
        digitalParameters.getOrder().setItems(Collections.singleton(digitalItem));
        digitalParameters.getOrder().setShopId(957391L);
        Order order = orderCreateHelper.createOrder(digitalParameters);

        paymentHelper.payForOrder(order);

        List<OrderStatusUpdateResult> statusUpdateResults = client.bulkUpdateOrderStatus(
                Collections.singletonList(order.getId()),
                ClientRole.SHOP,
                order.getShopId(),
                order.getShopId(),
                DELIVERY,
                null
        );

        assertEquals(1, statusUpdateResults.size());
        assertEquals(DELIVERY, statusUpdateResults.get(0).getStatus());
    }

    @Test
    public void yaPlusAbleToChangeStatusToDeliveryMultiClientIdTest() {
        OrderItem digitalItem = OrderItemProvider.buildOrderItemDigital("1");
        Parameters digitalParameters = WhiteParametersProvider.digitalOrderPrameters();
        digitalParameters.getOrder().setItems(Collections.singleton(digitalItem));
        digitalParameters.getOrder().setShopId(957391L);
        Order order = orderCreateHelper.createOrder(digitalParameters);

        paymentHelper.payForOrder(order);
        List<OrderStatusUpdateResult> statusUpdateResults = client.bulkUpdateOrderStatus(
                Collections.singletonList(order.getId()),
                RequestClientInfo.builder(ClientRole.SHOP)
                        .withClientIds(Set.of(1L, 957391L, 2L))
                        .build(),
                DELIVERY,
                null
        );

        assertEquals(1, statusUpdateResults.size());
        assertEquals(DELIVERY, statusUpdateResults.get(0).getStatus());
    }

    @Test
    public void shouldUpdateStatusesIndependently() {
        //given:
        var orderToFail = createOrder();
        updateOrdersStatus(List.of(orderToFail.getId()), DELIVERY, null);
        var ids = new ArrayList<>(orderIds);
        ids.add(orderToFail.getId());

        //when:
        var updateResults = updateOrdersStatus(ids, DELIVERY, null);

        //then:
        assertThat(updateResults, hasSize(ids.size()));
        var firstResult = updateResults.get(0);
        var lastResult = updateResults.get(ids.size() - 1);
        assertTrue(firstResult.isUpdated());
        assertFalse(lastResult.isUpdated());
    }

    @Test
    public void shouldReturnCurrentOrderStatusWhenUpdateFailed() {
        //given:
        var orderToFail = createOrder();

        //when:
        var updateResults = updateOrdersStatus(List.of(orderToFail.getId()), RESERVED, null);

        //then:
        assertThat(updateResults, hasSize(1));
        var result = updateResults.get(0);
        assertFalse(result.isUpdated());
        assertThat(result.getStatus(), is(PROCESSING));
        assertThat(result.getSubstatus(), is(STARTED));
        assertThat(result.getErrorDetails(),
                is(endsWith("status PROCESSING and substatus STARTED " +
                        "is not allowed for status RESERVED and substatus null")));
        checkOrderStatus(orderToFail.getId(), PROCESSING, STARTED);
    }

    @Test
    public void shouldBulkUpdateOrderSubstatuses() {
        //given:
        var newSubstatus = OrderSubstatus.PACKAGING;

        //when:
        var updateResults = updateOrdersStatus(orderIds, PROCESSING, newSubstatus);

        //then:
        checkSuccessfulUpdateResultsAndOrder(updateResults, orderIds, PROCESSING, newSubstatus);
    }

    @Test
    public void bulkUpdateShouldPreserveOrderOfIds() {
        //given:
        var newSubstatus = OrderSubstatus.PACKAGING;
        var ids = new ArrayList<>(this.orderIds);
        Collections.reverse(ids);

        //when:
        var updateResults = updateOrdersStatus(ids, PROCESSING, newSubstatus);

        //then:
        checkSuccessfulUpdateResultsAndOrder(updateResults, ids, PROCESSING, newSubstatus);
    }

    private void checkSuccessfulUpdateResultsAndOrder(List<OrderStatusUpdateResult> updateResults, List<Long> orderIds,
                                                      OrderStatus newStatus, OrderSubstatus newSubstatus) {
        int ordersCount = orderIds.size();
        assertThat(updateResults, hasSize(orderIds.size()));
        for (int i = 0; i < ordersCount; i++) {
            var result = updateResults.get(i);
            var orderId = orderIds.get(i);

            assertThat(result.getOrderId(), is(equalTo(orderId)));
            assertThat(result.getErrorDetails(), is(nullValue()));
            assertThat(result.getStatus(), is(newStatus));
            assertThat(result.getSubstatus(), is(newSubstatus));
            assertTrue(result.isUpdated());
            checkOrderStatus(orderId, newStatus, newSubstatus);
        }
    }

    private List<OrderStatusUpdateResult> updateOrdersStatus(List<Long> orderIds, OrderStatus status,
                                                             OrderSubstatus substatus) {
        return client.bulkUpdateOrderStatus(
                orderIds,
                ClientRole.SYSTEM,
                123L,
                null,
                status,
                substatus
        );
    }

    private Order createOrder() {
        return orderCreateHelper.createOrder(new Parameters());
    }

    private void checkOrderStatus(Long orderId, OrderStatus status, OrderSubstatus substatus) {
        Order order = client.getOrder(orderId, ClientRole.SYSTEM, null);
        assertEquals(status, order.getStatus());
        assertEquals(substatus, order.getSubstatus());
    }
}
