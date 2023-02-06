package ru.yandex.market.logistics.cs.dbqueue.servicecounter;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.logistics.cs.AbstractIntegrationTest;
import ru.yandex.market.logistics.cs.dbqueue.common.AccountingTaskLifecycleListener;
import ru.yandex.market.logistics.cs.dbqueue.common.AccountingTaskLifecycleListener.LifecycleEvent;
import ru.yandex.market.logistics.cs.dbqueue.common.AccountingTaskLifecycleListener.LifecycleEventType;
import ru.yandex.market.logistics.cs.dbqueue.common.AccountingTaskLifecycleListener.QueueCoordinates;
import ru.yandex.market.logistics.cs.domain.entity.Event;
import ru.yandex.market.logistics.cs.domain.entity.ServiceCounter;
import ru.yandex.market.logistics.cs.domain.enumeration.EventType;
import ru.yandex.market.logistics.cs.logbroker.checkouter.CheckouterEventProcessor;
import ru.yandex.market.logistics.cs.logbroker.checkouter.OrderHistoryEventParser;
import ru.yandex.market.logistics.cs.logbroker.checkouter.SimpleCombinatorRoute;
import ru.yandex.market.logistics.cs.repository.EventRepository;
import ru.yandex.market.logistics.cs.repository.QueueTaskRepository;
import ru.yandex.market.logistics.cs.repository.ServiceCounterRepository;
import ru.yandex.market.logistics.cs.service.ServiceCounterBatchPayloadService;
import ru.yandex.market.logistics.cs.service.impl.EventServiceImpl;
import ru.yandex.market.logistics.cs.util.DateTimeUtils;
import ru.yandex.market.logistics.cs.util.OrderEventUtils;
import ru.yandex.market.logistics.cs.util.RouteBuilder;
import ru.yandex.market.logistics.cs.util.TestDtoFactory;
import ru.yandex.market.logistics.cs.util.TestDtoFactory.IndexedOrderHistoryEvent;
import ru.yandex.market.logistics.cs.util.TestDtoFactory.SingleServiceOrderHistoryEvent;
import ru.yandex.money.common.dbqueue.api.EnqueueParams;
import ru.yandex.money.common.dbqueue.api.QueueProducer;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.event.HistoryEventType.NEW_ORDER;
import static ru.yandex.market.checkout.checkouter.event.HistoryEventType.ORDER_STATUS_UPDATED;
import static ru.yandex.market.logistics.cs.config.dbqueue.ServiceCounterBatchQueueConfig.QUEUE_LOCATION;
import static ru.yandex.market.logistics.cs.dbqueue.common.SingleQueueShardRouter.MASTER;

@DisplayName("Интеграционный тест на консьюмера очереди сервисных счётчиков")
class ServiceCounterBatchQueueIT extends AbstractIntegrationTest {
    private static final QueueCoordinates QUEUE_COORDINATES = new QueueCoordinates(MASTER, QUEUE_LOCATION);

    @Autowired
    private MeterRegistry registry;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private QueueProducer<ServiceCounterBatchPayload> serviceCounterBatchPayloadQueueProducer;
    @Autowired
    private ServiceCounterRepository serviceCounterRepository;
    @Autowired
    private ServiceCounterBatchPayloadService serviceCounterBatchPayloadService;
    @Autowired
    private QueueTaskRepository queueTaskRepository;
    @Autowired
    private AccountingTaskLifecycleListener taskLifecycleListener;

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private CheckouterEventProcessor processor;

    @BeforeEach
    public void setUp() {
        EventServiceImpl counterService = new EventServiceImpl(eventRepository);
        OrderHistoryEventParser parser = new OrderHistoryEventParser(mapper, true, true, true, true);
        processor = new CheckouterEventProcessor(
            parser,
            counterService,
            serviceCounterBatchPayloadService,
            registry
        );
        registry.forEachMeter(registry::remove);
        taskLifecycleListener.reset();
    }

