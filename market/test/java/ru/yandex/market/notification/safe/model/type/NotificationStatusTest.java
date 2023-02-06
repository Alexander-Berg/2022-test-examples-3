package ru.yandex.market.notification.safe.model.type;

import org.junit.Test;

import ru.yandex.market.notification.test.model.AbstractModelTest;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Unit-тесты для {@link NotificationStatus}.
 *
 * @author Vladislav Bauer
 */
public class NotificationStatusTest extends AbstractModelTest {

    @Test
    public void testGetSentStatus() {
        assertThat(NotificationStatus.getSentStatus(true), equalTo(NotificationStatus.SENT));
        assertThat(NotificationStatus.getSentStatus(false), equalTo(NotificationStatus.SENDING_ERROR));
    }

    /**
     * <b>Если набор {@link NotificationStatus} изменился, необходимо также поправить отчеты и мониторинги.</b>
     */
    @Test
    public void testCodes() {
        checkEnum(NotificationStatus.class, 7);

        assertThatCode(NotificationStatus.NEW, 1);
        assertThatCode(NotificationStatus.PREPARED, 2);
        assertThatCode(NotificationStatus.PREPARATION_ERROR, 3);
        assertThatCode(NotificationStatus.SENT, 4);
        assertThatCode(NotificationStatus.SENDING_ERROR, 5);
        assertThatCode(NotificationStatus.SPAM, 6);
        assertThatCode(NotificationStatus.NO_RECIPIENT, 7);
    }

}
