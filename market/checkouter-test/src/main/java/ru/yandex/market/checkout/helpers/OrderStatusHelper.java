package ru.yandex.market.checkout.helpers;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.google.common.base.Throwables;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderService;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.OrderUpdateService;
import ru.yandex.market.checkout.checkouter.order.UpdateOrderStatusReasonDetails;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;
import ru.yandex.market.checkout.common.WebTestHelper;
import ru.yandex.market.checkout.helpers.utils.MockMvcAware;
import ru.yandex.market.checkout.helpers.utils.ResultActionsContainer;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;
import ru.yandex.market.checkout.util.services.OrderServiceHelper;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.BUSINESS_ID;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.CLIENT_ID;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.CLIENT_ROLE;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.REASON_DETAILS;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.SHOP_ID;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.STATUS;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.SUBSTATUS;
import static ru.yandex.market.checkout.checkouter.client.ClientRole.BUSINESS;
import static ru.yandex.market.checkout.checkouter.client.ClientRole.BUSINESS_USER;
import static ru.yandex.market.checkout.checkouter.client.ClientRole.SHOP;
import static ru.yandex.market.checkout.checkouter.client.ClientRole.SHOP_USER;

@WebTestHelper
public class OrderStatusHelper extends MockMvcAware {

    private static final int DEFAULT_ITEMS_COUNT = 5;
    private static final List<ClientRole> SHOP_ROLES = Arrays.asList(SHOP, SHOP_USER);
    private static final LinkedHashSet<OrderStatus> SUCCESSFUL_STATUS_GRAPH = new LinkedHashSet<>();

    static {
        SUCCESSFUL_STATUS_GRAPH.add(OrderStatus.UNPAID);
//        SUCCESSFUL_STATUS_GRAPH.add(OrderStatus.PENDING);
        SUCCESSFUL_STATUS_GRAPH.add(OrderStatus.PROCESSING);
        SUCCESSFUL_STATUS_GRAPH.add(OrderStatus.DELIVERY);
        SUCCESSFUL_STATUS_GRAPH.add(OrderStatus.PICKUP);
        SUCCESSFUL_STATUS_GRAPH.add(OrderStatus.DELIVERED);
    }

    @Autowired
    private OrderPayHelper orderPayHelper;
    @Autowired
    private OrderService orderService;
    @Autowired
    private OrderUpdateService orderUpdateService;
    @Autowired
    private TmsTaskHelper tmsTaskHelper;
    @Autowired
    private OrderServiceHelper orderServiceHelper;
    @Autowired
    private QueuedCallService queuedCallService;
    @Autowired
    private ReturnHelper returnHelper;


    @Autowired
    public OrderStatusHelper(WebApplicationContext webApplicationContext,
                             TestSerializationService testSerializationService) {
        super(webApplicationContext, testSerializationService);
    }

    public Order updateOrderStatus(long orderId, OrderStatus status) {
        return updateOrderStatus(orderId, ClientInfo.SYSTEM, status, null, null, null);
    }

    public Order updateOrderStatusWithReasonDetails(
            long orderId,
            OrderStatus status,
            OrderSubstatus substatus,
            UpdateOrderStatusReasonDetails reasonDetails
    ) {
        return updateOrderStatus(orderId, ClientInfo.SYSTEM, status, substatus, null, reasonDetails);
    }

    public Order updateOrderStatus(long orderId, OrderStatus status, OrderSubstatus substatus) {
        return updateOrderStatus(orderId, ClientInfo.SYSTEM, status, substatus, null, null);
    }

    public Order updateOrderStatus(long orderId, ClientInfo clientInfo, OrderStatus status, OrderSubstatus substatus) {
        return updateOrderStatus(orderId, clientInfo, status, substatus, null, null);
    }

