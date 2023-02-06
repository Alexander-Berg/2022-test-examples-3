package ru.yandex.market.ocrm.module.checkouter.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.stereotype.Component;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.CheckouterOrderHistoryEventsApi;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.delivery.tracking.TrackCheckpoint;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvents;
import ru.yandex.market.checkout.checkouter.order.CompatibleCancellationRequest;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderEditOptions;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderItems;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.request.BasicOrderRequest;
import ru.yandex.market.checkout.checkouter.request.OrderRequest;
import ru.yandex.market.crm.util.CrmCollections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;

@Component
public class MockCheckouterAPI {

    private final CheckouterAPI checkouterAPI;
    private final CheckouterOrderHistoryEventsApi checkouterOrderHistoryEventsApi;
    private final Map<Long, Collection<OrderItem>> mockOrdersItemsMap = new HashMap<>();
    private final Map<Long, Collection<TrackCheckpoint>> mockOrdersTrackCheckpointMap = new HashMap<>();
    private final Map<Long, Order> mockOrderMap = new HashMap<>();

    public MockCheckouterAPI(
            CheckouterAPI checkouterAPI,
            CheckouterOrderHistoryEventsApi checkouterOrderHistoryEventsApi
    ) {
        this.checkouterAPI = checkouterAPI;
        this.checkouterOrderHistoryEventsApi = checkouterOrderHistoryEventsApi;
    }

    public void clear() {
        Mockito.reset(checkouterAPI);
        mockOrdersItemsMap.clear();
        mockOrdersTrackCheckpointMap.clear();
        mockOrderMap.clear();
    }

    public void mockGetOrder(Long orderId, Order order) {
        if (mockOrderMap.isEmpty()) {
            Mockito.when(checkouterAPI.getOrder(anyLong(), any(), any()))
                    .thenAnswer(invocation -> {
                        Long id = invocation.getArgument(0);
                        return mockOrderMap.get(id);
                    });

            Mockito.when(checkouterAPI.getOrder(any(), any()))
                    .thenAnswer(invocation -> {
                        OrderRequest request = invocation.getArgument(1);
                        return mockOrderMap.get(request.getOrderId());
                    });
        }

        mockOrderMap.put(orderId, order);
    }

    public void mockGetOrder(Long orderId, TrackCheckpoint trackCheckpoint) {
        if (mockOrderMap.isEmpty()) {
            Answer<Object> orderAnswer = invocation -> {
                Long invOrderId = invocation.getArgument(0);
                Order order = mockOrderMap.computeIfAbsent(invOrderId, id -> new Order());

                if (order.getDelivery() == null) {
                    order.setDelivery(new Delivery());
                }
                if (order.getDelivery().getParcels() == null) {
                    order.getDelivery().setParcels(new ArrayList<>());
                }
                Collection<TrackCheckpoint> checkpoints = mockOrdersTrackCheckpointMap.get(invOrderId);
                Track track = new Track();
                track.setCheckpoints(List.copyOf(checkpoints));
                Mockito.when(checkouterAPI.getTracksByOrderId(anyLong(), any(), anyLong())).thenReturn(List.of(track));
                return order;
            };

            Mockito.when(checkouterAPI.getOrder(anyLong(), any(), any())).thenAnswer(orderAnswer);
            Mockito.when(checkouterAPI.getOrder(any(), any())).thenAnswer(orderAnswer);

        } else {
            Collection<TrackCheckpoint> mockedCheckpoints = mockOrdersTrackCheckpointMap.getOrDefault(
                    orderId,
                    List.of()
            );

            Order order = mockOrderMap.computeIfAbsent(orderId, id -> new Order());
            if (order.getDelivery() == null) {
                order.setDelivery(new Delivery());
            }
            if (order.getDelivery().getParcels() == null) {
                order.getDelivery().setParcels(new ArrayList<>());
            }
            Track track = new Track();
            track.setCheckpoints(CrmCollections.concat(List.of(trackCheckpoint), mockedCheckpoints));
            Mockito.when(checkouterAPI.getTracksByOrderId(anyLong(), any(), anyLong())).thenReturn(List.of(track));
        }

        mockOrdersTrackCheckpointMap.merge(orderId, new ArrayList<>(List.of(trackCheckpoint)), CrmCollections::concat);
    }

    public void mockGetOrderItems(Long orderId, Collection<OrderItem> orderItems) {
        if (mockOrdersItemsMap.isEmpty()) {
            Mockito.when(checkouterAPI.getOrderItems(ArgumentMatchers.any(), ArgumentMatchers.any()))
                    .thenAnswer(invocation -> {
                        BasicOrderRequest request = invocation.getArgument(1);
                        Collection<OrderItem> items = mockOrdersItemsMap.get(request.getOrderId());
                        if (items == null) {
                            return null;
                        }
                        return new OrderItems(items);
                    });
        }

        mockOrdersItemsMap.merge(orderId, orderItems, CrmCollections::concat);
    }

