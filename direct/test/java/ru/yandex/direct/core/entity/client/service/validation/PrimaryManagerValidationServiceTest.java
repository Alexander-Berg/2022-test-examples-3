package ru.yandex.direct.core.entity.client.service.validation;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.client.model.ClientPrimaryManager;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.service.validation.UserDefectIds;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.ClientPrimaryManagerInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.DefectIds;
import ru.yandex.direct.validation.result.DefectInfo;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

import static com.google.common.base.Preconditions.checkState;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoErrors;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.utils.CommonUtils.notEquals;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;


@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class PrimaryManagerValidationServiceTest {

    @Autowired
    PrimaryManagerValidationService validationService;
    @Autowired
    private Steps steps;

    private ClientId subjectClientId;
    private UserInfo newManagerInfo;
    private Long newManagerUid;


    @Before
    public void setUp() throws Exception {
        ClientPrimaryManagerInfo startPrimaryManagerInfo = steps.idmGroupSteps().createIdmPrimaryManager();
        subjectClientId = startPrimaryManagerInfo.getSubjectClientId();
        newManagerInfo =
                steps.clientSteps().createDefaultClientWithRoleInAnotherShard(RbacRole.MANAGER).getChiefUserInfo();
        steps.userSteps().mockUserInExternalClients(newManagerInfo.getUser());
        newManagerUid = newManagerInfo.getUid();
    }

    @Test
    public void updateValidation_success() {
        ValidationResult<ClientPrimaryManager, Defect> validationResult =
                validationService.updateValidation(
                        new ClientPrimaryManager()
                                .withSubjectClientId(subjectClientId)
                                .withPrimaryManagerUid(newManagerUid)
                                .withIsIdmPrimaryManager(true));

        assertThat(validationResult).is(matchedBy(hasNoErrors()));
    }

    @Test
    public void updateValidation_whenSubjectClientIdIsNull_failure() {
        ValidationResult<ClientPrimaryManager, Defect> validationResult =
                validationService.updateValidation(
                        new ClientPrimaryManager()
                                .withSubjectClientId(null)
                                .withPrimaryManagerUid(newManagerUid)
                                .withIsIdmPrimaryManager(true));

        Path errorPath = path(field(ClientPrimaryManager.SUBJECT_CLIENT_ID.name()));
        assertThat(validationResult)
                .is(matchedBy(hasDefectWithDefinition(validationError(errorPath, DefectIds.CANNOT_BE_NULL))));
    }

    @Test
    public void updateValidation_whenClientIsNotClientOrAgency_failure() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClientWithRole(RbacRole.MEDIA);
        ValidationResult<ClientPrimaryManager, Defect> validationResult =
                validationService.updateValidation(
                        new ClientPrimaryManager()
                                .withSubjectClientId(clientInfo.getClientId())
                                .withPrimaryManagerUid(newManagerUid)
                                .withIsIdmPrimaryManager(true));

        Path errorPath = path(field(ClientPrimaryManager.SUBJECT_CLIENT_ID.name()));
        Matcher<DefectInfo<Defect>> expectedError = validationError(errorPath, ClientDefects.Gen.NOT_APPROPRIATE_ROLE);

        assertThat(validationResult)
                .is(matchedBy(hasDefectWithDefinition(expectedError)));
    }

    @Test
    public void updateValidation_whenUidIsNull_success() {
        ValidationResult<ClientPrimaryManager, Defect> validationResult =
                validationService.updateValidation(
                        new ClientPrimaryManager()
                                .withSubjectClientId(subjectClientId)
                                .withPrimaryManagerUid(null)
                                .withIsIdmPrimaryManager(true));

        assertThat(validationResult).is(matchedBy(hasNoErrors()));
    }

    @Test
    public void updateValidation_whenUserNotExist_failure() {
        Long notExistUid = steps.userSteps().createUserInBlackboxStub().getUid();
        newManagerUid = notExistUid;
        ValidationResult<ClientPrimaryManager, Defect> validationResult =
                validationService.updateValidation(
                        new ClientPrimaryManager()
                                .withSubjectClientId(subjectClientId)
                                .withPrimaryManagerUid(notExistUid)
                                .withIsIdmPrimaryManager(true));

        Path errorPath = path(field(ClientPrimaryManager.PRIMARY_MANAGER_UID.name()));
        assertThat(validationResult)
                .is(matchedBy(hasDefectWithDefinition(
                        validationError(errorPath, UserDefectIds.Gen.USER_NOT_FOUND)
                )));
    }

    @Test
    public void updateValidation_whenUserNotManager_failure() {
        UserInfo userInfo = steps.userSteps().createDefaultUser();
        checkState(notEquals(userInfo.getUser().getRole(), RbacRole.MANAGER));
        Long uid = userInfo.getUid();
        newManagerUid = uid;
        ValidationResult<ClientPrimaryManager, Defect> validationResult =
                validationService.updateValidation(
                        new ClientPrimaryManager()
                                .withSubjectClientId(subjectClientId)
                                .withPrimaryManagerUid(uid)
                                .withIsIdmPrimaryManager(true));

        Path errorPath = path(field(ClientPrimaryManager.PRIMARY_MANAGER_UID.name()));
        assertThat(validationResult)
                .is(matchedBy(hasDefectWithDefinition(
                        validationError(errorPath, UserDefectIds.Gen.USER_MUST_BE_MANAGER)
                )));
    }

    @Test
    public void updateValidation_whenUserUnknownInBalanceAsManager_failure() {
        User user = newManagerInfo.getUser();
        user.withRole(RbacRole.SUPPORT);
        steps.userSteps().mockUserInExternalClients(user);

        ValidationResult<ClientPrimaryManager, Defect> validationResult =
                validationService.updateValidation(
                        new ClientPrimaryManager()
                                .withSubjectClientId(subjectClientId)
                                .withPrimaryManagerUid(newManagerUid)
                                .withIsIdmPrimaryManager(true));

        Path errorPath = path(field(ClientPrimaryManager.PRIMARY_MANAGER_UID.name()));
        assertThat(validationResult)
                .is(matchedBy(hasDefectWithDefinition(
                        validationError(errorPath, UserDefectIds.Gen.USER_MUST_BE_MANAGER_IN_BALANCE)
                )));
    }

}
