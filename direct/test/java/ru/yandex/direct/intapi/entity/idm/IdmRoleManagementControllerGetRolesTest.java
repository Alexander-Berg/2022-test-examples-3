package ru.yandex.direct.intapi.entity.idm;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import one.util.streamex.StreamEx;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
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

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.idm.model.IdmGroup;
import ru.yandex.direct.core.entity.idm.model.IdmRequiredRole;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.ClientPrimaryManagerInfo;
import ru.yandex.direct.core.testing.info.IdmGroupRoleInfo;
import ru.yandex.direct.core.testing.info.SupportForClientInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.ClientSteps;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.idm.controller.IdmRoleManagementController;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.utils.JsonUtils;

import static com.google.common.base.Preconditions.checkState;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.direct.utils.CommonUtils.ifNotNull;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@IntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class IdmRoleManagementControllerGetRolesTest {
    private static final String GET_ROLES_PATH = "/idm/get-roles";
    private static final long NEW_GROUP_ID = 77L;

    @Autowired
    private Steps steps;
    @Autowired
    private IdmRoleManagementController controller;
    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;

    private String[] allPersonalRoleManagerLogins;
    private Long[] allRoleClientIds;
    private String[] clientLogins;
    private Long[] clientClientIds;
    private MockMvc mockMvc;
    private Integer[] allGroupIds;

    @Before
    public void setUp() {
        ClientSteps clientSteps = steps.clientSteps();

        //Персональные роли и клиенты всех видов
        List<UserInfo> internalUsers = Arrays.stream(RbacRole.values())
                .filter(l -> l.isInternal() || l.anyOf(RbacRole.LIMITED_SUPPORT))
                .map(clientSteps::createDefaultClientWithRoleInAnotherShard)
                .map(ClientInfo::getChiefUserInfo)
                .collect(toList());
        List<ClientInfo> clients = Arrays.stream(RbacRole.values())
                .filter(l -> !l.isInternal() && !l.anyOf(RbacRole.LIMITED_SUPPORT))
                .map(clientSteps::createDefaultClientWithRole)
                .collect(toList());
        clientLogins = StreamEx.of(clients)
                .map(ClientInfo::getLogin)
                .toArray(String[]::new);
        clientClientIds = StreamEx.of(clients)
                .map(ClientInfo::getClientId)
                .map(ClientId::asLong)
                .toArray(Long[]::new);

        //Саппорт по запросу представителя клиента
        SupportForClientInfo supportForClientInfo = steps.idmGroupSteps().createSupportForClientInfo();

        //Главные менеджеры
        UserInfo firstManagerInfo =
                clientSteps.createDefaultClientWithRoleInAnotherShard(RbacRole.MANAGER).getChiefUserInfo();
        ClientInfo firstClientInfo = clientSteps.createDefaultClient();
        ClientPrimaryManagerInfo firstPrimaryManagerInfo =
                steps.idmGroupSteps().addIdmPrimaryManager(firstManagerInfo, firstClientInfo);
        UserInfo secondManagerInfo =
                clientSteps.createDefaultClientWithRoleInAnotherShard(RbacRole.MANAGER).getChiefUserInfo();
        ClientInfo secondClientInfo =
                clientSteps.createClient(new ClientInfo().withShard(ClientSteps.ANOTHER_SHARD));
        ClientPrimaryManagerInfo secondPrimaryManagerInfo =
                steps.idmGroupSteps().addIdmPrimaryManager(secondManagerInfo, secondClientInfo);

        allPersonalRoleManagerLogins = StreamEx.of(internalUsers)
                .map(i -> i.getUser().getLogin())
                .append(supportForClientInfo.getOperatorInfo().getUser().getLogin())
                .append(firstPrimaryManagerInfo.getManagerInfo().getUser().getLogin())
                .append(secondPrimaryManagerInfo.getManagerInfo().getUser().getLogin())
                .map(String::toLowerCase)
                .toArray(String[]::new);

        //Групповые роли
        IdmGroupRoleInfo firstIdmGroupRoleInfo = steps.idmGroupSteps().createDefaultIdmGroupRole();
        IdmGroup newIdmGroup = steps.idmGroupSteps().addIfNotExistIdmGroup(NEW_GROUP_ID, IdmRequiredRole.MANAGER);
        IdmGroupRoleInfo secondIdmGroupRoleInfo =
                steps.idmGroupSteps().addIdmGroupRole(
                        new IdmGroupRoleInfo()
                                .withClientInfo(
                                        clientSteps.createClient(new ClientInfo()
                                                .withShard(ClientSteps.ANOTHER_SHARD)))
                                .withIdmGroup(newIdmGroup));
        allRoleClientIds = StreamEx.of(firstIdmGroupRoleInfo.getClientId(), secondIdmGroupRoleInfo.getClientId(),
                firstPrimaryManagerInfo.getSubjectClientId(), secondPrimaryManagerInfo.getSubjectClientId(),
                supportForClientInfo.getSubjectClientInfo().getClientId())
                .map(ClientId::asLong)
                .toArray(Long[]::new);
        allGroupIds = StreamEx.of(firstIdmGroupRoleInfo.getIdmGroupId(), secondIdmGroupRoleInfo.getIdmGroupId())
                .map(Long::intValue)
                .toArray(Integer[]::new);

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    public void getRoles_withDefaultParamsAndSinglePage() throws Exception {
        //Дергаем ручку без параметров, по дефолту может вернуться до 1000 записей
        JsonNode jsonNode = doRequest(GET_ROLES_PATH);
        int code = jsonNode.get("code").asInt();
        checkState(code == 0, "Unexpected code");
        JsonNode roles = jsonNode.get("roles");
        List<String> returnedPassportLogins = roles.findValuesAsText("passport-login");
        List<String> clientIdAsStings = roles.findValuesAsText("client_id");
        List<Long> returnedClientIds = mapList(clientIdAsStings, Long::parseLong);
        List<JsonNode> groupNodes = roles.findValues("group");
        List<Integer> returnedGroupIds = mapList(groupNodes, JsonNode::asInt);

        //Проверяем полноту и точность возвращённых ролей
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(returnedPassportLogins)
                    .as("returned passport logins")
                    .contains(allPersonalRoleManagerLogins)
                    .doesNotContain(clientLogins);
            soft.assertThat(returnedClientIds)
                    .as("returned clientIds")
                    .contains(allRoleClientIds)
                    .doesNotContain(clientClientIds);
            soft.assertThat(returnedGroupIds)
                    .as("returned groupIds")
                    .contains(allGroupIds);
        });
    }

    @Test
    public void getRoles_withPagingByLimitedCountRoles() throws Exception {
        //Дергаем ручку пока не получим все роли порциями по 3 штуки
        int pageLimit = 3;
        String nextUrl = GET_ROLES_PATH + "?limit=" + pageLimit;
        List<String> returnedPassportLogins = new ArrayList<>();
        List<Long> returnedClientIds = new ArrayList<>();
        List<Integer> returnedGroupIds = new ArrayList<>();
        while (nextUrl != null) {
            JsonNode jsonNode = doRequest(nextUrl);
            JsonNode nextUrlNode = jsonNode.get("next-url");
            nextUrl = ifNotNull(nextUrlNode, JsonNode::asText);
            int code = jsonNode.get("code").asInt();
            checkState(code == 0, "Unexpected code");
            JsonNode roles = jsonNode.get("roles");
            checkState(roles.size() <= pageLimit,
                    "Unexpected count of roles, expected not great then %s but found %s", pageLimit, roles.size());
            List<String> passportLogins = roles.findValuesAsText("passport-login");
            returnedPassportLogins.addAll(passportLogins);
            List<String> clientIdAsStings = roles.findValuesAsText("client_id");
            List<Long> clientIds = mapList(clientIdAsStings, Long::parseLong);
            returnedClientIds.addAll(clientIds);
            List<JsonNode> groupNodes = roles.findValues("group");
            List<Integer> groupIds = mapList(groupNodes, JsonNode::asInt);
            returnedGroupIds.addAll(groupIds);
        }

        //Проверяем полноту и точность возвращённых ролей
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(returnedPassportLogins)
                    .as("returned passport logins")
                    .contains(allPersonalRoleManagerLogins)
                    .doesNotContain(clientLogins);
            soft.assertThat(returnedClientIds)
                    .as("returned clientIds")
                    .contains(allRoleClientIds)
                    .doesNotContain(clientClientIds);
            soft.assertThat(returnedGroupIds)
                    .as("returned groupIds")
                    .contains(allGroupIds);
        });
    }

    private JsonNode doRequest(String relativeUrl) throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get(GET_ROLES_PATH)
                .accept(MediaType.APPLICATION_JSON);
        URL url = new URL("https://localhost:443" + relativeUrl);
        String query = url.getQuery();
        if (isNotEmpty(query)) {
            List<NameValuePair> valuePairs = URLEncodedUtils.parse(query, UTF_8);
            for (NameValuePair pair : valuePairs) {
                requestBuilder.param(pair.getName(), pair.getValue());
            }
        }
        ResultActions perform = mockMvc.perform(requestBuilder);
        perform.andExpect(status().isOk());
        String answer = perform.andReturn().getResponse().getContentAsString();
        return JsonUtils.fromJson(answer);
    }

}
