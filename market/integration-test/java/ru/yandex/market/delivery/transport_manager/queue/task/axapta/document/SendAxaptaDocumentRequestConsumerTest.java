package ru.yandex.market.delivery.transport_manager.queue.task.axapta.document;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.queue.base.QueueRegister;
import ru.yandex.market.delivery.transport_manager.service.axapta.document.AxaptaDocumentRequestSender;
import ru.yandex.money.common.dbqueue.api.Task;
import ru.yandex.money.common.dbqueue.config.QueueShardId;

class SendAxaptaDocumentRequestConsumerTest extends AbstractContextualTest {
    private SendAxaptaDocumentRequestConsumer consumer;

    @Autowired
    private QueueRegister queueRegister;

    @Autowired
    private ObjectMapper objectMapper;

    private AxaptaDocumentRequestSender documentRequestSender = Mockito.mock(
        AxaptaDocumentRequestSender.class
    );

    @Test
    void executeTask() {
        consumer = new SendAxaptaDocumentRequestConsumer(
            queueRegister,
            objectMapper,
            documentRequestSender
        );

        consumer.executeTask(task());

        Mockito.verify(documentRequestSender, Mockito.times(1)).send(100500L);
    }

    private static Task<SendAxaptaDocumentRequestDto> task() {
        return Task.<SendAxaptaDocumentRequestDto>builder(new QueueShardId("id"))
            .withPayload(new SendAxaptaDocumentRequestDto(100500L))
            .build();
    }
}
