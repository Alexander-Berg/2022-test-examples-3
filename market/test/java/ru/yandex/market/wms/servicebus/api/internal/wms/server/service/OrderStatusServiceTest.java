package ru.yandex.market.wms.servicebus.api.internal.wms.server.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.logistic.api.model.common.OrderStatusType;
import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.OrderStatus;
import ru.yandex.market.logistic.api.utils.DateTime;
import ru.yandex.market.wms.common.spring.dto.orderstatus.OrderHistory;
import ru.yandex.market.wms.common.spring.dto.orderstatus.OrderStatusHistory;
import ru.yandex.market.wms.common.spring.servicebus.model.dto.OrderDto;
import ru.yandex.market.wms.common.spring.servicebus.model.request.PushOrdersStatusesChangedRequest;
import ru.yandex.market.wms.servicebus.api.external.logistics.client.LogisticsApiClient;
import ru.yandex.market.wms.servicebus.api.external.logistics.client.mapper.OrderStatusMapper;
import ru.yandex.market.wms.servicebus.api.internal.api.client.WmsApiClient;
import ru.yandex.market.wms.servicebus.api.internal.wms.server.exception.InternalServerException;
import ru.yandex.market.wms.servicebus.api.internal.wms.server.exception.NoOrdersToPushOrdersStatusesHistoryException;
import ru.yandex.market.wms.servicebus.api.internal.wms.server.mapper.OrderStatusHistoryMapper;
import ru.yandex.market.wms.servicebus.async.service.PushOrderStatusAsyncService;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderStatusServiceTest {

    private final LogisticsApiClient logisticsApiClient = mock(LogisticsApiClient.class);
    private final OrderStatusMapper orderStatusMapper = mock(OrderStatusMapper.class);
    private final WmsApiClient wmsApiClient = mock(WmsApiClient.class);
    private final OrderStatusHistoryMapper orderStatusHistoryMapper = mock(OrderStatusHistoryMapper.class);
    private final PushOrderStatusAsyncService pushOrderStatusAsyncService = mock(PushOrderStatusAsyncService.class);

    private final OrderStatusService orderStatusService = new OrderStatusService(logisticsApiClient, orderStatusMapper,
            wmsApiClient, orderStatusHistoryMapper);

    private static final int BATCH_MAX_SIZE = 100;

    @BeforeEach
    void setUp() {
        orderStatusService.setPushOrderStatusAsyncService(pushOrderStatusAsyncService);
    }

    @AfterEach
    void tearDown() {
        Mockito.reset(logisticsApiClient);
        Mockito.reset(orderStatusMapper);
        Mockito.reset(wmsApiClient);
        Mockito.reset(orderStatusHistoryMapper);
        Mockito.reset(pushOrderStatusAsyncService);
    }

    @Test
    void pushEmptyOrdersStatusesChangedWithHistoryThrowsException() {
        List<OrderDto> orders = List.of();
        assertThrows(
            NoOrdersToPushOrdersStatusesHistoryException.class,
            () -> orderStatusService.pushOrdersStatusesChangedWithHistory(orders)
        );
    }

    @Test
    void pushOrdersStatusesChangedWithHistoryWithCountLessThanMaxOrdersCountSendsOnePush() {
        OrderDto order = createOrder("test");
        ResourceId resourceId = createResourceId(order);
        List<OrderStatus> orderStatuses = List.of(createOderStatus());
        OrderStatusHistory orderStatusHistory = createOrderStatusHistory(resourceId, orderStatuses);
        OrderHistory orderHistory = new OrderHistory(orderStatusHistory);
        ru.yandex.market.logistic.api.model.fulfillment.response.entities.OrderStatusHistory lgwOrderStatusHistory =
                createLgwOrderStatusHistory(orderStatuses, resourceId);

        when(orderStatusMapper.orderToResourceId(order)).thenReturn(resourceId);
        when(wmsApiClient.getOrderHistory(resourceId)).thenReturn(orderHistory);
        when(orderStatusHistoryMapper.map(orderStatusHistory)).thenReturn(lgwOrderStatusHistory);
        when(logisticsApiClient.pushOrdersStatusHistory(anyList())).thenReturn(ResponseEntity.ok().build());
        orderStatusService.pushOrdersStatusesChangedWithHistory(List.of(order));

        ArgumentCaptor<List<ru.yandex.market.logistic.api.model.fulfillment.response.entities.OrderStatusHistory>>
                historyCaptor = ArgumentCaptor.forClass(List.class);
        verify(logisticsApiClient).pushOrdersStatusHistory(historyCaptor.capture());
        assertEquals(1, historyCaptor.getAllValues().size());
        assertEquals(1, historyCaptor.getAllValues().get(0).size());
        // Проверка соответствия входных параметров к тем, что передаются в LGW
        assertEquals(
            order.getExternOrderKey(),
            historyCaptor.getAllValues().get(0).get(0).getOrderId().getYandexId()
        );
    }

    @Test
    void pushOrdersStatusesChangedWithHistoryWithCountEqualsToBatchMaxSizeSendsOnePush() {
        int ordersCount = BATCH_MAX_SIZE;
        List<OrderDto> orders = IntStream.range(0, ordersCount).boxed()
            .map(i -> createOrder("test" + i))
            .peek(order -> {
                ResourceId resourceId = createResourceId(order);
                List<OrderStatus> orderStatuses = List.of(createOderStatus());
                OrderStatusHistory orderStatusHistory = createOrderStatusHistory(resourceId, orderStatuses);
                OrderHistory orderHistory = new OrderHistory(orderStatusHistory);
                ru.yandex.market.logistic.api.model.fulfillment.response.entities.OrderStatusHistory
                        lgwOrderStatusHistory = createLgwOrderStatusHistory(orderStatuses, resourceId);

                when(orderStatusMapper.orderToResourceId(order)).thenReturn(resourceId);
                when(wmsApiClient.getOrderHistory(resourceId)).thenReturn(orderHistory);
                when(orderStatusHistoryMapper.map(orderStatusHistory)).thenReturn(lgwOrderStatusHistory);
            })
            .collect(Collectors.toList());
        when(logisticsApiClient.pushOrdersStatusHistory(anyList())).thenReturn(ResponseEntity.ok().build());

        orderStatusService.pushOrdersStatusesChangedWithHistory(orders);

        verify(orderStatusMapper, times(ordersCount)).orderToResourceId(any(OrderDto.class));
        verify(wmsApiClient, times(ordersCount)).getOrderHistory(any());
        verify(orderStatusHistoryMapper, times(ordersCount)).map(any());
        verify(pushOrderStatusAsyncService, times(0)).sendPushOrderStatus(any());

        ArgumentCaptor<List<ru.yandex.market.logistic.api.model.fulfillment.response.entities.OrderStatusHistory>>
                historyCaptor = ArgumentCaptor.forClass(List.class);
        verify(logisticsApiClient, times(1)).pushOrdersStatusHistory(historyCaptor.capture());
        List<List<ru.yandex.market.logistic.api.model.fulfillment.response.entities.OrderStatusHistory>> values =
                historyCaptor.getAllValues();

        assertEquals(1, values.size());
        assertEquals(BATCH_MAX_SIZE, values.get(0).size());

        // Проверка соответствия входных параметров к тем, что передаются в LGW
        IntStream.range(0, BATCH_MAX_SIZE).forEach(i -> {
            String yandexId = values.get(0).get(i).getOrderId().getYandexId();
            assertEquals(orders.get(i).getExternOrderKey(), yandexId);
        });
    }

    @Test
    void pushOrdersStatusesChangedWithHistoryWithCountGreaterThanBatchMaxSizeSendsTwoPushes() {
        int ordersCount = BATCH_MAX_SIZE + 1;
        List<OrderDto> orders = IntStream.range(0, ordersCount).boxed()
            .map(i -> createOrder("test" + i))
            .peek(order -> {
                ResourceId resourceId = createResourceId(order);
                List<OrderStatus> orderStatuses = List.of(createOderStatus());
                OrderStatusHistory orderStatusHistory = createOrderStatusHistory(resourceId, orderStatuses);
                OrderHistory orderHistory = new OrderHistory(orderStatusHistory);
                ru.yandex.market.logistic.api.model.fulfillment.response.entities.OrderStatusHistory
                        lgwOrderStatusHistory = createLgwOrderStatusHistory(orderStatuses, resourceId);

                when(orderStatusMapper.orderToResourceId(order)).thenReturn(resourceId);
                when(wmsApiClient.getOrderHistory(resourceId)).thenReturn(orderHistory);
                when(orderStatusHistoryMapper.map(orderStatusHistory)).thenReturn(lgwOrderStatusHistory);
            })
            .collect(Collectors.toList());
        when(logisticsApiClient.pushOrdersStatusHistory(anyList())).thenReturn(ResponseEntity.ok().build());

        orderStatusService.pushOrdersStatusesChangedWithHistory(orders);

        verify(orderStatusMapper, times(ordersCount)).orderToResourceId(any(OrderDto.class));
        verify(wmsApiClient, times(ordersCount)).getOrderHistory(any());
        verify(orderStatusHistoryMapper, times(ordersCount)).map(any());
        verify(pushOrderStatusAsyncService, times(0)).sendPushOrderStatus(any());

        ArgumentCaptor<List<ru.yandex.market.logistic.api.model.fulfillment.response.entities.OrderStatusHistory>>
                historyCaptor = ArgumentCaptor.forClass(List.class);
        verify(logisticsApiClient, times(2)).pushOrdersStatusHistory(historyCaptor.capture());
        List<List<ru.yandex.market.logistic.api.model.fulfillment.response.entities.OrderStatusHistory>> values =
                historyCaptor.getAllValues();

        assertEquals(2, values.size());
        assertEquals(BATCH_MAX_SIZE, values.get(0).size());
        assertEquals(1, values.get(1).size());

        // Проверка соответствия входных параметров к тем, что передаются в LGW
        AtomicInteger currentBatch = new AtomicInteger();
        values.forEach(batch -> {
            int currentBatchIndex = currentBatch.getAndIncrement();
            IntStream.range(0, batch.size()).forEach(i -> {
                String yandexId = batch.get(i).getOrderId().getYandexId();
                int skip = currentBatchIndex * BATCH_MAX_SIZE;
                assertEquals(orders.get(skip + i).getExternOrderKey(), yandexId);
            });
        });
    }

    @Test
    void pushOrdersStatusesChangedWithHistoryWithMappingErrorsForAllOrdersThrowsException() {
        int ordersCount = BATCH_MAX_SIZE + 1;
        List<OrderDto> orders = IntStream.range(0, ordersCount).boxed()
            .map(i -> createOrder("test" + i))
            .peek(order -> {
                when(orderStatusMapper.orderToResourceId(order)).thenThrow(new RuntimeException());
            })
            .collect(Collectors.toList());

        assertThrows(
            InternalServerException.class,
            () -> orderStatusService.pushOrdersStatusesChangedWithHistory(orders)
        );

        verify(pushOrderStatusAsyncService, times(0)).sendPushOrderStatus(any());
        verify(logisticsApiClient, times(0)).pushOrdersStatusHistory(any());
    }

    @Test
    void pushOrdersStatusesChangedWithHistoryWithErrorsFromApiForAllOrdersThrowsException() {
        int ordersCount = BATCH_MAX_SIZE + 1;
        List<OrderDto> orders = IntStream.range(0, ordersCount).boxed()
            .map(i -> createOrder("test" + i))
            .peek(order -> {
                ResourceId resourceId = createResourceId(order);
                when(orderStatusMapper.orderToResourceId(order)).thenReturn(resourceId);
                when(wmsApiClient.getOrderHistory(resourceId)).thenThrow(new RuntimeException());
            })
            .collect(Collectors.toList());

        assertThrows(
            InternalServerException.class,
            () -> orderStatusService.pushOrdersStatusesChangedWithHistory(orders)
        );

        verify(pushOrderStatusAsyncService, times(0)).sendPushOrderStatus(any());
        verify(logisticsApiClient, times(0)).pushOrdersStatusHistory(any());
    }

    @Test
    void pushOrdersStatusesChangedWithHistoryWithLgwMappingErrorsForAllOrdersThrowsException() {
        int ordersCount = BATCH_MAX_SIZE + 1;
        List<OrderDto> orders = IntStream.range(0, ordersCount).boxed()
            .map(i -> createOrder("test" + i))
            .peek(order -> {
                ResourceId resourceId = createResourceId(order);
                List<OrderStatus> orderStatuses = List.of(createOderStatus());
                OrderStatusHistory orderStatusHistory = createOrderStatusHistory(resourceId, orderStatuses);
                OrderHistory orderHistory = new OrderHistory(orderStatusHistory);

                when(orderStatusMapper.orderToResourceId(order)).thenReturn(resourceId);
                when(wmsApiClient.getOrderHistory(resourceId)).thenReturn(orderHistory);
                when(orderStatusHistoryMapper.map(orderStatusHistory)).thenThrow(new RuntimeException());
            })
            .collect(Collectors.toList());

        assertThrows(
            InternalServerException.class,
            () -> orderStatusService.pushOrdersStatusesChangedWithHistory(orders)
        );

        verify(pushOrderStatusAsyncService, times(0)).sendPushOrderStatus(any());
        verify(logisticsApiClient, times(0)).pushOrdersStatusHistory(any());
    }

    @Test
    void pushOrdersStatusesChangedWithHistoryWithErrorFromLgwThrowsException() {
        int ordersCount = BATCH_MAX_SIZE + 1;
        List<OrderDto> orders = IntStream.range(0, ordersCount).boxed()
            .map(i -> createOrder("test" + i))
            .peek(order -> {
                ResourceId resourceId = createResourceId(order);
                List<OrderStatus> orderStatuses = List.of(createOderStatus());
                OrderStatusHistory orderStatusHistory = createOrderStatusHistory(resourceId, orderStatuses);
                OrderHistory orderHistory = new OrderHistory(orderStatusHistory);
                ru.yandex.market.logistic.api.model.fulfillment.response.entities.OrderStatusHistory
                        lgwOrderStatusHistory = createLgwOrderStatusHistory(orderStatuses, resourceId);

                when(orderStatusMapper.orderToResourceId(order)).thenReturn(resourceId);
                when(wmsApiClient.getOrderHistory(resourceId)).thenReturn(orderHistory);
                when(orderStatusHistoryMapper.map(orderStatusHistory)).thenReturn(lgwOrderStatusHistory);
            })
            .collect(Collectors.toList());

        when(logisticsApiClient.pushOrdersStatusHistory(any())).thenThrow(new RuntimeException());

        assertThrows(
            RuntimeException.class,
            () -> orderStatusService.pushOrdersStatusesChangedWithHistory(orders)
        );

        verify(pushOrderStatusAsyncService, times(0)).sendPushOrderStatus(any());
        verify(logisticsApiClient, times(1)).pushOrdersStatusHistory(any());
    }

    @Test
    void pushOrdersStatusesChangedWithHistoryWithErrorsNotForAllOrdersSendsPushAndCreateMessageWithProblemOrders() {
        int ordersCount = BATCH_MAX_SIZE + 1;

        Set<String> problemOrdersIndexes = Set.of("test0", "test10", "test50", "test100");

        List<OrderDto> orders = IntStream.range(0, ordersCount).boxed()
            .map(i -> createOrder("test" + i))
            .peek(order -> {
                ResourceId resourceId = createResourceId(order);
                List<OrderStatus> orderStatuses = List.of(createOderStatus());
                OrderStatusHistory orderStatusHistory = createOrderStatusHistory(resourceId, orderStatuses);
                OrderHistory orderHistory = new OrderHistory(orderStatusHistory);
                ru.yandex.market.logistic.api.model.fulfillment.response.entities.OrderStatusHistory
                        lgwOrderStatusHistory = createLgwOrderStatusHistory(orderStatuses, resourceId);

                when(orderStatusMapper.orderToResourceId(order)).thenReturn(resourceId);
                if (problemOrdersIndexes.contains(order.getExternOrderKey())) {
                    when(wmsApiClient.getOrderHistory(resourceId)).thenThrow(new RuntimeException());
                } else {
                    when(wmsApiClient.getOrderHistory(resourceId)).thenReturn(orderHistory);
                }
                when(orderStatusHistoryMapper.map(orderStatusHistory)).thenReturn(lgwOrderStatusHistory);
            })
            .collect(Collectors.toList());
        when(logisticsApiClient.pushOrdersStatusHistory(anyList())).thenReturn(ResponseEntity.ok().build());

        orderStatusService.pushOrdersStatusesChangedWithHistory(orders);

        verify(orderStatusMapper, times(ordersCount)).orderToResourceId(any(OrderDto.class));
        verify(wmsApiClient, times(ordersCount)).getOrderHistory(any());
        verify(orderStatusHistoryMapper, times(97)).map(any());

        ArgumentCaptor<PushOrdersStatusesChangedRequest> pushOrdersStatusesChangedRequestArgumentCaptor =
                ArgumentCaptor.forClass(PushOrdersStatusesChangedRequest.class);
        verify(pushOrderStatusAsyncService).sendPushOrderStatus(pushOrdersStatusesChangedRequestArgumentCaptor
                .capture());
        List<PushOrdersStatusesChangedRequest> pushOrdersStatusesChangedRequestArgumentValues =
                pushOrdersStatusesChangedRequestArgumentCaptor.getAllValues();
        assertEquals(1, pushOrdersStatusesChangedRequestArgumentValues.size());

        AtomicInteger counter = new AtomicInteger();
        pushOrdersStatusesChangedRequestArgumentValues.get(0).getOrders().forEach(order -> {
            if (!problemOrdersIndexes.contains(order.getExternOrderKey())) {
                throw new RuntimeException("Order key is not exists in problemOrdersIndexes");
            }
            counter.getAndIncrement();
        });
        assertEquals(problemOrdersIndexes.size(), counter.get());

        ArgumentCaptor<List<ru.yandex.market.logistic.api.model.fulfillment.response.entities.OrderStatusHistory>>
                historyCaptor = ArgumentCaptor.forClass(List.class);
        verify(logisticsApiClient).pushOrdersStatusHistory(historyCaptor.capture());
        List<List<ru.yandex.market.logistic.api.model.fulfillment.response.entities.OrderStatusHistory>> values =
                historyCaptor.getAllValues();

        assertEquals(1, values.size());
        assertEquals(97, values.get(0).size());

        // Проверка соответствия входных параметров к тем, что передаются в LGW
        assertEquals(0, values.get(0).stream()
            .filter(orderStatusHistory -> problemOrdersIndexes
                .contains(orderStatusHistory.getOrderId().getYandexId()))
            .count());
    }

    @Test
    void pushOrdersStatusHistory() {
        OrderDto order = createOrder("test");
        ResourceId resourceId = createResourceId(order);
        List<OrderStatus> orderStatuses = List.of(createOderStatus());
        List<ru.yandex.market.logistic.api.model.fulfillment.response.entities.OrderStatusHistory>
                orderStatusHistories = List.of(createLgwOrderStatusHistory(orderStatuses, resourceId));
        when(logisticsApiClient.pushOrdersStatusHistory(anyList())).thenReturn(ResponseEntity.ok().build());
        orderStatusService.pushOrdersStatusHistory(orderStatusHistories);
        verify(logisticsApiClient).pushOrdersStatusHistory(orderStatusHistories);
    }

    @Test
    void pushOrdersStatusesChangedWithHistoryThrowsErrorWhenLgwResponseIsNotOk() {
        OrderDto order = createOrder("test");
        ResourceId resourceId = createResourceId(order);
        List<OrderStatus> orderStatuses = List.of(createOderStatus());
        List<ru.yandex.market.logistic.api.model.fulfillment.response.entities.OrderStatusHistory>
                orderStatusHistories = List.of(createLgwOrderStatusHistory(orderStatuses, resourceId));

        when(logisticsApiClient.pushOrdersStatusHistory(anyList())).thenReturn(ResponseEntity.badRequest().build());

        assertThrows(
            InternalServerException.class,
            () -> orderStatusService.pushOrdersStatusHistory(orderStatusHistories)
        );
    }

    private static OrderDto createOrder(String key) {
        return OrderDto.builder()
            .orderKey(key)
            .externOrderKey(key)
            .build();
    }

    private static ResourceId createResourceId(OrderDto order) {
        return new ResourceId(order.getExternOrderKey(), order.getOrderKey());
    }

    private static OrderStatusHistory createOrderStatusHistory(
            ResourceId resourceId,
            List<OrderStatus> orderStatuses
    ) {
        return new OrderStatusHistory(
            orderStatuses,
            resourceId
        );
    }

    private OrderStatus createOderStatus() {
        return new OrderStatus(
            OrderStatusType.ORDER_CREATED_FF,
            DateTime.fromLocalDateTime(LocalDateTime.now()),
            null
        );
    }

    private
    ru.yandex.market.logistic.api.model.fulfillment.response.entities.OrderStatusHistory createLgwOrderStatusHistory(
            List<OrderStatus> orderStatuses,
            ResourceId resourceId
    ) {
        return new ru.yandex.market.logistic.api.model.fulfillment.response.entities.OrderStatusHistory(orderStatuses,
                resourceId);
    }
}
