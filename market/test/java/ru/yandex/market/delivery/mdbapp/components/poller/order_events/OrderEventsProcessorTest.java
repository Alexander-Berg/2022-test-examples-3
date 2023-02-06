package ru.yandex.market.delivery.mdbapp.components.poller.order_events;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.delivery.mdbapp.AbstractTest;
import ru.yandex.market.delivery.mdbapp.components.failover.OrderEventFailoverableService;
import ru.yandex.market.delivery.mdbapp.components.logging.BackLogOrdersTskvLogger;
import ru.yandex.market.delivery.mdbapp.components.logging.EventFlowParametersHolder;
import ru.yandex.market.delivery.mdbapp.components.logging.OrderEventAction;
import ru.yandex.market.delivery.mdbapp.components.service.OrderEventsService;
import ru.yandex.market.delivery.mdbapp.components.service.checkouter.client.CheckouterServiceClient;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.OrderEventFailoverEntity;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.OrderEventsFailoverCounter;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.type.FailCauseType;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.type.FailoverEntityType;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.type.TicketCreationStatus;
import ru.yandex.market.delivery.mdbapp.components.storage.repository.OrderEventsFailoverRepository;
import ru.yandex.market.delivery.mdbapp.configuration.FailoverConfiguration;
import ru.yandex.market.delivery.mdbapp.configuration.FeatureProperties;
import ru.yandex.market.delivery.mdbapp.exception.FrozenServiceException;
import ru.yandex.market.delivery.mdbapp.integration.gateway.OrderEventsGatewayWrapper;
import ru.yandex.market.delivery.mdbapp.integration.service.OrderEventsErrorMonitoringHandler;
import ru.yandex.market.delivery.mdbapp.util.OrderEventUtils;
import ru.yandex.market.logistics.logging.backlog.layout.logback.BackLogLayout;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
public class OrderEventsProcessorTest extends AbstractTest {

    private static final String MESSAGE = "Накрылся медным тазом этот ваш процессинг";

    private final OrderEventsGatewayWrapper gateway = mock(OrderEventsGatewayWrapper.class);
    private final CheckouterServiceClient checkouterServiceClient = mock(CheckouterServiceClient.class);
    private final ApplicationEventPublisher appEventPublisher = mock(ApplicationEventPublisher.class);
    private final OrderEventsFailoverRepository failoverRepository = mock(OrderEventsFailoverRepository.class);
    private final FailoverConfiguration failoverConfiguration = new FailoverConfiguration();
    private final EventFlowParametersHolder flowParametersHolder = new EventFlowParametersHolder();
    private final BackLogOrdersTskvLogger backLogOrdersTskvLogger = new BackLogOrdersTskvLogger(flowParametersHolder);
    private final OrderEventsErrorMonitoringHandler orderEventsErrorMonitoringHandler =
        mock(OrderEventsErrorMonitoringHandler.class);
    private final TestableClock clock = new TestableClock();
    private final OrderEventsService eventsService = mock(OrderEventsService.class);
    private final FeatureProperties featureProperties = new FeatureProperties();

    private final OrderEventsProcessor processor;

    private final ConsoleAppender<ILoggingEvent> logAppender = mock(ConsoleAppender.class);

    {
        //three attempts
        failoverConfiguration.setRetryIntervals(Arrays.asList(0, 0, 0));
        failoverConfiguration.setMaxRetryCount(3);
        processor = new OrderEventsProcessorFlowParametersDecorator(
            new OrderEventsProcessorImpl(
                gateway,
                checkouterServiceClient,
                new OrderEventFailoverableService(
                    failoverConfiguration,
                    failoverRepository,
                    flowParametersHolder,
                    eventsService,
                    featureProperties,
                    clock
                ),
                appEventPublisher,
                backLogOrdersTskvLogger,
                orderEventsErrorMonitoringHandler,
                clock,
                flowParametersHolder
            ),
            flowParametersHolder
        );
    }

    @BeforeEach
    public void setUp() {
        Logger logger = (((Logger) LoggerFactory.getLogger("BACK_LOG_TSKV")));
        LayoutWrappingEncoder<ILoggingEvent> layoutWrappingEncoder = new LayoutWrappingEncoder<>();
        layoutWrappingEncoder.setLayout(new BackLogLayout());
        logAppender.setEncoder(layoutWrappingEncoder);
        logger.addAppender(logAppender);
        logger.setLevel(Level.INFO);

        failoverRepository.deleteAll();
        featureProperties.setUseSavedEventsInFailover(false);
        clock.setFixed(Instant.parse("2022-06-01T00:01:00.200Z"), ZoneOffset.UTC);
        flowParametersHolder.setFailoverFlowActive(false);
    }

