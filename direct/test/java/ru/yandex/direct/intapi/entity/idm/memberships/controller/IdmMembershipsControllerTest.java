package ru.yandex.direct.intapi.entity.idm.memberships.controller;

import java.util.ArrayList;
import java.util.List;

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

import ru.yandex.direct.core.entity.idm.model.IdmGroup;
import ru.yandex.direct.core.entity.idm.model.IdmGroupMember;
import ru.yandex.direct.core.entity.idm.model.IdmRequiredRole;
import ru.yandex.direct.core.entity.idm.repository.IdmGroupsMembersRepository;
import ru.yandex.direct.core.entity.idm.repository.IdmGroupsRepository;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.idm.memberships.model.Membership;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.utils.JsonUtils;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.singletonList;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;
import static ru.yandex.direct.core.testing.steps.ClientSteps.ANOTHER_SHARD;
import static ru.yandex.direct.core.testing.steps.ClientSteps.DEFAULT_SHARD;
import static ru.yandex.direct.utils.CommonUtils.ifNotNull;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@IntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class IdmMembershipsControllerTest {

    private static final String ADD_BATCH_MEMBERSHIPS_URL = "/idm/add-batch-memberships";
    private static final String REMOVE_BATCH_MEMBERSHIPS_URL = "/idm/remove-batch-memberships";
    private static final String GET_MEMBERSHIPS_URL = "/idm/get-memberships";

    private static final long IDM_GROUP_ID = 11L;
    private static final String OK_ANSWER = "{\"code\":0}";

    @Autowired
    private Steps steps;
    @Autowired
    private IdmGroupsRepository idmGroupsRepository;
    @Autowired
    private IdmGroupsMembersRepository idmGroupsMembersRepository;
    @Autowired
    private IdmMembershipsController idmMembershipsController;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        IdmGroup idmGroup = new IdmGroup()
                .withIdmGroupId(IDM_GROUP_ID)
                .withRequiredRole(IdmRequiredRole.MANAGER);
        idmGroupsRepository.add(singletonList(idmGroup));
        mockMvc = MockMvcBuilders.standaloneSetup(idmMembershipsController).build();
    }

    @Test
    public void addMemberships_success() throws Exception {
        //Подготваливаем данные
        UserInfo userInfo = steps.clientSteps().createDefaultClientWithRoleInAnotherShard(RbacRole.MANAGER)
                .getChiefUserInfo();
        Membership newMembership = new Membership()
                .withDomainLogin(userInfo.getUser().getDomainLogin())
                .withPassportLogin(userInfo.getUser().getLogin())
                .withGroup((int) IDM_GROUP_ID);
        ClientId clientId = userInfo.getClientInfo().getClientId();
        IdmGroupMember expectedMember = new IdmGroupMember().withUid(userInfo.getUid())
                .withClientId(clientId)
                .withDomainLogin(userInfo.getUser().getDomainLogin())
                .withIdmGroupId(IDM_GROUP_ID)
                .withLogin(userInfo.getUser().getLogin());

        //Дергаем ручку
        List<Membership> memberships = new ArrayList<>();
        memberships.add(newMembership);
        String json = JsonUtils.toJson(memberships);
        MockHttpServletRequestBuilder requestBuilder = post(ADD_BATCH_MEMBERSHIPS_URL)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("data", json);
        ResultActions perform = mockMvc.perform(requestBuilder);
        perform.andExpect(status().isOk());

        //Проверяем результат
        String answer = perform.andReturn().getResponse().getContentAsString();
        IdmGroupMember actualMember =
                idmGroupsMembersRepository.getMember(userInfo.getShard(), clientId, IDM_GROUP_ID)
                        .orElse(null);
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(answer).isEqualTo(OK_ANSWER);
            soft.assertThat(actualMember).isEqualTo(expectedMember);
        });
    }

    @Test
    public void addMemberships_whenLoginNotExist_failure() throws Exception {
        //Подготваливаем данные
        int fakeIdmGroupId = Integer.MAX_VALUE - 1;
        String idmFakeDomainLogin = "idm_fake_domain_login";
        String idmFakePassportLogin = "idm_fake_passport_login";
        Membership newMembership = new Membership()
                .withDomainLogin(idmFakeDomainLogin)
                .withPassportLogin(idmFakePassportLogin)
                .withGroup(fakeIdmGroupId);

        //Дергаем ручку
        List<Membership> memberships = new ArrayList<>();
        memberships.add(newMembership);
        String json = JsonUtils.toJson(memberships);
        MockHttpServletRequestBuilder requestBuilder = post(ADD_BATCH_MEMBERSHIPS_URL)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("data", json);
        ResultActions perform = mockMvc.perform(requestBuilder);
        perform.andExpect(status().isOk());

        //Проверяем результат
        String answer = perform.andReturn().getResponse().getContentAsString();
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(answer).contains("\"code\":207");
            soft.assertThat(answer).contains(idmFakeDomainLogin);
            soft.assertThat(answer).contains(idmFakePassportLogin);
            soft.assertThat(answer).contains(Integer.toString(fakeIdmGroupId));
        });
    }

    @Test
    public void removeMemberships_success() throws Exception {
        //Подготваливаем исходное состояние и данные
        UserInfo userInfo = steps.clientSteps().createDefaultClientWithRoleInAnotherShard(RbacRole.MANAGER)
                .getChiefUserInfo();
        String login = userInfo.getUser().getLogin();
        String domainLogin = userInfo.getUser().getDomainLogin();
        ClientId clientId = userInfo.getClientInfo().getClientId();
        IdmGroupMember newMember = new IdmGroupMember().withUid(userInfo.getUid())
                .withClientId(clientId)
                .withDomainLogin(domainLogin)
                .withIdmGroupId(IDM_GROUP_ID)
                .withLogin(login);
        Integer shard = userInfo.getShard();
        idmGroupsMembersRepository.addMembersWhichNotExist(shard, singletonList(newMember));

        //Дергаем ручку
        Membership membership = new Membership()
                .withDomainLogin(domainLogin)
                .withPassportLogin(login)
                .withGroup((int) IDM_GROUP_ID);
        List<Membership> memberships = new ArrayList<>();
        memberships.add(membership);
        String json = JsonUtils.toJson(memberships);
        MockHttpServletRequestBuilder requestBuilder = post(REMOVE_BATCH_MEMBERSHIPS_URL)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("data", json);
        ResultActions perform = mockMvc.perform(requestBuilder);
        perform.andExpect(status().isOk());

        //Проверяем результат
        String answer = perform.andReturn().getResponse().getContentAsString();
        IdmGroupMember actualMember = idmGroupsMembersRepository.getMember(shard, clientId, IDM_GROUP_ID)
                .orElse(null);
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(answer).isEqualTo(OK_ANSWER);
            soft.assertThat(actualMember).isNull();
        });
    }

    @Test
    public void getMemberships_withSinglePage_success() throws Exception {
        //Подготваливаем исходное состояние и данные
        UserInfo userInfo = steps.clientSteps().createDefaultClientWithRoleInAnotherShard(RbacRole.MANAGER)
                .getChiefUserInfo();
        String login = userInfo.getUser().getLogin();
        String domainLogin = userInfo.getUser().getDomainLogin();
        ClientId clientId = userInfo.getClientInfo().getClientId();
        IdmGroupMember newMember = new IdmGroupMember().withUid(userInfo.getUid())
                .withClientId(clientId)
                .withDomainLogin(domainLogin)
                .withIdmGroupId(IDM_GROUP_ID)
                .withLogin(login);
        Integer shard = userInfo.getShard();
        idmGroupsMembersRepository.addMembersWhichNotExist(shard, singletonList(newMember));

        //Дергаем ручку
        MockHttpServletRequestBuilder requestBuilder = get(GET_MEMBERSHIPS_URL)
                .accept(MediaType.APPLICATION_JSON);
        ResultActions perform = mockMvc.perform(requestBuilder);
        perform.andExpect(status().isOk());

        //Проверяем результат
        String answer = perform.andReturn().getResponse().getContentAsString();
        JsonNode jsonNode = JsonUtils.fromJson(answer);
        int code = jsonNode.get("code").asInt();
        JsonNode memberships = jsonNode.get("memberships");
        List<String> logins = memberships.findValuesAsText("login");
        List<String> passportLogins = memberships.findValuesAsText("passport_login");
        List<JsonNode> groupNodes = memberships.findValues("group");
        List<Integer> groups = mapList(groupNodes, JsonNode::asInt);
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(code).isEqualTo(0);
            soft.assertThat(passportLogins).contains(login);
            soft.assertThat(logins).contains(domainLogin);
            soft.assertThat(groups).contains((int) IDM_GROUP_ID);
        });
    }

    @Test
    public void getMemberships_withPaging_success() throws Exception {
        //Подготваливаем исходное состояние и данные
        int pageSize = 3;
        int membersCount = 10;
        String[] domainLogins = new String[membersCount];
        String[] pasportLogins = new String[membersCount];
        Integer[] groupIds = new Integer[membersCount];
        for (int i = 0; i < membersCount; i++) {
            //проверяем, что пейджинг корректно работает с разными шардами
            int shard = i % 2 == 0 ? DEFAULT_SHARD : ANOTHER_SHARD;
            UserInfo managerInfo = steps.clientSteps().createClient(
                    new ClientInfo()
                    .withShard(shard)
                    .withClient(defaultClient().withRole(RbacRole.MANAGER))).getChiefUserInfo();
            int idmGroupId = 100 + i;
            steps.idmGroupSteps().addIfNotExistIdmGroup(idmGroupId, IdmRequiredRole.MANAGER);
            IdmGroupMember newMember = new IdmGroupMember().withUid(managerInfo.getUid())
                    .withClientId(managerInfo.getClientInfo().getClientId())
                    .withDomainLogin(managerInfo.getUser().getDomainLogin())
                    .withIdmGroupId((long) idmGroupId)
                    .withLogin(managerInfo.getUser().getLogin());
            idmGroupsMembersRepository.addMembersWhichNotExist(shard, singletonList(newMember));
            domainLogins[i] = managerInfo.getUser().getDomainLogin();
            pasportLogins[i] = managerInfo.getUser().getLogin();
            groupIds[i] = idmGroupId;
        }

        //Дергаем ручку порциями по 3, пока не вернёт всех членов всех групп
        ArrayList<String> returnedDomainLogins = new ArrayList<>();
        ArrayList<String> returnedPassportLogins = new ArrayList<>();
        ArrayList<Integer> returnedGroupIds = new ArrayList<>();
        String nextUrl = GET_MEMBERSHIPS_URL + "?limit=" + pageSize;
        while (nextUrl != null) {
            MockHttpServletRequestBuilder requestBuilder = get(nextUrl)
                    .accept(MediaType.APPLICATION_JSON);
            ResultActions perform = mockMvc.perform(requestBuilder);
            perform.andExpect(status().isOk());
            String answer = perform.andReturn().getResponse().getContentAsString();
            JsonNode jsonNode = JsonUtils.fromJson(answer);
            JsonNode nextUrlNode = jsonNode.get("next-url");
            nextUrl = ifNotNull(nextUrlNode, JsonNode::asText);
            int code = jsonNode.get("code").asInt();
            checkState(code == 0, "Unexpected code");
            JsonNode memberships = jsonNode.get("memberships");
            returnedDomainLogins.addAll(memberships.findValuesAsText("login"));
            returnedPassportLogins.addAll(memberships.findValuesAsText("passport_login"));
            returnedGroupIds.addAll(mapList(memberships.findValues("group"), JsonNode::asInt));
        }

        //проверяем полноту возвращённых данных
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(returnedDomainLogins).as("returned domain logins").contains(domainLogins);
            soft.assertThat(returnedPassportLogins).as("returned passport logins").contains(pasportLogins);
            soft.assertThat(returnedGroupIds).as("returned group ids").contains(groupIds);
        });
    }

}
