package ru.yandex.market.delivery.transport_manager.service.les;

import com.amazon.sqs.javamessaging.SQSQueueDestination;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.queue.task.les.event_received.LesEventReceivedProducer;
import ru.yandex.market.logistics.les.ScOutboundReadyEvent;
import ru.yandex.market.logistics.les.base.Event;

class LesConsumerTest extends AbstractContextualTest {
    @Autowired
    private LesConsumer lesConsumer;

    @Autowired
    private LesEventReceivedProducer lesEventReceivedProducer;

    @Test
    void acceptEvent() {
        Event event = event(scOutboundReadyEvent());
        lesConsumer.acceptEvent(
            Mockito.mock(SQSQueueDestination.class),
            "bb",
            "cc",
            123L,
            event
        );

        Mockito.verify(lesEventReceivedProducer).enqueue(event);
    }

    private static Event event(ScOutboundReadyEvent payload) {
        return new Event(
            "source",
            "someId",
            123L,
            "OUTBOUND_FINISHED",
            payload,
            "desc"
        );
    }

    private static ScOutboundReadyEvent scOutboundReadyEvent() {
        return new ScOutboundReadyEvent(
            1L,
            "100500",
            "ololo"
        );
    }
}
