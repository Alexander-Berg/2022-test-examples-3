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
import ru.yandex.market.mbi.msapi.logbroker.ChunkStatus;
import ru.yandex.market.mbi.msapi.logbroker.MetadataRepository;
import ru.yandex.market.mbi.msapi.logbroker.ReceiveConfig;
import ru.yandex.market.mbi.msapi.logbroker.ReceiverService;
import ru.yandex.market.mbi.msapi.logbroker_new.wrappers.StreamConsumerWrapper;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author kateleb
 */
@RunWith(MockitoJUnitRunner.class)
public class LbReceiverTest extends BaseReadTestUtil {

    private static final Logger log = LoggerFactory.getLogger(LbReceiverTest.class);
    private static final int MIN_CHUNK_SIZE = 5;
    private final List<String> andyTheCollector = new ArrayList<>();
    @Mock
    StreamConsumerFactory consumerFactory;
    @Mock
    StreamListenerFactory listenerFactory;
    @Mock
    private LbReceiveManager readManager;
    @Mock
    private ReceiveConfig config;
    @Mock
    private MetadataRepository metadata;
    @Mock
    private StreamConsumerWrapper freddyTheConsumer;
    private LbReceiver lbreader;
    private ClicksStreamListener clicksProcessor;

    @Before
    public void setup() {
        when(config.getTopic()).thenReturn(TOPIC);
        when(config.getUserLogType()).thenReturn(TEST_LOG_TYPE);
        when(config.getReceiver()).thenReturn(TEST_READER);
        when(consumerFactory.get()).thenReturn(freddyTheConsumer);
        when(metadata.insertNewChunk(TEST_READER, TOPIC_AND_PARTITION, 101L, TEST_LOG_TYPE, 6)).thenReturn(101L);
//        when(metadata.insertNewChunk(TEST_READER, TOPIC_AND_PARTITION, 102L, TEST_LOG_TYPE, 5)).thenReturn(102L);
        when(metadata.insertNewChunk(TEST_READER, TOPIC_AND_PARTITION, 102L, TEST_LOG_TYPE, 7)).thenReturn(102L);

        lbreader = new LbReceiver(readManager, consumerFactory, listenerFactory, config,
                Collections.singletonList(lineHandler(andyTheCollector)));
        lbreader.setConsumer(freddyTheConsumer);
        clicksProcessor = new ClicksStreamListener(lbreader, metadata, new ReceiverService(metadata,
                new JsonLineParser()), 100, Clock.fixed(FIXED_TIME, ZoneId.systemDefault()), MIN_CHUNK_SIZE
        );

        when(listenerFactory.listener(lbreader)).thenReturn(clicksProcessor);
    }

    @Test
    public void testStartReadThenStopGracefully() throws InterruptedException {
        //start receiver and wait for messages
        ExecutorService executor = Executors.newCachedThreadPool();
        executor.execute(() -> lbreader.run());
        //oh! some message came
        clicksProcessor.onRead(readData(OK_CHUNK_6_LINES), () -> log.info("Committed!"));
        //oh! another message!
        clicksProcessor.onRead(readData(OK_CHUNK_7_LINES), () -> log.info("Committed!"));
        //got stop signal from the boss
        lbreader.stop();
        //new message came to listener after reader stop, should not process it
        clicksProcessor.onRead(readData(OK_CHUNK_8_LINES), () -> log.info("Committed!"));
        //listener got stop consumer confirmation
        clicksProcessor.onClose();
        //just in case, should finish immediately
        executor.shutdown();
        executor.awaitTermination(2, TimeUnit.SECONDS);

        verify(consumerFactory).get();
        verify(listenerFactory).listener(lbreader);

        verify(metadata).insertNewChunk(TEST_READER, TOPIC_AND_PARTITION, 101L, TEST_LOG_TYPE, 6);
        verify(metadata).updateChunkStatus(101L, FIXED_TIME, ChunkStatus.RECEIVED, 0);
        verify(metadata).insertNewChunk(TEST_READER, TOPIC_AND_PARTITION, 102L, TEST_LOG_TYPE, 7);
        verify(metadata).updateChunkStatus(102L, FIXED_TIME, ChunkStatus.RECEIVED, 0);
        verify(readManager).fireSuccess(101L);
        verify(readManager).fireSuccess(102L);
        verifyNoMoreInteractions(metadata);
        verifyNoMoreInteractions(readManager);
        verify(freddyTheConsumer).stopConsume();

        assertTrue(lbreader.consumeFinished());
        assertTrue(clicksProcessor.isClosed());
        assertThat(andyTheCollector.size(), is(13));
        assertThat(andyTheCollector, is(Arrays.asList("line1", "line2", "line3", "line4", "line5", "line6",
                "line101", "line102", "line103", "line104", "line105", "line106", "line107")));
    }

