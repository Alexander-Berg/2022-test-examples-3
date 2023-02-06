package ru.yandex.direct.core.entity.client.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.direct.balance.client.BalanceClient;
import ru.yandex.direct.balance.client.model.request.CreateClientRequest;
import ru.yandex.direct.blackbox.client.BlackboxClient;
import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.agency.service.AgencyService;
import ru.yandex.direct.core.entity.campaign.repository.WalletRepository;
import ru.yandex.direct.core.entity.campaign.service.WalletService;
import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.entity.client.service.validation.AddClientValidationService;
import ru.yandex.direct.core.entity.currency.service.CurrencyDictCache;
import ru.yandex.direct.core.entity.currency.service.CurrencyService;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.geo.repository.FakeGeoRegionRepository;
import ru.yandex.direct.core.entity.user.service.BlackboxUserService;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.service.integration.balance.BalanceService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.model.LoginOrUid;
import ru.yandex.direct.dbutil.model.UidAndClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.env.EnvironmentType;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.regions.GeoTreeFactory;
import ru.yandex.direct.result.Result;
import ru.yandex.direct.tvm.TvmIntegration;
import ru.yandex.inside.passport.PassportDomain;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.inside.passport.blackbox2.protocol.request.params.AliasesParameterValue;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxAliases;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxCorrectResponse;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxOAuthStatus;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxResponseBuilder;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.validation.defects.Defects.pddLogin;
import static ru.yandex.direct.currency.CurrencyCode.RUB;
import static ru.yandex.direct.regions.Region.RUSSIA_REGION_ID;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.invalidValue;
import static ru.yandex.direct.validation.defect.StringDefects.notEmptyString;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;
import static ru.yandex.inside.passport.blackbox2.protocol.BlackboxMethod.USER_INFO;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AddClientServiceTest {

    private static final Integer DIRECT_SERVICE_FAKE_ID = 111111;
    private static final String DIRECT_SERVICE_FAKE_TOKEN = "DIRECT_SERVICE_FAKE_TOKEN";
    private static final Long BAD_UID = 7575757575L;
    private static final Long GOOD_UID = 1234567890L;
    private static final Long PDD_UID = 5005005005L;
    private static final Long LITE_UID = 6006006006L;
    private static final Long WALLET_UID = 127357575L;
    private static final Long NO_WALLET_UID = 129997573L;

    private static final Long GOOD_CLIENT_ID = 12345690L;
    private static final Long LITE_CLIENT_ID = 6006006L;
    private static final Long WALLET_CLIENT_ID = 1277575L;
    private static final Long NO_WALLET_CLIENT_ID = 1997573L;

    @Autowired
    private ClientService clientService;
    @Autowired
    private TvmIntegration tvmIntegration;
    @Autowired
    private WalletService walletService;
    @Autowired
    private WalletRepository walletRepository;
    @Autowired
    private ShardHelper shardHelper;
    @Autowired
    private GeoTreeFactory geoTreeFactory;
    @Autowired
    private UserService userService;
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
    @Autowired
    private BalanceService balanceService;

    private AddClientService addClientService;

    @Before
    public void setUp() {
        final var geoRegionRepository = new FakeGeoRegionRepository();
        final var currencyService = new CurrencyService(currencyDictCache, agencyService,
                balanceService, featureService, ppcPropertiesSupport, geoRegionRepository);
        final var addClientValidationService = new AddClientValidationService(currencyService);

        BalanceClient balanceClient = mock(BalanceClient.class);
        when(balanceClient.createClient(eq(GOOD_UID), any(CreateClientRequest.class)))
                .thenReturn(GOOD_CLIENT_ID);
        when(balanceClient.createClient(eq(LITE_UID), any(CreateClientRequest.class)))
                .thenReturn(LITE_CLIENT_ID);
        when(balanceClient.createClient(eq(WALLET_UID), any(CreateClientRequest.class)))
                .thenReturn(WALLET_CLIENT_ID);
        when(balanceClient.createClient(eq(NO_WALLET_UID), any(CreateClientRequest.class)))
                .thenReturn(NO_WALLET_CLIENT_ID);
        BalanceService balanceService =
                new BalanceService(balanceClient, DIRECT_SERVICE_FAKE_ID, DIRECT_SERVICE_FAKE_TOKEN);

        BlackboxCorrectResponse badResponse = new BlackboxResponseBuilder(USER_INFO)
                .setStatus(BlackboxOAuthStatus.VALID.value())
                .setLogin(Optional.of(""))
                .setUidDomain(Optional.of(Tuple2.tuple(PassportUid.cons(BAD_UID), PassportDomain.YANDEX_RU)))
                .build().getOrThrow();
        BlackboxCorrectResponse goodResponse = new BlackboxResponseBuilder(USER_INFO)
                .setStatus(BlackboxOAuthStatus.VALID.value())
                .setLogin(Optional.of("someLogin1"))
                .setUidDomain(Optional.of(Tuple2.tuple(PassportUid.cons(GOOD_UID), PassportDomain.YANDEX_RU)))
                .setAliases(Map.of(BlackboxAliases.PORTAL, "someLogin1"))
                .build().getOrThrow();
        BlackboxCorrectResponse badPddResponse = new BlackboxResponseBuilder(USER_INFO)
                .setStatus(BlackboxOAuthStatus.VALID.value())
                .setLogin(Optional.of("pdd@domain.ru"))
                .setUidDomain(Optional.of(Tuple2.tuple(PassportUid.cons(BAD_UID), PassportDomain.YANDEX_RU)))
                .setHosted(Optional.of(true))
                .setAliases(Map.of(BlackboxAliases.PORTAL, "pdd@domain.ru", BlackboxAliases.PDD, "pdd@domain.ru"))
                .build().getOrThrow();
        BlackboxCorrectResponse liteLoginResponse = new BlackboxResponseBuilder(USER_INFO)
                .setStatus(BlackboxOAuthStatus.VALID.value())
                .setLogin(Optional.of("lite@domain.ru"))
                .setUidDomain(Optional.of(Tuple2.tuple(PassportUid.cons(LITE_UID), PassportDomain.YANDEX_RU)))
                .setAliases(Map.of(BlackboxAliases.PORTAL, "lite@domain.ru"))
                .build().getOrThrow();
        BlackboxCorrectResponse walletClientResponse = new BlackboxResponseBuilder(USER_INFO)
                .setStatus(BlackboxOAuthStatus.VALID.value())
                .setLogin(Optional.of("someLogin2"))
                .setUidDomain(Optional.of(Tuple2.tuple(PassportUid.cons(WALLET_UID), PassportDomain.YANDEX_RU)))
                .setAliases(Map.of(BlackboxAliases.PORTAL, "lite@domain.ru"))
                .build().getOrThrow();
        BlackboxCorrectResponse noWalletClientResponse = new BlackboxResponseBuilder(USER_INFO)
                .setStatus(BlackboxOAuthStatus.VALID.value())
                .setLogin(Optional.of("someLogin3"))
                .setUidDomain(Optional.of(Tuple2.tuple(PassportUid.cons(NO_WALLET_UID), PassportDomain.YANDEX_RU)))
                .setAliases(Map.of(BlackboxAliases.PORTAL, "lite@domain.ru"))
                .build().getOrThrow();
        when(blackboxClient.userInfo(
                any(), eq(PassportUid.cons(BAD_UID)), any(AliasesParameterValue.class), anyString())
        ).thenReturn(badResponse);
        when(blackboxClient.userInfo(
                any(), eq(PassportUid.cons(GOOD_UID)), any(AliasesParameterValue.class), anyString())
        ).thenReturn(goodResponse);
        when(blackboxClient.userInfo(
                any(), eq(PassportUid.cons(PDD_UID)), any(AliasesParameterValue.class), anyString())
        ).thenReturn(badPddResponse);
        when(blackboxClient.userInfo(
                any(), eq(PassportUid.cons(LITE_UID)), any(AliasesParameterValue.class), anyString())
        ).thenReturn(liteLoginResponse);
        when(blackboxClient.userInfo(
                any(), eq(PassportUid.cons(WALLET_UID)), any(AliasesParameterValue.class), anyString())
        ).thenReturn(walletClientResponse);
        when(blackboxClient.userInfo(
                any(), eq(PassportUid.cons(NO_WALLET_UID)), any(AliasesParameterValue.class),
                anyString())
        ).thenReturn(noWalletClientResponse);

        var blackboxUserService = new BlackboxUserService(blackboxClient, EnvironmentType.DEVELOPMENT, tvmIntegration);
        addClientService = new AddClientService(balanceService, clientService, walletService,
                geoTreeFactory, addClientValidationService, blackboxUserService);
    }

    /**
     * Проверяем, что в запросе, в котором передано фио, нельзя создать клиента с пустым логином
     */
    @Test
    public void createClient_positiveTest() {
        LoginOrUid loginOrUid = LoginOrUid.of(GOOD_UID);
        Result<UidAndClientId> result = addClientService.processRequest(loginOrUid, "Fio Fio",
                RUSSIA_REGION_ID, RUB, RbacRole.CLIENT);
        assertTrue(result.isSuccessful());

        Client client = clientService.massGetClientsByUids(List.of(GOOD_UID)).get(GOOD_UID);
        assertThat(client.getWorkCurrency(), is(RUB));
        assertThat(client.getCountryRegionId(), is(RUSSIA_REGION_ID));
        assertThat(client.getRole(), is(RbacRole.CLIENT));
    }

    /**
     * Проверяем, что в запросе, нельзя создать клиента с пустым логином в паспорте
     */
    @Test
    public void emptyLoginForUid_FioRequest_NegativeTest() {
        LoginOrUid loginOrUid = LoginOrUid.of(BAD_UID);
        Result<UidAndClientId> result = addClientService.processRequest(loginOrUid, "Fio Fio",
                RUSSIA_REGION_ID, RUB, RbacRole.CLIENT);
        assertThat("Должны получить ошибку, так как логин невалидный", result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(), invalidValue())));
    }


    /**
     * Проверяем, что нельзя в запросе указать пустой логин
     */
    @Test
    public void emptyLogin_FioRequest_NegativeTest() {
        LoginOrUid loginOrUid = LoginOrUid.of("");
        Result<UidAndClientId> result = addClientService.processRequest(loginOrUid, "Fio Fio",
                RUSSIA_REGION_ID, RUB, RbacRole.CLIENT);
        assertThat("Должны получить ошибку, так как логин невалидный", result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(field("login")), notEmptyString())));
    }

    /**
     * Проверяем, что нельзя создать клиента с ПДД логином.
     */
    @Test
    public void pddLogin_NegativeTest() {
        LoginOrUid loginOrUid = LoginOrUid.of(PDD_UID);
        Result<UidAndClientId> result = addClientService.processRequest(loginOrUid, "Fio Fio",
                RUSSIA_REGION_ID, RUB, RbacRole.CLIENT);
        assertThat("Должны получить ошибку, так как логин из ПДД", result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(), pddLogin())));
    }

    /**
     * Проверяем, что можно создать клиента с Lite логином, и ему запишется правильный email
     */
    @Test
    public void liteLogin_PositiveTest() {
        LoginOrUid loginOrUid = LoginOrUid.of(LITE_UID);
        Result<UidAndClientId> result = addClientService.processRequest(loginOrUid, "Fio Fio",
                RUSSIA_REGION_ID, RUB, RbacRole.CLIENT);
        assertTrue(result.isSuccessful());

        String userEmail = userService.getUserEmail(LITE_UID);
        assertThat(userEmail, equalTo("lite@domain.ru"));
    }

    /**
     * Проверяем, что если создать клиента без кошелька, он действительно таковой создастся
     */
    @Test
    public void createWithNoWallet_NoWalletCreatedTest() {
        LoginOrUid loginOrUid = LoginOrUid.of(NO_WALLET_UID);
        Result<UidAndClientId> result = addClientService.processRequest(loginOrUid, "Fio Fio",
                RUSSIA_REGION_ID, RUB, RbacRole.CLIENT);
        assumeThat("Клиент должен создаться успешно", result.getValidationResult() == null ||
                !result.getValidationResult().hasAnyErrors(), is(true));
        ClientId clientId = result.getResult().getClientId();
        CurrencyCode clientCurrency = clientService.getWorkCurrency(clientId).getCode();
        int shard = shardHelper.getShardByClientId(clientId);
        Long walletCid = walletRepository.getActualClientWalletId(shard, clientId, clientCurrency);
        assertThat("Кошелька не должно быть", walletCid, nullValue());
    }

    /**
     * Проверяем, что если создать клиента c кошельком, общий счёт создастся
     */
    @Test
    public void createWithWallet_WalletCreatedTest() {
        LoginOrUid loginOrUid = LoginOrUid.of(WALLET_UID);
        Result<UidAndClientId> result = addClientService.processRequest(loginOrUid, "Fio Fio",
                RUSSIA_REGION_ID, RUB, RbacRole.CLIENT, AddClientOptions.defaultOptions().withWallet(true));
        assumeThat("Клиент должен создаться успешно", result.getValidationResult() == null ||
                !result.getValidationResult().hasAnyErrors(), is(true));
        ClientId clientId = result.getResult().getClientId();
        CurrencyCode clientCurrency = clientService.getWorkCurrency(clientId).getCode();
        int shard = shardHelper.getShardByClientId(clientId);
        Long walletCid = walletRepository.getActualClientWalletId(shard, clientId, clientCurrency);
        assertThat("Кошелёк должен быть", walletCid, notNullValue());
    }
}
