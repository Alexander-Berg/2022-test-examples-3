package ru.yandex.market.notification.safe.composer.impl;

import java.time.Instant;

import javax.annotation.concurrent.Immutable;
import javax.xml.bind.annotation.XmlRootElement;

import org.junit.Test;

import ru.yandex.market.notification.model.data.NotificationContextData;
import ru.yandex.market.notification.simple.model.type.CodeNotificationType;
import ru.yandex.market.notification.simple.model.type.NotificationPriority;
import ru.yandex.market.notification.simple.model.type.NotificationTransport;
import ru.yandex.market.notification.safe.composer.NotificationComposerFacade;
import ru.yandex.market.notification.safe.composer.PersistentNotificationComposer;
import ru.yandex.market.notification.safe.model.PersistentNotification;
import ru.yandex.market.notification.safe.model.PersistentNotificationAddress;
import ru.yandex.market.notification.safe.model.PersistentNotificationDestination;
import ru.yandex.market.notification.safe.model.PersistentNotificationGroup;
import ru.yandex.market.notification.safe.model.data.PersistentBinaryData;
import ru.yandex.market.notification.safe.model.data.PersistentDeliveryData;
import ru.yandex.market.notification.safe.model.type.NotificationAddressStatus;
import ru.yandex.market.notification.safe.model.type.NotificationStatus;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Unit-тесты для {@link PersistentNotificationComposerImpl}.
 *
 * @author Vladislav Bauer
 */
public class PersistentNotificationComposerImplTest extends AbstractComposerTest {

    @Test
    public void testComposeDestination() {
        final long groupId = 1L;
        final TestDestination destination = new TestDestination();

        final NotificationComposerFacade facade = createFacade();
        final PersistentNotificationComposer composer = new PersistentNotificationComposerImpl(facade);
        final PersistentNotificationDestination persistentDestination = composer.composeDestination(groupId, destination);

        final PersistentBinaryData data = persistentDestination.getData();
        assertThat(persistentDestination.getId(), nullValue());
        assertThat(persistentDestination.getGroupId(), equalTo(groupId));
        assertThat(data.getType(), equalTo(TEST_TYPE));
        assertThat(data.getData(), equalTo(TEST_DATA));

        verify(facade, times(1)).getDestinationTypeDetector();
        verify(facade, times(1)).getDataSerializer();
        verifyNoMoreInteractions(facade);
    }

    @Test
    public void testComposeGroup() {
        final NotificationComposerFacade facade = createFacade();
        final PersistentNotificationComposer composer = new PersistentNotificationComposerImpl(facade);
        final PersistentNotificationGroup persistentGroup = composer.composeGroup(new TestData());

        final PersistentBinaryData data = persistentGroup.getContextData().orElseThrow(IllegalStateException::new);
        assertThat(persistentGroup.getId(), nullValue());
        assertThat(data.getType(), notNullValue());
        assertThat(data.getData(), notNullValue());

        verify(facade, times(1)).getDataSerializer();
        verifyNoMoreInteractions(facade);
    }

    @Test
    public void testComposeAddress() {
        final long notificationId = 1L;
        final long destinationId = 2L;

        final NotificationComposerFacade facade = createFacade();
        final PersistentNotificationComposer composer = new PersistentNotificationComposerImpl(facade);
        final PersistentNotificationAddress persistentAddress =
            composer.composeAddress(notificationId, destinationId, new TestAddress());

        final PersistentBinaryData data = persistentAddress.getData();
        assertThat(persistentAddress.getId(), nullValue());
        assertThat(persistentAddress.getStatus(), equalTo(NotificationAddressStatus.NEW));
        assertThat(persistentAddress.getDestinationId(), equalTo(destinationId));
        assertThat(persistentAddress.getNotificationId(), equalTo(notificationId));
        assertThat(data.getType(), equalTo(TEST_TYPE));
        assertThat(data.getData(), equalTo(TEST_DATA));

        verify(facade, times(1)).getAddressTypeDetector();
        verify(facade, times(1)).getDataSerializer();
        verifyNoMoreInteractions(facade);
    }

    @Test
    public void testComposeNotification() {
        final NotificationComposerFacade facade = createFacade();
        final PersistentNotificationComposer composer = new PersistentNotificationComposerImpl(facade);

        final long groupId = 1L;
        final String spamHash = "";
        final CodeNotificationType type = new CodeNotificationType(2L);
        final NotificationTransport transportType = NotificationTransport.EMAIL;
        final NotificationPriority priorityType = NotificationPriority.HIGH;
        final Instant deliveryTime = Instant.now();
        final TestContent content = new TestContent();

        final PersistentNotification persistentNotification = composer.composeNotification(
            groupId, type, transportType, priorityType, deliveryTime, content, spamHash, false
        );

        assertThat(persistentNotification.getId(), nullValue());
        assertThat(persistentNotification.getType(), equalTo(type));
        assertThat(persistentNotification.getCreatedTime(), notNullValue());
        assertThat(persistentNotification.getSpamHash(), equalTo(spamHash));
        assertThat(persistentNotification.getStatus(), equalTo(NotificationStatus.NEW));
        assertThat(persistentNotification.getGroupId(), equalTo(groupId));

        final PersistentBinaryData data = persistentNotification.getContent();
        assertThat(data.getType(), equalTo(TEST_TYPE));
        assertThat(data.getData(), equalTo(TEST_DATA));

        final PersistentDeliveryData deliveryData = persistentNotification.getDeliveryData();
        assertThat(deliveryData.getTransportType(), equalTo(transportType));
        assertThat(deliveryData.getDeliveryTime(), equalTo(deliveryTime));
        assertThat(deliveryData.getPriorityType(), equalTo(priorityType));
        assertThat(deliveryData.getTriesCount(), equalTo(0));
        assertThat(deliveryData.getSentTime(), nullValue());

        verify(facade, times(1)).getContentTypeDetector();
        verify(facade, times(1)).getDataSerializer();
        verifyNoMoreInteractions(facade);
    }


    @Immutable
    @XmlRootElement(name = "data")
    private static class TestData implements NotificationContextData {

        TestData() {
        }

    }

}
