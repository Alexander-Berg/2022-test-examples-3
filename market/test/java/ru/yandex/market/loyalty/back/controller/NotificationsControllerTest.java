package ru.yandex.market.loyalty.back.controller;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.api.model.notifications.NotificationAcceptRequest;
import ru.yandex.market.loyalty.api.model.notifications.NotificationReferralPayloadResponse;
import ru.yandex.market.loyalty.api.model.notifications.NotificationType;
import ru.yandex.market.loyalty.api.model.notifications.ReferralNotificationPayload;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.client.MarketLoyaltyClient;
import ru.yandex.market.loyalty.core.dao.ydb.NotificationDao;
import ru.yandex.market.loyalty.core.model.notification.Notification;
import ru.yandex.market.loyalty.core.stub.NotificationDaoStub;
import ru.yandex.market.loyalty.core.utils.UserDataFactory;
import ru.yandex.market.loyalty.test.TestFor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@TestFor(NotificationsController.class)
public class NotificationsControllerTest extends MarketLoyaltyBackMockedDbTestBase {
    @Autowired
    private NotificationDao notificationDao;
    @Autowired
    private MarketLoyaltyClient marketLoyaltyClient;

    @Test
    public void shouldReturnNotificationStatus() throws JsonProcessingException {
        long testUid = insertNotificationAndReturnUid();
        List<NotificationReferralPayloadResponse> collect =
                marketLoyaltyClient.currentNotifications(testUid, true).stream().map(e -> (NotificationReferralPayloadResponse) e).collect(Collectors.toList());
        assertEquals(2, collect.size());
        assertTrue(collect.stream().anyMatch(n -> n.getType() == NotificationType.REFERRAL_ACCRUAL));
        assertTrue(collect.stream().anyMatch(n -> n.getType() == NotificationType.REFERRAL_REFUND));
        assertEquals("1200",
                collect.stream().filter(n -> n.getType() == NotificationType.REFERRAL_ACCRUAL).findAny().get().getParams().get("amount"));
    }

    @Test
    public void shouldMarkAsReadNotification() throws JsonProcessingException {
        long testUid = insertNotificationAndReturnUid();

        List<NotificationReferralPayloadResponse> notificationPayloadResponses =
                marketLoyaltyClient.currentNotifications(UserDataFactory.DEFAULT_UID, true);

        List<String> collect = notificationPayloadResponses.stream()
                .map(NotificationReferralPayloadResponse::getId)
                .collect(Collectors.toList());
        NotificationAcceptRequest notificationAcceptRequest = new NotificationAcceptRequest();
        notificationAcceptRequest.setNotificationIds(collect);
        notificationAcceptRequest.setUid(UserDataFactory.DEFAULT_UID);
        marketLoyaltyClient.acceptNotifications(notificationAcceptRequest);

        List<Notification> notifications = notificationDao.findNotifications(UserDataFactory.DEFAULT_UID);
        for (Notification notification : notifications) {
            assertTrue(notification.isRead());
        }

        List<NotificationReferralPayloadResponse> notificationPayloadResponsesAfterAccept =
                marketLoyaltyClient.currentNotifications(UserDataFactory.DEFAULT_UID, true);
        assertTrue(notificationPayloadResponsesAfterAccept.isEmpty());
    }

    @Test
    public void shouldInsert() {
        marketLoyaltyClient.insertNotifications("someId", UserDataFactory.ANOTHER_UID, 101, true, 1);

        List<Notification> notifications = notificationDao.findNotifications(UserDataFactory.ANOTHER_UID);
        for (Notification notification : notifications) {
            assertFalse(notification.isRead());
        }

        List<NotificationReferralPayloadResponse> notificationPayloadResponses =
                marketLoyaltyClient.currentNotifications(UserDataFactory.ANOTHER_UID, false);

        assertFalse(notificationPayloadResponses.isEmpty());
    }

    private long insertNotificationAndReturnUid() throws JsonProcessingException {
        ((NotificationDaoStub) notificationDao).clear();
        String testUid = UUID.randomUUID().toString();
        String testUid2 = UUID.randomUUID().toString();
        Notification one = new Notification(testUid, UserDataFactory.DEFAULT_UID, NotificationType.REFERRAL_ACCRUAL,
                Instant.now().minus(Duration.ofDays(4)), Instant.now().plus(Duration.ofDays(4)),
                new ObjectMapper().writeValueAsString(new ReferralNotificationPayload(600, 1, null, 0)), false);
        Notification two = new Notification(testUid2, UserDataFactory.DEFAULT_UID, NotificationType.REFERRAL_ACCRUAL,
                Instant.now().minus(Duration.ofDays(4)), Instant.now().plus(Duration.ofDays(4)),
                new ObjectMapper().writeValueAsString(new ReferralNotificationPayload(600, 2, null, 0)), false);
        Notification three = new Notification(testUid, UserDataFactory.DEFAULT_UID, NotificationType.REFERRAL_REFUND,
                Instant.now().minus(Duration.ofDays(4)), Instant.now().plus(Duration.ofDays(4)),
                new ObjectMapper().writeValueAsString(new ReferralNotificationPayload(300, 1, null, 10)), false);
        Notification four = new Notification(testUid, UserDataFactory.DEFAULT_UID, NotificationType.REFERRAL_ACCRUAL,
                Instant.now().minus(Duration.ofDays(4)), Instant.now().plus(Duration.ofDays(4)),
                new ObjectMapper().writeValueAsString(new ReferralNotificationPayload(600, 1, null, 110)), true);
        notificationDao.insertNotification(one);
        notificationDao.insertNotification(two);
        notificationDao.insertNotification(three);
        notificationDao.insertNotification(four);
        return UserDataFactory.DEFAULT_UID;
    }
}
