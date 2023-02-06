package ru.yandex.market.notification;

import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.notification.service.NotificationSendContext;
import ru.yandex.market.core.notification.service.NotificationService;
import ru.yandex.market.notification.notifications.PeriodicNotification;
import ru.yandex.market.shop.FunctionalTest;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@DbUnitDataSet
class PeriodicNotifierExecutorTest extends FunctionalTest {
    @Autowired
    TransactionTemplate transactionTemplate;
    @Autowired
    PeriodicNotificationDao dao;

    Clock clock;
    NotificationService notificationServiceMock;
    PeriodicNotification<?> n1Mock;
    PeriodicNotification<?> n2Mock;

    PeriodicNotifierExecutor executor;
    Instant now;

    @BeforeEach
    void setUp() {
        now = ZonedDateTime.parse("2021-02-04T14:41:45+03:00").toInstant();

        clock = mock(Clock.class);
        when(clock.instant()).thenReturn(now);

        notificationServiceMock = mock(NotificationService.class);

        n1Mock = mock(PeriodicNotification.class);
        when(n1Mock.getNotificationId()).thenReturn("notification 1");

        n2Mock = mock(PeriodicNotification.class);
        when(n2Mock.getNotificationId()).thenReturn("notification 2");

        executor = new PeriodicNotifierExecutor(
                List.of(n1Mock, n2Mock),
                dao,
                notificationServiceMock,
                transactionTemplate,
                clock
        );
    }

    @Test
    @DisplayName("Отправка 1-му магазину, у которого есть данные по 2м партнерам и все хорошо и повторно не отправляет")
    void testSingleShopWithTwoPartners() {
        dao.upsert(n2Mock.getNotificationId(), now.plusSeconds(1));

        long partner1 = 234L;
        long partner2 = 345L;
        when(n1Mock.getPartnerIds(null)).thenReturn(List.of(partner1, partner2));

        var notificationCtx1 = mock(NotificationSendContext.class);
        var notificationCtx2 = mock(NotificationSendContext.class);
        when(n1Mock.getPartnerNotification(partner1, null)).thenReturn(Optional.of(notificationCtx1));
        when(n1Mock.getPartnerNotification(partner2, null)).thenReturn(Optional.of(notificationCtx2));
        when(n1Mock.getNextNotificationTimeAfter(any())).thenReturn(now.plusSeconds(1));

        executor.doJob(null);

        verify(notificationServiceMock).send(notificationCtx1);
        verify(notificationServiceMock).send(notificationCtx2);

        verify(n1Mock, atLeast(1)).getNotificationId();
        verify(n2Mock, atLeast(1)).getNotificationId();

        verify(n1Mock).getPartnerIds(null);
        verify(n1Mock).getPartnerNotification(partner1, null);
        verify(n1Mock).getPartnerNotification(partner2, null);
        verify(n1Mock).getNextNotificationTimeAfter(now);
        verify(n1Mock).prepareData();

        verifyNoMoreInteractions(n1Mock, n2Mock, notificationServiceMock);

        executor.doJob(null);

        verify(n1Mock, atLeast(1)).getNotificationId();
        verify(n2Mock, atLeast(1)).getNotificationId();
        verifyNoMoreInteractions(n1Mock, n2Mock, notificationServiceMock);
    }

    @Test
    @DisplayName("Отправка 1-му магазину: нет данных по первому партнеру не мешают отправке второму")
    void testSingleShopNoDataForFirstPartner() {
        dao.upsert(n2Mock.getNotificationId(), now.plusSeconds(1));

        long partner1 = 234L;
        long partner2 = 345L;
        when(n1Mock.getPartnerIds(null)).thenReturn(List.of(partner1, partner2));

        var notificationCtx = mock(NotificationSendContext.class);

        when(n1Mock.getPartnerNotification(partner1, null)).thenReturn(Optional.empty());
        when(n1Mock.getPartnerNotification(partner2, null)).thenReturn(Optional.of(notificationCtx));

        when(n1Mock.getNextNotificationTimeAfter(any())).thenReturn(now.plusSeconds(1));

        executor.doJob(null);

        verify(notificationServiceMock).send(notificationCtx);

        verify(n1Mock, atLeast(1)).getNotificationId();
        verify(n2Mock, atLeast(1)).getNotificationId();

        verify(n1Mock).getPartnerIds(null);
        verify(n1Mock).getPartnerNotification(partner1, null);
        verify(n1Mock).getPartnerNotification(partner2, null);
        verify(n1Mock).getNextNotificationTimeAfter(now);
        verify(n1Mock).prepareData();

        verifyNoMoreInteractions(n1Mock, n2Mock, notificationServiceMock);

        executor.doJob(null);

        verify(n1Mock, atLeast(1)).getNotificationId();
        verify(n2Mock, atLeast(1)).getNotificationId();
        verifyNoMoreInteractions(n1Mock, n2Mock, notificationServiceMock);
    }

