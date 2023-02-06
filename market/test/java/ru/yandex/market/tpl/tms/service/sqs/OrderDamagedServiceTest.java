package ru.yandex.market.tpl.tms.service.sqs;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.jms.core.JmsTemplate;

import ru.yandex.market.logistics.les.OrderDamagedEvent;
import ru.yandex.market.logistics.les.base.Event;
import ru.yandex.market.tpl.core.domain.order.producer.OrderDamagedPayload;
import ru.yandex.market.tpl.tms.service.external.sqs.OrderDamagedService;
import ru.yandex.market.tpl.tms.test.TplTmsAbstractTest;

@RequiredArgsConstructor
public class OrderDamagedServiceTest extends TplTmsAbstractTest {

    private static final String QUEUE_NAME = "courier_out";
    private static final String SOURCE = "courier";
    private static final Long ORDER_ID = 4L;

    private final JmsTemplate jmsTemplate;
    private final OrderDamagedService service;

    @Test
    void sendMessageToSqsTest() {
        service.processPayload(
            new OrderDamagedPayload(
                "request id",
                1234,
                ORDER_ID.toString(),
                "event_uuid"
            )
        );

        ArgumentCaptor<Event> argumentCaptor = ArgumentCaptor.forClass(Event.class);
        Mockito.verify(jmsTemplate).convertAndSend(Mockito.eq(QUEUE_NAME), argumentCaptor.capture());

        Event eventDto = argumentCaptor.getValue();
        Assertions.assertThat(eventDto.getSource()).isEqualTo(SOURCE);
        Assertions.assertThat(eventDto.getEventType()).isEqualTo("ORDER_IS_DAMAGED");
        Assertions.assertThat(eventDto.getEventId()).isEqualTo("event_uuid");
        Assertions.assertThat(eventDto.getPayload()).isEqualTo(new OrderDamagedEvent(ORDER_ID.toString()));
    }
}
