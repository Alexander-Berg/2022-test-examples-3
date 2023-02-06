package ru.yandex.direct.intapi.entity.idm;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.entity.client.model.ClientPrimaryManager;
import ru.yandex.direct.core.entity.client.repository.ClientRepository;
import ru.yandex.direct.core.entity.client.service.ClientPrimaryManagerService;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.utils.JsonUtils;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.direct.intapi.entity.idm.converter.IdmCommonNames.SLUG;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;


@IntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class IdmRolesControllerMainManagerTest {

    private static final String ADD_ROLE_URL = "/idm/add-role";
    private static final String REMOVE_ROLE_URL = "/idm/remove-role";
    private static final String GET_ALL_ROLES_URL = "/idm/get-all-roles";
    private static final String MAIN_MANAGER_ROLE = "main_manager_for_client";
    private static final String LOGIN_JSON_FIELD = "login";
    private static final String PASSPORT_LOGIN_JSON_FIELD = "passport-login";
    private static final String CLIENT_ID_JSON_FIELD = "client_id";
    private static final String CODE_JSON_FIELD = "code";
    private static final String ERROR_JSON_FIELD = "error";

    @Autowired
    private Steps steps;
    @Autowired
    private IdmRolesController controller;
    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private ClientPrimaryManagerService clientPrimaryManagerService;

    private MockMvc mockMvc;
    private String passportLogin;
    private String domainLogin;
    private long clientId;
    private Integer clientShard;
    private Long startManagerUid;

    @Before
    public void setUp() {
        UserInfo managerInfo = steps.clientSteps().createDefaultClientWithRoleInAnotherShard(RbacRole.MANAGER)
                .getChiefUserInfo();
        passportLogin = managerInfo.getUser().getLogin();
        domainLogin = managerInfo.getUser().getDomainLogin();
        ClientInfo clientInfo = steps.clientSteps().createDefaultClientWithRole(RbacRole.CLIENT);
        clientShard = clientInfo.getShard();
        clientId = clientInfo.getClientId().asLong();
        ModelChanges<Client> modelChanges = new ModelChanges<>(clientId, Client.class);
        startManagerUid = managerInfo.getUid();
        modelChanges.process(startManagerUid, Client.PRIMARY_MANAGER_UID);
        modelChanges.process(true, Client.IS_IDM_PRIMARY_MANAGER);
        Client client = clientRepository.get(clientShard, List.of(clientInfo.getClientId())).get(0);
        AppliedChanges<Client> appliedChanges = modelChanges.applyTo(client);
        clientRepository.update(clientShard, List.of(appliedChanges));

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .defaultResponseCharacterEncoding(StandardCharsets.UTF_8)
                .build();
    }

    @Test
    public void getAllMainManagerRoles() throws Exception {
        //Дергаем ручку
        MockHttpServletRequestBuilder requestBuilder = get(GET_ALL_ROLES_URL)
                .accept(MediaType.APPLICATION_JSON);
        ResultActions perform = mockMvc.perform(requestBuilder);
        perform.andExpect(status().isOk());

        //Проверяем результат
        String answer = perform.andReturn().getResponse().getContentAsString();
        JsonNode jsonNode = JsonUtils.fromJson(answer);
        int code = jsonNode.get(CODE_JSON_FIELD).asInt();
        JsonNode users = jsonNode.get("users");
        List<String> logins = users.findValuesAsText(LOGIN_JSON_FIELD);
        List<String> passportLogins = users.findValuesAsText(PASSPORT_LOGIN_JSON_FIELD);
        List<String> directRoles = users.findValuesAsText("direct");
        List<String> clientIds = users.findValuesAsText(CLIENT_ID_JSON_FIELD);
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(code).as("result code").isEqualTo(0);
            soft.assertThat(passportLogins).as("passport logins").contains(passportLogin);
            soft.assertThat(logins).as("domain logins").contains(domainLogin);
            soft.assertThat(directRoles).as("role names").contains(MAIN_MANAGER_ROLE);
            soft.assertThat(clientIds).as("clientIds").contains(Long.toString(clientId));
        });
    }

    @Test
    public void addRole_success() throws Exception {
        UserInfo newManagerInfo = steps.clientSteps().createDefaultClientWithRoleInAnotherShard(RbacRole.MANAGER)
                .getChiefUserInfo();
        steps.userSteps().mockUserInExternalClients(newManagerInfo.getUser());
        String newPassportLogin = newManagerInfo.getUser().getLogin();
        String newDomainLogin = newManagerInfo.getUser().getDomainLogin();

        //Дергаем ручку
        String jsonRole = JsonUtils.toJson(singletonMap(SLUG, MAIN_MANAGER_ROLE));
        String jsonFields = JsonUtils.toJson(Map.of(
                PASSPORT_LOGIN_JSON_FIELD, newPassportLogin,
                CLIENT_ID_JSON_FIELD, clientId));
        MockHttpServletRequestBuilder requestBuilder = post(ADD_ROLE_URL)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("login", newDomainLogin)
                .param("role", jsonRole)
                .param("fields", jsonFields);
        ResultActions perform = mockMvc.perform(requestBuilder);
        perform.andExpect(status().isOk());

        //Проверяем результат
        String answer = perform.andReturn().getResponse().getContentAsString();
        JsonNode jsonNode = JsonUtils.fromJson(answer);
        int code = jsonNode.get(CODE_JSON_FIELD).asInt();
        String passportLoginAnswer = jsonNode.findValuesAsText(PASSPORT_LOGIN_JSON_FIELD).get(0);
        String clientIdAnswer = jsonNode.findValuesAsText(CLIENT_ID_JSON_FIELD).get(0);
        Client actualClient = clientRepository.get(clientShard, singletonList(ClientId.fromLong(clientId))).get(0);
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(code).as("result code").isEqualTo(0);
            soft.assertThat(passportLoginAnswer).as("passport login").isEqualTo(newPassportLogin);
            soft.assertThat(clientIdAnswer).as("ClientId").isEqualTo(Long.toString(clientId));
            soft.assertThat(actualClient.getPrimaryManagerUid())
                    .as("manager uid").isEqualTo(newManagerInfo.getUid());
        });
    }

    @Test
    public void addRole_whenWrongRole_failure() throws Exception {
        UserInfo newSupportInfo = steps.clientSteps().createDefaultClientWithRoleInAnotherShard(RbacRole.SUPPORT)
                .getChiefUserInfo();
        steps.userSteps().mockUserInExternalClients(newSupportInfo.getUser());
        String newPassportLogin = newSupportInfo.getUser().getLogin();
        String newDomainLogin = newSupportInfo.getUser().getDomainLogin();

        //Дергаем ручку
        String jsonRole = JsonUtils.toJson(singletonMap(SLUG, MAIN_MANAGER_ROLE));
        String jsonFields = JsonUtils.toJson(Map.of(
                PASSPORT_LOGIN_JSON_FIELD, newPassportLogin,
                CLIENT_ID_JSON_FIELD, clientId));
        MockHttpServletRequestBuilder requestBuilder = post(ADD_ROLE_URL)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("login", newDomainLogin)
                .param("role", jsonRole)
                .param("fields", jsonFields);
        ResultActions perform = mockMvc.perform(requestBuilder);
        perform.andExpect(status().isOk());

        String expectedError =
                String.format("primaryManagerUid : Пользователь с uid=%d должен иметь роль 'Менеджер' в Директе",
                        newSupportInfo.getUid());

        //Проверяем результат
        String answer = perform.andReturn().getResponse().getContentAsString();
        JsonNode jsonNode = JsonUtils.fromJson(answer);
        int code = jsonNode.get(CODE_JSON_FIELD).asInt();
        String actualError = jsonNode.get(ERROR_JSON_FIELD).asText();
        Client actualClient = clientRepository.get(clientShard, singletonList(ClientId.fromLong(clientId))).get(0);
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(code).as("result code").isEqualTo(1);
            soft.assertThat(actualError).as("error text").isEqualTo(expectedError);
            soft.assertThat(actualClient.getPrimaryManagerUid()).as("manager uid")
                    .as("manager uid").isEqualTo(startManagerUid);
        });
    }

    @Test
    public void removeRole() throws Exception {
        //Дергаем ручку
        String jsonRole = JsonUtils.toJson(singletonMap(SLUG, MAIN_MANAGER_ROLE));
        String jsonFields = JsonUtils.toJson(Map.of(
                PASSPORT_LOGIN_JSON_FIELD, passportLogin,
                CLIENT_ID_JSON_FIELD, clientId));
        MockHttpServletRequestBuilder requestBuilder = post(REMOVE_ROLE_URL)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("login", domainLogin)
                .param("role", jsonRole)
                .param("data", jsonFields);
        ResultActions perform = mockMvc.perform(requestBuilder);
        perform.andExpect(status().isOk());

        //Проверяем результат
        String answer = perform.andReturn().getResponse().getContentAsString();
        JsonNode jsonNode = JsonUtils.fromJson(answer);
        int code = jsonNode.get(CODE_JSON_FIELD).asInt();
        List<ClientPrimaryManager> actualPrimaryManagers = clientPrimaryManagerService.getAllIdmPrimaryManagers();
        List<String> actualLogins = mapList(actualPrimaryManagers, ClientPrimaryManager::getPassportLogin);
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(code).as("result code").isEqualTo(0);
            soft.assertThat(actualLogins)
                    .as("actual main manager's passport logins list").doesNotContain(passportLogin);
        });
    }

}
