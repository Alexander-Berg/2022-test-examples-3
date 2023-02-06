package ru.yandex.market.stats.test.auth.idm;

import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import ru.yandex.market.stat.auth.idm.IdmController;
import ru.yandex.market.stat.auth.idm.IdmRolesDB;
import ru.yandex.market.stat.auth.idm.IdmRolesHolder;
import ru.yandex.market.stat.auth.idm.IdmUser;
import ru.yandex.market.stat.auth.idm.Role;
import ru.yandex.market.stat.auth.idm.RoleShortcut;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class IdmControllerTest {

    private final IdmRolesDB service = new TestRolesDB();
    private final TestRolesHolder rolesHolder = new TestRolesHolder();
    private final IdmController controller = new IdmController(service, rolesHolder);
    private MockHttpServletRequest request = new MockHttpServletRequest(null, "my.request.url");

    @Test
    public void testInfoResponse() {
        String expectedJson = getResource("/info-response.json").trim();
        String info = controller.info();
        System.out.println(info);
        Assert.assertThat("info response failed! " + info, info, Matchers.startsWith("{\"code\":0,"));
        Assert.assertThat("Wrong info format", info,
            Matchers.is(expectedJson));
    }

    @Test
    public void testAddRole() throws IOException {
        request.addParameter("login", "vasya");
        request.addParameter("role", "{\"group\": \"rollbacker\"}");
        request.addParameter("path", "/group/rollbacker/");
        String result = controller.addRole(request);
        System.out.println(result);
        Assert.assertThat("Add role failed " + result, result, Matchers.is("{\"code\":0}"));
    }

    @Test
    public void testAddNonExistentRole() throws IOException {
        request.addParameter("login", "vasya");
        request.addParameter("role", "{\"group\": \"pg_remover\"}");
        request.addParameter("path", "/group/pg_remover");
        String result = controller.addRole(request);
        System.out.println(result);
        Assert.assertThat("Add role failed incorrectly" + result, result, Matchers.is("{\"code\":4,\"fatal\":\"Unknown role: /group/pg_remover\"}"));
    }

    @Test
    public void testAddEmptyRole() throws IOException {
        request.addParameter("login", "vasya");
        request.addParameter("path", "/group");
        String result = controller.addRole(request);
        System.out.println(result);
        Assert.assertThat("Add role failed incorrectly: " + result, result, Matchers.is("{\"code\":4,\"fatal\":\"Unknown role: /group\"}"));
    }

    @Test
    public void testAddRoleEmptyLogin() throws IOException {
        request.addParameter("group", "182");
        String result = controller.addRole(request);
        System.out.println(result);
        Assert.assertThat("Add role failed incorrectly: " + result, result, Matchers.is("{\"code\":4,\"fatal\":\"Group roles are not supported, please provide the login\"}"));
    }

    @Test
    public void testRemoveRole() throws IOException {
        request.addParameter("login", "vasya");
        request.addParameter("role", "{\"group\": \"rollbacker\"}");
        request.addParameter("path", "/group/rollbacker");
        String result = controller.removeRole(request);
        System.out.println(result);
        Assert.assertThat("Removing role failed " + result, result, Matchers.is("{\"code\":0}"));
    }

    @Test
    public void testRemoveNonExistentRole() throws IOException {
        request.addParameter("login", "vasya");
        request.addParameter("role", "{\"group\": \"pg_remover\"}");
        request.addParameter("path", "/group/pg_remover");
        String result = controller.removeRole(request);
        System.out.println(result);
        Assert.assertThat("Removing role failed " + result, result,
            Matchers.is("{\"code\":1,\"warning\":\"Unknown role: /group/pg_remover\"}"));
    }

    @Test
    public void testRemoveRoleEmptyLogin() {
        request.addParameter("group", "182");
        String result = controller.removeRole(request);
        System.out.println(result);
        Assert.assertThat("Removing role failed incorrectly: " + result, result,
            Matchers.is("{\"code\":4,\"fatal\":\"Group roles are not supported, please provide the login\"}"));
    }


    @Test
    public void testGetAllRoles() {
        String expectedJson = getResource("/get-roles-response.json");
        String result = controller.getAllRoles();
        System.out.println(result);
        Assert.assertThat("Wrong user roles response: " + result, result,
            Matchers.is(expectedJson));
    }

    @Test
    public void testErrorResponse() {
        String info = controller.handleError(request, new RuntimeException("AGGGGR!"));
        System.out.println(info);
        Assert.assertThat("Wrong error response! " + info, info,
            Matchers.is("{\"code\":2,\"error\":\"java.lang.RuntimeException: AGGGGR!\"}"));
    }


    private class TestRolesHolder extends IdmRolesHolder {
        TestRolesHolder() {
            super("group", "Тест", "TEST");
        }

        @Override
        protected Map<String, Role> describeRoles() {
            List<Role> roles = new LinkedList<>(Arrays.asList(
                new Role("viewer", "Наблюдатель", "Viewer",
                    "Может дергать ручки, возвращающие статусы", "Has permissions to call info handles"),

                new Role("rollbacker", "Уполномоченный откатывать клики в ПГ", "PG force rollback",
                    "Может дергать ручки, возвращающие статусы и откатывать клики в ПГ антифроде",
                    "Has permissions to call info handles and to rollback clicks with pg antifraud"),

                new Role("admin", "Администратор", "Administrator",
                    "Может всё", "Has permissions for all handles and more", false)
            ));

            //В тесте нужно зафиксировать порядок для легкости сравнения
            Map<String, Role> map = new LinkedHashMap<>();
            for (Role r : roles) {
                map.put(r.getId(), r);
            }

            return map;
        }
    }

    private class TestRolesDB implements IdmRolesDB {

        private RoleShortcut admin = new RoleShortcut("group", "admin");
        private RoleShortcut viewer = new RoleShortcut("group", "viewer");
        private RoleShortcut rollbacker = new RoleShortcut("group", "rollbacker");
        private IdmUser vasya = new IdmUser("vasya", new LinkedHashSet<>(Arrays.asList(viewer, rollbacker)));
        private IdmUser kormushin = new IdmUser("kormushin", new HashSet<>(Collections.singletonList(admin)));

        @Override
        public List<IdmUser> getAllUsers() {
            return Arrays.asList(
                vasya,
                kormushin);
        }

        @Override
        public void addRole(String user, RoleShortcut role) {
            System.out.println("Adding role " + role);
        }

        @Override
        public void removeRole(String user, RoleShortcut role) {
            System.out.println("Removing role " + role);
        }

        @Override
        public Set<RoleShortcut> getUserRoles(String user) {
            return getAllUsers().stream()
                .filter(u -> u.getLogin().equals(user))
                .map(us -> us.getRoles()).findFirst().orElse(new HashSet<>());
        }

        @Override
        public boolean userHasRole(String user, RoleShortcut role) {
            return getUserRoles(user).contains(role);
        }

    }

    @SneakyThrows
    private String getResource(String name) {
        return IOUtils.toString(getClass().getResourceAsStream(name), "UTF-8");
    }
}
