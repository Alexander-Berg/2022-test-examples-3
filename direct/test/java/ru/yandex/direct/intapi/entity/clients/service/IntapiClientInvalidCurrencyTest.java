package ru.yandex.direct.intapi.entity.clients.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.direct.blackbox.client.BlackboxClient;
import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.communication.service.CommunicationEventService;
import ru.yandex.direct.core.entity.agency.service.AgencyService;
import ru.yandex.direct.core.entity.apifinancetokens.repository.ApiFinanceTokensRepository;
import ru.yandex.direct.core.entity.campaign.service.CampaignService;
import ru.yandex.direct.core.entity.campaign.service.WalletService;
import ru.yandex.direct.core.entity.client.repository.ClientRepository;
import ru.yandex.direct.core.entity.client.service.AddClientService;
import ru.yandex.direct.core.entity.client.service.ClientNdsService;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.client.service.validation.AddClientValidationService;
import ru.yandex.direct.core.entity.currency.service.CurrencyDictCache;
import ru.yandex.direct.core.entity.currency.service.CurrencyService;
import ru.yandex.direct.core.entity.feature.model.Feature;
import ru.yandex.direct.core.entity.feature.model.FeatureSettings;
import ru.yandex.direct.core.entity.feature.service.FeatureManagingService;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.geo.repository.FakeGeoRegionRepository;
import ru.yandex.direct.core.entity.user.service.ApiUserService;
import ru.yandex.direct.core.entity.user.service.BlackboxUserService;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.service.integration.balance.BalanceService;
import ru.yandex.direct.core.service.integration.balance.RegisterClientResult;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.model.LoginOrUid;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.env.EnvironmentType;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.clients.model.ClientState;
import ru.yandex.direct.intapi.entity.clients.model.ClientsAddOrGetResponse;
import ru.yandex.direct.rbac.PpcRbac;
import ru.yandex.direct.regions.GeoTreeFactory;
import ru.yandex.direct.result.Result;
import ru.yandex.direct.tvm.TvmIntegration;
import ru.yandex.direct.validation.defect.CommonDefects;
import ru.yandex.direct.validation.result.DefectInfo;
import ru.yandex.direct.validation.result.ValidationResult;
import ru.yandex.inside.passport.PassportDomain;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.inside.passport.blackbox2.protocol.request.params.AliasesParameterValue;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxAliases;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxOAuthStatus;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxResponseBuilder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.currency.CurrencyCode.RUB;
import static ru.yandex.direct.currency.CurrencyCode.USD;
import static ru.yandex.direct.dbschema.ppc.Tables.CLIENTS_TO_FETCH_NDS;
import static ru.yandex.direct.regions.Region.RUSSIA_REGION_ID;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.inside.passport.blackbox2.protocol.BlackboxMethod.USER_INFO;

@IntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class IntapiClientInvalidCurrencyTest {
    private static final Long PREMODERATION_FEATURE_ID = 7777777L;

    @Autowired
    private FeatureService featureService;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private ShardHelper shardHelper;

    @Autowired
    private ApiUserService apiUserService;

    @Autowired
    private UserService userService;

    @Autowired
    private ApiFinanceTokensRepository apiFinanceTokensRepository;

    @Autowired
    private FeatureManagingService featureManagingService;

    @Autowired
    private ClientService clientService;

    @Autowired
    private WalletService walletService;

    @Autowired
    private GeoTreeFactory geoTreeFactory;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private PpcRbac ppcRbac;

    @Autowired
    private TvmIntegration tvmIntegration;

    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;
    @Autowired
    private CampaignService campaignService;
    @Autowired
    private ClientNdsService clientNdsService;

    @Autowired
    private BlackboxClient blackboxClient;

    @Autowired
    private CommunicationEventService communicationEventService;

    @Autowired
    private CurrencyDictCache currencyDictCache;
    @Autowired
    private AgencyService agencyService;

    private BalanceService balanceService;
    private IntapiClientService service;

    @Before
    public void setUp() {
        balanceService = mock(BalanceService.class);

        final var geoRegionRepository = new FakeGeoRegionRepository();
        final var currencyService = new CurrencyService(currencyDictCache, agencyService,
                balanceService, featureService, ppcPropertiesSupport, geoRegionRepository);
        final var addClientValidationService = new AddClientValidationService(currencyService);

        var blackboxUserService = new BlackboxUserService(blackboxClient, EnvironmentType.DEVELOPMENT, tvmIntegration);
        var addClientService = new AddClientService(balanceService, clientService, walletService,
                geoTreeFactory, addClientValidationService, blackboxUserService);
        if (featureManagingService.getCachedFeatures().stream()
                .noneMatch(t -> t.getId().equals(PREMODERATION_FEATURE_ID))) {
            featureManagingService.addFeatures(List.of(new Feature().withId(PREMODERATION_FEATURE_ID)
                    .withFeatureTextId(FeatureName.PAYMENT_BEFORE_MODERATION.getName())
                    .withFeaturePublicName(FeatureName.PAYMENT_BEFORE_MODERATION.getName())
                    .withSettings(new FeatureSettings().withRoles(ImmutableSet.of("client")))));
        }

        service = new IntapiClientService(clientService, featureService, addClientService,
                userService, apiFinanceTokensRepository, apiUserService, featureManagingService,
                shardHelper, clientRepository, ppcRbac, walletService, blackboxUserService,
                clientNdsService, campaignService, communicationEventService);
    }

    @Test
    public void successTest() {
        String login = "validCurrencyIntapiClientTestLogin1";
        Long uid = 1561121L;
        ClientId clientId = ClientId.fromLong(200340019L);
        ClientsAddOrGetResponse response = testForUser(clientId, login, uid, RUSSIA_REGION_ID, RUB);
        assertThat(response.getUserId(), is(uid));
        assertThat(response.getClientId(), is(clientId.asLong()));
        assertThat(response.getFinanceToken(), notNullValue());
        testCheckOnlyAfterCreating(uid);
        testNdsIsFetched(shardHelper.getShardByClientId(clientId), clientId);
    }

    @Test
    public void failTest() {
        String login = "invalidCurrencyIntapiClientTestLogin1";
        Long uid = 1561122L;
        ClientId clientId = ClientId.fromLong(200340020L);
        LoginOrUid loginOrUid = LoginOrUid.of(uid);
        DefectInfo expectedMessage = Result.broken(new ValidationResult<>(new Object(),
                null, null,
                Map.of(field("currency"), ValidationResult.failed(USD, CommonDefects.invalidValue()))))
                .getErrors()
                .get(0);
        assertThatThrownBy(() -> testForUser(clientId, login, uid, RUSSIA_REGION_ID, USD))
                .hasMessageStartingWith(expectedMessage.toString());
        verifySomeBalanceCallsNeverDone();
    }


    private ClientsAddOrGetResponse testForUser(ClientId clientId, String login, Long uid,
                                                Long regionId, CurrencyCode currency) {
        mockInstancesForTest(clientId, login, uid);
        LoginOrUid loginOrUid = LoginOrUid.of(null, uid);
        ClientsAddOrGetResponse response =
                (ClientsAddOrGetResponse) service.addOrGetClient(loginOrUid, "test fio", regionId, currency, true);
        return response;
    }

    private void mockInstancesForTest(ClientId clientId, String login, Long uid) {
        when(balanceService.registerNewClient(any(), any(), any()))
                .thenReturn(RegisterClientResult.success(clientId));
        when(balanceService.registerNewClient(any(), any(), any(), anyBoolean()))
                .thenReturn(RegisterClientResult.success(clientId));
        BlackboxResponseBuilder builder = new BlackboxResponseBuilder(USER_INFO)
                .setStatus(BlackboxOAuthStatus.VALID.value())
                .setLogin(Optional.of(login))
                .setAliases(Map.of(BlackboxAliases.PORTAL, login))
                .setUidDomain(Optional.of(Tuple2.tuple(PassportUid.cons(uid), PassportDomain.YANDEX_RU)));
        when(blackboxClient.userInfo(any(), any(PassportUid.class), any(AliasesParameterValue.class), anyString()))
                .thenReturn(builder.build().getOrThrow());
    }

    private void testNdsIsFetched(int shard, ClientId clientId) {
        assertThat(
                dslContextProvider.ppc(shard).select(CLIENTS_TO_FETCH_NDS.TRIES)
                        .from(CLIENTS_TO_FETCH_NDS)
                        .where(CLIENTS_TO_FETCH_NDS.CLIENT_ID.eq(clientId.asLong()))
                        .fetchOne(CLIENTS_TO_FETCH_NDS.TRIES),
                is(0L));
    }

    private void testCheckOnlyAfterCreating(Long uid) {
        LoginOrUid loginOrUid = LoginOrUid.of(null, uid);
        ClientState response = service.checkClientState(loginOrUid).getClientState();
        assertThat(response, is(ClientState.API_ENABLED));
    }

    /**
     * Проверяем, что методы похода в баланс ниже по коду не будут сделаны
     */
    private void verifySomeBalanceCallsNeverDone() {
        verify(balanceService, never()).registerNewClient(any(), any(), any());
        verify(balanceService, never()).registerNewClient(any(), any(), any(), anyBoolean());
    }
}
