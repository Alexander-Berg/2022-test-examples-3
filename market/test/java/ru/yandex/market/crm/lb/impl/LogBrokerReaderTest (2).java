package ru.yandex.market.crm.lb.impl;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;

import com.google.common.collect.Lists;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.crm.environment.Environment;
import ru.yandex.market.crm.lb.LBInstallation;
import ru.yandex.market.crm.lb.LogBrokerMessageConsumer;
import ru.yandex.market.crm.lb.LogIdentifier;
import ru.yandex.market.crm.lb.ReaderConfig;
import ru.yandex.market.crm.lb.SessionFactory;
import ru.yandex.market.crm.lb.dao.PartitionDao;
import ru.yandex.market.crm.lb.domain.LogBrokerOffset;
import ru.yandex.market.crm.lb.test.LBConsumerStub;
import ru.yandex.market.crm.lb.test.Message;
import ru.yandex.market.crm.lb.test.ReadingSessionStubFactory;
import ru.yandex.market.crm.lb.test.StreamConsumerStub;
import ru.yandex.market.crm.lb.test.TestLBReaderConfig;
import ru.yandex.market.crm.lb.tx.EmptyTxStrategy;
import ru.yandex.market.crm.lb.tx.SingleTxStrategy;
import ru.yandex.market.crm.lb.tx.TxStrategy;
import ru.yandex.market.crm.util.CrmStrings;
import ru.yandex.market.crm.util.CurrentDc;
import ru.yandex.market.jmf.background.BackgroundService;
import ru.yandex.market.jmf.db.Constants;
import ru.yandex.market.jmf.db.test.DbTestTool;
import ru.yandex.market.jmf.handshake.HandshakeDao;
import ru.yandex.market.jmf.handshake.HandshakeService;
import ru.yandex.market.jmf.lock.LockService;
import ru.yandex.misc.thread.ThreadUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author apershukov
 */
@SpringJUnitConfig(classes = TestLBReaderConfig.class)
public class LogBrokerReaderTest {

    private static final Logger LOG = LoggerFactory.getLogger(LogBrokerReaderTest.class);
    private static final LogIdentifier TEST_LOG = new LogIdentifier("test-ident", "test-type",
            LBInstallation.LOGBROKER);
    private static final String CLIENT_ID = "test-acc/test-lb-reader";
    private static final String ANOTHER_CLIENT_ID = "test-acc/test-lb-new-reader";

    @Inject
    private DbTestTool dbTestTool;
    @Inject
    private PartitionDao partitionDao;
    @Inject
    @Named(Constants.DEFAULT_JDBC_TEMPLATE)
    private JdbcTemplate jdbcTemplate;
    @Inject
    private LockService lockService;
    @Inject
    private HandshakeDao handshakeDao;
    @Inject
    private HandshakeService handshakeService;
    @Inject
    private BackgroundService backgroundService;
    @Inject
    private TransactionTemplate txTemplate;
    @Inject
    private ReaderConfig readerConfig;

    private LogBrokerReader reader;
    private LogBrokerReader reader2;
    private StreamConsumerStub streamConsumerStub;

    private static String partition(String dc, int number) {
        return "rt3." + dc + "--" + LogBrokerReaderTest.TEST_LOG + ":" + number;
    }

    private static String partition(int number) {
        return partition(CurrentDc.get(), number);
    }

    private static void assertConsumed(String content, LBConsumerStub consumer) throws Exception {
        byte[] message = consumer.pollConsumedBatch();
        assertNotNull(message, "No message with content '" + content + '\'');
        assertEquals(content, CrmStrings.valueOf(message));
    }

    private static void assertNoMoreMessages(LBConsumerStub consumer) throws Exception {
        byte[] content = consumer.getConsumedBatches().poll(2, TimeUnit.SECONDS);
        String contentAsString = CrmStrings.valueOf(content);

        assertNull(
                content,
                "Consumer has accepted another message: " + contentAsString
        );
    }

    @BeforeEach
    public void setUp() {
        streamConsumerStub = new StreamConsumerStub();
    }

    @AfterEach
    public void tearDown() {
        if (reader != null) {
            reader.shutdownNow();
        }
        if (reader2 != null) {
            reader2.shutdownNow();
        }

        dbTestTool.clearDatabase();
        dbTestTool.execute("DROP TABLE IF EXISTS processing_results");
    }

