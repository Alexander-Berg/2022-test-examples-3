package ru.yandex.direct.intapi.entity.connect.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import one.util.streamex.StreamEx;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.ClientSteps;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.UserSteps;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.rbac.RbacRole;

import static com.google.common.base.Preconditions.checkState;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.direct.core.testing.steps.ClientSteps.ANOTHER_SHARD;
import static ru.yandex.direct.core.testing.steps.ClientSteps.DEFAULT_SHARD;
import static ru.yandex.direct.intapi.entity.connect.container.ConnectUtils.ORG_ASSOCIATED_ROLE_PATH;
import static ru.yandex.direct.intapi.entity.connect.container.ConnectUtils.ORG_SUBJECT_TYPE;
import static ru.yandex.direct.intapi.entity.connect.container.ConnectUtils.USER_CHIEF_ROLE_PATH;
import static ru.yandex.direct.intapi.entity.connect.container.ConnectUtils.USER_EMPLOYEE_ROLE_PATH;
import static ru.yandex.direct.intapi.entity.connect.container.ConnectUtils.USER_SUBJECT_TYPE;
import static ru.yandex.direct.intapi.util.IntapiUtils.jsonAsCompactString;
import static ru.yandex.direct.intapi.util.IntapiUtils.node;
import static ru.yandex.direct.utils.CommonUtils.ifNotNull;
import static ru.yandex.direct.utils.JsonUtils.fromJson;

@IntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ConnectIdmControllerGetRolesTest extends ConnectIdmControllerBase {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private static final String GET_ROLES_PATH = "/connect/idm/get-roles";
    private static final String SPECIFIED_CLIENT_ANSWER_TEMPLATE = "{\"roles\":[%1$s],\"code\":0}";
    private static final int ROLES_NUMBER_PER_CONNECT_CLIENT = 4;

    @Autowired
    private Steps steps;

    private ClientInfo firstClientInfo;
    private List<String> firstClientRoles;
    private String[] bothClientsRoles;

    private static List<String> getRolesAsStrings(Client client, Long repUid) {
        Long orgId = client.getConnectOrgId();
        Long clientId = client.getClientId();
        Long chiefUid = client.getChiefUid();
        return StreamEx
                .of(
                        getChiefRoleJson(chiefUid, orgId, clientId),
                        getEmployeeRoleJson(chiefUid, orgId, clientId),
                        getEmployeeRoleJson(repUid, orgId, clientId),
                        getAssociatedRoleJson(orgId, clientId))
                .filter(Objects::nonNull)
                .toList();
    }

    private static String getAssociatedRoleJson(Long orgId, Long clientId) {
        return ifNotNull(orgId, oId -> getRoleJson(ORG_ASSOCIATED_ROLE_PATH, oId, oId, clientId, ORG_SUBJECT_TYPE));
    }

    private static String getEmployeeRoleJson(Long repUid, Long orgId, Long clientId) {
        return ifNotNull(repUid, uid -> getRoleJson(USER_EMPLOYEE_ROLE_PATH, uid, orgId, clientId, USER_SUBJECT_TYPE));
    }

    private static String getChiefRoleJson(Long uid, Long orgId, Long clientId) {
        return getRoleJson(USER_CHIEF_ROLE_PATH, uid, orgId, clientId, USER_SUBJECT_TYPE);
    }

    private static String getRoleJson(String path, Long id, Long orgId, Long resourceId, String subjectType) {
        return jsonAsCompactString(
                node("path", path),
                node("id", innToString(id)),
                node("org_id", innToString(orgId)),
                node("fields", node("resource_id", innToString(resourceId))),
                node("subject_type", subjectType)
        );
    }

    private static String innToString(Object obj) {
        return ifNotNull(obj, String::valueOf);
    }


    @Before
    public void setUp() {
        super.initTest();
        ClientSteps clientSteps = steps.clientSteps();
        UserSteps userSteps = steps.userSteps();
        //first client
        firstClientInfo = clientSteps.createClient(new ClientInfo().withShard(DEFAULT_SHARD));
        steps.clientSteps().setFakeConnectOrgId(firstClientInfo);
        UserInfo firstUserInfo = userSteps.createRepresentative(firstClientInfo);
        firstClientRoles = getRolesAsStrings(firstClientInfo.getClient(), firstUserInfo.getUid());
        //second client
        ClientInfo secondClientInfo = clientSteps.createClient(new ClientInfo().withShard(ANOTHER_SHARD));
        steps.clientSteps().setFakeConnectOrgId(secondClientInfo);
        UserInfo secondUserInfo = userSteps.createRepresentative(secondClientInfo);
        List<String> secondClientRoles = getRolesAsStrings(secondClientInfo.getClient(), secondUserInfo.getUid());

        bothClientsRoles = StreamEx.of(firstClientRoles, secondClientRoles)
                .flatMap(StreamEx::of)
                .toArray(String[]::new);
    }

    @Test
    public void getRoles_getSpecifiedConnectOrgClient_success() throws Exception {
        String clientRolesArray = StreamEx.of(firstClientRoles).joining(",");
        String expectedAnswer = String.format(SPECIFIED_CLIENT_ANSWER_TEMPLATE, clientRolesArray);

        //Дергаем ручку
        String query = "resource_id=" + firstClientInfo.getClientId();
        String answer = doRequest(get(GET_ROLES_PATH), query);

        //Проверяем ответ
        assertThat(answer).as("server answer").isEqualTo(expectedAnswer);
    }

    @Test
    public void getRoles_getSpecifiedNonConnectOrgClient_success() throws Exception {
        //Создаём клиента без ConnectOrgId
        ClientInfo clientInfo = steps.clientSteps().createDefaultClientWithRole(RbacRole.CLIENT);
        UserInfo userInfo = steps.userSteps().createRepresentative(clientInfo);
        checkState(clientInfo.getClient().getConnectOrgId() == null,
                "It is expected that ClientSteps#createDefaultClient() creates client without connectOrgId");

        //Ожидаемый ответ
        List<String> expectedRoles = getRolesAsStrings(clientInfo.getClient(), userInfo.getUid());
        String clientRolesArray = StringUtils.join(expectedRoles, ",");
        String expectedAnswer = String.format(SPECIFIED_CLIENT_ANSWER_TEMPLATE, clientRolesArray);

        //Дергаем ручку
        String query = "resource_id=" + clientInfo.getClientId();
        String answer = doRequest(get(GET_ROLES_PATH), query);

        //Проверяем ответ
        assertThat(answer).as("server answer").isEqualTo(expectedAnswer);
    }

    @Test
    public void getRoles_getNonexistentClient_failure() throws Exception {
        int nonexistentClientId = Integer.MAX_VALUE - 1;

        String answer = doRequest(get(GET_ROLES_PATH), "resource_id=" + nonexistentClientId);

        Map<String, Object> expectedAnswer = Map.of(
                "error", "resource_id='" + nonexistentClientId + "' not found",
                "code", 1);
        assertThat(fromJson(answer, MAP_TYPE)).as("server answer").isEqualTo(expectedAnswer);
    }

    @Test
    public void getRoles_withSinglePage_success() throws Exception {
        //Дергаем ручку без параметров, должны вернуться роли всех клиентов имеющих ConnectOrgId за один раз
        String answer = doRequest(get(GET_ROLES_PATH), null);
        JsonNode jsonNode = fromJson(answer);
        ArrayList<String> returnedRoles = new ArrayList<>();
        ArrayNode roles = (ArrayNode) jsonNode.get("roles");
        for (JsonNode role : roles) {
            returnedRoles.add(role.toString());
        }

        assertThat(returnedRoles).as("returned roles").contains(bothClientsRoles);
    }

    @Test
    public void getRoles_withSeveralPages_success() throws Exception {
        //Дергаем ручку с лимитом в один клиент, должны вернуться роли всех клиентов имеющих ConnectOrgId за
        // множество итераций
        int clientsLimit = 1;
        String nextUrl = GET_ROLES_PATH + "?clients_limit=" + clientsLimit;
        ArrayList<String> returnedRoles = new ArrayList<>();
        while (nextUrl != null) {
            String query = nextUrl.substring(nextUrl.indexOf('?') + 1);
            String answer = doRequest(get(GET_ROLES_PATH), query);
            JsonNode jsonNode = fromJson(answer);
            JsonNode nextUrlNode = jsonNode.get("next-url");
            nextUrl = ifNotNull(nextUrlNode, JsonNode::asText);
            ArrayNode roles = (ArrayNode) jsonNode.get("roles");
            int maxRolesNumber = ROLES_NUMBER_PER_CONNECT_CLIENT * clientsLimit;
            checkState(roles.size() <= maxRolesNumber,
                    "Unexpected count of roles, expected not great then %s but found %s", maxRolesNumber, roles.size());
            for (JsonNode role : roles) {
                returnedRoles.add(role.toString());
            }
        }

        assertThat(returnedRoles).as("returned roles").contains(bothClientsRoles);
    }
}