    @AfterEach
    public void checkEventIsProcessed() {
        eventRepository.findAll().forEach(event -> assertTrue(event.isProcessed()));
    }

    @Test
    @DisplayName("Тест на задымление")
    void smokeTest() {
        assertDoesNotThrow(() -> processor.process(List.of()));
    }

    @Test
    @SneakyThrows
    @SuppressWarnings({"unchecked", "rawtypes"})
    @DisplayName("Корректная обработка при невалидном пейлоаде")
    void incorrectRouteConsumption() {
        serviceCounterBatchPayloadQueueProducer.enqueue((EnqueueParams) EnqueueParams.create("junk"));
        await().atMost(Duration.ofMinutes(1))
            .until(() -> containsTaskLifecycleEventOfType(LifecycleEventType.CRASHED));
    }

    private static Stream<Arguments> validEventTypeMappingProvider() {
        return Stream.of(
            Arguments.of(NEW_ORDER, null, EventType.NEW),
            Arguments.of(ORDER_STATUS_UPDATED, OrderStatus.CANCELLED, EventType.CANCELLED)
        );
    }

    @SneakyThrows
    @Test
    @DisplayName("Поддержка дубликатов событий чекаутера")
    void duplicatedEventsSupported() {
        IndexedOrderHistoryEvent testData =
            TestDtoFactory.randomHistoryEventWithRoute(NEW_ORDER, OrderStatus.PLACING, OrderStatus.RESERVED);

        processor.process(List.of(testData.getEvent()));

        await().atMost(Duration.ofMinutes(1))
            .until(queueTaskRepository::count, equalTo(0L));

        long serviceCounterNum = serviceCounterRepository.count();

        processor.process(List.of(testData.getEvent()));

        await().atMost(Duration.ofMinutes(1))
            .until(queueTaskRepository::count, equalTo(0L));

        assertEquals(1, eventRepository.count());
        assertEquals(serviceCounterNum, serviceCounterRepository.count());
    }

    @ParameterizedTest
    @MethodSource("validEventTypeMappingProvider")
    void singleServiceConsumption(
        HistoryEventType historyEventType,
        OrderStatus orderStatus,
        EventType expectedEventType
    ) {
        SingleServiceOrderHistoryEvent testData =
            TestDtoFactory.singleServiceWithParcelOrder(historyEventType, orderStatus);

        processor.process(List.of(testData.getEvent()));

        List<Event> savedEvents = eventRepository.findAll();
        assertEquals(1, savedEvents.size());
        Event savedEvent = savedEvents.get(0);

        Event expectedEvent = Event.builder()
            .key(OrderEventUtils.toEventKey(testData.getEvent()).orElseThrow())
            .type(expectedEventType)
            .eventTimestamp(DateTimeUtils.toLocalDateTime(testData.getEvent().getTranDate()))
            .maxServiceTime(DateTimeUtils.toLocalDateTime(testData.getService().getStartTime()))
            .route(testData.getRouteNode())
            .externalId(testData.getEvent().getId())
            .build();

        assertEntitiesEqual(expectedEvent, savedEvent);

        await().atMost(Duration.ofMinutes(1))
            .until(serviceCounterRepository::count, greaterThanOrEqualTo(1L));

        List<ServiceCounter> savedCounters = serviceCounterRepository.findAll();
        assertEquals(1, savedCounters.size());
        ServiceCounter savedCounter = savedCounters.get(0);

        ServiceCounter expectedCounter = ServiceCounter.builder()
            .serviceId(testData.getService().getId())
            .day(DateTimeUtils.toLocalDate(testData.getService().getStartTime()))
            .eventId(savedEvent.getId())
            .serviceVersion(0L)
            .itemCount(0)
            .realItemCount(0)
            .orderCount(expectedEventType == EventType.NEW ? 1 : -1)
            .build();

        assertEntitiesEqual(expectedCounter, savedCounter);

        await().atMost(Duration.ofMinutes(1))
            .until(() -> containsTaskLifecycleEventOfType(LifecycleEventType.FINISHED));
    }

