package ru.yandex.market.tpl.tms.service.external.sqs;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.jms.core.JmsTemplate;

import ru.yandex.market.logistics.les.base.Event;
import ru.yandex.market.logistics.les.tpl.TplOrderDeliveryTaskFailedEvent;
import ru.yandex.market.logistics.les.tpl.enums.TplOrderDeliveryTaskFailReason;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.core.domain.order.producer.OrderLockerDeliveryFailedSendToSqsPayload;
import ru.yandex.market.tpl.tms.test.TplTmsAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
public class OrderLockerDeliveryFailedSendToSqsServiceTest extends TplTmsAbstractTest {

    private static final String REQUEST_ID = "request-id";
    private static final String QUEUE_NAME = "courier_out";
    private static final OrderDeliveryTaskFailReasonType FAIL_REASON =
            OrderDeliveryTaskFailReasonType.DIMENSIONS_EXCEEDS;
    private static final TplOrderDeliveryTaskFailReason EVENT_FAIL_REASON =
            TplOrderDeliveryTaskFailReason.DIMENSIONS_EXCEEDED;
    private static final String EXTERNAL_ORDER_ID = "ext-id";
    private static final String EVENT_ID = "event-uuid";
    private static final String EVENT_TYPE = "TPL_ORDER_DELIVERY_TASK_FAILED";
    private static final long TIMESTAMP = 1324;

    private final JmsTemplate jmsTemplate;
    private final OrderLockerDeliveryFailedSendToSqsService service;

    @Test
    void sendMessageToSqsTest() {
        // when
        service.processPayload(new OrderLockerDeliveryFailedSendToSqsPayload(
                REQUEST_ID,
                TIMESTAMP,
                EXTERNAL_ORDER_ID,
                EVENT_ID,
                FAIL_REASON
        ));

        // then
        ArgumentCaptor<Event> argumentCaptor = ArgumentCaptor.forClass(Event.class);
        Mockito.verify(jmsTemplate).convertAndSend(Mockito.eq(QUEUE_NAME), argumentCaptor.capture());

        Event eventDto = argumentCaptor.getValue();
        assertThat(eventDto.getEventType()).isEqualTo(EVENT_TYPE);
        assertThat(eventDto.getEventId()).isEqualTo(EVENT_ID);
        assertThat(eventDto.getTimestamp()).isEqualTo(TIMESTAMP);
        assertThat(eventDto.getPayload()).isExactlyInstanceOf(TplOrderDeliveryTaskFailedEvent.class);

        TplOrderDeliveryTaskFailedEvent internalEvent = (TplOrderDeliveryTaskFailedEvent) eventDto.getPayload();
        assertThat(internalEvent).isNotNull();
        assertThat(internalEvent.getExternalOrderId()).isEqualTo(EXTERNAL_ORDER_ID);
        assertThat(internalEvent.getReason()).isEqualTo(EVENT_FAIL_REASON);
    }
}
