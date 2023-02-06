package ru.yandex.direct.intapi.entity.idm.service;

import java.util.Map;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.direct.blackbox.client.BlackboxClient;
import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.agency.service.AgencyService;
import ru.yandex.direct.core.entity.campaign.service.WalletService;
import ru.yandex.direct.core.entity.client.service.AddClientService;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.client.service.ManagerHierarchyService;
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
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.model.UidAndClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.env.EnvironmentType;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.idm.model.AddRoleRequest;
import ru.yandex.direct.intapi.entity.idm.model.IdmResponse;
import ru.yandex.direct.intapi.entity.idm.model.IdmRole;
import ru.yandex.direct.intapi.entity.idm.model.RemoveRoleRequest;
import ru.yandex.direct.rbac.PpcRbac;
import ru.yandex.direct.rbac.RbacClientsRelations;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.rbac.RbacSubrole;
import ru.yandex.direct.rbac.UserPerminfo;
import ru.yandex.direct.regions.GeoTreeFactory;
import ru.yandex.direct.staff.client.StaffClient;
import ru.yandex.direct.staff.client.model.json.PersonInfo;
import ru.yandex.direct.tvm.TvmIntegration;
import ru.yandex.inside.passport.PassportDomain;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.inside.passport.blackbox2.protocol.request.params.AliasesParameterValue;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxAliases;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxOAuthStatus;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxResponseBuilder;

import static java.util.Collections.singletonMap;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;
import static ru.yandex.direct.intapi.entity.idm.service.IdmAddRoleServiceTest.getPersonInfo;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.inside.passport.blackbox2.protocol.BlackboxMethod.USER_INFO;

@IntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class IdmRemoveRoleServiceTest {

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
    private WalletService walletService;

    @Autowired
    private PpcRbac ppcRbac;

    @Autowired
    private ManagerHierarchyService managerHierarchyService;

    @Autowired
    private UserValidationService userValidationService;

    @Autowired
    private RbacClientsRelations rbacClientsRelations;

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

    private IdmRemoveRoleService idmRemoveRoleService;

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

        IdmNotificationMailSenderService mailSenderService = mock(IdmNotificationMailSenderService.class);

        idmRemoveRoleService = new IdmRemoveRoleService(userService, clientService, ppcRbac, managerHierarchyService,
                mailSenderService, rbacClientsRelations);

        user = generateUser();

        mockInstancesForTest(user.getClientId(), user.getLogin(), user.getUid());
        mockStaffClientForTest(user);
    }

    @Test
    public void removeRole_success_whenRoleIsSuper() {
        removeRoleAndCheckNewRoleIsEmpty(IdmRole.SUPER);
    }

    @Test
    public void removeRole_success_whenRoleIsSuperreader() {
        removeRoleAndCheckNewRoleIsEmpty(IdmRole.SUPERREADER);
    }

    @Test
    public void removeRole_success_whenRoleIsSupport() {
        removeRoleAndCheckNewRoleIsEmpty(IdmRole.SUPPORT);
    }

    @Test
    public void removeRole_success_whenRoleIsPlacer() {
        removeRoleAndCheckNewRoleIsEmpty(IdmRole.PLACER);
    }

    @Test
    public void removeRole_success_whenRoleIsMedia() {
        removeRoleAndCheckNewRoleIsEmpty(IdmRole.MEDIA);
    }

    @Test
    public void removeRole_success_whenRoleIsInternalAdAdmin() {
        removeRoleAndCheckNewRoleIsEmpty(IdmRole.INTERNAL_AD_ADMIN);
    }

    @Test
    public void removeRole_success_whenRoleIsInternalAdSuperreader() {
        removeRoleAndCheckNewRoleIsEmpty(IdmRole.INTERNAL_AD_SUPERREADER);
    }

    @Test
    public void removeRole_success_whenRoleIsInternalAdManager() {
        removeRoleAndCheckNewRoleIsEmpty(IdmRole.INTERNAL_AD_MANAGER);
    }

    @Test
    public void removeRole_success_whenRoleIsLimitedSupport() {
        removeRoleAndCheckNewRoleIsEmpty(IdmRole.LIMITED_SUPPORT);
    }

    @Test
    public void removeRole_success_whenRoleIsDeveloper() {
        removeRoleAndCheckNewRoleIsEmpty(IdmRole.DEVELOPER);
        checkUserIsNotDeveloper(user.getUid());
    }

    @Test
    public void removeRole_success_whenUserDoesNotExist() {
        removeRole(IdmRole.SUPER, user);
        checkUserWithUidDoesNotExist(user.getUid());
    }

    @Test
    public void removeRole_success_whenRoleIsAlreadyRemoved() {
        addRole(IdmRole.SUPER, user);
        removeRole(IdmRole.SUPER, user);
        removeRole(IdmRole.SUPER, user);
        checkRoleAndSubrole(user.getUid(), RbacRole.EMPTY, null);
    }

    @Test
    public void removeRole_success_whenRoleIsTeamleader() {
        addRole(IdmRole.MANAGER, user);
        addRole(IdmRole.TEAMLEADER, user);
        removeRole(IdmRole.TEAMLEADER, user);
        checkRoleAndSubrole(user.getUid(), RbacRole.MANAGER, null);
    }

    @Test
    public void removeRole_success_whenRoleIsSuperTeamleader() {
        addRole(IdmRole.MANAGER, user);
        addRole(IdmRole.SUPERTEAMLEADER, user);
        removeRole(IdmRole.SUPERTEAMLEADER, user);
        checkRoleAndSubrole(user.getUid(), RbacRole.MANAGER, null);
    }

    @Test
    public void removeRole_success_whenUserIsTeamleader_andRoleIsManager() {
        addRole(IdmRole.MANAGER, user);
        addRole(IdmRole.TEAMLEADER, user);
        removeRole(IdmRole.MANAGER, user);
        checkRoleAndSubrole(user.getUid(), RbacRole.MANAGER, RbacSubrole.TEAMLEADER);
    }

    @Test
    public void removeRole_success_whenUserIsSuperTeamleader_andRoleIsManager() {
        addRole(IdmRole.MANAGER, user);
        addRole(IdmRole.SUPERTEAMLEADER, user);
        removeRole(IdmRole.MANAGER, user);
        checkRoleAndSubrole(user.getUid(), RbacRole.MANAGER, RbacSubrole.SUPERTEAMLEADER);
    }

    @Test
    public void removeRole_success_whenRequestRoleDiffersFromDBRole() {
        addRole(IdmRole.PLACER, user);
        removeRole(IdmRole.MEDIA, user);
        checkRoleAndSubrole(user.getUid(), RbacRole.PLACER, null);
    }

    @Test
    public void removeRole_success_whenRequestDomainLoginDiffersFromDBDomainLogin() {
        addRole(IdmRole.PLACER, user);
        removeRole(IdmRole.PLACER, user.withDomainLogin("test-domain-login-2"));
        checkRoleAndSubrole(user.getUid(), RbacRole.PLACER, null);
    }

    @Test
    public void removeRole_success_whenRequestDomainLoginEqualsIgnoreCaseToDBDomainLogin() {
        addRole(IdmRole.PLACER, user);
        removeRole(IdmRole.PLACER, user.withDomainLogin("tEsT-DOMAIN-login"));
        checkRoleAndSubrole(user.getUid(), RbacRole.EMPTY, null);
    }

    @Test
    public void removeRole_whenAnotherPassportLogin() {
        addRole(IdmRole.MANAGER, user);
        ClientInfo client = steps.clientSteps().createDefaultClient();
        removeRole(IdmRole.MANAGER, user.withLogin(client.getLogin()));
        // проверяем что роли не изменились
        checkRoleAndSubrole(user.getUid(), RbacRole.MANAGER, null);
        checkRoleAndSubrole(client.getUid(), RbacRole.CLIENT, null);
    }

    @Test
    public void removeRole_andThenAddOtherRole() {
        addRole(IdmRole.PLACER, user);
        removeRole(IdmRole.PLACER, user);
        addRole(IdmRole.MANAGER, user);
        checkRoleAndSubrole(user.getUid(), RbacRole.MANAGER, null);
    }

    @Test
    public void removeRole_andThenAddTeamLeaderRole() {
        addRole(IdmRole.PLACER, user);
        removeRole(IdmRole.PLACER, user);
        addRole(IdmRole.TEAMLEADER, user, false);
        checkRoleAndSubrole(user.getUid(), RbacRole.EMPTY, null);
    }

    @Test
    public void removeRole_andThenAddRoleWithOtherDomainLogin() {
        addRole(IdmRole.PLACER, user);
        removeRole(IdmRole.PLACER, user);

        User expectedUser = new User()
                .withUid(user.getUid())
                .withLogin(user.getLogin())
                .withDomainLogin("other-domain-login")
                .withFio("test fio")
                .withEmail("test_email@yandex.ru")
                .withPhone(null);

        mockStaffClientForTest(expectedUser);
        addRole(IdmRole.PLACER, expectedUser);

        User actualUser = userService.getUser(user.getUid());
        CompareStrategy strategy = DefaultCompareStrategies.onlyFields(
                newPath("domainLogin"), newPath("fio"), newPath("email"), newPath("phone"));

        assertThat(actualUser, beanDiffer(expectedUser).useCompareStrategy(strategy));
    }

    private void removeRoleAndCheckNewRoleIsEmpty(IdmRole role) {
        addRole(role, user);
        removeRole(role, user);
        checkRoleAndSubrole(user.getUid(), RbacRole.EMPTY, null);
    }

    private void addRole(IdmRole role, User user) {
        addRole(role, user, true);
    }

    private void addRole(IdmRole role, User user, boolean isSuccessful) {
        AddRoleRequest request = new AddRoleRequest()
                .withRole(role.getTypedValue())
                .withPassportLogin(user.getLogin())
                .withDomainLogin(user.getDomainLogin());

        IdmResponse response = idmAddRoleService.addRole(request);
        assumeThat(response.getCode(), is(isSuccessful ? 0 : 1));
    }

    private void removeRole(IdmRole role, User user) {
        RemoveRoleRequest request = new RemoveRoleRequest()
                .withRole(role.getTypedValue())
                .withPassportLogin(user.getLogin())
                .withDomainLogin(user.getDomainLogin());

        IdmResponse response = idmRemoveRoleService.removeRole(request);
        assumeThat(response.getCode(), is(0));
    }

    private void checkRoleAndSubrole(Long uid, RbacRole expectedRole, RbacSubrole expectedSubrole) {
        Optional<UserPerminfo> userPerminfo = ppcRbac.getUserPerminfo(uid);
        assertThat(userPerminfo.isPresent(), is(true));
        assertThat(userPerminfo.get().role(), is(expectedRole));
        assertThat(userPerminfo.get().subrole(), is(expectedSubrole));
    }

    private void checkUserIsNotDeveloper(Long uid) {
        User actualUser = userService.getUser(uid);
        assertFalse("пользователь - не разработчик", actualUser.getDeveloper());
    }

    private void checkUserWithUidDoesNotExist(Long uid) {
        User actualUser = userService.getUser(uid);
        assertThat(actualUser, nullValue());
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

    private User generateUser() {
        UidAndClientId uidAndClientId = steps.clientSteps().generateNewUidAndClientId();
        return generateNewUser()
                .withUid(uidAndClientId.getUid())
                .withClientId(uidAndClientId.getClientId())
                .withLogin(steps.userSteps().getUserLogin(uidAndClientId.getUid()))
                .withDomainLogin("test-domain-login");
    }

    private void mockStaffClientForTest(User user) {
        PersonInfo personInfo = getPersonInfo(user);
        when(staffClient.getStaffUserInfos(any()))
                .thenReturn(singletonMap(personInfo.getLogin(), personInfo));
    }
}
