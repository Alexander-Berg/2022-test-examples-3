package ru.yandex.market.unredeemed;

import java.io.IOException;
import java.util.List;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageBatch;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageData;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.shop.FunctionalTest;

@ExtendWith(MockitoExtension.class)
public class LogbrokerLogisticReturnsEventsProcessorTest extends FunctionalTest {

    @Mock
    private MessageBatch messageBatchMock;
    @Mock
    private MessageData messageDataMock;

    @Autowired
    private LogbrokerLogisticReturnsEventsProcessor logbrokerLogisticReturnsEventsProcessor;

    @BeforeEach
    void setup() {
        Mockito.when(messageBatchMock.getMessageData()).thenReturn(List.of(messageDataMock));
    }

    @Test
    @DisplayName("Тест на обработку событий из топика lrm - запись события с source null")
    @DbUnitDataSet(
            before = "db/LogbrokerLogisticReturnsEventsProcessorTest.readLogbrokerEvent.before.csv",
            after = "db/LogbrokerLogisticReturnsEventsProcessorTest.readLogbrokerEvent.after.csv"
    )
    void readLogbrokerEmptySourceEventTest() throws IOException {
        setupMock("logistic-returns-event.json");
        logbrokerLogisticReturnsEventsProcessor.process(messageBatchMock);
    }

    @Test
    @DisplayName("Тест на обработку событий из топика lrm - запись события невыкупа (source!=client)")
    @DbUnitDataSet(
            before = "db/LogbrokerLogisticReturnsEventsProcessorTest.readLogbrokerEvent.before.csv",
            after = "db/LogbrokerLogisticReturnsEventsProcessorTest.readLogbrokerEvent.after.csv"
    )
    void readLogbrokerNonClientSourceEventTest() throws IOException {
        setupMock("logistic-returns-non-client-source-event.json");
        logbrokerLogisticReturnsEventsProcessor.process(messageBatchMock);
    }

    @Test
    @DisplayName("Тест на обработку событий из топика lrm - запись события возврата (source==client)," +
            " будет 0 значение unredeemed_count")
    @DbUnitDataSet(
            before = "db/LogbrokerLogisticReturnsEventsProcessorTest.readLogbrokerEvent.before.csv",
            after = "db/LogbrokerLogisticReturnsEventsProcessorTest.readLogbrokerEvent.return.after.csv"
    )
    void readLogbrokerClientSourceEventTest() throws IOException {
        setupMock("logistic-returns-client-source-event.json");
        logbrokerLogisticReturnsEventsProcessor.process(messageBatchMock);
    }

    private void setupMock(String jsonEvent) throws IOException {
        byte[] eventBytes = getClass().getResourceAsStream("events/" + jsonEvent).readAllBytes();
        Mockito.when(messageDataMock.getDecompressedData()).thenReturn(eventBytes);
    }
}
