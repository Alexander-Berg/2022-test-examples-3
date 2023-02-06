package ru.yandex.direct.core.entity.campaign.service.operation;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import jdk.jfr.Description;
import one.util.streamex.StreamEx;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.campaign.model.BaseCampaign;
import ru.yandex.direct.core.entity.campaign.model.BillingAggregateCampaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusBsSynced;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusModerate;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusPostmoderate;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.SmsFlag;
import ru.yandex.direct.core.entity.campaign.model.WalletTypedCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignModifyRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.CampaignOptions;
import ru.yandex.direct.core.entity.campaign.service.RequestBasedMetrikaClientFactory;
import ru.yandex.direct.core.entity.campaign.service.RestrictedCampaignsAddOperation;
import ru.yandex.direct.core.entity.campaign.service.type.add.CampaignAddOperationSupportFacade;
import ru.yandex.direct.core.entity.campaign.service.validation.AddRestrictedCampaignValidationService;
import ru.yandex.direct.core.entity.product.model.Product;
import ru.yandex.direct.core.entity.product.model.ProductType;
import ru.yandex.direct.core.entity.product.repository.ProductRepository;
import ru.yandex.direct.core.entity.retargeting.service.common.GoalUtilsService;
import ru.yandex.direct.core.entity.strategy.repository.StrategyTypedRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbutil.model.UidAndClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.metrika.client.MetrikaClient;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.result.MassResult;

