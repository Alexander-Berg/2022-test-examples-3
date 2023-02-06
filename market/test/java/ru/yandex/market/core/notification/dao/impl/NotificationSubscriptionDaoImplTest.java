package ru.yandex.market.core.notification.dao.impl;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.notification.model.subscription.NotificationSubscription;
import ru.yandex.market.core.notification.model.subscription.SubscriptionStatus;
import ru.yandex.market.notification.mail.model.address.EmailAddress;
import ru.yandex.market.notification.simple.model.type.NotificationTransport;
import ru.yandex.market.notification.telegram.bot.model.address.TelegramIdAddress;

class NotificationSubscriptionDaoImplTest extends FunctionalTest {
    private static final long NOTIFICATION_TYPE = 1L;
    private static final List<Long> CAMPAIGN_IDS = List.of(1L);
    private static final List<EmailAddress> EMAILS = List.of(EmailAddress.create("email", EmailAddress.Type.TO));
    private static final List<TelegramIdAddress> TGS = List.of(TelegramIdAddress.create("bot", 1L));

    @Autowired
    NotificationSubscriptionDaoImpl dao;

    private static Stream<Map.Entry<String, List<Long>>> campaignIds() {
        return Stream.of(
                Map.entry("c=no", List.of()),
                Map.entry("c=some", CAMPAIGN_IDS)
        );
    }

    private static Stream<Map.Entry<String, List<TelegramIdAddress>>> tgs() {
        return Stream.of(
                Map.entry("t=no", List.of()),
                Map.entry("t=some", TGS)
        );
    }

    private static Stream<Map.Entry<String, List<EmailAddress>>> emails() {
        return Stream.of(
                Map.entry("e=no", List.of()),
                Map.entry("e=some", EMAILS)
        );
    }

    static Stream<Arguments> getSubscribedTelegramIdsData() {
        return tgs().flatMap(tg -> campaignIds().map(cid -> Arguments.of(
                tg.getKey() + "," + cid.getKey(), tg.getValue(), cid.getValue()
        )));
    }

    @ParameterizedTest(name = "{displayName} - {0}")
    @MethodSource("getSubscribedTelegramIdsData")
    void getSubscribedTelegramIds(String name, List<TelegramIdAddress> tgs, List<Long> campaignIds) {
        dao.getSubscribedTelegramIds(
                NOTIFICATION_TYPE,
                tgs,
                campaignIds
        );
    }

    static Stream<Arguments> getUnsubscribedTelegramIdsData() {
        return tgs().map(tg -> Arguments.of(
                tg.getKey(), tg.getValue()
        ));
    }

    @ParameterizedTest(name = "{displayName} - {0}")
    @MethodSource("getUnsubscribedTelegramIdsData")
    void getUnsubscribedTelegramIds(String name, List<TelegramIdAddress> tgs) {
        dao.getUnsubscribedTelegramIds(
                NOTIFICATION_TYPE,
                tgs
        );
    }

    static Stream<Arguments> getUnsubscribedEmailsData() {
        return emails().flatMap(e -> campaignIds().map(cid -> Arguments.of(
                e.getKey() + "," + cid.getKey(), e.getValue(), cid.getValue()
        )));
    }

    @ParameterizedTest(name = "{displayName} - {0}")
    @MethodSource("getUnsubscribedEmailsData")
    void getUnsubscribedEmails(String name, List<EmailAddress> emails, List<Long> campaignIds) {
        dao.getUnsubscribedEmails(
                NOTIFICATION_TYPE,
                emails,
                campaignIds
        );
    }

    @Test
    void updateStatus() {
        var contactId = 100L;
        dao.updateStatus(contactId, List.of(new NotificationSubscription(
                NOTIFICATION_TYPE,
                NotificationTransport.EMAIL,
                contactId,
                SubscriptionStatus.SUBSCRIBED,
                1L
        )));
    }
}