    @Test
    @DatabaseSetup(value = "/repository/checkouter/before/before_cancelling_ddr_order.xml")
    @ExpectedDatabase(
        value = "/repository/checkouter/after/after_cancelling_ddr_order.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void cancellingOrderAfterRecalculationTest() {
        LocalDateTime inFuture100days = LocalDateTime.parse("2170-01-01T00:00:00");
        LocalDateTime now = DateTimeUtils.nowUtc();
        int itemsQuantity = 14;
        Long orderId = 123123L;

        LocalDateTime eventTime1 = now.minus(3, ChronoUnit.HOURS);
        LocalDateTime eventTime2 = now.minus(2, ChronoUnit.HOURS);
        LocalDateTime eventTime3 = now.minus(1, ChronoUnit.HOURS);

        // Adding events in chronology event1 (NEW) -> event2 (CHANGE_ROUTE) -> event3 (CHANGE_ROUTE)
        prepareEventWithOneServiceForTest(
            EventType.NEW, 10L, orderId + "_111", eventTime1, inFuture100days, itemsQuantity, null);
        prepareEventWithOneServiceForTest(
            EventType.CHANGE_ROUTE, 20L, orderId + "_222", eventTime2, inFuture100days, itemsQuantity, 10L);
        Event event3 = prepareEventWithOneServiceForTest(
            EventType.CHANGE_ROUTE, 30L, orderId + "_333", eventTime3, inFuture100days, itemsQuantity, 20L);

        SingleServiceOrderHistoryEvent testData = TestDtoFactory.emptyRouteWithParcelOrder(
            ORDER_STATUS_UPDATED,
            OrderStatus.CANCELLED
        );

        OrderHistoryEvent event = testData.getEvent();
        Order order = event.getOrderAfter();

        // Hardcoding values to check correctly in database
        order.setId(orderId);
        order.getDelivery().getParcels().get(0).setId(444L);

        processor.process(List.of(event));

        await().atMost(Duration.ofMinutes(1))
            .until(() -> containsTaskLifecycleEventOfType(LifecycleEventType.FINISHED));

        // Checking route of cancelled event (4) is equal to last event (3)
        Optional<Event> event4Opt = eventRepository.findLast(orderId.toString());
        softly.assertThat(event4Opt).isPresent();

        Event event4 = event4Opt.get();
        softly.assertThat(event4.getType()).isEqualTo(EventType.CANCELLED);

        SimpleCombinatorRoute actualRoute =
            assertDoesNotThrow(() -> mapper.treeToValue(event4.getRoute(), SimpleCombinatorRoute.class));
        SimpleCombinatorRoute expectedRoute =
            assertDoesNotThrow(() -> mapper.treeToValue(event3.getRoute(), SimpleCombinatorRoute.class));
        softly.assertThat(actualRoute.getRoute()).isEqualTo(expectedRoute.getRoute());
    }

    @Test
    @DisplayName("Сервисный счётчик для dummy-батча сохраняенся с нулевыми счётчиками")
    void dummyBatchSavedWithZeroCounters() {
        SingleServiceOrderHistoryEvent testData =
            TestDtoFactory.singleServiceWithParcelOrder(NEW_ORDER, OrderStatus.RESERVED);
        testData.getEvent().getOrderAfter().setFake(true);

        processor.process(List.of(testData.getEvent()));

        List<Event> savedEvents = eventRepository.findAll();
        assertEquals(1, savedEvents.size());
        Event savedEvent = savedEvents.get(0);

        Event expectedEvent = Event.builder()
            .key(OrderEventUtils.toEventKey(testData.getEvent()).orElseThrow())
            .type(EventType.NEW)
            .eventTimestamp(DateTimeUtils.toLocalDateTime(testData.getEvent().getTranDate()))
            .maxServiceTime(DateTimeUtils.toLocalDateTime(testData.getService().getStartTime()))
            .route(testData.getRouteNode())
            .externalId(testData.getEvent().getId())
            .dummy(true)
            .build();

        assertEntitiesEqual(expectedEvent, savedEvent);

        await().atMost(Duration.ofMinutes(1))
            .until(serviceCounterRepository::count, greaterThanOrEqualTo(1L));

        List<ServiceCounter> savedCounters = serviceCounterRepository.findAll();
        assertEquals(1, savedCounters.size());
        ServiceCounter savedCounter = savedCounters.get(0);

        ServiceCounter expectedCounter = ServiceCounter.builder()
            .serviceId(testData.getService().getId())
            .day(DateTimeUtils.toLocalDate(testData.getService().getStartTime()))
            .eventId(savedEvent.getId())
            .serviceVersion(0L)
            .itemCount(0)
            .realItemCount(0)
            .orderCount(0)
            .build();

        assertEntitiesEqual(expectedCounter, savedCounter);

        await().atMost(Duration.ofMinutes(1))
            .until(() -> containsTaskLifecycleEventOfType(LifecycleEventType.FINISHED));
    }

