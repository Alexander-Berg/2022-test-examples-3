package ru.yandex.market.passport.internal.idm.spring;

import javax.servlet.http.HttpServletRequest;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import ru.yandex.market.passport.internal.idm.api.IdmDao;
import ru.yandex.market.passport.internal.idm.api.common.LocalizedString;
import ru.yandex.market.passport.internal.idm.api.common.StatusResult;
import ru.yandex.market.passport.internal.idm.api.info.RolesInfo;
import ru.yandex.market.passport.internal.idm.api.info.RolesInfoResult;
import ru.yandex.market.passport.internal.idm.api.info.SimpleRoleInfo;
import ru.yandex.market.passport.internal.idm.api.sync.AllRoles;
import ru.yandex.market.passport.internal.idm.api.sync.PagedRole;
import ru.yandex.market.passport.internal.idm.api.sync.PagedRoles;
import ru.yandex.market.passport.internal.idm.api.sync.Role;
import ru.yandex.market.passport.internal.idm.api.sync.UserRoles;
import ru.yandex.market.passport.internal.idm.api.update.AddRoleOperation;
import ru.yandex.market.passport.internal.idm.api.update.RemoveRoleOperation;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

/**
 * @author anmalysh
 * @since 10/28/2018
 */
public class IdmControllerTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private HttpServletRequest request;

    @Mock
    private IdmDao idmDao;

    private IdmController controller;

    @Before
    public void setUp() {
        controller = new IdmController(idmDao);
    }

    @Test
    public void testAddUserRole() {
        AddRoleOperation operation = new AddRoleOperation();
        operation.setLogin("user1");
        operation.setPath("/group/clothes/role/admin");
        operation.setFields(ImmutableMap.of("key", "value"));
        operation.setRole(ImmutableMap.of("group", "clothes", "role", "admin"));

        StatusResult result = new StatusResult();
        result.setCode(0);

        when(idmDao.addRole(operation)).thenReturn(result);

        StatusResult actualResult = controller.addRole(
            "user1",
            null,
            "/group/clothes/role/admin",
            "{\"group\":\"clothes\",\"role\":\"admin\"}",
            "{\"key\":\"value\"}");

        assertEquals(result, actualResult);
    }

    @Test
    public void testAddGroupRole() {
        AddRoleOperation operation = new AddRoleOperation();
        operation.setGroup(2L);
        operation.setPath("/group/clothes/role/admin");
        operation.setFields(ImmutableMap.of("key", "value"));
        operation.setRole(ImmutableMap.of("group", "clothes", "role", "admin"));

        StatusResult result = new StatusResult();
        result.setCode(0);

        when(idmDao.addRole(operation)).thenReturn(result);

        StatusResult actualResult = controller.addRole(
            null,
            2L,
            "/group/clothes/role/admin",
            "{\"group\":\"clothes\",\"role\":\"admin\"}",
            "{\"key\":\"value\"}");

        assertEquals(result, actualResult);
    }

    @Test
    public void testGroupAndRoleNull() {
        StatusResult result = new StatusResult();
        result.setCode(1);
        result.setError("Error adding role: Either login or group should be filled");

        StatusResult actualResult = controller.addRole(
            null,
            null,
            "/group/clothes/role/admin",
            "{\"group\":\"clothes\",\"role\":\"admin\"}",
            "{\"key\":\"value\"}");

        assertEquals(result, actualResult);
    }


    @Test
    public void testGroupAndRoleFilled() {
        StatusResult result = new StatusResult();
        result.setCode(1);
        result.setError("Error adding role: Both login and group shouldn't be filled");

        StatusResult actualResult = controller.addRole(
            "user1",
            1L,
            "/group/clothes/role/admin",
            "{\"group\":\"clothes\",\"role\":\"admin\"}",
            "{\"key\":\"value\"}");

        assertEquals(result, actualResult);
    }

    @Test
    public void testAddRoleFailed() {
        AddRoleOperation operation = new AddRoleOperation();
        operation.setLogin("user1");
        operation.setPath("/group/clothes/role/admin");
        operation.setRole(ImmutableMap.of("group", "clothes", "role", "admin"));

        StatusResult result = new StatusResult();
        result.setCode(1);
        result.setError("Error adding role: Test exception");

        when(idmDao.addRole(operation))
            .thenThrow(new RuntimeException("Test exception"));

        StatusResult actualResult = controller.addRole(
            "user1",
            null,
            "/group/clothes/role/admin",
            "{\"group\":\"clothes\",\"role\":\"admin\"}",
            null);

        assertEquals(result, actualResult);
    }

    @Test
    public void testRemoveRole() {
        RemoveRoleOperation operation = new RemoveRoleOperation();
        operation.setLogin("user1");
        operation.setPath("/group/clothes/role/admin");
        operation.setFields(ImmutableMap.of("key", "value"));
        operation.setRole(ImmutableMap.of("group", "clothes", "role", "admin"));
        operation.setFired(true);
        operation.setDeleted(false);

        StatusResult result = new StatusResult();
        result.setCode(0);

        when(idmDao.removeRole(operation)).thenReturn(result);

        StatusResult actualResult = controller.removeRole(
            "user1",
            null,
            "/group/clothes/role/admin",
            "{\"group\":\"clothes\",\"role\":\"admin\"}",
            "{\"key\":\"value\"}",
            1,
            0);

        assertEquals(result, actualResult);
    }

    @Test
    public void testRemoveRoleFailed() {
        RemoveRoleOperation operation = new RemoveRoleOperation();
        operation.setLogin("user1");
        operation.setPath("/group/clothes/role/admin");
        operation.setRole(ImmutableMap.of("group", "clothes", "role", "admin"));

        StatusResult result = new StatusResult();
        result.setCode(1);
        result.setError("Error removing role: Test exception");

        when(idmDao.removeRole(operation))
            .thenThrow(new RuntimeException("Test exception"));

        StatusResult actualResult = controller.removeRole(
            "user1",
            null,
            "/group/clothes/role/admin",
            "{\"group\":\"clothes\",\"role\":\"admin\"}",
            null,
            null,
            null);

        assertEquals(result, actualResult);
    }

    @Test
    public void testGetInfo() {
        RolesInfoResult result = new RolesInfoResult();
        result.setCode(0);

        RolesInfo info = new RolesInfo();
        info.setName(new LocalizedString("name"));
        info.setSlug("slug");
        info.setValues(ImmutableMap.of("role", new SimpleRoleInfo("Роль")));
        result.setRoles(info);

        when(idmDao.getRolesInfo()).thenReturn(result);

        RolesInfoResult actualResult = controller.getInfo();

        assertEquals(result, actualResult);
    }

    @Test
    public void testGetInfoFails() {
        RolesInfoResult result = new RolesInfoResult();
        result.setCode(1);
        result.setError("Error getting roles info: Test exception");

        when(idmDao.getRolesInfo())
            .thenThrow(new RuntimeException("Test exception"));

        RolesInfoResult actualResult = controller.getInfo();

        assertEquals(result, actualResult);
    }

    @Test
    public void testGetAllRoles() {
        AllRoles result = new AllRoles();
        result.setCode(0);

        UserRoles roles = new UserRoles();
        roles.setLogin("user");

        Role role = new Role();
        role.addRole("role", "Роль");

        roles.setRoles(ImmutableList.of(role));
        result.setUsers(ImmutableList.of(roles));

        when(idmDao.getAllRoles()).thenReturn(result);

        AllRoles actualResult = controller.getAllRoles();

        assertEquals(result, actualResult);
    }

    @Test
    public void testGetAllRolesFails() {
        AllRoles result = new AllRoles();
        result.setCode(1);
        result.setError("Error getting all users roles: Test exception");

        when(idmDao.getAllRoles())
            .thenThrow(new RuntimeException("Test exception"));

        AllRoles actualResult = controller.getAllRoles();

        assertEquals(result, actualResult);
    }

    @Test
    public void testGetPagedRoles() {
        PagedRoles result = new PagedRoles();
        result.setCode(0);
        result.setNextUrl("nextUrl");

        PagedRole pagedRole = new PagedRole();
        pagedRole.setGroup(1);
        pagedRole.setPath("/role/operator");

        result.setUsers(ImmutableList.of(pagedRole));

        when(idmDao.getPagedRoles("cursorMark")).thenReturn(result);
        when(request.getContextPath()).thenReturn("/api/idm/get-roles");

        PagedRoles actualResult = controller.getPagedRoles("cursorMark", request);

        result.setNextUrl("/api/idm/get-roles?cursor_mark=cursorMark");
        assertEquals(result, actualResult);
    }


    @Test
    public void testGetPagedRolesFails() {
        PagedRoles result = new PagedRoles();
        result.setCode(1);
        result.setError("Error getting paged users roles: Test exception");

        when(idmDao.getPagedRoles("cursorMark"))
            .thenThrow(new RuntimeException("Test exception"));

        PagedRoles actualResult = controller.getPagedRoles("cursorMark", request);

        assertEquals(result, actualResult);
    }
}
