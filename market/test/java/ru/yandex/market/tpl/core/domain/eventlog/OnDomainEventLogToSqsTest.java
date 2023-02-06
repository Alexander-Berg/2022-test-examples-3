package ru.yandex.market.tpl.core.domain.eventlog;

import java.time.Clock;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.jms.core.JmsTemplate;

import ru.yandex.market.logistics.les.CourierOrderEvent;
import ru.yandex.market.logistics.les.base.Event;
import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.base.EntityEvent;
import ru.yandex.market.tpl.core.domain.base.fsm.StatusTransition;
import ru.yandex.market.tpl.core.domain.base.fsm.StatusTransitionType;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.events.OrderFlowStatusChangedEvent;
import ru.yandex.market.tpl.core.domain.order.producer.OrderEventSqsLogProducer;
import ru.yandex.market.tpl.core.external.sqs.SqsQueueProperties;
import ru.yandex.market.tpl.core.service.sqs.SendEventToSqsService;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static ru.yandex.market.tpl.core.domain.eventlog.EventLogEntity.JSON_MAPPER;

@RequiredArgsConstructor
public class OnDomainEventLogToSqsTest extends TplAbstractTest {

    private final ConfigurationProviderAdapter configurationProviderAdapter;
    private final JmsTemplate jmsTemplate;
    private final Clock clock;

    private final OrderEventSqsLogProducer orderEventSqsLogProducer = Mockito.mock(OrderEventSqsLogProducer.class);
    private final SqsQueueProperties sqsQueueProperties = Mockito.mock(SqsQueueProperties.class);

    private final Order order = Mockito.mock(Order.class);

    private static final String EXTERNAL_ORDER_ID = "ex05";
    private static final Long ORDER_ID = 5L;
    private static final String EVENT_TYPE = "OrderCancelled";
    private static final String EVENT_TYPE_SNAKE_CASE = "ORDER_CANCELLED";
    private static final String QUEUE = "courier_out";
    private static final String SOURCE = "courier";

    private OnDomainEventLogToSqs onDomainEventLogToSqs;

    @BeforeEach
    void setUp() {
        SendEventToSqsService sendEventToSqsService = new SendEventToSqsService(
            configurationProviderAdapter,
            orderEventSqsLogProducer,
            sqsQueueProperties,
            jmsTemplate,
            clock
        );
        onDomainEventLogToSqs = new OnDomainEventLogToSqs(
            configurationProviderAdapter,
            sendEventToSqsService
        );
        Mockito.when(configurationProviderAdapter.isBooleanEnabled(ConfigurationProperties.SEND_EVENT_LOG_TO_SQS))
            .thenReturn(true);
        Mockito.when(configurationProviderAdapter.isBooleanEnabled(ConfigurationProperties.SEND_EVENT_LOG_TO_SQS_SYNCHRONOUSLY))
            .thenReturn(false);
        Mockito.when(sqsQueueProperties.getOutQueue())
            .thenReturn(QUEUE);
        Mockito.when(sqsQueueProperties.getSource())
            .thenReturn(SOURCE);
        Mockito.when(order.getExternalOrderId())
                .thenReturn(EXTERNAL_ORDER_ID);
    }

    @AfterEach
    void tearDown() {
        Mockito.reset(jmsTemplate);
        Mockito.reset(sqsQueueProperties);
        Mockito.reset(order);
    }

    @Test
    void sendSynchronouslyIfFlagEnabled() {
        Mockito.when(configurationProviderAdapter.isBooleanEnabled(
            ConfigurationProperties.SEND_EVENT_LOG_TO_SQS_SYNCHRONOUSLY
        ))
            .thenReturn(true);

        EntityEvent<Order> event = mockOrderEvent(ORDER_ID);

        onDomainEventLogToSqs.logOrderEvent(event);

        ArgumentCaptor<Event> argumentCaptor = ArgumentCaptor.forClass(Event.class);
        Mockito.verify(jmsTemplate).convertAndSend(eq(QUEUE), argumentCaptor.capture());

        Event argument = argumentCaptor.getValue();

        assertThat(argument.getEventId()).isEqualTo(event.getUuid().toString());

        assertThat(argument.getPayload()).isInstanceOf(CourierOrderEvent.class);
        assertThat(argument.getEventType()).isEqualTo(EVENT_TYPE_SNAKE_CASE);
        assertThat(argument.getSource()).isEqualTo(SOURCE);
        assertThat(argument.getEventId()).isEqualTo(event.getUuid().toString());

        CourierOrderEvent payload = (CourierOrderEvent) argument.getPayload();
        assertThat(payload.getExternalOrderId()).isEqualTo(EXTERNAL_ORDER_ID);
        assertThat(payload.getRollback()).isEqualTo(false);
        assertThat(payload.getRawEvent()).isEqualTo(JSON_MAPPER.valueToTree(event));
    }

