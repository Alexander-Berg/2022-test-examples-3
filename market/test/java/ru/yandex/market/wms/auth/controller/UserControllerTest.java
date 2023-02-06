package ru.yandex.market.wms.auth.controller;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.ldap.query.LdapQuery;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.wms.auth.config.AuthIntegrationTest;
import ru.yandex.market.wms.auth.config.filters.SerialKeyColumnFilter;
import ru.yandex.market.wms.auth.config.filters.UserColumnFilter;
import ru.yandex.market.wms.auth.core.model.LdapUser;
import ru.yandex.market.wms.auth.dao.UserDao;
import ru.yandex.market.wms.auth.model.request.UserBeginnerRemoveRequest;
import ru.yandex.market.wms.auth.model.request.UserCloneRequest;
import ru.yandex.market.wms.auth.model.request.UserPatchRequest;
import ru.yandex.market.wms.auth.model.request.UserRolePutRequest;
import ru.yandex.market.wms.auth.service.GuidService;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

public class UserControllerTest extends AuthIntegrationTest {

    @Autowired
    @MockBean
    private LdapTemplate ldapTemplate;

    @Autowired
    @MockBean
    private GuidService guidService;

    @Autowired
    @SpyBean
    private UserDao userDao;

    @AfterEach
    public void reset() {
        Mockito.reset(ldapTemplate);
        Mockito.reset(guidService);
        Mockito.reset(userDao);
    }

    @Test
    @DatabaseSetup(value = "/db/controller/user/get/users.xml", connection = "scprdd1Connection")
    public void getUser() throws Exception {
        ResultActions result = mockMvc.perform(get("/users/0x02996E9")
                .contentType(MediaType.APPLICATION_JSON));
        result.andExpect(status().isOk())
                .andExpect(content().json(getFileContent("controller/users/response/get-user.json")));
    }

    @Test
    @DatabaseSetup(value = "/db/controller/user/get/users.xml", connection = "scprdd1Connection")
    public void findAll() throws Exception {
        ResultActions result = mockMvc.perform(get("/users").param("filter", "login==AD10")
                .contentType(MediaType.APPLICATION_JSON));
        result.andExpect(status().isOk())
                .andExpect(content().json(getFileContent("controller/users/response/get-users.json")));
    }

