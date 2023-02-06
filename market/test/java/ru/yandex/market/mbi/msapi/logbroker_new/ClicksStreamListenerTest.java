package ru.yandex.market.mbi.msapi.logbroker_new;

import java.time.Clock;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.common.collect.ImmutableMap;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.ConsumerInitResponse;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.ConsumerReadResponse;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageBatch;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageData;
import ru.yandex.market.mbi.msapi.handler.lines.JsonLineParser;
import ru.yandex.market.mbi.msapi.logbroker.MetadataRepository;
import ru.yandex.market.mbi.msapi.logbroker.ReceiveConfig;
import ru.yandex.market.mbi.msapi.logbroker.ReceiverService;
import ru.yandex.market.mbi.msapi.logbroker_new.wrappers.StreamConsumerWrapper;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mbi.msapi.logbroker.ChunkStatus.RECEIVED;
import static ru.yandex.market.mbi.msapi.logbroker.ChunkStatus.SAVED_TO_STASH;

/**
 * @author kateleb
 */
@RunWith(MockitoJUnitRunner.class)
public class ClicksStreamListenerTest extends BaseReadTestUtil {

    private static final Logger log = LoggerFactory.getLogger(ClicksStreamListenerTest.class);
    private static final int MIN_CHUNK_SIZE = 5;

    @Mock
    private LbReceiveManager readManager;
    @Mock
    private ReceiveConfig config;
    @Mock
    private MetadataRepository metadata;
    @Mock
    private LbReceiver lbreader;
    @Mock
    private StreamConsumerWrapper freddyTheConsumer;
    private ClicksStreamListener clicksProcessor;
    private ReceiverService receiveProcessor;

    @Before
    public void setup() {
        when(config.getTopic()).thenReturn(TOPIC);
        when(config.getUserLogType()).thenReturn(TEST_LOG_TYPE);
        when(config.getReceiver()).thenReturn(TEST_READER);
        when(metadata.getLastOffsets(TEST_READER, TOPIC)).thenReturn(ImmutableMap.of(TOPIC_AND_PARTITION,
                START_OFFSET));

        when(metadata.insertNewChunk(TEST_READER, TOPIC_AND_PARTITION, 101L, TEST_LOG_TYPE, 6)).thenReturn(101L);
        when(metadata.insertNewChunk(TEST_READER, TOPIC_AND_PARTITION, 101L, TEST_LOG_TYPE, 1)).thenReturn(101L);
        when(metadata.insertNewChunk(TEST_READER, TOPIC_AND_PARTITION, 102L, TEST_LOG_TYPE, 5)).thenReturn(102L);
        when(metadata.insertNewChunk(TEST_READER, TOPIC_AND_PARTITION, 104L, TEST_LOG_TYPE, 8)).thenReturn(104L);
        when(metadata.insertNewChunk(TEST_READER, TOPIC_AND_PARTITION, 104L, TEST_LOG_TYPE, 4)).thenReturn(104L);
        when(metadata.insertNewChunk(TEST_READER, TOPIC_AND_PARTITION, 106L, TEST_LOG_TYPE, 6)).thenReturn(106L);
        when(metadata.insertNewChunk(TEST_READER, TOPIC_AND_PARTITION, 106L, TEST_LOG_TYPE, 12)).thenReturn(106L);
        when(metadata.insertNewChunk(TEST_READER, TOPIC_AND_PARTITION, 106L, TEST_LOG_TYPE, 13)).thenReturn(106L);

        when(lbreader.getConsumer()).thenReturn(freddyTheConsumer);
        when(lbreader.getConfig()).thenReturn(config);
        when(lbreader.getReadManager()).thenReturn(readManager);

        receiveProcessor = new ReceiverService(metadata, new JsonLineParser());
        clicksProcessor = new ClicksStreamListener(lbreader, metadata, receiveProcessor,
                100, Clock.fixed(FIXED_TIME, ZoneId.systemDefault()), MIN_CHUNK_SIZE);
    }

    @Test
    public void testInit() {
        clicksProcessor.onInit(new ConsumerInitResponse("session1"));
        verify(metadata).getLastOffsets(TEST_READER, TOPIC);
    }

    @Test
    public void testReceiveOkChunk() {
        clicksProcessor.onRead(readData(OK_CHUNK_6_LINES), () -> log.info("Committed!"));
        verify(metadata).insertNewChunk(TEST_READER, TOPIC_AND_PARTITION, 101, TEST_LOG_TYPE, 6);
        verify(metadata).updateChunkStatus(101, FIXED_TIME, RECEIVED, 0);
        verify(readManager).fireSuccess(101L);
        verifyNoMoreInteractions(readManager);
    }

