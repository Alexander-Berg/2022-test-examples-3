package ru.yandex.market.delivery.transport_manager.queue.task.distribution_center.document;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.queue.base.QueueRegister;
import ru.yandex.market.delivery.transport_manager.service.distribution_center.document.DcDocumentSender;
import ru.yandex.money.common.dbqueue.api.Task;
import ru.yandex.money.common.dbqueue.config.QueueShardId;

class SendDocumentRequestResultToDcConsumerTest extends AbstractContextualTest {
    private SendDocumentRequestResultToDcConsumer consumer;

    @Autowired
    private QueueRegister queueRegister;

    @Autowired
    private ObjectMapper objectMapper;

    private final DcDocumentSender dcDocumentSender = Mockito.mock(DcDocumentSender.class);

    @BeforeEach
    void setUp() {
        consumer = new SendDocumentRequestResultToDcConsumer(
            queueRegister,
            objectMapper,
            dcDocumentSender
        );
    }

    @Test
    void executeTask() {
        consumer.executeTask(task());
        Mockito.verify(dcDocumentSender).send(100500L);
    }

    private static Task<SendDocumentRequestResultToDcDto> task() {
        return Task.<SendDocumentRequestResultToDcDto>builder(new QueueShardId("id"))
            .withPayload(new SendDocumentRequestResultToDcDto(100500L))
            .build();
    }
}
