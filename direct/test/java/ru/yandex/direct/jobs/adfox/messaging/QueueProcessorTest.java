package ru.yandex.direct.jobs.adfox.messaging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.jobs.adfox.messaging.ytutils.RawQueueMessage;
import ru.yandex.direct.jobs.adfox.messaging.ytutils.YtOrderedTableReader;

import static freemarker.template.utility.Collections12.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Disabled("Не работающая фича 'частные сделки'")
class QueueProcessorTest {

    private YtOrderedTableReader tableReaderMock;
    private RawMessageConsumer rawMessageConsumerMock;
    private QueueProcessor queueProcessor;

    @BeforeEach
    void setUp() {
        rawMessageConsumerMock = mock(RawMessageConsumer.class);
        tableReaderMock = mock(YtOrderedTableReader.class);
        queueProcessor = new QueueProcessor(tableReaderMock, rawMessageConsumerMock);
    }

    @Test
    void processNewMessages_success() {
        assertThatCode(() -> queueProcessor.processNewMessages(-1))
                .doesNotThrowAnyException();
    }

    @Test
    @SuppressWarnings("unchecked")
    void processNewMessages_success_ifSingleMessageFailed() throws Exception {
        when(tableReaderMock.read(anyLong(), anyLong(), any()))
                .thenReturn(singletonList(new RawQueueMessage(0L, "messageText")));
        doThrow(new RuntimeException()).when(rawMessageConsumerMock).consume(any());

        assertThatCode(() -> queueProcessor.processNewMessages(-1))
                .doesNotThrowAnyException();
    }

    @Test
    void processNewMessages_fails_ifCantReadFromYt() {
        doThrow(new RuntimeException("Can't read from YT")).when(tableReaderMock).read(anyLong(), anyLong(), any());

        assertThatThrownBy(() -> queueProcessor.processNewMessages(-1))
                .hasMessageContaining("Can't read from YT");
    }

    @Test
    void getLastProcessedRawIndex_success_ifNoNewMessages() {
        queueProcessor.processNewMessages(10);
        assertThat(queueProcessor.getLastSuccessfulRowIndex()).isEqualTo(10L);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getLastProcessedRawIndex_success_ifNewMessagesPresent() {
        when(tableReaderMock.read(anyLong(), anyLong(), any()))
                .thenReturn(singletonList(new RawQueueMessage(11L, "messageText")));

        queueProcessor.processNewMessages(10);
        // прочитали сообщение с offset = 11L. Ожидаем получить его в #getLastSuccessfulRowIndex()
        assertThat(queueProcessor.getLastSuccessfulRowIndex()).isEqualTo(11L);
    }

    @Test
    void getLastProcessedRawIndex_success_ifCantReadFromYt() {
        doThrow(new RuntimeException("Can't read from YT")).when(tableReaderMock).read(anyLong(), anyLong(), any());

        try {
            queueProcessor.processNewMessages(10);
        } catch (Exception e) { /*do nothing*/ }

        assertThat(queueProcessor.getLastSuccessfulRowIndex()).isEqualTo(10L);
    }
}