    @Test
    @DisplayName("Отправка 1-му магазину: ошибка данных по первому партнеру не мешают отправке второму, джоба падает")
    void testSingleShopDataErrorForFirstPartner() {
        dao.upsert(n2Mock.getNotificationId(), now.plusSeconds(1));

        long partner1 = 234L;
        long partner2 = 345L;
        when(n1Mock.getPartnerIds(null)).thenReturn(List.of(partner1, partner2));

        var notificationCtx = mock(NotificationSendContext.class);

        when(n1Mock.getPartnerNotification(partner1, null)).thenThrow(new RuntimeException("can't get notification data"));
        when(n1Mock.getPartnerNotification(partner2, null)).thenReturn(Optional.of(notificationCtx));

        when(n1Mock.getNextNotificationTimeAfter(any())).thenReturn(now.plusSeconds(1));

        assertThrows(RuntimeException.class, () -> executor.doJob(null));

        verify(notificationServiceMock).send(notificationCtx);

        verify(n1Mock, atLeast(1)).getNotificationId();
        verify(n2Mock, atLeast(1)).getNotificationId();

        verify(n1Mock).getPartnerIds(null);
        verify(n1Mock).getPartnerNotification(partner1, null);
        verify(n1Mock).getPartnerNotification(partner2, null);
        verify(n1Mock).getNextNotificationTimeAfter(now);
        verify(n1Mock).prepareData();

        verifyNoMoreInteractions(n1Mock, n2Mock, notificationServiceMock);

        executor.doJob(null);

        verify(n1Mock, atLeast(1)).getNotificationId();
        verify(n2Mock, atLeast(1)).getNotificationId();
        verifyNoMoreInteractions(n1Mock, n2Mock, notificationServiceMock);
    }

    @Test
    @DisplayName("Отправка 1-му магазину: ошибка отправки первому партнеру не мешают отправке второму, джоба падает")
    void testSingleShopFirstPartnerNotificationSendError() {
        dao.upsert(n2Mock.getNotificationId(), now.plusSeconds(1));

        long partner1 = 234L;
        long partner2 = 345L;
        when(n1Mock.getPartnerIds(null)).thenReturn(List.of(partner1, partner2));

        var notificationCtx1 = mock(NotificationSendContext.class);
        var notificationCtx2 = mock(NotificationSendContext.class);

        when(n1Mock.getPartnerNotification(partner1, null)).thenReturn(Optional.of(notificationCtx1));
        when(n1Mock.getPartnerNotification(partner2, null)).thenReturn(Optional.of(notificationCtx2));

        when(notificationServiceMock.send(notificationCtx1)).thenThrow(new RuntimeException("can't send notification"));

        when(n1Mock.getNextNotificationTimeAfter(any())).thenReturn(now.plusSeconds(1));

        assertThrows(RuntimeException.class, () -> executor.doJob(null));

        verify(notificationServiceMock).send(notificationCtx1);
        verify(notificationServiceMock).send(notificationCtx2);

        verify(n1Mock, atLeast(1)).getNotificationId();
        verify(n2Mock, atLeast(1)).getNotificationId();

        verify(n1Mock).getPartnerIds(null);
        verify(n1Mock).getPartnerNotification(partner1, null);
        verify(n1Mock).getPartnerNotification(partner2, null);
        verify(n1Mock).getNextNotificationTimeAfter(now);
        verify(n1Mock).prepareData();

        verifyNoMoreInteractions(n1Mock, n2Mock, notificationServiceMock);

        executor.doJob(null);

        verify(n1Mock, atLeast(1)).getNotificationId();
        verify(n2Mock, atLeast(1)).getNotificationId();
        verifyNoMoreInteractions(n1Mock, n2Mock, notificationServiceMock);
    }