    @AfterEach
    public void tearDown() {
        verify(failoverRepository).deleteAll();
        verifyNoMoreInteractions(
            failoverRepository,
            checkouterServiceClient,
            appEventPublisher,
            orderEventsErrorMonitoringHandler
        );
    }

    @Test
    public void processEventIsntAppeared() {
        OrderHistoryEvent event = createEvent();

        softly.assertThat(processor.processEvent(event)).isTrue();

        verify(gateway).processEvent(event);
        verify(failoverRepository).markFixedEvent(anyLong());
        verifyBacklog(2, false, true);
    }

    @Test
    public void processEventFailedOneTime() {
        OrderHistoryEvent event = createEvent();
        doThrow(new RuntimeException(MESSAGE)).when(gateway).processEvent(event);
        when(failoverRepository.findByEventId(event.getId())).thenReturn(Optional.empty());

        softly.assertThat(processor.processEvent(event)).isFalse();

        verify(failoverRepository, times(2)).findByEventId(event.getId());
        ArgumentCaptor<OrderEventsFailoverCounter> captor = ArgumentCaptor.forClass(OrderEventsFailoverCounter.class);
        verify(failoverRepository).save(captor.capture());
        softly.assertThat(captor.getValue().getTicketCreationStatus()).isEqualTo(TicketCreationStatus.NOT_CREATED);
        softly.assertThat(captor.getValue().getEventPayload()).isNull();
        verifyBacklog(3, false, false);
    }

    @Test
    public void processEventFailedOneTimeWithSavePayload() {
        featureProperties.setUseSavedEventsInFailover(true);
        OrderHistoryEvent event = createEvent();
        doThrow(new RuntimeException(MESSAGE)).when(gateway).processEvent(event);
        when(failoverRepository.findByEventId(event.getId())).thenReturn(Optional.empty());
        when(eventsService.convertEvent(event)).thenReturn("event payload");

        softly.assertThat(processor.processEvent(event)).isFalse();

        verify(failoverRepository, times(2)).findByEventId(event.getId());
        ArgumentCaptor<OrderEventsFailoverCounter> captor = ArgumentCaptor.forClass(OrderEventsFailoverCounter.class);
        verify(failoverRepository).save(captor.capture());
        softly.assertThat(captor.getValue().getTicketCreationStatus()).isEqualTo(TicketCreationStatus.NOT_CREATED);
        softly.assertThat(captor.getValue().getEventPayload()).isEqualTo("event payload");
    }

    @Test
    public void processEventFailedSecondTime() {
        OrderHistoryEvent event = createEvent();
        OrderEventsFailoverCounter counter = new OrderEventsFailoverCounter(
            event.getId(),
            null,
            "",
            null,
            TicketCreationStatus.NOT_CREATED,
            FailCauseType.UNKNOWN
        );
        int attemptCount = counter.getAttemptCount();
        doThrow(new RuntimeException(MESSAGE)).when(gateway).processEvent(event);
        when(failoverRepository.findByEventId(event.getId())).thenReturn(Optional.of(counter));

        flowParametersHolder.setFailoverFlowActive(true);
        softly.assertThat(processor.processEvent(event)).isFalse();

        verify(failoverRepository, times(2)).findByEventId(event.getId());
        verify(failoverRepository).save(counter);
        verifyBacklog(3, true, false);

        softly.assertThat(counter.getAttemptCount()).isEqualTo(attemptCount + 1);
    }

    @Test
    public void processEventFailedOneTimeThenWorkedSuccessful() {
        OrderHistoryEvent event = createEvent();
        OrderEventsFailoverCounter counter = new OrderEventsFailoverCounter(
            event.getId(),
            null,
            "",
            null,
            TicketCreationStatus.NOT_CREATED,
            FailCauseType.UNKNOWN
        );
        int attemptCount = counter.getAttemptCount();

        doThrow(new RuntimeException(MESSAGE)).doNothing().when(gateway).processEvent(event);
        when(failoverRepository.findByEventId(event.getId())).thenReturn(Optional.of(counter));

        softly.assertThat(processor.processEvent(event)).isFalse();

        verify(failoverRepository, times(2)).findByEventId(event.getId());
        verify(failoverRepository).save(counter);

        softly.assertThat(counter.getAttemptCount()).isEqualTo(attemptCount + 1);
        verify(failoverRepository, never()).markFixedEvent(event.getId());

        softly.assertThat(processor.processEvent(event)).isTrue();

        verify(failoverRepository).markFixedEvent(event.getId());
        softly.assertThat(counter.getAttemptCount()).isEqualTo(attemptCount + 1);
    }

