package ru.yandex.market.notification.safe.service.provider.impl;

import java.time.Instant;

import org.junit.Test;

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

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
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


    private void checkIt(final SpamCheckInfo info, final NotificationSpamStatus spamStatus) {
        final PersistentNotification notification = createNotification();
        final SimpleSpamFilterProvider provider = createSpamFilterProvider(info);
        final NotificationSpamStatus status = provider.provide(notification);

        assertThat(status, equalTo(spamStatus));
    }

    private PersistentNotification createNotification() {
        final Instant now = Instant.now();
        final PersistentDeliveryData deliveryData = new PersistentDeliveryData(
            NotificationTransport.EMAIL, NotificationPriority.HIGH, now, null, 0
        );
        final PersistentBinaryData binaryData = new PersistentBinaryData(new byte[]{}, "");
        return new PersistentNotification(
            1L, 2L, new CodeNotificationType(3L), NotificationStatus.NEW, now, binaryData, deliveryData, ""
        );
    }

    private SimpleSpamFilterProvider createSpamFilterProvider(final SpamCheckInfo info) {
        final PersistentNotificationDao notificationDao = mock(PersistentNotificationDao.class);
        when(
            notificationDao.getSpamCheckInfo(
                anyLong(), any(NotificationTransport.class), anyString(), any(Instant.class), any(Instant.class),
                any(NotificationPriority.class), anyInt()
            )
        ).thenReturn(info);
        return new SimpleSpamFilterProvider(notificationDao, 1, 1);
    }

}
