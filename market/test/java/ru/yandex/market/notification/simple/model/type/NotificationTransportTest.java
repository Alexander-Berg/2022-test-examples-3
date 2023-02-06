package ru.yandex.market.notification.simple.model.type;

import org.junit.Test;

import ru.yandex.market.notification.test.model.AbstractModelTest;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Unit-тесты для {@link NotificationTransport}.
 *
 * @author Vladislav Bauer
 */
public class NotificationTransportTest extends AbstractModelTest {

    @Test
    public void testCodes() {
        checkEnum(NotificationTransport.class, 5);

        assertThatCode(NotificationTransport.EMAIL, 1);
        assertThatCode(NotificationTransport.SMS, 2);
        assertThatCode(NotificationTransport.MOBILE_PUSH, 3);
        assertThatCode(NotificationTransport.MBI_WEB_UI, 4);
        assertThatCode(NotificationTransport.TELEGRAM_BOT, 5);
    }

    @Test
    public void testAllowEmptyAddresses() {
        assertThat(NotificationTransport.EMAIL.allowEmptyAddresses(), equalTo(false));
        assertThat(NotificationTransport.SMS.allowEmptyAddresses(), equalTo(false));
        assertThat(NotificationTransport.MOBILE_PUSH.allowEmptyAddresses(), equalTo(false));
        assertThat(NotificationTransport.MBI_WEB_UI.allowEmptyAddresses(), equalTo(true));
        assertThat(NotificationTransport.TELEGRAM_BOT.allowEmptyAddresses(), equalTo(false));
    }

}
