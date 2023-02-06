package ru.yandex.market.notification.safe.composer.impl;

import java.time.Instant;

import org.junit.Test;

import ru.yandex.market.notification.model.data.NotificationContent;
import ru.yandex.market.notification.model.transport.NotificationAddress;
import ru.yandex.market.notification.model.transport.NotificationDestination;
import ru.yandex.market.notification.safe.composer.NotificationComposer;
import ru.yandex.market.notification.safe.composer.NotificationComposerFacade;
import ru.yandex.market.notification.safe.model.PersistentNotification;
import ru.yandex.market.notification.safe.model.PersistentNotificationAddress;
import ru.yandex.market.notification.safe.model.PersistentNotificationDestination;
import ru.yandex.market.notification.safe.model.data.PersistentBinaryData;
import ru.yandex.market.notification.safe.model.data.PersistentDeliveryData;
import ru.yandex.market.notification.safe.model.type.NotificationAddressStatus;
import ru.yandex.market.notification.safe.model.type.NotificationStatus;
import ru.yandex.market.notification.simple.model.type.CodeNotificationType;
import ru.yandex.market.notification.simple.model.type.NotificationPriority;
import ru.yandex.market.notification.simple.model.type.NotificationTransport;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Unit-тесты для {@link NotificationComposerImpl}.
 *
 * @author Vladislav Bauer
 */
public class NotificationComposerImplTest extends AbstractComposerTest {

    @Test
    public void testComposeContent() {
        final PersistentNotification persistentNotification = createNotification();
        final NotificationComposerFacade facade = createFacade();
        final NotificationComposer composer = new NotificationComposerImpl(facade);
        final NotificationContent content = composer.composeContent(persistentNotification);

        assertThat(content, notNullValue());

        verify(facade, times(1)).getContentTypeDetector();
        verify(facade, times(1)).getDataSerializer();
        verifyNoMoreInteractions(facade);
    }

    @Test
    public void testComposeAddress() {
        final PersistentNotificationAddress persistentAddress = createAddress();
        final NotificationComposerFacade facade = createFacade();
        final NotificationComposer composer = new NotificationComposerImpl(facade);
        final NotificationAddress address = composer.composeAddress(persistentAddress);

        assertThat(address, notNullValue());

        verify(facade, times(1)).getAddressTypeDetector();
        verify(facade, times(1)).getDataSerializer();
        verifyNoMoreInteractions(facade);
    }

    @Test
    public void testComposeDestination() {
        final PersistentNotificationDestination persistentDestination = createDestination();
        final NotificationComposerFacade facade = createFacade();
        final NotificationComposer composer = new NotificationComposerImpl(facade);
        final NotificationDestination destination = composer.composeDestination(persistentDestination);

        assertThat(destination, notNullValue());

        verify(facade, times(1)).getDestinationTypeDetector();
        verify(facade, times(1)).getDataSerializer();
        verifyNoMoreInteractions(facade);
    }


    private PersistentBinaryData createBinaryData() {
        return new PersistentBinaryData(TEST_DATA, TEST_TYPE);
    }

    private PersistentNotificationAddress createAddress() {
        final PersistentBinaryData binaryData = createBinaryData();
        return new PersistentNotificationAddress(1L, 2L, 3L, binaryData, NotificationAddressStatus.NEW);
    }

    private PersistentNotificationDestination createDestination() {
        final PersistentBinaryData data = createBinaryData();
        return new PersistentNotificationDestination(1L, 2L, data);
    }

    private PersistentNotification createNotification() {
        final Instant now = Instant.now();
        final PersistentBinaryData binaryData = createBinaryData();
        final CodeNotificationType type = new CodeNotificationType(3L);
        final PersistentDeliveryData deliveryData = new PersistentDeliveryData(
            NotificationTransport.EMAIL, NotificationPriority.NORMAL, now, now, 3
        );
        return new PersistentNotification(1L, 2L, type, NotificationStatus.NEW, now, binaryData, deliveryData, "");
    }

}