    public void mockGetOrderHistory(Long orderId, Collection<OrderHistoryEvent> orderItems) {
        Mockito.doReturn(checkouterOrderHistoryEventsApi).when(checkouterAPI).orderHistoryEvents();
        Mockito.when(checkouterOrderHistoryEventsApi.getOrdersHistoryEvents(
                        ArgumentMatchers.argThat(argument ->
                                Arrays.stream(null != argument ? argument.getOrderIds() : new long[]{})
                                        .anyMatch(id -> Objects.equals(id, orderId))
                        )
                ))
                .thenReturn(new OrderHistoryEvents(orderItems));
    }

    public void clearMockGetOrderItems() {
        mockOrdersItemsMap.replaceAll((k, v) -> null);
    }

    /**
     * Мок вызова нового метода получения заказа через новое архивное API
     * (которое позволяет работать с архивными заказами)
     *
     * @deprecated Use mockGetOrder(Long orderId, Order order)
     */
    @Deprecated
    public void mockGetOrderNewApi(Long orderId, Order order) {
        mockGetOrder(orderId, order);
    }

    public void mockBeenCalled(Long orderId) {
        Mockito.doNothing()
                .when(checkouterAPI)
                .beenCalled(
                        ArgumentMatchers.eq(orderId),
                        ArgumentMatchers.any(),
                        ArgumentMatchers.anyLong(),
                        ArgumentMatchers.any()
                );
    }

    public void mockBeenCalledWithErrorThrow(Long orderId) {
        Mockito.doThrow(new RuntimeException("simulation of order confirmation error"))
                .when(checkouterAPI)
                .beenCalled(
                        ArgumentMatchers.eq(orderId),
                        ArgumentMatchers.any(),
                        ArgumentMatchers.anyLong(),
                        ArgumentMatchers.any()
                );
    }

    public void verifyBeenCalled(Long orderId) {
        Mockito.verify(checkouterAPI, times(1))
                .beenCalled(
                        ArgumentMatchers.eq(orderId),
                        ArgumentMatchers.any(),
                        ArgumentMatchers.anyLong(),
                        ArgumentMatchers.any()
                );
    }

    public void mockCreateCancellationRequest(Long orderId, Order expectedOrder) {
        Mockito.when(checkouterAPI.createCancellationRequest(
                        ArgumentMatchers.eq(orderId),
                        ArgumentMatchers.any(CompatibleCancellationRequest.class),
                        ArgumentMatchers.any(ClientRole.class),
                        ArgumentMatchers.anyLong()
                ))
                .thenReturn(expectedOrder);
    }

    public void mockCreateCancellationRequestWithErrorThrow(Long orderId) {
        Mockito.when(checkouterAPI.createCancellationRequest(
                        ArgumentMatchers.eq(orderId),
                        ArgumentMatchers.any(CompatibleCancellationRequest.class),
                        ArgumentMatchers.any(ClientRole.class),
                        ArgumentMatchers.anyLong()
                ))
                .thenThrow(new RuntimeException("simulation of order cancellation error"));
    }

    public void verifyCreateCancellationRequest(Long orderId) {
        Mockito.verify(checkouterAPI, times(1))
                .createCancellationRequest(
                        ArgumentMatchers.eq(orderId),
                        ArgumentMatchers.any(CompatibleCancellationRequest.class),
                        ArgumentMatchers.any(ClientRole.class),
                        ArgumentMatchers.anyLong()
                );
    }

    public void mockUpdateOrderStatus(Long orderId, OrderStatus orderStatus, OrderSubstatus orderSubstatus) {
        Mockito.verify(checkouterAPI).updateOrderStatus(
                eq(orderId),
                any(),
                anyLong(),
                anyLong(),
                eq(orderStatus),
                eq(orderSubstatus)
        );
    }

    public void mockUpdateOrderStatusAndReturnOrder(Order order) {
        Mockito.when(checkouterAPI.updateOrderStatus(
                        anyLong(),
                        any(),
                        anyLong(),
                        anyLong(),
                        any(),
                        any()
                ))
                .thenReturn(order);
    }

    public void mockGetOrderEditOptions(OrderEditOptions options) {
        Mockito.when(checkouterAPI.getOrderEditOptions(anyLong(), any(), any(), any(), any(), anyBoolean()))
                .thenReturn(options);
        Mockito.when(checkouterAPI.getOrderEditOptions(anyLong(), any(), any(), any(), any()))
                .thenReturn(options);
    }
}
