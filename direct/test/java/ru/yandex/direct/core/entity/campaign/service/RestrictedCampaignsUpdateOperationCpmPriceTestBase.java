package ru.yandex.direct.core.entity.campaign.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategy;
import ru.yandex.direct.core.aggregatedstatuses.repository.AggregatedStatusesRepository;
import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum;
import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason;
import ru.yandex.direct.core.entity.aggregatedstatuses.campaign.AggregatedStatusCampaignData;
import ru.yandex.direct.core.entity.campaign.model.BaseCampaign;
import ru.yandex.direct.core.entity.campaign.model.CampOptionsStrategy;
import ru.yandex.direct.core.entity.campaign.model.CampaignMetatype;
import ru.yandex.direct.core.entity.campaign.model.CampaignSource;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusBsSynced;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithPricePackage;
import ru.yandex.direct.core.entity.campaign.model.CampaignsAutobudget;
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform;
import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.StrategyData;
import ru.yandex.direct.core.entity.campaign.model.StrategyName;
import ru.yandex.direct.core.entity.campaign.repository.CampaignModifyRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.type.add.container.RestrictedCampaignsAddOperationContainer;
import ru.yandex.direct.core.entity.campaign.service.type.update.CampaignUpdateOperationSupportFacade;
import ru.yandex.direct.core.entity.campaign.service.validation.UpdateRestrictedCampaignValidationService;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackage;
import ru.yandex.direct.core.entity.pricepackage.model.TargetingsFixed;
import ru.yandex.direct.core.entity.strategy.repository.StrategyTypedRepository;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.repository.TestClientRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.model.UidClientIdShard;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.metrika.client.MetrikaClient;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.test.utils.differ.BigDecimalDiffer;

import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.core.entity.campaign.service.CampaignWithPricePackageUtils.getStrategy;
import static ru.yandex.direct.core.entity.dbqueue.DbQueueJobTypes.RECALC_BRAND_LIFT_CAMPAIGNS;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultCpmPriceCampaignWithSystemFields;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;
import static ru.yandex.direct.core.testing.data.TestPricePackages.allowedPricePackageClient;
import static ru.yandex.direct.core.testing.data.TestPricePackages.approvedPricePackage;
import static ru.yandex.direct.core.testing.data.TestPricePackages.emptyTargetingsCustom;
import static ru.yandex.direct.core.testing.data.TestRegions.SIBERIAN_DISTRICT;
import static ru.yandex.direct.core.testing.data.TestRegions.VOLGA_DISTRICT;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;
import static ru.yandex.direct.feature.FeatureName.CPM_PRICE_CAMPAIGN;
import static ru.yandex.direct.regions.Region.REGION_TYPE_DISTRICT;
import static ru.yandex.direct.test.utils.matcher.LocalDateTimeMatcher.approximatelyNow;

@ParametersAreNonnullByDefault
public class RestrictedCampaignsUpdateOperationCpmPriceTestBase {

    static final Supplier<LocalDate> MINUS_MONTHS = () -> LocalDate.now().minusMonths(1);
    static final Supplier<LocalDate> YESTERDAY = () -> LocalDate.now().minusDays(1);
    static final Supplier<LocalDate> TODAY = LocalDate::now;
    static final Supplier<LocalDate> TOMORROW = () -> LocalDate.now().plusDays(1);
    static final Supplier<LocalDate> PLUS_MONTHS = () -> LocalDate.now().plusMonths(1);

    @Autowired
    RbacService rbacService;
    @Autowired
    MetrikaClient metrikaClient;
    @Autowired
    Steps steps;
    @Autowired
    private StrategyTypedRepository strategyTypedRepository;
    @Autowired
    private CampaignModifyRepository campaignModifyRepository;
    @Autowired
    private CampaignTypedRepository campaignTypedRepository;
    @Autowired
    private DslContextProvider ppcDslContextProvider;
    @Autowired
    private UpdateRestrictedCampaignValidationService updateRestrictedCampaignValidationService;
    @Autowired
    private CampaignUpdateOperationSupportFacade campaignUpdateOperationSupportFacade;
    @Autowired
    private CampaignAdditionalActionsService campaignAdditionalActionsService;
    @Autowired
    protected AggregatedStatusesRepository aggregatedStatusesRepository;
    @Autowired
    private TestClientRepository testClientRepository;
    @Autowired
    private RequestBasedMetrikaClientFactory metrikaClientFactory;
    @Autowired
    private FeatureService featureService;

