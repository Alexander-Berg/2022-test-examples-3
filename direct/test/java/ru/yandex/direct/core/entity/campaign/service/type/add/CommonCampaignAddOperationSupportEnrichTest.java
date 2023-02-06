package ru.yandex.direct.core.entity.campaign.service.type.add;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import ru.yandex.direct.core.entity.balance.service.BalanceInfoQueueService;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusBsSynced;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusModerate;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusPostmoderate;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CampaignTypeKinds;
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign;
import ru.yandex.direct.core.entity.campaign.model.Wallet;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.campaign.service.CampaignOptions;
import ru.yandex.direct.core.entity.campaign.service.WalletService;
import ru.yandex.direct.core.entity.campaign.service.type.add.container.AddServicedCampaignInfo;
import ru.yandex.direct.core.entity.campaign.service.type.add.container.RestrictedCampaignsAddOperationContainer;
import ru.yandex.direct.core.entity.campaign.service.type.add.container.RestrictedCampaignsAddOperationContainerImpl;
import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.notification.NotificationService;
import ru.yandex.direct.core.entity.product.model.Product;
import ru.yandex.direct.core.entity.product.model.ProductSimple;
import ru.yandex.direct.core.entity.product.model.ProductType;
import ru.yandex.direct.core.entity.product.service.ProductService;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.model.UidAndClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.rbac.PpcRbac;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.direct.common.util.RepositoryUtils.NOW_PLACEHOLDER;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultCampaignByCampaignType;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;
import static ru.yandex.direct.core.testing.data.TestUsers.defaultUser;

@RunWith(Parameterized.class)
public class CommonCampaignAddOperationSupportEnrichTest {
    private static final int SHARD = 1;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private AddServicedCampaignService addServicedCampaignService;
    @Mock
    private ClientService clientService;
    @Mock
    private RbacService rbacService;
    @Mock
    private PpcRbac ppcRbac;
    @Mock
    private WalletService walletService;
    @Mock
    private ProductService productService;
    @Mock
    private UserService userService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private BalanceInfoQueueService balanceInfoQueueService;
    @Mock
    private DslContextProvider ppcDslContextProvider;
    @Mock
    private ShardHelper shardHelper;

    @Mock
    private CampaignRepository campaignRepository;

    @InjectMocks
    private CommonCampaignAddOperationSupport support;

    private CampaignAddOperationSupportFacade campaignAddOperationSupportFacade;

    private Long operatorUid;
    private Long operatorClientId;

    private ClientId clientId;
    private Long uid;

    private User user;

    private Long chiefUid;
    private long productId;
    private RestrictedCampaignsAddOperationContainer addCampaignParametersContainer;
    private long campaignId;
    private String name;

    @Parameterized.Parameter
    public CampaignType campaignType;

    @Parameterized.Parameters(name = "{0}")
    public static Collection typeOfCampaignParameter() {
        return Arrays.asList(new Object[][]{
                {CampaignType.TEXT},
                {CampaignType.PERFORMANCE},
                {CampaignType.MOBILE_CONTENT},
                {CampaignType.DYNAMIC},
                {CampaignType.MCBANNER},
                {CampaignType.INTERNAL_AUTOBUDGET},
                {CampaignType.INTERNAL_DISTRIB},
                {CampaignType.INTERNAL_FREE},
        });
    }

    @Before
    public void before() {
        operatorUid = RandomNumberUtils.nextPositiveLong();
        operatorClientId = RandomNumberUtils.nextPositiveLong();
        clientId = ClientId.fromLong(RandomNumberUtils.nextPositiveLong());
        uid = RandomNumberUtils.nextPositiveLong();
        chiefUid = RandomNumberUtils.nextPositiveLong();
        productId = RandomNumberUtils.nextPositiveLong();

        addCampaignParametersContainer = createDefaultAddCampaignContainer();

        if (campaignAddOperationSupportFacade == null) {
            campaignAddOperationSupportFacade = new CampaignAddOperationSupportFacade(List.of(support),
                    campaignRepository);
        }

        Client client = defaultClient().withClientId(clientId.asLong());
        user = defaultUser()
                .withClientId(clientId)
                .withId(uid);

        doReturn(client).when(clientService).getClient(eq(clientId));
        doReturn(user).when(userService).getUser(eq(uid));

        campaignId = RandomNumberUtils.nextPositiveLong();

        ProductSimple productSimple = new Product()
                .withType(ProductType.TEXT)
                .withId(productId);
        doReturn(productId).when(productService).calculateProductId(any(), any());
        doReturn(operatorClientId).when(shardHelper).getClientIdByUid(operatorUid);

        name = "name" + RandomStringUtils.randomAlphabetic(5);
    }

