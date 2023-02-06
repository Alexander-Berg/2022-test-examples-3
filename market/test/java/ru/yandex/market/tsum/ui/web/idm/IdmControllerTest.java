package ru.yandex.market.tsum.ui.web.idm;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.commons.fileupload.util.Streams;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.passport.tvmauth.BlackboxEnv;
import ru.yandex.passport.tvmauth.TvmClient;
import ru.yandex.passport.tvmauth.CheckedServiceTicket;
import ru.yandex.passport.tvmauth.TicketStatus;
import ru.yandex.passport.tvmauth.ClientStatus;
import ru.yandex.passport.tvmauth.CheckedUserTicket;
import ru.yandex.passport.tvmauth.roles.Roles;
import ru.yandex.market.tsum.core.TestMongo;
import ru.yandex.market.tsum.core.auth.TsumRole;
import ru.yandex.market.tsum.core.auth.TsumUserDao;
import ru.yandex.market.tsum.release.dao.ReleaseDao;
import ru.yandex.market.tsum.ui.auth.TsumRolesService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 2019-03-20
 */
@Ignore
@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(SpringJUnit4ClassRunner.class)
@PowerMockIgnore("javax.management.*")
@SuppressStaticInitializationFor("ru.yandex.library.ticket_parser2.ServiceTicket")
@WebAppConfiguration
@ContextConfiguration(classes = {
    IdmController.class, TestMongo.class, TsumUserDao.class, ReleaseDao.class,
    IdmControllerTest.FakeTsumRolesService.class, IdmControllerTest.Config.class
})
public class IdmControllerTest {

    private static final String VALID_TOKEN = "abc";
    private static final String OTHER_TOKEN = "xxx";

    @Autowired
    private IdmController controller;

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Before
    public void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testNoTvmAuth() throws Exception {
        mockMvc.perform(get("/idm-tvm/info"))
            .andExpect(status().isForbidden());
    }

    @Test
    public void testInvalidTicket() throws Exception {
        mockMvc.perform(get("/idm-tvm/info").header("X-Ya-Service-Ticket", "invalid"))
            .andExpect(status().isForbidden());
    }


    @Test
    public void testInvalidSource() throws Exception {
        mockMvc.perform(get("/idm-tvm/info").header("X-Ya-Service-Ticket", OTHER_TOKEN))
            .andExpect(status().isForbidden());
    }

    @Test
    public void testToRoleObject() {
        JsonObject expected = new JsonObject();
        expected.addProperty(IdmController.ROOT_ROLE_ID, "a");
        expected.addProperty("a", "b");
        expected.addProperty("b", "c");
        Assert.assertEquals(expected, controller.toRoleObject("a/b/c"));
    }

    @Test
    public void testToRoleString() {
        JsonObject roleObject = new JsonObject();
        roleObject.addProperty(IdmController.ROOT_ROLE_ID, "a");
        roleObject.addProperty("a", "b");
        roleObject.addProperty("b", "c");
        Assert.assertEquals("a/b/c", controller.toRoleString(roleObject));
    }

    @Test
    public void testError() throws Exception {
        mockMvc.perform(
            post("/idm-tvm/add-role/")
                .header("X-Ya-Service-Ticket", VALID_TOKEN)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("login", "user42")) //No role
            .andExpect(status().isOk())
            .andExpect(content().json("{\"code\": 1}"));
    }

    @Test
    public void testInfo() throws Exception {
        String expectedJson = Streams.asString(getClass().getResourceAsStream("info-response.json"));
        mockMvc.perform(get("/idm-tvm/info").header("X-Ya-Service-Ticket", VALID_TOKEN))
            .andExpect(status().isOk())
            .andExpect(content().json(expectedJson));

    }

