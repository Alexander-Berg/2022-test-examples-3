package ru.yandex.market.pricelabs.tms.logbroker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import NKwYT.Queries.TPriceLabsFeedsItem;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.kikimr.persqueue.compression.CompressionCodec;
import ru.yandex.kikimr.persqueue.consumer.StreamConsumer;
import ru.yandex.kikimr.persqueue.consumer.StreamListener;
import ru.yandex.kikimr.persqueue.consumer.StreamListener.ReadResponder;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.ConsumerReadResponse;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageBatch;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageData;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageMeta;
import ru.yandex.market.pricelabs.misc.Utils;
import ru.yandex.market.pricelabs.tms.logbroker.LogbrokerConfig.ConsumerSourceWithName;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

@Slf4j
@ExtendWith(MockitoExtension.class)
class LogbrokerProcessorTest {

    private static final String SAMPLE = "789ce312ce282929b0d2d7cfc9cc4d4dd42b2ad537313091b875fdff2b562d27ce037f1e04a" +
            "871bb3848343434b0589c0589463cfd74e7156b032398ea60649ac1b8ff2b90b583b197b18b8989c30744c89e62623332b134303" +
            "73438c168c1e4a469686960646a69a16fe667e4146658146994ee63ee5f5819ea5d1611e25de05c5e6c5e18551266595ee3c118c" +
            "198c09005d50d005f36365b";

    @Mock
    private StreamConsumer streamConsumer;

    @Mock
    private ReadResponder readResponder;

    @Captor
    private ArgumentCaptor<StreamListener> listenerCaptor;

    private List<TPriceLabsFeedsItem> items;
    private StreamListener listener;

    @BeforeEach
    void init() throws InterruptedException {
        items = new ArrayList<>();

        var executor = Executors.newSingleThreadScheduledExecutor();
        var processor = new PurchasePriceLogbrokerProcessor(
                List.of(new ConsumerSourceWithName("test", () -> streamConsumer)), items::add, 60, executor);
        processor.start();

        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

        verify(streamConsumer).startConsume(listenerCaptor.capture());
        listener = listenerCaptor.getValue();
    }

    @Test
    void test() {
        listener.onRead(response(batch(Utils.decodeHex(SAMPLE))), readResponder);

        assertEquals(1, items.size());
        log.info("{}", items.get(0));

        Mockito.verify(readResponder).commit();

    }

    private ConsumerReadResponse response(MessageBatch... batches) {
        return new ConsumerReadResponse(List.of(batches), 0);
    }

    private MessageBatch batch(byte[]... content) {
        return new MessageBatch("", 0, Stream.of(content)
                .map(data -> new MessageData(data, 0, meta()))
                .collect(Collectors.toList()));
    }

    private MessageMeta meta() {
        return new MessageMeta(new byte[0], 0, 0, 0, "", CompressionCodec.RAW, Map.of());
    }
}