    @Test
    public void testReceiveOkChunkWithNewLine() {
        clicksProcessor.onRead(readData(OK_CHUNK_LINE_NL), () -> log.info("Committed!"));
        verify(metadata).insertNewChunk(TEST_READER, TOPIC_AND_PARTITION, 101, TEST_LOG_TYPE, 6);
        verify(metadata).updateChunkStatus(101, FIXED_TIME, RECEIVED, 0);
        verify(readManager).fireSuccess(101L);
        verifyNoMoreInteractions(readManager);
    }


    @Test
    public void testReceiveEmptyChunk() {
        clicksProcessor.onRead(readData(EMPTY_DATA), () -> log.info("Committed!"));
        verifyNoMoreInteractions(metadata);
        verifyNoMoreInteractions(readManager);

    }

    @Test
    public void testReceiveBadChunk() {
        clicksProcessor.onRead(readData(INCORRECT_CHUNK_DATA), () -> log.info("Committed!"));
        verify(metadata).insertNewChunk(TEST_READER,
                TOPIC_AND_PARTITION, 101, TEST_LOG_TYPE, 6);
        verify(metadata).stashChunkContent(101, INCORRECT_CHUNK_DATA);
        verify(metadata).updateChunkStatus(101, FIXED_TIME, SAVED_TO_STASH, 1);
        verifyNoMoreInteractions(readManager);
    }

    @Test
    public void testReceiveSeveralBatchesWithOneError() {
        clicksProcessor.onRead(readSeveralSmallChunksWithError(), () -> log.info("Committed!"));
//        verify(metadata).insertNewChunk(TEST_READER, TOPIC_AND_PARTITION, 104, TEST_LOG_TYPE, 8);
        verify(metadata).insertNewChunk(TEST_READER, TOPIC_AND_PARTITION, 106, TEST_LOG_TYPE, 12);
        verify(metadata).stashChunkContent(106,
                OK_CHUNK_2_LINES + "\n" + ANOTHER_OK_CHUNK_1_LINE + "\n" + INCORRECT_CHUNK_DATA + "\n" +
                        "{\"rowid\":\"line6\"}\n{\"rowid\":\"line7\"}\n{\"rowid\":\"line8\"}");
        verify(metadata).updateChunkStatus(106, FIXED_TIME, SAVED_TO_STASH, 1);
//        verify(metadata).updateChunkStatus(106, FIXED_TIME, RECEIVED, 0);
        verifyNoMoreInteractions(metadata);
//        verify(readManager).fireSuccess(106L);
        verifyNoMoreInteractions(readManager);
    }

    @Test
    @Ignore
    public void testReceiveSeveralBigChunksOK() {
        clicksProcessor.onRead(readSeveralBigChunksNoError(), () -> log.info("Committed!"));
        verify(metadata).insertNewChunk(TEST_READER, TOPIC_AND_PARTITION, 102, TEST_LOG_TYPE, 5);
        verify(metadata).insertNewChunk(TEST_READER, TOPIC_AND_PARTITION, 104, TEST_LOG_TYPE, 1);
        verify(metadata).insertNewChunk(TEST_READER, TOPIC_AND_PARTITION, 106, TEST_LOG_TYPE, 3);

        verify(metadata).updateChunkStatus(102, FIXED_TIME, RECEIVED, 0);
        verify(metadata).updateChunkStatus(104, FIXED_TIME, RECEIVED, 0);
        verify(metadata).updateChunkStatus(106, FIXED_TIME, RECEIVED, 0);
        verifyNoMoreInteractions(metadata);
        verify(readManager).fireSuccess(102L);
        verify(readManager).fireSuccess(104L);
        verify(readManager).fireSuccess(106L);
        verifyNoMoreInteractions(readManager);
    }

    @Test
    public void testReceiveSeveralAccumulatableChunksOK() {
        clicksProcessor.onRead(readSeveralSmallChunksNoError(), () -> log.info("Committed!"));
        //6+1+0+1 records for 101-104 first batch
//        verify(metadata).insertNewChunk(TEST_READER, TOPIC_AND_PARTITION, 104, TEST_LOG_TYPE, 8);
        //1+4 105-106 second batch
        verify(metadata).insertNewChunk(TEST_READER, TOPIC_AND_PARTITION, 106, TEST_LOG_TYPE, 13);

//        verify(metadata).updateChunkStatus(104, FIXED_TIME, RECEIVED, 0);
        verify(metadata).updateChunkStatus(106, FIXED_TIME, RECEIVED, 0);
        verifyNoMoreInteractions(metadata);

//        verify(readManager).fireSuccess(104L);
        verify(readManager).fireSuccess(106L);
        verifyNoMoreInteractions(readManager);
    }

    @Test
    public void testReceiveOkChunkAfterStop() {
        when(lbreader.stopped()).thenReturn(true);
        clicksProcessor.onRead(readData(OK_CHUNK_6_LINES), () -> log.info("Committed!"));
        verifyNoMoreInteractions(metadata);
        verifyNoMoreInteractions(readManager);
    }

