package ru.yandex.market.checkout.checkouter.order.status;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.BulkOrderCancellationResponse;
import ru.yandex.market.checkout.checkouter.order.CancellationRequest;
import ru.yandex.market.checkout.checkouter.order.CompatibleCancellationRequest;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderStatusUpdateResult;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.StatusAndSubstatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.helpers.utils.Parameters;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author : poluektov
 * date: 21.01.2019.
 */
public class OrderStatusBulkUpdateTest extends AbstractWebTestBase {

    private Order order1;
    private Order order2;

    @BeforeEach
    public void createOrders() {
        Parameters parameters = new Parameters();
        parameters.setPaymentMethod(PaymentMethod.YANDEX);

        order1 = orderCreateHelper.createOrder(parameters);
        order2 = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order1, OrderStatus.PROCESSING);
        orderStatusHelper.proceedOrderToStatus(order2, OrderStatus.PROCESSING);
    }

    @Test
    public void testStatusUpdateBulk() {
        List<Long> orderIds = Arrays.asList(order1.getId(), 1241551L, order2.getId());

        List<OrderStatusUpdateResult> result = orderUpdateService.updateOrderStatusBulk(
                orderIds,
                StatusAndSubstatus.of(OrderStatus.CANCELLED, OrderSubstatus.BROKEN_ITEM),
                ClientInfo.SYSTEM, true);

        assertThat(result, hasSize(3));
        assertThat(result.get(0).isUpdated(), equalTo(true));
        assertThat(result.get(0).getErrorDetails(), nullValue());

        assertThat(result.get(1).isUpdated(), equalTo(false));
        assertThat(result.get(1).getErrorDetails(), notNullValue());

        assertThat(result.get(2).isUpdated(), equalTo(true));
        assertThat(result.get(2).getErrorDetails(), nullValue());
    }


    @Test
    public void bulkCreateCancellationRequests() {
        List<Long> orderIds = Arrays.asList(order1.getId(), 1241551L, order2.getId());

        CompatibleCancellationRequest cancellationRequest =
                new CompatibleCancellationRequest(OrderSubstatus.USER_FRAUD.name(), "notes");

        boolean isSuccess = client.bulkCreateCancellationRequests(
                ClientRole.CALL_CENTER_OPERATOR,
                123L,
                cancellationRequest,
                orderIds);

        assertTrue(isSuccess);

        Map<Long, Order> orders = orderService.getOrders(orderIds);

        for (Order order : orders.values()) {
            assertEquals(cancellationRequest.getSubstatus(), order.getCancellationRequest().getSubstatus().name());
            assertEquals(cancellationRequest.getNotes(), order.getCancellationRequest().getNotes());
        }
    }

    @Test
    public void bulkCreateCancellationRequestsResponseTest() {
        List<Long> orderIds = Arrays.asList(order1.getId(), 1241551L, order2.getId());

        CompatibleCancellationRequest cancellationRequest =
                new CompatibleCancellationRequest(OrderSubstatus.USER_FRAUD.name(), "notes");

        BulkOrderCancellationResponse response = client.bulkCreateCancellationRequestsWithResponseBody(
                ClientRole.CALL_CENTER_OPERATOR,
                123L,
                cancellationRequest,
                orderIds);

        assertThat(response.getCancelledOrders(), containsInAnyOrder(order1.getId(), order2.getId()));
        assertThat(response.getFailedOrders(), containsInAnyOrder(1241551L));
    }

    @Test
    public void shouldOverrideCancelReasonIfSpecificProvided() {
        List<Long> orderIds = Arrays.asList(order1.getId(), order2.getId());

        CompatibleCancellationRequest cancellationRequest =
                new CompatibleCancellationRequest(OrderSubstatus.USER_FRAUD.name(), "notes");

        Map<Long, CompatibleCancellationRequest> specificRequestMap = Map.of(order2.getId(),
                new CompatibleCancellationRequest(OrderSubstatus.USER_CHANGED_MIND.name(), "notes"));

        BulkOrderCancellationResponse response = client.bulkCreateCancellationRequestsWithResponseBody(
                ClientRole.CALL_CENTER_OPERATOR,
                123L,
                cancellationRequest,
                orderIds,
                specificRequestMap);

        CancellationRequest cr1 = orderService.getOrder(order1.getId()).getCancellationRequest();
        CancellationRequest cr2 = orderService.getOrder(order2.getId()).getCancellationRequest();

        assertAll(
                () -> assertThat(response.getCancelledOrders(), containsInAnyOrder(order1.getId(), order2.getId())),
                () -> assertEquals(OrderSubstatus.USER_FRAUD, cr1.getSubstatus()),
                () -> assertEquals(OrderSubstatus.USER_CHANGED_MIND, cr2.getSubstatus())
        );
    }
}
