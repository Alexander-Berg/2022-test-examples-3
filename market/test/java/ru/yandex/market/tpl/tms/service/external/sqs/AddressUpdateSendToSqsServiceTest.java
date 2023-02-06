package ru.yandex.market.tpl.tms.service.external.sqs;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.jms.core.JmsTemplate;

import ru.yandex.market.logistics.les.base.Event;
import ru.yandex.market.logistics.les.tpl.TplAddressChangedEvent;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.producer.OrderDeliveryAddressUpdatePayload;
import ru.yandex.market.tpl.tms.test.TplTmsAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
public class AddressUpdateSendToSqsServiceTest extends TplTmsAbstractTest {

    private static final String QUEUE_NAME = "courier_out";
    private static final String SOURCE = "courier";
    private static final String EXTERNAL_ORDER_ID = "ex4";
    private static final String EVENT_ID = "event uuid";
    private static final String EVENT_TYPE = "TPL_ADDRESS_CHANGED";
    private static final long TIMESTAMP = 1324;

    private final JmsTemplate jmsTemplate;
    private final OrderGenerateService orderGenerateService;
    private final AddressUpdateSendToSqsService service;

    @BeforeEach
    void init() {
        orderGenerateService.createOrder(EXTERNAL_ORDER_ID);
    }

    @Test
    void sendMessageToSqsTest() {
        // when
        service.processPayload(
                new OrderDeliveryAddressUpdatePayload(
                        "request id",
                        TIMESTAMP,
                        EXTERNAL_ORDER_ID,
                        EVENT_ID,
                        Source.SYSTEM
                )
        );

        // then
        ArgumentCaptor<Event> argumentCaptor = ArgumentCaptor.forClass(Event.class);
        Mockito.verify(jmsTemplate).convertAndSend(Mockito.eq(QUEUE_NAME), argumentCaptor.capture());

        Event eventDto = argumentCaptor.getValue();
        assertThat(eventDto.getSource()).isEqualTo(SOURCE);
        assertThat(eventDto.getEventType()).isEqualTo(EVENT_TYPE);
        assertThat(eventDto.getEventId()).isEqualTo(EVENT_ID);
        assertThat(eventDto.getTimestamp()).isEqualTo(TIMESTAMP);
        assertThat(eventDto.getPayload()).isExactlyInstanceOf(TplAddressChangedEvent.class);

        TplAddressChangedEvent internalEvent = (TplAddressChangedEvent) eventDto.getPayload();
        assertThat(internalEvent).isNotNull();
    }
}
