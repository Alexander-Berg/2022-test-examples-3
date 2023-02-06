package ru.yandex.cs.billing.tms;

import java.io.IOException;
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
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import ru.yandex.cs.billing.AbstractCsBillingTmsFunctionalTest;
import ru.yandex.kikimr.persqueue.compression.CompressionCodec;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.ConsumerReadResponse;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageBatch;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageData;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageMeta;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.msapi.RawCopyLineFactory;
import ru.yandex.market.mbi.msapi.handler.lines.LineHandlerFactory;
import ru.yandex.market.mbi.msapi.logbroker.ChunkStatus;
import ru.yandex.market.mbi.msapi.logbroker.MetadataRepository;
import ru.yandex.market.mbi.msapi.logbroker.ReceiveConfig;
import ru.yandex.market.mbi.msapi.logbroker.ReceiverService;
import ru.yandex.market.mbi.msapi.logbroker_new.ClicksStreamListener;
import ru.yandex.market.mbi.msapi.logbroker_new.LbReceiveManager;
import ru.yandex.market.mbi.msapi.logbroker_new.LbReceiver;
import ru.yandex.market.mbi.msapi.logbroker_new.StreamConsumerFactory;
import ru.yandex.market.mbi.msapi.logbroker_new.StreamListenerFactory;
import ru.yandex.market.mbi.msapi.logbroker_new.wrappers.StreamConsumerWrapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MstatReceiveExecutorTest extends AbstractCsBillingTmsFunctionalTest {

    private static final Instant FIXED_TIME = Instant.parse("2020-05-01T10:00:00Z");
    private static final Logger log = LoggerFactory.getLogger(MstatReceiveExecutorTest.class);
    private static final String TOPIC = "rt3.man--marketstat-dev--market-clicks-log:0";
    private static final int PARTITION = 0;
    @Autowired
    private ReceiveConfig lbkxShopsClicksConfig;
    @Autowired
    private ReceiveConfig lbkxModelBidsClicksConfig;
    @Autowired
    private ReceiveConfig lbkxMarketplaceModelBidsClicksConfig;

    @Autowired
    private ReceiverService logBrokerShopsReceiveService;
    @Autowired
    private ReceiverService lbModelBidsReceiveService;
    @Autowired
    private ReceiverService lbMarketplaceModelBidsReceiveService;

    @Autowired
    private RawCopyLineFactory logBrokerShopsRawClickCopyLineFactory;
    @Autowired
    private RawCopyLineFactory logBrokerModelbidsRawClickCopyLineFactory;
    @Autowired
    private RawCopyLineFactory logBrokerMarketplaceModelbidsRawClickCopyLineFactory;

    @Autowired
    private MetadataRepository logBrokerShopsMetadataRepository;
    @Autowired
    private MetadataRepository logBrokerModelbidsMetadataRepository;
    @Autowired
    private MetadataRepository logBrokerMarketplaceModelbidsMetadataRepository;

    @Autowired
    private NamedParameterJdbcTemplate csBillingNamedParameterJdbcTemplate;

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
                lbkxShopsClicksConfig,
                logBrokerShopsReceiveService,
                Collections.singletonList(logBrokerShopsRawClickCopyLineFactory),
                logBrokerShopsMetadataRepository,
                consumerFactory,
                listenerFactory
        );

        when(consumerFactory.get()).thenReturn(freddyTheConsumer);
        when(lbreader.getConfig()).thenReturn(lbkxShopsClicksConfig);
        when(lbreader.getConsumer()).thenReturn(freddyTheConsumer);
        when(lbreader.getReadManager()).thenReturn(manager);
        when(lbreader.getLineHandlers()).thenReturn(Collections.singletonList(logBrokerShopsRawClickCopyLineFactory.createLineHandler()));
        patricTheListener = new ClicksStreamListener(lbreader, logBrokerShopsMetadataRepository,
                logBrokerShopsReceiveService,
                100, Clock.fixed(FIXED_TIME, ZoneId.systemDefault()), 5);

        when(listenerFactory.listener(any(LbReceiver.class))).thenReturn(patricTheListener);
    }

    @Test
    @DbUnitDataSet(
            after = "MstatReceiveExecutorTest/savingData.click.ok.after.csv",
            dataSource = "csBillingDataSource"
    )
    void savingValidDataTest() throws InterruptedException, IOException {
        fillChunksState(ChunkStatus.SUSPENDED, lbkxShopsClicksConfig);
        readChunk(true, "/chunkData.txt");
    }


    @Test
    @DbUnitDataSet(
            after = "MstatReceiveExecutorTest/savingData.click.fail.after.csv",
            dataSource = "csBillingDataSource"
    )
    void savingInvalidDataTest() throws InterruptedException, IOException {
        fillChunksState(ChunkStatus.SUSPENDED, lbkxShopsClicksConfig);
        readChunk(false, "/chunkData.txt");
    }

    @Test
    @DbUnitDataSet(
            before = "MstatReceiveExecutorTest/duplicateClicks.before.csv",
            after = "MstatReceiveExecutorTest/duplicateClicks.after.csv",
            dataSource = "csBillingDataSource"
    )
    void duplicateClicksTest() throws InterruptedException, IOException {
        fillChunksState(ChunkStatus.SUSPENDED, lbkxShopsClicksConfig);
        readChunk(true, "/chunkData_duplicate.txt");
    }

    @Test
    @DbUnitDataSet(
            after = "MstatReceiveExecutorTest/stashedRecords.duplicate.click.after.csv",
            dataSource = "csBillingDataSource"
    )
    void savingToStashedRecordsTest() throws InterruptedException, IOException {
        fillChunksState(ChunkStatus.SUSPENDED, lbkxShopsClicksConfig);
        readChunk(false, "/chunkData_duplicate.txt");
    }

    @Test
    @DbUnitDataSet(
            after = "MstatReceiveExecutorTest/savingData.mbid.ok.after.csv",
            dataSource = "csBillingDataSource"
    )
    void savingValidModelBidsClicks() throws IOException, InterruptedException {
        configureModelBidsManager(
                lbkxModelBidsClicksConfig,
                lbModelBidsReceiveService,
                logBrokerModelbidsRawClickCopyLineFactory,
                logBrokerModelbidsMetadataRepository
        );
        fillChunksState(ChunkStatus.SUSPENDED, lbkxModelBidsClicksConfig);
        readChunk(true, "/chunkData.txt");

    }

    @Test
    @DbUnitDataSet(
            after = "MstatReceiveExecutorTest/savingData.marketplacembid.ok.after.csv",
            dataSource = "csBillingDataSource"
    )
    void savingValidMarketplaceModelBidsClicks() throws IOException, InterruptedException {
        configureModelBidsManager(
                lbkxMarketplaceModelBidsClicksConfig,
                lbMarketplaceModelBidsReceiveService,
                logBrokerMarketplaceModelbidsRawClickCopyLineFactory,
                logBrokerMarketplaceModelbidsMetadataRepository
        );
        fillChunksState(ChunkStatus.SUSPENDED, lbkxMarketplaceModelBidsClicksConfig);
        readChunk(true, "/chunkData.txt");

    }

    private void configureModelBidsManager(ReceiveConfig config,
                                           ReceiverService receiverService,
                                           LineHandlerFactory lineFactory,
                                           MetadataRepository metadataRepository) {
        manager = new LbReceiveManager(
                config,
                receiverService,
                List.of(lineFactory),
                metadataRepository,
                consumerFactory,
                listenerFactory
        );
        when(lbreader.getConfig()).thenReturn(config);
        when(lbreader.getReadManager()).thenReturn(manager);
        when(lbreader.getLineHandlers()).thenReturn(
                List.of(lineFactory.createLineHandler()));
        patricTheListener = new ClicksStreamListener(
                lbreader,
                metadataRepository,
                receiverService,
                100, Clock.fixed(FIXED_TIME, ZoneId.systemDefault()), 5);

        when(listenerFactory.listener(any(LbReceiver.class))).thenReturn(patricTheListener);
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
        String content = getStringResource(dataFilename);
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

    private void fillChunksState(ChunkStatus status, ReceiveConfig readConfig) {
        csBillingNamedParameterJdbcTemplate.update(
                "INSERT INTO WUSER.CSBILLING_CHUNKS_STATE " +
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
                        "(wuser.S_CSBILLING_CHUNKS_STATE.nextval, " +
                        "'" + readConfig.getReceiver() + "', " +
                        "'" + TOPIC + "', " +
                        "1, " +
                        "'" + readConfig.getUserLogType() + "', " +
                        "5, " +
                        "1, " +
                        ":status, " +
                        "sysdate)",
                ImmutableMap.of("status", status.toString()));
    }
}