    @Test
    @DatabaseSetup(value = "/db/service/user/delete/scprdd1-before.xml", connection = "scprdd1Connection")
    @DatabaseSetup(value = "/db/service/user/delete/enterprise-before.xml", connection = "enterpriseConnection")
    @ExpectedDatabase(
            value = "/db/service/user/delete/scprdd1-after.xml",
            connection = "scprdd1Connection",
            assertionMode = NON_STRICT
    )
    @ExpectedDatabase(
            value = "/db/service/user/delete/enterprise-after.xml",
            connection = "enterpriseConnection",
            assertionMode = NON_STRICT
    )
    public void deleteUser() throws Exception {
        mockMvc.perform(delete("/users/0x02996E9D").contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(value = "/db/dao/user/status/activate/before.xml", connection = "scprdd1Connection")
    @ExpectedDatabase(
            value = "/db/dao/user/status/activate/after.xml",
            connection = "scprdd1Connection",
            assertionMode = NON_STRICT
    )
    public void activateUser() throws Exception {
        mockMvc.perform(post("/users/0x02996E/activate").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(value = "/db/dao/user/status/deactivate/before.xml", connection = "scprdd1Connection")
    @ExpectedDatabase(
            value = "/db/dao/user/status/deactivate/after.xml",
            connection = "scprdd1Connection",
            assertionMode = NON_STRICT
    )
    public void deactivateUser() throws Exception {
        mockMvc.perform(post("/users/0x02996E/deactivate").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @SuppressWarnings("unchecked")
    @Test
    @DatabaseSetup(value = "/db/controller/user/clone/before.xml", connection = "scprdd1Connection")
    @DatabaseSetup(value = "/db/controller/user/clone/before-scprd.xml")
    @DatabaseSetup(value = "/db/dao/mobile-user/insert/before.xml", connection = "enterpriseConnection")
    @ExpectedDatabase(
            value = "/db/controller/user/clone/after.xml",
            connection = "scprdd1Connection",
            assertionMode = NON_STRICT_UNORDERED,
            columnFilters = {UserColumnFilter.class}
    )
    @ExpectedDatabase(
            value = "/db/controller/user/clone/after-scprd.xml",
            assertionMode = NON_STRICT_UNORDERED,
            columnFilters = {SerialKeyColumnFilter.class}
    )
    public void cloneUser() throws Exception {
        String login1 = "AD2";
        String login2 = "AD3";
        Set<String> logins = new LinkedHashSet<>(2);
        logins.add(login1);
        logins.add(login2);
        UserCloneRequest request = UserCloneRequest.builder().logins(logins).build();

        List<LdapUser> expected1 = List.of(
                new LdapUser(login1, String.format("cn=%s,ou=users,ou=vms,dc=mast,dc=local", login1), login1)
        );
        List<LdapUser> expected2 = List.of(
                new LdapUser(login2, String.format("cn=%s,ou=users,ou=vms,dc=mast,dc=local", login2), login2)
        );
        when(ldapTemplate.search(any(), (AttributesMapper<Object>) any())).thenAnswer(
                (InvocationOnMock invocationOnMock) -> {
                    LdapQuery argument = invocationOnMock.getArgument(0);
                    if (argument.filter().equals(new EqualsFilter("sAMAccountName", login1))) {
                        return expected1;
                    } else if (argument.filter().equals(new EqualsFilter("sAMAccountName", login2))) {
                        return expected2;
                    } else {
                        throw new Exception();
                    }
                }
        );
        when(guidService.getGuid()).thenReturn("0000001", "0000002", "0000003", "0000004", "0000005", "0000006",
                "0000007", "0000008", "0000009", "0000010", "0000011", "0000012", "0000013", "0000014", "0000015",
                "0000016", "0000017", "0000018");
        doNothing().when(userDao).putUserPrefInstances(any());
        String content = new ObjectMapper().writeValueAsString(request);
        mockMvc.perform(post("/users/0x361E53EAF2B242975FF70E7E9477BC7D/clone").content(content)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        verify(guidService, times(18)).getGuid();
    }

    @Test
    @DatabaseSetup(value = "/db/dao/user/get/role/roles.xml", connection = "scprdd1Connection")
    public void getRoles() throws Exception {
        mockMvc.perform(get("/users/roles")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent("controller/users/response/get-roles.json")));
    }

    @Test
    @DatabaseSetup(value = "/db/dao/user/put/role/before.xml", connection = "scprdd1Connection")
    @ExpectedDatabase(
            value = "/db/dao/user/put/role/after.xml",
            connection = "scprdd1Connection",
            assertionMode = NON_STRICT
    )
    public void setRoles() throws Exception {
        when(guidService.getGuid()).thenReturn("0000001", "0000002", "0000003", "0000004");
        UserRolePutRequest request = UserRolePutRequest.builder()
                .logins(List.of("AD1", "AD15"))
                .roleNames(List.of("SCE-Administrator", "SCE-RF User"))
                .build();
        String content = new ObjectMapper().writeValueAsString(request);
        mockMvc.perform(put("/users/roles").content(content)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        verify(guidService, times(4)).getGuid();
    }

    @Test
    @DatabaseSetup(value = "/db/dao/user/patch/user-data/before.xml", connection = "scprdd1Connection")
    @ExpectedDatabase(
            value = "/db/dao/user/patch/user-data/after-email.xml",
            connection = "scprdd1Connection",
            assertionMode = NON_STRICT
    )
    public void patchUser() throws Exception {
        UserPatchRequest request = UserPatchRequest.builder().emailAddress("hello@yandex.ru").build();
        String content = new ObjectMapper().writeValueAsString(request);
        mockMvc.perform(patch("/users/0x02996E").content(content)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(value = "/db/dao/user/patch/user-data/before.xml", connection = "scprdd1Connection")
    @ExpectedDatabase(
            value = "/db/dao/user/patch/user-data/after-fullname.xml",
            connection = "scprdd1Connection",
            assertionMode = NON_STRICT
    )
    public void patchFullName() throws Exception {
        UserPatchRequest request = UserPatchRequest.builder().fullName("Lionel Messi").build();
        String content = new ObjectMapper().writeValueAsString(request);
        mockMvc.perform(patch("/users/0x02996E").content(content)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(value = "/db/dao/user/patch/user-data/before.xml", connection = "scprdd1Connection")
    @ExpectedDatabase(
            value = "/db/dao/user/patch/user-data/after-locale.xml",
            connection = "scprdd1Connection",
            assertionMode = NON_STRICT
    )
    public void patchLocale() throws Exception {
        UserPatchRequest request = UserPatchRequest.builder().locale("aus").build();
        String content = new ObjectMapper().writeValueAsString(request);
        mockMvc.perform(patch("/users/0x02996E").content(content)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(value = "/db/controller/user/beginner-end-time/before.xml")
    @ExpectedDatabase(
            value = "/db/controller/user/beginner-end-time/after.xml",
            assertionMode = NON_STRICT
    )
    public void setUserBeginnerEndTime() throws Exception {
        String content = "{" +
                "\"users\" : [ " +
                "   {" +
                "       \"login\" : \"user1\", " +
                "       \"beginnerEndTime\" : \"2022-03-14T14:34:56.000+02:00\" " +
                "   }," +
                "   {" +
                "       \"login\" : \"user2\", " +
                "       \"beginnerEndTime\" : \"2022-03-14T14:34:56.000+02:00\" " +
                "   }" +
                "]" +
                "}";

        mockMvc.perform(post("/users/beginner").content(content)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(value = "/db/controller/user/beginner-end-time/before.xml")
    public void getAllBeginners() throws Exception {

        final String response = "[\"user1\",\"user2\"]";

        mockMvc.perform(get("/users/beginner")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(response));
    }

    @Test
    @DatabaseSetup(value = "/db/controller/user/beginner-end-time/before-remove.xml")
    @ExpectedDatabase(
            value = "/db/controller/user/beginner-end-time/after-remove.xml",
            assertionMode = NON_STRICT
    )
    public void removeUserBeginner() throws Exception {

        var request = UserBeginnerRemoveRequest.builder()
                .period(LocalDateTime.parse("2022-03-14T15:34:56"))
                .build();

        String content = new ObjectMapper().registerModule(new JavaTimeModule()).writeValueAsString(request);

        mockMvc.perform(delete("/users/beginner").content(content)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup(value = "/db/controller/user/staff/before.xml")
    public void getStaffLogins() throws Exception {
        mockMvc.perform(get("/users/employee-staff?logins=user1,user2")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent("controller/users/response/get-staff.json")));
    }

    @Test
    @DatabaseSetup(value = "/db/controller/user/staff/before-empty.xml")
    public void getStaffLoginsNotFound() throws Exception {
        mockMvc.perform(get("/users/employee-staff?logins=user1,user2")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    @DatabaseSetup(value = "/db/controller/user/staff/before-empty.xml")
    public void getStaffLoginsEmptyList() throws Exception {
        mockMvc.perform(get("/users/employee-staff?logins=")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }
}
