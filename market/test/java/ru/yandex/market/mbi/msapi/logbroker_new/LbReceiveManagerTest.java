package ru.yandex.market.mbi.msapi.logbroker_new;

import java.time.Clock;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.market.mbi.msapi.handler.lines.JsonLineParser;
import ru.yandex.market.mbi.msapi.logbroker.MetadataRepository;
import ru.yandex.market.mbi.msapi.logbroker.ReceiveConfig;
import ru.yandex.market.mbi.msapi.logbroker.ReceiverService;
import ru.yandex.market.mbi.msapi.logbroker_new.wrappers.StreamConsumerWrapper;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author kateleb
 */
@RunWith(MockitoJUnitRunner.class)
public class LbReceiveManagerTest extends BaseReadTestUtil {

    private static final Logger log = LoggerFactory.getLogger(LbReceiverTest.class);
    private static final int CONSUME_TIMEOUT_SEC = 2;
    private static final int MIN_CHUNK_SIZE = 5;
    @Mock
    StreamConsumerFactory consumerFactory;
    @Mock
    StreamListenerFactory listenerFactory;
    @Mock
    private ReceiveConfig config;
    @Mock
    private MetadataRepository metadata;
    @Mock
    private StreamConsumerWrapper freddyTheConsumer;
    @Mock
    private LbReceiver lbreader;
    private ClicksStreamListener patricTheListener;
    private final List<String> andyTheCollector = new ArrayList<>();
    private LbReceiveManager readManager;

    @Before
    public void setup() {
        when(config.getTopic()).thenReturn(TOPIC);
        when(config.getUserLogType()).thenReturn(TEST_LOG_TYPE);
        when(config.getReceiver()).thenReturn(TEST_READER);
        when(config.isEnabled()).thenReturn(true);

        when(config.getReceiveExecutionTimeLimit()).thenReturn(CONSUME_TIMEOUT_SEC);
        when(config.getReceiveExecutionTimeUnit()).thenReturn(TimeUnit.SECONDS);
        ReceiverService receiverService = new ReceiverService(metadata, new JsonLineParser());
        readManager = new LbReceiveManager(config, receiverService, getLineHandlerFactories(andyTheCollector), metadata,
                consumerFactory, listenerFactory);

        when(consumerFactory.get()).thenReturn(freddyTheConsumer);
        when(lbreader.getConfig()).thenReturn(config);
        when(lbreader.getConsumer()).thenReturn(freddyTheConsumer);
        when(lbreader.getReadManager()).thenReturn(readManager);
        when(lbreader.getLineHandlers()).thenReturn(Collections.singletonList(lineHandler(andyTheCollector)));
        patricTheListener = new ClicksStreamListener(lbreader, metadata, receiverService,
                100, Clock.fixed(FIXED_TIME, ZoneId.systemDefault()), MIN_CHUNK_SIZE);

        when(listenerFactory.listener(any(LbReceiver.class))).thenReturn(patricTheListener);
        when(metadata.insertNewChunk(TEST_READER, TOPIC_AND_PARTITION, 101L, TEST_LOG_TYPE, 6)).thenReturn(101L);
        when(metadata.insertNewChunk(TEST_READER, TOPIC_AND_PARTITION, 102L, TEST_LOG_TYPE, 1)).thenReturn(102L);
    }


    @Test
    public void testManagerReceive() throws InterruptedException {

        //Запускаем receive на 2 секунды
        ExecutorService executor = Executors.newCachedThreadPool();
        executor.execute(() -> readManager.receive());

        //Пришли какие-то данные
        patricTheListener.onRead(readData(OK_CHUNK_6_LINES), () -> log.info("Committed!"));
        //Эти данные придут, но не обработаются, а останутся в буфере, поскольку чанк меньше минимального для вставки
        patricTheListener.onRead(readData(OK_CHUNK_1_LINE), () -> log.info("Committed!"));

        //Спим до момента, пока ридер не начнёт завершаться плюс еще чуть-чуть
        TimeUnit.SECONDS.sleep(CONSUME_TIMEOUT_SEC + 2);
        //сигнал от консьюмера, что он завершился - только после этого ридер закроется
        patricTheListener.onClose();

        executor.shutdown();
        executor.awaitTermination(CONSUME_TIMEOUT_SEC, TimeUnit.SECONDS);

        //Всё стартануло
        verify(consumerFactory).get();
        verify(listenerFactory).listener(any(LbReceiver.class));
        verify(freddyTheConsumer).startConsume(patricTheListener);

        //Всё красиво завершилось
        verify(lbreader).finishConsume();
        verify(freddyTheConsumer).stopConsume();
        assertTrue(patricTheListener.isClosed());

        //Данные прочитались
        assertThat(andyTheCollector.size(), is(6));
        assertThat(andyTheCollector, is(Arrays.asList("line1", "line2", "line3", "line4", "line5", "line6")));
    }


    @Test
    public void testManagerNotReceiveWhenDisabled() throws InterruptedException {

        when(config.isEnabled()).thenReturn(false);
        //Запускаем receive на 2 секунды
        ExecutorService executor = Executors.newCachedThreadPool();
        executor.execute(() -> readManager.receive());
        executor.shutdown();
        executor.awaitTermination(CONSUME_TIMEOUT_SEC, TimeUnit.SECONDS);

        verifyNoMoreInteractions(consumerFactory);
        verifyNoMoreInteractions(listenerFactory);
        verifyNoMoreInteractions(metadata);
        verifyNoMoreInteractions(freddyTheConsumer);
    }
}