    @Test
    public void enrich_DefaultCampaign() {
        AddServicedCampaignInfo addServicedCampaignInfo = new AddServicedCampaignInfo()
                .withIsServiced(false);

        doReturn(List.of(addServicedCampaignInfo)).when(addServicedCampaignService)
                .getServicedInfoForNewCampaigns(same(addCampaignParametersContainer), any(List.class));

        CommonCampaign commonCampaign = defaultCampaignByCampaignType(campaignType)
                .withName(name)
                .withId(campaignId);

        campaignAddOperationSupportFacade.onPreValidated(addCampaignParametersContainer, List.of(commonCampaign));
        CommonCampaign expectedCommonCampaign = getCommonCampaignWithEnrichedFields()
                .withIsServiceRequested(false)
                .withStatusPostModerate(CampaignStatusPostmoderate.NEW);

        assertThat(commonCampaign).isEqualToIgnoringGivenFields(expectedCommonCampaign);
    }

    @Test
    public void enrich_DefaultCampaign_HasDisbaledWalletAndCampaigns_CampaignWalletIsZero() {
        AddServicedCampaignInfo addServicedCampaignInfo = new AddServicedCampaignInfo()
                .withIsServiced(false);

        doReturn(List.of(addServicedCampaignInfo)).when(addServicedCampaignService)
                .getServicedInfoForNewCampaigns(same(addCampaignParametersContainer), any(List.class));

        Wallet disabledWallet = new Wallet(RandomNumberUtils.nextPositiveLong(), RandomNumberUtils.nextPositiveLong(),
                addCampaignParametersContainer.getClientId(), false, CurrencyCode.RUB.getCurrency());
        doReturn(disabledWallet).when(walletService)
                .getWalletForNewCampaigns(eq(addCampaignParametersContainer), any());

        doReturn(false).when(campaignRepository)
                .isFirstCampaignsUnderWallet(eq(addCampaignParametersContainer.getShard()), any(), any(), any());

        CommonCampaign commonCampaign = defaultCampaignByCampaignType(campaignType)
                .withName(name)
                .withId(campaignId);

        campaignAddOperationSupportFacade.onPreValidated(addCampaignParametersContainer, List.of(commonCampaign));
        CommonCampaign expectedCommonCampaign = getCommonCampaignWithEnrichedFields()
                .withIsServiceRequested(false)
                .withStatusPostModerate(CampaignStatusPostmoderate.NEW);

        assertThat(commonCampaign).isEqualToIgnoringGivenFields(expectedCommonCampaign);
    }

    @Test
    public void enrich_DefaultCampaign_HasDisbaledWalletAndNoCampaigns_AddWalletIdToCampaign() {
        AddServicedCampaignInfo addServicedCampaignInfo = new AddServicedCampaignInfo()
                .withIsServiced(false);

        doReturn(List.of(addServicedCampaignInfo)).when(addServicedCampaignService)
                .getServicedInfoForNewCampaigns(same(addCampaignParametersContainer), any(List.class));

        Wallet wallet = new Wallet(RandomNumberUtils.nextPositiveLong(), RandomNumberUtils.nextPositiveLong(),
                addCampaignParametersContainer.getClientId(), false, CurrencyCode.RUB.getCurrency());
        doReturn(wallet).when(walletService)
                .getWalletForNewCampaigns(eq(addCampaignParametersContainer), any());

        doReturn(true).when(campaignRepository)
                .isFirstCampaignsUnderWallet(eq(addCampaignParametersContainer.getShard()), any(), any(), any());

        CommonCampaign commonCampaign = defaultCampaignByCampaignType(campaignType)
                .withName(name)
                .withId(campaignId);

        campaignAddOperationSupportFacade.onPreValidated(addCampaignParametersContainer, List.of(commonCampaign));
        CommonCampaign expectedCommonCampaign = getCommonCampaignWithEnrichedFields()
                .withIsServiceRequested(false)
                .withStatusPostModerate(CampaignStatusPostmoderate.NEW)
                .withWalletId(wallet.getWalletCampaignId());

        assertThat(commonCampaign).isEqualToIgnoringGivenFields(expectedCommonCampaign);
    }

