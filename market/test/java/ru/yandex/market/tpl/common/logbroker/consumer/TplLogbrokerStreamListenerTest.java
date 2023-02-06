package ru.yandex.market.tpl.common.logbroker.consumer;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import ru.yandex.kikimr.persqueue.consumer.StreamListener;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.ConsumerReadResponse;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageBatch;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageData;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageMeta;

import static java.util.stream.Collectors.toList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TplLogbrokerStreamListenerTest {

    private final Consumer<LogbrokerMessage> consumer = mock(Consumer.class);
    private final TplLogbrokerStreamListener subject = new TplLogbrokerStreamListener(consumer);
    private final StreamListener.ReadResponder noOpResponder = () -> {};

    @Test
    void decodesBatchSuccessfully() {
        String topic = "test-topic";
        String itemPrefix = "item ";

        List<MessageBatch> batches = ImmutableList.of(
                new MessageBatch(
                        topic, 0,
                        IntStream.rangeClosed(1, 7)
                                .mapToObj(i -> createMockMessageData(itemPrefix + i, i))
                                .collect(toList())),
                new MessageBatch(
                        topic, 0,
                        IntStream.rangeClosed(8, 10)
                                .mapToObj(i -> createMockMessageData(itemPrefix + i, i))
                                .collect(toList()))
        );
        ConsumerReadResponse readResponse = new ConsumerReadResponse(batches, 1);


        subject.onRead(readResponse, noOpResponder);

        var messageCaptor = ArgumentCaptor.forClass(LogbrokerMessage.class);

        verify(consumer, times(10)).accept(messageCaptor.capture());

        var counter = 0;
        for (LogbrokerMessage it : messageCaptor.getAllValues()) {
            counter++;
            Assertions.assertEquals(topic, it.getEntityName());
            Assertions.assertEquals(itemPrefix + counter, it.getPayload());
        }
    }

    private MessageData createMockMessageData(String returnedData, long offset) {
        MessageData mockedData = mock(MessageData.class);
        when(mockedData.getDecompressedData()).thenReturn(returnedData.getBytes(StandardCharsets.UTF_8));
        when(mockedData.getOffset()).thenReturn(offset);
        when(mockedData.getMessageMeta()).thenReturn(mock(MessageMeta.class));
        return mockedData;
    }

}
