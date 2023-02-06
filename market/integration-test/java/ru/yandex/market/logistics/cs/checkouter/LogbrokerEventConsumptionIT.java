package ru.yandex.market.logistics.cs.checkouter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.ConsumerReadResponse;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageBatch;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageData;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.logistics.cs.AbstractIntegrationTest;
import ru.yandex.market.logistics.cs.checkouter.common.ConsumerTransportMock;
import ru.yandex.market.logistics.cs.domain.entity.Event;
import ru.yandex.market.logistics.cs.domain.entity.ServiceCounter;
import ru.yandex.market.logistics.cs.domain.enumeration.EventType;
import ru.yandex.market.logistics.cs.repository.EventRepository;
import ru.yandex.market.logistics.cs.repository.ServiceCounterRepository;
import ru.yandex.market.logistics.cs.util.DateTimeUtils;
import ru.yandex.market.logistics.cs.util.LogbrokerMessageFactory;
import ru.yandex.market.logistics.cs.util.OrderEventUtils;
import ru.yandex.market.logistics.cs.util.TestDtoFactory;
import ru.yandex.market.logistics.cs.util.TestDtoFactory.IndexedOrderHistoryEvent;
import ru.yandex.market.logistics.lom.model.dto.CombinatorRoute.DeliveryService;
import ru.yandex.market.logistics.lom.model.dto.CombinatorRoute.ProcessedItem;

import static java.util.stream.Collectors.toMap;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.checkout.checkouter.event.HistoryEventType.NEW_ORDER;

class LogbrokerEventConsumptionIT extends AbstractIntegrationTest {

    @Qualifier("checkouterAnnotationObjectMapper")
    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private ServiceCounterRepository serviceCounterRepository;

    @Autowired
    private ConsumerTransportMock consumerTransport;

    @BeforeEach
    public void setUp() {
        mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
        consumerTransport.start();
    }

    @AfterEach
    public void tearDown() {
        consumerTransport.stop();
        serviceCounterRepository.deleteAll();
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("data")
    @DisplayName("Процессинг 1 батча с евентом")
    @SneakyThrows
    @SuppressWarnings("unused")
    void singleEventBatchProcessing(String displayName, boolean withLogisticDate) {
        IndexedOrderHistoryEvent testData = TestDtoFactory.randomHistoryEventWithRoute(
            mapper,
            NEW_ORDER,
            OrderStatus.PLACING,
            OrderStatus.RESERVED,
            withLogisticDate
        );

        byte[] messagePayload = mapper.writeValueAsBytes(testData.getEvent());
        OrderHistoryEvent deserializedEvent = mapper.readValue(messagePayload, OrderHistoryEvent.class);

        consumerTransport.addEvent(new ConsumerReadResponse(List.of(
            new MessageBatch(
                "test",
                0,
                List.of(new MessageData(
                    messagePayload,
                    1,
                    LogbrokerMessageFactory.emptyMessageMeta()
                ))
            )), 0));

        await().atMost(Duration.ofMinutes(1))
            .until(eventRepository::count, greaterThanOrEqualTo(1L));

        List<Event> savedEvents = eventRepository.findAll();
        assertEquals(1, savedEvents.size());

        Event savedEvent = savedEvents.get(0);

        LocalDateTime maxTime = testData.getServiceList().stream()
            .map(DeliveryService::getStartTime)
            .map(DateTimeUtils::toLocalDateTime)
            .max(Comparator.naturalOrder())
            .orElseThrow();

        String eventKey = OrderEventUtils.toEventKey(deserializedEvent).orElseThrow();
        LocalDateTime expectedEventTimestamp = DateTimeUtils.toLocalDateTime(deserializedEvent.getTranDate());

        Event expectedEvent = Event.builder()
            .key(eventKey)
            .type(EventType.NEW)
            .eventTimestamp(expectedEventTimestamp)
            .maxServiceTime(maxTime)
            .route(testData.getRouteNode())
            .externalId(deserializedEvent.getId())
            .build();

        assertEntitiesEqual(expectedEvent, savedEvent);

        Map<Long, DeliveryService> expectedServiceCounters = testData.getServiceList()
            .stream()
            .collect(toMap(DeliveryService::getId, Function.identity(), (x, y) -> x));

        await().atMost(Duration.ofMinutes(1))
            .until(serviceCounterRepository::count, equalTo((long) expectedServiceCounters.size()));

        Map<Long, ServiceCounter> savedCounters = serviceCounterRepository.findAll().stream()
            .collect(toMap(ServiceCounter::getServiceId, Function.identity()));

        long savedEventId = savedEvent.getId();
        for (Entry<Long, ServiceCounter> e : savedCounters.entrySet()) {
            DeliveryService expectedDeliveryService = expectedServiceCounters.get(e.getKey());
            int itemCount = expectedDeliveryService.getItems().stream().mapToInt(ProcessedItem::getQuantity).sum();

            ServiceCounter expectedCounter = ServiceCounter.builder()
                .serviceId(expectedDeliveryService.getId())
                .day(withLogisticDate
                    ? DateTimeUtils.toLocalDate(expectedDeliveryService.getLogisticDate())
                    : DateTimeUtils.toLocalDate(expectedDeliveryService.getStartTime())
                )
                .eventId(savedEventId)
                .itemCount(itemCount)
                .realItemCount(itemCount)
                .orderCount(1)
                .serviceVersion(0L)
                .build();

            assertEntitiesEqual(expectedCounter, e.getValue());
        }
    }

    private static Stream<Arguments> data() {
        return Stream.of(
            Arguments.of("без логистических дат в сервисах маршрута", false),
            Arguments.of("с логистической датой в сервисах маршрута", true)
        );
    }
}
