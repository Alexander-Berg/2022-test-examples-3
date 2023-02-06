package ru.yandex.market.pers.notify.api.service.sk;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.notify.mock.MockedBlackBoxPassportService;
import ru.yandex.market.pers.notify.model.Email;
import ru.yandex.market.pers.notify.model.NotificationType;
import ru.yandex.market.pers.notify.model.SubscriptionSettings;
import ru.yandex.market.pers.notify.model.Uid;
import ru.yandex.market.pers.notify.model.sk.SecretKeyData;
import ru.yandex.market.pers.notify.model.subscription.EmailSubscription;
import ru.yandex.market.pers.notify.model.subscription.EmailSubscriptionParam;
import ru.yandex.market.pers.notify.model.subscription.EmailSubscriptionStatus;
import ru.yandex.market.pers.notify.settings.SubscriptionAndIdentityService;
import ru.yandex.market.pers.notify.test.MockedDbTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.pers.notify.mock.MockFactory.SBER_ID;


public class UnsubscribeMailManagerTest extends MockedDbTest {
    private static final String EMAIL = "unsub-mailer@yandex.ru";
    private static final long UID = 12345;

    @Autowired
    private UnsubscribeMailManager manager;
    @Autowired
    private SubscriptionAndIdentityService subscriptionAndIdentityService;
    @Autowired
    private MockedBlackBoxPassportService blackBoxPassportService;

    @BeforeEach
    public void activeEmails() {
        blackBoxPassportService.doReturn(UID, EMAIL);
    }

    @Test
    public void testUnsubscribeAllByEmailInNewModel() {
        subscriptionAndIdentityService.setSubscriptionsSettings(new Uid(UID), EMAIL, new SubscriptionSettings(true, true, true, false, true));

        SecretKeyData data = new SecretKeyData().addEmail(EMAIL).addType(NotificationType.ALL);
        manager.unsubscribe(data);

        assertTrue(data.isSuccess());
        SubscriptionSettings settings = subscriptionAndIdentityService.getSubscriptionsSettings(new Uid(UID), EMAIL);
        assertEquals(new SubscriptionSettings(false, false, false, false, false), settings);
    }

    @Test
    public void testUnsubscribeAllByEmail2InNewModel() {
        subscriptionAndIdentityService.setSubscriptionsSettings(new Uid(UID), EMAIL, new SubscriptionSettings(true, true, true, false, true));

        SecretKeyData data = new SecretKeyData().addEmail(EMAIL).addType(NotificationType.ALL);
        manager.unsubscribe(data);

        assertTrue(data.isSuccess());
        SubscriptionSettings settings = subscriptionAndIdentityService.getSubscriptionsSettings(new Uid(UID), EMAIL);
        assertEquals(new SubscriptionSettings(false, false, false, false, false), settings);
    }

    @Test
    public void unsubscribePaOnSaleWhenNothingInDb() {
        EmailSubscription subscription = new EmailSubscription(EMAIL, NotificationType.PA_ON_SALE, EmailSubscriptionStatus.CONFIRMED);
        subscription.addParameter(EmailSubscriptionParam.PARAM_MODEL_ID, "1");
        SecretKeyData data = new SecretKeyData().addEmail(EMAIL).addEmailSubscription(subscription);
        manager.unsubscribe(data);

        assertTrue(data.isSuccess());
        assertEquals(0, subscriptionAndIdentityService
            .getEmailSubscriptions(EMAIL, NotificationType.PA_ON_SALE).size());
    }

    @Test
    public void unsubscribePaOnSale() {
        subscriptionAndIdentityService.setSubscriptionsSettings(new Uid(UID), EMAIL, new SubscriptionSettings(true, true, true, false, false));

        EmailSubscription subscription1 = new EmailSubscription(EMAIL, NotificationType.PA_ON_SALE, EmailSubscriptionStatus.CONFIRMED);
        subscription1.addParameter(EmailSubscriptionParam.PARAM_MODEL_ID, "1");
        EmailSubscription subscription2 = new EmailSubscription(EMAIL, NotificationType.PA_ON_SALE, EmailSubscriptionStatus.CONFIRMED);
        subscription2.addParameter(EmailSubscriptionParam.PARAM_MODEL_ID, "1");
        EmailSubscription subscription3 = new EmailSubscription(EMAIL, NotificationType.PA_ON_SALE, EmailSubscriptionStatus.CONFIRMED);
        subscription3.addParameter(EmailSubscriptionParam.PARAM_MODEL_ID, "2");


        subscriptionAndIdentityService.updateSubscriptions(EMAIL, new ArrayList<EmailSubscription>() {{
            add(subscription1);
            add(subscription2);
            add(subscription3);
        }});
        List<EmailSubscription> subscriptions = subscriptionAndIdentityService
            .getEmailSubscriptions(EMAIL, NotificationType.PA_ON_SALE);

        assertEquals(3, subscriptions.size());
        SecretKeyData data = new SecretKeyData().addEmail(EMAIL).addEmailSubscription(subscription1);
        manager.unsubscribe(data);

        assertTrue(data.isSuccess());
        subscriptions = subscriptionAndIdentityService
            .getEmailSubscriptions(EMAIL, NotificationType.PA_ON_SALE);
        assertEquals(1, subscriptions.size());
        assertEquals(subscription3, subscriptions.get(0));
    }

    @Test
    void getAvailableEmailsForPassportUid() {
        final Set<Email> availableEmails = subscriptionAndIdentityService.getAvailableEmails(Uid.of(1L));
        assertFalse(availableEmails.isEmpty());
    }

    @Test
    void getAvailableEmailsForSberId() {
        final Set<Email> availableEmails = subscriptionAndIdentityService.getAvailableEmails(Uid.of(SBER_ID));
        assertFalse(availableEmails.isEmpty());
        assertEquals(2, availableEmails.size());
        assertThat(availableEmails,
            containsInAnyOrder(new Email("first@email.com", false), new Email("second@email.com", false)));
    }
}
