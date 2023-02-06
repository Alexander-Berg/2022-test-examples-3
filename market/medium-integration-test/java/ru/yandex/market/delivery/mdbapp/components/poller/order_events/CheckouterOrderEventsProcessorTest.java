package ru.yandex.market.delivery.mdbapp.components.poller.order_events;

import java.time.Clock;
import java.time.Instant;
import java.util.Date;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.web.client.HttpServerErrorException;

import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.delivery.mdbapp.AbstractMediumContextualTest;
import ru.yandex.market.delivery.mdbapp.components.failover.OrderEventFailoverableService;
import ru.yandex.market.delivery.mdbapp.components.logging.BackLogOrdersTskvLogger;
import ru.yandex.market.delivery.mdbapp.components.logging.EventFlowParametersHolder;
import ru.yandex.market.delivery.mdbapp.components.service.OrderEventsFailoverService;
import ru.yandex.market.delivery.mdbapp.components.service.OrderEventsService;
import ru.yandex.market.delivery.mdbapp.components.service.checkouter.client.CheckouterServiceClient;
import ru.yandex.market.delivery.mdbapp.exception.FrozenServiceException;
import ru.yandex.market.delivery.mdbapp.integration.gateway.OrderEventsGateway;
import ru.yandex.market.delivery.mdbapp.integration.gateway.OrderEventsGatewayWrapper;
import ru.yandex.market.delivery.mdbapp.integration.service.OrderEventsErrorMonitoringHandler;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@DisplayName("Тест обработки внутренней очереди событий чекаутера")
class CheckouterOrderEventsProcessorTest extends AbstractMediumContextualTest {

    @Autowired
    private OrderEventFailoverableService failoverService;
    @Autowired
    private OrderEventsFailoverService failoverRetryService;
    @Autowired
    private OrderEventsService orderEventsService;

    @Autowired
    private TransactionOperations transactionOperations;

    @Autowired
    private OrderEventsGateway gateway;
    @Autowired
    private OrderEventsGatewayWrapper gatewayWrapper;
    @Autowired
    private CheckouterServiceClient checkouterServiceClient;
    @Autowired
    private ApplicationEventPublisher appEventPublisher;
    @Autowired
    private BackLogOrdersTskvLogger backLogOrdersTskvLogger;
    @Autowired
    private OrderEventsErrorMonitoringHandler orderEventsErrorMonitoringHandler;
    @Autowired
    private Clock clock;
    @Autowired
    private EventFlowParametersHolder eventFlowParametersHolder;

    @AfterEach
    public void tearDown() {
        verifyNoMoreInteractions(gateway);
    }

    @Test
    @DisplayName("Успешная обработка")
    @DatabaseSetup("/components/poller/order_events_queue/before/setup.xml")
    @ExpectedDatabase(
        value = "/components/poller/order_events_queue/after/success.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void success() {
        CheckouterOrderEventsProcessor processor = getProcessor();

        Integer numProcessed = transactionOperations.execute(
            tc -> processor.processEventsBatch(1, 2, 10)
        );

        softly.assertThat(numProcessed).isEqualTo(1);

        ArgumentCaptor<OrderHistoryEvent> captor = ArgumentCaptor.forClass(OrderHistoryEvent.class);
        verify(gateway).processEvent(captor.capture());
        softly.assertThat(captor.getValue().toString()).isEqualTo(createEvent().toString());
    }

    @Test
    @DisplayName("Ошибка при обработке. Нет сообщения")
    @DatabaseSetup("/components/poller/order_events_queue/before/setup.xml")
    @ExpectedDatabase(
        value = "/components/poller/order_events_queue/after/fail.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void failNoMessage() {
        CheckouterOrderEventsProcessor processor = getProcessor();

        doThrow(new RuntimeException()).when(gateway).processEvent(any(OrderHistoryEvent.class));

        Integer numProcessed = transactionOperations.execute(
            tc -> processor.processEventsBatch(1, 2, 10)
        );

        softly.assertThat(numProcessed).isEqualTo(1);

        ArgumentCaptor<OrderHistoryEvent> captor = ArgumentCaptor.forClass(OrderHistoryEvent.class);
        verify(gateway).processEvent(captor.capture());
        softly.assertThat(captor.getValue().toString()).isEqualTo(createEvent().toString());
    }

    @Test
    @DisplayName("Ошибка при обработке. Неизвестный тип")
    @DatabaseSetup("/components/poller/order_events_queue/before/setup.xml")
    @ExpectedDatabase(
        value = "/components/poller/order_events_queue/after/fail.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void failUnknown() {
        testFail(new RuntimeException("err"));
    }

    @Test
    @DisplayName("Ошибка при обработке. InternalServerError")
    @DatabaseSetup("/components/poller/order_events_queue/before/setup.xml")
    @ExpectedDatabase(
        value = "/components/poller/order_events_queue/after/fail_internal_error.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void failInternalServerError() {
        testFail(HttpServerErrorException.create(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "error",
            HttpHeaders.EMPTY,
            null,
            null
        ));
    }

    @Test
    @DisplayName("Ошибка при обработке. FrozenServiceException")
    @DatabaseSetup("/components/poller/order_events_queue/before/setup.xml")
    @ExpectedDatabase(
        value = "/components/poller/order_events_queue/after/fail_frozen_exception.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void failFrozenServiceException() {
        testFail(new FrozenServiceException("171"));
    }

    private void testFail(Exception e) {
        CheckouterOrderEventsProcessor processor = getProcessor();

        doThrow(e).when(gateway).processEvent(any(OrderHistoryEvent.class));

        Integer numProcessed = transactionOperations.execute(
            tc -> processor.processEventsBatch(1, 2, 10)
        );

        softly.assertThat(numProcessed).isEqualTo(1);

        ArgumentCaptor<OrderHistoryEvent> captor = ArgumentCaptor.forClass(OrderHistoryEvent.class);
        verify(gateway).processEvent(captor.capture());
        softly.assertThat(captor.getValue().toString()).isEqualTo(createEvent().toString());
    }

    @Nonnull
    private OrderHistoryEvent createEvent() {
        OrderHistoryEvent event = new OrderHistoryEvent();
        event.setId(1L);
        event.setType(HistoryEventType.NEW_ORDER);
        Order orderAfter = new Order();
        orderAfter.setId(123L);
        Delivery delivery = new Delivery();
        DeliveryDates deliveryDates = new DeliveryDates();
        deliveryDates.setFromDate(Date.from(Instant.parse("2022-06-16T21:00:00Z")));
        delivery.setDeliveryDates(deliveryDates);
        orderAfter.setDelivery(delivery);
        event.setOrderAfter(orderAfter);
        return event;
    }

    @Nonnull
    private CheckouterOrderEventsProcessor getProcessor() {
        OrderEventsProcessor eventsProcessor = new OrderEventsProcessorImpl(
            gatewayWrapper,
            checkouterServiceClient,
            failoverService,
            appEventPublisher,
            backLogOrdersTskvLogger,
            orderEventsErrorMonitoringHandler,
            clock,
            eventFlowParametersHolder
        );
        return new CheckouterOrderEventsProcessorImpl(
            failoverService,
            failoverRetryService,
            eventsProcessor,
            orderEventsService
        );
    }
}
