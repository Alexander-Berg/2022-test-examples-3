package ru.yandex.market.ocrm.module.order;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.entity.AttributeTypeService;
import ru.yandex.market.jmf.entity.EntityStorageService;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.metadata.metaclass.Attribute;
import ru.yandex.market.jmf.utils.Maps;
import ru.yandex.market.ocrm.module.checkouter.CheckouterViewsHelper;
import ru.yandex.market.ocrm.module.order.domain.Order;
import ru.yandex.market.ocrm.module.order.domain.OrderItem;
import ru.yandex.market.ocrm.module.order.domain.OrderItemHistory;
import ru.yandex.market.ocrm.module.order.test.OrderTestUtils;

import static org.junit.jupiter.params.provider.Arguments.arguments;

@Transactional
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ModuleOrderTestConfiguration.class)
public class OrderItemEntityStorageStrategyTest {
    @Inject
    OrderTestUtils orderTestUtils;
    @Inject
    EntityStorageService entityStorageService;
    @Inject
    AttributeTypeService attributeTypeService;

    @AfterEach
    public void tearDown() {
        orderTestUtils.clearCheckouterAPI();
    }

    @Test
    void getOrderItem_withoutHistory() {
        var order = orderTestUtils.createOrder();
        var orderItemParams = orderTestUtils.mockOrderItem(order, Map.of());

        Collection<OrderItem> items = getOrderItems(order);
        Assertions.assertNotNull(items);
        Assertions.assertEquals(1, items.size());

        OrderItem orderItem = items.iterator().next();
        assertEqualsOrderItem(orderItemParams, orderItem);
    }

    @Test
    void getOrderItem_withHistory() {
        long checkouterId = Randoms.longValue();
        var order = orderTestUtils.createOrder();
        var orderItemHistory = orderTestUtils.createOrderItemHistory(
                order, Maps.of(OrderItemHistory.CHECKOUTER_ID, checkouterId));
        var orderItemParams = orderTestUtils.mockOrderItem(
                order, Maps.of(OrderItem.CHECKOUTER_ID, checkouterId));
        fixOrderItemParams(orderItemParams, orderItemHistory, Map.of());

        Collection<OrderItem> items = getOrderItems(order);
        Assertions.assertNotNull(items);
        Assertions.assertEquals(1, items.size());

        OrderItem orderItem = items.iterator().next();
        assertEqualsOrderItem(orderItemParams, orderItem);
    }

    @Test
    void getOrderItem_withHistoryLost() {
        long checkouterId1 = Randoms.longValue();
        var order = orderTestUtils.createOrder();
        var orderItemHistory1 = orderTestUtils.createOrderItemHistory(
                order, Map.of(OrderItemHistory.CHECKOUTER_ID, checkouterId1));
        var orderItemParams1 = orderTestUtils.mockOrderItem(
                order, Map.of(OrderItem.CHECKOUTER_ID, checkouterId1));
        fixOrderItemParams(orderItemParams1, orderItemHistory1, Map.of());

        long checkouterId2 = Randoms.longValue();
        var orderItemHistory2 = orderTestUtils.createOrderItemHistory(
                order, Map.of(OrderItemHistory.CHECKOUTER_ID, checkouterId2));
        var orderItemParams2FromHistory = orderTestUtils.mockCheckouterOrderHistory_lostItem(
                order, Map.of(OrderItem.CHECKOUTER_ID, checkouterId2));
        fixOrderItemParams(orderItemParams2FromHistory, orderItemHistory2, Map.of(OrderItem.COUNT, 0));

        Collection<OrderItem> items = getOrderItems(order);
        Assertions.assertNotNull(items);
        Assertions.assertEquals(2, items.size());

        OrderItem orderItem1 = items.stream()
                .filter(i -> Objects.equals(i.getCheckouterId(), checkouterId1))
                .findFirst().orElse(null);
        OrderItem orderItem2 = items.stream()
                .filter(i -> Objects.equals(i.getCheckouterId(), checkouterId2))
                .findFirst().orElse(null);
        assertEqualsOrderItem(orderItemParams1, orderItem1);
        assertEqualsOrderItem(orderItemParams2FromHistory, orderItem2);
    }

    /**
     * В OrderItemEntityStorageStrategy заполняются указанные поля из обекта истории в ЕО (OrderItemHistory)
     * делаем так же
     * <p>
     * Для айтемов, которых не стало в Заказе нужно вернуть COUNT = 0, поэтому
     * в OrderItemEntityStorageStrategy явно указывается COUNT = 0
     * В истории Чекаутера COUNT != 0, т.к. данные берутся из состояния Заказа, где айтем еще не удален
     * делаем так же
     */
    private void fixOrderItemParams(Map<String, Object> source,
                                    OrderItemHistory orderItemHistory,
                                    Map<String, Object> mergeParams) {
        source.put(OrderItem.INITIAL_COUNT, orderItemHistory.getInitialCount());
        source.put(OrderItem.MISSING_COUNT, orderItemHistory.getMissingCount());
        source.put(OrderItem.COUNT_CHANGE_REASON, orderItemHistory.getCountChangeReason());

        // Для COUNT
        mergeParams.forEach(source::put);
    }

