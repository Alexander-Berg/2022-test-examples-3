package ru.yandex.market.notification.safe.dao.mapper;

import java.sql.ResultSet;

import org.junit.Test;

import ru.yandex.market.notification.safe.dao.mapper.PersistentNotificationDestinationRowMapper.Columns;
import ru.yandex.market.notification.safe.model.PersistentNotificationDestination;
import ru.yandex.market.notification.safe.model.data.PersistentBinaryData;
import ru.yandex.market.notification.test.util.ClassUtils;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
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
        final PersistentBinaryData expectedBinaryData = createBinaryData();
        final PersistentNotificationDestination expected =
            new PersistentNotificationDestination(1L, 2L, expectedBinaryData);

        final ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getLong(AbstractPersistentRowMapper.Columns.COLUMN_ID)).thenReturn(expected.getId());
        when(resultSet.getLong(Columns.COLUMN_GROUP_ID)).thenReturn(expected.getGroupId());
        when(resultSet.getString(Columns.COLUMN_DATA_TYPE)).thenReturn(expected.getData().getType());
        when(resultSet.getClob(Columns.COLUMN_DATA)).thenReturn(createClob(expected.getData()));

        final PersistentNotificationDestination actual = INSTANCE.mapRow(resultSet, 0);
        assertThat(actual.getId(), equalTo(expected.getId()));
        assertThat(actual.getGroupId(), equalTo(expected.getGroupId()));
        checkBinaryData(actual.getData(), expected.getData());

        verify(resultSet, times(2)).getLong(anyString());
        verify(resultSet, times(1)).getClob(anyString());
        verify(resultSet, times(1)).getString(anyString());
        verifyNoMoreInteractions(resultSet);
    }

    @Test
    public void testColumns() {
        ClassUtils.checkConstructor(Columns.class);
    }

}
