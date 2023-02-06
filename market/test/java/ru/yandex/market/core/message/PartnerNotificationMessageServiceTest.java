package ru.yandex.market.core.message;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import ru.market.partner.notification.client.PartnerNotificationClient;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.framework.pager.Pager;
import ru.yandex.market.core.message.db.DbMessageService;
import ru.yandex.market.core.message.db.DbMessageServiceTest;
import ru.yandex.market.core.message.model.HeaderFilter;
import ru.yandex.market.notification.simple.model.type.NotificationPriority;
import ru.yandex.market.partner.notification.client.model.GetMessageHeadersResponse;
import ru.yandex.market.partner.notification.client.model.GetWebUINotificationResponse;
import ru.yandex.market.partner.notification.client.model.MessageDTO;
import ru.yandex.market.partner.notification.client.model.MessageHeaderDTO;
import ru.yandex.market.partner.notification.client.model.PagerDTO;
import ru.yandex.market.partner.notification.client.model.PriorityDTO;
import ru.yandex.market.partner.notification.client.model.WebUINotificationResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

@DbUnitDataSet(before = "db/DbMessageServiceTest.before.csv")
public class PartnerNotificationMessageServiceTest extends FunctionalTest {
    @Autowired
    PartnerNotificationMessageService messageService;