    @Test
    void produceOnOrderEvent() {
        EntityEvent<Order> event = mockOrderEvent(ORDER_ID);

        onDomainEventLogToSqs.logOrderEvent(event);

        ArgumentCaptor<Event> argumentCaptor = ArgumentCaptor.forClass(Event.class);
        Mockito.verify(jmsTemplate, Mockito.timeout(500L)).convertAndSend(eq(QUEUE), argumentCaptor.capture());
        Event argument = argumentCaptor.getValue();

        assertThat(argument.getEventId()).isEqualTo(event.getUuid().toString());

        assertThat(argument.getPayload()).isInstanceOf(CourierOrderEvent.class);
        assertThat(argument.getEventType()).isEqualTo(EVENT_TYPE_SNAKE_CASE);
        assertThat(argument.getSource()).isEqualTo(SOURCE);
        assertThat(argument.getEventId()).isEqualTo(event.getUuid().toString());

        CourierOrderEvent payload = (CourierOrderEvent) argument.getPayload();
        assertThat(payload.getExternalOrderId()).isEqualTo(EXTERNAL_ORDER_ID);
        assertThat(payload.getRollback()).isEqualTo(false);
        assertThat(payload.getRawEvent()).isEqualTo(JSON_MAPPER.valueToTree(event));
    }

    @Test
    void formatEventTypeOnOrderFlowStatusChangedEvent() {
        Mockito.when(configurationProviderAdapter.isBooleanEnabled(
                ConfigurationProperties.SEND_EVENT_LOG_TO_SQS_SYNCHRONOUSLY
            ))
            .thenReturn(true);

        EntityEvent<Order> event = mockOrderFlowStatusChangedEvent(OrderFlowStatus.TRANSPORTATION_RECIPIENT);

        onDomainEventLogToSqs.logOrderEvent(event);

        ArgumentCaptor<Event> argumentCaptor = ArgumentCaptor.forClass(Event.class);
        Mockito.verify(jmsTemplate).convertAndSend(eq(QUEUE), argumentCaptor.capture());

        Event argument = argumentCaptor.getValue();

        assertThat(argument.getPayload()).isInstanceOf(CourierOrderEvent.class);
        assertThat(argument.getSource()).isEqualTo(SOURCE);
        assertThat(argument.getEventId()).isEqualTo(event.getUuid().toString());

        assertThat(argument.getEventType()).isEqualTo(
            "ORDER_FLOW_STATUS_CHANGED_TO_" + OrderFlowStatus.TRANSPORTATION_RECIPIENT.name()
        );
    }

    @Test
    void doNotProduceIfFlagOff() {
        Mockito.when(configurationProviderAdapter.isBooleanEnabled(ConfigurationProperties.SEND_EVENT_LOG_TO_SQS))
            .thenReturn(false);
        EntityEvent<Order> event = mockOrderEvent(ORDER_ID);

        onDomainEventLogToSqs.logOrderEvent(event);

        Mockito.verifyNoInteractions(orderEventSqsLogProducer);
        Mockito.verifyNoInteractions(jmsTemplate);
    }

    private EntityEvent<Order> mockOrderEvent(Long orderId) {
        return new EntityEvent<>(order) {
            @Override
            public String getEventType() {
                return EVENT_TYPE;
            }

            @Override
            public Class<?> getAggregateType() {
                return Order.class;
            }

            @Override
            public Long getAggregateId() {
                return orderId;
            }
        };
    }

    private OrderFlowStatusChangedEvent mockOrderFlowStatusChangedEvent(OrderFlowStatus transitionTo) {
        Mockito.doReturn(Order.class)
            .when(order)
            .getRealClass();
        Mockito.doReturn(ORDER_ID)
            .when(order)
            .getId();
        return new OrderFlowStatusChangedEvent(
            order,
            new StatusTransition<>(null, transitionTo, StatusTransitionType.NORMAL, null)
        );
    }
}
