package ru.yandex.direct.intapi.entity.connect.service;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.FreelancerInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.connect.model.AddRoleRequest;
import ru.yandex.direct.intapi.entity.connect.model.ConnectError;
import ru.yandex.direct.intapi.entity.connect.model.IdmErrorResponse;
import ru.yandex.direct.intapi.entity.connect.model.IdmResponse;
import ru.yandex.direct.intapi.entity.connect.model.RequestFields;
import ru.yandex.direct.intapi.entity.connect.model.SubjectType;
import ru.yandex.direct.rbac.RbacRepType;
import ru.yandex.direct.rbac.RbacRole;

import static com.google.common.base.Preconditions.checkState;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.intapi.entity.connect.container.ConnectUtils.ORG_ASSOCIATED_ROLE_PATH;
import static ru.yandex.direct.intapi.entity.connect.container.ConnectUtils.USER_CHIEF_ROLE_PATH;
import static ru.yandex.direct.intapi.entity.connect.container.ConnectUtils.USER_EMPLOYEE_ROLE_PATH;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@IntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ConnectIdmAddRoleServiceTest {
    private static final long ORG_ID = 10L;

    @Autowired
    Steps steps;
    @Autowired
    UserService userService;
    @Autowired
    ConnectIdmAddRoleService connectIdmAddRoleService;
    @Autowired
    ClientService clientService;

    private ClientInfo clientInfo;
    private ClientId clientId;
    private Long chiefUserId;
    private User chiefRep;

    @Before
    public void setUp() {
        clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId();
        steps.clientSteps().setClientProperty(clientInfo, Client.CONNECT_ORG_ID, ORG_ID);
        clientInfo.getClient().withConnectOrgId(ORG_ID);
        chiefUserId = clientInfo.getUid();
        chiefRep = userService.getUser(chiefUserId);
        steps.userSteps().mockUserInExternalClients(chiefRep);
    }

    // выдача роли chief

    @Test
    public void addRoleEmployee_success() {
        User userInBlackbox = steps.userSteps().createUserInBlackboxStub();
        steps.userSteps().mockUserInExternalClients(userInBlackbox);
        Long newRepId = userInBlackbox.getUid();
        AddRoleRequest request = addEmployeeRequest(newRepId);

        addRoleExpectSuccess(request);

        User user = userService.getUser(newRepId);
        assertThat(user).as("user").isNotNull();
        assertThat(user.getClientId()).as("user.clientId").isEqualTo(clientId);
    }

    @Test
    public void addRoleEmployee_doNothing_forChief() {
        AddRoleRequest request = addEmployeeRequest(chiefUserId);

        addRoleExpectSuccess(request);

        checkUserValid(chiefUserId, clientId, RbacRepType.CHIEF);
    }

    @Test
    public void addRoleEmployee_doNothing_forEmployee() {
        UserInfo secondRep = steps.userSteps().createUser(clientInfo, RbacRepType.MAIN);
        Long userId = secondRep.getUid();
        AddRoleRequest request = addEmployeeRequest(userId);

        addRoleExpectSuccess(request);

        checkUserValid(userId, clientId, RbacRepType.MAIN);
    }

    // выдача роли chief

    @Test
    public void addRoleChief_doNothing_forChief() {
        Long userId = clientInfo.getUid();
        AddRoleRequest request = addChiefRequest(userId);
        addRoleExpectSuccess(request);

        checkUserValid(userId, clientId, RbacRepType.CHIEF);
    }

    @Test
    public void addRoleChief_success_forEmployee() {
        Long originalChief = chiefUserId;
        UserInfo secondRepInfo = steps.userSteps().createRepresentative(clientInfo);
        Long userId = secondRepInfo.getUid();
        User secondRep = userService.getUser(userId);
        steps.userSteps().mockUserInExternalClients(secondRep);
        steps.clientSteps().mockClientInExternalClients(clientInfo.getClient(), List.of(chiefRep, secondRep));
        AddRoleRequest request = addChiefRequest(userId);

        addRoleExpectSuccess(request);

        checkUserValid(userId, clientId, RbacRepType.CHIEF);
        checkUserValid(originalChief, clientInfo.getClientId(), RbacRepType.MAIN);
    }

    @Test
    public void addRoleUnknownClient_failure_forOrg() {
        AddRoleRequest request = addOrgRequest(clientId.asLong());
        Long unknownResourceId = request.getFields().getResourceId() + 10L;
        request.setFields(new RequestFields().setResourceId(unknownResourceId));
        IdmResponse idmResponse = connectIdmAddRoleService.addRole(request);
        assertThat(idmResponse).is(matchedBy(beanDiffer(new IdmErrorResponse(ConnectError.RESOURCE_NOT_FOUND))));
    }

    @Test
    public void addRoleNotRegularClient_failure_forOrg() {
        ClientInfo notRegularClient = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPER);
        long resourceId = notRegularClient.getClientId().asLong();
        AddRoleRequest request = addOrgRequest(resourceId);
        IdmResponse idmResponse = connectIdmAddRoleService.addRole(request);
        assertThat(idmResponse)
                .is(matchedBy(beanDiffer(new IdmErrorResponse(ConnectError.RESOURCE_HAS_NO_CLIENT_ROLE))));
    }

    @Test
    public void addRoleFreelancerClient_failure_forOrg() {
        FreelancerInfo freelancerInfo = steps.freelancerSteps().addDefaultFreelancer();
        long resourceId = freelancerInfo.getClientId().asLong();
        AddRoleRequest request = addOrgRequest(resourceId);
        IdmResponse idmResponse = connectIdmAddRoleService.addRole(request);
        assertThat(idmResponse)
                .is(matchedBy(beanDiffer(new IdmErrorResponse(ConnectError.RESOURCE_CANNOT_BE_FREELANCER))));
    }

    @Test
    public void addRoleAlreadySetConnectOrgId_success_forOrg() {
        AddRoleRequest request = addOrgRequest(clientId.asLong());
        addRoleExpectSuccess(request);
    }

    @Test
    public void addRoleOtherHasSameConnectOrgId_failure_forOrg() {
        ClientInfo clientWithoutOrgId = steps.clientSteps().createDefaultClient();
        AddRoleRequest request = addOrgRequest(clientWithoutOrgId.getClientId().asLong());
        IdmResponse idmResponse = connectIdmAddRoleService.addRole(request);
        assertThat(idmResponse)
                .is(matchedBy(beanDiffer(new IdmErrorResponse(ConnectError.ID_ASSOCIATED_TO_ANOTHER_RESOURCE))));
    }

    @Test
    public void addRole_success_forOrg() {
        long nextOrgId = ORG_ID + 1L;
        ClientInfo clientWithoutOrgId = steps.clientSteps().createDefaultClient();
        AddRoleRequest request = createOrgAddRoleRequest(nextOrgId, clientWithoutOrgId.getClientId().asLong());
        addRoleExpectSuccess(request);
        Client clientAfterAddOrgId = clientService.getClient(clientWithoutOrgId.getClientId());
        checkState(clientAfterAddOrgId != null);
        assertThat(clientAfterAddOrgId.getConnectOrgId()).isEqualTo(nextOrgId);
    }

    private void addRoleExpectSuccess(AddRoleRequest request) {
        IdmResponse idmResponse = connectIdmAddRoleService.addRole(request);
        assertThat(idmResponse.getCode()).as("response.code").isEqualTo(0);
    }

    /**
     * Проверяет, что пользователь с заданным {@code userId} является представителем клиента {@code clientId}
     * типа {@code repType}
     */
    private void checkUserValid(long userId, ClientId clientId, RbacRepType repType) {
        User user = userService.getUser(userId);
        assertThat(user).isNotNull();
        assertThat(user.getClientId()).isEqualTo(clientId);
        assertThat(user.getRepType()).as("user.repType").isEqualTo(repType);
    }

    private AddRoleRequest addEmployeeRequest(Long userId) {
        return createPersonalAddRoleRequest(userId, USER_EMPLOYEE_ROLE_PATH);
    }

    private AddRoleRequest addChiefRequest(Long userId) {
        return createPersonalAddRoleRequest(userId, USER_CHIEF_ROLE_PATH);
    }

    private AddRoleRequest addOrgRequest(Long resourceId) {
        return createOrgAddRoleRequest(ORG_ID, resourceId);
    }

    private AddRoleRequest createOrgAddRoleRequest(Long orgId, Long resourceId) {
        return new AddRoleRequest()
                .setId(orgId)
                .setPath(ORG_ASSOCIATED_ROLE_PATH)
                .setSubjectType(SubjectType.ORGANIZATION)
                .setFields(new RequestFields().setResourceId(resourceId));
    }

    private AddRoleRequest createPersonalAddRoleRequest(Long userId, String rolePath) {
        return new AddRoleRequest()
                .setId(userId)
                .setPath(rolePath)
                .setOrgId(ORG_ID)
                .setSubjectType(SubjectType.USER)
                .setFields(new RequestFields().setResourceId(clientId.asLong()));
    }

}
