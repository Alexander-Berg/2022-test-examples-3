package ru.yandex.market.partner.notification.transport.webui.dao;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.partner.notification.AbstractFunctionalTest;
import ru.yandex.market.partner.notification.transport.webui.model.Message;
import ru.yandex.market.partner.notification.transport.webui.model.MessageHeader;

class MessageDaoCreateMessageTest extends AbstractFunctionalTest {
    @Autowired
    MessageDao messageDao;

    @Test
    @DbUnitDataSet(after = "MessageDaoTest.createOneMessage.after.csv")
    void createOneMessage() throws SQLException {
        Message msg = Message.builder()
                .setHeader(getMessageTemplate()
                        .setSentTime(Instant.parse("2022-03-01T19:17:14Z"))
                        .build())
                .setBody("body")
                .build();

        messageDao.create(msg, List.of(msg.getUserId()));
    }

    @Test
    @DbUnitDataSet(after = "MessageDaoTest.createTwoMessagesTheSameDate.after.csv")
    void createTwoMessagesTheSameDate() throws SQLException {
        Message msg = Message.builder()
                .setHeader(getMessageTemplate()
                        .setSentTime(Instant.parse("2022-03-01T19:17:14Z"))
                        .build())
                .setBody("body")
                .build();

        messageDao.create(msg, List.of(msg.getUserId()));
        messageDao.create(msg, List.of(msg.getUserId()));
    }

    @Test
    @DbUnitDataSet(after = "MessageDaoTest.createTwoMessagesDifferentDates.after.csv")
    void createTwoMessagesDifferentDates() throws SQLException {
        Message msg = Message.builder()
                .setHeader(getMessageTemplate()
                        .setSentTime(Instant.parse("2022-03-01T19:17:14Z"))
                        .build())
                .setBody("body")
                .build();
        Message msg2 = Message.builder()
                .setHeader(getMessageTemplate()
                        .setSentTime(Instant.parse("2022-03-02T19:17:14Z"))
                        .setSubject("subject2")
                        .setThemeId(2L)
                        .setGroupId(12L)
                        .build())
                .setBody("body")
                .build();

        messageDao.create(msg, List.of(msg.getUserId()));
        messageDao.create(msg2, List.of(msg2.getUserId()));
    }

    @Test
    @DbUnitDataSet(
            before = "MessageDaoTest.createMessagePartitionExists.before.csv",
            after = "MessageDaoTest.createMessagePartitionExists.after.csv"
    )
    void createMessagePartitionExists() throws SQLException {
        Message msg = Message.builder()
                .setHeader(getMessageTemplate()
                        .setSentTime(Instant.parse("2022-03-01T19:17:14Z"))
                        .build())
                .setBody("body")
                .build();

        messageDao.create(msg, List.of(msg.getUserId()));
    }

    @Test
    @DbUnitDataSet(after = "MessageDaoTest.createOneMessageWithTwoUsers.after.csv")
    void createOneMessageWithTwoUsers() throws SQLException {
        Message msg = Message.builder()
                .setHeader(getMessageTemplate()
                        .setSentTime(Instant.parse("2022-03-01T19:17:14Z"))
                        .build())
                .setBody("body")
                .build();

        messageDao.create(msg, List.of(555L, 777L));
    }

    static MessageHeader.Builder getMessageTemplate() {
        return MessageHeader.builder()
                .setNotificationTypeId(2L)
                .setSentTime(Instant.parse("2019-08-13T19:17:14Z"))
                .setSubject("subject")
                .setPriority(2)
                .setShopId(22L)
                .setUserId(33L)
                .setGroupId(4L)
                .setThemeId(5L);
    }
}
