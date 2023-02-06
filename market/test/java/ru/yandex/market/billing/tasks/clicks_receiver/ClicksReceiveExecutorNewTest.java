package ru.yandex.market.billing.tasks.clicks_receiver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.kikimr.persqueue.compression.CompressionCodec;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.ConsumerReadResponse;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageBatch;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageData;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageMeta;
import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.billing.clicks.ChunkMetadataRepository;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.msapi.RawCopyLineFactory;
import ru.yandex.market.mbi.msapi.logbroker.ChunkStatus;
import ru.yandex.market.mbi.msapi.logbroker.ReceiveConfig;
import ru.yandex.market.mbi.msapi.logbroker.ReceiverService;
import ru.yandex.market.mbi.msapi.logbroker.RecoveryJob;
import ru.yandex.market.mbi.msapi.logbroker_new.ClicksStreamListener;
import ru.yandex.market.mbi.msapi.logbroker_new.LbReceiveManager;
import ru.yandex.market.mbi.msapi.logbroker_new.LbReceiver;
import ru.yandex.market.mbi.msapi.logbroker_new.StreamConsumerFactory;
import ru.yandex.market.mbi.msapi.logbroker_new.StreamListenerFactory;
import ru.yandex.market.mbi.msapi.logbroker_new.wrappers.StreamConsumerWrapper;
import ru.yandex.market.mbi.util.io.MbiFiles;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClicksReceiveExecutorNewTest extends FunctionalTest {

    private static final Instant FIXED_TIME = Instant.parse("2020-05-01T10:00:00Z");
    private static final Logger log = LoggerFactory.getLogger(ClicksReceiveExecutorNewTest.class);
    private static final String TOPIC = "rt3.man--marketstat-dev--market-clicks-log:0";
    private static final int PARTITION = 0;
    @Autowired
    private ReceiveConfig newLbClicksConfiguration;

    @Autowired
    private ReceiverService logBrokerReceiverService;

    @Autowired
    private RawCopyLineFactory logBrokerRawClickCopyLineFactory;

    @Autowired
    private ChunkMetadataRepository logBrokerMetadataRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private StreamConsumerWrapper freddyTheConsumer;
    private LbReceiver lbreader;
    private ClicksStreamListener patricTheListener;
    private LbReceiveManager manager;
    private StreamConsumerFactory consumerFactory;
    private StreamListenerFactory listenerFactory;

    @BeforeEach
    void init() {
        consumerFactory = mock(StreamConsumerFactory.class);
        listenerFactory = mock(StreamListenerFactory.class);
        lbreader = mock(LbReceiver.class);
        patricTheListener = mock(ClicksStreamListener.class);
        freddyTheConsumer = mock(StreamConsumerWrapper.class);

        manager = new LbReceiveManager(
                newLbClicksConfiguration,
                logBrokerReceiverService,
                Collections.singletonList(logBrokerRawClickCopyLineFactory),
                logBrokerMetadataRepository,
                consumerFactory,
                listenerFactory
        );

        when(consumerFactory.get()).thenReturn(freddyTheConsumer);
        when(lbreader.getConfig()).thenReturn(newLbClicksConfiguration);
        when(lbreader.getConsumer()).thenReturn(freddyTheConsumer);
        when(lbreader.getReadManager()).thenReturn(manager);
        when(lbreader.getLineHandlers()).thenReturn(Collections.singletonList(logBrokerRawClickCopyLineFactory.createLineHandler()));
        patricTheListener = new ClicksStreamListener(lbreader, logBrokerMetadataRepository, logBrokerReceiverService,
                100, Clock.fixed(FIXED_TIME, ZoneId.systemDefault()), 5);

        when(listenerFactory.listener(any(LbReceiver.class))).thenReturn(patricTheListener);
    }

    @Test
    @DbUnitDataSet(
            after = "ClicksReceiveExecutorTest.savingToStashedRecords.after.csv"

    )
    void savingToStashedRecordsTest() throws Exception {
        fillChunksState(ChunkStatus.SUSPENDED);
        readChunk(false, "chunkData_duplicate.txt");
    }

    @Test
    @DbUnitDataSet(
            after = "ClicksReceiveExecutorTest.savingData.after.csv"
    )
    void savingDataTest() throws Exception {
        fillChunksState(ChunkStatus.SUSPENDED);
        readChunk(false, "chunkData.txt");
    }

    @Test
    @DbUnitDataSet(after = "ClicksReceiveExecutorTest.savingData.null.promo.after.csv")
    void testCheckPromoTypeNull() throws Exception {
        readChunk(true, "chunkData_promo_null.txt");
    }

    @Test
    @DbUnitDataSet(
            before = "ClicksReceiveExecutorTest.duplicateClicks.before.csv",
            after = "ClicksReceiveExecutorTest.duplicateClicks.after.csv"
    )
    void duplicateClicksTest() throws Exception {
        fillChunksState(ChunkStatus.SUSPENDED);
        readChunk(true, "chunkData_duplicate.txt");
    }

    @Test
    @DbUnitDataSet(
            before = "ClicksReceiveExecutorTest.recovery.before.csv",
            after = "ClicksReceiveExecutorTest.recovery.after.csv"
    )
    void recoveryFromStashedRecordsTest() {
        fillChunksState(ChunkStatus.SAVED_TO_STASH);
        RecoveryJob job = new RecoveryJob(manager,
                Collections.singletonList(logBrokerRawClickCopyLineFactory.createLineHandler()));
        job.recoverFailedChunks(
                newLbClicksConfiguration.getReceiver(),
                TOPIC
        );
    }


    private void readChunk(boolean valid, String dataFilename) throws IOException, InterruptedException {
        ExecutorService executor = Executors.newCachedThreadPool();
        executor.execute(() -> manager.receive());
        patricTheListener.onRead(packData(valid, dataFilename), () -> log.info("Committed!"));
        //симуляция завершения чтения
        patricTheListener.onClose();
        when(lbreader.consumeFinished()).thenReturn(true);
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.SECONDS);

        //Всё стартануло
        verify(consumerFactory).get();
        verify(listenerFactory).listener(any(LbReceiver.class));
        verify(freddyTheConsumer).startConsume(patricTheListener);
    }

    private ConsumerReadResponse packData(boolean valid, String dataFilename) throws IOException {
        String content = MbiFiles.readText(() -> this.getClass().getResourceAsStream(dataFilename),
                StandardCharsets.UTF_8);
        if (!valid) {
            content = "invalid" + content;
        }
        List<MessageData> chunks = new ArrayList<>();
        long offset = 10L;
        chunks.add(new MessageData(content.getBytes(), offset, new MessageMeta("sid".getBytes(), offset,
                1536672270000L, Instant.now().toEpochMilli(),
                "127.0.0.1", CompressionCodec.RAW, new HashMap<>())));
        List<MessageBatch> batches = new ArrayList<>();
        batches.add(new MessageBatch(TOPIC.split(":")[0], PARTITION, chunks));
        return new ConsumerReadResponse(batches, 1);
    }

    private void fillChunksState(ChunkStatus status) {
        jdbcTemplate.update(
                "INSERT INTO WUSER.CHUNKS_STATE " +
                        "(TRANS_ID, " +
                        "RECEIVER_NAME, " +
                        "TOPIC, " +
                        "\"OFFSET\", " +
                        "USER_LOG_TYPE, " +
                        "RAW_RECORD_COUNT, " +
                        "FAIL_COUNT, " +
                        "STATUS, " +
                        "START_TIME) " +
                        "VALUES " +
                        "(wuser.s_chunks_state.nextval, " +
                        "'" + newLbClicksConfiguration.getReceiver() + "', " +
                        "'" + TOPIC + "', " +
                        "1, " +
                        "'" + newLbClicksConfiguration.getUserLogType() + "', " +
                        "5, " +
                        "1, " +
                        "?, " +
                        "sysdate)",
                status.toString());
    }
}