    @Test
    public void testReadMultiplePartitionsBySingleConsumer() throws InterruptedException {
        LBConsumerStub consumer = new LBConsumerStub(TEST_LOG);
        startReader(consumer);

        String partition1 = partition(0);
        String partition2 = partition(1);

        streamConsumerStub.sendLock(partition1, 11);
        streamConsumerStub.sendLock(partition2, 21);

        streamConsumerStub.assertLocked(partition1, 11);
        streamConsumerStub.assertLocked(partition2, 21);

        assertFalse(lockService.tryLock(lockKey(partition1)));
        assertFalse(lockService.tryLock(lockKey(partition2)));

        Message[] messages1 = IntStream.rangeClosed(10, 20)
                .mapToObj(i -> new Message("message-1-" + i, i + 1))
                .toArray(Message[]::new);

        streamConsumerStub.sendData(partition1, messages1);

        Message[] messages2 = IntStream.rangeClosed(20, 30)
                .mapToObj(i -> new Message("message-2-" + i, i + 1))
                .toArray(Message[]::new);

        streamConsumerStub.sendData(partition2, messages2);

        assertNotNull(consumer.pollConsumedBatch());
        assertNotNull(consumer.pollConsumedBatch());

        assertConsumerOffset(20, partition1, consumer.getId());
        assertConsumerOffset(30, partition2, consumer.getId());
    }

    @Test
    public void testReadSinglePartitionByMultipleReaders() throws InterruptedException {
        LBConsumerStub marla = new LBConsumerStub("marla-consumer", TEST_LOG);
        LBConsumerStub tayler = new LBConsumerStub("tayler-consumer", TEST_LOG);
        StreamConsumerStub firstStreamConsumer = new StreamConsumerStub();
        StreamConsumerStub secondStreamConsumer = new StreamConsumerStub();
        reader = startReader(firstStreamConsumer, CLIENT_ID, marla);
        reader2 = startReader(secondStreamConsumer, ANOTHER_CLIENT_ID, tayler);

        String partition = partition(0);

        firstStreamConsumer.sendLock(partition, 10);
        firstStreamConsumer.assertLocked(partition, 10);
        assertFalse(lockService.tryLock(lockKey(partition, CLIENT_ID)));

        Message[] messages1 = IntStream.rangeClosed(1, 10)
                .mapToObj(i -> new Message("Message for Marla " + i, i + 10))
                .toArray(Message[]::new);

        firstStreamConsumer.sendData(partition, messages1);

        secondStreamConsumer.sendLock(partition, 20);
        secondStreamConsumer.assertLocked(partition, 20);
        assertFalse(lockService.tryLock(lockKey(partition, ANOTHER_CLIENT_ID)));

        Message[] messages2 = IntStream.rangeClosed(1, 10)
                .mapToObj(i -> new Message("Message for Tayler " + i, i + 20))
                .toArray(Message[]::new);

        secondStreamConsumer.sendData(partition, messages2);

        assertThat(CrmStrings.valueOf(marla.pollConsumedBatch()), is("Message for Marla 1"));
        assertThat(CrmStrings.valueOf(tayler.pollConsumedBatch()), is("Message for Tayler 1"));

        assertConsumerOffset(20, partition, marla.getId());
        assertConsumerOffset(30, partition, tayler.getId());
    }

    /**
     * ?? javadoc ?? {@link MessageDispenser} ????????????????:
     * ?? ???????????? ???????? ?? ???????????????? ???????????? ?????????? ???? ?????????? ?????????????????? ???????????????????? ???????????? ?????????? ???????????????????????? ????????????????????
     * ?????????? ?????? ??????????????????, ???????????????? ???? ?????? ????????.
     * ???????????? ?????? ?????? ???? ????????????????
     * ???????? ???????? ???? ?????????? ?????????????????? (???? ???????????? ?????? ????????-???? ???? ??????????????):
     * {@link MessageDispenserTest#testForceFlushInterval()}
     */
    @Disabled
    @Test
    public void testIncompleteBatchForciblySendsToConsumer() throws InterruptedException {
        int numberOfMessages = 2;
        int safeIntervalSize = numberOfMessages + 1;

        readerConfig = new ReaderConfig(
                readerConfig.isEnabled(),
                readerConfig.getWorkersPoolSize(),
                safeIntervalSize,
                readerConfig.getErrorDelay(),
                readerConfig.getBufferingCoefficient(),
                readerConfig.getInstallation()
        );
        LBConsumerStub consumer = new LBConsumerStub(TEST_LOG);
        startReader(consumer);

        String partition = partition(0);

        streamConsumerStub.sendLock(partition, numberOfMessages + 1);
        streamConsumerStub.assertLocked(partition, numberOfMessages + 1);
        assertFalse(lockService.tryLock(lockKey(partition, CLIENT_ID)));

        Message[] messages1 = IntStream.rangeClosed(1, numberOfMessages)
                .mapToObj(i -> new Message("Message-" + i, i + 1))
                .toArray(Message[]::new);

        streamConsumerStub.sendData(partition, messages1);

        assertNotNull(consumer.pollConsumedBatch(MessageProcessor.FORCE_FLUSHING_INTERVAL, TimeUnit.MILLISECONDS));

        assertConsumerOffset(numberOfMessages + 1, partition, consumer.getId());
    }

