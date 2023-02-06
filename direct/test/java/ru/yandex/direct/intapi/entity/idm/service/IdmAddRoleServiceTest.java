package ru.yandex.direct.intapi.entity.idm.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.direct.blackbox.client.BlackboxClient;
import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.agency.service.AgencyService;
import ru.yandex.direct.core.entity.campaign.service.WalletService;
import ru.yandex.direct.core.entity.client.service.AddClientService;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.client.service.validation.AddClientValidationService;
import ru.yandex.direct.core.entity.currency.service.CurrencyDictCache;
import ru.yandex.direct.core.entity.currency.service.CurrencyService;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.geo.repository.FakeGeoRegionRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.repository.UserRepository;
import ru.yandex.direct.core.entity.user.service.BlackboxUserService;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.entity.user.service.validation.UserValidationService;
import ru.yandex.direct.core.service.integration.balance.BalanceService;
import ru.yandex.direct.core.service.integration.balance.RegisterClientResult;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.model.UidAndClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.env.EnvironmentType;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.idm.model.AddRoleRequest;
import ru.yandex.direct.intapi.entity.idm.model.IdmFatalResponse;
import ru.yandex.direct.intapi.entity.idm.model.IdmResponse;
import ru.yandex.direct.intapi.entity.idm.model.IdmRole;
import ru.yandex.direct.rbac.PpcRbac;
import ru.yandex.direct.rbac.RbacRepType;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.rbac.RbacSubrole;
import ru.yandex.direct.regions.GeoTreeFactory;
import ru.yandex.direct.staff.client.StaffClient;
import ru.yandex.direct.staff.client.model.json.Name;
import ru.yandex.direct.staff.client.model.json.PersonInfo;
import ru.yandex.direct.staff.client.model.json.RuEnValue;
import ru.yandex.direct.tvm.TvmIntegration;
import ru.yandex.inside.passport.PassportDomain;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.inside.passport.blackbox2.protocol.request.params.AliasesParameterValue;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxAliases;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxOAuthStatus;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxResponseBuilder;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.utils.CommonUtils.ifNotNull;
import static ru.yandex.inside.passport.blackbox2.protocol.BlackboxMethod.USER_INFO;

@IntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class IdmAddRoleServiceTest {

    @Autowired
    private Steps steps;

    @Autowired
    private ShardHelper shardHelper;

    @Autowired
    private UserService userService;

    @Autowired
    private ClientService clientService;

    @Autowired
    private GeoTreeFactory geoTreeFactory;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PpcRbac ppcRbac;

    @Autowired
    private WalletService walletService;

    @Autowired
    private UserValidationService userValidationService;

    @Autowired
    private TvmIntegration tvmIntegration;
    @Autowired
    private BlackboxClient blackboxClient;

    @Autowired
    private CurrencyDictCache currencyDictCache;
    @Autowired
    private AgencyService agencyService;
    @Autowired
    private FeatureService featureService;
    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;

    private BalanceService balanceService;
    private StaffClient staffClient;
    private IdmAddRoleService idmAddRoleService;

    private User user;

    @Before
    public void setUp() {
        balanceService = mock(BalanceService.class);
        staffClient = mock(StaffClient.class);

        final var geoRegionRepository = new FakeGeoRegionRepository();
        final var currencyService = new CurrencyService(currencyDictCache, agencyService,
                balanceService, featureService, ppcPropertiesSupport, geoRegionRepository);
        final var addClientValidationService = new AddClientValidationService(currencyService);

        var blackboxUserService = new BlackboxUserService(blackboxClient, EnvironmentType.DEVELOPMENT, tvmIntegration);
        AddClientService addClientService = new AddClientService(balanceService, clientService, walletService,
                geoTreeFactory, addClientValidationService, blackboxUserService);
        idmAddRoleService = new IdmAddRoleService(userService, ppcRbac, addClientService,
                clientService, userRepository, shardHelper, staffClient, userValidationService,
                blackboxClient, balanceService, tvmIntegration, EnvironmentType.DEVELOPMENT);

        user = generateUser();
        mockInstancesForTest(user.getClientId(), user.getLogin(), user.getUid());
        mockStaffClientForTest(user);
    }

    @Test
    public void addRoleForNewClient_success_whenRoleIsManager() {
        addRoleAndCheckCheckResult(IdmRole.MANAGER, RbacRole.MANAGER);
    }

    @Test
    public void addRoleForNewClient_success_whenRoleIsSuper() {
        addRoleAndCheckCheckResult(IdmRole.SUPER, RbacRole.SUPER);
    }

    @Test
    public void addRoleForNewClient_success_whenRoleIsSuperreader() {
        addRoleAndCheckCheckResult(IdmRole.SUPERREADER, RbacRole.SUPERREADER);
    }

    @Test
    public void addRoleForNewClient_success_whenRoleIsSupport() {
        addRoleAndCheckCheckResult(IdmRole.SUPPORT, RbacRole.SUPPORT);
    }

    @Test
    public void addRoleForNewClient_success_whenRoleIsPlacer() {
        addRoleAndCheckCheckResult(IdmRole.PLACER, RbacRole.PLACER);
    }

    @Test
    public void addRoleForNewClient_success_whenRoleIsMedia() {
        addRoleAndCheckCheckResult(IdmRole.MEDIA, RbacRole.MEDIA);
    }

    @Test
    public void addRoleForNewClient_success_whenRoleIsInternalAdAdmin() {
        addRoleAndCheckCheckResult(IdmRole.INTERNAL_AD_ADMIN, RbacRole.INTERNAL_AD_ADMIN);
    }

    @Test
    public void addRoleForNewClient_success_whenRoleIsInternalAdManager() {
        addRoleAndCheckCheckResult(IdmRole.INTERNAL_AD_MANAGER, RbacRole.INTERNAL_AD_MANAGER);
    }

    @Test
    public void addRoleForNewClient_success_whenRoleIsInternalAdSuperreader() {
        addRoleAndCheckCheckResult(IdmRole.INTERNAL_AD_SUPERREADER, RbacRole.INTERNAL_AD_SUPERREADER);
    }

    @Test
    public void addRoleForNewClient_success_whenRoleIsLimitedSupport() {
        addRoleAndCheckCheckResult(IdmRole.LIMITED_SUPPORT, RbacRole.LIMITED_SUPPORT);
    }

    @Test
    public void addRoleForNewClient_success_whenRoleIsDeveloper() {
        addRoleAndCheckCheckResult(IdmRole.DEVELOPER, RbacRole.SUPERREADER);
        checkUserIsDeveloper(user.getUid());
    }

    @Test
    public void addRoleForNewClient_failure_whenRoleIsTeamleader() {
        String errorText = "логин " + user.getLogin() + " не имеет роли менеджера (текущая роль: empty)";
        addRoleExpectError(IdmRole.TEAMLEADER, user, errorText);
        checkUserWithUidDoesNotExist(user.getUid());
    }

    @Test
    public void addRoleForNewClient_failure_whenRoleIsSuperteamleader() {
        String errorText = "логин " + user.getLogin() + " не имеет роли менеджера (текущая роль: empty)";
        addRoleExpectError(IdmRole.SUPERTEAMLEADER, user, errorText);
        checkUserWithUidDoesNotExist(user.getUid());
    }

    @Test
    public void addRoleForExistingClient_success_whenRoleIsTeamleader() {
        addRole(IdmRole.MANAGER, user, true);
        addRole(IdmRole.TEAMLEADER, user, true);
        checkUserAndRole(user, RbacRole.MANAGER, RbacSubrole.TEAMLEADER);
    }

    @Test
    public void addRoleForExistingClient_success_whenRoleIsSuperteamleader() {
        addRole(IdmRole.MANAGER, user, true);
        addRole(IdmRole.SUPERTEAMLEADER, user, true);
        checkUserAndRole(user, RbacRole.MANAGER, RbacSubrole.SUPERTEAMLEADER);
    }

    @Test
    public void addRoleForExistingClient_success_whenRoleIsManagerAndOldRoleIsTeamleader() {
        addRole(IdmRole.MANAGER, user, true);
        addRole(IdmRole.TEAMLEADER, user, true);
        addRole(IdmRole.MANAGER, user, true);
        checkUserAndRole(user, RbacRole.MANAGER, RbacSubrole.TEAMLEADER);
    }

    @Test
    public void addRoleForExistingClient_success_whenRoleIsManagerAndOldRoleIsSuperteamleader() {
        addRole(IdmRole.MANAGER, user, true);
        addRole(IdmRole.SUPERTEAMLEADER, user, true);
        addRole(IdmRole.MANAGER, user, true);
        checkUserAndRole(user, RbacRole.MANAGER, RbacSubrole.SUPERTEAMLEADER);
    }

    @Test
    public void addRoleForExistingClient_failure_whenRoleIsTeamleader_andOldRoleIsNotManager() {
        addRole(IdmRole.PLACER, user, true);
        addRole(IdmRole.TEAMLEADER, user, false);
        // проверяем что после второго запроса роль в БД не поменялась
        checkUserAndRole(user, RbacRole.PLACER, null);
    }

    @Test
    public void addRoleForExistingClient_failure_whenRoleChanges() {
        addRole(IdmRole.MANAGER, user, true);
        String errorText = "логин " + user.getLogin() + " уже имеет роль: manager";
        addRoleExpectError(IdmRole.SUPER, user, errorText);
        checkUserAndRole(user, RbacRole.MANAGER, null);
    }

    @Test
    public void addRoleForExistingClient_failure_whenRoleChangesFromPlacerToManager() {
        // отдельный тест для этого случая, потому что была ошибка именно с этим сценарием
        addRole(IdmRole.PLACER, user, true);
        addRole(IdmRole.MANAGER, user, false);
        checkUserAndRole(user, RbacRole.PLACER, null);
    }

    @Test
    public void addRoleForExistingClient_success_whenRoleDoesNotChange() {
        addRole(IdmRole.MANAGER, user, true);
        addRole(IdmRole.MANAGER, user, true);
        checkUserAndRole(user, RbacRole.MANAGER, null);
    }

    @Test
    public void addRoleForNewClient_failure_whenStaffUserNotFound() {
        when(staffClient.getStaffUserInfos(any())).thenReturn(emptyMap());
        addRole(IdmRole.SUPPORT, user, false);
        checkUserWithUidDoesNotExist(user.getUid());
    }

    @Test
    public void addRoleForNewClient_failure_whenStaffUserEmailIsNull() {
        mockStaffClientForTest(user.withEmail(null));
        addRole(IdmRole.SUPPORT, user, false);
        checkUserWithUidDoesNotExist(user.getUid());
    }

    @Test
    public void addRoleForNewClient_failure_whenPassportLoginIsNull() {
        addRole(IdmRole.SUPPORT, user.withLogin(null), false);
        checkUserWithUidDoesNotExist(user.getUid());
    }

    @Test
    public void addRoleForNewClient_failure_whenDomainLoginIsNull() {
        addRole(IdmRole.SUPPORT, user.withDomainLogin(null), false);
        checkUserWithUidDoesNotExist(user.getUid());
    }

    @Test
    public void addRoleForExistingClient_success_whenUserIsBlocked() {
        addRole(IdmRole.SUPPORT, user, true);
        blockUser(user.getClientId(), user.getUid());
        addRole(IdmRole.SUPPORT, user, true);

        User expectedUser = new User().withStatusBlocked(false).withRole(RbacRole.SUPPORT);
        checkUser(user.getUid(), expectedUser);
    }

    @Test
    public void addRoleForExistingClient_failure_whenUserIsBlocked_andDomainLoginIsDifferent() {
        addRole(IdmRole.SUPPORT, user, true);
        blockUser(user.getClientId(), user.getUid());

        mockStaffClientForTest(user.withDomainLogin("test-domain-login-2"));
        addRole(IdmRole.SUPPORT, user, false);

        User expectedUser = new User().withStatusBlocked(true).withRole(RbacRole.SUPPORT);
        checkUser(user.getUid(), expectedUser);
    }

    private void addRoleAndCheckCheckResult(IdmRole role, RbacRole expectedRole) {
        addRole(role, user, true);
        checkUserAndRole(user, expectedRole, null);
    }

    private IdmResponse addRole(IdmRole role, User user, boolean isSuccessful) {
        AddRoleRequest request = new AddRoleRequest()
                .withRole(role.getTypedValue())
                .withPassportLogin(user.getLogin())
                .withDomainLogin(user.getDomainLogin());

        IdmResponse response = idmAddRoleService.addRole(request);
        assumeThat(response.getCode(), is(isSuccessful ? 0 : 1));

        return response;
    }

    private void addRoleExpectError(IdmRole role, User user, String errorText) {
        IdmResponse response = addRole(role, user, false);
        IdmResponse expectedResponse = new IdmFatalResponse(errorText);
        assertThat(response, beanDiffer(expectedResponse));
    }

    private void checkUserAndRole(User user, RbacRole expectedRole, RbacSubrole expectedSubrole) {
        User expectedUser = new User()
                .withUid(user.getUid())
                .withClientId(user.getClientId())
                .withRepType(RbacRepType.CHIEF)
                .withLogin(user.getLogin())
                .withDomainLogin(user.getDomainLogin())
                .withFio(user.getFio())
                .withEmail(user.getEmail())
                .withPhone(user.getPhone())
                .withRole(expectedRole)
                .withSubRole(expectedSubrole);
        checkUser(user.getUid(), expectedUser);

        List<Long> managerUids = ppcRbac.getAllManagers();
        assertThat(managerUids.contains(user.getUid()), is(RbacRole.MANAGER.equals(expectedRole)));
    }

    private void checkUser(Long uid, User expectedUser) {
        User actualUser = userService.getUser(uid);
        assertThat(actualUser, beanDiffer(expectedUser).useCompareStrategy(onlyExpectedFields()));
    }

    private void checkUserIsDeveloper(Long uid) {
        User actualUser = userService.getUser(uid);
        assertTrue("пользователь - разработчик", actualUser.getDeveloper());
    }

    private void checkUserWithUidDoesNotExist(Long uid) {
        User actualUser = userService.getUser(uid);
        assertThat(actualUser, nullValue());
    }

    private void blockUser(ClientId clientId, Long userId) {
        userService.blockUser(clientId, userId);
    }

    private void mockInstancesForTest(ClientId clientId, String login, Long uid) {
        when(balanceService.registerNewClient(any(), any(), any(), anyBoolean()))
                .thenReturn(RegisterClientResult.success(clientId));

        var response = new BlackboxResponseBuilder(USER_INFO)
                .setStatus(BlackboxOAuthStatus.VALID.value())
                .setLogin(Optional.of(login))
                .setAliases(Map.of(BlackboxAliases.PORTAL, login))
                .setUidDomain(Optional.of(Tuple2.tuple(PassportUid.cons(uid), PassportDomain.YANDEX_RU)))
                .build();
        when(blackboxClient.userInfo(any(), anyString(), anyList(), anyString())).thenReturn(response);
        when(blackboxClient.userInfo(any(), anyString(), any(AliasesParameterValue.class), anyString()))
                .thenReturn(response);
    }

    private void mockStaffClientForTest(User user) {
        PersonInfo personInfo = getPersonInfo(user);
        when(staffClient.getStaffUserInfos(any()))
                .thenReturn(singletonMap(personInfo.getLogin(), personInfo));
    }

    private User generateUser() {
        UidAndClientId uidAndClientId = steps.clientSteps().generateNewUidAndClientId();
        return generateNewUser()
                .withUid(uidAndClientId.getUid())
                .withLogin(steps.userSteps().getUserLogin(uidAndClientId.getUid()))
                .withClientId(uidAndClientId.getClientId())
                .withDomainLogin("test-domain-login");
    }

    static PersonInfo getPersonInfo(User user) {
        String[] fio = user.getFio().split(" ");

        RuEnValue lastName = new RuEnValue();
        lastName.setRu(fio[0]);
        RuEnValue firstName = new RuEnValue();
        firstName.setRu(fio[1]);

        Name name = new Name();
        name.setFirst(firstName);
        name.setLast(lastName);

        PersonInfo personInfo = new PersonInfo();
        personInfo.setName(name);
        personInfo.setLogin(user.getDomainLogin());
        personInfo.setWorkEmail(user.getEmail());
        personInfo.setWorkPhone(ifNotNull(user.getPhone(), Integer::valueOf));

        return personInfo;
    }
}