    @Test
    @DisplayName("Обработка пейлоада с нулевыми сервисами")
    @DatabaseSetup("/repository/servicecounter/before/filter_zero_service.xml")
    @ExpectedDatabase(
        value = "/repository/servicecounter/after/filter_zero_service.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void zeroServiceIdInPayload() {
        ServiceCounterBatchPayload payload = ServiceCounterBatchPayload.builder()
            .eventId(1L)
            .orderCount(1)
            .dummy(false)
            .counters(List.of(
                new ServiceDeliveryDescriptor(1L, LocalDate.now(), 1),
                new ServiceDeliveryDescriptor(2L, LocalDate.now(), 1),
                new ServiceDeliveryDescriptor(0L, LocalDate.now(), 1)
            ))
            .build();

        serviceCounterBatchPayloadQueueProducer.enqueue(EnqueueParams.create(payload));
        await().atMost(Duration.ofMinutes(1))
            .until(() -> containsTaskLifecycleEventOfType(LifecycleEventType.FINISHED));
    }

    private boolean containsTaskLifecycleEventOfType(LifecycleEventType eventType) {
        LifecycleEvent event =
            taskLifecycleListener.getEvents(QUEUE_COORDINATES).poll();
        return Optional.ofNullable(event)
            .map(e -> e.getType().equals(eventType))
            .orElse(Boolean.FALSE);
    }

    private Event prepareEventWithOneServiceForTest(
        EventType eventType,
        Long serviceId,
        String key,
        LocalDateTime eventTime,
        LocalDateTime startTime,
        Integer itemsQuantity,
        Long previousServiceId
    ) {
        SimpleCombinatorRoute route = new SimpleCombinatorRoute(
            RouteBuilder.route(
                RouteBuilder.segment(0L)
                    .partnerId(12345L)
                    .services(
                        RouteBuilder.service(serviceId)
                            .start(startTime)
                            .items(RouteBuilder.item(0).quantity(itemsQuantity))
                    )
            ),
            null
        );
        Event e = eventRepository.save(
            Event.builder()
                .key(key)
                .route(mapper.valueToTree(route))
                .type(eventType)
                .eventTimestamp(eventTime)
                .maxServiceTime(eventTime)
                .processed(true)
                .build()
        );
        if (EventType.CHANGE_ROUTE == eventType && previousServiceId != null) {
            serviceCounterRepository.save(
                ServiceCounter.builder()
                    .serviceId(previousServiceId)
                    .serviceVersion(0L)
                    .eventId(e.getId())
                    .itemCount(-itemsQuantity)
                    .realItemCount(-itemsQuantity)
                    .orderCount(-1)
                    .day(startTime.toLocalDate())
                    .build()
            );
        }
        serviceCounterRepository.save(
            ServiceCounter.builder()
                .serviceId(serviceId)
                .serviceVersion(0L)
                .eventId(e.getId())
                .itemCount(itemsQuantity)
                .realItemCount(itemsQuantity)
                .orderCount(1)
                .day(startTime.toLocalDate())
                .build()
        );
        return e;
    }
}
