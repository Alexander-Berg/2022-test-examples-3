package ru.yandex.market.core.notification.dao.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;

import ru.yandex.common.util.id.HasId;
import ru.yandex.market.core.notification.model.PersistentNotificationType;
import ru.yandex.market.notification.simple.model.type.NotificationPriority;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit тесты для {@link PersistentNotificationTypeRowMapper}.
 *
 * @author avetokhin 10/01/17.
 */
class PersistentNotificationTypeRowMapperTest {
    private static final long ID = 1L;
    private static final int PRIORITY_ID = 3;
    private static final String ROLES_STR = "1,3,1,3";

    @Test
    void testMapRow() throws SQLException {
        ResultSet rs = prepareResultSet();
        PersistentNotificationType nnType = PersistentNotificationTypeRowMapper.INSTANCE.mapRow(rs, 1);

        assertThat(nnType.getId()).isEqualTo(ID);
        assertThat(nnType.getPriorityType()).contains(HasId.getById(NotificationPriority.class, PRIORITY_ID));
        assertThat(nnType.getRoles()).containsExactlyInAnyOrder(1, 3);
    }

    private ResultSet prepareResultSet() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong(PersistentNotificationTypeRowMapper.Columns.COLUMN_ID)).thenReturn(ID);
        when(rs.getInt(PersistentNotificationTypeRowMapper.Columns.COLUMN_PRIORITY_TYPE)).thenReturn(PRIORITY_ID);
        when(rs.getString(PersistentNotificationTypeRowMapper.Columns.COLUMN_ROLES)).thenReturn(ROLES_STR);
        verifyNoMoreInteractions(rs);
        return rs;
    }

}