    @Test
    public void testStopDueError() throws InterruptedException {
        //start receiver and wait for messages
        ExecutorService executor = Executors.newCachedThreadPool();
        executor.execute(() -> lbreader.run());
        //oh! some message came
        clicksProcessor.onRead(readData(OK_CHUNK_6_LINES), () -> log.info("Committed!"));
        //small read
        clicksProcessor.onRead(readData(OK_CHUNK_2_LINES), () -> log.info("Committed" +
                "!"));
        //oh! another message! Causing error after process!
        clicksProcessor.onRead(readData(OK_CHUNK_3_LINES), () -> {
            throw new IllegalArgumentException("I'm your sudden error from Logbroker!");
        });
        //new message came to listener after an error, should not process it
        clicksProcessor.onRead(readData(OK_CHUNK_2_LINES), () -> log.info("Committed!"));
        //listener got stop consumer confirmation
        clicksProcessor.onClose();
        //just in case, should finish immediately
        executor.shutdown();
        executor.awaitTermination(2, TimeUnit.SECONDS);

        verify(freddyTheConsumer).stopConsume();

        assertTrue(lbreader.consumeFinished());
        assertTrue(clicksProcessor.isClosed());
        assertThat(andyTheCollector.size(), is(11));
        assertThat(andyTheCollector, is(Arrays.asList("line1", "line2", "line3", "line4", "line5", "line6",
                "line8", "line9", "line11", "line12", "line13")));
    }


    @Test
    public void testStartReadSeveralChunksThenStopGracefully() throws InterruptedException {
        //start receiver and wait for messages
        ExecutorService executor = Executors.newCachedThreadPool();
        executor.execute(() -> lbreader.run());
        //oh! some message came
        clicksProcessor.onRead(readData(Arrays.asList(OK_CHUNK_6_LINES, OK_CHUNK_1_LINE)),
                () -> log.info("Committed!"));

        //got stop signal from the boss
        lbreader.stop();
        //new message came to listener after reader stop, should not process it
        clicksProcessor.onRead(readData(OK_CHUNK_7_LINES), () -> log.info("Committed!"));
        //listener got stop consumer confirmation
        clicksProcessor.onClose();
        //just in case, should finish immediately
        executor.shutdown();
        executor.awaitTermination(2, TimeUnit.SECONDS);

        verify(consumerFactory).get();
        verify(listenerFactory).listener(lbreader);

        verify(metadata).insertNewChunk(TEST_READER, TOPIC_AND_PARTITION, 102L, TEST_LOG_TYPE, 7);
        verify(metadata).updateChunkStatus(102L, FIXED_TIME, ChunkStatus.RECEIVED, 0);
        verifyNoMoreInteractions(metadata);

        verify(readManager).fireSuccess(102L);
        verifyNoMoreInteractions(readManager);

        verify(freddyTheConsumer).stopConsume();
        assertTrue(lbreader.consumeFinished());
        assertTrue(clicksProcessor.isClosed());
        assertThat(andyTheCollector.size(), is(7));
        assertThat(andyTheCollector, is(Arrays.asList("line1", "line2", "line3", "line4", "line5", "line6", "line7")));
    }

    @Test
    public void testReadSeveralChunksStopDueError() throws InterruptedException {
        //start receiver and wait for messages
        ExecutorService executor = Executors.newCachedThreadPool();
        executor.execute(() -> lbreader.run());
        //oh! some message came
        clicksProcessor.onRead(readData(Arrays.asList(OK_CHUNK_6_LINES, OK_CHUNK_1_LINE)), () -> log.info("Committed" +
                "!"));
        //small read
        clicksProcessor.onRead(readData(Collections.singletonList(OK_CHUNK_2_LINES)), () -> log.info("Committed" +
                "!"));
        //oh! another message! Causing error after process!
        clicksProcessor.onRead(readData(OK_CHUNK_3_LINES), () -> {
            throw new IllegalArgumentException("I'm your sudden error from Logbroker!");
        });
        //new message came to listener after an error, should not process it
        clicksProcessor.onRead(readData(OK_CHUNK_7_LINES), () -> log.info("Committed!"));
        //listener got stop consumer confirmation
        clicksProcessor.onClose();
        //just in case, should finish immediately
        executor.shutdown();
        executor.awaitTermination(2, TimeUnit.SECONDS);

        verify(freddyTheConsumer).stopConsume();

        assertTrue(lbreader.consumeFinished());
        assertTrue(clicksProcessor.isClosed());
        assertThat(andyTheCollector.size(), is(12));
        assertThat(andyTheCollector, is(Arrays.asList("line1", "line2", "line3", "line4", "line5", "line6", "line7",
                "line8", "line9", "line11", "line12", "line13")));
    }
}
