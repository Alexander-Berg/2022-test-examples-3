package ru.yandex.market.notification.safe.model;

import org.hamcrest.Matchers;
import org.junit.Test;

import ru.yandex.market.notification.safe.model.data.PersistentDeliveryData;
import ru.yandex.market.notification.simple.model.type.NotificationTransport;

import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit-тесты для {@link PersistentNotification}.
 *
 * @author Vladislav Bauer
 */
public class PersistentNotificationTest {

    @Test
    public void testGetTransport() {
        final PersistentDeliveryData deliveryData = mock(PersistentDeliveryData.class);
        final PersistentNotification notification = mock(PersistentNotification.class);
        final NotificationTransport transport = NotificationTransport.EMAIL;

        when(deliveryData.getTransportType()).thenReturn(transport);
        when(notification.getDeliveryData()).thenReturn(deliveryData);

        final NotificationTransport status = PersistentNotification.getTransport(notification);
        assertThat(status, Matchers.equalTo(transport));
    }

}
