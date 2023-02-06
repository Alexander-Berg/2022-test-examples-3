package ru.yandex.direct.intapi.entity.idm.service;

import java.util.Map;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.direct.balance.client.BalanceClient;
import ru.yandex.direct.balance.client.model.response.FindClientResponseItem;
import ru.yandex.direct.balance.client.model.response.FirmCountryCurrencyItem;
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
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.model.UidAndClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.env.EnvironmentType;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.idm.model.AddRoleRequest;
import ru.yandex.direct.intapi.entity.idm.model.IdmResponse;
import ru.yandex.direct.intapi.entity.idm.model.IdmRole;
import ru.yandex.direct.rbac.PpcRbac;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.regions.GeoTreeFactory;
import ru.yandex.direct.regions.Region;
import ru.yandex.direct.staff.client.StaffClient;
import ru.yandex.direct.staff.client.model.json.PersonInfo;
import ru.yandex.direct.tvm.TvmIntegration;
import ru.yandex.inside.passport.PassportDomain;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.inside.passport.blackbox2.protocol.request.params.AliasesParameterValue;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxAliases;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxOAuthStatus;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxResponseBuilder;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;
import static ru.yandex.direct.intapi.entity.idm.service.IdmAddRoleServiceTest.getPersonInfo;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.inside.passport.blackbox2.protocol.BlackboxMethod.USER_INFO;

@IntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class IdmAddRoleServiceClientExistsInBalanceTest {

    private static final Integer DIRECT_SERVICE_FAKE_ID = 111111;
    private static final String DIRECT_SERVICE_FAKE_TOKEN = "DIRECT_SERVICE_FAKE_TOKEN";

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
    private TvmIntegration tvmIntegration;

    @Autowired
    private UserValidationService userValidationService;
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

    private BalanceClient balanceClient;
    private StaffClient staffClient;
    private IdmAddRoleService idmAddRoleService;

    private User user;

    @Before
    public void setUp() {
        balanceClient = mock(BalanceClient.class);

        final var balanceService =
                new BalanceService(balanceClient, DIRECT_SERVICE_FAKE_ID, DIRECT_SERVICE_FAKE_TOKEN);
        final var geoRegionRepository = new FakeGeoRegionRepository();
        final var currencyService = new CurrencyService(currencyDictCache, agencyService,
                balanceService, featureService, ppcPropertiesSupport, geoRegionRepository);
        final var addClientValidationService = new AddClientValidationService(currencyService);

        staffClient = mock(StaffClient.class);

        var blackboxUserService = new BlackboxUserService(blackboxClient, EnvironmentType.DEVELOPMENT, tvmIntegration);
        AddClientService addClientService = new AddClientService(balanceService, clientService, walletService,
                geoTreeFactory, addClientValidationService, blackboxUserService);

        idmAddRoleService = new IdmAddRoleService(userService, ppcRbac, addClientService,
                clientService, userRepository, shardHelper, staffClient, userValidationService,
                blackboxClient, balanceService, tvmIntegration, EnvironmentType.DEVELOPMENT);

        user = generateUser();
        mockBlackbox(user.getLogin(), user.getUid());
        mockStaffClient(user);
    }

    @Test
    public void addRole_success_whenClientIdExistsInBalance() {
        when(balanceClient.findClient(any())).thenReturn(singletonList(new FindClientResponseItem()
                .withClientId(user.getClientId().asLong())));
        when(balanceClient.getFirmCountryCurrency(any())).thenReturn(singletonList(new FirmCountryCurrencyItem()
                .withCurrency(CurrencyCode.RUB.name())
                .withRegionId((int) Region.RUSSIA_REGION_ID)));
        when(balanceClient.createClient(anyLong(), any())).thenReturn(user.getClientId().asLong());

        addRole(IdmRole.PLACER, user, true);
        verify(balanceClient, never()).createUserClientAssociation(any(), any(), any());
    }

    @Test
    public void addRole_success_whenClientIdDoesNotExistInBalance() {
        when(balanceClient.findClient(any())).thenReturn(emptyList());
        when(balanceClient.createClient(anyLong(), any())).thenReturn(user.getClientId().asLong());

        addRole(IdmRole.PLACER, user, true);
        verify(balanceClient).createUserClientAssociation(anyLong(),
                eq(user.getClientId().asLong()), eq(user.getUid()));
    }

    @Test
    public void addRole_failure_whenClientIdAssociatedWithAnotherUid() {
        ClientId clientId =
                steps.clientSteps().createDefaultClientWithRoleInAnotherShard(RbacRole.PLACER).getClientId();
        when(balanceClient.findClient(any())).thenReturn(singletonList(new FindClientResponseItem()
                .withClientId(clientId.asLong())));
        addRole(IdmRole.PLACER, user, false);
        checkUserWithLoginDoesNotExist(user.getLogin());
    }

    @Test
    public void addRole_success_whenClientIdAssociatedOnlyWithThisUid() {
        User existingUser =
                steps.clientSteps().createDefaultClientWithRoleInAnotherShard(RbacRole.PLACER)
                        .getChiefUserInfo().getUser();

        when(balanceClient.findClient(any())).thenReturn(singletonList(new FindClientResponseItem()
                .withClientId(existingUser.getClientId().asLong())));
        mockBlackbox(existingUser.getLogin(), existingUser.getUid());
        mockStaffClient(existingUser);

        addRole(IdmRole.PLACER, existingUser, true);
        User expectedUser = new User()
                .withUid(existingUser.getUid())
                .withClientId(existingUser.getClientId())
                .withLogin(existingUser.getLogin())
                .withRole(RbacRole.PLACER);
        checkUser(existingUser.getUid(), expectedUser);
    }

    @Test
    public void addRole_failure_whenPassportLoginNotFound() {
        BlackboxResponseBuilder builder = new BlackboxResponseBuilder(USER_INFO)
                .setStatus(BlackboxOAuthStatus.VALID.value())
                .setLogin(Optional.empty())
                .setUidDomain(Optional.empty());
        when(blackboxClient.userInfo(any(), anyString(), anyList(), anyString()))
                .thenReturn(builder.build().getOrThrow());

        addRole(IdmRole.PLACER, user, false);
        checkUserWithLoginDoesNotExist(user.getLogin());
    }

    private void addRole(IdmRole role, User user, boolean isSuccessful) {
        AddRoleRequest request = new AddRoleRequest()
                .withRole(role.getTypedValue())
                .withPassportLogin(user.getLogin())
                .withDomainLogin(user.getDomainLogin());

        IdmResponse response = idmAddRoleService.addRole(request);
        assumeThat(response.getCode(), is(isSuccessful ? 0 : 1));
    }

    private void checkUser(Long uid, User expectedUser) {
        User actualUser = userService.getUser(uid);
        assertThat(actualUser, beanDiffer(expectedUser).useCompareStrategy(onlyExpectedFields()));
    }

    private void checkUserWithLoginDoesNotExist(String login) {
        User actualUser = userService.getUserByLogin(login);
        assertThat(actualUser, nullValue());
    }

    private void mockBlackbox(String login, Long uid) {
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

    private void mockStaffClient(User user) {
        PersonInfo personInfo = getPersonInfo(user);
        when(staffClient.getStaffUserInfos(any()))
                .thenReturn(singletonMap(personInfo.getLogin(), personInfo));
    }

    private User generateUser() {
        UidAndClientId uidAndClientId = steps.clientSteps().generateNewUidAndClientId();
        return generateNewUser()
                .withUid(uidAndClientId.getUid())
                .withClientId(uidAndClientId.getClientId())
                .withLogin(steps.userSteps().getUserLogin(uidAndClientId.getUid()))
                .withDomainLogin("test-domain-login");
    }
}
