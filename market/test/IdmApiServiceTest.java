package ru.yandex.market.jmf.idm.integration.test;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opentest4j.AssertionFailedError;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.crm.dao.RolesDao;
import ru.yandex.market.crm.dao.UsersRolesDao;
import ru.yandex.market.crm.domain.UserRole;
import ru.yandex.market.jmf.idm.integration.IdmApiService;

@Transactional
@SpringJUnitConfig(InternalIdmIntegrationTestConfiguration.class)
public class IdmApiServiceTest {

    public static final String ROLE_FOR_IDM_TEST = "role_for_idm_test";
    public static final String ROLE_FOR_IDM_TEST_TWO = "role_for_idm_test_two";
    public static final String EMPLOYEE_STAFF_LOGIN = "staffLogin";
    private final RolesDao rolesDao;
    private final UsersRolesDao usersRolesDao;
    private final IdmApiService idmApi;
    private final String slug;

    public IdmApiServiceTest(RolesDao rolesDao, UsersRolesDao usersRolesDao, IdmApiService idmApi,
                             @Value("${external.idm.slug}") String slug) {
        this.rolesDao = rolesDao;
        this.usersRolesDao = usersRolesDao;
        this.idmApi = idmApi;
        this.slug = slug;
    }

    @Test
    void testInfo() {
        Map<String, UserRole> roles = Map.of(
                "test_role", new UserRole("test_role", "testRole")
        );
        Mockito.when(rolesDao.getRoles()).thenReturn(roles);

        var idmRoles = idmApi.info();
        Assertions.assertEquals(Maps.transformValues(roles, UserRole::getName), idmRoles.getRoles().getValues());
    }

    @Test
    void testAddRole() {
        idmApi.addRole(EMPLOYEE_STAFF_LOGIN, """
                {
                 "%s": "%s"
                }
                """.formatted(slug, ROLE_FOR_IDM_TEST));

        Mockito.verify(usersRolesDao).addRole(EMPLOYEE_STAFF_LOGIN, ROLE_FOR_IDM_TEST);
    }

    @Test
    void testRemoveRole() {
        idmApi.removeRole(EMPLOYEE_STAFF_LOGIN, """
                {
                 "%s": "%s"
                }
                """.formatted(slug, ROLE_FOR_IDM_TEST));

        Mockito.verify(usersRolesDao).removeRole(EMPLOYEE_STAFF_LOGIN, ROLE_FOR_IDM_TEST);
    }

    @Test
    void testGetUsersRoles() {
        Mockito.when(usersRolesDao.getUsersRoles()).thenReturn(Map.of(
                EMPLOYEE_STAFF_LOGIN, Set.of(ROLE_FOR_IDM_TEST, ROLE_FOR_IDM_TEST_TWO)
        ));

        var idmUsers = idmApi.getUsersRoles().getUsers();

        var idmUser = idmUsers.stream()
                .filter(user -> user.getLogin().equals(EMPLOYEE_STAFF_LOGIN))
                .findAny()
                .orElseThrow(() -> new AssertionFailedError("There is no expected user in getUsersRoles return value"));

        var idmRoles = idmUser.getRoles();
        Assertions.assertTrue(idmRoles.stream().anyMatch(el -> el.containsValue(ROLE_FOR_IDM_TEST)));
        Assertions.assertTrue(idmRoles.stream().anyMatch(el -> el.containsValue(ROLE_FOR_IDM_TEST_TWO)));
    }
}
