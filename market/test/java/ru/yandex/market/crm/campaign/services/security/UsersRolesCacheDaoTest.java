package ru.yandex.market.crm.campaign.services.security;

import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.crm.domain.Account;
import ru.yandex.market.crm.domain.CompositeUserRole;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UsersRolesCacheDaoTest {
    @Mock
    public JdbcTemplate jdbcTemplate;

    @Mock
    public NamedParameterJdbcTemplate namedJdbcTemplate;

    private UsersRolesCacheDao usersRolesCacheDao;

    @Before
    public void before() {
        usersRolesCacheDao = new UsersRolesCacheDao(jdbcTemplate, namedJdbcTemplate, 10);
    }

    /**
     * Проверяет, что при запросе ролей для одного и того же uid'а возвращается значение из кеша
     */
    @Test
    public void testWithSameUidsReturnValueFromCache() {
        var role1 = new CompositeUserRole(Account.MARKET_ACCOUNT, "abc");
        var role2 = new CompositeUserRole(Account.MARKET_ACCOUNT, "def");

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any()))
                .thenReturn(List.of(role1));
        var userRoles1 = usersRolesCacheDao.getRoles(0);
        assertEquals(Set.of(role1), userRoles1);

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any()))
                .thenReturn(List.of(role2));
        var userRoles2 = usersRolesCacheDao.getRoles(1);
        assertEquals(Set.of(role2), userRoles2);

        var cachedRoles = usersRolesCacheDao.getRoles(0);
        assertEquals(Set.of(role1), cachedRoles);

        verify(jdbcTemplate, times(2))
            .query(anyString(), any(RowMapper.class), any());
    }
}
