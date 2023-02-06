package ru.yandex.direct.grid.processing.service.autooverdraft;

import java.math.BigDecimal;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.WalletRestMoney;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.campaign.service.CampaignService;
import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.entity.client.repository.ClientOptionsRepository;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.service.integration.balance.BalanceService;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.currency.Money;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.autooverdraft.GdPersonPaymentMethodInfo;
import ru.yandex.direct.grid.processing.model.client.GdClient;
import ru.yandex.direct.grid.processing.model.client.GdClientInfo;
import ru.yandex.direct.grid.processing.service.validation.GridValidationService;
import ru.yandex.direct.grid.processing.util.ContextHelper;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonMap;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

public class AutoOverdraftDataServiceTest {

    private static final long WALLET_ID = 123456L;
    private static final BigDecimal OVERDRAFT_LIMIT = BigDecimal.TEN;
    private static GridGraphQLContext gridGraphQLContext;
    private static GdClientInfo clientInfo;
    private static GdClient client;
    private static Campaign wallet;

    @Mock
    private FeatureService featureService;
    @Mock
    private CampaignRepository campaignRepository;
    @Mock
    private CampaignService campaignsService;
    @Mock
    private BalanceService balanceService;
    @Mock
    private ClientService clientService;
    @Mock
    private ClientOptionsRepository clientOptionsRepository;
    @Mock
    private DslContextProvider dslContextProvider;
    @Mock
    private ShardHelper shardHelper;
    @Mock
    private GridValidationService gridValidationService;
    @Mock
    private PpcPropertiesSupport ppcPropertiesSupport;

    @InjectMocks
    private AutoOverdraftDataService autoOverdraftDataService;

    @BeforeClass
    public static void initTestData() {
        gridGraphQLContext = ContextHelper.buildDefaultContext();
        clientInfo = gridGraphQLContext.getQueriedClient();
        client = new GdClient().withInfo(clientInfo);
        wallet = new Campaign()
                .withType(CampaignType.WALLET)
                .withId(WALLET_ID)
                .withStatusArchived(false);
    }

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        doReturn(new Client().withClientId(clientInfo.getId()).withIsBrand(false).withOverdraftLimit(OVERDRAFT_LIMIT))
                .when(clientService)
                .getClient(eq(ClientId.fromLong(clientInfo.getId())));
        doReturn(ImmutableList.of(wallet))
                .when(campaignsService)
                .searchNotEmptyCampaignsByClientIdAndTypes(eq(ClientId.fromLong(clientInfo.getId())), any());
        WalletRestMoney walletRestMoney =
                new WalletRestMoney()
                        .withWalletId(wallet.getId())
                        .withRest(Money.valueOf(1_000, CurrencyCode.RUB));
        doReturn(ImmutableMap.of(wallet.getId(), walletRestMoney))
                .when(campaignsService)
                .getWalletsRestMoney(eq(ClientId.fromLong(clientInfo.getId())), any());
    }

    @Test
    public void successGetPaymentOptions() {
        List<GdPersonPaymentMethodInfo> personPaymentMethods =
                autoOverdraftDataService.getPersonPaymentMethods(gridGraphQLContext, client);
    }

    @Test(expected = IllegalStateException.class)
    public void walletNotFoundFromGetPaymentOptions() {
        doReturn(ImmutableList.copyOf(emptyList()))
                .when(campaignsService)
                .searchNotEmptyCampaignsByClientIdAndTypes(eq(ClientId.fromLong(clientInfo.getId())), any());
        List<GdPersonPaymentMethodInfo> personPaymentMethods =
                autoOverdraftDataService.getPersonPaymentMethods(gridGraphQLContext, client);
    }

    @Test(expected = IllegalStateException.class)
    public void clientNotFoundFromGetPaymentOptions() {
        doReturn(null)
                .when(clientService)
                .getClient(eq(ClientId.fromLong(clientInfo.getId())));
        List<GdPersonPaymentMethodInfo> personPaymentMethods =
                autoOverdraftDataService.getPersonPaymentMethods(gridGraphQLContext, client);
    }

    @Test(expected = IllegalStateException.class)
    public void clientIsBrandFromGetPaymentOptions() {
        doReturn(new Client().withClientId(clientInfo.getId()).withIsBrand(true).withOverdraftLimit(OVERDRAFT_LIMIT))
                .when(clientService)
                .getClient(eq(ClientId.fromLong(clientInfo.getId())));
        List<GdPersonPaymentMethodInfo> personPaymentMethods =
                autoOverdraftDataService.getPersonPaymentMethods(gridGraphQLContext, client);
    }

    @Test(expected = IllegalStateException.class)
    public void clientHasZeroOverdraftLimitFromGetPaymentOptions() {
        doReturn(new Client().withClientId(clientInfo.getId()).withIsBrand(false).withOverdraftLimit(BigDecimal.ZERO))
                .when(clientService)
                .getClient(eq(ClientId.fromLong(clientInfo.getId())));
        List<GdPersonPaymentMethodInfo> personPaymentMethods =
                autoOverdraftDataService.getPersonPaymentMethods(gridGraphQLContext, client);
    }

    @Test(expected = IllegalStateException.class)
    public void clientWalletHasWrongCurrency() {
        WalletRestMoney walletRestMoney =
                new WalletRestMoney()
                        .withWalletId(wallet.getId())
                        .withRest(Money.valueOf(1_000, CurrencyCode.YND_FIXED));
        doReturn(singletonMap(WALLET_ID, walletRestMoney))
                .when(campaignsService)
                .getWalletsRestMoney(eq(ClientId.fromLong(clientInfo.getId())), any());
        List<GdPersonPaymentMethodInfo> personPaymentMethods =
                autoOverdraftDataService.getPersonPaymentMethods(gridGraphQLContext, client);
    }
}