    @Test
    public void processEventFailedThreeTimes() {
        OrderHistoryEvent event = createEvent();
        OrderEventsFailoverCounter counter = new OrderEventsFailoverCounter(
            event.getId(),
            null,
            "",
            null,
            TicketCreationStatus.NOT_CREATED,
            FailCauseType.UNKNOWN
        );
        //second attempt
        counter.attemptFailed("");
        //third attempt
        counter.attemptFailed("");

        when(failoverRepository.findByEventId(event.getId())).thenReturn(Optional.of(counter));
        doThrow(new RuntimeException(MESSAGE)).when(gateway).processEvent(event);

        softly.assertThat(processor.processEvent(event)).isFalse();

        verify(failoverRepository, times(2)).findByEventId(event.getId());
        verify(failoverRepository).save(counter);
        verify(checkouterServiceClient)
            .updateDeliveryServiceStatusForSingleParcelOrder(eq(event.getOrderAfter().getId()), any());
        verify(appEventPublisher).publishEvent(any());
        ArgumentCaptor<Exception> orderEventsErrorMonitoringHandlerCaptor = ArgumentCaptor.forClass(Exception.class);
        verify(orderEventsErrorMonitoringHandler).handle(orderEventsErrorMonitoringHandlerCaptor.capture());
        softly.assertThat(orderEventsErrorMonitoringHandlerCaptor.getValue().getMessage()).isEqualTo(MESSAGE);
    }

    @Test
    public void processEventFrozen() {
        OrderHistoryEvent event = createEvent();
        doThrow(new FrozenServiceException("171")).when(gateway).processEvent(event);
        when(failoverRepository.findByEventId(event.getId())).thenReturn(Optional.empty());

        softly.assertThat(processor.processEvent(event)).isFalse();

        verify(failoverRepository, times(2)).findByEventId(event.getId());
        ArgumentCaptor<OrderEventsFailoverCounter> captor = ArgumentCaptor.forClass(OrderEventsFailoverCounter.class);
        verify(failoverRepository).save(captor.capture());
        softly.assertThat(captor.getValue().getTicketCreationStatus()).isEqualTo(TicketCreationStatus.NOT_CREATED);
        softly.assertThat(captor.getValue().getOrderEventFailoverEntities().size())
            .isEqualTo(1);
        OrderEventFailoverEntity orderEventFailoverEntity =
            captor.getValue().getOrderEventFailoverEntities().stream().findFirst().orElseThrow();
        softly.assertThat(orderEventFailoverEntity.getEntityId()).isEqualTo("171");
        softly.assertThat(orderEventFailoverEntity.getEntityType()).isEqualTo(FailoverEntityType.PARTNER);
    }

    @Test
    public void processEventFrozenRetry() {
        OrderHistoryEvent event = createEvent();
        doThrow(new FrozenServiceException("171")).when(gateway).processEvent(event);
        when(failoverRepository.findByEventId(event.getId())).thenReturn(Optional.of(
            new OrderEventsFailoverCounter(
                event.getId(),
                OrderEventUtils.getOrderId(event),
                "aaa",
                TicketCreationStatus.N_A,
                FailCauseType.FROZEN_SERVICE
            )
                .setQueued(false)
                .setFixed(false)
                .setSkippedByCancellation(false)
                .setFailureOrderEventAction(OrderEventAction.FF_ORDER_CREATE)
        ));

        softly.assertThat(processor.processEvent(event)).isFalse();

        verify(failoverRepository, times(2)).findByEventId(event.getId());
        ArgumentCaptor<OrderEventsFailoverCounter> captor = ArgumentCaptor.forClass(OrderEventsFailoverCounter.class);
        verify(failoverRepository).save(captor.capture());
        softly.assertThat(captor.getValue().getTicketCreationStatus()).isEqualTo(TicketCreationStatus.NOT_CREATED);
        softly.assertThat(captor.getValue().getLastFailCauseType()).isEqualTo(FailCauseType.FROZEN_SERVICE);
        softly.assertThat(captor.getValue().getOrderEventFailoverEntities().size())
            .isEqualTo(1);
        OrderEventFailoverEntity orderEventFailoverEntity =
            captor.getValue().getOrderEventFailoverEntities().stream().findFirst().orElseThrow();
        softly.assertThat(orderEventFailoverEntity.getEntityId()).isEqualTo("171");
        softly.assertThat(orderEventFailoverEntity.getEntityType()).isEqualTo(FailoverEntityType.PARTNER);
    }

