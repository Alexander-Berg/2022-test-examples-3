package ru.yandex.market.mbi.logprocessor.logbroker;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.kikimr.persqueue.compression.CompressionCodec;
import ru.yandex.kikimr.persqueue.consumer.StreamListener;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.ConsumerReadResponse;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageBatch;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageData;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageMeta;
import ru.yandex.market.logbroker.consumer.LogbrokerDataProcessor;
import ru.yandex.market.logbroker.db.LogbrokerMonitorExceptionsService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class LogbrokerListenerTest {
    private static final int BATCH_SIZE = 10;

    private LogbrokerDataProcessor processor;
    private LogbrokerListener listener;

    private ConsumerReadResponse read;
    private StreamListener.ReadResponder readResponder;
    private String topic;

    private static MessageMeta messageMeta() {
        long currentTime = System.currentTimeMillis();
        return new MessageMeta("testSourceId".getBytes(),
                1,
                currentTime,
                currentTime,
                "0.0.0.0",
                CompressionCodec.RAW,
                Collections.emptyMap());
    }

    @BeforeEach
    public void before() {
        // setting up mocks
        processor = mock(LogbrokerDataProcessor.class);
        LogbrokerMonitorExceptionsService exceptionsService = mock(LogbrokerMonitorExceptionsService.class);
        listener = new LogbrokerListener(
                processor,
                exceptionsService,
                BATCH_SIZE
        );

        read = mock(ConsumerReadResponse.class);
        readResponder = mock(StreamListener.ReadResponder.class);

        topic = "/it/is/topic";
    }

    @Test
    public void onReadTwoBatchesTest() {
        MessageData messageData1 = new MessageData(new byte[0], 0, messageMeta());
        MessageData messageData2 = new MessageData(new byte[0], 0, messageMeta());
        MessageData messageData3 = new MessageData(new byte[0], 0, messageMeta());
        List<MessageBatch> messageBatches = List.of(
                new MessageBatch(topic, 1, List.of(messageData1, messageData2)),
                new MessageBatch(topic, 2, List.of(messageData3))
        );

        doReturn(messageBatches)
                .when(read).getBatches();

        doAnswer(invocation -> {
            MessageBatch batch = invocation.getArgument(0);
            assertEquals(batch.getTopic(), topic, "check topic of combined batch");
            assertEquals(1, batch.getPartition(), "check partition of combined batch");
            assertEquals(batch.getMessageData(), List.of(messageData1, messageData2, messageData3),
                    "check MessageData of combined batch");

            return null;
        }).when(processor).process(any());

        listener.onRead(read, readResponder);

        verify(processor).process(any());
        verify(readResponder).commit();
    }

    @Test
    public void onReadBigBatchTest() {
        List<MessageData> messageDataList = new LinkedList<>();
        final int countOfRecords = Double.valueOf(BATCH_SIZE * 3.5).intValue();

        for (int i = 0; i < countOfRecords; ++i) {
            messageDataList.add(new MessageData(new byte[0], 0, messageMeta()));
        }

        List<MessageBatch> messageBatches = List.of(new MessageBatch(topic, 1, messageDataList));

        doReturn(messageBatches)
                .when(read).getBatches();

        List<MessageData> resultMesageDataList = new LinkedList<>();

        doAnswer(invocation -> {
            MessageBatch batch = invocation.getArgument(0);
            resultMesageDataList.addAll(batch.getMessageData());
            return null;
        }).when(processor).process(any());

        listener.onRead(read, readResponder);

        assertEquals(messageDataList.size(), resultMesageDataList.size());
        verify(processor, times(4)).process(any());
        verify(readResponder).commit();
    }
}
