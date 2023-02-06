package ru.yandex.market.tpl.tms.service.sqs;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.jms.core.JmsTemplate;

import ru.yandex.market.logistics.les.RevertOrderDamagedEvent;
import ru.yandex.market.logistics.les.base.Event;
import ru.yandex.market.tpl.core.domain.order.producer.RevertOrderDamagedPayload;
import ru.yandex.market.tpl.tms.service.external.sqs.RevertOrderDamagedService;
import ru.yandex.market.tpl.tms.test.TplTmsAbstractTest;

@RequiredArgsConstructor
public class RevertOrderDamagedServiceTest extends TplTmsAbstractTest {

    private static final String QUEUE_NAME = "courier_out";
    private static final String SOURCE = "courier";
    private static final String EXTERNAL_ORDER_ID = "ex4";
    private static final String EVENT_ID = "event uuid";
    private static final long TIMESTAMP = 1324;

    private final JmsTemplate jmsTemplate;
    private final RevertOrderDamagedService service;

    @Test
    void sendMessageToSqsTest() {
        service.processPayload(
            new RevertOrderDamagedPayload(
                "request id",
                TIMESTAMP,
                EXTERNAL_ORDER_ID,
                EVENT_ID
            )
        );

        ArgumentCaptor<Event> argumentCaptor = ArgumentCaptor.forClass(Event.class);
        Mockito.verify(jmsTemplate).convertAndSend(Mockito.eq(QUEUE_NAME), argumentCaptor.capture());

        Event eventDto = argumentCaptor.getValue();
        Assertions.assertThat(eventDto.getSource()).isEqualTo(SOURCE);
        Assertions.assertThat(eventDto.getEventType()).isEqualTo("REVERT_ORDER_DAMAGED");
        Assertions.assertThat(eventDto.getEventId()).isEqualTo(EVENT_ID);
        Assertions.assertThat(eventDto.getTimestamp()).isEqualTo(TIMESTAMP);
        Assertions.assertThat(eventDto.getPayload()).isExactlyInstanceOf(RevertOrderDamagedEvent.class);

        RevertOrderDamagedEvent internalEvent = (RevertOrderDamagedEvent) eventDto.getPayload();
        Assertions.assertThat(internalEvent.getExternalOrderId()).isEqualTo(EXTERNAL_ORDER_ID);
    }
}
