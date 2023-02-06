package ru.yandex.direct.intapi.entity.clients.service;

import java.util.HashMap;
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
import ru.yandex.direct.balance.client.BalanceClient;
import ru.yandex.direct.blackbox.client.BlackboxClient;
import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.communication.service.CommunicationEventService;
import ru.yandex.direct.core.entity.apifinancetokens.repository.ApiFinanceTokensRepository;
import ru.yandex.direct.core.entity.campaign.repository.WalletRepository;
import ru.yandex.direct.core.entity.campaign.service.CampaignService;
import ru.yandex.direct.core.entity.campaign.service.WalletService;
import ru.yandex.direct.core.entity.client.repository.ClientRepository;
import ru.yandex.direct.core.entity.client.service.AddClientService;
import ru.yandex.direct.core.entity.client.service.ClientNdsService;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.client.service.validation.AddClientValidationService;
import ru.yandex.direct.core.entity.feature.model.Feature;
import ru.yandex.direct.core.entity.feature.model.FeatureSettings;
import ru.yandex.direct.core.entity.feature.repository.ClientFeaturesRepository;
import ru.yandex.direct.core.entity.feature.service.FeatureManagingService;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.user.service.ApiUserService;
import ru.yandex.direct.core.entity.user.service.BlackboxUserService;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.service.integration.balance.BalanceService;
import ru.yandex.direct.core.validation.defects.Defects;
import ru.yandex.direct.dbutil.model.LoginOrUid;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.dbutil.sharding.ShardSupport;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.env.EnvironmentType;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.clients.model.ClientsAddOrGetResponse;
import ru.yandex.direct.rbac.PpcRbac;
import ru.yandex.direct.regions.GeoTreeFactory;
import ru.yandex.direct.result.Result;
import ru.yandex.direct.tvm.TvmIntegration;
import ru.yandex.direct.validation.result.DefectInfo;
import ru.yandex.direct.validation.result.ValidationResult;
import ru.yandex.inside.passport.PassportDomain;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.inside.passport.blackbox2.protocol.request.params.AliasesParameterValue;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxAliases;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxOAuthStatus;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxResponseBuilder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.currency.CurrencyCode.RUB;
import static ru.yandex.direct.regions.Region.RUSSIA_REGION_ID;
import static ru.yandex.direct.validation.defect.CommonDefects.invalidValue;
import static ru.yandex.inside.passport.blackbox2.protocol.BlackboxMethod.USER_INFO;

@IntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class IntapiClientNonExistentLoginTest {

    private static final Long PREMODERATION_FEATURE_ID = 7777777L;
    private static final String DIRECT_SERVICE_FAKE_TOKEN = "DIRECT_SERVICE_FAKE_TOKEN";
    private static final Integer DIRECT_SERVICE_FAKE_ID = 111111;

    @Autowired
    private FeatureService featureService;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private ShardSupport shardSupport;

    @Autowired
    private ShardHelper shardHelper;

    @Autowired
    private ApiUserService apiUserService;

    @Autowired
    private UserService userService;

    @Autowired
    private ApiFinanceTokensRepository apiFinanceTokensRepository;

    @Autowired
    private ClientFeaturesRepository clientFeaturesRepository;

    @Autowired
    private FeatureManagingService featureManagingService;

    @Autowired
    private ClientService clientService;

    @Autowired
    private GeoTreeFactory geoTreeFactory;

    @Autowired
    private AddClientValidationService addClientValidationService;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private WalletService walletService;

    @Autowired
    private PpcRbac ppcRbac;

    @Autowired
    private TvmIntegration tvmIntegration;

    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private CampaignService campaignService;

    @Autowired
    private ClientNdsService clientNdsService;

    @Autowired
    private BlackboxClient blackboxClient;

    @Autowired
    private CommunicationEventService communicationEventService;

    private BalanceClient balanceClient;
    private IntapiClientService service;

    @Before
    public void setUp() {
        balanceClient = mock(BalanceClient.class);
        var balanceService = new BalanceService(balanceClient, DIRECT_SERVICE_FAKE_ID, DIRECT_SERVICE_FAKE_TOKEN);
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
    public void addNonExistingLogin_Error() {
        Long uid = 1111122L;
        LoginOrUid loginOrUid = LoginOrUid.of(uid);
        DefectInfo expectedMessage = Result.broken(ValidationResult.failed(loginOrUid.get(), invalidValue()))
                .getErrors()
                .get(0);
        assertThatThrownBy(() -> testForUser(uid, null, true, false))
                .hasMessageStartingWith(expectedMessage.toString());
    }

    @Test
    public void addEmptyLogin_Error() {
        Long uid = 1111222L;
        LoginOrUid loginOrUid = LoginOrUid.of(uid);
        DefectInfo expectedMessage = Result.broken(ValidationResult.failed(loginOrUid.get(), invalidValue()))
                .getErrors()
                .get(0);
        assertThatThrownBy(() -> testForUser(uid, "", true, false))
                .hasMessageStartingWith(expectedMessage.toString());
        verifyZeroInteractions(balanceClient);
    }

    @Test
    public void addInvalidLogin_Error() {
        Long uid = 1111322L;
        LoginOrUid loginOrUid = LoginOrUid.of(uid);
        DefectInfo expectedMessage = Result.broken(ValidationResult.failed(loginOrUid.get(), invalidValue()))
                .getErrors()
                .get(0);
        assertThatThrownBy(() -> testForUser(uid, null, false, false))
                .hasMessageStartingWith(expectedMessage.toString());
        verifyZeroInteractions(balanceClient);
    }

    @Test
    public void addPddLogin_Error() {
        Long uid = 1111133L;
        LoginOrUid loginOrUid = LoginOrUid.of(uid);
        DefectInfo expectedMessage = Result.broken(ValidationResult.failed(loginOrUid.get(), Defects.pddLogin()))
                .getErrors()
                .get(0);
        assertThatThrownBy(() -> testForUser(uid, "pdd@domain.ru", true, true))
                .hasMessageStartingWith(expectedMessage.toString());
        verifyZeroInteractions(balanceClient);
    }

    @Test
    public void addBadEmailLogin_Error() {
        Long uid = 1111134L;
        LoginOrUid loginOrUid = LoginOrUid.of(uid);
        DefectInfo expectedMessage = Result.broken(ValidationResult.failed(loginOrUid.get(), invalidValue()))
                .getErrors()
                .get(0);
        assertThatThrownBy(() -> testForUser(uid, "email@domain.ru@domain.ru", true, false))
                .hasMessageStartingWith(expectedMessage.toString());
        verifyZeroInteractions(balanceClient);
    }

    private ClientsAddOrGetResponse testForUser(Long uid, String loginForBlackboxAnswer, boolean isAnswerValid,
                                                boolean isPddLogin) {
        mockInstancesForTest(uid, loginForBlackboxAnswer, isAnswerValid, isPddLogin);
        LoginOrUid loginOrUid = LoginOrUid.of(null, uid);
        ClientsAddOrGetResponse response =
                (ClientsAddOrGetResponse) service.addOrGetClient(loginOrUid, "test fio", RUSSIA_REGION_ID, RUB, true);
        return response;
    }

    private void mockInstancesForTest(
            Long uid, String loginForBlackboxAnswer, boolean isAnswerValid, boolean isPddLogin) {
        Map<Integer, String> aliases = new HashMap<>();
        if (loginForBlackboxAnswer != null) {
            aliases.put(BlackboxAliases.PORTAL, loginForBlackboxAnswer);
        }
        if (isPddLogin) {
            aliases.put(BlackboxAliases.PDD, loginForBlackboxAnswer);
        }
        BlackboxResponseBuilder builder = new BlackboxResponseBuilder(USER_INFO)
                .setStatus(isAnswerValid ? BlackboxOAuthStatus.VALID.value() : BlackboxOAuthStatus.INVALID.value())
                .setLogin(loginForBlackboxAnswer == null ? Optional.empty() : Optional.of(loginForBlackboxAnswer))
                .setUidDomain(Optional.of(Tuple2.tuple(PassportUid.cons(uid), PassportDomain.YANDEX_RU)))
                .setAliases(aliases)
                .setHosted(Optional.of(isPddLogin));
        when(blackboxClient.userInfo(any(), any(PassportUid.class), any(AliasesParameterValue.class), anyString()))
                .thenReturn(builder.build().getOrThrow());
    }
}
