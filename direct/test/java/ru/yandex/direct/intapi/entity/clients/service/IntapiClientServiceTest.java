package ru.yandex.direct.intapi.entity.clients.service;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.direct.communication.service.CommunicationEventService;
import ru.yandex.direct.core.entity.apifinancetokens.repository.ApiFinanceTokensRepository;
import ru.yandex.direct.core.entity.campaign.service.CampaignService;
import ru.yandex.direct.core.entity.campaign.service.WalletService;
import ru.yandex.direct.core.entity.client.repository.ClientRepository;
import ru.yandex.direct.core.entity.client.service.AddClientOptions;
import ru.yandex.direct.core.entity.client.service.AddClientService;
import ru.yandex.direct.core.entity.client.service.ClientNdsService;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.feature.service.FeatureManagingService;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.service.ApiUserService;
import ru.yandex.direct.core.entity.user.service.BlackboxUserService;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.model.LoginOrUid;
import ru.yandex.direct.dbutil.model.UidAndClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.i18n.Language;
import ru.yandex.direct.intapi.entity.clients.model.ClientsAddOrGetResponse;
import ru.yandex.direct.rbac.PpcRbac;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.result.Result;
import ru.yandex.inside.passport.PassportDomain;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxAliases;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxCorrectResponse;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxOAuthStatus;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxResponseBuilder;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.currency.CurrencyCode.TRY;
import static ru.yandex.direct.regions.Region.TURKEY_REGION_ID;
import static ru.yandex.inside.passport.blackbox2.protocol.BlackboxMethod.USER_INFO;

@RunWith(Parameterized.class)
public class IntapiClientServiceTest {
    private static final User USER = new User()
            .withLogin("testlogin")
            .withUid(1L)
            .withClientId(ClientId.fromLong(19L))
            .withFio("test fio")
            .withRole(RbacRole.CLIENT)
            .withEmail("email@ya.ru")
            .withLang(Language.TR);

    private static final String PHONE_ALIAS_LOGIN = "79991234567";

    private static final BlackboxCorrectResponse BLACKBOX_USER_RESPONSE = new BlackboxResponseBuilder(USER_INFO)
            .setStatus(BlackboxOAuthStatus.VALID.value())
            .setLogin(Optional.of(USER.getLogin()))
            .setUidDomain(Optional.of(Tuple2.tuple(PassportUid.cons(USER.getUid()), PassportDomain.YANDEX_RU)))
            .setAliases(Map.of(BlackboxAliases.PORTAL, USER.getLogin()))
            .build()
            .getOrThrow();

    private static final String TOKEN = "1234";

    @Parameterized.Parameter
    public LoginOrUid loginOrUid;

    private AddClientService addClientService;
    private UserService userService;
    private ApiFinanceTokensRepository apiFinanceTokensRepository;
    private ShardHelper shardHelper;
    private ClientRepository clientRepository;
    private CampaignService campaignService;
    private ClientNdsService clientNdsService;

    private IntapiClientService service;

    @Parameterized.Parameters
    public static Iterable<Object[]> parameters() {
        return Arrays.asList(
                new Object[]{LoginOrUid.of(USER.getLogin(), null)},
                new Object[]{LoginOrUid.of(null, USER.getUid())},
                new Object[]{LoginOrUid.of(PHONE_ALIAS_LOGIN, null)}
        );
    }

    @Before
    public void setUp() {
        ClientService clientService = mock(ClientService.class);
        FeatureService featureService = mock(FeatureService.class);
        addClientService = mock(AddClientService.class);
        userService = mock(UserService.class);
        apiFinanceTokensRepository = mock(ApiFinanceTokensRepository.class);
        ApiUserService apiUserService = mock(ApiUserService.class);
        FeatureManagingService featureManagingService = mock(FeatureManagingService.class);
        shardHelper = mock(ShardHelper.class);
        clientRepository = mock(ClientRepository.class);
        PpcRbac ppcRbac = mock(PpcRbac.class);
        campaignService = mock(CampaignService.class);
        clientNdsService = mock(ClientNdsService.class);
        WalletService walletService = mock(WalletService.class);
        BlackboxUserService blackboxUserService = mock(BlackboxUserService.class);
        CommunicationEventService communicationEventService = mock(CommunicationEventService.class);
        when(blackboxUserService.getAndCheckUser(eq(loginOrUid)))
                .thenReturn(Result.successful(BLACKBOX_USER_RESPONSE));
        when(blackboxUserService.getAndCheckUser(eq(LoginOrUid.of(PHONE_ALIAS_LOGIN))))
                .thenReturn(Result.successful(BLACKBOX_USER_RESPONSE));

        service = new IntapiClientService(clientService, featureService, addClientService,
                userService, apiFinanceTokensRepository, apiUserService, featureManagingService, shardHelper,
                clientRepository, ppcRbac, walletService, blackboxUserService,
                clientNdsService, campaignService, communicationEventService);
    }

