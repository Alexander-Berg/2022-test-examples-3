package ru.yandex.market.billing.checkout.handler;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.common.test.db.DataSetType;
import ru.yandex.market.common.test.db.DbUnitDataSet;

class EventHandlerServiceImplTest extends FunctionalTest {

    @Autowired
    private EventHandlerServiceImpl eventHandlerService;

    @Test
    @DbUnitDataSet(
            after = "db/EventExceptionHandlerServiceImplTest.saveExceptionEvent.after.xml",
            type = DataSetType.XML
    )
    void testSaveExceptionEvent() {
        eventHandlerService.handleExceptionOrderEvent(
                makeEvent(1L, 1L),
                new Exception()
        );
    }

    @Test
    @DbUnitDataSet(
            before = "db/EventExceptionHandlerServiceImplTest.saveIgnoredEvent.before.xml",
            after = "db/EventExceptionHandlerServiceImplTest.saveIgnoredEvent.after.xml",
            type = DataSetType.XML
    )
    void testSaveIgnoredEvent() {
        eventHandlerService.handleIgnoredOrderEvent(
                makeEvent(2L, 1L)
        );
    }

    @Test
    @DbUnitDataSet(before = "db/EventExceptionHandlerServiceImplTest.testGetIgnoredOrders.csv")
    void testGetIgnoredOrders() {
        Set<Long> actual = eventHandlerService.getIgnoredOrders();
        Assertions.assertThat(actual)
                .hasSameElementsAs(List.of(1L, 2L));
    }

    @Test
    void testSaveExceptionUniqueOrderLimitExceeded() {
        EventHandlerServiceImpl serviceSpy = Mockito.spy(eventHandlerService);
        Mockito.when(serviceSpy.getIgnoredOrders())
                .thenReturn(LongStream.range(1, 2501).boxed().collect(Collectors.toSet()));

        Assertions.assertThatThrownBy(() ->
                serviceSpy.handleExceptionOrderEvent(
                        makeEvent(2501L, 2501L),
                        new IllegalArgumentException()
                )
        ).isExactlyInstanceOf(EventExceptionHandlerException.class)
                .hasMessage("Ignored orders is exceeded")
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testSaveExceptionOrderLimitNotExceeded() {
        LongStream.range(1L, 101L)
                .forEach(id -> eventHandlerService.handleExceptionOrderEvent(
                        makeEvent(id, 1L),
                        new Exception()
                ));

        Assertions.assertThatCode(() ->
                eventHandlerService.handleExceptionOrderEvent(
                        makeEvent(101L, 1L),
                        new Exception()
                )).doesNotThrowAnyException();
    }

    @Test
    @DbUnitDataSet(
            before = "db/EventExceptionHandlerServiceImplTest.testGetIgnoredEvents.xml",
            type = DataSetType.XML
    )
    void testGetIgnoredEvents() {
        List<Long> eventIds = eventHandlerService.getIgnoredEvents(1L)
                .stream()
                .map(OrderHistoryEvent::getId)
                .collect(Collectors.toList());

        Assertions.assertThat(eventIds)
                .containsExactly(1L, 3L);
    }

    @Test
    @DbUnitDataSet(
            before = "db/EventExceptionHandlerServiceImplTest.testDeleteIgnoredEvents.before.xml",
            after = "db/EventExceptionHandlerServiceImplTest.testDeleteIgnoredEvents.after.xml",
            type = DataSetType.XML
    )
    void testDeleteIgnoredEvents() {
        eventHandlerService.deleteIgnoredEvents(1L);
    }

    private OrderHistoryEvent makeEvent(long eventId, long orderId) {
        OrderHistoryEvent event = new OrderHistoryEvent();

        event.setId(eventId);
        Order order = new Order();
        order.setId(orderId);
        event.setOrderAfter(order);

        return event;
    }
}