    @Test
    public void testNewConsumerStartsFromLastAvailableOffset() {
        String partition = partition(0);

        LBConsumerStub consumer = new LBConsumerStub(TEST_LOG);

        startReader(consumer);

        streamConsumerStub.sendLock(partition, 1, 5);
        streamConsumerStub.assertLocked(partition, 5);
    }

    /**
     * ???????????????? ???????? ?????? ?????? ?????????????????????? ???????????????????? ?????????????? ?????????????????????? ????????????
     * ?????????????? ???????????????????????? ??????????????????????
     */
    @Test
    public void testConsiderOnlyOffsetsOfAliveConsumers() {
        String partition = partition(0);

        LBConsumerStub consumer = new LBConsumerStub(TEST_LOG);

        saveOffset(partition, consumer.getId(), 15);
        saveOffset(partition, "some--dead--consumer", 5);

        startReader(consumer);

        streamConsumerStub.sendLock(partition, 1, 10);
        streamConsumerStub.assertLocked(partition, 16);
    }

    @Test
    public void testCatchUpFollowingConsumer() throws Exception {
        String partition = partition(0);

        LBConsumerStub consumer1 = new LBConsumerStub("consumer-1", TEST_LOG);
        saveOffset(partition, consumer1.getId(), 10);

        LBConsumerStub consumer2 = new LBConsumerStub("consumer-2", TEST_LOG);
        saveOffset(partition, consumer2.getId(), 11);

        startReader(consumer1, consumer2);

        streamConsumerStub.sendLock(partition, 5);
        streamConsumerStub.assertLocked(partition, 11);

        Message[] messages = IntStream.rangeClosed(1, 10)
                .mapToObj(i -> new Message("message-" + i, i + 10))
                .toArray(Message[]::new);

        streamConsumerStub.sendData(partition, messages);

        assertConsumed(messages[0].getContent(), consumer1);
        assertConsumed(messages[1].getContent(), consumer2);

        assertConsumerOffset(20, partition, consumer1.getId());
        assertConsumerOffset(20, partition, consumer2.getId());
    }

