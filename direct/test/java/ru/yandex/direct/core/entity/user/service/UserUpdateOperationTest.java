package ru.yandex.direct.core.entity.user.service;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.repository.UserRepository;
import ru.yandex.direct.core.entity.user.service.validation.UpdateUserValidationService;
import ru.yandex.direct.core.entity.user.service.validation.UserValidationService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.result.Result;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class UserUpdateOperationTest {

    private static final String NEW_PHONE = randomNumeric(5);
    private static final String NEW_FIO = randomAlphanumeric(5);
    private static final int SHARD = 1;
    private static final int SHARD_ELSE = 2;
    private ClientInfo agencyClientInfo;
    private UserInfo userInfo1;
    private UserInfo userInfo2;

    @Autowired
    private Steps steps;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserValidationService userValidationService;

    @Autowired
    private UserService userService;

    @Autowired
    private ShardHelper shardHelper;

    @Autowired
    private UpdateUserValidationService updateUserValidationService;

    @Before
    public void before() {
        agencyClientInfo = steps.clientSteps().createDefaultClientWithRole(RbacRole.AGENCY);
        userInfo1 = steps.clientSteps().createClientUnderAgency(agencyClientInfo,
                new ClientInfo().withShard(SHARD)).getChiefUserInfo();
        userInfo2 = steps.clientSteps().createClientUnderAgency(agencyClientInfo,
                new ClientInfo().withShard(SHARD_ELSE)).getChiefUserInfo();
    }

    // возвращаемый результат при обновлении одного пользователя

    @Test
    public void prepareAndApply_OneValidItem_ResultIsExpected() {
        ModelChanges<User> modelChanges = modelChangesWithValidPhone(userInfo1.getUid());
        updateAndAssertResult(Applicability.PARTIAL, modelChanges, true);
    }

    @Test
    public void prepareAndApply_OneItemWithFailedValidation_ResultHasItemError() {
        ModelChanges<User> modelChanges = modelChangesWithInvalidPhone(userInfo1.getUid());
        updateAndAssertResult(Applicability.PARTIAL, modelChanges, false);
    }

    // возвращаемый результат при обновлении двух пользователей

    @Test
    public void prepareAndApply_PartialYes_TwoValidItems_ResultIsExpected() {
        checkUpdateResultOfTwoItems(Applicability.PARTIAL,
                modelChangesWithValidPhone(userInfo1.getUid()),
                modelChangesWithValidFIO(userInfo2.getUid()), true, true);
    }

    @Test
    public void prepareAndApply_PartialYes_OneValidAndOneInvalidItemOnValidation_ResultIsExpected() {
        checkUpdateResultOfTwoItems(Applicability.PARTIAL,
                modelChangesWithValidFIO(userInfo1.getUid()),
                modelChangesWithInvalidFIO(userInfo2.getUid()),
                true, false);
    }

    @Test
    public void prepareAndApply_PartialYes_TwoInvalidItemsOnValidation_ResultIsExpected() {
        checkUpdateResultOfTwoItems(Applicability.PARTIAL,
                modelChangesWithInvalidFIO(userInfo1.getUid()),
                modelChangesWithInvalidFIO(userInfo2.getUid()),
                false, false);
    }

    @Test
    public void prepareAndApply_PartialNo_TwoValidItems_ResultIsExpected() {
        checkUpdateResultOfTwoItems(Applicability.FULL,
                modelChangesWithValidPhone(userInfo1.getUid()),
                modelChangesWithValidFIO(userInfo2.getUid()),
                true, true);
    }

    @Test
    public void prepareAndApply_PartialNo_OneValidAndOneInvalidItemOnValidation_ResultIsExpected() {
        checkUpdateResultOfTwoItems(Applicability.FULL,
                modelChangesWithValidFIO(userInfo1.getUid()),
                modelChangesWithInvalidFIO(userInfo2.getUid()),
                true, false);
    }

    @Test
    public void prepareAndApply_PartialNo_TwoInvalidItemsOnValidation_ResultIsExpected() {
        checkUpdateResultOfTwoItems(Applicability.FULL,
                modelChangesWithInvalidFIO(userInfo1.getUid()),
                modelChangesWithInvalidFIO(userInfo2.getUid()),
                false, false);
    }

    @Test
    public void prepareAndApply_PartialNo_OneValidAndOneInvalidItem_ResultIsExpected() {
        checkUpdateResultOfTwoItems(Applicability.FULL,
                new ModelChanges<>(-1L, User.class),
                modelChangesWithValidPhone(userInfo2.getUid()),
                false, true);
    }

    private void checkUpdateResultOfTwoItems(Applicability applicability,
                                             ModelChanges<User> modelChanges1, ModelChanges<User> modelChanges2,
                                             boolean modelChanges1Valid, boolean modelChanges2Valid) {
        List<ModelChanges<User>> modelChangesList = asList(modelChanges1, modelChanges2);

        UserUpdateOperation updateOperation = createUpdateOperation(applicability, modelChangesList);
        MassResult<Long> result = updateOperation.prepareAndApply();

        assertThat("результат операции должен быть положительным", result.isSuccessful(), is(true));
        assertThat("успешность поэлементных результатов не соответствует ожидаемой",
                mapList(result.getResult(), Result::isSuccessful),
                contains(modelChanges1Valid, modelChanges2Valid));
    }

    private void updateAndAssertResult(Applicability applicability, ModelChanges<User> modelChanges,
                                       boolean itemResult) {
        UserUpdateOperation updateOperation = createUpdateOperation(applicability, singletonList(modelChanges));
        MassResult<Long> result = updateOperation.prepareAndApply();

        assertThat("результат операции должен быть положительный", result.isSuccessful(), is(true));
        assertThat("результат обновления элемента не соответствует ожидаемому",
                result.getResult().get(0).isSuccessful(), is(itemResult));
    }

    private UserUpdateOperation createUpdateOperation(Applicability applicability,
                                                      List<ModelChanges<User>> modelChangesList) {
        return new UserUpdateOperation(applicability, userRepository, updateUserValidationService,
                userValidationService,
                userService, shardHelper, modelChangesList, agencyClientInfo.getUid());
    }

    private ModelChanges<User> modelChangesWithValidPhone(long uid) {
        return ModelChanges.build(uid, User.class, User.PHONE, NEW_PHONE);
    }

    private ModelChanges<User> modelChangesWithInvalidPhone(long uid) {
        return ModelChanges.build(uid, User.class, User.PHONE, "");
    }

    private ModelChanges<User> modelChangesWithValidFIO(long uid) {
        return ModelChanges.build(uid, User.class, User.FIO, NEW_FIO);
    }

    private ModelChanges<User> modelChangesWithInvalidFIO(long uid) {
        return ModelChanges.build(uid, User.class, User.FIO, "");
    }
}
