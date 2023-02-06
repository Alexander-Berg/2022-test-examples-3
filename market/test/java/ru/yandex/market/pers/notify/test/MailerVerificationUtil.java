package ru.yandex.market.pers.notify.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.yandex.market.pers.notify.api.service.sk.SecretKeyManager;
import ru.yandex.market.pers.notify.api.service.sk.UnsubscribeMailManager;
import ru.yandex.market.pers.notify.api.service.subscription.EmailSubscriptionDAO;
import ru.yandex.market.pers.notify.model.sk.SecretKeyData;
import ru.yandex.market.pers.notify.model.subscription.EmailSubscription;
import ru.yandex.market.pers.notify.model.subscription.EmailSubscriptionStatus;
import ru.yandex.market.pers.notify.model.web.PersNotifyTag;

import java.security.GeneralSecurityException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author semin-serg
 */
@Component
public class MailerVerificationUtil {

    private final SecretKeyManager secretKeyManager;
    private final UnsubscribeMailManager unsubscribeMailManager;
    private final EmailSubscriptionDAO emailSubscriptionDAO;

    @Autowired
    public MailerVerificationUtil(SecretKeyManager secretKeyManager, UnsubscribeMailManager unsubscribeMailManager,
                                  EmailSubscriptionDAO emailSubscriptionDAO) {
        this.secretKeyManager = secretKeyManager;
        this.unsubscribeMailManager = unsubscribeMailManager;
        this.emailSubscriptionDAO = emailSubscriptionDAO;
    }

    public void checkUnsubscribeLinkWorks(String action, String sk, long subscriptionId) {
        assertNotNull(action);
        assertNotNull(sk);
        SecretKeyData secretKeyData;
        try {
            secretKeyData = secretKeyManager.resolveLink(action, sk, PersNotifyTag.COMMAND_UNSUBSCRIBE);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("", e);
        }
        unsubscribeMailManager.unsubscribe(secretKeyData);
        EmailSubscription emailSubscription = emailSubscriptionDAO.getSubscription(subscriptionId);
        assertEquals(EmailSubscriptionStatus.UNSUBSCRIBED, emailSubscription.getSubscriptionStatus());
    }

}