    ClientInfo defaultClient;
    PricePackage defaultPricePackage;
    Long operatorUid;

    @Before
    public void before() {
        defaultClient = steps.clientSteps().createClient(defaultClient().withWorkCurrency(CurrencyCode.RUB));
        defaultPricePackage = steps.pricePackageSteps().createPricePackage(defaultPricePackage()
                .withPrice(BigDecimal.valueOf(20L))).getPricePackage();
        operatorUid = defaultClient.getUid();
        steps.dbQueueSteps().registerJobType(RECALC_BRAND_LIFT_CAMPAIGNS);
    }

    void createPriceCampaign(CampaignWithPricePackage priceCampaign) {
        var addCampaignParametersContainer = RestrictedCampaignsAddOperationContainer.create(
                defaultClient.getShard(), operatorUid, defaultClient.getClientId(), defaultClient.getUid(),
                defaultClient.getUid());
        campaignModifyRepository.addCampaigns(ppcDslContextProvider.ppc(defaultClient.getShard()),
                addCampaignParametersContainer, List.of(priceCampaign));
    }


    MassResult<Long> apply(ModelChanges<? extends BaseCampaign> modelChanges) {
        var defaultOptions = new CampaignOptions();
        return apply(modelChanges, defaultOptions);
    }

    MassResult<Long> apply(ModelChanges<? extends BaseCampaign> modelChanges, CampaignOptions options) {
        RestrictedCampaignsUpdateOperation restrictedCampaignsUpdateOperation = new RestrictedCampaignsUpdateOperation(
                Collections.singletonList(modelChanges),
                operatorUid,
                UidClientIdShard.of(defaultClient.getUid(), defaultClient.getClientId(), defaultClient.getShard()),
                campaignModifyRepository,
                campaignTypedRepository,
                strategyTypedRepository,
                updateRestrictedCampaignValidationService,
                campaignUpdateOperationSupportFacade,
                campaignAdditionalActionsService,
                ppcDslContextProvider, rbacService, metrikaClientFactory, featureService,
                Applicability.PARTIAL, options);
        return restrictedCampaignsUpdateOperation.apply();
    }

    CpmPriceCampaign getCampaignFromRepository(Long campaignId) {
        List<? extends BaseCampaign> typedCampaigns =
                campaignTypedRepository.getTypedCampaigns(defaultClient.getShard(),
                        Collections.singletonList(campaignId));
        return (CpmPriceCampaign) typedCampaigns.get(0);
    }

    static CampaignWithPricePackage updateOriginCampaignToExpected(CampaignWithPricePackage campaign) {
        return updateOriginCampaignToExpected(campaign,
                campaign.getStrategy().getStrategyData().getBudget(),
                campaign.getStartDate(),
                campaign.getEndDate());
    }

    static CampaignWithPricePackage updateOriginCampaignToExpected(CampaignWithPricePackage campaign,
                                                                   BigDecimal budget,
                                                                   LocalDate newStartDate,
                                                                   LocalDate newEndDate) {
        return campaign
                .withStartDate(newStartDate)
                .withEndDate(newEndDate)
                .withStatusBsSynced(CampaignStatusBsSynced.NO)
                .withStrategy((DbStrategy) new DbStrategy()
                        .withStrategy(CampOptionsStrategy.DIFFERENT_PLACES)
                        .withStrategyName(StrategyName.PERIOD_FIX_BID)
                        .withStrategyData(new StrategyData()
                                .withName("period_fix_bid")
                                .withVersion(1L)
                                .withAutoProlongation(0L)
                                .withStart(newStartDate)
                                .withFinish(newEndDate)
                                .withBudget(budget))
                        .withAutobudget(CampaignsAutobudget.NO)
                        .withPlatform(CampaignsPlatform.CONTEXT));
    }

