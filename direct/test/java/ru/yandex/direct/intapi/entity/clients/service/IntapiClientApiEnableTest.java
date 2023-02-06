package ru.yandex.direct.intapi.entity.clients.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.RandomUtils;
import org.assertj.core.api.SoftAssertions;
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
import ru.yandex.direct.core.entity.user.model.ApiEnabled;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.core.entity.user.repository.ApiUserRepository;
import ru.yandex.direct.core.entity.user.service.ApiUserService;
import ru.yandex.direct.core.entity.user.service.BlackboxUserService;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.service.integration.balance.BalanceService;
import ru.yandex.direct.core.service.integration.balance.RegisterClientResult;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.model.LoginOrUid;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.env.EnvironmentType;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.clients.model.ClientState;
import ru.yandex.direct.rbac.PpcRbac;
import ru.yandex.direct.regions.GeoTreeFactory;
import ru.yandex.direct.regions.Region;
import ru.yandex.direct.tvm.TvmIntegration;
import ru.yandex.inside.passport.PassportDomain;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.inside.passport.blackbox2.protocol.request.params.AliasesParameterValue;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxAliases;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxOAuthStatus;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxResponseBuilder;

import static org.apache.commons.lang3.RandomStringUtils.random;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.dbschema.ppc.Tables.CLIENTS_TO_FETCH_NDS;
import static ru.yandex.inside.passport.blackbox2.protocol.BlackboxMethod.USER_INFO;

@IntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class IntapiClientApiEnableTest {

    private static final Long PREMODERATION_FEATURE_ID = 7777777L;

    @Autowired
    private FeatureService featureService;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private ShardHelper shardHelper;

    @Autowired
    private ApiUserRepository apiUserRepository;

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
    private Steps steps;
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

    private ClientInfo clientInfo;

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
                .noneMatch(t -> t.getFeatureTextId().equals(FeatureName.PAYMENT_BEFORE_MODERATION.getName()))) {
            featureManagingService.addFeatures(List.of(new Feature().withId(PREMODERATION_FEATURE_ID)
                    .withFeatureTextId(FeatureName.PAYMENT_BEFORE_MODERATION.getName())
                    .withFeaturePublicName(FeatureName.PAYMENT_BEFORE_MODERATION.getName())
                    .withSettings(new FeatureSettings().withRoles(ImmutableSet.of("client")))));
        }

        service = new IntapiClientService(clientService, featureService, addClientService,
                userService, apiFinanceTokensRepository, apiUserService, featureManagingService,
                shardHelper, clientRepository, ppcRbac, walletService, blackboxUserService,
                clientNdsService, campaignService, communicationEventService);

        clientInfo = steps.clientSteps().createDefaultClient();
    }

    @Test
    public void testExistingClientWithApiTrue_ApiEnabled() {
        mockInstancesForTest(clientInfo.getClientId(), clientInfo.getLogin(), clientInfo.getUid());
        service.addOrGetClient(LoginOrUid.of(clientInfo.getUid()), "fio", Region.RUSSIA_REGION_ID,
                CurrencyCode.RUB, true);
        ApiUser apiUser = apiUserRepository.fetchByUid(clientInfo.getShard(), clientInfo.getUid());
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(apiUser.getApiEnabled()).isEqualTo(ApiEnabled.DEFAULT);
            softly.assertThat(apiUser.getApiOfferAccepted()).isEqualTo(true);
            softly.assertThat(apiUser.getApiFinancialOperationsAllowed()).isEqualTo(true);
            softly.assertThat(service.checkClientState(LoginOrUid.of(null, clientInfo.getUid())).getClientState())
                    .isEqualTo(ClientState.API_ENABLED);
        });
    }

    @Test
    public void testExistingClientWithApiFalse_ApiNotEnabled() {
        mockInstancesForTest(clientInfo.getClientId(), clientInfo.getLogin(), clientInfo.getUid());
        service.addOrGetClient(LoginOrUid.of(clientInfo.getUid()), "fio", Region.RUSSIA_REGION_ID,
                CurrencyCode.RUB, false);
        ApiUser apiUser = apiUserRepository.fetchByUid(clientInfo.getShard(), clientInfo.getUid());
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(apiUser.getApiEnabled()).isEqualTo(ApiEnabled.DEFAULT);
            softly.assertThat(apiUser.getApiOfferAccepted()).isEqualTo(false);
            softly.assertThat(apiUser.getApiFinancialOperationsAllowed()).isEqualTo(false);
            softly.assertThat(service.checkClientState(LoginOrUid.of(null, clientInfo.getUid())).getClientState())
                    .isEqualTo(ClientState.API_DISABLED);
        });
    }

    @Test
    public void testNewClientWithApiFalse_ApiNotEnabled() {
        Long fakeClientId = 125341324L;
        Long fakeUid = RandomUtils.nextLong(100000000, 999999999);
        String fakeLogin = random(12, true, false);
        mockInstancesForTest(ClientId.fromLong(fakeClientId), fakeLogin, fakeUid);
        service.addOrGetClient(LoginOrUid.of(fakeUid), "fio", Region.RUSSIA_REGION_ID, CurrencyCode.RUB, false);
        ApiUser apiUser = apiUserRepository.fetchByUid(shardHelper.getShardByClientId(
                ClientId.fromLong(fakeClientId)), fakeUid);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(apiUser.getApiEnabled()).isEqualTo(ApiEnabled.DEFAULT);
            softly.assertThat(apiUser.getApiOfferAccepted()).isEqualTo(false);
            softly.assertThat(apiUser.getApiFinancialOperationsAllowed()).isEqualTo(false);
            softly.assertThat(service.checkClientState(LoginOrUid.of(null, fakeUid)).getClientState())
                    .isEqualTo(ClientState.API_DISABLED);
            softly.assertThat(getNdsFetchTriesNum(shardHelper.getShardByClientId(ClientId.fromLong(fakeClientId)),
                    ClientId.fromLong(fakeClientId))).isNotNull();
        });
    }

    private Long getNdsFetchTriesNum(int shard, ClientId clientId) {
        return dslContextProvider.ppc(shard).select(CLIENTS_TO_FETCH_NDS.TRIES)
                .from(CLIENTS_TO_FETCH_NDS)
                .where(CLIENTS_TO_FETCH_NDS.CLIENT_ID.eq(clientId.asLong()))
                .fetchOne(CLIENTS_TO_FETCH_NDS.TRIES);
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
                .setUidDomain(Optional.of(Tuple2.tuple(PassportUid.cons(uid),
                        PassportDomain.YANDEX_RU)));
        when(blackboxClient.userInfo(any(), any(PassportUid.class), any(AliasesParameterValue.class), anyString()))
                .thenReturn(builder.build().getOrThrow());
    }
}