    @BeforeEach
    void setUp() {
        doReturn(new GetMessageHeadersResponse()
                .headers(List.of())
                .pager(new PagerDTO().itemCount(0))
        )
                .when(partnerNotificationClient)
                .getMessageHeaders(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "nope"))
                .when(partnerNotificationClient)
                .getMessage(any(), any(), any());
        doReturn(new GetWebUINotificationResponse()
                .notifications(List.of()))
                .when(partnerNotificationClient)
                .getWebUINotificationsByGroupId(any());
    }

    @Test
    void getMessageHeadersWithoutGroupIds() {
        // given
        var dbMessageService = mock(DbMessageService.class);
        messageService = new PartnerNotificationMessageService(
                partnerNotificationClient,
                dbMessageService
        );

        // when
        var headers = messageService.getMessageHeaders(
                1L,
                (Long) null,
                HeaderFilter.newBuilder()
                        .withDateFrom(new Date())
                        .withDateTo(new Date())
                        .withShopId(1L)
                        .build(),
                new Pager(0, 2)
        );

        // then
        assertThat(headers).isEmpty();
        verifyNoInteractions(dbMessageService);
    }

    @Test
    void getMessageHeadersOnlyPNGroupIds() {
        // given
        var uid = 1L;
        var messageGroupIds = List.of(101L, 102L);
        var pager = new Pager(0, 2);

        // when
        var headers = messageService.getMessageHeaders(
                uid,
                (Long) null,
                HeaderFilter.newBuilder()
                        .withDateFrom(new Date())
                        .withDateTo(new Date())
                        .withGroupIds(messageGroupIds)
                        .withShopId(1L)
                        .build(),
                pager
        );

        // then
        assertThat(headers).isEmpty();
    }

    /**
     * @see DbMessageServiceTest#getGroupHeadersFound()
     */
    @Test
    void getMessageHeadersWithFallbackGroupIds() {
        var messageGroupIds = List.of(101L, 102L);
        var pager = new Pager(0, messageGroupIds.size());
        var sentTime = new Date(2022 - 1900, 4, 17);

        doReturn(new GetMessageHeadersResponse().headers(
                        List.of(new MessageHeaderDTO()
                                .messageId(3L)
                                .sentTime(OffsetDateTime.ofInstant(sentTime.toInstant(), ZoneId.systemDefault()))
                                .priority(PriorityDTO.LOW),
                                new MessageHeaderDTO()
                                        .messageId(2L)
                                        .sentTime(OffsetDateTime.ofInstant(sentTime.toInstant(), ZoneId.systemDefault()))
                                        .priority(PriorityDTO.NORMAL))
                        )
                .pager(new PagerDTO().currentPage(0).pageSize(2).itemCount(2))
        )
        .when(partnerNotificationClient)
        .getMessageHeaders(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());

        // when
        var headers = messageService.getMessageHeaders(
                1L,
                (Long) null,
                HeaderFilter.newBuilder()
                        .withDateFrom(new Date())
                        .withDateTo(new Date())
                        .withGroupIds(messageGroupIds)
                        .withShopId(1L)
                        .build(),
                pager
        );

        // then

        assertThat(headers)
                .hasSize(pager.getItemCount())
                .satisfiesExactly(
                        h -> {
                            assertThat(h.getId()).isEqualTo(3);
                            assertThat(h.getImportance()).isEqualTo(NotificationPriority.LOW);
                            assertThat(h.getSentTime()).isEqualTo(sentTime);
                        },
                        h -> {
                            assertThat(h.getId()).isEqualTo(2);
                            assertThat(h.getImportance()).isEqualTo(NotificationPriority.NORMAL);
                            assertThat(h.getSentTime()).isEqualTo(sentTime);
                        }
                );
    }


    @Test
    void getNotificationMessageOnlyPN() {
        // when
        var message = messageService.getNotificationMessage(3L, 1L);

        // then
        assertThat(message).isEmpty();
    }

    @Test
    void getNotificationMessageWithFallback() {

        // when
        doReturn(new MessageDTO()
                .id(1L)
                .priority(PriorityDTO.HIGH))
                .when(partnerNotificationClient).getMessage(1L, 1L, 1L);

        var message = messageService.getNotificationMessage(1L, 1L);

        // then
        assertThat(message).get().satisfies(h -> {
            assertThat(h.getId()).isEqualTo(1);
            assertThat(h.getImportance()).isEqualTo(NotificationPriority.HIGH);
        });
    }

    @Test
    void getNotificationMessagesOnlyPN() {
        var messageGroupIds = List.of(101L, 102L);

        // when
        var message = messageService.getCutoffNotificationMessages(messageGroupIds);

        // then
        assertThat(message).isEmpty();
    }

    @Test
    void getNotificationMessagesWithFallback() {
        PartnerNotificationMessageServiceTest.mockPN(
                partnerNotificationClient,
                new WebUINotificationResponse()
                        .subject("subject 1")
                        .notificationTypeId(59L)
                        .body("body 1")
                        .priority(1L)
                        .groupId(101L),
                new WebUINotificationResponse()
                        .subject("subject 2")
                        .notificationTypeId(59L)
                        .body("body 2")
                        .priority(1L)
                        .groupId(102L)
        );

        var messageGroupIds = List.of(101L, 102L);

        // when
        var message = messageService.getCutoffNotificationMessages(messageGroupIds);

        // then
        assertThat(message)
                .hasSize(2)
                .hasEntrySatisfying(101L, wc -> {
                    assertThat(wc.getBody()).isEqualTo("body 1");
                    assertThat(wc.getNotificationTypeId()).isEqualTo(59L);
                    assertThat(wc.getPriority()).isEqualTo(1);
                    assertThat(wc.getSubject()).isEqualTo("subject 1");
                })
                .hasEntrySatisfying(102L, wc -> {
                    assertThat(wc.getBody()).isEqualTo("body 2");
                    assertThat(wc.getNotificationTypeId()).isEqualTo(59L);
                    assertThat(wc.getSubject()).isEqualTo("subject 2");
                    assertThat(wc.getPriority()).isEqualTo(1);
                });
    }

    public static void mockPN(
            PartnerNotificationClient partnerNotificationClient,
            WebUINotificationResponse... responses
    ) {
        doReturn(new GetWebUINotificationResponse().notifications(List.of(responses)))
                .when(partnerNotificationClient).getWebUINotificationsByGroupId(any());
    }
}
