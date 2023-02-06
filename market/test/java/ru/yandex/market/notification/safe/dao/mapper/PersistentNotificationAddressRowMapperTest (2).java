package ru.yandex.market.notification.safe.dao.mapper;

import java.sql.ResultSet;

import org.junit.jupiter.api.Test;

import ru.yandex.market.notification.safe.dao.mapper.PersistentNotificationAddressRowMapper.Columns;
import ru.yandex.market.notification.safe.model.PersistentNotificationAddress;
import ru.yandex.market.notification.safe.model.data.PersistentBinaryData;
import ru.yandex.market.notification.safe.model.type.NotificationAddressStatus;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.notification.safe.dao.mapper.PersistentNotificationAddressRowMapper.INSTANCE;

/**
 * Unit-тесты для {@link PersistentNotificationAddressRowMapper}.
 *
 * @author Vladislav Bauer
 */
public class PersistentNotificationAddressRowMapperTest extends AbstractRowMapperTest {

    @Test
    public void testMapper() throws Exception {
        PersistentBinaryData expectedBinaryData = createBinaryData();
        PersistentNotificationAddress expected = new PersistentNotificationAddress(
            1L, 2L, 3L, expectedBinaryData, NotificationAddressStatus.NEW
        );

        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getLong(AbstractPersistentRowMapper.Columns.COLUMN_ID)).thenReturn(expected.getId());
        when(resultSet.getLong(Columns.COLUMN_NOTIFICATION_ID)).thenReturn(expected.getNotificationId());
        when(resultSet.getLong(Columns.COLUMN_DESTINATION_ID)).thenReturn(expected.getDestinationId());
        when(resultSet.getInt(Columns.COLUMN_STATUS)).thenReturn(expected.getStatus().getId());
        when(resultSet.getString(Columns.COLUMN_DATA_TYPE)).thenReturn(expected.getData().getType());
        when(resultSet.getClob(Columns.COLUMN_DATA)).thenReturn(createClob(expected.getData()));

        PersistentNotificationAddress actual = INSTANCE.mapRow(resultSet, 0);
        assertThat(actual.getId(), equalTo(expected.getId()));
        assertThat(actual.getNotificationId(), equalTo(expected.getNotificationId()));
        assertThat(actual.getDestinationId(), equalTo(expected.getDestinationId()));
        assertThat(actual.getStatus(), equalTo(expected.getStatus()));
        checkBinaryData(actual.getData(), expected.getData());

        verify(resultSet, times(3)).getLong(anyString());
        verify(resultSet, times(1)).getString(anyString());
        verify(resultSet, times(1)).getClob(anyString());
        verify(resultSet, times(1)).getInt(anyString());
        verifyNoMoreInteractions(resultSet);
    }
}
