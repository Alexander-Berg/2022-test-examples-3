package ru.yandex.market.logistics.cs.checkouter;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;

import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.logistics.cs.AbstractTest;
import ru.yandex.market.logistics.cs.config.FeaturePropertiesConfiguration;
import ru.yandex.market.logistics.cs.dbqueue.servicecounter.ServiceCounterBatchPayload;
import ru.yandex.market.logistics.cs.dbqueue.servicecounter.ServiceCounterBatchProducerImpl;
import ru.yandex.market.logistics.cs.dbqueue.servicecounter.ServiceDeliveryDescriptor;
import ru.yandex.market.logistics.cs.dbqueue.servicecounter.ServiceDeliveryDescriptorExtractor;
import ru.yandex.market.logistics.cs.dbqueue.servicecounter.ServiceDeliveryDescriptorExtractorImpl;
import ru.yandex.market.logistics.cs.domain.entity.Event;
import ru.yandex.market.logistics.cs.domain.entity.Event.EventBuilder;
import ru.yandex.market.logistics.cs.domain.entity.EventKeyTypePair;
import ru.yandex.market.logistics.cs.domain.enumeration.EventType;
import ru.yandex.market.logistics.cs.logbroker.checkouter.CheckouterEventProcessor;
import ru.yandex.market.logistics.cs.logbroker.checkouter.OrderHistoryEventParser;
import ru.yandex.market.logistics.cs.repository.EventRepository;
import ru.yandex.market.logistics.cs.service.PartnerCargoTypeFactorService;
import ru.yandex.market.logistics.cs.service.ServiceCounterBatchPayloadService;
import ru.yandex.market.logistics.cs.service.impl.EventServiceImpl;
import ru.yandex.market.logistics.cs.service.impl.ServiceCounterBatchPayloadServiceImpl;
import ru.yandex.market.logistics.cs.util.DateTimeUtils;
import ru.yandex.market.logistics.cs.util.OrderEventUtils;
import ru.yandex.market.logistics.cs.util.TestDtoFactory;
import ru.yandex.market.logistics.cs.util.TestDtoFactory.IndexedOrderHistoryEvent;
import ru.yandex.market.logistics.lom.model.dto.CombinatorRoute.DeliveryService;
import ru.yandex.market.logistics.lom.model.dto.CombinatorRoute.ProcessedItem;
import ru.yandex.money.common.dbqueue.api.EnqueueParams;
import ru.yandex.money.common.dbqueue.api.QueueProducer;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.checkout.checkouter.event.HistoryEventType.NEW_ORDER;
import static ru.yandex.market.checkout.checkouter.event.HistoryEventType.ORDER_STATUS_UPDATED;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PLACING;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.RESERVED;

@DisplayName("Обработчик событий чекаутера")
class CheckouterEventProcessorTest extends AbstractTest {

    @Mock
    private EventRepository eventRepository;
    @Mock
    private QueueProducer<ServiceCounterBatchPayload> serviceCounterEventProducer;
    @Mock
    private PartnerCargoTypeFactorService partnerCargoTypeFactorService;

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private CheckouterEventProcessor processor;

    @BeforeEach
    public void setUp() {
        MeterRegistry registry = new SimpleMeterRegistry();
        EventServiceImpl eventService = new EventServiceImpl(eventRepository);
        ServiceCounterBatchProducerImpl serviceCounterBatchProducer =
            new ServiceCounterBatchProducerImpl(serviceCounterEventProducer);
        ServiceDeliveryDescriptorExtractor serviceDeliveryDescriptorExtractor =
            new ServiceDeliveryDescriptorExtractorImpl(partnerCargoTypeFactorService);
        FeaturePropertiesConfiguration featurePropertiesConfiguration = new FeaturePropertiesConfiguration();
        ServiceCounterBatchPayloadService serviceCounterBatchPayloadService =
            new ServiceCounterBatchPayloadServiceImpl(
                featurePropertiesConfiguration,
                serviceDeliveryDescriptorExtractor,
                serviceCounterBatchProducer
            );
        OrderHistoryEventParser parser =
            new OrderHistoryEventParser(mapper, true, true, true, true);
        processor = new CheckouterEventProcessor(
            parser,
            eventService,
            serviceCounterBatchPayloadService,
            registry
        );
    }

    @Test
    void smokeTest() {
        assertDoesNotThrow(() -> processor.process(List.of()));
    }

