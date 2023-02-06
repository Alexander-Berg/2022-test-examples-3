package ru.yandex.direct.core.entity.user.repository;

import javax.annotation.ParametersAreNonnullByDefault;

import org.assertj.core.api.SoftAssertions;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.PassportClientStub;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.i18n.Language;
import ru.yandex.direct.rbac.RbacRepType;
import ru.yandex.direct.rbac.RbacRole;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class UserRepositoryAddUsersTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private Steps steps;
    @Autowired
    private PassportClientStub passportClientStub;

    @Test
    public void addUsers_whenUserIsClientNotChiefRepresentative_success() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClientWithRole(RbacRole.CLIENT);
        Long uid = passportClientStub.generateNewUserUid();
        String login = passportClientStub.getLoginByUid(uid);
        ClientId clientId = clientInfo.getClientId();
        Integer shard = clientInfo.getShard();

        User newUser = getUser(clientInfo, uid, login);
        userRepository.addUsers(shard, singletonList(newUser));

        User actualUser = userRepository.fetchByUids(shard, singleton(uid)).get(0);
        User expectedUser = getUser(clientInfo, uid, login);
        Long shardLoginUid = userRepository.getShardLoginIds(singleton(login)).get(0)
                .getUid();
        ClientId shardUidClientId = userRepository.getShardUidIds(singleton(uid)).get(0)
                .getClientId();
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(actualUser).as("actual user")
                    .is(matchedBy(beanDiffer(expectedUser).useCompareStrategy(onlyExpectedFields())));
            soft.assertThat(shardLoginUid).as("ppcdict.shard_login").isEqualTo(uid);
            soft.assertThat(shardUidClientId).as("ppcdict.shard_uid").isEqualTo(clientId);
        });
    }

    private User getUser(ClientInfo clientInfo, Long uid, String login) {
        return new User()
                .withUid(uid)
                .withLogin(login)
                .withEmail(login + "@yandex.ru")
                .withFio("Василий Пупкин")
                .withRole(RbacRole.CLIENT)
                .withRepType(RbacRepType.MAIN)
                .withClientId(clientInfo.getClientId())
                .withChiefUid(clientInfo.getUid())
                .withLang(Language.EN);
    }

    @Test
    public void addUsers_withCanManagePricePackages_true() {
        testCanManagePricePackages(Boolean.TRUE);
    }

    @Test
    public void addUsers_withCanManagePricePackages_false() {
        testCanManagePricePackages(Boolean.FALSE);
    }

    private void testCanManagePricePackages(Boolean canManagePricePackages) {
        var userInfo =
                steps.userSteps().createUser(generateNewUser().withCanManagePricePackages(canManagePricePackages));
        User actualUser = userRepository.fetchByUids(userInfo.getClientInfo().getShard(), singleton(userInfo.getUid()))
                .get(0);
        Assert.assertThat(actualUser.getCanManagePricePackages(), is(equalTo(canManagePricePackages)));
    }


    @Test
    public void addUsers_withCanApprovePricePackages_true() {
        testCanApprovePricePackages(Boolean.TRUE);
    }

    @Test
    public void addUsers_withCanApprovePricePackages_false() {
        testCanApprovePricePackages(Boolean.FALSE);
    }

    private void testCanApprovePricePackages(Boolean canApprovePricePackages) {
        var userInfo =
                steps.userSteps().createUser(generateNewUser().withCanApprovePricePackages(canApprovePricePackages));
        User actualUser = userRepository.fetchByUids(userInfo.getClientInfo().getShard(), singleton(userInfo.getUid()))
                .get(0);
        Assert.assertThat(actualUser.getCanApprovePricePackages(), is(equalTo(canApprovePricePackages)));
    }

}
