package ru.yandex.market.crm.triggers.services.bpm.correlation;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import javax.inject.Inject;

import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.crm.core.services.jackson.JacksonConfig;
import ru.yandex.market.crm.mapreduce.domain.user.Uid;
import ru.yandex.market.crm.triggers.services.bpm.UidBpmMessage;
import ru.yandex.market.crm.triggers.test.TestDatabaseConfig;
import ru.yandex.market.mcrm.db.test.DbTestTool;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static ru.yandex.market.crm.triggers.services.bpm.correlation.CorrelationTestUtils.message;

/**
 * @author apershukov
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = PendingMessagesDAOTest.TestConfig.class)
public class PendingMessagesDAOTest {

    @Inject
    private DbTestTool dbTestTool;
    @Inject
    private JdbcTemplate jdbcTemplate;
    @Inject
    private PendingMessageSerializer pendingMessageSerializer;
    private PendingMessagesDAO dao;

    @Before
    public void setUp() {
        dao = new PendingMessagesDAO(jdbcTemplate, 1);
        dbTestTool.runScript("/triggers_sql/pending_messages.sql");
    }

    @After
    public void tearDown() {
        jdbcTemplate.update("TRUNCATE TABLE pending_messages_1 CASCADE");
    }

    /**
     * Переданное сообщение можно сразу получить
     */
    @Test
    public void testInsertAndPoll() {
        UidBpmMessage message = message();
        insertMessage(message);

        List<PendingMessage> pendingMessages = dao.pollMessages(1);
        assertThat(pendingMessages, hasSize(1));

        UidBpmMessage polledMessage = pendingMessageSerializer.deserialize(pendingMessages.get(0).getMessage());
        assertNotEquals(0, pendingMessages.get(0).getId());
        assertNotNull(polledMessage);

        assertEquals(message.getType(), polledMessage.getType());
        assertEquals(message.getUid(), polledMessage.getUid());
        assertEquals(Map.<String, Object>of(), polledMessage.getCorrelationVariables());
        assertEquals(message.getVariables(), polledMessage.getVariables());
    }

    /**
     * Уже залоченные (взятые на обработку) сообщения нельзя взять повторно
     */
    @Test
    public void testLockedMessageCannotBeReceived() {
        UidBpmMessage message = message();
        insertMessage(message);

        List<PendingMessage> messages = dao.pollMessages(1);
        assertThat(messages, not(empty()));

        messages = dao.pollMessages(1);
        assertThat(messages, empty());
    }

    /**
     * При удалении сообщение исчезает из очереди
     */
    @Test
    public void testDeleteMessage() {
        UidBpmMessage message = message();
        insertMessage(message);

        long id = getSingleMessageId();

        dao.deleteMessage(id);

        List<?> messages = dao.pollMessages(1);
        assertThat(messages, empty());
    }

    /**
     * Вызов unlock для сообщения делает его снова доступным для извлечения
     */
    @Test
    public void testUnlockMessage() {
        UidBpmMessage message = message();
        insertMessage(message);

        List<PendingMessage> messages = dao.pollMessages(1);
        assertThat(messages, hasSize(1));

        long id = messages.get(0).getId();

        dao.unlockMessage(id);

        messages = dao.pollMessages(1);
        assertThat(messages, hasSize(1));
        assertEquals(id, messages.get(0).getId());
    }

    /**
     * Количество получаемых сообщений ограничено аргументом метода
     */
    @Test
    public void testRestrictBatchSize() {
        List<PendingMessage> messages = LongStream.rangeClosed(1, 10)
                .mapToObj(x -> message(Uid.asPuid(x)))
                .map(pendingMessageSerializer::serialize)
                .collect(Collectors.toList());

        dao.insertMessages(messages);

        long[] actualPuids = dao.pollMessages(5).stream()
                .map(message -> new MessageContainer(
                        message.getId(),
                        pendingMessageSerializer.deserialize(message.getMessage())
                ))
                .map(container -> container.getMessage().getUid().getValue())
                .mapToLong(Long::parseLong)
                .toArray();

        long[] expectedPuids = LongStream.rangeClosed(1, 5).toArray();

        assertArrayEquals(expectedPuids, actualPuids);
    }

    /**
     * В случае если существует инцидент, связанный с каким-либо сообщением
     * для определенного пользователя, блокируется получение любых сообщений
     * для этого пользователя
     */
    @Test
    public void testIfIncidentExistsMessageForUserIsNotAvailable() {
        Uid puid1 = Uid.asPuid(111L);
        insertMessage(message(puid1));
        insertMessage(message(puid1));
        insertMessage(message(puid1));

        Uid puid2 = Uid.asPuid(222L);
        insertMessage(message(puid2));

        List<Long> messageIds = getMessageIds();
        assertThat(messageIds, not(empty()));

        dao.insertIncident(puid1.toString(), messageIds.get(0), "Error message");

        List<MessageContainer> containers = dao.pollMessages(5).stream()
                .map(message -> new MessageContainer(
                        message.getId(),
                        pendingMessageSerializer.deserialize(message.getMessage())
                ))
                .collect(Collectors.toList());
        assertThat(containers, hasSize(1));
        assertEquals(puid2, containers.get(0).getMessage().getUid());
    }

    private long getSingleMessageId() {
        List<Long> ids = getMessageIds();
        assertThat(ids, hasSize(1));
        return ids.get(0);
    }

    @NotNull
    private List<Long> getMessageIds() {
        return jdbcTemplate.queryForList(
                "SELECT id FROM pending_messages_1",
                Long.class
        );
    }

    private void insertMessage(UidBpmMessage message) {
        dao.insertMessages(List.of(pendingMessageSerializer.serialize(message)));
    }

    @Configuration
    @Import({
            TestDatabaseConfig.class,
            JacksonConfig.class,
            PendingMessageSerializerConfiguration.class
    })
    static class TestConfig {
    }
}
