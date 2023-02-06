package ru.yandex.market.delivery.transport_manager.queue.task.tpl.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.queue.base.QueueRegister;
import ru.yandex.market.delivery.transport_manager.service.unit_queue.UnitQueueProcessor;
import ru.yandex.money.common.dbqueue.api.Task;
import ru.yandex.money.common.dbqueue.config.QueueShardId;

class UnitQueueProcessingConsumerTest extends AbstractContextualTest {
    private UnitQueueProcessingConsumer consumer;

    @Autowired
    private QueueRegister queueRegister;

    @Autowired
    private ObjectMapper objectMapper;

    private final UnitQueueProcessor processor = Mockito.mock(UnitQueueProcessor.class);

    @BeforeEach
    void setUp() {
        consumer = new UnitQueueProcessingConsumer(
            queueRegister,
            objectMapper,
            processor
        );
    }

    @Test
    void executeTask() {
        consumer.executeTask(task());

        Mockito.verify(processor, Mockito.times(1)).process(100500L);
    }

    private static Task<UnitQueueProcessingDto> task() {
        return Task.<UnitQueueProcessingDto>builder(new QueueShardId("id"))
            .withPayload(new UnitQueueProcessingDto(100500L))
            .build();
    }
}
