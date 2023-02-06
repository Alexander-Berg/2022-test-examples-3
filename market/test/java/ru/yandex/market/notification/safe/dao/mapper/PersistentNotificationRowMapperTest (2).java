package ru.yandex.market.notification.safe.dao.mapper;

import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;

import javax.sql.rowset.serial.SerialClob;

import org.junit.Test;

import ru.yandex.market.notification.safe.dao.mapper.PersistentNotificationRowMapper.Columns;
import ru.yandex.market.notification.safe.model.PersistentNotification;
import ru.yandex.market.notification.safe.model.data.PersistentBinaryData;
import ru.yandex.market.notification.safe.model.data.PersistentDeliveryData;
import ru.yandex.market.notification.simple.model.type.CodeNotificationType;
import ru.yandex.market.notification.simple.model.type.NotificationPriority;
import ru.yandex.market.notification.simple.model.type.NotificationTransport;
import ru.yandex.market.notification.test.util.ClassUtils;
import ru.yandex.market.notification.test.util.DataSerializerUtils;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.notification.safe.dao.mapper.PersistentNotificationRowMapper.INSTANCE;
import static ru.yandex.market.notification.safe.model.type.NotificationStatus.PREPARED;
import static ru.yandex.market.notification.simple.model.type.NotificationPriority.NORMAL;
import static ru.yandex.market.notification.simple.model.type.NotificationTransport.EMAIL;

/**
 * Unit-тесты для {@link PersistentNotificationRowMapper}.
 *
 * @author Vladislav Bauer
 */
public class PersistentNotificationRowMapperTest {

    @Test
    public void testMapper() throws Exception {
        final Instant now = Instant.now();
        final NotificationTransport transport = EMAIL;
        final NotificationPriority priority = NORMAL;

        final CodeNotificationType notificationType = new CodeNotificationType(3L);
        final PersistentDeliveryData delivery = new PersistentDeliveryData(transport, priority, now, now, 3);
        final PersistentBinaryData binaryData = new PersistentBinaryData(new byte[]{1, 2, 3}, "test");

        final PersistentNotification expected = new PersistentNotification(
            1L, 2L, notificationType, PREPARED, now, binaryData, delivery, "", false);

        final ResultSet resultSet = mock(ResultSet.class);

        when(resultSet.getLong(AbstractPersistentRowMapper.Columns.COLUMN_ID)).thenReturn(expected.getId());
        when(resultSet.getLong(Columns.COLUMN_GROUP_ID)).thenReturn(expected.getGroupId());
        when(resultSet.getLong(Columns.COLUMN_TYPE)).thenReturn(notificationType.getId());
        when(resultSet.getInt(Columns.COLUMN_STATUS_TYPE)).thenReturn(expected.getStatus().getId());
        when(resultSet.getString(Columns.COLUMN_SPAM_HASH)).thenReturn(expected.getSpamHash());
        when(resultSet.getTimestamp(Columns.COLUMN_CREATED_TIME)).thenReturn(timestamp(expected.getCreatedTime()));

        when(resultSet.getInt(Columns.COLUMN_TRANSPORT_TYPE)).thenReturn(transport.getId());
        when(resultSet.getInt(Columns.COLUMN_PRIORITY_TYPE)).thenReturn(priority.getId());
        when(resultSet.getTimestamp(Columns.COLUMN_DELIVERY_TIME)).thenReturn(timestamp(delivery.getDeliveryTime()));
        when(resultSet.getTimestamp(Columns.COLUMN_SENT_TIME)).thenReturn(timestamp(delivery.getSentTime()));
        when(resultSet.getInt(Columns.COLUMN_TRIES_COUNT)).thenReturn(delivery.getTriesCount());

        final Clob clob = new SerialClob(DataSerializerUtils.toString(binaryData.getData()).toCharArray());
        when(resultSet.getString(Columns.COLUMN_CONTENT_TYPE)).thenReturn(binaryData.getType());
        when(resultSet.getClob(Columns.COLUMN_CONTENT)).thenReturn(clob);

        final PersistentNotification actual = INSTANCE.mapRow(resultSet, 0);

        assertThat(actual.getId(), equalTo(expected.getId()));
        assertThat(actual.getGroupId(), equalTo(expected.getGroupId()));
        assertThat(actual.getType(), equalTo(expected.getType()));
        assertThat(actual.getStatus(), equalTo(expected.getStatus()));
        assertThat(actual.getSpamHash(), equalTo(expected.getSpamHash()));
        assertThat(actual.getCreatedTime(), equalTo(expected.getCreatedTime()));

        final PersistentDeliveryData actualDeliveryData = actual.getDeliveryData();
        assertThat(actualDeliveryData.getTransportType(), equalTo(transport));
        assertThat(actualDeliveryData.getPriorityType(), equalTo(priority));
        assertThat(actualDeliveryData.getDeliveryTime(), equalTo(delivery.getDeliveryTime()));
        assertThat(actualDeliveryData.getSentTime(), equalTo(delivery.getSentTime()));
        assertThat(actualDeliveryData.getTriesCount(), equalTo(delivery.getTriesCount()));

        final PersistentBinaryData actualContent = actual.getContent();
        assertThat(actualContent.getData(), equalTo(actualContent.getData()));
        assertThat(actualContent.getType(), equalTo(actualContent.getType()));

        verify(resultSet, times(3)).getLong(anyString());
        verify(resultSet, times(3)).getTimestamp(anyString());
        verify(resultSet, times(2)).getString(anyString());
        verify(resultSet, times(1)).getClob(anyString());
        verify(resultSet, times(4)).getInt(anyString());
        verifyNoMoreInteractions(resultSet);
    }

    @Test
    public void testColumns() {
        ClassUtils.checkConstructor(Columns.class);
    }


    private Timestamp timestamp(final Instant createdTime) {
        return Timestamp.from(createdTime);
    }
}
