package ru.yandex.market.tpl.tms.service.external.sqs;

import java.time.Clock;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jms.core.JmsTemplate;

import ru.yandex.market.logistics.les.base.Event;
import ru.yandex.market.tpl.core.domain.order.OrderFlowStatusHistory;
import ru.yandex.market.tpl.core.external.sqs.SqsQueueProperties;
import ru.yandex.market.tpl.tms.service.external.sqs.mapper.OrderFlowStatusHistoryMapper;

@ExtendWith(MockitoExtension.class)
class OrderFlowStatusHistorySenderTest {

    public static final String TRACKER_COURIER_OUT_QUEUE = "tracker_courier_out";

    @Mock
    private JmsTemplate jmsTemplate;
    @Mock
    private OrderFlowStatusHistoryMapper mapper;
    private Clock clock;

    private OrderFlowStatusHistorySender sender;

    @BeforeEach
    void init() {
        clock = Clock.systemDefaultZone();

        SqsQueueProperties sqsQueueProperties = new SqsQueueProperties();
        sqsQueueProperties.setSource("courier");
        sqsQueueProperties.setTrackerQueue(TRACKER_COURIER_OUT_QUEUE);
        sender = new OrderFlowStatusHistorySender(
                mapper,
                sqsQueueProperties,
                jmsTemplate,
                clock
        );
    }

    @Test
    void send() {
        OrderFlowStatusHistory event = new OrderFlowStatusHistory();
        event.setId(12345098L);

        sender.send(event);

        ArgumentCaptor<Event> argumentCaptor = ArgumentCaptor.forClass(Event.class);
        Mockito.verify(jmsTemplate).convertAndSend(Mockito.eq(TRACKER_COURIER_OUT_QUEUE), argumentCaptor.capture());

        Event eventDto = argumentCaptor.getValue();
        Assertions.assertThat(eventDto.getEventType()).isEqualTo(OrderFlowStatusHistorySender.EVENT_TYPE);
        Assertions.assertThat(eventDto.getEventId()).isEqualTo(event.getId().toString());
    }
}
