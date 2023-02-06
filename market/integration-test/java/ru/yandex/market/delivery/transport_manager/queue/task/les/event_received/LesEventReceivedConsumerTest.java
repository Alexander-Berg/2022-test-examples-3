package ru.yandex.market.delivery.transport_manager.queue.task.les.event_received;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.queue.base.QueueRegister;
import ru.yandex.market.delivery.transport_manager.service.core.TmEventPublisher;
import ru.yandex.market.logistics.les.ScOutboundReadyEvent;
import ru.yandex.market.logistics.les.base.Event;
import ru.yandex.money.common.dbqueue.api.Task;
import ru.yandex.money.common.dbqueue.config.QueueShardId;

class LesEventReceivedConsumerTest extends AbstractContextualTest {
    @Autowired
    private QueueRegister queueRegister;

    @Autowired
    private ObjectMapper objectMapper;

    private TmEventPublisher publisher = Mockito.mock(TmEventPublisher.class);

    private LesEventReceivedConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new LesEventReceivedConsumer(
            queueRegister,
            objectMapper,
            publisher
        );
    }

    @Test
    void executeTask() {
        Event event = event(scOutboundReadyEvent());
        consumer.executeTask(task(event));

        Mockito.verify(publisher, Mockito.times(1)).publishEvent(Mockito.any());
    }

    private static Task<LesEventReceivedDto> task(Event event) {
        return Task.<LesEventReceivedDto>builder(new QueueShardId("id"))
            .withPayload(new LesEventReceivedDto(event))
            .build();
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
