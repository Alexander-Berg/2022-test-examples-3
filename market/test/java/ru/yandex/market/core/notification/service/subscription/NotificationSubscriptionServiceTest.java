package ru.yandex.market.core.notification.service.subscription;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.notification.mail.model.address.EmailAddress;
import ru.yandex.market.notification.model.transport.NotificationAddress;
import ru.yandex.market.notification.simple.model.type.NotificationTransport;

import static org.apache.commons.collections.ListUtils.union;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.market.core.notification.matcher.CollectionEqualsMatcher.equalToCollection;

/**
 * @author rinbik
 */
public class NotificationSubscriptionServiceTest extends FunctionalTest {
    private static final long NOTIFICATION_TEMPLATE_ID = 1631026286;
    private static final List<String> WHITE_LIST = List.of("go1@ya.ru");
    private static final List<String> BLACK_LIST = List.of("test@ya.ru");
    private static final List<String> ALL_LIST = union(WHITE_LIST, BLACK_LIST);
    private static final Collection<NotificationAddress> ALL_EMAILS = toCollection(ALL_LIST);
    private static final Collection<NotificationAddress> WHITE_EMAILS = toCollection(WHITE_LIST);
    private static final long NEW_CAMPAIGN = 109L;
    private static final long OLD_CAMPAIGN = 107L;

    @Autowired
    private NotificationSubscriptionService notificationSubscriptionService;

    @DbUnitDataSet(before = "db/NotificationSubscriptionServiceTest.email.before.csv")
    @DisplayName("Проверка, что отписка адреса в старой кампании не влияет на подписку в текущей.")
    @Test
    void testEmailList() {
        var filtered = notificationSubscriptionService.getSubscribedAddresses(NOTIFICATION_TEMPLATE_ID,
                NotificationTransport.EMAIL, ALL_EMAILS, List.of(NEW_CAMPAIGN));
        assertThat(filtered, notNullValue());
        assertThat(filtered, equalToCollection(ALL_EMAILS));
    }

    @DbUnitDataSet(before = "db/NotificationSubscriptionServiceTest.email.before.csv")
    @DisplayName("Проверка, что идентификатор кампании учитывается при фильтрации адресов.")
    @Test
    void testEmailSkipped() {
        var filtered = notificationSubscriptionService.getSubscribedAddresses(NOTIFICATION_TEMPLATE_ID,
                NotificationTransport.EMAIL, ALL_EMAILS, List.of(OLD_CAMPAIGN));
        assertThat(filtered, notNullValue());
        assertThat(filtered, equalToCollection(WHITE_EMAILS));
    }

    private static Collection<NotificationAddress> toCollection(List<String> emails) {
        return emails.stream()
                .map(e -> EmailAddress.create(e, EmailAddress.Type.TO))
                .map(a -> (NotificationAddress) a)
                .collect(Collectors.toList());
    }
}