    /**
     * ???????????????? ?????????????????? ???????????? ?????? ?????????????????????????? ???????????????????? postgres
     * <p>
     * ???????? ?????? ????????????????????. ???????? ???? ?????? ???????????? ?????????????? ?????????????????? ??????????.
     * <p>
     * ?????????????????? ??????:
     * 1. ???????????? ?? ???????????? ???????????? ???????????????????? ?????????? ???? ???????????????? ???? ??????????????????
     * ???????????? ???????????? ??????????????????????. ?????? ?????????????????? ?????????????? ?? ?????????????????????? ??????????????
     * ??????????????????
     * 2. ?? ???????????????????? ???????????????????? ?????????? ???????????? ?????????????????????? ???????????? ???? ?????????????????? ??
     * ?????????????? ?????????? ???? ?????????? ???? ?????? ???? ??????????????????.
     * 3. ?????????????????? ?????????????????? ???????????? ???????????? ?????????????????? ??????????. ???????????? ???? ????????????????????.
     */
    @Test
    public void testStopConsumerOnError() throws Exception {
        String partition = partition(0);

        LBConsumerStub consumer1 = new LBConsumerStub("consumer-1", TEST_LOG);
        LBConsumerStub consumer2 = new LBConsumerStub("consumer-2", TEST_LOG) {

            @Override
            public void accept(List<byte[]> messages) {
                for (byte[] message : messages) {
                    if ("message-2".equalsIgnoreCase(CrmStrings.valueOf(message))) {
                        throw new RuntimeException("Ups! I've failed.");
                    }
                    super.accept(Collections.singletonList(message));
                }
            }
        };
        LBConsumerStub consumer3 = new LBConsumerStub("consumer-3", TEST_LOG);

        startReader(new EmptyTxStrategy(), consumer1, consumer2, consumer3);
        streamConsumerStub.sendLock(partition, 5);

        Message[] messages = IntStream.rangeClosed(1, 10)
                .mapToObj(i -> new Message("message-" + i, i))
                .toArray(Message[]::new);

        streamConsumerStub.sendData(partition, messages);

        assertConsumed(messages[0].getContent(), consumer1);
        assertConsumed(messages[1].getContent(), consumer1);
        assertConsumed(messages[2].getContent(), consumer1);

        assertConsumed(messages[0].getContent(), consumer2);
        assertNoMoreMessages(consumer2);

        assertConsumed(messages[0].getContent(), consumer3);
        assertConsumed(messages[1].getContent(), consumer3);
        assertConsumed(messages[2].getContent(), consumer3);

        assertConsumerOffset(10, partition, consumer1.getId());
        assertConsumerOffset(-1, partition, consumer2.getId());
        assertConsumerOffset(10, partition, consumer3.getId());
    }

    @Test
    public void testReadPartitionByFixedIntervals() throws Exception {
        String partition = partition(0);

        LBConsumerStub consumer = new LBConsumerStub("consumer", TEST_LOG);
        saveOffset(partition, consumer.getId(), 0);
        startReader(consumer);

        streamConsumerStub.sendLock(partition, 0);

        Message[] messages = IntStream.range(1, 13)
                .mapToObj(i -> new Message("Message-" + i, i))
                .toArray(Message[]::new);

        for (Message message : messages) {
            streamConsumerStub.sendData(partition, message);
        }

        assertConsumerOffset(10, partition, consumer.getId());
        for (int i = 0; i < 10; ++i) {
            assertConsumed(messages[i].getContent(), consumer);
        }
        assertNoMoreMessages(consumer);

        streamConsumerStub.assertCommited(partition, 10);
    }

    @Test
    public void testIgnoreCrossDCTopics() throws InterruptedException {
        String anotherDC = "unk";
        handshakeDao.addHandshake("anotherHost", anotherDC, "default");

        for (int i = 0; i < 10; ++i) {
            if (handshakeService.getActiveDC().contains(anotherDC)) {
                break;
            }
            Thread.sleep(500);
        }

        String partition1 = partition(CurrentDc.get(), 0);
        String partition2 = partition(anotherDC, 0);

        LBConsumerStub consumer = new LBConsumerStub(TEST_LOG);

        startReader(consumer);

        streamConsumerStub.sendLock(partition1, 100);
        streamConsumerStub.sendLock(partition2, 100);

        streamConsumerStub.assertLocked(partition1, 100);
        streamConsumerStub.assertNotLocked(partition2);
    }

    @Test
    public void testReadTopicFromDcWithDisabledInstance() {
        String anotherDC = "unk";

        jdbcTemplate.update(
                "INSERT INTO lb_active_readers (dc, last_load_ts)\n" +
                        "VALUES (?, ?)",
                anotherDC,
                LocalDateTime.now().minus(10, ChronoUnit.MINUTES)
        );

        String partition = partition(anotherDC, 0);

        LBConsumerStub consumer = new LBConsumerStub(TEST_LOG);

        startReader(consumer);

        streamConsumerStub.sendLock(partition, 100);
        streamConsumerStub.assertLocked(partition, 100);
    }

    @Disabled
    @Test
    public void testReleaseTakenLocksOnSessionFailure() throws InterruptedException {
        String partition = partition(0);

        LBConsumerStub consumer = new LBConsumerStub(TEST_LOG);

        startReader(consumer);
        streamConsumerStub.sendLock(partition, 100);
        streamConsumerStub.assertLocked(partition, 100);

        Message message = new Message("message-1", 11);
        streamConsumerStub.sendData(partition, message);

        streamConsumerStub.sendError();

        assertConsumerOffset(11, partition, consumer.getId());

        assertTrue(lockService.tryLock(partition));
    }

