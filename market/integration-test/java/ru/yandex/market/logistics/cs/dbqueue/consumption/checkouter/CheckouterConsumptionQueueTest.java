package ru.yandex.market.logistics.cs.dbqueue.consumption.checkouter;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.logistics.cs.AbstractIntegrationTest;
import ru.yandex.market.logistics.cs.logbroker.checkouter.CheckouterEventProcessor;
import ru.yandex.market.logistics.cs.logbroker.checkouter.OrderEventConsumer;
import ru.yandex.market.logistics.cs.repository.QueueTaskRepository;
import ru.yandex.market.logistics.cs.util.OrderEventUtils;
import ru.yandex.market.logistics.cs.util.TestDtoFactory;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.checkout.checkouter.event.HistoryEventType.NEW_ORDER;
import static ru.yandex.market.checkout.checkouter.event.HistoryEventType.ORDER_STATUS_UPDATED;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PLACING;

@DisplayName("Очередь logbroker_checkouter_consumption")
@MockBean(CheckouterEventProcessor.class)
public class CheckouterConsumptionQueueTest extends AbstractIntegrationTest {

    @Autowired
    private QueueTaskRepository queueTaskRepository;

    @Autowired
    private OrderEventConsumer orderEventConsumer;

    @Autowired
    private CheckouterEventProcessor checkouterEventsProcessor;

    @Captor
    private ArgumentCaptor<List<OrderHistoryEvent>> eventsCaptor;

    @DisplayName("Все отфильтрованные события, пришедшие из чекаутера, обрабатываются CheckouterEventProcessor")
    @ParameterizedTest(name = "{0}, {1}, {2}")
    @MethodSource("validEventTypeMappingProvider")
    void testProducer(
        HistoryEventType historyEventType,
        OrderStatus orderBeforeStatus,
        OrderStatus orderAfterStatus
    ) {
        List<OrderHistoryEvent> eventsToProcess = getEvents(historyEventType, orderBeforeStatus, orderAfterStatus);

        List<OrderHistoryEvent> allEvents = new ArrayList<>(eventsToProcess);
        allEvents.addAll(getEventsToFilter());
        Collections.shuffle(allEvents);

        orderEventConsumer.consume(allEvents);

        List<OrderHistoryEvent> expectedEvents = allEvents.stream()
            .filter(event -> OrderEventUtils.isOrderCreated(event) || OrderEventUtils.isOrderCancelled(event))
            .collect(Collectors.toList());

        await().atMost(Duration.ofMinutes(1))
            .until(queueTaskRepository::count, equalTo(0L));


        verify(checkouterEventsProcessor).process(eventsCaptor.capture());
        List<OrderHistoryEvent> actualEvents = eventsCaptor.getValue();

        softly.assertThat(actualEvents.size()).isEqualTo(expectedEvents.size());
        IntStream.range(0, actualEvents.size()).forEach(i ->
            softly.assertThat(actualEvents.get(i).toString()).isEqualTo(expectedEvents.get(i).toString())
        );
    }

    private static Stream<Arguments> validEventTypeMappingProvider() {
        return Stream.concat(
            Arrays.stream(OrderStatus.values())
                .flatMap(orderBeforeStatus -> Arrays.stream(OrderStatus.values())
                    .map(orderAfterStatus -> Pair.of(orderBeforeStatus, orderAfterStatus)))
                .map(pair -> Arguments.of(NEW_ORDER, pair.getKey(), pair.getValue())),
            Arrays.stream(OrderStatus.values())
                .filter(status -> status != OrderStatus.CANCELLED)
                .map(orderBeforeStatus -> Arguments.of(
                    ORDER_STATUS_UPDATED,
                    orderBeforeStatus,
                    OrderStatus.CANCELLED
                ))
        );
    }

    private List<OrderHistoryEvent> getEvents(
        HistoryEventType historyEventType,
        OrderStatus orderBeforeStatus,
        OrderStatus orderAfterStatus
    ) {
        return IntStream.range(0, 10).mapToObj(i ->
            TestDtoFactory.randomHistoryEventWithRoute(historyEventType, orderBeforeStatus, orderAfterStatus).getEvent()
        ).collect(Collectors.toList());
    }

    private List<OrderHistoryEvent> getEventsToFilter() {
        return IntStream.range(0, 5).mapToObj(i ->
            TestDtoFactory.randomHistoryEventWithRoute(
                ORDER_STATUS_UPDATED,
                PLACING,
                OrderStatus.getByIdOrUnknown(i)
            ).getEvent()
        ).collect(Collectors.toList());
    }
}
