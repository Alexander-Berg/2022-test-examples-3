package ru.yandex.market.abo.mm.db;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.EmptyTest;
import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.abo.mm.model.Message;
import ru.yandex.market.abo.mm.model.MessageAttachment;
import ru.yandex.market.abo.mm.model.MessageType;
import ru.yandex.market.abo.mm.model.MessageUser;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author antipov93.
 * @date 01.06.18.
 */
class DbMailServiceTest extends EmptyTest {

    private static final long ACC_ID = 112233L;
    @Autowired
    private DbMailService dbMailService;
    @Autowired
    private JdbcTemplate pgJdbcTemplate;

    @Test
    @SuppressWarnings("ConstantConditions")
    void store() {
        long id = pgJdbcTemplate.queryForObject("select nextval('s_mm_message')", Long.class);
        Message message = createMessage(id);
        dbMailService.store(Collections.singletonList(message));
        Message stored = dbMailService.loadMessage(id);

        assertEquals(message.getBody(), stored.getBody());
        assertEquals(message.getSubject(), stored.getSubject());
        assertEquals(message.isSpam(), stored.isSpam());
        assertEquals(message.getFromEmail(), stored.getFromEmail());
        assertEquals(message.getFromName(), stored.getFromName());
        assertEquals(message.getToEmail(), stored.getToEmail());
        assertEquals(message.getToName(), stored.getToName());
        assertEquals(message.getAccountId(), stored.getAccountId());
        assertEquals(message.getType(), stored.getType());

        List<Long> attachIds = pgJdbcTemplate.queryForList("select id from mm_message_attachment where message_id = ?",
                Long.class, id);
        assertEquals(2, attachIds.size());
        attachIds.forEach(attachId -> {
            MessageAttachment storedAttach = dbMailService.loadMessageAttachment(attachId);
            MessageAttachment attach = message.getAttachments().stream()
                    .filter(att -> att.getFilename().equals(storedAttach.getFilename()))
                    .findFirst().orElseThrow(RuntimeException::new);

            assertEquals(attach.getMessageId(), storedAttach.getMessageId());
            assertArrayEquals(attach.getContent(), storedAttach.getContent());
        });
    }

    @Test
    void findMsg() {
        Message msg = createMessage(111);
        dbMailService.store(List.of(msg));
        assertTrue(dbMailService.findMessage(msg.getBody(), (long) ACC_ID,
                DateUtil.asLocalDateTime(msg.getTime()).minusMinutes(10)).isPresent());

        assertFalse(dbMailService.findMessage(msg.getBody() + "fooo", (long) ACC_ID,
                DateUtil.asLocalDateTime(msg.getTime())).isPresent());

        assertFalse(dbMailService.findMessage(msg.getBody(), (long) ACC_ID,
                DateUtil.asLocalDateTime(msg.getTime()).plusMinutes(1)).isPresent());
    }

    @Test
    void latestMsgTimes() {
        long id = 0;

        Message message = createMessage(id);
        dbMailService.store(Collections.singletonList(message));

        Map<Long, LocalDateTime> msgTimes = dbMailService.latestMessageTimes(Collections.singletonList((long) ACC_ID));
        assertEquals(message.getTime().toInstant(),
                msgTimes.get((long) ACC_ID).toInstant(ZoneOffset.from(OffsetDateTime.now().getOffset())));
    }

    @ParameterizedTest(name = "testPrepareTextForDb_{index}")
    @MethodSource("prepareTextForDbSource")
    void testPrepareTextForDb(String input, String expected, int limit) {
        assertEquals(expected, DbMailService.limitAndClear(input, limit));
    }

    private static Stream<Arguments> prepareTextForDbSource() {
        return Stream.of(
                Arguments.of(null, "", -1),
                Arguments.of("", "", -1),
                Arguments.of("a\u0000b", "ab", -1),
                Arguments.of("some normal\ntext", "some normal\ntext", -1),
                Arguments.of("123", "12", 2),
                Arguments.of("tab\ttabulation", "tab\ttabulation", -1),
                Arguments.of("<html><body><h>foobar</h></body></html>", "<html><body><h>foobar</h></body></html>", -1),
                Arguments.of("йцу кен", "йцу кен", -1)
        );
    }

    private static Message createMessage(long id) {
        Message result = new Message(id);
        result.setTime(new Date());
        result.setBody("somebody");
        result.setFrom(new MessageUser("mr", "robot"));
        result.getToList().add(new MessageUser("ms", "robot"));
        result.setSubject("subj");
        result.setSpam(true);
        result.setHeader("HDR");
        result.setAccountId(ACC_ID);
        result.addAttachment(new MessageAttachment(id, "attach.me", new byte[]{0, 0, 1, 1}));
        result.addAttachment(new MessageAttachment(id, "i.am.null", null));
        result.setTypeId(MessageType.AUTO.getCode());
        return result;
    }
}
