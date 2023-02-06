package ru.yandex.market.notification.safe.dao.mapper;

import java.sql.ResultSet;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import ru.yandex.market.notification.safe.dao.mapper.PersistentNotificationDestinationRowMapper.Columns;
import ru.yandex.market.notification.safe.model.PersistentNotificationDestination;
import ru.yandex.market.notification.safe.model.data.PersistentBinaryData;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.notification.safe.dao.mapper.PersistentNotificationDestinationRowMapper.INSTANCE;

/**
 * Unit-тесты для {@link PersistentNotificationDestinationRowMapper}.
 *
 * @author Vladislav Bauer
 */
public class PersistentNotificationDestinationRowMapperTest extends AbstractRowMapperTest {
    @Test
    public void testMapper() throws Exception {
        PersistentBinaryData expectedBinaryData = createBinaryData();
        PersistentNotificationDestination expected =
            new PersistentNotificationDestination(1L, 2L, expectedBinaryData);

        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getLong(AbstractPersistentRowMapper.Columns.COLUMN_ID)).thenReturn(expected.getId());
        when(resultSet.getLong(Columns.COLUMN_GROUP_ID)).thenReturn(expected.getGroupId());
        when(resultSet.getString(Columns.COLUMN_DATA_TYPE)).thenReturn(expected.getData().getType());
        when(resultSet.getClob(Columns.COLUMN_DATA)).thenReturn(createClob(expected.getData()));

        PersistentNotificationDestination actual = INSTANCE.mapRow(resultSet, 0);
        MatcherAssert.assertThat(actual.getId(), equalTo(expected.getId()));
        MatcherAssert.assertThat(actual.getGroupId(), equalTo(expected.getGroupId()));
        checkBinaryData(actual.getData(), expected.getData());

        verify(resultSet, times(2)).getLong(anyString());
        verify(resultSet, times(1)).getClob(anyString());
        verify(resultSet, times(1)).getString(anyString());
        verifyNoMoreInteractions(resultSet);
    }
}
