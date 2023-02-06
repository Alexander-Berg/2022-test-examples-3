package ru.yandex.market.abo.core.resupply.registry;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.base.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.core.util.Assert;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import ru.yandex.common.util.collections.Either;
import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.abo.core.resupply.registry.checkouter.OrderFetchingError;
import ru.yandex.market.abo.core.resupply.registry.checkouter.RegistryCheckouterService;
import ru.yandex.market.abo.cpa.order.CheckouterOrdersHelper;
import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.CheckouterOrderHistoryEventsApi;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.event.HistoryEventReason;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvents;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.OfferItemKey;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderItemInstance;
import ru.yandex.market.checkout.checkouter.order.OrderItemInstances;
import ru.yandex.market.checkout.checkouter.order.OrderSearchRequest;
import ru.yandex.market.checkout.checkouter.order.PagedOrders;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequest;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestStatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.itemsremoval.ItemsRemovalChangeRequestPayload;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.checkout.common.rest.Pager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class RegistryCheckouterServiceTest {

    private CheckouterAPI checkouterClient;
    private CheckouterOrderHistoryEventsApi historyEventsApi;
    private CheckouterOrdersHelper checkouterOrdersHelper;
    private static final ObjectMapper mapper = new ObjectMapper();

    private RegistryCheckouterService checkouterService;

    @BeforeEach
    public void init() {
        checkouterClient = Mockito.mock(CheckouterAPI.class);
        historyEventsApi = Mockito.mock(CheckouterOrderHistoryEventsApi.class);
        checkouterOrdersHelper = Mockito.mock(CheckouterOrdersHelper.class);
        checkouterService = new RegistryCheckouterService(checkouterClient, checkouterOrdersHelper);
        when(checkouterClient.orderHistoryEvents()).thenReturn(historyEventsApi);
    }

    @AfterEach
    public void resetMocks() {
        Mockito.reset(checkouterClient, historyEventsApi, checkouterOrdersHelper);
    }

    @Test
    void processPartialReturnOrdersEmptyOrders() {
        Assert.isEmpty(checkouterService.processPartialReturnOrders(Collections.emptyList()));
    }

    @Test
    void processPartialReturnOrdersAsFull() throws Exception {
        Order orderBefore = getOrder();
        Order orderAfter = getOrderAfter();
        OrderHistoryEvent orderHistoryEvent = new OrderHistoryEvent();
        orderHistoryEvent.setOrderBefore(orderBefore);
        orderHistoryEvent.setOrderAfter(orderAfter);
        orderHistoryEvent.setReason(HistoryEventReason.USER_REQUESTED_REMOVE);
        orderHistoryEvent.setType(HistoryEventType.ITEMS_UPDATED);
        ClientInfo clientInfo = new ClientInfo(ClientRole.WAREHOUSE, 1L);
        orderHistoryEvent.setAuthor(clientInfo);
        OrderHistoryEvents events = new OrderHistoryEvents();
        events.setContent(Collections.singletonList(orderHistoryEvent));
        when(historyEventsApi.getOrdersHistoryEvents(any())).thenReturn(events);
        List<Pair<Order, ? extends Collection<OrderItem>>> pairs =
                checkouterService.processPartialReturnOrders(Collections.singletonList(orderAfter));
        Assertions.assertEquals(1, pairs.size());
        Assertions.assertEquals(1, pairs.get(0).getSecond().size());
    }

    @Test
    void processPartialReturnOrdersAsPartial() throws Exception {
        Order orderBefore = getOrder();
        Order orderAfter = getOrderAfter();
        OrderHistoryEvent orderHistoryEvent = new OrderHistoryEvent();
        orderHistoryEvent.setOrderBefore(orderBefore);
        orderHistoryEvent.setOrderAfter(orderAfter);
        orderHistoryEvent.setReason(HistoryEventReason.USER_REQUESTED_REMOVE);
        orderHistoryEvent.setType(HistoryEventType.ITEMS_UPDATED);
        ClientInfo clientInfo = new ClientInfo(ClientRole.DELIVERY_SERVICE, 1L);
        orderHistoryEvent.setAuthor(clientInfo);
        OrderHistoryEvents events = new OrderHistoryEvents();
        events.setContent(Collections.singletonList(orderHistoryEvent));
        when(historyEventsApi.getOrdersHistoryEvents(any())).thenReturn(events);
        List<Pair<Order, ? extends Collection<OrderItem>>> pairs =
                checkouterService.processPartialReturnOrders(Collections.singletonList(orderAfter));
        Assertions.assertEquals(1, pairs.size());
        Assertions.assertEquals(2, pairs.get(0).getSecond().size());
        List<OrderItem> orderItems = pairs.get(0).getSecond().stream().collect(Collectors.toList());
        Assertions.assertEquals(3, orderItems.get(0).getCount());
        Assertions.assertEquals(3, orderItems.get(1).getCount());
    }

    @Test
    void processPartialReturnOrdersAsFullCaseOfReason() throws Exception {
        Order orderBefore = getOrder();
        Order orderAfter = getOrderAfter();
        OrderHistoryEvent orderHistoryEvent = new OrderHistoryEvent();
        orderHistoryEvent.setOrderBefore(orderBefore);
        orderHistoryEvent.setOrderAfter(orderAfter);
        orderHistoryEvent.setReason(HistoryEventReason.ITEMS_NOT_FOUND);
        orderHistoryEvent.setType(HistoryEventType.ITEMS_UPDATED);
        ClientInfo clientInfo = new ClientInfo(ClientRole.DELIVERY_SERVICE, 1L);
        orderHistoryEvent.setAuthor(clientInfo);
        OrderHistoryEvents events = new OrderHistoryEvents();
        events.setContent(Collections.singletonList(orderHistoryEvent));
        when(historyEventsApi.getOrdersHistoryEvents(any())).thenReturn(events);
        List<Pair<Order, ? extends Collection<OrderItem>>> pairs =
                checkouterService.processPartialReturnOrders(Collections.singletonList(orderAfter));
        Assertions.assertEquals(1, pairs.size());
        Assertions.assertEquals(1, pairs.get(0).getSecond().size());
    }

    @Test
    void processPartialReturnOrdersAsFullCaseOfClientRole() throws Exception {
        Order orderBefore = getOrder();
        Order orderAfter = getOrderAfter();
        OrderHistoryEvent orderHistoryEvent = new OrderHistoryEvent();
        orderHistoryEvent.setOrderBefore(orderBefore);
        orderHistoryEvent.setOrderAfter(orderAfter);
        orderHistoryEvent.setReason(HistoryEventReason.USER_REQUESTED_REMOVE);
        orderHistoryEvent.setType(HistoryEventType.ITEMS_UPDATED);
        ClientInfo clientInfo = new ClientInfo(ClientRole.WAREHOUSE, 1L);
        orderHistoryEvent.setAuthor(clientInfo);
        OrderHistoryEvents events = new OrderHistoryEvents();
        events.setContent(Collections.singletonList(orderHistoryEvent));
        when(historyEventsApi.getOrdersHistoryEvents(any())).thenReturn(events);
        List<Pair<Order, ? extends Collection<OrderItem>>> pairs =
                checkouterService.processPartialReturnOrders(Collections.singletonList(orderAfter));
        Assertions.assertEquals(1, pairs.size());
        Assertions.assertEquals(1, pairs.get(0).getSecond().size());
    }



    @NotNull
    private Order getOrder() throws Exception {
        OrderItem orderItem = new OrderItem();
        orderItem.setOfferItemKey(new OfferItemKey("0", 0L, "0"));
        orderItem.setOrderId(3960222L);
        orderItem.setId(5271467L);
        orderItem.setShopSku("10125");
        orderItem.setSupplierId(48000L);
        orderItem.setCount(4);
        HashSet<Integer> objects = new HashSet<>();
        objects.add(600);
        orderItem.setCargoTypes(objects);
        orderItem.setInstances(getArrayNode());

        OrderItem orderItem2 = new OrderItem();
        orderItem2.setOfferItemKey(new OfferItemKey("1", 1L, "1"));
        orderItem2.setOrderId(3960222L);
        orderItem2.setId(52714672L);
        orderItem2.setShopSku("10125");
        orderItem2.setSupplierId(48000L);
        orderItem2.setCount(3);
        orderItem2.setCargoTypes(objects);
        orderItem2.setInstances(getArrayNode());

        Order order = new Order();
        order.setId(3960222L);
        order.setItems(Arrays.asList(orderItem, orderItem2));
        return order;
    }

    @NotNull
    private Order getOrderAfter() throws Exception {
        OrderItem orderItem = new OrderItem();
        orderItem.setOfferItemKey(new OfferItemKey("0", 0L, "0"));
        orderItem.setOrderId(3960222L);
        orderItem.setId(5271467L);
        orderItem.setShopSku("10125");
        orderItem.setSupplierId(48000L);
        orderItem.setCount(1);
        HashSet<Integer> objects = new HashSet<>();
        objects.add(600);
        orderItem.setCargoTypes(objects);
        orderItem.setInstances(getArrayNode());


        Order order = new Order();
        order.setId(3960222L);
        order.setItems(Arrays.asList(orderItem));
        return order;
    }

    private ArrayNode getArrayNode() throws Exception {
        String json = IOUtils.toString(getClass().getResourceAsStream("/registry/order_item_instances.json"),
                Charsets.UTF_8);

        return (ArrayNode) mapper.readTree(json);
    }

    @Test
    public void getOrdersForEmptyItems() {
        Map<String, Either<OrderFetchingError, Order>> orders = checkouterService.getOrdersByItems(null);
        assertTrue(orders.isEmpty());
        orders = checkouterService.getOrdersByItems(Collections.emptyList());
        assertTrue(orders.isEmpty());
    }

    @Test
    public void getOrdersWithoutErrors() {
        Pager pager = new Pager(2, 1, 3, 20, 1, 1);
        List<Order> allOrders = generateOrders(2);
        PagedOrders pagedOrders = new PagedOrders(allOrders, pager);
        ArgumentCaptor<OrderSearchRequest> captor = ArgumentCaptor.forClass(OrderSearchRequest.class);
        Mockito.when(checkouterClient.getOrders(any(RequestClientInfo.class), captor.capture()))
                .thenReturn(pagedOrders);
        Map<String, Either<OrderFetchingError, Order>> orders =
                checkouterService.getOrdersByItems(generateRegistryItems(2));
        assertEquals(2, orders.size());
        for (int i = 0; i < 2; i++) {
            Either<OrderFetchingError, Order> orderOrError = orders.get(String.valueOf(i * 10 + 10));
            assertFalse(orderOrError.isLeftNotRight());
            assertEquals(allOrders.get(i), orderOrError.asRight());
        }

        List<OrderSearchRequest> requests = captor.getAllValues();
        assertEquals(1, requests.size());
        assertCorrectRequest(requests.get(0), allOrders);
    }

    @Test
    public void getOrdersWithOneRetry() {
        Pager pager = new Pager(2, 1, 3, 20, 1, 1);
        List<Order> allOrders = generateOrders(2);
        PagedOrders pagedOrders = new PagedOrders(allOrders, pager);
        ArgumentCaptor<OrderSearchRequest> captor = ArgumentCaptor.forClass(OrderSearchRequest.class);
        Mockito.when(checkouterClient.getOrders(any(RequestClientInfo.class), captor.capture()))
                .thenThrow(new RuntimeException("Connection timeout"))
                .thenReturn(pagedOrders);
        Map<String, Either<OrderFetchingError, Order>> orders =
                checkouterService.getOrdersByItems(generateRegistryItems(2));
        assertEquals(2, orders.size());
        for (int i = 0; i < 2; i++) {
            Either<OrderFetchingError, Order> orderOrError = orders.get(String.valueOf(i * 10 + 10));
            assertFalse(orderOrError.isLeftNotRight());
            assertEquals(allOrders.get(i), orderOrError.asRight());
        }

        List<OrderSearchRequest> requests = captor.getAllValues();
        assertEquals(2, requests.size());
        assertCorrectRequest(requests.get(0), allOrders);
        assertCorrectRequest(requests.get(1), allOrders);
    }

    @Test
    public void getOrdersWithErrors() {
        List<Order> allOrders = generateOrders(20);

        Pager pager = new Pager(20, 1, 20, 20, 1, 1);
        List<Order> pageOrders = allOrders.subList(0, 19);
        PagedOrders page = new PagedOrders(pageOrders, pager);

        ArgumentCaptor<OrderSearchRequest> captor = ArgumentCaptor.forClass(OrderSearchRequest.class);
        Mockito.when(checkouterClient.getOrders(any(RequestClientInfo.class), captor.capture())).thenReturn(page);

        List<RegistryItem> registryItems = generateRegistryItems(20);
        registryItems.get(10).setOrderId("EXT123");
        Map<String, Either<OrderFetchingError, Order>> orders = checkouterService.getOrdersByItems(registryItems);

        assertEquals(20, orders.size());
        for (int i = 0; i < 20; i++) {
            String orderId = i == 10 ? "EXT123" : String.valueOf(i * 10 + 10);
            Either<OrderFetchingError, Order> orderOrError = orders.get(orderId);
            if (i == 10) {
                assertError(orderOrError, OrderFetchingError.INCORRECT_ORDER_ID_FORMAT);
            } else if (i == 19) {
                assertError(orderOrError, OrderFetchingError.ORDER_NOT_FOUND);
            } else {
                assertFalse(orderOrError.isLeftNotRight());
                assertEquals(allOrders.get(i), orderOrError.asRight());
            }
        }

        List<OrderSearchRequest> requests = captor.getAllValues();
        assertEquals(1, requests.size());
        allOrders.remove(10);
        assertCorrectRequest(requests.get(0), allOrders);
    }


    @Test
    public void getOrdersWithTimeout() {
        List<Order> allOrders = generateOrders(20);

        ArgumentCaptor<OrderSearchRequest> captor = ArgumentCaptor.forClass(OrderSearchRequest.class);
        Mockito.when(checkouterClient.getOrders(any(RequestClientInfo.class), captor.capture()))
                .thenThrow(new RuntimeException("Connection timeout"));

        List<RegistryItem> registryItems = generateRegistryItems(20);
        Map<String, Either<OrderFetchingError, Order>> orders = checkouterService.getOrdersByItems(registryItems);

        assertEquals(20, orders.size());

        for (int i = 0; i < 20; i++) {
            String orderId = String.valueOf(i * 10 + 10);
            Either<OrderFetchingError, Order> orderOrError = orders.get(orderId);
            assertError(orderOrError, OrderFetchingError.TECHNICAL_ERROR);
        }

        List<OrderSearchRequest> requests = captor.getAllValues();
        assertEquals(3, requests.size());
        assertCorrectRequest(requests.get(0), allOrders);
        assertCorrectRequest(requests.get(1), allOrders);
        assertCorrectRequest(requests.get(2), allOrders);
    }

    private static RegistryItem createRegistryItem(long orderId) {
        RegistryItem registryItem = new RegistryItem();
        registryItem.setOrderId(String.valueOf(orderId));
        return registryItem;
    }

    private static Order createOrder(long orderId) {
        Order order = new Order();
        order.setId(orderId);
        return order;
    }

    private static List<Order> generateOrders(int size) {
        return IntStream.iterate(0, i -> i < size, i -> i + 1)
                .mapToObj(i -> createOrder(i * 10 + 10))
                .collect(Collectors.toList());
    }

    private static List<RegistryItem> generateRegistryItems(int size) {
        return IntStream.iterate(0, i -> i < size, i -> i + 1)
                .mapToObj(i -> createRegistryItem(i * 10 + 10))
                .collect(Collectors.toList());
    }

    private static void assertCorrectRequest(OrderSearchRequest request, List<Order> orders) {
        List<Long> orderIds = orders.stream()
                .map(Order::getId)
                .collect(Collectors.toList());
        assertEquals(Collections.singleton(Color.BLUE), request.rgbs);
        assertEquals(orderIds, request.orderIds);
        Pager requestPager = request.pageInfo;
        assertEquals(orders.size(), requestPager.getPageSize());
    }

    private static void assertError(Either<OrderFetchingError, Order> either, OrderFetchingError expectedError) {
        assertTrue(either.isLeftNotRight());
        assertEquals(expectedError, either.asLeft());
    }
}