    @Test
    public void testRolesWorkflow() throws Exception {

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
        addRole("user1", role1); //Same
        addRole("user1", role2);
        addRole("user2", role1);

        removeRole("user1", role1);
        removeRole("user2", role2); //UnExisting

        addRole("user2", role3);

        addRole("user3", role1);
        removeRole("user3", role1); //No roles for user3

        JsonObject expected = new JsonObject();
        expected.addProperty("code", 0);

        JsonArray usersArray = new JsonArray();
        usersArray.add(userWithRoles("user1", role2));
        usersArray.add(userWithRoles("user2", role1, role3));
        usersArray.add(userWithRoles("user3"));

        expected.add("users", usersArray);

        mockMvc.perform(
            get("/idm-tvm/get-all-roles/").header("X-Ya-Service-Ticket", VALID_TOKEN))
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
            post("/idm-tvm/add-role/")
                .header("X-Ya-Service-Ticket", VALID_TOKEN)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("login", login)
                .param("role", roleJson.toString()))
            .andExpect(status().isOk())
            .andExpect(content().json("{\"code\": 0}"));
    }

    private void removeRole(String login, JsonObject roleJson) throws Exception {
        mockMvc.perform(
            post("/idm-tvm/remove-role/")
                .header("X-Ya-Service-Ticket", VALID_TOKEN)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("login", login)
                .param("role", roleJson.toString()))
            .andExpect(status().isOk())
            .andExpect(content().json("{\"code\": 0}"));
    }

    @Component
    public static class FakeTsumRolesService implements TsumRolesService {
        @Override
        public List<TsumRole> getAllRoles() {
            TsumRole tsumAdmin = new TsumRole(ADMIN_ROLE, "Admin", "Владыка ЦУМа.");

            TsumRole ownerRoleValue = new TsumRole(
                "release-owner", "Владелец", "Может управлять релизами и выдавать права", "release-owner"
            );
            TsumRole manageRoleValue = new TsumRole(
                "release-manager", "Релиз менеджер", "Может управлять релизами", "release-manager"
            );


            TsumRole releaseRole = new TsumRole("releases", "Релизы", "Релизный интерфейс в ЦУМе", Arrays.asList(
                new TsumRole("infra", "Market Infra", "Иииииинфра", Arrays.asList(ownerRoleValue, manageRoleValue)),
                new TsumRole("front-desktop", "Десктопный фронтенд", "", Arrays.asList(ownerRoleValue, manageRoleValue))
            ));

            List<TsumRole> roles = new ArrayList<>();
            roles.add(tsumAdmin);
            roles.add(releaseRole);
            return roles;
        }
    }

    @Configuration
    public static class Config {
        @Bean
        public TvmClient tvmClient() {
            return new FakeTvmClient();
        }
    }

    public static class FakeTvmClient implements TvmClient {
        @Override
        public ClientStatus getStatus() {
            return null;
        }

        @Override
        public String getServiceTicketFor(String alias) {
            return null;
        }

        @Override
        public String getServiceTicketFor(int clientId) {
            return null;
        }

        @Override
        public CheckedServiceTicket checkServiceTicket(String ticketBody) {
            CheckedServiceTicket serviceTicket = Mockito.mock(CheckedServiceTicket.class);
            if (ticketBody.equals(VALID_TOKEN)) {
                Mockito.when(serviceTicket.getStatus()).thenReturn(TicketStatus.OK);
                Mockito.when(serviceTicket.getSrc()).thenReturn(IdmController.PRODUCTION_IDM_TVM_ID);
            } else if (ticketBody.equals(OTHER_TOKEN)) {
                Mockito.when(serviceTicket.getStatus()).thenReturn(TicketStatus.OK);
                Mockito.when(serviceTicket.getSrc()).thenReturn(424242);

            } else {
                Mockito.when(serviceTicket.getStatus()).thenReturn(TicketStatus.MALFORMED);
                Mockito.when(serviceTicket.getSrc()).thenReturn(-1);
            }
            return serviceTicket;
        }

        @Override
        public CheckedUserTicket checkUserTicket(String ticketBody) {
            return null;
        }

        @Override
        public CheckedUserTicket checkUserTicket(String ticketBody, BlackboxEnv overridedBbEnv) {
            return null;
        }

        @Override
        public Roles getRoles() {
            return null;
        }

        @Override
        public void close() {

        }
    }
}
