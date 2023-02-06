package ru.yandex.market.pers.notify.api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.pers.notify.ems.event.NotificationEvent;
import ru.yandex.market.pers.notify.ems.persistence.EventSourceDAO;
import ru.yandex.market.pers.notify.mock.MockedBlackBoxPassportService;
import ru.yandex.market.pers.notify.model.Identity;
import ru.yandex.market.pers.notify.model.NotificationSubtype;
import ru.yandex.market.pers.notify.model.NotificationType;
import ru.yandex.market.pers.notify.model.Uid;
import ru.yandex.market.pers.notify.model.YandexUid;
import ru.yandex.market.pers.notify.model.event.NotificationEventDataName;
import ru.yandex.market.pers.notify.model.subscription.EmailSubscription;
import ru.yandex.market.pers.notify.model.subscription.EmailSubscriptionParam;
import ru.yandex.market.pers.notify.model.subscription.EmailSubscriptionStatus;
import ru.yandex.market.pers.notify.test.MarketUtilsMockedDbTest;
import ru.yandex.market.pers.notify.test.VerificationUtil;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JournalSubscriptionTest extends MarketUtilsMockedDbTest {
    private static final String EMAIL = "foo@bar.com";
    private static final Uid UID = new Uid(12345L);
    private static final YandexUid YANDEXUID = new YandexUid("12345qwerty");
    private static final String PLACE = "somehub";
    private static final String PLATFORM = "someplatform";


    @Autowired
    private SubscriptionControllerInvoker subscriptionControllerInvoker;
    @Autowired
    private SettingsControllerInvoker settingsControllerInvoker;
    @Autowired
    private VerificationUtil verificationUtil;
    @Autowired
    private MockedBlackBoxPassportService blackBoxPassportService;
    @Autowired
    private EventSourceDAO eventSourceDAO;

    @Test
    void journalSubscriptionForAuthorizedMustBeConfirmed() throws Exception {
        blackBoxPassportService.doReturn(UID.getValue(), EMAIL);
        createSubscriptionAndCheckStatus(UID, EmailSubscriptionStatus.CONFIRMED);
    }

    @Test
    void journalSubscriptionForYandexuidMustNeedConfirmation() throws Exception {
        createSubscriptionAndCheckStatus(YANDEXUID, EmailSubscriptionStatus.NEED_SEND_CONFIRMATION);
    }

    @Test
    void confirmationMailMustBeScheduled() throws Exception {
        EmailSubscription subscription = createSubscriptionAndCheckStatus(YANDEXUID, EmailSubscriptionStatus.NEED_SEND_CONFIRMATION);
        NotificationEvent event = eventSourceDAO.getLastEventByEmail(EMAIL);
        assertNotNull(event);
        assertEquals(NotificationSubtype.CONFIRM_SUBSCRIPTION, event.getNotificationSubtype());
        assertEquals(subscription.getId().longValue(),
                Long.parseLong(event.getData().get(NotificationEventDataName.SUBSCRIPTION_ID)));
    }

    @Test
    void journalSubscriptionCanBeConfirmed() throws Exception {
        EmailSubscription subscription = createSubscriptionAndCheckStatus(YANDEXUID,
                EmailSubscriptionStatus.NEED_SEND_CONFIRMATION);
        verificationUtil.confirmSubscription(subscription);
        assertEquals(EmailSubscriptionStatus.CONFIRMED, subscriptionControllerInvoker.getSubscription(subscription.getId()).getSubscriptionStatus());
    }

    @Test
    void placeAndPlatformMustBeSavedForJournalSubscription() throws Exception {
        blackBoxPassportService.doReturn(UID.getValue(), EMAIL);
        subscriptionControllerInvoker.createSubscriptions(UID, EMAIL,
                Collections.singletonList(EmailSubscription.builder()
                        .setSubscriptionType(NotificationType.JOURNAL)
                        .addParameter(EmailSubscriptionParam.PARAM_PLACE, PLACE)
                        .addParameter(EmailSubscriptionParam.PARAM_PLATFORM, PLATFORM)
                        .build()));

        EmailSubscription subscription = subscriptionControllerInvoker.getSubscriptions(UID).get(0);
        assertEquals(PLACE, subscription.getParameters().get(EmailSubscriptionParam.PARAM_PLACE));
        assertEquals(PLATFORM, subscription.getParameters().get(EmailSubscriptionParam.PARAM_PLATFORM));
    }

    @Test
    void journalSubscriptionsMustBeReturnedAfterPushingSettings() throws Exception {
        blackBoxPassportService.doReturn(UID.getValue(), EMAIL);
        settingsControllerInvoker.createSubscriptions(UID.getValue(), EMAIL, SettingsControllerTest.SUBSCRIBED_SETTINGS);
        List<EmailSubscription> subscriptions = subscriptionControllerInvoker.getSubscriptions(UID, NotificationType.JOURNAL);
        assertEquals(1, subscriptions.size());
        assertEquals(EmailSubscriptionStatus.CONFIRMED, subscriptions.get(0).getSubscriptionStatus());
    }

    @Test
    void journalMustBeOnInSettingsAfterCreatingSubscription() throws Exception {
        blackBoxPassportService.doReturn(UID.getValue(), EMAIL);
        createJournalSubscription(UID);
        settingsControllerInvoker.checkSubscriptions(UID.getValue(), EMAIL, "/data/settings/subscribed_journal_only.json");
    }

    private EmailSubscription createSubscriptionAndCheckStatus(Identity identity, EmailSubscriptionStatus expectedStatus) throws Exception {
        createJournalSubscription(identity);
        List<EmailSubscription> subscriptions = subscriptionControllerInvoker.getSubscriptions(identity);
        assertEquals(1, subscriptions.size());
        assertEquals(NotificationType.JOURNAL, subscriptions.get(0).getSubscriptionType());
        assertEquals(expectedStatus, subscriptions.get(0).getSubscriptionStatus());
        return subscriptions.get(0);
    }

    private void createJournalSubscription(Identity identity) throws Exception {
        subscriptionControllerInvoker.createSubscriptions(identity, EMAIL,
                Collections.singletonList(EmailSubscription.builder()
                        .setSubscriptionType(NotificationType.JOURNAL).build()));
    }


}