    /**
     * ???????????????????????? ?????????????????? ???????????? ?????? ???????????? ?? ???????????????????????????? ???????????????????? postgres
     * ?????????????????? ?????? ?? ???????????? ???????????? ?????? ?????????????????? ?????????? ?????????????????? ?????????????? ???????????? ??????????????
     * ?? ???? ????????????????????????
     */
    @Test
    public void testTransactionMessageProcessing() throws Exception {
        String partition = partition(0);

        DbWritingConsumer consumer = new DbWritingConsumer(2);
        saveOffset(partition, consumer.getId(), 9);

        startReader(consumer);

        streamConsumerStub.sendLock(partition, 10);

        streamConsumerStub.sendData(
                partition,
                new Message("message-1", 10),
                new Message("message-2", 11),
                new Message("message-3", 12)
        );

        consumer.waitTransformed(3);

        streamConsumerStub.sendRelease(partition);

        assertConsumerOffset(9, partition, consumer.getId());
        assertNoMoreSavedResults(0);
    }

    /**
     * ???????????????????????? ???????? ?????? ?? ???????????? ???????????? ?? ???????????????? ?????????????????? ?????????????????????? ??????????????????
     * ???????????????????? ????????????, ???????????????????? ?????????????????? ???????????????????? ?????????????????? ???? ?????? ???? ?????????? ??????????????????????
     */
    @Test
    public void testSafeIntervals() throws Exception {
        String partition = partition(0);

        DbWritingConsumer consumer = new DbWritingConsumer(15);
        saveOffset(partition, consumer.getId(), 9);

        startReader(consumer);

        streamConsumerStub.sendLock(partition, 10);

        Message[] messages = IntStream.range(10, 31)
                .mapToObj(x -> new Message("Message-" + x, x))
                .toArray(Message[]::new);

        streamConsumerStub.sendData(partition, messages);

        consumer.waitTransformed(20);

        streamConsumerStub.sendRelease(partition);

        assertConsumerOffset(19, partition, consumer.getId());

        // ???????????? ?????????????????? ?????????????????? ?? ?????????????? ?????????????????????? ???? ?????? ????????????
        assertNoMoreSavedResults(20);
    }

    /**
     * ?? ???????????? ???????? ?? ???????????????? ?????????????????? ?????????????????? ?????????? ?????????????????? ????????????
     * ???????????? ???????? ?????????? ?????????? ?????????????????????? ???? ?????? ?????? ???????? ?????? ???? ??????????
     * ?????????????? ????????????????????
     */
    @Test
    public void testRetryFailedProcessings() throws InterruptedException {
        ensureResultsTable();

        String partition = partition(0);

        LBConsumerStub consumer = new LBConsumerStub(TEST_LOG) {

            private boolean failed = false;

            @Override
            public void accept(List<byte[]> messages) {
                messages.stream()
                        .map(String::new)
                        .forEach(value -> {
                            insertString(value);

                            if (!failed && "message-15".equals(value)) {
                                failed = true;
                                throw new RuntimeException("I've failed");
                            }
                        });
            }
        };

        saveOffset(partition, consumer.getId(), 10);

        startReader(consumer);

        streamConsumerStub.sendLock(partition, 11);

        Message[] messages = IntStream.range(0, 20)
                .mapToObj(i -> new Message("message-" + i, 11 + i))
                .toArray(Message[]::new);

        Lists.partition(Arrays.asList(messages), 10).stream()
                .map(x -> x.toArray(new Message[0]))
                .forEach(x -> streamConsumerStub.sendData(partition, x));

        ThreadUtils.sleep(1, TimeUnit.SECONDS);

        assertConsumerOffset(30, partition, consumer.getId());
        assertNoMoreSavedResults(20);
    }

    @Test
    public void testCommitCookiesEvenIfNoDataWasProcessed() throws InterruptedException {
        String partition = partition(0);

        LBConsumerStub consumer = new LBConsumerStub(TEST_LOG) {

            @Override
            public List<byte[]> transform(byte[] message) {
                return Collections.emptyList();
            }
        };

        saveOffset(partition, consumer.getId(), 10);

        startReader(consumer);

        streamConsumerStub.sendLock(partition, 11);

        IntStream.range(0, 15)
                .mapToObj(i -> new Message("message-" + i, 11 + i))
                .forEach(message -> streamConsumerStub.sendData(partition, message));

        assertConsumerOffset(20, partition, consumer.getId());
        streamConsumerStub.assertCommited(partition, 10);
    }