    public Order updateOrderStatus(long orderId,
                                   ClientInfo clientInfo,
                                   OrderStatus status,
                                   OrderSubstatus substatus,
                                   ResultActionsContainer container,
                                   UpdateOrderStatusReasonDetails reasonDetails) {
        try {
            MockHttpServletRequestBuilder builder = makeBuilder(orderId, clientInfo, status, substatus, reasonDetails);

            return performApiRequest(builder, Order.class, container);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public ResultActions updateOrderStatusForActions(long orderId,
                                                     ClientInfo clientInfo,
                                                     OrderStatus status,
                                                     OrderSubstatus substatus) {
        try {
            MockHttpServletRequestBuilder builder = makeBuilder(orderId, clientInfo, status, substatus, null);
            return performApiRequest(builder);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private MockHttpServletRequestBuilder makeBuilder(long orderId,
                                                      ClientInfo clientInfo,
                                                      OrderStatus status,
                                                      OrderSubstatus substatus,
                                                      UpdateOrderStatusReasonDetails reasonDetails) {
        MockHttpServletRequestBuilder builder = post("/orders/{orderId}/status", orderId)
                .param(CLIENT_ROLE, clientInfo.getRole().name())
                .param(STATUS, status.name());

        if (substatus != null) {
            builder.param(SUBSTATUS, substatus.name());
        }
        if (clientInfo.getId() != null) {
            builder.param(CLIENT_ID, String.valueOf(clientInfo.getId()));
        }
        if (SHOP_ROLES.contains(clientInfo.getRole()) && clientInfo.getShopId() != null) {
            builder.param(SHOP_ID, String.valueOf(clientInfo.getShopId()));
        }
        if ((clientInfo.getRole() == BUSINESS || clientInfo.getRole() == BUSINESS_USER)
                && clientInfo.getBusinessId() != null) {
            builder.param(BUSINESS_ID, String.valueOf(clientInfo.getBusinessId()));
        }
        if (reasonDetails != null) {
            builder.param(REASON_DETAILS, String.valueOf(reasonDetails));
        }
        return builder;
    }

    private Order proceedOrderToStatus(Order order, OrderStatus status, boolean callTask, boolean skipPayment) {
        while (order.getStatus() != status) {
            if (!skipPayment) {
                if (order.getStatus() == OrderStatus.UNPAID) {
                    orderPayHelper.payForOrder(order);
                    order = orderService.getOrder(order.getId());
                    continue;
                }
            }

            if (status == OrderStatus.CANCELLED) {
                return updateOrderStatus(order.getId(), OrderStatus.CANCELLED, OrderSubstatus.USER_CHANGED_MIND);
            }

            if (status == OrderStatus.DELIVERED) {
                returnHelper.mockSupplierInfo();
                returnHelper.mockShopInfo();
            }

            Pair<OrderStatus, OrderSubstatus> nextPair = nextStatus(order);
            if (nextPair == null) {
                throw new IllegalStateException(
                        "Cannot proceed to status " + status + " from status " + order.getStatus()
                );
            }
            order = updateOrderStatus(order.getId(), nextPair.first, nextPair.second);
            if (nextPair.first == OrderStatus.DELIVERY && callTask) {
                processHeldPaymentsTask();
                queuedCallService.executeQueuedCallBatch(CheckouterQCType.PAYMENT_PARTIAL_UNHOLD);
                queuedCallService.executeQueuedCallBatch(CheckouterQCType.PAYMENT_CANCEL);
                processHeldPaymentsTask();
                order = orderService.getOrder(order.getId());
            }
            if (nextPair.first == status) {
                return order;
            }
        }
        return order;
    }

    public Order proceedOrderToStatus(Order order, OrderStatus status) {
        return proceedOrderToStatus(order, status, true, false);
    }

    public Order proceedOrderToStatusWithoutTask(Order order, OrderStatus status) {
        return proceedOrderToStatus(order, status, false, false);
    }

    public Order proceedOrderFromUnpaidToCancelled(Order order) {
        return proceedOrderToStatus(order, OrderStatus.CANCELLED, false, true);
    }

    private static Pair<OrderStatus, OrderSubstatus> nextStatus(Order order) {
        switch (order.getStatus()) {
            case PENDING:
                if (order.isPreorder() && order.getSubstatus() == OrderSubstatus.PREORDER) {
                    return Pair.of(OrderStatus.PENDING, OrderSubstatus.AWAIT_CONFIRMATION);
                }

                return Pair.of(OrderStatus.PROCESSING, null);
            case PLACING:
                return Pair.of(OrderStatus.RESERVED, null);
            case RESERVED:
                return Pair.of(OrderStatus.PROCESSING, null);
            case PROCESSING:
                return Pair.of(OrderStatus.DELIVERY, null);
            case DELIVERY:
                return Pair.of((order.getDelivery().getType() == DeliveryType.PICKUP) ? OrderStatus.PICKUP :
                        OrderStatus.DELIVERED, null);
            case PICKUP:
                return Pair.of(OrderStatus.DELIVERED, null);
            default:
                return null;
        }
    }

    public Order createOrderWithStatusTransitions(Delivery delivery, OrderStatus... statuses) {
        return createOrderWithStatusTransitions((order) -> {
        }, delivery, statuses);
    }

    public Order createOrderWithStatusTransitions(Consumer<Order> orderConfigurer,
                                                  Delivery delivery,
                                                  OrderStatus... statuses) {
        Order order = OrderProvider.getBlueOrder();
        order.setDelivery(delivery);
        order.setItems(Collections.singleton(
                OrderItemProvider.buildOrderItem("qwerty", DEFAULT_ITEMS_COUNT)
        ));

        orderConfigurer.accept(order);
        order = orderServiceHelper.saveOrder(order);
        if (order.getStatus() == OrderStatus.PENDING) {
            order = orderUpdateService.updateOrderStatus(order.getId(), OrderStatus.PROCESSING);
        }
        assertEquals(OrderStatus.PROCESSING, order.getStatus());
        assertNull(order.getStatusExpiryDate());

        for (OrderStatus status : statuses) {
            order = orderUpdateService.updateOrderStatus(order.getId(), status);
        }
        return orderService.getOrder(order.getId(), ClientInfo.SYSTEM, null, null);
    }

    public Order requestStatusUpdate(long orderId, OrderStatus status) throws Exception {
        return requestStatusUpdate(orderId, ClientRole.SYSTEM, String.valueOf(BuyerProvider.UID), status, null);
    }

    public Order requestStatusUpdate(long orderId,
                                     ClientRole clientRole,
                                     String clientId,
                                     OrderStatus status) throws Exception {
        return requestStatusUpdate(orderId, clientRole, clientId, status, null);
    }

    public Order requestStatusUpdate(long orderId,
                                     ClientRole clientRole,
                                     String clientId,
                                     OrderStatus status,
                                     OrderSubstatus substatus) throws Exception {
        MvcResult result = rawRequestStatusUpdate(orderId, clientRole, clientId, status, substatus)
                .andExpect(status().isOk())
                .andDo(log())
                .andReturn();
        return testSerializationService.deserializeCheckouterObject(
                result.getResponse().getContentAsString(),
                Order.class
        );
    }

    public String requestStatusUpdateAndReturnString(long orderId,
                                                     ClientRole clientRole,
                                                     String clientId,
                                                     OrderStatus status,
                                                     OrderSubstatus substatus) throws Exception {
        return rawRequestStatusUpdate(orderId, clientRole, clientId, status, substatus)
                .andDo(log())
                .andReturn().getResponse().getContentAsString();
    }

    private ResultActions rawRequestStatusUpdate(long orderId,
                                                 ClientRole clientRole,
                                                 String clientId,
                                                 OrderStatus status,
                                                 OrderSubstatus substatus) throws Exception {
        MockHttpServletRequestBuilder requestBuilder = post("/orders/{orderId}/status", orderId)
                .param(CheckouterClientParams.CLIENT_ROLE, clientRole.name())
                .param(CheckouterClientParams.CLIENT_ID, clientId)
                .param(CheckouterClientParams.UID, clientId)
                .param(CheckouterClientParams.STATUS, String.valueOf(status))
                .contentType(MediaType.APPLICATION_JSON);

        if (substatus != null) {
            requestBuilder.param(CheckouterClientParams.SUBSTATUS, String.valueOf(substatus));
        }

        return mockMvc.perform(requestBuilder);
    }

    public interface Asserter {

        void accept(Order order, OrderStatus orderStatus) throws Exception;
    }

    public void proceedAllStatusesAndCheck(Supplier<Order> orderSupplier,
                                           Asserter asserter) throws Exception {

        checkCancelled(orderSupplier, asserter);

        Order order = orderSupplier.get();
        for (OrderStatus nextStatus : SUCCESSFUL_STATUS_GRAPH) {
            order = proceedOrderToStatus(order, nextStatus);
            try {
                asserter.accept(order, nextStatus);
            } catch (AssertionError assertionError) {
                throw new AssertionError("Failed on status " + nextStatus, assertionError);
            }
        }
    }

    private void checkCancelled(Supplier<Order> orderSupplier, Asserter asserter) throws Exception {
        Order orderToCancel = orderSupplier.get();
        orderToCancel = proceedOrderToStatus(orderToCancel, OrderStatus.CANCELLED);
        try {
            asserter.accept(orderToCancel, OrderStatus.CANCELLED);
        } catch (AssertionError assertionError) {
            throw new AssertionError("Failed on status CANCELLED", assertionError);
        }
    }

    public void processHeldPaymentsTask() {
        tmsTaskHelper.runProcessHeldPaymentsTaskV2();
    }
}
