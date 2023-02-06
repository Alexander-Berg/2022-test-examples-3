package ru.yandex.market.pers.qa.mock;

import ru.yandex.market.pers.notify.PersNotifyClient;
import ru.yandex.market.pers.notify.PersNotifyClientException;
import ru.yandex.market.pers.notify.model.Email;
import ru.yandex.market.pers.notify.model.EmailSubscriptionWriteRequest;
import ru.yandex.market.pers.notify.model.subscription.EmailSubscription;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 23.08.2018
 */
public class NotifyServiceMockUtils {
    private NotifyServiceMockUtils() {
    }

    public static void mockFailToSubscribe(PersNotifyClient client) {
        try {
            when(client.getEmails(anyLong()))
                .thenReturn(Collections.singleton(new Email("a@a.a", true)));
            when(client.createSubscriptions(any(EmailSubscriptionWriteRequest.class)))
                .thenThrow(PersNotifyClientException.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void mockNoNeedToSubscribe(PersNotifyClient client) {
        try {
            when(client.getEmails(anyLong()))
                .thenReturn(Collections.singleton(new Email("a@a.a", false)));
            when(client.createSubscriptions(any(EmailSubscriptionWriteRequest.class)))
                .thenThrow(PersNotifyClientException.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void mockSubscribeOk(PersNotifyClient client) {
        try {
            when(client.getEmails(anyLong()))
                .thenReturn(Collections.singleton(new Email("a@a.a", true)));
            when(client.createSubscriptions(any(EmailSubscriptionWriteRequest.class)))
                .thenReturn(Collections.singletonList(new EmailSubscription()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