    private void saveOffset(String partition, String id, int offset) {
        partitionDao.setOffset(id, partition, offset);
    }

    private void assertNoMoreSavedResults(int bound) throws InterruptedException {
        for (int i = 0; i < 3; ++i) {
            int entriesCount = jdbcTemplate.queryForObject(
                    "SELECT count(*) as c FROM processing_results",
                    Integer.class
            );
            if (entriesCount > bound) {
                fail(String.format(
                        "?? ???? ???????????????? ???????????? ?????????? ?????? ??????????????????.\n" +
                                "?????????????????????? ???????????????????? ????????????????????: %s.\n" +
                                "?????????????????????? ????????????????????: %s.",
                        bound,
                        entriesCount
                ));
            }
            Thread.sleep(50);
        }
    }

    /**
     * ???????????????????????? ?????????????????? ?? ???????????????????? ???????????????? ???? ????????
     * ???????? ??????????????????, ???????????? ?????? ?????????? ??????????????????-???????????????????? ???????????????????? ?? ???????????????? ?? ?????????????? 10 ????????????,
     * ?? ?????????? ?????? ???????????????? 3 ????????, ?? ?? ?????????? ???????? ?????????? ?????????? ????????????
     */
    @Disabled("Takes 1 minute")
    @Test
    public void testTurnReaderOnAndOff() throws Exception {
        String partition = partition(0);
        LBConsumerStub consumer = new LBConsumerStub(TEST_LOG);

        reader = createReader(streamConsumerStub, consumer);
        TestLBReadersManager turnOn = new TestLBReadersManager(false, reader, backgroundService);
        turnOn.start();
        assertFalse(reader.isStarted(), "Reader expected not to be started");

        turnOnReaderAssertStarted(turnOn);

        readMessageCheckOffset(partition, consumer, 10, "Message-1");
        turnOffReaderAssertNotStarted(turnOn);

        turnOnReaderAssertStarted(turnOn);
        readMessageCheckOffset(partition, consumer, 20, "Message-2");
    }

    private void readMessageCheckOffset(String partition,
                                        LBConsumerStub consumer,
                                        int offset, String message) throws Exception {
        streamConsumerStub.sendLock(partition, offset);
        streamConsumerStub.sendData(partition, new Message(message, offset));
        streamConsumerStub.sendRelease(partition);
        assertThat(IOUtils.toString(consumer.pollConsumedBatch(), "UTF-8"), is(message));
        assertConsumerOffset(offset, partition, consumer.getId());
    }

    private void turnOffReaderAssertNotStarted(TestLBReadersManager turnOn) throws InterruptedException {
        //???????????????? ????????????????
        turnOn.setTurnOn(false);
        Thread.sleep(15000); // watchdog ?????????????????? ???????????????? ???????????? 10 ????????????

        //????????????????, ?????? ???? ????????????????
        assertFalse(reader.isStarted(), "Reader started but expected to be turned off");
        streamConsumerStub.assertNotStarted();
    }

    private void turnOnReaderAssertStarted(TestLBReadersManager turnOn) throws InterruptedException {
        turnOn.setTurnOn(true);
        Thread.sleep(15000); // watchdog ?????????????????? ???????????????? ???????????? 10 ????????????

        //????????????????, ?????? ????????c????????????
        assertTrue(reader.isStarted(), "Reader not started");
        streamConsumerStub.assertStarted();
    }

    @Test
    public void testReadNoPartitionByOtherInstallationConsumer() throws InterruptedException {
        LBConsumerStub consumer = new LBConsumerStub(new LogIdentifier("test-ident", "test-type", LBInstallation.LBKX));
        startReader(consumer);

        streamConsumerStub.assertNotStarted();
    }

    private void startReader(LogBrokerMessageConsumer... consumers) {
        startReader(new SingleTxStrategy(txTemplate), consumers);
    }

    private void startReader(TxStrategy strategy, LogBrokerMessageConsumer... consumers) {
        reader = createReader(strategy, streamConsumerStub, CLIENT_ID, consumers);
        reader.start();
    }

    private LogBrokerReader startReader(StreamConsumerStub streamConsumer,
                                        String clientId,
                                        LogBrokerMessageConsumer... consumers) {
        LogBrokerReader reader = createReader(new SingleTxStrategy(txTemplate), streamConsumer, clientId, consumers);
        reader.start();
        return reader;
    }