    @Test
    @DisplayName("Отправка списку магазинов: отсутствие партнеров у первого не мешает отправить второму")
    void testFirstShopHasNoPartners() {
        long partner1 = 234L;

        when(n1Mock.getPartnerIds(null)).thenReturn(Collections.emptyList());
        when(n2Mock.getPartnerIds(null)).thenReturn(List.of(partner1));

        var notificationCtx = mock(NotificationSendContext.class);
        when(n2Mock.getPartnerNotification(partner1, null)).thenReturn(Optional.of(notificationCtx));

        when(n1Mock.getNextNotificationTimeAfter(any())).thenReturn(now.plusSeconds(1));
        when(n2Mock.getNextNotificationTimeAfter(any())).thenReturn(now.plusSeconds(1));

        executor.doJob(null);

        verify(notificationServiceMock).send(notificationCtx);

        verify(n1Mock, atLeast(1)).getNotificationId();
        verify(n2Mock, atLeast(1)).getNotificationId();

        verify(n1Mock).getPartnerIds(null);
        verify(n2Mock).getPartnerIds(null);

        verify(n2Mock).getPartnerNotification(partner1, null);

        verify(n1Mock).getNextNotificationTimeAfter(now);
        verify(n2Mock).getNextNotificationTimeAfter(now);
        verify(n1Mock).prepareData();
        verify(n2Mock).prepareData();

        verifyNoMoreInteractions(n1Mock, n2Mock, notificationServiceMock);

        executor.doJob(null);

        verify(n1Mock, atLeast(1)).getNotificationId();
        verify(n2Mock, atLeast(1)).getNotificationId();

        verifyNoMoreInteractions(n1Mock, n2Mock, notificationServiceMock);
    }

    @Test
    @DisplayName("Отправка списку магазинов: ошибка получения партнеров первого не мешает отправить второму, " +
            "повторная отправка первому")
    void testFirstShopGetPartnersError() {
        long partner1 = 234L;

        when(n1Mock.getPartnerIds(null)).thenThrow(new RuntimeException("can't get partners"));
        when(n2Mock.getPartnerIds(null)).thenReturn(List.of(partner1));

        var notificationCtx = mock(NotificationSendContext.class);
        when(n2Mock.getPartnerNotification(partner1, null)).thenReturn(Optional.of(notificationCtx));

        when(n2Mock.getNextNotificationTimeAfter(any())).thenReturn(now.plusSeconds(1));

        assertThrows(RuntimeException.class, () -> executor.doJob(null));

        verify(notificationServiceMock).send(notificationCtx);

        verify(n1Mock, atLeast(1)).getNotificationId();
        verify(n2Mock, atLeast(1)).getNotificationId();

        verify(n1Mock).getPartnerIds(null);
        verify(n2Mock).getPartnerIds(null);

        verify(n2Mock).getPartnerNotification(partner1, null);
        verify(n1Mock).prepareData();
        verify(n2Mock).prepareData();

        // не сохраняет NextNotificationTimeAfter для n1Mock, тк была ошибка получения партнеров
        verify(n2Mock).getNextNotificationTimeAfter(now);

        verifyNoMoreInteractions(n1Mock, n2Mock, notificationServiceMock);

        assertThrows(RuntimeException.class, () -> executor.doJob(null));

        verify(n1Mock, atLeast(1)).getNotificationId();
        verify(n2Mock, atLeast(1)).getNotificationId();

        verify(n1Mock, atLeast(1)).getPartnerIds(null);
        verify(n1Mock, atLeast(1)).prepareData();
        verify(n2Mock, atLeast(1)).prepareData();

        verifyNoMoreInteractions(n1Mock, n2Mock, notificationServiceMock);
    }
}