import static org.apache.commons.lang.math.RandomUtils.nextInt;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.common.util.RepositoryUtils.NOW_PLACEHOLDER;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_CAMPAIGN_WARNING_BALANCE;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_SMS_TIME_INTERVAL;
import static ru.yandex.direct.core.entity.product.ProductConstants.AGGREGATE_NAME_BY_PRODUCT_TYPE;
import static ru.yandex.direct.core.entity.product.service.ProductService.BUCKS;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultCpmBannerCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultDynamicCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultTextCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.newBillingAggregate;
import static ru.yandex.direct.core.testing.data.TestProducts.defaultProduct;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class RestrictedCampaignAddOperationBillingAggregatesTest {
    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StrategyTypedRepository strategyTypedRepository;

    @Autowired
    private CampaignTypedRepository campaignTypedRepository;

    @Autowired
    private CampaignModifyRepository campaignModifyRepository;

    @Autowired
    private AddRestrictedCampaignValidationService addRestrictedCampaignValidationService;

    @Autowired
    CampaignAddOperationSupportFacade campaignAddOperationSupportFacade;

    @Autowired
    private Steps steps;

    @Autowired
    public DslContextProvider dslContextProvider;
    @Autowired
    public RbacService rbacService;
    @Autowired
    public MetrikaClient metrikaClient;
    @Autowired
    private RequestBasedMetrikaClientFactory metrikaClientFactory;
    @Autowired
    private GoalUtilsService goalUtilsService;
    private ClientInfo defaultClient;

    @Before
    public void before() {
        defaultClient = steps.userSteps().createDefaultUser().getClientInfo();

        Set<ProductType> toAdd = Set.of(
                ProductType.CPM_VIDEO,
                ProductType.CPM_BANNER,
                ProductType.CPM_INDOOR,
                ProductType.CPM_AUDIO,
                ProductType.CPM_OUTDOOR
        );
        steps.productSteps()
                .addProductsIfNotExists(StreamEx.of(toAdd)
                        .map(productType -> defaultProduct()
                                .withId((long) nextInt())
                                .withType(productType)
                                .withUnitName(BUCKS)
                                .withCurrencyCode(defaultClient.getClient().getWorkCurrency())
                        ).toSet()
                );
    }

    @Test
    public void add_CheckTypesForCpmBanner() {
        RestrictedCampaignsAddOperation addOperation = new RestrictedCampaignsAddOperation(
                List.of(defaultCpmBannerCampaign()),
                defaultClient.getShard(),
                UidAndClientId.of(defaultClient.getUid(), defaultClient.getClientId()),
                defaultClient.getUid(),
                campaignModifyRepository,
                strategyTypedRepository,
                addRestrictedCampaignValidationService,
                campaignAddOperationSupportFacade, dslContextProvider, rbacService, new CampaignOptions(),
                metrikaClientFactory, goalUtilsService);

        MassResult<Long> result = addOperation.prepareAndApply();
        assertThat(result.getValidationResult().flattenErrors()).isEmpty();

        List<BillingAggregateCampaign> aggregates = getAggregates();
        assertThat(aggregates.size()).isEqualTo(5);

        List<Product> products = productRepository.getProductsById(StreamEx.of(aggregates)
                .map(BillingAggregateCampaign::getProductId)
                .toList());

        Set<ProductType> productTypesOfCreatedAggregates = StreamEx.of(products)
                .map(Product::getType)
                .toSet();

        assertThat(productTypesOfCreatedAggregates).containsExactlyInAnyOrder(ProductType.CPM_BANNER,
                ProductType.CPM_OUTDOOR, ProductType.CPM_INDOOR, ProductType.CPM_AUDIO, ProductType.CPM_VIDEO);
    }

    @Test
    @Description("Добавляем две кампании сразу, чтобы проверить, что нет ли проблем таких как было в DIRECT-152365")
    public void add_NotAddedForTextAndDynamic() {
        RestrictedCampaignsAddOperation addOperation = new RestrictedCampaignsAddOperation(
                List.of(defaultTextCampaign(), defaultDynamicCampaign()),
                defaultClient.getShard(),
                UidAndClientId.of(defaultClient.getUid(), defaultClient.getClientId()),
                defaultClient.getUid(),
                campaignModifyRepository,
                strategyTypedRepository,
                addRestrictedCampaignValidationService,
                campaignAddOperationSupportFacade, dslContextProvider, rbacService, new CampaignOptions(),
                metrikaClientFactory, goalUtilsService);

        MassResult<Long> result = addOperation.prepareAndApply();
        assertThat(result.getValidationResult().flattenErrors()).isEmpty();

        assertThat(getAggregates().size()).isEqualTo(0);
    }

    @Test
    public void add_NotAddedForText() {
        RestrictedCampaignsAddOperation addOperation = new RestrictedCampaignsAddOperation(
                List.of(defaultTextCampaign()),
                defaultClient.getShard(),
                UidAndClientId.of(defaultClient.getUid(), defaultClient.getClientId()),
                defaultClient.getUid(),
                campaignModifyRepository,
                strategyTypedRepository,
                addRestrictedCampaignValidationService,
                campaignAddOperationSupportFacade, dslContextProvider, rbacService, new CampaignOptions(),
                metrikaClientFactory, goalUtilsService);

        MassResult<Long> result = addOperation.prepareAndApply();
        assertThat(result.getValidationResult().flattenErrors()).isEmpty();

        assertThat(getAggregates().size()).isEqualTo(0);
    }

    @Test
    public void add_NotAddedForCpmWithFeatureDisableBillingAggregates() {
        steps.featureSteps().addClientFeature(defaultClient.getClientId(), FeatureName.DISABLE_BILLING_AGGREGATES, true);
        RestrictedCampaignsAddOperation addOperation = new RestrictedCampaignsAddOperation(
                List.of(defaultCpmBannerCampaign()),
                defaultClient.getShard(),
                UidAndClientId.of(defaultClient.getUid(), defaultClient.getClientId()),
                defaultClient.getUid(),
                campaignModifyRepository,
                strategyTypedRepository,
                addRestrictedCampaignValidationService,
                campaignAddOperationSupportFacade, dslContextProvider, rbacService, new CampaignOptions(),
                metrikaClientFactory, goalUtilsService);

        MassResult<Long> result = addOperation.prepareAndApply();
        assertThat(result.getValidationResult().flattenErrors()).isEmpty();

        assertThat(getAggregates().size()).isEqualTo(0);
    }

    @Test
    public void add_CreateMissingAggregates() {
        CampaignInfo camp = steps.campaignSteps().createActiveCampaignUnderWallet(defaultClient);
        Long productId = StreamEx.of(productRepository.getAllProducts()).
                filter(product -> product.getType() == ProductType.CPM_BANNER &&
                        product.getCurrencyCode() == CurrencyCode.RUB &&
                        product.getUnitName().equals(BUCKS))
                .map(Product::getId)
                .findFirst()
                .get();

        steps.campaignSteps().createCampaign(
                newBillingAggregate(defaultClient.getClientId(), defaultClient.getUid())
                        .withBalanceInfo(camp.getCampaign().getBalanceInfo().withProductId(productId)), defaultClient);

        RestrictedCampaignsAddOperation addOperation = new RestrictedCampaignsAddOperation(
                List.of(defaultCpmBannerCampaign()),
                defaultClient.getShard(),
                UidAndClientId.of(defaultClient.getUid(), defaultClient.getClientId()),
                defaultClient.getUid(),
                campaignModifyRepository,
                strategyTypedRepository,
                addRestrictedCampaignValidationService,
                campaignAddOperationSupportFacade, dslContextProvider, rbacService, new CampaignOptions(),
                metrikaClientFactory, goalUtilsService);

        MassResult<Long> result = addOperation.prepareAndApply();
        assertThat(result.getValidationResult().flattenErrors()).isEmpty();

        List<BillingAggregateCampaign> aggregates = getAggregates();
        assertThat(aggregates.size()).isEqualTo(5);

        List<Product> products = productRepository.getProductsById(StreamEx.of(aggregates)
                .map(BillingAggregateCampaign::getProductId)
                .toList());

        Set<ProductType> productTypesOfCreatedAggregates = StreamEx.of(products)
                .map(Product::getType)
                .toSet();

        assertThat(productTypesOfCreatedAggregates).containsExactlyInAnyOrder(ProductType.CPM_BANNER,
                ProductType.CPM_OUTDOOR,
                ProductType.CPM_INDOOR, ProductType.CPM_AUDIO, ProductType.CPM_VIDEO);
    }

    @Test
    public void add_CheckBillingAggregatorStructure() {
        RestrictedCampaignsAddOperation addOperation = new RestrictedCampaignsAddOperation(
                List.of(defaultCpmBannerCampaign()),
                defaultClient.getShard(),
                UidAndClientId.of(defaultClient.getUid(), defaultClient.getClientId()),
                defaultClient.getUid(),
                campaignModifyRepository,
                strategyTypedRepository,
                addRestrictedCampaignValidationService,
                campaignAddOperationSupportFacade, dslContextProvider, rbacService, new CampaignOptions(),
                metrikaClientFactory, goalUtilsService);

        MassResult<Long> result = addOperation.prepareAndApply();
        assertThat(result.getValidationResult().flattenErrors()).isEmpty();

        List<BillingAggregateCampaign> aggregates = getAggregates();
        assertThat(aggregates.size()).isEqualTo(5);

        List<Product> products = productRepository.getProductsById(StreamEx.of(aggregates)
                .map(BillingAggregateCampaign::getProductId)
                .toList());

        Long cpmBannerProductId = StreamEx.of(products)
                .filter(product -> product.getType() == ProductType.CPM_BANNER &&
                        product.getCurrencyCode() == defaultClient.getClient().getWorkCurrency() &&
                        product.getUnitName().equals(BUCKS))
                .map(Product::getId).findFirst().get();

        BillingAggregateCampaign expected = getExpectedBillingAggregateCampaign(getWallet(), cpmBannerProductId,
                ProductType.CPM_BANNER);
        BillingAggregateCampaign actual = StreamEx.of(aggregates)
                .filter(billingAggregate -> billingAggregate.getProductId().equals(cpmBannerProductId))
                .findFirst()
                .get();
        assertThat(actual).is(matchedBy(beanDiffer(expected)
                .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())));
    }

    private static BillingAggregateCampaign getExpectedBillingAggregateCampaign(WalletTypedCampaign wallet,
                                                                                Long productId,
                                                                                ProductType productType) {
        return new BillingAggregateCampaign()
                .withOrderId(0L)
                .withUid(wallet.getUid())
                .withClientId(wallet.getClientId())
                .withAgencyId(wallet.getAgencyId())
                .withAgencyUid(wallet.getAgencyUid())
                .withManagerUid(wallet.getManagerUid())
                .withFio(wallet.getFio())
                .withEmail(wallet.getEmail())
                .withCurrency(wallet.getCurrency())
                .withStatusBsSynced(CampaignStatusBsSynced.NO)
                .withStatusActive(false)
                .withStatusEmpty(false)
                .withStatusModerate(CampaignStatusModerate.YES)
                .withStatusPostModerate(CampaignStatusPostmoderate.ACCEPTED)
                .withStatusShow(true)
                .withName(AGGREGATE_NAME_BY_PRODUCT_TYPE.get(productType))
                .withProductId(productId)
                .withType(CampaignType.BILLING_AGGREGATE)
                .withWalletId(wallet.getId())
                .withLastChange(NOW_PLACEHOLDER)
                .withEnablePausedByDayBudgetEvent(true)
                .withEnableSendAccountNews(true)
                .withSmsTime(DEFAULT_SMS_TIME_INTERVAL)
                .withTimeZoneId(0L)
                .withFio(wallet.getFio())
                .withWarningBalance(DEFAULT_CAMPAIGN_WARNING_BALANCE)
                .withSmsFlags(EnumSet.of(SmsFlag.PAUSED_BY_DAY_BUDGET_SMS))
                .withEmail(wallet.getEmail())
                .withIsServiceRequested(false)
                .withIsSumAggregated(null)
                .withSum(null)
                .withSumToPay(null)
                .withSumLast(null)
                .withSumSpent(null)
                .withLastChange(null);
    }

    private List<BillingAggregateCampaign> getAggregates() {
        Collection<? extends BaseCampaign> billingAggregateCampaigns =
                campaignTypedRepository.getClientsTypedCampaignsByType(defaultClient.getShard(),
                        defaultClient.getClientId(),
                        Set.of(CampaignType.BILLING_AGGREGATE)).values();
        return StreamEx.of(billingAggregateCampaigns)
                .select(BillingAggregateCampaign.class)
                .toList();
    }

    private WalletTypedCampaign getWallet() {
        Collection<? extends BaseCampaign> wallets =
                campaignTypedRepository.getClientsTypedCampaignsByType(defaultClient.getShard(),
                        defaultClient.getClientId(),
                        Set.of(CampaignType.WALLET)).values();
        return StreamEx.of(wallets)
                .select(WalletTypedCampaign.class)
                .findFirst()
                .get();
    }

}
