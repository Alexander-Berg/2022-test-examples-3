package ru.yandex.direct.core.entity.user.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;
import org.apache.commons.lang3.RandomStringUtils;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.balance.client.BalanceClient;
import ru.yandex.direct.balance.client.model.response.FindClientResponseItem;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.result.Result;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.user.service.validation.UserDefects.balanceUserAssociatedWithAnotherClient;
import static ru.yandex.direct.core.entity.user.service.validation.UserDefects.chiefDeletionProhibited;
import static ru.yandex.direct.core.entity.user.service.validation.UserDefects.userHasActiveAutoPay;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeWalletCampaign;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class UserServiceTest {

    private static final int SHARD = 1;
    private static final int SHARD_ELSE = 2;

    @Autowired
    private Steps steps;

    @Autowired
    private UserService userService;

    @Autowired
    private BalanceClient balanceClient;

    private UserInfo user1;
    private ClientInfo client1;
    private Long uid1;
    private ClientId clientId1;
    private String login1;
    private UserInfo user2;
    private Long uid2;
    private ClientId clientId2;
    private String login2;


    @Before
    public void setUp() {
        client1 = new ClientInfo().withShard(SHARD);
        user1 = steps.clientSteps().createClient(client1).getChiefUserInfo();
        uid1 = user1.getUid();
        clientId1 = user1.getClientInfo().getClientId();
        login1 = user1.getUser().getLogin();
        user2 = steps.clientSteps().createClient(new ClientInfo().withShard(SHARD_ELSE)).getChiefUserInfo();
        uid2 = user2.getUid();
        clientId2 = user2.getClientInfo().getClientId();
        login2 = user2.getUser().getLogin();

        //Отвязываем mock'и, потенциально сделанные другими тестами
        Mockito.reset(balanceClient);
    }

    @Test
    public void testAddSubclient() {
        Map<ClientId, List<Long>> map = userService.massGetUidsByClientIds(Arrays.asList(clientId1, clientId2));
        Map<ClientId, List<Long>> expectedMap = new HashMap<>();
        expectedMap.put(clientId1, singletonList(uid1));
        expectedMap.put(clientId2, singletonList(uid2));
        assertThat(map, beanDiffer(expectedMap));
    }

    @Test
    public void test_getUserUidsWithoutHavingOnlyGeoOrMcbCampaigns_noneCampaigns() {
        Set<Long> actual = userService.getUserUidsWithoutHavingOnlyGeoOrMcbCampaigns(
                Sets.newHashSet(uid1, uid2));

        assertThat(
                actual, Matchers.equalTo(Sets.newHashSet(uid1, uid2)));
    }

    @Test
    public void test_getUserUidsWithoutHavingOnlyGeoOrMcbCampaigns_OnlyMcbOrGeoCampaigns() {
        steps.campaignSteps().createCampaign(TestCampaigns.newMcbCampaign(clientId2, uid2), user2.getClientInfo());

        Set<Long> actual = userService.getUserUidsWithoutHavingOnlyGeoOrMcbCampaigns(
                Sets.newHashSet(uid1, uid2));

        assertThat(
                actual, Matchers.equalTo(singleton(uid1)));
    }

    @Test
    public void test_getUserUidsWithoutHavingOnlyGeoOrMcbCampaigns_NotOnlyMcbOrGeoCampaigns() {
        steps.campaignSteps().createCampaign(TestCampaigns.newTextCampaign(clientId2, uid2), user2.getClientInfo());
        steps.campaignSteps().createCampaign(TestCampaigns.newMcbCampaign(clientId2, uid2), user2.getClientInfo());

        Set<Long> actual = userService.getUserUidsWithoutHavingOnlyGeoOrMcbCampaigns(
                Sets.newHashSet(uid1, uid2));

        assertThat(
                actual, Matchers.equalTo(Sets.newHashSet(uid1, uid2)));
    }

    @Test
    public void test_massUpdateHidden_EmptyList() {
        List<String> processedLogins = userService.massSetHidden(
                Collections.emptyList()
        );

        assertThat(processedLogins, Matchers.emptyCollectionOf(String.class));
    }

    @Test
    public void test_massUpdateHidden_ExistingLogin() {
        List<String> processedLogins = userService.massSetHidden(
                Arrays.asList(login1, login2)
        );

        assertThat(processedLogins, Matchers.equalTo(Arrays.asList(login1, login2)));
    }

    @Test
    public void test_massUpdateHidden_ExistingAndNotExistingLogin() {
        String nonExistingLogin = RandomStringUtils.randomAlphanumeric(5);
        List<String> processedLogins = userService.massSetHidden(
                Arrays.asList(login1, login2, nonExistingLogin)
        );

        assertThat(processedLogins, Matchers.equalTo(Arrays.asList(login1, login2)));
    }

    @Test
    public void getChiefsLoginsByClientIdsTest() {
        Map<ClientId, String> map = userService.getChiefsLoginsByClientIds(Arrays.asList(clientId1, clientId2));
        Map<ClientId, String> expectedMap = new HashMap<>();
        expectedMap.put(clientId1, login1);
        expectedMap.put(clientId2, login2);
        assertThat(map, beanDiffer(expectedMap));
    }

    @Test
    public void getChiefUserByClientIdMapTest() {
        var map = userService.getChiefUserByClientIdMap(List.of(clientId1, clientId2));
        assertThat("first client chief login is right", map.get(clientId1).getLogin(), is(login1));
        assertThat("second client chief login is right", map.get(clientId2).getLogin(), is(login2));
    }

    @Test
    public void massGetUidByLoginTest_userExists_getUid() {
        Map<String, Long> uidsByLogin = userService.massGetUidByLogin(singletonList(user1.getClientInfo().getLogin()));
        assertThat(uidsByLogin.get(user1.getClientInfo().getLogin()), is(uid1));
    }

    @Test
    public void massGetUidByLoginTest_userDoesNotExist_getNull() {
        String nonExistentLogin = "nonexistent-login";
        Map<String, Long> uidsByLogin = userService.massGetUidByLogin(singletonList(nonExistentLogin));
        assertThat(uidsByLogin.get(nonExistentLogin), nullValue());
    }

    @Test
    public void testDropClientRep_successful() {
        User regularUser = getRegularUser();

        dropAndCheck(regularUser);
    }

    @Test
    public void testDropClientRepWithDisabledAutoPay_successful() {
        User regularUser = getRegularUser();
        createAutoPay(regularUser, false);
        dropAndCheck(regularUser);
    }

    @Test
    public void testDropClientRep_balanceUserAssociatedWithAnotherClient() {
        User regularUser = getRegularUser();
        Long anotherClienId = regularUser.getClientId().asLong() + 1;

        when(balanceClient.findClient(any()))
                .thenReturn(singletonList(new FindClientResponseItem().withClientId(anotherClienId)));

        tryDropUserAndCheckError(regularUser, balanceUserAssociatedWithAnotherClient());
    }

    @Test
    public void testDropClientRep_userHasActiveAutoPay() {
        var user = getRegularUser();
        createAutoPay(user, true);

        tryDropUserAndCheckError(user, userHasActiveAutoPay());
    }

    @Test
    public void testDropClientRep_nullUser_successful() {
        Result<User> result = userService.dropClientRep(null);
        assertThat(result.isSuccessful(), Matchers.is(true));
    }

    @Test
    public void testDropClientRep_chiefDeletionProhibited() {
        tryDropUserAndCheckError(user1.getUser(), chiefDeletionProhibited());
    }

    private User getRegularUser() {
        return steps.userSteps().createRepresentative(client1).getUser();
    }

    private void tryDropUserAndCheckError(User user, Defect expectedError) {
        Result<User> result = userService.dropClientRep(user);
        var expectedResult = Result.broken(ValidationResult.failed(user, expectedError));

        assertThat("Given expected error", result, beanDiffer(expectedResult));
    }

    private void dropAndCheck(User regularUser) {
        User dbUser = userService.getUser(regularUser.getUid());
        assertThat("user exists", dbUser, notNullValue());

        Result<User> result = userService.dropClientRep(regularUser);
        assertThat(result.isSuccessful(), Matchers.is(true));

        dbUser = userService.getUser(regularUser.getUid());
        assertThat("user deleted successfully", dbUser, nullValue());
    }

    private void createAutoPay(User user, Boolean isAutoPayEnabled) {
        CampaignInfo walletInfo = steps.campaignSteps()
                .createCampaign(new CampaignInfo()
                        .withCampaign(activeWalletCampaign(user.getClientId(), user.getUid()))
                        .withClientInfo(client1)
                );
        steps.campaignSteps().addFakeAutoPay(walletInfo, user, isAutoPayEnabled);
    }
}
