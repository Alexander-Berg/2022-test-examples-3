package ru.yandex.market.logistics.lom.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.jobs.model.ProcessingErrorPayload;
import ru.yandex.market.logistics.lom.jobs.processor.SendProcessingErrorToMqmProcessor;
import ru.yandex.market.logistics.mqm.client.MqmClient;
import ru.yandex.market.logistics.mqm.model.enums.ProcessType;
import ru.yandex.market.logistics.mqm.model.request.planfact.ProcessingError;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class SendProcessingErrorToMqmTest extends AbstractContextualTest {

    @Autowired
    private SendProcessingErrorToMqmProcessor processor;

    @Autowired
    private MqmClient mqmClient;

    @AfterEach
    void afterTest() {
        verifyNoMoreInteractions(mqmClient);
    }

    @Test
    @DisplayName("Процессинг ошибки")
    void process() {
        processor.processPayload(new ProcessingErrorPayload(
            REQUEST_ID,
            ProcessType.LOM_ORDER_CREATE,
            1L,
            123L,
            400,
            "Error message"
        ));

        verify(mqmClient).addProcessingError(
            new ProcessingError(
                ProcessType.LOM_ORDER_CREATE,
                1L,
                123L,
                400,
                "Error message"
            )
        );

    }
}
