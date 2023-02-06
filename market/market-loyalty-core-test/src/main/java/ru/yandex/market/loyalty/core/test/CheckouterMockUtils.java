package ru.yandex.market.loyalty.core.test;

import org.springframework.stereotype.Service;

import ru.yandex.market.checkout.checkouter.client.CheckouterClient;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderSearchRequest;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.PagedOrders;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.checkout.common.rest.Pager;
import ru.yandex.market.loyalty.core.utils.CheckouterUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

@Service
public class CheckouterMockUtils {
    private final CheckouterClient checkouterClient;

    public CheckouterMockUtils(
            CheckouterClient checkouterClient
    ) {
        this.checkouterClient = checkouterClient;
    }

    public void mockCheckoutGetOrdersResponse() {
        mockCheckoutGetOrdersResponse(CheckouterUtils.DEFAULT_ORDER_ID, DEFAULT_UID, OrderStatus.DELIVERED);
    }

    public void mockCheckoutGetOrdersResponse(long orderId, long uid, OrderStatus orderStatus) {
        Order order = CheckouterUtils.defaultOrder(orderStatus)
                .setOrderId(orderId)
                .setUid(uid)
                .addItem(CheckouterUtils.defaultOrderItem().build())
                .build();
        mockCheckoutGetOrdersResponse(order);
    }

    public void mockCheckoutGetOrdersResponse(Order order) {
        when(checkouterClient.getOrders(any(RequestClientInfo.class), argThat(arg -> !arg.archived))).thenReturn(
                new PagedOrders(
                        List.of(order),
                        new Pager(1, 0, 1,
                                5, 1, 0
                        )
                ));
    }

    public void mockCheckoutGetOrdersResponse(List<Order> orders) {
        when(checkouterClient.getOrders(any(RequestClientInfo.class), any(OrderSearchRequest.class))).thenReturn(
                new PagedOrders(
                        orders,
                        new Pager(orders.size(), 0, orders.size(),
                                5, 1, 0
                        )
                ));
    }

    public void mockCheckoutGetOrdersByUserRecent(Order... orders) {
        List<Order> orderList = new ArrayList<>(Arrays.asList(orders));

        when(checkouterClient.getOrdersByUserRecent(
                anyLong(), any(), any(), anyInt(), any(), any())).thenReturn(
                orderList
        );
    }

    public void mockCheckoutGetOrdersCountResponse(long uid, int count) {
        when(checkouterClient.getOrdersCount(any(RequestClientInfo.class), argThat(arg -> !arg.archived))).thenReturn(
                count
        );
    }

    public void mockCheckouterPagedEvents(Order... orders) {
        when(checkouterClient.getOrders(any(RequestClientInfo.class), argThat(arg -> !arg.archived))).thenReturn(
                new PagedOrders(
                        Arrays.asList(orders),
                        new Pager(orders.length, 1, orders.length, 50, 1, 1)
                )
        );
        when(checkouterClient.getOrders(any(RequestClientInfo.class), argThat(arg -> arg.archived))).thenReturn(
                new PagedOrders(
                        List.of(),
                        new Pager(0, 1, 0, 50, 1, 1)
                )
        );
    }

    public static CheckouterUtils.OrderBuilder getOrderInStatus(OrderStatus orderStatus) {
        return CheckouterUtils.defaultOrder(orderStatus)
                .setNoAuth(true)
                .addItem(CheckouterUtils.defaultOrderItem().build());
    }

    public static CheckouterUtils.OrderBuilder getOrderInStatus(Long orderId, OrderStatus orderStatus) {
        return getOrderInStatus(orderStatus).setOrderId(orderId);
    }

}
