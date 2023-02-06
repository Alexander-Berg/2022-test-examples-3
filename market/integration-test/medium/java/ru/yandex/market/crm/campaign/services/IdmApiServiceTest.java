package ru.yandex.market.crm.campaign.services;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.market.crm.campaign.services.security.Roles;
import ru.yandex.market.crm.campaign.test.AbstractServiceMediumTest;
import ru.yandex.market.crm.core.test.utils.BlackboxHelper;
import ru.yandex.market.crm.domain.Account;
import ru.yandex.market.crm.json.serialization.JsonSerializer;
import ru.yandex.market.crm.services.IdmApiService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class IdmApiServiceTest extends AbstractServiceMediumTest {
    private static final String LOGIN = "user1";
    private static final Long PUID = 1L;

    @Inject
    private BlackboxHelper blackboxHelper;
    @Inject
    private IdmApiService idmApiService;
    @Inject
    private JsonSerializer jsonSerializer;

    @Value("${external.idm.account.slug}")
    private String accountSlug;
    @Value("${external.idm.role.slug}")
    private String roleSlug;

    @BeforeEach
    public void setUp() {
        blackboxHelper.setUpResolveUserInfoByLogin(PUID, LOGIN);
        blackboxHelper.setUpResolveYandexTeamInfoByUid(PUID, LOGIN);
    }

    /**
     * После добавления ролей пользователям сервис корректно возвращает эти роли
     */
    @Test
    public void testAddUserRoles() {
        idmApiService.addRole(LOGIN, createRoleAsString(Account.MARKET_ACCOUNT, Roles.ADMIN));

        idmApiService.addRole(LOGIN, createRoleAsString(Account.MARKET_ACCOUNT, Roles.OPERATOR));

        var usersRoles = idmApiService.getUsersRoles();
        var users = usersRoles.getUsers();
        assertNotNull(users);
        assertEquals(1, users.size());

        var user = users.get(0);
        assertEquals(LOGIN, user.getLogin());

        var userRoles = user.getRoles();
        assertNotNull(userRoles);
        assertEquals(2, userRoles.size());

        var rolesInMarket = new HashSet<String>();
        userRoles.forEach(role -> {
            assertEquals(2, role.size());
            assertEquals(Account.MARKET_ACCOUNT, role.get(accountSlug));
            rolesInMarket.add(role.get(roleSlug));
        });
        assertEquals(Set.of(Roles.ADMIN, Roles.OPERATOR), rolesInMarket);
    }

    /**
     * После удаления роли у пользователя сервис перестаёт возвращать данную роль в списке ролей пользователя
     */
    @Test
    public void testRemoveUserRoles() {
        idmApiService.addRole(LOGIN, createRoleAsString(Account.MARKET_ACCOUNT, Roles.ADMIN));

        idmApiService.addRole(LOGIN, createRoleAsString(Account.MARKET_ACCOUNT, Roles.OPERATOR));

        idmApiService.removeRole(LOGIN, createRoleAsString(Account.MARKET_ACCOUNT, Roles.OPERATOR));

        var usersRoles = idmApiService.getUsersRoles();
        var users = usersRoles.getUsers();
        assertNotNull(users);
        assertEquals(1, users.size());

        var user = users.get(0);
        assertEquals(LOGIN, user.getLogin());

        var userRoles = user.getRoles();
        assertNotNull(userRoles);
        assertEquals(1, userRoles.size());

        var userRole = userRoles.get(0);
        assertEquals(2, userRole.size());
        assertEquals(Account.MARKET_ACCOUNT, userRole.get(accountSlug));
        assertEquals(Roles.ADMIN, userRole.get(roleSlug));
    }

    private String createRoleAsString(String account, String role) {
        return jsonSerializer.writeObjectAsString(Map.of(
                accountSlug, account,
                roleSlug, role
        ));
    }
}
