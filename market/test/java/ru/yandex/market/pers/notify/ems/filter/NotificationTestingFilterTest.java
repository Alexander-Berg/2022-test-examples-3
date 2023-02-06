package ru.yandex.market.pers.notify.ems.filter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.pers.notify.ems.configuration.NotificationConfigFactory;
import ru.yandex.market.pers.notify.model.NotificationSubtype;
import ru.yandex.market.pers.notify.ems.event.NotificationEvent;
import ru.yandex.market.pers.notify.test.MarketMailerMockedDbTest;
import ru.yandex.market.pers.notify.test.RunInTesting;
import ru.yandex.market.pers.notify.testing.TestAddressService;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Ivan Anisimov
 *         valter@yandex-team.ru
 *         22.09.15
 */
@RunInTesting
public class NotificationTestingFilterTest extends MarketMailerMockedDbTest {
    public static final String WHITELIST_EMAIL = "valetr@yandex.ru";
    @Autowired
    private NotificationTestingFilter notificationTestingFilter;
    @Autowired
    private TestAddressService testAddressService;

    @Test
    public void testCache() {
        testAddressService.addTestEmail(WHITELIST_EMAIL);

        NotificationEvent event = new NotificationEvent();
        event.setAddress(WHITELIST_EMAIL);
		event.setNotificationSubtype(NotificationSubtype.PA_WELCOME);
		event.setConfig(new NotificationConfigFactory().eventConfig(NotificationSubtype.PA_WELCOME));

		assertTrue(notificationTestingFilter.filter(event, null) != NotificationTestingFilter.DO_NOT_SEND_CONSUMER);

		event.setAddress("123@yandex.ru");

		assertTrue(notificationTestingFilter.filter(event, null) == NotificationTestingFilter.DO_NOT_SEND_CONSUMER);
    }
}
