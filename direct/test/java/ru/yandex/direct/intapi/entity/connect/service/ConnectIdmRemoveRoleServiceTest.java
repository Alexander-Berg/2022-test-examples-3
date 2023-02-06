package ru.yandex.direct.intapi.entity.connect.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.user.repository.UserRepository;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.testing.data.TestClients;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.PassportClientStub;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.connect.model.ConnectError;
import ru.yandex.direct.intapi.entity.connect.model.IdmErrorResponse;
import ru.yandex.direct.intapi.entity.connect.model.IdmResponse;
import ru.yandex.direct.intapi.entity.connect.model.IdmSuccessResponse;
import ru.yandex.direct.intapi.entity.connect.model.RemoveRoleRequest;
import ru.yandex.direct.intapi.entity.connect.model.RequestFields;
import ru.yandex.direct.intapi.entity.connect.model.SubjectType;
import ru.yandex.direct.result.Result;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static com.google.common.base.Preconditions.checkState;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.user.service.validation.UserDefects.balanceUserAssociatedWithAnotherClient;
import static ru.yandex.direct.core.entity.user.service.validation.UserDefects.userHasActiveAutoPay;
import static ru.yandex.direct.intapi.entity.connect.container.ConnectUtils.ORG_ASSOCIATED_ROLE_PATH;
import static ru.yandex.direct.intapi.entity.connect.container.ConnectUtils.USER_CHIEF_ROLE_PATH;
import static ru.yandex.direct.intapi.entity.connect.container.ConnectUtils.USER_EMPLOYEE_ROLE_PATH;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@IntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ConnectIdmRemoveRoleServiceTest {

    private static final String NON_EXISTING_ROLE_PATH = "/non-existing/role/path";
    private static final Long ORGANIZATION_ID = 22323222L;

    @Autowired
    ConnectIdmRemoveRoleService connectIdmRemoveRoleService;

    @Autowired
    ClientService clientService;

    @Autowired
    Steps steps;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PassportClientStub passportClientStub;

    private RemoveRoleRequest request;
    private ClientInfo clientInfo;
    private UserInfo regularUser;


    @Before
    public void setUp() {
        Client client = TestClients.defaultClient().withConnectOrgId(ORGANIZATION_ID);
        clientInfo = steps.clientSteps().createClient(client);

        regularUser = steps.userSteps().createRepresentative(clientInfo);

        request = new RemoveRoleRequest();
        RequestFields fields = new RequestFields();
        fields.setResourceId(clientInfo.getClientId().asLong());
        request.setFields(fields);
        request.setSubjectType(SubjectType.USER);
        request.setPath(USER_CHIEF_ROLE_PATH);
        request.setId(clientInfo.getUid());
        request.setOrgId(ORGANIZATION_ID);
    }

    @Test
    public void removeRole_fields_validation() {
        RequestFields emptyFields = new RequestFields();
        request.setFields(emptyFields);
        IdmResponse idmResponse = connectIdmRemoveRoleService.removeRole(request);
        checkError(idmResponse, ConnectError.NO_RESOURCE_ID);
    }

    @Test
    public void removeRole_subjectType_validation() {
        request.setSubjectType(null);
        IdmResponse idmResponse = connectIdmRemoveRoleService.removeRole(request);
        checkError(idmResponse, ConnectError.NO_SUBJECT_TYPE);
    }

    @Test
    public void removeRole_path_validation() {
        request.setPath("  ");
        IdmResponse idmResponse = connectIdmRemoveRoleService.removeRole(request);
        checkError(idmResponse, ConnectError.NO_PATH);
    }

    @Test
    public void removeRole_emptyPath_validation() {
        request.setPath(null);
        IdmResponse idmResponse = connectIdmRemoveRoleService.removeRole(request);
        checkError(idmResponse, ConnectError.NO_PATH);
    }

    @Test
    public void removeRole_id_validation() {
        request.setId(null);
        IdmResponse idmResponse = connectIdmRemoveRoleService.removeRole(request);
        checkError(idmResponse, ConnectError.NO_ID);
    }

    @Test
    public void removeRole_orgId_notOrgSubjectType_validation() {
        request.setOrgId(null);
        request.setSubjectType(SubjectType.USER);
        IdmResponse idmResponse = connectIdmRemoveRoleService.removeRole(request);
        checkError(idmResponse, ConnectError.NO_ORG_ID);
    }

    @Test
    public void removeRole_apply_path_validation() {
        request.setPath(NON_EXISTING_ROLE_PATH);
        IdmResponse idmResponse = connectIdmRemoveRoleService.removeRole(request);
        checkError(idmResponse, ConnectError.WRONG_PATH);
    }

    @Test
    public void removeRole_resourceNotFound() {
        RequestFields fields = new RequestFields();
        fields.setResourceId(100499L);
        request.setFields(fields);
        IdmResponse idmResponse = connectIdmRemoveRoleService.removeRole(request);
        checkError(idmResponse, ConnectError.RESOURCE_NOT_FOUND);
    }

    @Test
    public void removeRole_org_resourceNotAssociated() {
        request.setSubjectType(SubjectType.ORGANIZATION);
        request.setPath(ORG_ASSOCIATED_ROLE_PATH);
        Long connectOrgId = clientInfo.getClient().getConnectOrgId();
        request.setId(connectOrgId + 1L);
        IdmResponse idmResponse = connectIdmRemoveRoleService.removeRole(request);
        checkError(idmResponse, ConnectError.RESOURCE_NOT_ASSOCIATED);
    }

    @Test
    public void removeRole_user_resourceNotAssociated() {
        Long connectOrgId = clientInfo.getClient().getConnectOrgId();
        request.setOrgId(connectOrgId + 1L);
        IdmResponse idmResponse = connectIdmRemoveRoleService.removeRole(request);
        checkError(idmResponse, ConnectError.RESOURCE_NOT_ASSOCIATED);
    }

    @Test
    public void removeRole_user_userAssociatedToAnother() {
        UserInfo userInfo = steps.userSteps().createDefaultUser();
        request.setId(userInfo.getClientId().asLong());
        IdmResponse idmResponse = connectIdmRemoveRoleService.removeRole(request);
        checkError(idmResponse, ConnectError.USER_ASSOCIATED_TO_ANOTHER_RESOURCE);
    }

    @Test
    public void removeRole_user_userCannotBeChief() {
        request.setPath(USER_EMPLOYEE_ROLE_PATH);
        request.setId(clientInfo.getClient().getChiefUid());
        IdmResponse idmResponse = connectIdmRemoveRoleService.removeRole(request);
        checkError(idmResponse, ConnectError.USER_CANNOT_BE_CHIEF);
    }

    @Test
    public void removeRole_user_balanceUserAssociatedWithAnotherClient() {
        request.setPath(USER_EMPLOYEE_ROLE_PATH);
        request.setId(regularUser.getUid());

        IdmResponse idmResponse = fakeExecutionReturnsError(balanceUserAssociatedWithAnotherClient())
                .removeRole(request);
        checkError(idmResponse, ConnectError.USER_ASSOCIATED_TO_ANOTHER_BALANCE_CLIENT);
    }

    @Test
    public void removeRole_user_userHasActiveAutoPay() {
        request.setPath(USER_EMPLOYEE_ROLE_PATH);
        request.setId(regularUser.getUid());

        IdmResponse idmResponse = fakeExecutionReturnsError(userHasActiveAutoPay())
                .removeRole(request);
        checkError(idmResponse, ConnectError.USER_HAS_ACTIVE_AUTOPAY);
    }

    @Test
    public void removeRole_org_successful() {
        request.setSubjectType(SubjectType.ORGANIZATION);
        request.setPath(ORG_ASSOCIATED_ROLE_PATH);
        request.setOrgId(null);
        Long oldOrgId = clientInfo.getClient().getConnectOrgId();
        request.setId(oldOrgId);
        IdmResponse idmResponse = connectIdmRemoveRoleService.removeRole(request);
        assertThat(idmResponse).is(matchedBy(beanDiffer(new IdmSuccessResponse())));
        Client clientAfterRemovingRole = clientService.getClient(clientInfo.getClientId());
        checkState(clientAfterRemovingRole != null);
        assertThat(clientAfterRemovingRole.getConnectOrgId()).isNull();
    }

    @Test
    public void removeRole_userChief_successful() {
        request.setId(regularUser.getUid());
        IdmResponse idmResponse = connectIdmRemoveRoleService.removeRole(request);
        assertThat(idmResponse).is(matchedBy(beanDiffer(new IdmSuccessResponse())));
    }

    @Test
    public void removeRole_userEmployee_successful() {
        request.setPath(USER_EMPLOYEE_ROLE_PATH);
        request.setId(regularUser.getUid());
        IdmResponse idmResponse = connectIdmRemoveRoleService.removeRole(request);
        assertThat(idmResponse).is(matchedBy(beanDiffer(new IdmSuccessResponse())));
    }

    private ConnectIdmRemoveRoleService fakeExecutionReturnsError(Defect defect) {
        UserService mockedUserService = mock(UserService.class);
        when(mockedUserService.dropClientRep(any()))
                .thenReturn(Result.broken(ValidationResult.failed(regularUser.getUser(), defect)));
        return new ConnectIdmRemoveRoleService(clientService, mockedUserService);
    }


    private void checkError(IdmResponse idmResponse, ConnectError error) {
        assertThat(idmResponse.getCode()).isNotZero().as("Given error response");
        assertThat(idmResponse).is(matchedBy(beanDiffer(new IdmErrorResponse(error))));
    }

}
