package ru.yandex.market.communication.proxy.processor;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageBatch;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageData;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.communication.proxy.AbstractCommunicationProxyTest;
import ru.yandex.market.communication.proxy.logbroker.LogbrokerOrderEventsProcessor;

public class LogbrokerOrderEventsProcessorTest extends AbstractCommunicationProxyTest {

    @Mock
    private MessageBatch messageBatchMock;
    @Mock
    private MessageData messageDataMock;

    @Autowired
    private LogbrokerOrderEventsProcessor logbrokerOrderEventsProcessor;

    @BeforeEach
    void setup() {
        Mockito.when(messageBatchMock.getMessageData())
                .thenReturn(List.of(messageDataMock));
    }

    @Test
    @DbUnitDataSet(
            before = "MarkDisabled.before.csv",
            after = "MarkDisabled.after.csv"
    )
    public void testOrderHistoryEventProcessor() {
        setupMock("new-order-history-event.json");
        logbrokerOrderEventsProcessor.process(messageBatchMock);
    }

    @Test
    @DbUnitDataSet(
            before = "MarkDisabled.before.csv",
            after = "MarkDisabled.before.csv"
    )
    public void testInvalidJson() {
        setupMock("invalid-new-order-history-event.json");
        logbrokerOrderEventsProcessor.process(messageBatchMock);
    }

    private void setupMock(String jsonEvent) {
        byte[] orderEventJson;
        try {
            orderEventJson = getClass()
                    .getResourceAsStream("json/" + jsonEvent)
                    .readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Mockito.when(messageDataMock.getDecompressedData())
                .thenReturn(orderEventJson);
    }
}
