package ru.yandex.market.logbroker.consumer;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionOperations;

import ru.yandex.kikimr.persqueue.consumer.StreamConsumer;
import ru.yandex.kikimr.persqueue.consumer.StreamListener;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.ConsumerReadResponse;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageBatch;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageData;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageMeta;
import ru.yandex.market.logbroker.consumer.util.LbParser;
import ru.yandex.market.logbroker.consumer.util.impl.DummyLbReaderOffsetDao;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class ParsingStreamListenerTest {

    @Test
    @DisplayName("Проверка обработки батча из одной партиции")
    void singleBatchTest() {
        ParsingStreamListener<String> listenerUnderTest =
                getListenerMock(list -> assertThat(list).isNotEmpty().hasSize(10));
        StreamListener.ReadResponder noOpResponder = () -> {};
        List<MessageBatch> batches = ImmutableList.of(
                new MessageBatch("test-topic", 0,
                        IntStream.rangeClosed(1, 10)
                                .mapToObj(i ->
                                        createMessageDataMock("Hello from partition 0", i)
                                ).collect(toList()))
        );
        ConsumerReadResponse readResponse = new ConsumerReadResponse(batches, 1);

        listenerUnderTest.onInitCanThrow(null);
        listenerUnderTest.onReadCanThrow(readResponse, noOpResponder);
    }

    @Test
    @DisplayName("Проверка обработки нескольких батчей из разных партиций")
    void multiBatchTest() {
        ParsingStreamListener<String> listenerUnderTest = getListenerMock(list -> {
            assertThat(list).isNotEmpty();
            if (list.get(0).equals("Hello from partition 0")) {
                assertThat(list).hasSize(7);
            } else {
                assertThat(list).hasSize(3);
            }
        });
        //7 сообщений из партиции 0 и 3 сообщения из партиции 1
        StreamListener.ReadResponder noOpResponder = () -> {
        };
        List<MessageBatch> batches = ImmutableList.of(
                new MessageBatch("test-topic", 0,
                        IntStream.rangeClosed(1, 7)
                                .mapToObj(i ->
                                        createMessageDataMock("Hello from partition 0", i)
                                ).collect(toList())),
                new MessageBatch("test-topic", 1,
                        IntStream.rangeClosed(1, 3)
                                .mapToObj(i ->
                                        createMessageDataMock("Hello from partition 1", i)
                                ).collect(toList()))
        );
        ConsumerReadResponse readResponse = new ConsumerReadResponse(batches, 1);

        listenerUnderTest.onInitCanThrow(null);
        listenerUnderTest.onReadCanThrow(readResponse, noOpResponder);
    }

    private ParsingStreamListener<String> getListenerMock(Consumer<List<String>> verifier) {
        String entityName = "entity";
        StreamConsumer consumerMock = Mockito.mock(StreamConsumer.class);
        AutoCloseable noOpCleaner = () -> {
        };
        LbParser<String> parser = (entity, line) -> line;
        TransactionOperations transactionOperations = new TransactionOperations() {
            @Override
            public <T> T execute(TransactionCallback<T> action) throws TransactionException {
                return action.doInTransaction(null);
            }
        };
        return new ParsingStreamListener<>(
                entityName,
                consumerMock,
                noOpCleaner,
                new DummyLbReaderOffsetDao(),
                parser,
                transactionOperations,
                verifier,
                1,
                StreamListenerExceptionStrategies.PROPAGATE_ALL
        );
    }

    private MessageData createMessageDataMock(String returnedData, long offset) {
        MessageData mockedData = Mockito.mock(MessageData.class);
        when(mockedData.getDecompressedData()).thenReturn(returnedData.getBytes(StandardCharsets.UTF_8));
        when(mockedData.getOffset()).thenReturn(offset);
        when(mockedData.getMessageMeta()).thenReturn(Mockito.mock(MessageMeta.class));
        return mockedData;
    }

}
