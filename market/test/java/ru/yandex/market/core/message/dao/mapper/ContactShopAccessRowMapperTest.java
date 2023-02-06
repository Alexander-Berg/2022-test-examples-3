package ru.yandex.market.core.message.dao.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;

import org.junit.Test;

import ru.yandex.market.core.message.model.ContactShopMessageRoles;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit тесты для {@link ContactShopAccessRowMapper}.
 *
 * @author avetokhin 11/01/17.
 */
public class ContactShopAccessRowMapperTest {
    private static final long SHOP_ID = 1L;
    private static final String ROLES_STR = "1,3";

    @Test
    public void testMapRow() throws SQLException {
        final ResultSet rs = prepareResultSet();
        final ContactShopMessageRoles contactRoles = new ContactShopAccessRowMapper().mapRow(rs, 1);
        assertThat(contactRoles.getShopId(), equalTo(SHOP_ID));
        assertThat(contactRoles.getRoles(), equalTo(new HashSet<>(Arrays.asList(1, 3))));
    }

    private ResultSet prepareResultSet() throws SQLException {
        final ResultSet rs = mock(ResultSet.class);

        when(rs.getLong(ContactShopAccessRowMapper.COLUMN_SHOP_ID)).thenReturn(SHOP_ID);
        when(rs.getString(ContactShopAccessRowMapper.COLUMN_ROLES)).thenReturn(ROLES_STR);

        return rs;
    }

}
