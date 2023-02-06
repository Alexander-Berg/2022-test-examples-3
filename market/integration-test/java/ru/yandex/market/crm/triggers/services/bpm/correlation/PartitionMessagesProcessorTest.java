package ru.yandex.market.crm.triggers.services.bpm.correlation;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.crm.core.services.jackson.JacksonConfig;
import ru.yandex.market.crm.mapreduce.domain.user.Uid;
import ru.yandex.market.crm.triggers.services.bpm.UidBpmMessage;
import ru.yandex.market.crm.triggers.services.bpm.correlation.PartitionMessagesProcessor.MessageConsumer;
import ru.yandex.market.crm.triggers.test.TestDatabaseConfig;
import ru.yandex.market.mcrm.db.test.DbTestTool;
import ru.yandex.market.mcrm.tx.TxService;
import ru.yandex.misc.thread.ThreadUtils;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.crm.triggers.services.bpm.correlation.CorrelationTestUtils.message;

/**
 * @author apershukov
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = PartitionMessagesProcessorTest.TestConfig.class)
public class PartitionMessagesProcessorTest {

    @Configuration
    @Import({
            TestDatabaseConfig.class,
            JacksonConfig.class,
            PendingMessageSerializerConfiguration.class
    })
    static class TestConfig {

        @Bean
        public PendingMessagesDAO pendingMessagesDAO(JdbcTemplate jdbcTemplate) {
            return new PendingMessagesDAO(jdbcTemplate, 1);
        }
    }

    private static class Incident {
        final String uid;
        final String message;

        Incident(String uid, String message) {
            this.uid = uid;
            this.message = message;
        }
    }

    private boolean waitCondition(Supplier<Boolean> checker) {
        long deadline = System.currentTimeMillis() + 5_000;
        while (System.currentTimeMillis() < deadline) {
            if (checker.get()) {
                return true;
            }
            ThreadUtils.sleep(500);
        }
        return false;
    }

    @Inject
    private PendingMessagesDAO pendingMessagesDAO;

    @Inject
    private DbTestTool dbTestTool;

    @Inject
    private JdbcTemplate jdbcTemplate;

    @Inject
    private TxService txService;

    @Inject
    private PendingMessageSerializer pendingMessageSerializer;

    private PartitionMessagesProcessor processor;

    @Before
    public void setUp() {
        dbTestTool.runScript("/triggers_sql/pending_messages.sql");
    }

    @After
    public void tearDown() throws Exception {
        if (processor != null) {
            processor.stop();
            processor.awaitTermination();
        }
        jdbcTemplate.update("TRUNCATE TABLE pending_messages_1 CASCADE");
    }

    /**
     * Сообщения в очереди получаются и передаются на вход в консьюмер
     */
    @Test
    public void testProcessPendingMessage() throws Exception {
        UidBpmMessage message = message();
        insertMessage(message);

        BlockingQueue<UidBpmMessage> queue = new ArrayBlockingQueue<>(1);
        processor = startProcessor(queue::put);

        message = queue.poll(5, TimeUnit.SECONDS);
        assertNotNull("No message processed", message);

        assertThat(pendingMessagesDAO.pollMessages(1), empty());
    }

    /**
     * Если сообщение, связанное с каким-либо идентификатором фейлится при обработке
     * обработка всех сообщений для этого идентификатора приостановливатся.
     */
    @Test
    public void testMessageFailStopsProcessingForAllMessagesForUser() throws Exception {
        Uid puid1 = Uid.asPuid(111L);
        generateMessages(() -> message(puid1), 20);

        Uid puid2 = Uid.asPuid(222L);
        generateMessages(() -> message(puid2), 5);

        AtomicInteger counter = new AtomicInteger(0);
        BlockingQueue<UidBpmMessage> queue = new LinkedBlockingQueue<>();
        processor = startProcessor(message -> {
            if (puid1.equals(message.getUid())) {
                System.err.println(message);
                queue.put(message);

                if (counter.getAndIncrement() == 0) {
                    throw new RuntimeException("Message processing failed");
                }
            }
        });

        var message = queue.poll(5, TimeUnit.SECONDS);
        assertNotNull("First message has not been received", message);

        message = queue.poll(2, TimeUnit.SECONDS);
        assertNull("More than one of messages for first user processed", message);

        boolean noLockedMessages = waitCondition(() -> !isLockedForId(puid1));
        assertTrue("Messages for first puid are still locked", noLockedMessages);

        List<Incident> incidents = getAllIncidents();
        assertThat(incidents, hasSize(1));

        Incident incident = incidents.get(0);
        assertEquals(puid1.toString(), incident.uid);
        assertThat(incident.message, containsString("Message processing failed"));
    }

    /**
     * Обработка сообщения происходит внутри транзакции
     */
    @Test
    public void testMessageAreProcessingInSeparateTransaction() {
        insertMessage(message());

        jdbcTemplate.update("CREATE TABLE IF NOT EXISTS dummy_table(v VARCHAR NOT NULL)");

        processor = startProcessor(message -> {
            jdbcTemplate.update("INSERT INTO dummy_table(v) VALUES (?)", "abc");
            throw new RuntimeException("Message processing error");
        });

        boolean incidentRegistered = waitCondition(() -> !getAllIncidents().isEmpty());
        assertTrue("No incident registered", incidentRegistered);

        Boolean changesSaved = jdbcTemplate.queryForObject(
                "SELECT exists(SELECT 1 FROM dummy_table)",
                Boolean.class
        );

        assertNotEquals("Failed message processing changes is not rolled back", Boolean.TRUE, changesSaved);
    }

    /**
     * При ошибке десериализации сообщение пропускается и создается инцидент
     */
    @Test
    public void testCreateIncidentOnDeserializeError() throws Exception {
        Uid puid1 = Uid.asPuid(111L);
        UidBpmMessage message1 = message(puid1);
        insertMessage(message1);

        Uid puid2 = Uid.asPuid(222L);
        UidBpmMessage message2 = message(puid2);
        PendingMessage pendingMessage = pendingMessageSerializer.serialize(message2);
        pendingMessagesDAO.insertMessages(List.of(new PendingMessage(
                pendingMessage.getUid(),
                pendingMessage.getMessage().replace("PUID", "invalid type")
        )));

        Uid puid3 = Uid.asPuid(333L);
        UidBpmMessage message3 = message(puid3);
        insertMessage(message3);

        BlockingQueue<UidBpmMessage> queue = new ArrayBlockingQueue<>(3);
        processor = startProcessor(queue::put);

        var message = queue.poll(5, TimeUnit.SECONDS);
        assertNotNull("First message has not been received", message);
        assertThat(message.getUid(), equalTo(puid1));

        message = queue.poll(5, TimeUnit.SECONDS);
        assertNotNull("Second message has not been received", message);
        assertThat(message.getUid(), equalTo(puid3));

        assertThat(pendingMessagesDAO.pollMessages(1), empty());

        List<Incident> incidents = getAllIncidents();
        assertThat(incidents, hasSize(1));

        Incident incident = incidents.get(0);
        assertThat(incident.uid, equalTo(puid2.toString()));
        assertNotNull(incident.message);
    }

    private PartitionMessagesProcessor startProcessor(MessageConsumer consumer) {
        var processor = new PartitionMessagesProcessor(
                txService,
                pendingMessagesDAO,
                pendingMessageSerializer,
                1,
                10,
                1,
                consumer,
                1
        );
        processor.start();
        return processor;
    }

    private boolean isLockedForId(Uid uid) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                """
                        SELECT EXISTS(
                            SELECT 1 FROM pending_messages_1
                            WHERE
                                user_id = ? AND lock_key IS NOT NULL
                        )""",
                Boolean.class,
                uid.toString()
        ));
    }

    private List<Incident> getAllIncidents() {
        return jdbcTemplate.query(
                "SELECT user_id, error_message FROM message_incidents_1",
                (rs, i) -> new Incident(
                        rs.getString(1),
                        rs.getString(2)
                )
        );
    }

    private void insertMessage(UidBpmMessage message) {
        pendingMessagesDAO.insertMessages(List.of(pendingMessageSerializer.serialize(message)));
    }

    private void generateMessages(Supplier<UidBpmMessage> supplier, int count) {
        List<PendingMessage> messages = Stream.generate(supplier)
                .map(pendingMessageSerializer::serialize)
                .limit(count)
                .collect(Collectors.toList());

        pendingMessagesDAO.insertMessages(messages);
    }
}
