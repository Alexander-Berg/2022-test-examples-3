package ru.yandex.market.tsup.dbqueue.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.common.db.queue.base.QueueRegister;
import ru.yandex.market.tsup.AbstractContextualTest;
import ru.yandex.market.tsup.core.event.EventDistributor;
import ru.yandex.market.tsup.core.event.EventType;
import ru.yandex.market.tsup.core.event.impl.demo.DemoEventPayload;
import ru.yandex.money.common.dbqueue.api.Task;
import ru.yandex.money.common.dbqueue.config.QueueShardId;

class EventDistributionConsumerTest extends AbstractContextualTest {
    @Autowired
    private QueueRegister queueRegister;

    @Autowired
    private ObjectMapper objectMapper;

    @Mock
    private EventDistributor eventDistributor;

    private EventDistributionConsumer eventDistributionConsumer;

    @BeforeEach
    void setUp() {
        eventDistributionConsumer = new EventDistributionConsumer(
            queueRegister,
            objectMapper,
            eventDistributor
        );
    }

    @Test
    void executeTask() {
        eventDistributionConsumer.executeTask(task(EventType.DEMO, new DemoEventPayload("ololo")));
        Mockito.verify(eventDistributor).distribute(EventType.DEMO, new DemoEventPayload("ololo"));
    }

    private Task<EventDistributionDto> task(EventType eventType, DemoEventPayload value) {
        return Task.<EventDistributionDto>builder(new QueueShardId("id"))
            .withPayload(new EventDistributionDto(eventType, objectMapper.convertValue(value, JsonNode.class)))
            .build();
    }
}