    private List<OrderItem> getOrderItems(Order order) {
        return entityStorageService.list(
                Query.of(OrderItem.FQN)
                        .withFilters(Filters.eq(OrderItem.PARENT, order)));
    }

    private void assertEqualsOrderItem(Map<String, Object> expectedParams, OrderItem orderItem) {
        Assertions.assertNotNull(orderItem);

        expectedParams.forEach((attributeCode, value) -> {
            Attribute attribute = orderItem.getMetaclass().getAttribute(attributeCode);
            Object expected = attributeTypeService.wrap(attribute, value);
            // хардкодим, т.к. внутри OrderItemImpl для pictureUrl делается так же
            if (OrderItem.PICTURE_URL.equals(attributeCode)) {
                expected = CheckouterViewsHelper.toOrderItemImage(expected.toString());
            }
            Object actual = orderItem.getAttribute(attribute);
            Assertions.assertEquals(expected, actual, "Атрибут " + attributeCode + " не верный");
        });
    }

    static Stream<Arguments> data() {
        return Stream.of(
                arguments(3, 5, 2, 5, 2),
                arguments(2, 5, 2, 5, 2),
                arguments(3, 6, 2, 5, 2),
                arguments(3, 5, 1, 5, 2),
                arguments(3, 6, 1, 5, 2),
                arguments(3, null, 2, 5, 2),
                arguments(3, null, null, 5, 2),
                arguments(3, 5, null, 5, 2));
    }

    @ParameterizedTest
    @MethodSource("data")
    void getOrderItem_withIncorrectCountOfItem(Integer count, Integer dbInitialCount, Integer dbMissingCount,
                                               Integer historyInitialCount, Integer historyMissingCount) {
        long checkouterId1 = Randoms.longValue();
        var order = orderTestUtils.createOrder();
        var events = getEvents(checkouterId1, historyInitialCount, historyMissingCount);
        orderTestUtils.mockGetOrderHistory(order, events);

        var orderItemHistory1 = orderTestUtils.createEmptyOrderItemHistory(
                order,
                Maps.of(
                        OrderItemHistory.CHECKOUTER_ID, checkouterId1,
                        OrderItemHistory.INITIAL_COUNT, dbInitialCount,
                        OrderItemHistory.MISSING_COUNT, dbMissingCount));
        var orderItemParams1 = orderTestUtils.mockOrderItem(
                order,
                Map.of(
                        OrderItem.CHECKOUTER_ID, checkouterId1,
                        OrderItem.COUNT, count));
        fixOrderItemParams(orderItemParams1, orderItemHistory1, Map.of());

        Collection<OrderItem> items = getOrderItems(order);
        Assertions.assertNotNull(items);

        OrderItem orderItem1 = items.stream()
                .filter(i -> Objects.equals(i.getCheckouterId(), checkouterId1))
                .findFirst().orElse(null);
        assertEqualsOrderItem(
                Map.of(
                        OrderItemHistory.INITIAL_COUNT, historyInitialCount,
                        OrderItemHistory.MISSING_COUNT, historyMissingCount,
                        OrderItem.COUNT, count),
                orderItem1);
    }

    ArrayList<OrderHistoryEvent> getEvents(Long checkouterId, Integer initialCount, Integer missingCount) {
        var events = new ArrayList<OrderHistoryEvent>();
        events.add(getEvent(checkouterId, initialCount, null, HistoryEventType.NEW_ORDER));
        events.add(getEvent(checkouterId, initialCount - missingCount, initialCount, HistoryEventType.ITEMS_UPDATED));
        return events;
    }

    OrderHistoryEvent getEvent(Long checkouterId, Integer afterCount, Integer beforeCount, HistoryEventType type) {
        OrderHistoryEvent event = new OrderHistoryEvent();
        event.setType(type);
        event.setOrderAfter(getOrder(checkouterId, afterCount));
        if (null != beforeCount) {
            event.setOrderBefore(getOrder(checkouterId, beforeCount));
        }
        return event;
    }

    ru.yandex.market.checkout.checkouter.order.Order getOrder(Long checkouterId, Integer count) {
        var item = new ru.yandex.market.checkout.checkouter.order.OrderItem();
        item.setId(checkouterId);
        item.setCount(count);

        var items = new ArrayList<ru.yandex.market.checkout.checkouter.order.OrderItem>();
        items.add(item);

        var order = new ru.yandex.market.checkout.checkouter.order.Order();
        order.setItems(items);

        return order;
    }
}
