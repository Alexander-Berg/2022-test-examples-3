package ru.yandex.market.crm.platform.reducers;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.crm.platform.YieldMock;
import ru.yandex.market.crm.platform.common.Uids;
import ru.yandex.market.crm.platform.commons.UidType;
import ru.yandex.market.crm.platform.models.OrderMonitoringEvent;
import ru.yandex.market.crm.platform.models.OrderMonitorings;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.crm.platform.models.OrderMonitoringEvent.State.OFF;
import static ru.yandex.market.crm.platform.models.OrderMonitoringEvent.State.ON;

public class OrderMonitoringsReducerTest {

    private final OrderMonitorings stored = OrderMonitorings.newBuilder()
            .setOrderId(1)
            .addMonitoring("DELAYED_DELIVERY_START")
            .addMonitoring("DELAYED_PACKAGING")
            .addUids(Uids.create(UidType.PUID, 123))
            .build();

    private OrderMonitoringsReducer reducer;
    private YieldMock collector;


    @Before
    public void before() {
        this.reducer = new OrderMonitoringsReducer();
        this.collector = new YieldMock();
    }

    /**
     * Предыдущий факт отсутствует.
     * Сохранено новое состояние.
     */
    @Test
    public void noOldStateTest() {
        // используемый готовый, чтобы не городить новый
        OrderMonitorings newState = this.stored;
        reducer.reduce(emptyList(), singleton(newState), collector);
        assertEquals(newState, collector.getAdded(OrderMonitoringsReducer.FACT_ID).iterator().next());
        assertEquals(2, getEvents(ON).size());
    }

    /**
     * Пустой список мониторингов по заказу.
     * Старое состояние удаляется, новое не сохраняется.
     */
    @Test
    public void oldStateIsRemovedTest() {
        OrderMonitorings newState = OrderMonitorings.newBuilder().setOrderId(stored.getOrderId()).build();
        reducer.reduce(singletonList(stored), singleton(newState), collector);

        assertEquals(stored, collector.getRemoved(OrderMonitoringsReducer.FACT_ID).iterator().next());
        assertTrue(collector.getAdded(OrderMonitoringsReducer.FACT_ID).isEmpty());
    }

    /**
     * Новое состояния содержит мониторинги.
     * Старое обновляется.
     */
    @Test
    public void newStateIsSavedTest() {
        OrderMonitorings newState = OrderMonitorings.newBuilder()
                .setOrderId(stored.getOrderId())
                .addAllUids(stored.getUidsList())
                .addMonitoring("DELAYED_DELIVERY_START")
                .addMonitoring("NEW_MONITORING")
                .build();

        reducer.reduce(singletonList(stored), singleton(newState), collector);
        assertEquals(newState, collector.getAdded(OrderMonitoringsReducer.FACT_ID).iterator().next());
    }

    /**
     * Пустой список мониторингов по заказу.
     * Создаются события погашения мониторингов для всех типов в старом состоянии.
     */
    @Test
    public void offEventsAreCreatedTest() {
        OrderMonitorings newState = OrderMonitorings.newBuilder()
                .setOrderId(stored.getOrderId())
                .addAllUids(stored.getUidsList())
                .build();

        reducer.reduce(singletonList(stored), singleton(newState), collector);

        List<OrderMonitoringEvent> off = getEvents(OFF);
        assertEquals(2, off.size());
        assertTrue(off.contains(makeEvent("DELAYED_DELIVERY_START", OFF)));
        assertTrue(off.contains(makeEvent("DELAYED_PACKAGING", OFF)));
    }

    /**
     * Создаются факты загорания и погашения мониторингов по заказу.
     */
    @Test
    public void offAndOnEventsAreCreatedTest() {
        OrderMonitorings newState = OrderMonitorings.newBuilder()
                .setOrderId(stored.getOrderId())
                .addAllUids(stored.getUidsList())
                .addMonitoring("DELAYED_DELIVERY_START") // содержится в старом состоянии, событие не создается
                .addMonitoring("NEW_MONITORING")
                .build();

        reducer.reduce(singletonList(stored), singleton(newState), collector);
        List<OrderMonitoringEvent> off = getEvents(OFF);
        List<OrderMonitoringEvent> on = getEvents(ON);

        assertEquals(1, off.size());
        assertEquals(makeEvent("DELAYED_PACKAGING", OFF), off.get(0));

        assertEquals(1, on.size());
        assertEquals(makeEvent("NEW_MONITORING", ON), on.get(0));
    }

    private OrderMonitoringEvent makeEvent(String monitoring, OrderMonitoringEvent.State state) {
        return OrderMonitoringEvent.newBuilder()
                .setOrderId(stored.getOrderId())
                .addAllUids(stored.getUidsList())
                .setMonitoring(monitoring)
                .setState(state)
                .build();
    }

    private List<OrderMonitoringEvent> getEvents(OrderMonitoringEvent.State state) {
        return collector.getAdded(OrderMonitoringsReducer.MONITORING_EVENT_FACT_ID)
                .stream()
                .map(OrderMonitoringEvent.class::cast)
                .filter(m -> m.getState() == state)
                .collect(Collectors.toList());
    }
}