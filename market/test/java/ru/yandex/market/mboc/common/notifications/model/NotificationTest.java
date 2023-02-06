package ru.yandex.market.mboc.common.notifications.model;

import java.time.LocalDateTime;

import org.assertj.core.api.Assertions;
import org.junit.Test;

/**
 * @author prediger
 */
public class NotificationTest {

    @Test
    public void testNullData() {
        Notification notification = new Notification();
        Assertions.assertThatThrownBy(() -> notification.setData(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testStatusTsChange() {
        Notification notification = new Notification();
        LocalDateTime statusTsOld = LocalDateTime.parse("2018-10-01T00:00:00.001");
        notification.setStatusTsInternal(statusTsOld);

        notification.setStatusInternal(Notification.Status.SENT);
        Assertions.assertThat(notification.getStatusTs()).isEqualTo(statusTsOld);

        notification.updateStatus(Notification.Status.NEW);
        Assertions.assertThat(notification.getStatusTs()).isAfter(statusTsOld);
    }

    @Test
    public void testStatusTsSameStatus() {
        Notification notification = new Notification();
        LocalDateTime statusTsOld = LocalDateTime.parse("2018-10-01T00:00:00.001");
        notification.setStatusTsInternal(statusTsOld);

        notification.updateStatus(Notification.Status.NEW);
        Assertions.assertThat(notification.getStatusTs()).isEqualTo(statusTsOld);
    }
}