    @Test
    public void enrich_DefaultCampaign_HasEnabledWalletAndCampaigns_AddWalletIdToCampaign() {
        AddServicedCampaignInfo addServicedCampaignInfo = new AddServicedCampaignInfo()
                .withIsServiced(false);

        doReturn(List.of(addServicedCampaignInfo)).when(addServicedCampaignService)
                .getServicedInfoForNewCampaigns(same(addCampaignParametersContainer), any(List.class));

        Wallet wallet = new Wallet(RandomNumberUtils.nextPositiveLong(), RandomNumberUtils.nextPositiveLong(),
                addCampaignParametersContainer.getClientId(), true, CurrencyCode.RUB.getCurrency());
        doReturn(wallet).when(walletService)
                .getWalletForNewCampaigns(eq(addCampaignParametersContainer), any());

        doReturn(false).when(campaignRepository)
                .isFirstCampaignsUnderWallet(eq(addCampaignParametersContainer.getShard()), any(), any(), any());

        CommonCampaign commonCampaign = defaultCampaignByCampaignType(campaignType)
                .withName(name)
                .withId(campaignId);

        campaignAddOperationSupportFacade.onPreValidated(addCampaignParametersContainer, List.of(commonCampaign));
        CommonCampaign expectedCommonCampaign = getCommonCampaignWithEnrichedFields()
                .withIsServiceRequested(false)
                .withStatusPostModerate(CampaignStatusPostmoderate.NEW)
                .withWalletId(wallet.getWalletCampaignId());

        assertThat(commonCampaign).isEqualToIgnoringGivenFields(expectedCommonCampaign);
    }


    @Test
    public void enrich_ServicedCampaign() {
        AddServicedCampaignInfo addServicedCampaignInfo = new AddServicedCampaignInfo()
                .withIsServiced(true)
                .withManagerUid(operatorUid);

        doReturn(List.of(addServicedCampaignInfo)).when(addServicedCampaignService)
                .getServicedInfoForNewCampaigns(same(addCampaignParametersContainer), any(List.class));


        CommonCampaign commonCampaign = defaultCampaignByCampaignType(campaignType)
                .withName(name)
                .withId(campaignId);

        campaignAddOperationSupportFacade.onPreValidated(addCampaignParametersContainer, List.of(commonCampaign));
        CommonCampaign expectedCommonCampaign = getCommonCampaignWithEnrichedFields()
                .withIsServiceRequested(false)
                .withStatusPostModerate(CampaignStatusPostmoderate.ACCEPTED)
                .withManagerUid(operatorUid);

        assertThat(commonCampaign).isEqualTo(expectedCommonCampaign);
    }

