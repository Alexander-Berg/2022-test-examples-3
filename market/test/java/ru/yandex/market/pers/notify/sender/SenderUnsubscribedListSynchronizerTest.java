package ru.yandex.market.pers.notify.sender;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.notify.api.service.subscription.EmailSubscriptionDAO;
import ru.yandex.market.pers.notify.model.SenderAccount;
import ru.yandex.market.pers.notify.external.sender.SenderClient;
import ru.yandex.market.pers.notify.model.subscription.EmailSubscription;
import ru.yandex.market.pers.notify.settings.SubscriptionAndIdentityService;
import ru.yandex.market.pers.notify.test.MarketMailerMockedDbTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.pers.notify.model.NotificationType.STORE_ADVERTISING;
import static ru.yandex.market.pers.notify.model.subscription.EmailSubscriptionStatus.CONFIRMED;

public class SenderUnsubscribedListSynchronizerTest extends MarketMailerMockedDbTest {

    @Autowired
    private SubscriptionAndIdentityService subscriptionAndIdentityService;

    @Autowired
    private SenderUnsubscribedListSynchronizer storeSenderUnsubscribedListSynchronizer;

    @Autowired
    private SenderClient senderClient;

    @Autowired
    private EmailSubscriptionDAO subscriptionDAO;

    @Test
    public void testEmailFromSenderListIsUnsubscribed() throws IOException {
        EmailSubscription subscription1 = createSubscription("test_1@email.ru");
        EmailSubscription subscription2 = createSubscription("test_2@email.ru");

        when(senderClient.getUnsubscribedListContent(eq("8YNJPZT2-RW"), eq(SenderAccount.STORE)))
                .thenReturn(ImmutableSet.of(subscription1.getEmail()));

        when(senderClient.getUnsubscribedListContent(eq("K70DVZW2-SDD1"), eq(SenderAccount.STORE)))
                .thenReturn(ImmutableSet.of(subscription2.getEmail()));

        List<EmailSubscription> subscriptions1 =
                subscriptionAndIdentityService.getEmailSubscriptions(subscription1.getEmail());
        assertEquals(1, subscriptions1.size());

        List<EmailSubscription> subscriptions2 =
                subscriptionAndIdentityService.getEmailSubscriptions(subscription2.getEmail());
        assertEquals(1, subscriptions2.size());

        storeSenderUnsubscribedListSynchronizer.doSync();

        subscriptions1 = subscriptionAndIdentityService.getEmailSubscriptions(subscription1.getEmail());
        assertTrue(subscriptions1.isEmpty());

        subscriptions2 = subscriptionAndIdentityService.getEmailSubscriptions(subscription2.getEmail());
        assertTrue(subscriptions2.isEmpty());


        verify(senderClient, atLeastOnce())
                .removeFromUnsubscribedList(eq("8YNJPZT2-RW"), eq(subscription1.getEmail()), eq(SenderAccount.STORE));
        verify(senderClient, atLeastOnce())
                .removeFromUnsubscribedList(eq("K70DVZW2-SDD1"), eq(subscription2.getEmail()), eq(SenderAccount.STORE));
    }

    private EmailSubscription createSubscription(String email) {
        EmailSubscription subscription = new EmailSubscription(email, STORE_ADVERTISING, CONFIRMED);
        subscriptionDAO.saveSubscriptions(email, Collections.singletonList(subscription));
        return subscription;
    }
}