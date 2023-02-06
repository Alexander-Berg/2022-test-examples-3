package ru.yandex.market.partner.notification.api;

import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import ru.yandex.market.common.retrofit.CommonRetrofitHttpExecutionException;
import ru.yandex.market.partner.notification.AbstractFunctionalTest;
import ru.yandex.market.partner.notification.transport.webui.dao.MessageDao;
import ru.yandex.market.partner.notification.transport.webui.model.Message;
import ru.yandex.market.partner.notification.transport.webui.model.MessageFilter;
import ru.yandex.market.partner.notification.transport.webui.model.MessageHeader;
import ru.yandex.mj.generated.client.self.api.MessagesApiClient;
import ru.yandex.mj.generated.client.self.model.GetMessageHeadersResponse;
import ru.yandex.mj.generated.client.self.model.MessageDTO;
import ru.yandex.mj.generated.client.self.model.MessageHeaderDTO;
import ru.yandex.mj.generated.client.self.model.PagerDTO;
import ru.yandex.mj.generated.client.self.model.PriorityDTO;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MessagesApiServiceTest extends AbstractFunctionalTest {

    @Autowired
    MessagesApiClient messagesApiClient;

    @Autowired
    MessageDao messageDao;

    @Test
    void getMessageHeaders() throws Exception {
        var message = createMessages().stream()
                .filter(h -> Long.valueOf(22L).equals(h.getShopId()))
                .filter(h -> h.getUserId() == 31L)
                .filter(h -> h.getPriority() == 1)
                .findFirst()
                .get();

        GetMessageHeadersResponse result = messagesApiClient.getMessageHeaders(
                33L,
                31L,
                OffsetDateTime.parse("2022-03-12T14:00:00Z"),
                null,
                22L,
                null,
                List.of(PriorityDTO.HIGH),
                null,
                null,
                null
        ).scheduleResponse().get().body();

        var expected = new GetMessageHeadersResponse()
                .headers(List.of(
                        new MessageHeaderDTO()
                                .messageId(message.getId())
                                .sentTime(OffsetDateTime.ofInstant(message.getSentTime(), ZoneId.systemDefault()))
                                .subject("subject4")
                                .groupId(14L)
                                .priority(PriorityDTO.HIGH)
                ))
                .pager(new PagerDTO()
                        .currentPage(0)
                        .from(1)
                        .itemCount(1)
                        .pageSize(10)
                        .pages(List.of(0))
                        .to(10));

        assertThat(result, equalTo(expected));
    }

    @Test
    void getMessage() throws Exception {
        var message = createMessages().stream()
                .filter(h -> Long.valueOf(22L).equals(h.getShopId()))
                .filter(h -> h.getUserId() == 31L)
                .filter(h -> h.getPriority() == 1)
                .findFirst()
                .get();

        MessageDTO result = messagesApiClient.getMessage(message.getId(), 33L, 31L)
                .scheduleResponse().get().body();

        var expected = new MessageDTO()
                .id(message.getId())
                .shopId(22L)
                .priority(PriorityDTO.HIGH)
                .sentTime(OffsetDateTime.ofInstant(message.getSentTime(), ZoneId.systemDefault()))
                .subject("subject4")
                .body("body4");

        assertThat(result, equalTo(expected));
    }

    @Test
    void getMessageWrongUser() throws Exception {
        var message = createMessages().stream()
                .filter(h -> Long.valueOf(22L).equals(h.getShopId()))
                .filter(h -> h.getUserId() == 31L)
                .filter(h -> h.getPriority() == 1)
                .findFirst()
                .get();

        var exception = assertThrows(ExecutionException.class, () -> {
            messagesApiClient.getMessage(message.getId(), 33L, null).scheduleResponse().get();
        });
        var cause = (CommonRetrofitHttpExecutionException) exception.getCause();
        assertThat(cause.getHttpCode(), equalTo(HttpStatus.NOT_FOUND.value()));

    }

    /**
     * Выглядит странно, но так сейчас в mbi работает - тесткейсы копировал с реальных вызовов mbi
     * надо будет переделать на токен после того как с фронта напрямую
     * будем ходить в partner-notification, а не через прокси в mbi
     */
    @Test
    public void getMessageHeadersPager() throws Exception {
        for (int i = 0; i < 127; i++) {
            messageDao.create(getTestMessages().get(0), List.of(777L));
        }

        var pager = messagesApiClient.getMessageHeaders(777L, null, null, null, null, null, null,
                Collections.emptyList(), null, null).schedule().get().getPager();

        assertThat(pager, equalTo(new PagerDTO()
                .currentPage(0)
                .from(1)
                .itemCount(127)
                .pageSize(10)
                .pages(List.of(0, 1, 2, 3, 4, 5, 6, 7))
                .to(10)));

        pager = messagesApiClient.getMessageHeaders(777L, null, null, null, null, null, null,
                Collections.emptyList(), 0, null).schedule().get().getPager();

        assertThat(pager, equalTo(new PagerDTO()
                .currentPage(0)
                .from(1)
                .itemCount(127)
                .pageSize(10)
                .pages(List.of(0, 1, 2, 3, 4, 5, 6, 7))
                .to(10)));

        pager = messagesApiClient.getMessageHeaders(777L, null, null, null, null, null, null,
                Collections.emptyList(), 6, null).schedule().get().getPager();

        assertThat(pager, equalTo(new PagerDTO()
                .currentPage(6)
                .from(61)
                .itemCount(127)
                .pageSize(10)
                .pages(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12))
                .to(70)));

        pager = messagesApiClient.getMessageHeaders(777L, null, null, null, null, null, null,
                null, 12, null).schedule().get().getPager();

        assertThat(pager, equalTo(new PagerDTO()
                .currentPage(12)
                .from(121)
                .itemCount(127)
                .pageSize(10)
                .pages(List.of(5, 6, 7, 8, 9, 10, 11, 12))
                .to(130)));
    }

    private List<MessageHeader> createMessages() throws SQLException {
        for (var m : getTestMessages()) {
            messageDao.create(m, List.of(m.getUserId()));
        }
        return messageDao.getHeaders(MessageFilter.builder().build(), 0, 50).stream()
                .sorted(Comparator.comparing(MessageHeader::getSentTime).reversed())
                .collect(Collectors.toList());
    }

    private static List<Message> getTestMessages() {
    /*
        id,notification_type_id,sent_time,subject,importance,shop_id,user_id,theme_id
        1,2,2022-03-11 11:17:14 +0000,subject1,1,21,31,1
        2,2,2022-03-11 12:17:14 +0000,subject2,2,21,31,2
        4,2,2022-03-12 14:17:14 +0000,subject4,1,22,31,4
        5,2,2022-03-13 15:17:14 +0000,subject5,2,22,31,1
        7,2,2022-03-14 17:17:14 +0000,subject7,1,23,31,3
        8,2,2022-03-14 18:17:14 +0000,subject8,2,23,31,4
        9,2,2022-03-14 19:17:14 +0000,subject9,3,23,33,1
     */
        return List.of(
                Message.builder()
                        .setHeader(MessageHeader.builder()
                                .setId(0L)
                                .setNotificationTypeId(2L)
                                .setSentTime(Instant.parse("2022-03-11T11:17:14Z"))
                                .setSubject("subject1")
                                .setPriority(1)
                                .setShopId(21L)
                                .setUserId(31L)
                                .setThemeId(1L)
                                .setGroupId(11L)
                                .build())
                        .setBody("body1")
                        .build(),
                Message.builder()
                        .setHeader(MessageHeader.builder()
                                .setId(0L)
                                .setNotificationTypeId(2L)
                                .setSentTime(Instant.parse("2022-03-11T12:17:14Z"))
                                .setSubject("subject2")
                                .setPriority(2)
                                .setShopId(21L)
                                .setUserId(31L)
                                .setThemeId(2L)
                                .setGroupId(11L)
                                .build())
                        .setBody("body2")
                        .build(),
                Message.builder()
                        .setHeader(MessageHeader.builder()
                                .setId(0L)
                                .setNotificationTypeId(2L)
                                .setSentTime(Instant.parse("2022-03-12T13:17:14Z"))
                                .setSubject("subject3")
                                .setPriority(3)
                                .setShopId(21L)
                                .setUserId(33L)
                                .setThemeId(3L)
                                .setGroupId(13L)
                                .build())
                        .setBody("body3")
                        .build(),
                Message.builder()
                        .setHeader(MessageHeader.builder()
                                .setId(0L)
                                .setNotificationTypeId(2L)
                                .setSentTime(Instant.parse("2022-03-12T14:17:14Z"))
                                .setSubject("subject4")

                                .setPriority(1)
                                .setShopId(22L)
                                .setUserId(31L)
                                .setThemeId(4L)
                                .setGroupId(14L)
                                .build())
                        .setBody("body4")
                        .build(),
                Message.builder()
                        .setHeader(MessageHeader.builder()
                                .setId(0L)
                                .setNotificationTypeId(2L)
                                .setSentTime(Instant.parse("2022-03-13T15:17:14Z"))
                                .setSubject("subject5")
                                .setPriority(2)
                                .setShopId(22L)
                                .setUserId(31L)
                                .setThemeId(1L)
                                .setGroupId(11L)
                                .build())
                        .setBody("body5")
                        .build(),
                Message.builder()
                        .setHeader(MessageHeader.builder()
                                .setId(0L)
                                .setNotificationTypeId(2L)
                                .setSentTime(Instant.parse("2022-03-13T16:17:14Z"))
                                .setSubject("subject6")
                                .setPriority(3)
                                .setShopId(22L)
                                .setUserId(33L)
                                .setThemeId(2L)
                                .setGroupId(12L)
                                .build())
                        .setBody("body6")
                        .build(),
                Message.builder()
                        .setHeader(MessageHeader.builder()
                                .setId(0L)
                                .setNotificationTypeId(2L)
                                .setSentTime(Instant.parse("2022-03-14T17:17:14Z"))
                                .setSubject("subject7")
                                .setPriority(1)
                                .setShopId(23L)
                                .setUserId(31L)
                                .setThemeId(3L)
                                .setGroupId(13L)
                                .build())
                        .setBody("body7")
                        .build(),
                Message.builder()
                        .setHeader(MessageHeader.builder()
                                .setId(0L)
                                .setNotificationTypeId(2L)
                                .setSentTime(Instant.parse("2022-03-14T18:17:14Z"))
                                .setSubject("subject8")
                                .setPriority(2)
                                .setShopId(23L)
                                .setUserId(31L)
                                .setThemeId(4L)
                                .setGroupId(14L)
                                .build())
                        .setBody("body8")
                        .build(),
                Message.builder()
                        .setHeader(MessageHeader.builder()
                                .setId(0L)
                                .setNotificationTypeId(2L)
                                .setSentTime(Instant.parse("2022-03-14T19:17:14Z"))
                                .setSubject("subject9")
                                .setPriority(3)
                                .setShopId(null)
                                .setUserId(33L)
                                .setThemeId(1L)
                                .setGroupId(11L)
                                .build())
                        .setBody("body9")
                        .build()
        );
    }

}
