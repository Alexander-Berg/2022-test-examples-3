package ru.yandex.market.loyalty.admin.test;

import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.client.CheckouterClient;
import ru.yandex.market.checkout.checkouter.client.OrderFilter;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvents;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.loyalty.admin.tms.checkouter.CheckouterEventRestProcessor;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.utils.CheckouterUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * @author dinyat
 * 08/09/2017
 */
public abstract class MarketLoyaltyAdminCheckouterEventProcessorTest extends MarketLoyaltyAdminMockedDbTest {
    @Autowired
    protected CheckouterEventRestProcessor processor;
    @Autowired
    protected CheckouterClient checkouterClient;

    @Before
    public void setUpConfigurationProperties() {
        configurationService.set(ConfigurationService.CHECKOUTER_LOGBROKER_ENABLED, false);
    }

    protected void processEvent(OrderStatus orderStatus, HistoryEventType historyEventType, long orderId) {
        processEvent(orderStatus, historyEventType, Color.BLUE, orderId);
    }

    protected void processEvent(
            OrderStatus orderStatus, HistoryEventType historyEventType, Color rgb, long orderId
    ) {
        processEvent(
                orderStatus,
                Arrays.stream(OrderSubstatus.values())
                        .filter(ss -> ss.getStatus() == orderStatus)
                        .findFirst()
                        .orElse(null),
                historyEventType,
                rgb,
                orderId
        );
    }

    protected void processEvent(
            OrderStatus orderStatus, OrderSubstatus substatus, HistoryEventType historyEventType, Color rgb,
            long orderId
    ) {
        processEvent(orderStatus, substatus, historyEventType, rgb, orderId, null, null);
    }

    protected void processEvent(
            OrderStatus orderStatus, OrderSubstatus substatus, HistoryEventType historyEventType, Color rgb,
            long orderId, String multiOrderId, Integer count
    ) {
        Order order = CheckouterUtils.defaultOrder(orderStatus, substatus)
                .setOrderId(orderId)
                .setRgb(rgb)
                .addItems(Collections.emptyList())
                .setMultiOrderId(multiOrderId)
                .setOrdersCount(count)
                .build();
        processEvent(order, historyEventType);
    }

    protected void processEvent(OrderStatus orderStatus, HistoryEventType historyEventType, Order order) {
        processEvent(o -> o.setStatus(orderStatus), historyEventType, order);
    }

    protected void processEvent(Consumer<Order> modifier, HistoryEventType historyEventType, Order order) {
        Order orderBefore = order.clone();
        modifier.accept(order);
        OrderHistoryEvent event = CheckouterUtils.getEvent(orderBefore, order, historyEventType, clock);
        processEvent(event);
    }

    protected void processEvent(Order order, HistoryEventType historyEventType) {
        OrderHistoryEvent event = CheckouterUtils.getEvent(order, historyEventType, clock);
        processEvent(event);
    }

    protected void processEvent(OrderHistoryEvent event) {
        when(checkouterClient.orderHistoryEvents().getOrderHistoryEvents(anyLong(), anyInt(), anySet(), eq(false),
                anySet(), any(OrderFilter.class)))
                .thenReturn(new OrderHistoryEvents(Collections.singleton(event)));
        processor.processCheckouterEvents(0, 4, 0);
    }
}