    @Test
    public void testReceiveOkChunkButSomeError() {
        clicksProcessor.onRead(readData(OK_CHUNK_6_LINES), () -> {
            throw new IllegalArgumentException("I'm your sudden error from Logbroker!");
        });
        verify(lbreader).stop();
    }

    private ConsumerReadResponse readSeveralSmallChunksWithError() {
        long offset = START_OFFSET;
        List<MessageData> chunks = new ArrayList<>();
        //good chunk1 101
        chunks.add(new MessageData((OK_CHUNK_2_LINES).getBytes(), ++offset, meta(offset)));
        //good chunk2 102
        chunks.add(new MessageData(ANOTHER_OK_CHUNK_1_LINE.getBytes(), ++offset, meta(offset)));
        //empty chunk 103
        chunks.add(new MessageData(EMPTY_DATA.getBytes(), ++offset, meta(offset)));
        //unparseable chunk 104
        chunks.add(new MessageData(INCORRECT_CHUNK_DATA.getBytes(), ++offset, meta(offset)));

        List<MessageData> moreChunks = new ArrayList<>();
        //good chunk3 plus \n 105
        moreChunks.add(new MessageData("{\"rowid\":\"line6\"}\n".getBytes(), ++offset, meta(offset)));
        //good chunk4 106
        moreChunks.add(new MessageData("{\"rowid\":\"line7\"}\n{\"rowid\":\"line8\"}".getBytes(),
                ++offset, meta(offset)));

        List<MessageBatch> batches = new ArrayList<>();
        batches.add(new MessageBatch(TOPIC, PARTITION, chunks));
        batches.add(new MessageBatch(TOPIC, PARTITION, moreChunks));

        return new ConsumerReadResponse(batches, 1);
    }

    private ConsumerReadResponse readSeveralBigChunksNoError() {
        long offset = START_OFFSET;
        List<MessageData> chunks = new ArrayList<>();
        //good chunk1 101
        chunks.add(new MessageData((OK_CHUNK_6_LINES).getBytes(), ++offset, meta(offset)));
        //good chunk2 102
        chunks.add(new MessageData(ANOTHER_OK_CHUNK_1_LINE.getBytes(), ++offset, meta(offset)));
        //empty chunk 103
        chunks.add(new MessageData(EMPTY_DATA.getBytes(), ++offset, meta(offset)));
        //good chunk 104
        chunks.add(new MessageData(OK_CHUNK_1_LINE.getBytes(), ++offset, meta(offset)));

        List<MessageData> moreChunks = new ArrayList<>();
        //good chunk3 plus \n 105
        moreChunks.add(new MessageData("{\"rowid\":\"line6\"}\n".getBytes(), ++offset, meta(offset)));
        //good chunk4 106
        moreChunks.add(new MessageData("{\"rowid\":\"line7\"}\n{\"rowid\":\"line8\"}".getBytes(),
                ++offset, meta(offset)));

        List<MessageBatch> batches = new ArrayList<>();
        batches.add(new MessageBatch(TOPIC, PARTITION, chunks));
        batches.add(new MessageBatch(TOPIC, PARTITION, moreChunks));

        return new ConsumerReadResponse(batches, 1);
    }

    private ConsumerReadResponse readSeveralSmallChunksNoError() {
        long offset = START_OFFSET;
        List<MessageData> chunks = new ArrayList<>();
        //good chunk1 101
        chunks.add(new MessageData((OK_CHUNK_6_LINES).getBytes(), ++offset, meta(offset)));
        //good chunk2 102
        chunks.add(new MessageData(ANOTHER_OK_CHUNK_1_LINE.getBytes(), ++offset, meta(offset)));
        //empty chunk 103
        chunks.add(new MessageData(EMPTY_DATA.getBytes(), ++offset, meta(offset)));
        //good chunk 104
        chunks.add(new MessageData(OK_CHUNK_1_LINE.getBytes(), ++offset, meta(offset)));

        List<MessageData> moreChunks = new ArrayList<>();
        //good chunk3 plus \n 105
        moreChunks.add(new MessageData("{\"rowid\":\"line6\"}\n".getBytes(), ++offset, meta(offset)));
        //good chunk4 106
        moreChunks.add(new MessageData(("{\"rowid\":\"line7\"}\n{\"rowid\":\"line8\"}\n{\"rowid\":\"line9\"}\n" +
                "{\"rowid\":\"line10\"}").getBytes(),
                ++offset, meta(offset)));

        List<MessageBatch> batches = new ArrayList<>();
        batches.add(new MessageBatch(TOPIC, PARTITION, chunks));
        batches.add(new MessageBatch(TOPIC, PARTITION, moreChunks));

        return new ConsumerReadResponse(batches, 1);
    }


    @Test
    public void splitWithNL() {
        String line = "a\nb\nc\n\n";
        Assert.assertEquals(receiveProcessor.splitIntoLines(line).size(), 3);
        Assert.assertThat(receiveProcessor.splitIntoLines(line), CoreMatchers.equalTo(Arrays.asList("a", "b", "c")));
    }
}