    @Test
    public void enrich_AgenciesCampaign() {
        doReturn(RbacRole.AGENCY).when(rbacService).getUidRole(eq(operatorUid));
        doReturn(Optional.empty()).when(ppcRbac).getUserPerminfo(operatorUid);
        doReturn(new Client()
                .withAgencyClientId(operatorClientId)
                .withAgencyUserId(operatorUid)
                .withWorkCurrency(CurrencyCode.RUB))
                .when(clientService)
                .getClient(eq(addCampaignParametersContainer.getClientId()));
        doReturn(UidAndClientId.of(operatorUid, ClientId.fromLong(operatorClientId)))
                .when(userService).getAgencyUidAndClientId(eq(operatorUid), anyLong(), anyLong());

        AddServicedCampaignInfo addServicedCampaignInfo = new AddServicedCampaignInfo()
                .withIsServiced(false);

        doReturn(List.of(addServicedCampaignInfo)).when(addServicedCampaignService)
                .getServicedInfoForNewCampaigns(same(addCampaignParametersContainer), any(List.class));

        CommonCampaign commonCampaign = defaultCampaignByCampaignType(campaignType)
                .withName(name)
                .withId(campaignId);

        campaignAddOperationSupportFacade.onPreValidated(addCampaignParametersContainer, List.of(commonCampaign));
        CommonCampaign expectedCommonCampaign = getCommonCampaignWithEnrichedFields()
                .withName(name)
                .withIsServiceRequested(false)
                .withStatusPostModerate(CampaignStatusPostmoderate.ACCEPTED)
                .withManagerUid(null)
                .withAgencyId(operatorClientId)
                .withAgencyUid(operatorUid);

        assertThat(commonCampaign).isEqualTo(expectedCommonCampaign);
    }

    @Test
    public void enrich_DefaultCampaign_ReadyToModerate() {
        addCampaignParametersContainer = createAddCampaignContainer(
                new CampaignOptions.Builder().withReadyToModerate(true).build());
        AddServicedCampaignInfo addServicedCampaignInfo = new AddServicedCampaignInfo()
                .withIsServiced(false);

        doReturn(List.of(addServicedCampaignInfo)).when(addServicedCampaignService)
                .getServicedInfoForNewCampaigns(same(addCampaignParametersContainer), any(List.class));

        CommonCampaign commonCampaign = defaultCampaignByCampaignType(campaignType)
                .withName(name)
                .withId(campaignId);

        campaignAddOperationSupportFacade.onPreValidated(addCampaignParametersContainer, List.of(commonCampaign));
        CommonCampaign expectedCommonCampaign = getCommonCampaignWithEnrichedFields()
                .withIsServiceRequested(false)
                .withStatusPostModerate(CampaignStatusPostmoderate.NEW)
                .withStatusModerate(CampaignStatusModerate.READY);

        assertThat(commonCampaign).isEqualToIgnoringGivenFields(expectedCommonCampaign);
    }

    private RestrictedCampaignsAddOperationContainerImpl createDefaultAddCampaignContainer() {
        return createAddCampaignContainer(new CampaignOptions());
    }

    private RestrictedCampaignsAddOperationContainerImpl createAddCampaignContainer(CampaignOptions options) {
        return new RestrictedCampaignsAddOperationContainerImpl(
                SHARD,
                operatorUid,
                clientId,
                uid,
                chiefUid,
                null,
                options,
                null,
                emptyMap()
        );
    }

    private CommonCampaign getCommonCampaignWithEnrichedFields() {
        boolean isInternalCampaign = CampaignTypeKinds.INTERNAL.contains(campaignType);

        return defaultCampaignByCampaignType(campaignType)
                .withName(name)
                .withStatusBsSynced(CampaignStatusBsSynced.NO)
                .withStatusModerate(isInternalCampaign ? CampaignStatusModerate.READY : CampaignStatusModerate.NEW)
                .withLastChange(NOW_PLACEHOLDER)
                .withStatusActive(false)
                .withStatusShow(!isInternalCampaign)
                .withStatusEmpty(true)
                .withStatusArchived(false)
                .withId(campaignId)
                .withUid(chiefUid)
                .withFio(user.getFio())
                .withClientId(clientId.asLong())
                .withAgencyId(0L)
                .withOrderId(0L)
                .withProductId(productId)
                .withWalletId(0L)
                .withSumToPay(BigDecimal.ZERO)
                .withSum(BigDecimal.ZERO)
                .withSumSpent(BigDecimal.ZERO)
                .withSumLast(BigDecimal.ZERO)
                .withCurrency(CurrencyCode.RUB);
    }
}
