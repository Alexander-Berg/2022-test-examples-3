package ru.yandex.market.notification.safe.service.provider.impl;

import java.time.Instant;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import ru.yandex.market.notification.safe.dao.PersistentNotificationDao;
import ru.yandex.market.notification.safe.model.PersistentNotification;
import ru.yandex.market.notification.safe.model.data.PersistentBinaryData;
import ru.yandex.market.notification.safe.model.data.PersistentDeliveryData;
import ru.yandex.market.notification.safe.model.type.NotificationSpamStatus;
import ru.yandex.market.notification.safe.model.type.NotificationStatus;
import ru.yandex.market.notification.safe.model.vo.SpamCheckInfo;
import ru.yandex.market.notification.simple.model.type.CodeNotificationType;
import ru.yandex.market.notification.simple.model.type.NotificationPriority;
import ru.yandex.market.notification.simple.model.type.NotificationTransport;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit-тесты для {@link SimpleSpamFilterProvider}.
 *
 * @author Vladislav Bauer
 */
public class SimpleSpamFilterProviderTest {

    @Test
    public void testProvideNotSpam() {
        checkIt(new SpamCheckInfo(0, 0), NotificationSpamStatus.NOT_SPAM);
    }

    @Test
    public void testProvideSpam() {
        checkIt(new SpamCheckInfo(0, 1), NotificationSpamStatus.SPAM);
    }

    @Test
    public void testProvideUnknown() {
        checkIt(new SpamCheckInfo(1, 0), NotificationSpamStatus.UNKNOWN);
    }


    private void checkIt(SpamCheckInfo info, NotificationSpamStatus spamStatus) {
        var notification = createNotification();
        var provider = createSpamFilterProvider(info);
        var status = provider.provide(notification);

        assertThat(status, equalTo(spamStatus));
    }

    private PersistentNotification createNotification() {
        var now = Instant.now();
        var deliveryData = new PersistentDeliveryData(
            NotificationTransport.EMAIL, NotificationPriority.HIGH, now, null, 0
        );
        var binaryData = new PersistentBinaryData(new byte[]{}, "");
        return new PersistentNotification(
            1L, 2L, new CodeNotificationType(3L), NotificationStatus.NEW, now, binaryData, deliveryData, "",
            false
        );
    }

    private SimpleSpamFilterProvider createSpamFilterProvider(SpamCheckInfo info) {
        var notificationDao = mock(PersistentNotificationDao.class);
        when(
            notificationDao.getSpamCheckInfo(
                anyLong(), any(NotificationTransport.class), anyString(), any(Instant.class), any(Instant.class),
                any(NotificationPriority.class), anyInt()
            )
        ).thenReturn(info);
        return new SimpleSpamFilterProvider(notificationDao, 1, 1);
    }

}