    @Test
    public void addOrGetClient_existingClient_existingToken() {
        when(userService.getUserByLogin(USER.getLogin())).thenReturn(USER);
        when(userService.getUser(USER.getUid())).thenReturn(USER);
        when(apiFinanceTokensRepository.getMasterToken(USER.getUid())).thenReturn(Optional.of(TOKEN));
        ClientsAddOrGetResponse response = (ClientsAddOrGetResponse) service
                .addOrGetClient(loginOrUid, "test fio", TURKEY_REGION_ID, TRY, true);
        assertThat(response.getUserId(), is(USER.getUid()));
        assertThat(response.getClientId(), is(USER.getClientId().asLong()));
        assertThat(response.getLogin(), is(USER.getLogin()));
        assertThat(response.getFinanceToken(), is(TOKEN));
    }

    @Test
    public void addOrGetClient_existingClient_newToken() {
        when(userService.getUserByLogin(USER.getLogin())).thenReturn(USER);
        when(userService.getUser(USER.getUid())).thenReturn(USER);
        when(apiFinanceTokensRepository.getMasterToken(USER.getUid())).thenReturn(Optional.empty());
        when(apiFinanceTokensRepository.createAndGetMasterToken(USER.getUid())).thenReturn(TOKEN);
        ClientsAddOrGetResponse response = (ClientsAddOrGetResponse) service
                .addOrGetClient(loginOrUid, "test fio", TURKEY_REGION_ID, TRY, true);
        assertThat(response.getUserId(), is(USER.getUid()));
        assertThat(response.getClientId(), is(USER.getClientId().asLong()));
        assertThat(response.getLogin(), is(USER.getLogin()));
        assertThat(response.getFinanceToken(), is(TOKEN));
    }

    @Test
    public void addOrGetClient_existingClient_aliasLogin() {
        // С телефонным алиасом пользователя в БД нет, т.к. пользователь существует под основным логином
        when(userService.getUserByLogin(USER.getLogin())).thenReturn(null);
        when(userService.getUser(USER.getUid())).thenReturn(USER);
        when(apiFinanceTokensRepository.getMasterToken(USER.getUid())).thenReturn(Optional.empty());
        when(apiFinanceTokensRepository.createAndGetMasterToken(USER.getUid())).thenReturn(TOKEN);
        ClientsAddOrGetResponse response = (ClientsAddOrGetResponse) service
                .addOrGetClient(loginOrUid, "test fio", TURKEY_REGION_ID, TRY, true);
        assertThat(response.getUserId(), is(USER.getUid()));
        assertThat(response.getClientId(), is(USER.getClientId().asLong()));
        assertThat(response.getLogin(), is(USER.getLogin()));
        assertThat(response.getFinanceToken(), is(TOKEN));
    }

    @Test
    public void addOrGetClient_newClient_newToken() {
        when(userService.getUserByLogin(USER.getLogin())).thenReturn(null);
        when(userService.getUser(USER.getUid())).thenReturn(null);
        when(addClientService.processRequest(any(), eq(loginOrUid), eq("test fio"), eq(TURKEY_REGION_ID),
                eq(TRY), eq(RbacRole.CLIENT), any(AddClientOptions.class)))
                .thenReturn(Result.successful(UidAndClientId.of(USER.getUid(), USER.getClientId())));
        when(apiFinanceTokensRepository.getMasterToken(USER.getUid())).thenReturn(Optional.empty());
        when(apiFinanceTokensRepository.createAndGetMasterToken(USER.getUid())).thenReturn(TOKEN);
        ClientsAddOrGetResponse response = (ClientsAddOrGetResponse) service
                .addOrGetClient(loginOrUid, "test fio", TURKEY_REGION_ID, TRY, true);
        assertThat(response.getUserId(), is(USER.getUid()));
        assertThat(response.getClientId(), is(USER.getClientId().asLong()));
        assertThat(response.getLogin(), is(USER.getLogin()));
        assertThat(response.getFinanceToken(), is(TOKEN));
    }
}
