package ru.yandex.market.notification.safe.model.type;

import org.junit.Test;

import ru.yandex.market.notification.test.model.AbstractModelTest;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Unit-тесты для {@link NotificationAddressStatus}.
 *
 * @author Vladislav Bauer
 */
public class NotificationAddressStatusTest extends AbstractModelTest {

    @Test
    public void testGetSentStatus() {
        assertThat(NotificationAddressStatus.getSentStatus(true), equalTo(NotificationAddressStatus.SENT));
        assertThat(NotificationAddressStatus.getSentStatus(false), equalTo(NotificationAddressStatus.ERROR));
    }

    @Test
    public void testCodes() {
        checkEnum(NotificationAddressStatus.class, 3);

        assertThatCode(NotificationAddressStatus.NEW, 1);
        assertThatCode(NotificationAddressStatus.SENT, 2);
        assertThatCode(NotificationAddressStatus.ERROR, 3);
    }

}