    private static Stream<Arguments> validEventTypeMappingProvider() {
        return Stream.concat(
            Arrays.stream(OrderStatus.values())
                .flatMap(orderBeforeStatus -> Arrays.stream(OrderStatus.values())
                    .map(orderAfterStatus -> Pair.of(orderBeforeStatus, orderAfterStatus)))
                .map(pair -> Arguments.of(NEW_ORDER, pair.getKey(), pair.getValue(), EventType.NEW)),
            Arrays.stream(OrderStatus.values())
                .filter(status -> status != OrderStatus.CANCELLED)
                .map(orderBeforeStatus -> Arguments.of(
                    ORDER_STATUS_UPDATED,
                    orderBeforeStatus,
                    OrderStatus.CANCELLED,
                    EventType.CANCELLED
                ))
        );
    }

    @DisplayName(value = "Валидация преобразования события с набором состояний "
        + "(historyEvent.type, orderBefore.status, orderAfter.status) в батч с ожидаемым типом")
    @ParameterizedTest(name = "({0}, {1}, {2}) -> {3}")
    @MethodSource("validEventTypeMappingProvider")
    void singleValidHistoryEventConsumption(
        HistoryEventType historyEventType,
        OrderStatus orderBeforeStatus,
        OrderStatus orderAfterStatus,
        EventType expectedEventType
    ) {
        IndexedOrderHistoryEvent testData =
            TestDtoFactory.randomHistoryEventWithRoute(historyEventType, orderBeforeStatus, orderAfterStatus);

        boolean cancellationEvent = OrderEventUtils.isOrderCancelled(testData.getEvent());
        if (cancellationEvent) {
            // remove route from original order to ensure we do expect it to present
            OrderEventUtils.getParcel(testData.getEvent()).orElseThrow().setRoute(null);
            assertTrue(OrderEventUtils.getOrderRoute(testData.getEvent()).isEmpty());
        }

        LocalDateTime maxTime = testData.getServiceList().stream()
            .map(DeliveryService::getStartTime)
            .map(DateTimeUtils::toLocalDateTime)
            .max(Comparator.naturalOrder())
            .orElseThrow();

        String eventKey = OrderEventUtils.toEventKey(testData.getEvent()).orElseThrow();
        EventBuilder expectedEventBuilder = Event.builder()
            .key(eventKey)
            .type(expectedEventType)
            .eventTimestamp(DateTimeUtils.toLocalDateTime(testData.getEvent().getTranDate()))
            .maxServiceTime(maxTime)
            .route(testData.getRouteNode())
            .externalId(testData.getEvent().getId());

        Event expectedEvent = expectedEventBuilder.build();

        long savedEventId = TestDtoFactory.nextId();
        Event savedEvent = expectedEventBuilder.id(savedEventId).build();
        int expectedCount = cancellationEvent ? -1 : 1;

        List<ServiceDeliveryDescriptor> expectedServiceCounters = testData.getServiceList()
            .stream()
            .map(service -> new ServiceDeliveryDescriptor(
                service.getId(),
                DateTimeUtils.toLocalDate(service.getStartTime()),
                expectedCount * service.getItems().stream()
                    .mapToInt(ProcessedItem::getQuantity)
                    .sum()
            ))
            .collect(toList());

        ServiceCounterBatchPayload expectedBatch = new ServiceCounterBatchPayload(
            savedEventId,
            expectedEvent.getType(),
            expectedServiceCounters,
            expectedCount,
            false
        );

        assertTrue(expectedServiceCounters.size() >= 2);

        Long orderId = OrderEventUtils.getOrderId(testData.getEvent());
        if (cancellationEvent) {
            when(eventRepository.findLast(orderId.toString()))
                .thenReturn(Optional.of(expectedEvent));
        }

        when(eventRepository.existentKeys(any()))
            .thenReturn(Collections.emptySet());
        when(eventRepository.saveAll(singletonList(expectedEvent)))
            .thenReturn(singletonList(savedEvent));
        when(serviceCounterEventProducer.enqueue(EnqueueParams.create(expectedBatch)))
            .thenReturn(0L);
        when(partnerCargoTypeFactorService.getMaxFactor(anyLong(), any())).thenReturn(1.0);

        processor.process(singletonList(testData.getEvent()));

        if (cancellationEvent) {
            verify(eventRepository, times(1)).findLast(orderId.toString());
        }
        verify(eventRepository, times(1)).saveAll(singletonList(expectedEvent));
        verify(serviceCounterEventProducer, times(1)).enqueue(any());
        verifyNoMoreInteractions(eventRepository);
    }

