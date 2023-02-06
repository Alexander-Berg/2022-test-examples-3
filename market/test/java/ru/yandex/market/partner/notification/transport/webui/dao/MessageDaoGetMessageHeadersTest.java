package ru.yandex.market.partner.notification.transport.webui.dao;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.notification.simple.model.type.NotificationPriority;
import ru.yandex.market.partner.notification.AbstractFunctionalTest;
import ru.yandex.market.partner.notification.transport.webui.model.Message;
import ru.yandex.market.partner.notification.transport.webui.model.MessageFilter;
import ru.yandex.market.partner.notification.transport.webui.model.MessageHeader;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class MessageDaoGetMessageHeadersTest extends AbstractFunctionalTest {

    @Autowired
    MessageDao messageDao;

    @Test
    void getMessageHeadersUserFilter() throws SQLException {
        var headers = createMessages();
        List<MessageHeader> expected = headers.stream()
                .filter(h -> h.getUserId() == 33L)
                .sorted(Comparator.comparing(MessageHeader::getSentTime).reversed())
                .collect(Collectors.toList());

        var filter = MessageFilter.builder()
                .setUserId(33L)
                .setGroupIds(List.of()) // empty filter = no filter
                .setPriorities(List.of()) // empty filter = no filter
                .build();

        assertThat(messageDao.getHeadersCount(filter), equalTo(expected.size()));

        assertThat(messageDao.getHeaders(filter, 0, 10),
                equalTo(expected));
    }

    @Test
    void getMessageHeadersShopFilter() throws SQLException {
        var headers = createMessages();
        List<MessageHeader> expected = headers.stream()
                .filter(h -> h.getUserId() == 31L)
                .filter(h -> Long.valueOf(23L).equals(h.getShopId()))
                .sorted(Comparator.comparing(MessageHeader::getSentTime).reversed())
                .collect(Collectors.toList());

        var filter = MessageFilter.builder()
                .setUserId(31L)
                .setShopId(23L)
                .build();

        assertThat(messageDao.getHeadersCount(filter), equalTo(expected.size()));

        assertThat(messageDao.getHeaders(filter, 0, 10),
                equalTo(expected));
    }

    @Test
    void getMessageHeadersThemeFilter() throws SQLException {
        var headers = createMessages();
        List<MessageHeader> expected = headers.stream()
                .filter(h -> h.getUserId() == 31L)
                .filter(h -> Long.valueOf(1L).equals(h.getThemeId()))
                .sorted(Comparator.comparing(MessageHeader::getSentTime).reversed())
                .collect(Collectors.toList());

        var filter = MessageFilter.builder()
                .setUserId(31L)
                .setThemeId(1L)
                .build();

        assertThat(messageDao.getHeadersCount(filter), equalTo(expected.size()));

        assertThat(messageDao.getHeaders(filter, 0, 10),
                equalTo(expected));
    }

    @Test
    void getMessageHeadersPrioritiesFilter() throws SQLException {
        var headers = createMessages();
        List<MessageHeader> expected = headers.stream()
                .filter(h -> h.getUserId() == 31L)
                .filter(h -> h.getPriority() == 1 || h.getPriority() == 3)
                .sorted(Comparator.comparing(MessageHeader::getSentTime).reversed())
                .collect(Collectors.toList());

        var filter = MessageFilter.builder()
                .setUserId(31L)
                .setPriorities(List.of(NotificationPriority.LOW, NotificationPriority.HIGH))
                .build();

        assertThat(messageDao.getHeadersCount(filter), equalTo(expected.size()));

        assertThat(messageDao.getHeaders(filter, 0, 10),
                equalTo(expected));
    }

    @Test
    void getMessageHeadersFromFilter() throws SQLException {
        var headers = createMessages();
        List<MessageHeader> expected = headers.stream()
                .filter(h -> h.getUserId() == 31L)
                .filter(h -> h.getSentTime().isAfter(Instant.parse("2022-03-12T14:00:00Z")))
                .sorted(Comparator.comparing(MessageHeader::getSentTime).reversed())
                .collect(Collectors.toList());

        var filter = MessageFilter.builder()
                .setUserId(31L)
                .setFrom(Instant.parse("2022-03-12T14:00:00Z"))
                .build();

        assertThat(messageDao.getHeadersCount(filter), equalTo(expected.size()));

        assertThat(messageDao.getHeaders(filter, 0, 10),
                equalTo(expected));
    }

    @Test
    void getMessageHeadersToFilter() throws SQLException {
        var headers = createMessages();
        List<MessageHeader> expected = headers.stream()
                .filter(h -> h.getUserId() == 31L)
                .filter(h -> h.getSentTime().isBefore(Instant.parse("2022-03-12T14:00:00Z")))
                .sorted(Comparator.comparing(MessageHeader::getSentTime).reversed())
                .collect(Collectors.toList());

        var filter = MessageFilter.builder()
                .setUserId(31L)
                .setTo(Instant.parse("2022-03-12T14:00:00Z"))
                .build();

        assertThat(messageDao.getHeadersCount(filter), equalTo(expected.size()));

        assertThat(messageDao.getHeaders(filter, 0, 10),
                equalTo(expected));
    }

    @Test
    void getMessageHeadersOffsetLimit() throws SQLException {
        var headers = createMessages();
        List<MessageHeader> expected = headers.stream()
                .filter(h -> h.getUserId() == 31L)
                .sorted(Comparator.comparing(MessageHeader::getSentTime).reversed())
                .skip(2)
                .limit(3)
                .collect(Collectors.toList());

        var filter = MessageFilter.builder()
                .setUserId(31L)
                .build();

        assertThat(messageDao.getHeadersCount(filter), equalTo(6));

        assertThat(messageDao.getHeaders(filter, 2, 3),
                equalTo(expected));
    }

    @Test
    void getMessageHeadersGroupFilter() throws SQLException {
        var headers = createMessages();
        List<MessageHeader> expected = headers.stream()
                .filter(h -> h.getUserId() == 31L)
                .filter(h -> List.of(1L, 2L).contains(h.getGroupId()))
                .sorted(Comparator.comparing(MessageHeader::getSentTime).reversed())
                .collect(Collectors.toList());

        var filter = MessageFilter.builder()
                .setUserId(31L)
                .setGroupIds(List.of(1L, 2L))
                .build();

        assertThat(messageDao.getHeadersCount(filter), equalTo(expected.size()));

        assertThat(messageDao.getHeaders(filter, 0, 10),
                equalTo(expected));
    }

    private List<MessageHeader> createMessages() throws SQLException {
        for (var m : getTestMessages()) {
            messageDao.create(m, List.of(m.getUserId()));
        }
        var result = messageDao.getHeaders(MessageFilter.builder().build(), 0, 50);
        assertThat(result.size(), equalTo(9));
        return result;
    }

    private static List<Message> getTestMessages() {
    /*
        id,notification_type_id,sent_time,subject,importance,shop_id,user_id,theme_id
        1,2,2022-03-11 11:17:14 +0000,subject1,1,21,31,1
        2,2,2022-03-11 12:17:14 +0000,subject2,2,21,31,2
        3,2,2022-03-12 13:17:14 +0000,subject3,3,21,33,3
        4,2,2022-03-12 14:17:14 +0000,subject4,1,22,31,4
        5,2,2022-03-13 15:17:14 +0000,subject5,2,22,31,1
        6,2,2022-03-13 16:17:14 +0000,subject6,3,22,33,2
        7,2,2022-03-14 17:17:14 +0000,subject7,1,23,31,3
        8,2,2022-03-14 18:17:14 +0000,subject8,2,23,31,4
        9,2,2022-03-14 19:17:14 +0000,subject9,3,23,33,1
     */
        return List.of(
                Message.builder()
                        .setHeader(MessageHeader.builder()
                                .setNotificationTypeId(2L)
                                .setSentTime(Instant.parse("2022-03-11T11:17:14Z"))
                                .setSubject("subject1")
                                .setPriority(1)
                                .setShopId(21L)
                                .setUserId(31L)
                                .setThemeId(1L)
                                .setGroupId(1L)
                                .build())
                        .setBody("body1")
                        .build(),
                Message.builder()
                        .setHeader(MessageHeader.builder()
                                .setNotificationTypeId(2L)
                                .setSentTime(Instant.parse("2022-03-11T12:17:14Z"))
                                .setSubject("subject2")
                                .setPriority(2)
                                .setShopId(21L)
                                .setUserId(31L)
                                .setThemeId(2L)
                                .setGroupId(2L)
                                .build())
                        .setBody("body2")
                        .build(),
                Message.builder()
                        .setHeader(MessageHeader.builder()
                                .setNotificationTypeId(2L)
                                .setSentTime(Instant.parse("2022-03-12T13:17:14Z"))
                                .setSubject("subject3")
                                .setPriority(3)
                                .setShopId(21L)
                                .setUserId(33L)
                                .setThemeId(3L)
                                .setGroupId(3L)
                                .build())
                        .setBody("body3")
                        .build(),
                Message.builder()
                        .setHeader(MessageHeader.builder()
                                .setNotificationTypeId(2L)
                                .setSentTime(Instant.parse("2022-03-12T14:17:14Z"))
                                .setSubject("subject4")
                                .setPriority(1)
                                .setShopId(22L)
                                .setUserId(31L)
                                .setThemeId(4L)
                                .setGroupId(4L)
                                .build())
                        .setBody("body4")
                        .build(),
                Message.builder()
                        .setHeader(MessageHeader.builder()
                                .setNotificationTypeId(2L)
                                .setSentTime(Instant.parse("2022-03-13T15:17:14Z"))
                                .setSubject("subject5")
                                .setPriority(2)
                                .setShopId(22L)
                                .setUserId(31L)
                                .setThemeId(1L)
                                .setGroupId(1L)
                                .build())
                        .setBody("body5")
                        .build(),
                Message.builder()
                        .setHeader(MessageHeader.builder()
                                .setNotificationTypeId(2L)
                                .setSentTime(Instant.parse("2022-03-13T16:17:14Z"))
                                .setSubject("subject6")
                                .setPriority(3)
                                .setShopId(22L)
                                .setUserId(33L)
                                .setThemeId(2L)
                                .setGroupId(2L)
                                .build())
                        .setBody("body6")
                        .build(),
                Message.builder()
                        .setHeader(MessageHeader.builder()
                                .setNotificationTypeId(2L)
                                .setSentTime(Instant.parse("2022-03-14T17:17:14Z"))
                                .setSubject("subject7")
                                .setPriority(1)
                                .setShopId(23L)
                                .setUserId(31L)
                                .setThemeId(3L)
                                .setGroupId(3L)
                                .build())
                        .setBody("body7")
                        .build(),
                Message.builder()
                        .setHeader(MessageHeader.builder()
                                .setNotificationTypeId(2L)
                                .setSentTime(Instant.parse("2022-03-14T18:17:14Z"))
                                .setSubject("subject8")
                                .setPriority(2)
                                .setShopId(23L)
                                .setUserId(31L)
                                .setThemeId(4L)
                                .setGroupId(4L)
                                .build())
                        .setBody("body8")
                        .build(),
                Message.builder()
                        .setHeader(MessageHeader.builder()
                                .setNotificationTypeId(2L)
                                .setSentTime(Instant.parse("2022-03-14T19:17:14Z"))
                                .setSubject("subject9")
                                .setPriority(3)
                                .setShopId(null)
                                .setUserId(33L)
                                .setThemeId(1L)
                                .setGroupId(1L)
                                .build())
                        .setBody("body9")
                        .build()
        );
    }
}
