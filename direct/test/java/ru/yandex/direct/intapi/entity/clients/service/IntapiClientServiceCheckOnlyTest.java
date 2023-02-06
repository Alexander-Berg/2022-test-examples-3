package ru.yandex.direct.intapi.entity.clients.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.communication.service.CommunicationEventService;
import ru.yandex.direct.core.entity.apifinancetokens.repository.ApiFinanceTokensRepository;
import ru.yandex.direct.core.entity.campaign.model.Wallet;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.campaign.repository.WalletRepository;
import ru.yandex.direct.core.entity.campaign.service.CampaignService;
import ru.yandex.direct.core.entity.campaign.service.CommonCampaignService;
import ru.yandex.direct.core.entity.campaign.service.WalletService;
import ru.yandex.direct.core.entity.client.repository.ClientRepository;
import ru.yandex.direct.core.entity.client.service.AddClientService;
import ru.yandex.direct.core.entity.client.service.ClientNdsService;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.feature.service.FeatureManagingService;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.user.model.ApiEnabled;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.core.entity.user.service.ApiUserService;
import ru.yandex.direct.core.entity.user.service.BlackboxUserService;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.validation.defects.Defects;
import ru.yandex.direct.currency.currencies.CurrencyRub;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.model.LoginOrUid;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.intapi.entity.clients.model.CheckClientStateResponse;
import ru.yandex.direct.intapi.entity.clients.model.ClientRole;
import ru.yandex.direct.intapi.entity.clients.model.ClientState;
import ru.yandex.direct.rbac.PpcRbac;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.result.Result;
import ru.yandex.direct.validation.result.ValidationResult;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxCorrectResponse;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class IntapiClientServiceCheckOnlyTest {
    private static final String DEFAULT_LOGIN = "abc";
    private static final Long DEFAULT_UID = 1L;
    private static final ClientId DEFAULT_CLIENT_ID = ClientId.fromLong(2L);

    @Parameterized.Parameter()
    public Boolean isApiOfferAccepted;

    @Parameterized.Parameter(1)
    public Boolean isApiBlocked;

    @Parameterized.Parameter(2)
    public Boolean isInDirect;

    @Parameterized.Parameter(3)
    public Boolean isDirectBlocked;

    @Parameterized.Parameter(4)
    public ClientState expectedClientState;

    @Parameterized.Parameter(5)
    public RbacRole rbacRole;

    @Parameterized.Parameter(6)
    public Long agencyClientId;

    @Parameterized.Parameter(7)
    public Boolean isSuperSubclient;

    @Parameterized.Parameter(8)
    public ClientRole expectedClientRole;

    @Parameterized.Parameter(9)
    public Boolean walletEnabled;

    @Parameterized.Parameter(10)
    public Boolean expectedHasSharedWallet;

    @Parameterized.Parameter(11)
    public Result<BlackboxCorrectResponse> blackboxResult;

    @Parameterized.Parameter(12)
    public String expectedCanNotBeCreatedReason;

    private ApiUserService apiUserService;

    private IntapiClientService service;

    @Parameterized.Parameters
    public static Object[][] parameters() {
        return new Object[][]{
                // check client state
                {true, false, true, false, ClientState.API_ENABLED,
                        RbacRole.CLIENT, null, false, ClientRole.CLIENT,
                        null, false, null, null},
                {false, true, true, false, ClientState.API_BLOCKED,
                        RbacRole.CLIENT, null, false, ClientRole.CLIENT,
                        null, false, null, null},
                {true, true, true, false, ClientState.API_BLOCKED,
                        RbacRole.CLIENT, null, false, ClientRole.CLIENT,
                        null, false, null, null},
                {false, false, false, false, ClientState.NOT_EXISTS,
                        RbacRole.CLIENT, null, false, null,
                        null, null, Result.successful(mock(BlackboxCorrectResponse.class)), null},
                {false, false, false, false, ClientState.CAN_NOT_BE_CREATED,
                        RbacRole.CLIENT, null, false, null,
                        null, null, Result.broken(ValidationResult.failed(null, Defects.pddLogin())),
                        Defects.pddLogin().defectId().getCode()},
                {false, false, true, false, ClientState.API_DISABLED,
                        RbacRole.CLIENT, null, false, ClientRole.CLIENT,
                        null, false, null, null},
                {true, false, true, true, ClientState.BLOCKED,
                        RbacRole.CLIENT, null, false, ClientRole.CLIENT,
                        null, false, null, null},
                {false, true, true, true, ClientState.BLOCKED,
                        RbacRole.CLIENT, null, false, ClientRole.CLIENT,
                        null, false, null, null},
                {true, true, true, true, ClientState.BLOCKED,
                        RbacRole.CLIENT, null, false, ClientRole.CLIENT,
                        null, false, null, null},
                {false, false, true, true, ClientState.BLOCKED,
                        RbacRole.CLIENT, null, false, ClientRole.CLIENT,
                        null, false, null, null},
                // check client role
                {true, false, true, false, ClientState.API_ENABLED,
                        RbacRole.CLIENT, 1L, false, ClientRole.AGENCY_READ_ONLY_SUBCLIENT,
                        null, false, null, null},
                {true, false, true, false, ClientState.API_ENABLED,
                        RbacRole.CLIENT, 1L, true, ClientRole.AGENCY_SUBCLIENT,
                        null, false, null, null},
                {true, false, true, false, ClientState.API_ENABLED,
                        RbacRole.AGENCY, null, false, ClientRole.AGENCY,
                        null, false, null, null},
                {true, false, true, false, ClientState.API_ENABLED,
                        RbacRole.MANAGER, null, false, ClientRole.OTHER,
                        null, false, null, null},
                {true, false, true, false, ClientState.API_ENABLED,
                        RbacRole.SUPPORT, null, false, ClientRole.OTHER,
                        null, false, null, null},
                // check shared account existence
                {true, false, true, false, ClientState.API_ENABLED,
                        RbacRole.CLIENT, null, false, ClientRole.CLIENT,
                        false, false, null, null},
                {true, false, true, false, ClientState.API_ENABLED,
                        RbacRole.CLIENT, null, false, ClientRole.CLIENT,
                        true, true, null, null},
        };
    }

    @Before
    public void setUp() {
        ClientService clientService = mock(ClientService.class);
        when(clientService.getWorkCurrency(any(ClientId.class))).thenReturn(CurrencyRub.getInstance());
        when(clientService.isSuperSubclient(eq(DEFAULT_CLIENT_ID))).thenReturn(isSuperSubclient);

        FeatureService featureService = mock(FeatureService.class);
        AddClientService addClientService = mock(AddClientService.class);
        BlackboxUserService blackboxUserService = mock(BlackboxUserService.class);
        when(blackboxUserService.getAndCheckUser(any(LoginOrUid.class))).thenReturn(blackboxResult);

        ApiFinanceTokensRepository apiFinanceTokensRepository = mock(ApiFinanceTokensRepository.class);
        FeatureManagingService featureManagingService = mock(FeatureManagingService.class);
        ShardHelper shardHelper = mock(ShardHelper.class);
        when(shardHelper.getShardByClientId(any(ClientId.class))).thenReturn(1);

        UserService userService = mock(UserService.class);
        ClientRepository clientRepository = mock(ClientRepository.class);

        apiUserService = mock(ApiUserService.class);
        PpcRbac ppcRbac = mock(PpcRbac.class);
        WalletRepository walletRepository = mock(WalletRepository.class);
        when(walletRepository.getWalletForCampaigns(anyInt(), eq(DEFAULT_UID), isNull()))
                .thenReturn(walletEnabled == null ? null
                        : new Wallet(0L, 1L, DEFAULT_CLIENT_ID, walletEnabled, CurrencyRub.getInstance()));
        WalletService walletService = new WalletService(userService, walletRepository, shardHelper,
                clientService, mock(CampaignRepository.class), mock(DslContextProvider.class),
                mock(CommonCampaignService.class), mock(RbacService.class));

        CampaignService campaignService = mock(CampaignService.class);
        ClientNdsService clientNdsService = mock(ClientNdsService.class);
        CommunicationEventService communicationEventService = mock(CommunicationEventService.class);

        service = new IntapiClientService(clientService, featureService, addClientService,
                userService, apiFinanceTokensRepository, apiUserService, featureManagingService, shardHelper,
                clientRepository, ppcRbac, walletService, blackboxUserService,
                clientNdsService, campaignService, communicationEventService);
    }

    @Test
    public void testClientStateProperlyReturnedForLogin() {
        LoginOrUid loginOrUid = LoginOrUid.of(DEFAULT_LOGIN, null);
        when(apiUserService.getUser(anyString())).thenReturn(userForAnswer());
        when(apiUserService.getUser(anyLong())).thenReturn(null);
        testClientStateInternal(loginOrUid);
    }

    @Test
    public void testClientStateProperlyReturnedForUid() {
        LoginOrUid loginOrUid = LoginOrUid.of(null, DEFAULT_UID);
        when(apiUserService.getUser(any(String.class))).thenReturn(null);
        when(apiUserService.getUser(anyLong())).thenReturn(userForAnswer());
        testClientStateInternal(loginOrUid);
    }

    @Test
    public void testClientStateProperlyReturnedForLoginStateOnly() {
        LoginOrUid loginOrUid = LoginOrUid.of(DEFAULT_LOGIN, null);
        when(apiUserService.getUser(anyString())).thenReturn(userForAnswer());
        when(apiUserService.getUser(anyLong())).thenReturn(null);

        CheckClientStateResponse response = service.checkClientState(loginOrUid, true, false);
        assertThat(response.getClientState(), is(expectedClientState));
        assertThat(response.getClientRole(), nullValue());
        assertThat(response.getHasSharedWallet(), nullValue());
    }

    private void testClientStateInternal(LoginOrUid loginOrUid) {
        CheckClientStateResponse response = service.checkClientState(loginOrUid);
        assertThat(response.getClientState(), is(expectedClientState));
        assertThat(response.getClientRole(), is(expectedClientRole));
        assertThat(response.getHasSharedWallet(), is(expectedHasSharedWallet));
        assertThat(response.getCanNotBeCreatedReason(), is(expectedCanNotBeCreatedReason));
    }

    private ApiUser userForAnswer() {
        return !isInDirect ? null : new ApiUser()
                .withUid(DEFAULT_UID)
                .withChiefUid(DEFAULT_UID)
                .withLogin(DEFAULT_LOGIN)
                .withClientId(DEFAULT_CLIENT_ID)
                .withStatusBlocked(isDirectBlocked)
                .withRole(rbacRole)
                .withAgencyClientId(agencyClientId)
                .withApiOfferAccepted(isApiOfferAccepted)
                .withApiEnabled(isApiBlocked ? ApiEnabled.NO : ApiEnabled.DEFAULT);
    }
}
