package ru.yandex.market.mstat.planner.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.yandex.market.mstat.planner.controller.rest.IdmController;
import ru.yandex.market.mstat.planner.model.Contour;
import ru.yandex.market.mstat.planner.model.ContourGroup;
import ru.yandex.market.mstat.planner.model.UserPlannerRole;
import ru.yandex.market.mstat.planner.service.ContourService;
import ru.yandex.market.mstat.planner.util.idm.IdmUtils;
import ru.yandex.market.mstat.planner.utils.AbstractDbIntegrationTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.mstat.planner.config.ServicesAndDaoConfig.FakeTvmClient.OTHER_TOKEN;
import static ru.yandex.market.mstat.planner.config.ServicesAndDaoConfig.FakeTvmClient.VALID_TOKEN;

public class IdmControllerTest extends AbstractDbIntegrationTest {

    @Autowired
    private IdmController controller;

    @Autowired
    private ContourService contourService;

    private MockMvc mockMvc;

    @Before
    public void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testNoTvmAuth() throws Exception {
        mockMvc.perform(get("/api/v1/idm/info"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testInvalidTicket() throws Exception {
        mockMvc.perform(get("/api/v1/idm/info").header("X-Ya-Service-Ticket", "invalid"))
                .andExpect(status().isForbidden());
    }


    @Test
    public void testInvalidSource() throws Exception {
        mockMvc.perform(get("/api/v1/idm/info").header("X-Ya-Service-Ticket", OTHER_TOKEN))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testToRoleObject() {
        JsonObject expected = new JsonObject();
        expected.addProperty(IdmController.ROOT_ROLE_ID, "a");
        expected.addProperty("a", "b");
        expected.addProperty("b", "c");
        Assert.assertEquals(expected, IdmUtils.toRoleObject("a/b/c", IdmController.ROOT_ROLE_ID));
    }

    @Test
    public void testToRoleString() {
        JsonObject roleObject = new JsonObject();
        roleObject.addProperty(IdmController.ROOT_ROLE_ID, "a");
        roleObject.addProperty("a", "b");
        roleObject.addProperty("b", "c");
        Assert.assertEquals("a/b/c", IdmUtils.toRoleString(roleObject, IdmController.ROOT_ROLE_ID));
    }

    @Test
    public void testError() throws Exception {
        mockMvc.perform(
                post("/api/v1/idm/add-role/")
                        .header("X-Ya-Service-Ticket", VALID_TOKEN)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("login", "user42")) // No role
                .andExpect(status().isOk())
                .andExpect(content().json("{\"code\": 1}"));
    }

    @Test
    public void testInfo() throws Exception {
        data.createTestContour(data.groupId, "contour1");
        data.createTestContour(data.groupId, "contour2");

        JsonObject expected = new JsonObject();
        expected.addProperty("code", 0);

        final JsonObject rootRole = roleWithInfo("market-planner", "Маркет.Планнер", "Маркет.Планнер");

        final JsonObject secondLevelRoles = new JsonObject();
        rootRole.add("values", secondLevelRoles);

        for (UserPlannerRole role : UserPlannerRole.values()) {
            final JsonObject secondLevelRole = roleWithInfo(null, role.shortName, role.description);
            if (role.name().equals("contour_supervisor")) {
                final JsonObject contourSupervisorRole = roleWithInfo(role.name(), role.shortName, role.description);
                final JsonObject contourSupervisorValues = new JsonObject();
                for (Contour contour : contourService.getContours().values()) {
                    contourSupervisorValues.add(String.valueOf(contour.id), roleWithInfo(null, contour.name, contour.description));
                }
                contourSupervisorRole.add("values", contourSupervisorValues);
                secondLevelRole.add("roles", contourSupervisorRole);
            }
            if (role.name().equals("contour_group_supervisor")) {
                final JsonObject contourGroupSupervisorRole = roleWithInfo(role.name(), role.shortName, role.description);
                final JsonObject contourGroupSupervisorValues = new JsonObject();
                for (ContourGroup contourGroup : contourService.getContourGroupsMap().values()) {
                    contourGroupSupervisorValues.add(String.valueOf(contourGroup.getId()), roleWithInfo(null, contourGroup.getName(), null));
                }
                contourGroupSupervisorRole.add("values", contourGroupSupervisorValues);
                secondLevelRole.add("roles", contourGroupSupervisorRole);
            }
            secondLevelRoles.add(role.name(), secondLevelRole);
        }
        expected.add("roles", rootRole);

        System.out.println(expected);

        mockMvc.perform(get("/api/v1/idm/info").header("X-Ya-Service-Ticket", VALID_TOKEN))
                .andDo(result -> System.out.println(result.getResponse().getContentAsString()))
                .andExpect(status().isOk())
                .andExpect(content().json(expected.toString()));
    }

    private JsonObject roleWithInfo(String slug, String name, String help) {
        final JsonObject json = new JsonObject();
        if (slug != null) {
            json.addProperty("slug", slug);
        }
        json.addProperty("name", name);
        if (help != null) {
            json.addProperty("help", help);
        }
        return json;
    }

    @Test
    public void testRolesWorkflow() throws Exception {

        data.createEmployee(data.departmentId, "user1");
        data.createEmployee(data.departmentId, "user2");
        data.createEmployee(data.departmentId, "user3");

        JsonObject role1 = new JsonObject();
        role1.addProperty(IdmController.ROOT_ROLE_ID, "project");
        role1.addProperty("project", "proj1");
        role1.addProperty("proj1", "manager");

        JsonObject role2 = new JsonObject();
        role2.addProperty(IdmController.ROOT_ROLE_ID, "project");
        role2.addProperty("project", "proj2");
        role2.addProperty("proj2", "manager");

        JsonObject role3 = new JsonObject();
        role3.addProperty(IdmController.ROOT_ROLE_ID, "project");
        role3.addProperty("project", "proj2");
        role3.addProperty("proj2", "admin");

        addRole("user1", role1);
        addRole("user1", role1); // Same
        addRole("user1", role2);
        addRole("user2", role1);

        removeRole("user1", role1);
        removeRole("user2", role2); // UnExisting

        addRole("user2", role3);

        addRole("user3", role1);
        removeRole("user3", role1); // No roles for user3

        JsonObject expected = new JsonObject();
        expected.addProperty("code", 0);

        JsonArray usersArray = new JsonArray();
        usersArray.add(userWithRoles("user1", role2));
        usersArray.add(userWithRoles("user2", role1, role3));

        expected.add("users", usersArray);

        mockMvc.perform(
                get("/api/v1/idm/get-all-roles/").header("X-Ya-Service-Ticket", VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(content().json(expected.toString()));
    }

    private JsonObject userWithRoles(String login, JsonObject... roles) {
        JsonObject userObject = new JsonObject();
        userObject.addProperty("login", login);
        JsonArray rolesArray = new JsonArray();
        for (JsonObject role : roles) {
            rolesArray.add(role);
        }
        userObject.add("roles", rolesArray);
        return userObject;
    }

    private void addRole(String login, JsonObject roleJson) throws Exception {
        mockMvc.perform(
                post("/api/v1/idm/add-role/")
                        .header("X-Ya-Service-Ticket", VALID_TOKEN)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("login", login)
                        .param("role", roleJson.toString()))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"code\": 0}"));
    }

    private void removeRole(String login, JsonObject roleJson) throws Exception {
        mockMvc.perform(
                post("/api/v1/idm/remove-role/")
                        .header("X-Ya-Service-Ticket", VALID_TOKEN)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("login", login)
                        .param("role", roleJson.toString()))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"code\": 0}"));
    }
}
