package ru.yandex.market.pers.notify.api.controller.Util;

import java.util.Comparator;
import java.util.List;

import ru.yandex.market.pers.notify.model.subscription.EmailSubscription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class SubscriptionsUtil {

    public static void compareSubscriptions(List<EmailSubscription> result, List<EmailSubscription> checkable,
                                            boolean withParams) {
        Comparator<EmailSubscription> comparator = (o1, o2) -> {
            int emailsCompare = o1.getEmail().compareTo(o2.getEmail());
            int typesCompare = emailsCompare != 0 ? emailsCompare :
                    o1.getSubscriptionType().getId() - o2.getSubscriptionType().getId();
            return typesCompare != 0 ? typesCompare :
                    o1.getSubscriptionStatus().getId() - o2.getSubscriptionStatus().getId();
        };

        result.sort(comparator);
        checkable.sort(comparator);

        assertEquals(result.size(), checkable.size());
        for (int i = 0; i < result.size(); i++) {
            checkEmailSubscription(result.get(i), checkable.get(i), withParams);
        }
    }

    public static void checkEmailSubscription(EmailSubscription result, EmailSubscription checked, boolean withParams) {
        assertNotNull(checked.getId());
        assertEquals(result.getSubscriptionType(), checked.getSubscriptionType());
        assertEquals(result.getEmail().toLowerCase(), checked.getEmail().toLowerCase());
        assertEquals(result.getSubscriptionStatus(), checked.getSubscriptionStatus());
        assertEquals(result.getIp(), checked.getIp());
        if (withParams) {
            for (String param : result.getParameters().keySet()) {
                assertEquals(result.getParameters().get(param), checked.getParameters().get(param), param);
            }
        }
    }
}
