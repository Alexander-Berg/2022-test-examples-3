package ru.yandex.direct.intapi.entity.connect.controller;

import java.util.List;

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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.entity.client.repository.ClientRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.repository.UserRepository;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.rbac.RbacRepType;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.intapi.entity.connect.container.ConnectUtils.USER_CHIEF_ROLE_PATH;
import static ru.yandex.direct.intapi.entity.connect.container.ConnectUtils.USER_EMPLOYEE_ROLE_PATH;
import static ru.yandex.direct.intapi.entity.connect.container.ConnectUtils.USER_SUBJECT_TYPE;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@IntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ConnectIdmRolesControllerAddPersonalRoleTest {
    private static final String ADD_ROLES_PATH = "/connect/idm/add-role";

    @Autowired
    private ConnectIdmRolesController controller;
    @Autowired
    private Steps steps;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private ClientRepository clientRepository;
    private MockMvc mockMvc;
    private ClientId clientId;
    private Integer shard;
    private ClientInfo clientInfo;
    private User chefRep;

    @Before
    public void setUp() {
        clientInfo = steps.clientSteps().createDefaultClient();
        chefRep = clientInfo.getChiefUserInfo().getUser();
        steps.userSteps().mockUserInExternalClients(chefRep);
        steps.clientSteps().setFakeConnectOrgId(clientInfo);
        shard = clientInfo.getShard();
        clientId = clientInfo.getClientId();
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    public void addRole_whenEmptyRequest_failure() throws Exception {
        String expectedAnswer = "{\"code\":110,\"error\":\"absent fields.resource_id\"}";
        String jsonAnswer = doAddRoleEmptyRequest();
        assertThat(jsonAnswer).as("answer").isEqualTo(expectedAnswer);
    }

    @Test
    public void addRole_whenEmployee_success() throws Exception {
        //Подготавливаем начальное состояние
        User blackboxUser = steps.userSteps().createUserInBlackboxStub();
        steps.userSteps().mockUserInExternalClients(blackboxUser);
        Long uid = blackboxUser.getUid();

        //Выполняем запрос
        String jsonAnswer = doAddRoleRequest(USER_EMPLOYEE_ROLE_PATH, uid, clientInfo.getClient(), USER_SUBJECT_TYPE);

        //Сверяем ответ и состояние системы с ожидаемыми
        User actualUser = userRepository.fetchByUids(shard, singleton(uid)).get(0);
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(jsonAnswer).as("answer").isEqualTo("{\"code\":0}");
            soft.assertThat(actualUser).as("actual user")
                    .is(matchedBy(beanDiffer(blackboxUser).useCompareStrategy(onlyExpectedFields())));
        });
    }

    @Test
    public void addRole_whenEmployeeAlreadyAssociatedWithOtherClient_failure() throws Exception {
        //Подготавливаем начальное состояние
        Long fakeOtherClientId = Integer.MAX_VALUE - 1L;
        User blackboxUser = steps.userSteps().createUserInBlackboxStub();
        blackboxUser.withClientId(ClientId.fromLong(fakeOtherClientId));
        steps.userSteps().mockUserInExternalClients(blackboxUser);
        Long uid = blackboxUser.getUid();

        //Выполняем запрос
        String jsonAnswer = doAddRoleRequest(USER_EMPLOYEE_ROLE_PATH, uid, clientInfo.getClient(), USER_SUBJECT_TYPE);

        //Сверяем ответ с ожидаемым
        assertThat(jsonAnswer).as("answer")
                .isEqualTo("{\"code\":340,\"error\":\"user associated to another balance client\"}");
    }

    @Test
    public void addRole_whenChief_success() throws Exception {
        //Подготавливаем начальное состояние
        UserInfo newRepInfo = steps.userSteps().createUser(clientInfo, RbacRepType.MAIN);
        Long oldChiefUid = clientInfo.getUid();
        Long newChiefUid = newRepInfo.getUid();
        User newRep = userService.getUser(newChiefUid);
        steps.userSteps().mockUserInExternalClients(newRep);
        steps.clientSteps().mockClientInExternalClients(clientInfo.getClient(), List.of(chefRep, newRep));

        //Выполняем запрос
        String jsonAnswer = doAddRoleRequest(
                USER_CHIEF_ROLE_PATH, newChiefUid, clientInfo.getClient(), USER_SUBJECT_TYPE);

        //Сверяем ответ и состояние системы с ожидаемыми
        Long clientChiefUid = clientRepository.get(shard, singletonList(clientId)).get(0).getChiefUid();
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(jsonAnswer).as("answer").isEqualTo("{\"code\":0}");
            soft.assertThat(getActualRepType(newChiefUid)).as("new chief RepType")
                    .isEqualTo(RbacRepType.CHIEF);
            soft.assertThat(getActualRepType(oldChiefUid)).as("old chief RepType")
                    .isEqualTo(RbacRepType.MAIN);
            soft.assertThat(clientChiefUid).as("chief uid in Client")
                    .isEqualTo(newChiefUid);
        });
    }

    private RbacRepType getActualRepType(Long newChiefUid) {
        return userRepository.fetchByUids(shard, singleton(newChiefUid))
                .get(0)
                .getRepType();
    }

    private String doAddRoleRequest(String path, Long id, Client client, String subjectType) throws Exception {
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.post(ADD_ROLES_PATH)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("id", id.toString())
                .param("path", path)
                .param("org_id", client.getConnectOrgId().toString())
                .param("fields", "{\"resource_id\":" + client.getClientId().toString() + "}")
                .param("subject_type", subjectType);
        ResultActions perform = mockMvc.perform(requestBuilder);
        perform.andExpect(status().isOk());
        return perform.andReturn().getResponse().getContentAsString();
    }

    private String doAddRoleEmptyRequest() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.post(ADD_ROLES_PATH)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED);
        ResultActions perform = mockMvc.perform(requestBuilder);
        perform.andExpect(status().isOk());
        return perform.andReturn().getResponse().getContentAsString();
    }

}