    @Test
    public void processEventFrozenRetryToUnknown() {
        OrderHistoryEvent event = createEvent();
        doThrow(new RuntimeException(MESSAGE)).when(gateway).processEvent(event);
        when(failoverRepository.findByEventId(event.getId())).thenReturn(Optional.of(
            new OrderEventsFailoverCounter(
                event.getId(),
                OrderEventUtils.getOrderId(event),
                "aaa",
                TicketCreationStatus.N_A,
                FailCauseType.FROZEN_SERVICE
            )
                .setQueued(false)
                .setFixed(false)
                .setSkippedByCancellation(false)
                .setFailureOrderEventAction(OrderEventAction.FF_ORDER_CREATE)
                .addEntity(new OrderEventFailoverEntity().setEntityType(FailoverEntityType.PARTNER).setEntityId("171"))
        ));

        softly.assertThat(processor.processEvent(event)).isFalse();

        verify(failoverRepository, times(2)).findByEventId(event.getId());
        ArgumentCaptor<OrderEventsFailoverCounter> captor = ArgumentCaptor.forClass(OrderEventsFailoverCounter.class);
        verify(failoverRepository).save(captor.capture());
        softly.assertThat(captor.getValue().getTicketCreationStatus()).isEqualTo(TicketCreationStatus.NOT_CREATED);
        softly.assertThat(captor.getValue().getLastFailCauseType()).isEqualTo(FailCauseType.UNKNOWN);
        softly.assertThat(captor.getValue().getOrderEventFailoverEntities().size())
            .isEqualTo(0);
    }

    @Test
    public void processEventUnknownRetryToFrozen() {
        OrderHistoryEvent event = createEvent();
        doThrow(new FrozenServiceException("171")).when(gateway).processEvent(event);
        when(failoverRepository.findByEventId(event.getId())).thenReturn(Optional.of(
            new OrderEventsFailoverCounter(
                event.getId(),
                OrderEventUtils.getOrderId(event),
                "aaa",
                TicketCreationStatus.N_A,
                FailCauseType.UNKNOWN
            )
                .setQueued(false)
                .setFixed(false)
                .setSkippedByCancellation(false)
                .setFailureOrderEventAction(OrderEventAction.FF_ORDER_CREATE)
        ));

        softly.assertThat(processor.processEvent(event)).isFalse();

        verify(failoverRepository, times(2)).findByEventId(event.getId());
        ArgumentCaptor<OrderEventsFailoverCounter> captor = ArgumentCaptor.forClass(OrderEventsFailoverCounter.class);
        verify(failoverRepository).save(captor.capture());
        softly.assertThat(captor.getValue().getTicketCreationStatus()).isEqualTo(TicketCreationStatus.NOT_CREATED);
        softly.assertThat(captor.getValue().getLastFailCauseType()).isEqualTo(FailCauseType.FROZEN_SERVICE);
        softly.assertThat(captor.getValue().getOrderEventFailoverEntities().size())
            .isEqualTo(1);
        OrderEventFailoverEntity orderEventFailoverEntity =
            captor.getValue().getOrderEventFailoverEntities().stream().findFirst().orElseThrow();
        softly.assertThat(orderEventFailoverEntity.getEntityId()).isEqualTo("171");
        softly.assertThat(orderEventFailoverEntity.getEntityType()).isEqualTo(FailoverEntityType.PARTNER);
    }

    @Nonnull
    private OrderHistoryEvent createEvent() {
        OrderHistoryEvent event = new OrderHistoryEvent();
        event.setId(1L);
        event.setTranDate(Date.from(Instant.parse("2022-06-01T00:00:10.100Z")));
        Order orderAfter = new Order();
        orderAfter.setId(2L);
        event.setOrderAfter(orderAfter);
        return event;
    }

    private void verifyBacklog(int times, boolean isFailover, boolean isSuccess) {
        String isFailoverString = isFailover ? "true" : "false";
        String isSuccessString = isSuccess ? "true" : "false";

        ArgumentCaptor<Appender<ILoggingEvent>> argumentCaptor = ArgumentCaptor.forClass(Appender.class);
        verify(logAppender, times(times)).doAppend((ILoggingEvent) argumentCaptor.capture());

        LoggingEvent loggingEvent = (LoggingEvent) argumentCaptor.getAllValues().get(1);

        softly.assertThat(loggingEvent.getMessage()).isEqualTo("Processing event");

        Map<String, String> mdc = loggingEvent.getMDCPropertyMap();
        softly.assertThat(mdc.get("back_log:entities_types")).isEqualTo("eventId,orderId");
        softly.assertThat(mdc.get("back_log:entities_ids")).isEqualTo("eventId:1,orderId:2");
        softly.assertThat(mdc.get("back_log:extra_keys")).isEqualTo(
            "tranTime,isFailover,processingTimeMs,flowAction,startTime,eventType,isSuccess"
        );
        softly.assertThat(mdc.get("back_log:extra_values")).isEqualTo(
            "1654041610," + isFailoverString + ",0,UNKNOWN,1654041660,UNKNOWN," + isSuccessString
        );
    }
}