    void setupOperator(RbacRole operatorRole) {
        setupOperator(operatorRole, null);
    }

    void setupOperator(RbacRole operatorRole, Boolean developer) {
        ClientId clientId;
        switch (operatorRole) {
            case CLIENT:
                operatorUid = defaultClient.getUid();
                clientId = defaultClient.getClientId();
                break;
            default:
                var operatorClientInfo = steps.clientSteps().createClient(new ClientInfo()
                        .withClient(defaultClient()
                                .withRole(operatorRole))
                        .withChiefUserInfo(new UserInfo().withUser(generateNewUser()
                                .withDeveloper(developer))));
                operatorUid = operatorClientInfo.getUid();
                clientId = operatorClientInfo.getClientId();
                if (operatorRole == RbacRole.MANAGER) {
                    testClientRepository.setManagerToClient(operatorClientInfo.getShard(),
                            defaultClient.getClientId(), operatorUid);
                }
                break;
        }
        steps.featureSteps().addClientFeature(clientId, CPM_PRICE_CAMPAIGN, true);
    }

    protected PricePackage defaultPricePackage() {
        return approvedPricePackage()
                .withCurrency(CurrencyCode.RUB)
                .withPrice(BigDecimal.valueOf(20L))
                .withDateStart(LocalDate.of(2017, 1, 1))
                .withDateEnd(LocalDate.of(2222, 1, 1))
                .withOrderVolumeMin(1L)
                .withOrderVolumeMax(2000L)
                .withTargetingsFixed(new TargetingsFixed()
                        .withGeo(List.of(VOLGA_DISTRICT, SIBERIAN_DISTRICT))
                        .withGeoType(REGION_TYPE_DISTRICT)
                        .withGeoExpanded(List.of(VOLGA_DISTRICT, SIBERIAN_DISTRICT))
                        .withViewTypes(emptyList())
                        .withAllowExpandedDesktopCreative(true))
                .withTargetingsCustom(emptyTargetingsCustom())
                .withClients(List.of(allowedPricePackageClient(defaultClient)));
    }

    CpmPriceCampaign defaultCampaign() {
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        LocalDate startDate = LocalDate.of(currentYear + 1, 1, 1);
        LocalDate endDate = LocalDate.of(currentYear + 1, 2, 2);
        long flightOrderVolume = 1000L;
        var campaign = defaultCpmPriceCampaignWithSystemFields(defaultClient, defaultPricePackage)
                .withFlightOrderVolume(flightOrderVolume)
                .withStartDate(startDate)
                .withEndDate(endDate)
                .withStatusArchived(false);
        campaign.withStrategy(getStrategy(campaign, defaultPricePackage, false));
        return campaign;
    }

    DefaultCompareStrategy cpmPriceCampaignCompareStrategy() {
        return DefaultCompareStrategies
                .allFieldsExcept(newPath("createTime"), newPath("source"))
                .forFields(newPath("lastChange")).useMatcher(approximatelyNow(ZoneOffset.of("+03:00")))
                .forFields(newPath("source")).useMatcher(equalTo(CampaignSource.DIRECT))
                .forFields(newPath("metatype")).useMatcher(equalTo(CampaignMetatype.DEFAULT_))
                .forClasses(BigDecimal.class).useDiffer(new BigDecimalDiffer());
    }

    void addCampaignAggregatedStatus(CampaignWithPricePackage priceCampaign) {
        AggregatedStatusCampaignData campaignsStatus = new AggregatedStatusCampaignData(
                null, null, GdSelfStatusEnum.STOP_CRIT, GdSelfStatusReason.CPM_PRICE_INCORRECT);
        aggregatedStatusesRepository.updateCampaigns(defaultClient.getShard(), null,
                Map.of(priceCampaign.getId(), campaignsStatus));
    }

}