    @Test
    @DisplayName(value = "Корректное проставление dummy-флага")
    void correctDummyEventProcessing() {
        IndexedOrderHistoryEvent testData =
            TestDtoFactory.randomHistoryEventWithRoute(NEW_ORDER, PLACING, RESERVED);

        testData.getEvent().getOrderAfter().setFake(true);

        LocalDateTime maxTime = testData.getServiceList().stream()
            .map(DeliveryService::getStartTime)
            .map(DateTimeUtils::toLocalDateTime)
            .max(Comparator.naturalOrder())
            .orElseThrow();

        String eventKey = OrderEventUtils.toEventKey(testData.getEvent()).orElseThrow();
        EventBuilder expectedEventBuilder = Event.builder()
            .key(eventKey)
            .type(EventType.NEW)
            .eventTimestamp(DateTimeUtils.toLocalDateTime(testData.getEvent().getTranDate()))
            .maxServiceTime(maxTime)
            .route(testData.getRouteNode())
            .dummy(true)
            .externalId(testData.getEvent().getId());

        Event expectedEvent = expectedEventBuilder.build();

        long savedEventId = TestDtoFactory.nextId();
        Event savedEvent = expectedEventBuilder.id(savedEventId).build();
        int expectedCount = 1;

        List<ServiceDeliveryDescriptor> expectedServiceCounters = testData.getServiceList()
            .stream()
            .map(service -> new ServiceDeliveryDescriptor(
                service.getId(),
                DateTimeUtils.toLocalDate(service.getStartTime()),
                expectedCount * service.getItems().stream()
                    .mapToInt(ProcessedItem::getQuantity)
                    .sum()
            ))
            .collect(toList());

        ServiceCounterBatchPayload expectedBatch = new ServiceCounterBatchPayload(
            savedEventId,
            savedEvent.getType(),
            expectedServiceCounters,
            expectedCount,
            true
        );

        assertTrue(expectedServiceCounters.size() >= 2);

        when(eventRepository.existentKeys(any()))
            .thenReturn(Collections.emptySet());
        when(eventRepository.saveAll(singletonList(expectedEvent)))
            .thenReturn(singletonList(savedEvent));
        when(serviceCounterEventProducer.enqueue(EnqueueParams.create(expectedBatch)))
            .thenReturn(0L);
        when(partnerCargoTypeFactorService.getMaxFactor(anyLong(), any())).thenReturn(1.0);

        processor.process(singletonList(testData.getEvent()));

        verify(eventRepository, times(1)).saveAll(singletonList(expectedEvent));
        verify(serviceCounterEventProducer, times(1)).enqueue(any());
        verifyNoMoreInteractions(eventRepository);
    }

    @Test
    @DisplayName("При поглощении дублирующихся событий новые евенты не должны быть сохранены")
    void repeatingEventsFiltering() {
        IndexedOrderHistoryEvent testData =
            TestDtoFactory.randomHistoryEventWithRoute(NEW_ORDER, PLACING, OrderStatus.RESERVED);

        String eventKey = OrderEventUtils.toEventKey(testData.getEvent()).orElseThrow();
        when(eventRepository.existentKeys(any()))
            .thenReturn(singleton(new EventKeyTypePair(eventKey, EventType.NEW)));

        processor.process(singletonList(testData.getEvent()));
        verify(eventRepository, times(1)).saveAll(emptyList());
        verifyNoMoreInteractions(eventRepository, serviceCounterEventProducer);
    }

    @DisplayName("Игнорируются заказы с белого маркета")
    @ParameterizedTest
    @EnumSource(Color.class)
    void ignoreWhiteColor(Color color) {
        IndexedOrderHistoryEvent indexedOrderHistoryEvent = TestDtoFactory.randomHistoryEventWithRoute(
            NEW_ORDER,
            PLACING,
            RESERVED
        );
        indexedOrderHistoryEvent.getEvent().getOrderAfter().setRgb(color);

        processor.process(List.of(indexedOrderHistoryEvent.getEvent()));
        if (Color.WHITE != color) {
            verify(eventRepository).saveAll(anyList());
        } else {
            verifyNoMoreInteractions(eventRepository);
        }
    }
}