    private LogBrokerReader createReader(StreamConsumerStub streamConsumer, LogBrokerMessageConsumer... consumers) {
        return createReader(new SingleTxStrategy(txTemplate), streamConsumer, LogBrokerReaderTest.CLIENT_ID, consumers);
    }

    private LogBrokerReader createReader(TxStrategy strategy,
                                         StreamConsumerStub streamConsumer,
                                         String clientId,
                                         LogBrokerMessageConsumer... consumers) {
        SessionFactory sessionFactory = new ReadingSessionStubFactory(
                handshakeService,
                lockService,
                partitionDao,
                streamConsumer
        );

        MessageProcessor messageProcessor = new MessageProcessor(
                readerConfig,
                partitionDao,
                List.of(consumers),
                strategy,
                new LBLogger(Environment.INTEGRATION_TEST),
                clientId
        );

        return new LogBrokerReader(
                Arrays.asList(consumers),
                backgroundService,
                sessionFactory,
                messageProcessor,
                clientId,
                LBInstallation.LOGBROKER
        );
    }

    private void assertConsumerOffset(long expectedOffset, String partition, String consumerId) throws InterruptedException {
        long actualOffset = 0;
        for (int i = 0; i < 30; ++i) {
            TimeUnit.MILLISECONDS.sleep(500);

            actualOffset = partitionDao.get(partition).stream()
                    .filter(x -> consumerId.equals(x.getConsumerId()))
                    .findFirst()
                    .map(LogBrokerOffset::getOffset).orElse(-1L);

            if (expectedOffset == actualOffset) {
                return;
            }
        }
        throw new AssertionError("Invalid saved offset. Actual offset: " + actualOffset);
    }

    private void ensureResultsTable() {
        dbTestTool.execute(
                "CREATE TABLE IF NOT EXISTS processing_results (\n" +
                        "    message VARCHAR NOT NULL\n" +
                        ")"
        );
    }

    private void insertString(String value) {
        jdbcTemplate.update(
                "INSERT INTO processing_results (message)\n" +
                        "VALUES (?)",
                value
        );
    }

    private String lockKey(String partitionId) {
        return lockKey(partitionId, CLIENT_ID);
    }

    private String lockKey(String partitionId, String client) {
        return "lb_reader#" + client + "#" + partitionId;
    }

    private static class TestLBReadersManager extends LBReadersManager {
        private Boolean initialValue;

        TestLBReadersManager(Boolean initialValue, LogBrokerReader reader, BackgroundService backgroundService) {
            super(Collections.singletonList(reader), backgroundService, 10);
            this.initialValue = initialValue;
        }

        void setTurnOn(boolean turnOn) {
            LOG.info("Set reader enabled to " + turnOn);
            initialValue = turnOn;
        }

        @Override
        protected boolean lbReadersEnabled() {
            LOG.info("Check reader enabled: " + initialValue);
            return initialValue;
        }
    }

    private class DbWritingConsumer implements LogBrokerMessageConsumer<byte[]> {

        private final int failOnMessage;
        private final AtomicInteger transformedCount = new AtomicInteger(0);
        private int processedCount = 0;

        DbWritingConsumer(int failOnMessage) {
            this.failOnMessage = failOnMessage;
            ensureResultsTable();
        }

        @Override
        public void accept(List<byte[]> messages) {
            for (byte[] message : messages) {
                String value = new String(message, StandardCharsets.UTF_8);
                insertString(value + "-1");

                if (processedCount >= failOnMessage) {
                    throw new RuntimeException("Task failed");
                }

                insertString(value + "-2");
                ++processedCount;
            }
        }

        @Nonnull
        @Override
        public String getId() {
            return "DbWritingConsumer";
        }

        @Override
        public List<byte[]> transform(byte[] message) {
            transformedCount.incrementAndGet();
            return Collections.singletonList(message);
        }

        @Nonnull
        @Override
        public Set<LogIdentifier> getLogIdentifiers() {
            return Collections.singleton(TEST_LOG);
        }

        void waitTransformed(long expectedCount) {
            long startTime = System.currentTimeMillis();
            while (transformedCount.get() < expectedCount) {
                if (System.currentTimeMillis() - startTime > 2000) {
                    fail("Consumer did not transform enough messages");
                }
                ThreadUtils.sleep(500);
            }
        }
    }
}
