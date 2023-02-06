package ru.yandex.market.ff4shops.lgw;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.kikimr.persqueue.compression.CompressionCodec;
import ru.yandex.kikimr.persqueue.consumer.StreamListener;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.ConsumerReadResponse;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageBatch;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageData;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageMeta;
import ru.yandex.market.ff4shops.logbroker.LogbrokerStreamListener;
import ru.yandex.market.logbroker.consumer.LogbrokerDataProcessor;
import ru.yandex.market.logbroker.db.LogbrokerMonitorExceptionsService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class StocksStreamListenerTest {

    @Test
    public void testLinearize() {
        LogbrokerDataProcessor processor = mock(LogbrokerDataProcessor.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        LogbrokerMonitorExceptionsService exceptionsService = mock(LogbrokerMonitorExceptionsService.class);
        RetryTemplate retryTemplate = new RetryTemplate();
        doAnswer(invocation -> {
            TransactionCallback callback = invocation.getArgument(0);
            callback.doInTransaction(mock(TransactionStatus.class));
            return null;
        }).when(transactionTemplate).execute(any());
        LogbrokerStreamListener listener = new StocksStreamListener(
                processor, transactionTemplate, exceptionsService, retryTemplate, false
        );
        ConsumerReadResponse readResponse = mock(ConsumerReadResponse.class);
        doReturn(inputValue())
                .when(readResponse).getBatches();
        doReturn(0L)
                .when(readResponse).getCookie();
        ArgumentCaptor<MessageBatch> argumentCaptor = ArgumentCaptor.forClass(MessageBatch.class);
        listener.onRead(readResponse, mock(StreamListener.ReadResponder.class));
        verify(processor).process(argumentCaptor.capture());
        Assertions.assertEquals(3, argumentCaptor.getValue().getMessageData().size(), "Lineriizing failed: ");
        Assertions.assertEquals(0, argumentCaptor.getValue().getMessageData().get(0).getOffset(), "Lineriizing failed: ");
        Assertions.assertEquals(1, argumentCaptor.getValue().getMessageData().get(1).getOffset(), "Lineriizing failed: ");
        Assertions.assertEquals(2, argumentCaptor.getValue().getMessageData().get(2).getOffset(), "Lineriizing failed: ");
    }

    private List<MessageBatch> inputValue() {
        return List.of(
                new MessageBatch("1", 0,
                        List.of(new MessageData(new byte[0], 0, constructMessageMeta(0)))),
                new MessageBatch("1", 0,
                        List.of(new MessageData(new byte[0], 1, constructMessageMeta(1)))),
                new MessageBatch("1", 0,
                        List.of(new MessageData(new byte[0], 2, constructMessageMeta(2))))
        );
    }

    private MessageBatch outputValue() {
        return new MessageBatch("1", 0,
                        List.of(new MessageData(new byte[0], 0, constructMessageMeta(0)),
                                new MessageData(new byte[0], 1, constructMessageMeta(1)),
                                new MessageData(new byte[0], 2, constructMessageMeta(2)))
        );
    }

    private MessageMeta constructMessageMeta(int seqNo) {
        return new MessageMeta("test".getBytes(), seqNo, 0, 0, "::1",
                CompressionCodec.RAW, Collections.emptyMap());
    }
}
