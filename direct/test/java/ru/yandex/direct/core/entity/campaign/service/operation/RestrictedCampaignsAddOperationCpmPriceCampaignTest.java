package ru.yandex.direct.core.entity.campaign.service.operation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import jdk.jfr.Description;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategy;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierInventory;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierInventoryAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierType;
import ru.yandex.direct.core.entity.bidmodifier.InventoryType;
import ru.yandex.direct.core.entity.campaign.model.CampOptionsStrategy;
import ru.yandex.direct.core.entity.campaign.model.CampaignMeasurer;
import ru.yandex.direct.core.entity.campaign.model.CampaignMeasurerSystem;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusBsSynced;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusModerate;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusPostmoderate;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CampaignWarnPlaceInterval;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithBrandSafety;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithPricePackage;
import ru.yandex.direct.core.entity.campaign.model.CampaignsAutobudget;
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform;
import ru.yandex.direct.core.entity.campaign.model.ContentLanguage;
import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.PriceFlightReasonIncorrect;
import ru.yandex.direct.core.entity.campaign.model.PriceFlightStatusApprove;
import ru.yandex.direct.core.entity.campaign.model.PriceFlightStatusCorrect;
import ru.yandex.direct.core.entity.campaign.model.PriceFlightTargetingsSnapshot;
import ru.yandex.direct.core.entity.campaign.model.SmsFlag;
import ru.yandex.direct.core.entity.campaign.model.StrategyData;
import ru.yandex.direct.core.entity.campaign.model.StrategyName;
import ru.yandex.direct.core.entity.campaign.repository.CampaignModifyRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.CampaignOptions;
import ru.yandex.direct.core.entity.campaign.service.RequestBasedMetrikaClientFactory;
import ru.yandex.direct.core.entity.campaign.service.RestrictedCampaignsAddOperation;
import ru.yandex.direct.core.entity.campaign.service.type.add.CampaignAddOperationSupportFacade;
import ru.yandex.direct.core.entity.campaign.service.validation.AddRestrictedCampaignValidationService;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstantsService;
import ru.yandex.direct.core.entity.currency.model.cpmyndxfrontpage.FrontpageCampaignShowType;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackage;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackageCampaignOptions;
import ru.yandex.direct.core.entity.pricepackage.model.ShowsFrequencyLimit;
import ru.yandex.direct.core.entity.pricepackage.model.StatusApprove;
import ru.yandex.direct.core.entity.pricepackage.model.TargetingsCustom;
import ru.yandex.direct.core.entity.pricepackage.model.TargetingsFixed;
import ru.yandex.direct.core.entity.pricepackage.model.ViewType;
import ru.yandex.direct.core.entity.product.service.ProductService;
import ru.yandex.direct.core.entity.retargeting.service.common.GoalUtilsService;
import ru.yandex.direct.core.entity.strategy.repository.StrategyTypedRepository;
import ru.yandex.direct.core.entity.time.model.TimeInterval;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbutil.model.UidAndClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.libs.timetarget.TimeTarget;
import ru.yandex.direct.model.ModelProperty;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.test.utils.differ.BigDecimalDiffer;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.DefectIds;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_ADD_METRIKA_TAG_TO_URL;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_ADD_OPENSTAT_TAG_TO_URL;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;
import static ru.yandex.direct.core.testing.data.TestPricePackages.allowedPricePackageClient;
import static ru.yandex.direct.core.testing.data.TestPricePackages.approvedPricePackage;
import static ru.yandex.direct.core.testing.data.TestPricePackages.disallowedPricePackageClient;
import static ru.yandex.direct.core.testing.data.TestRegions.SIBERIAN_DISTRICT;
import static ru.yandex.direct.core.testing.data.TestRegions.VOLGA_DISTRICT;
import static ru.yandex.direct.feature.FeatureName.CPM_PRICE_CAMPAIGN;
import static ru.yandex.direct.feature.FeatureName.IAS_MEASURER;
import static ru.yandex.direct.feature.FeatureName.MOAT_MEASURER_CAMP;
import static ru.yandex.direct.feature.FeatureName.MOAT_USE_UNSTABLE_SCRIPT;
import static ru.yandex.direct.libs.timetarget.TimeTargetUtils.DEFAULT_TIMEZONE;
import static ru.yandex.direct.libs.timetarget.TimeTargetUtils.timeTarget24x7;
import static ru.yandex.direct.regions.Region.REGION_TYPE_DISTRICT;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.test.utils.matcher.LocalDateTimeMatcher.approximatelyNow;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.isNull;
import static ru.yandex.direct.validation.defect.CommonDefects.mustBeEmpty;
import static ru.yandex.direct.validation.defect.CommonDefects.objectNotFound;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class RestrictedCampaignsAddOperationCpmPriceCampaignTest {

    private static final DefaultCompareStrategy CPM_PRICE_CAMPAIGN_COMPARE_STRATEGY = DefaultCompareStrategies
            .onlyExpectedFields()
            .forFields(newPath("lastChange")).useMatcher(approximatelyNow())
            .forClasses(BigDecimal.class).useDiffer(new BigDecimalDiffer());
    private static final List<String> DISABLED_VIDEO_PLACEMENTS = List.of("vk.com", "music.yandex.ru");
    private static final String DOMAIN = "domain.com";
    private static final String ANOTHER_DOMAIN = "anotherdomain.com";
    private static final String WWW = "www.";
    private static final Long CPM_VIDEO_RUB_PRODUCTID = 509619L;

    @Autowired
    private CampaignModifyRepository campaignModifyRepository;

    @Autowired
    private StrategyTypedRepository strategyTypedRepository;

    @Autowired
    private AddRestrictedCampaignValidationService addRestrictedCampaignValidationService;

    @Autowired
    private CampaignAddOperationSupportFacade campaignAddOperationSupportFacade;

    @Autowired
    public DslContextProvider dslContextProvider;

    @Autowired
    public RbacService rbacService;

    @Autowired
    private CampaignTypedRepository campaignTypedRepository;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private ProductService productService;

    @Autowired
    private Steps steps;

    @Autowired
    private RequestBasedMetrikaClientFactory metrikaClientFactory;

    @Autowired
    private GoalUtilsService goalUtilsService;

    @Autowired
    private CampaignConstantsService campaignConstantsService;

    private ClientInfo defaultClient;

    @Before
    public void before() {
        defaultClient = steps.clientSteps().createClient(defaultClient().withWorkCurrency(CurrencyCode.RUB));
        steps.featureSteps().addClientFeature(defaultClient.getClientId(), CPM_PRICE_CAMPAIGN, true);
    }

    @Test
    public void simpleFields() {
        var pricePackage = steps.pricePackageSteps().createPricePackage(defaultPricePackage());
        var campaign = defaultCampaign(pricePackage.getPricePackageId());

        var result = addCampaign(campaign);
        assertThat(result.getValidationResult().flattenErrors()).isEmpty();

        var actualCampaign = getCampaignFromResult(result);
        var expectedCampaign = defaultCampaign(pricePackage.getPricePackageId())
                .withEnableCpcHold(false)
                .withHasExtendedGeoTargeting(false)
                .withHasTitleSubstitution(false)
                .withEnableCompanyInfo(false)
                .withStatusBsSynced(CampaignStatusBsSynced.NO)
                .withStatusModerate(CampaignStatusModerate.NEW)
                .withStatusPostModerate(CampaignStatusPostmoderate.NEW)
                .withStatusActive(false)
                .withStatusEmpty(false)
                .withStatusShow(true)
                .withStatusArchived(false)
                .withFlightStatusApprove(PriceFlightStatusApprove.NEW)
                .withFlightStatusCorrect(PriceFlightStatusCorrect.NEW)
                .withSumToPay(BigDecimal.ZERO)
                .withSumLast(BigDecimal.ZERO)
                .withSumSpent(BigDecimal.ZERO)
                .withSum(BigDecimal.ZERO)
                .withTimeTarget(timeTarget24x7())
                .withClientId(defaultClient.getClientId().asLong())
                .withStrategyId(null);
        Assertions.assertThat(actualCampaign).is(matchedBy(beanDiffer(expectedCampaign)
                .useCompareStrategy(CPM_PRICE_CAMPAIGN_COMPARE_STRATEGY)));
    }

    @Test
    public void strategy() {
        var pricePackage = steps.pricePackageSteps().createPricePackage(defaultPricePackage()
                .withPrice(BigDecimal.valueOf(20L)));

        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        var startDate = LocalDate.of(currentYear + 2, 1, 1);
        var endDate = LocalDate.of(currentYear + 2, 12, 1);
        var campaign = defaultCampaign(pricePackage.getPricePackageId())
                .withStartDate(startDate)
                .withEndDate(endDate)
                .withFlightOrderVolume(2345L);

        // вычисляется по формуле: v * p / 1000, где
        // v: объём заказа
        // p: cpm цена
        // 2345 * 20 / 1000
        BigDecimal expectedBudget = BigDecimal.valueOf(46.9);

        var result = addCampaign(campaign);
        assertThat(result.getValidationResult().flattenErrors()).isEmpty();

        var actualCampaign = getCampaignFromResult(result);
        var expectedCampaign = new CpmPriceCampaign()
                .withStrategy((DbStrategy) new DbStrategy()
                        .withStrategy(CampOptionsStrategy.DIFFERENT_PLACES)
                        .withStrategyName(StrategyName.PERIOD_FIX_BID)
                        .withStrategyData(new StrategyData()
                                .withName("period_fix_bid")
                                .withVersion(1L)
                                .withAutoProlongation(0L)
                                .withStart(startDate)
                                .withFinish(endDate)
                                .withBudget(expectedBudget))
                        .withAutobudget(CampaignsAutobudget.NO)
                        .withPlatform(CampaignsPlatform.CONTEXT))
                .withStrategyId(null);
        Assertions.assertThat(actualCampaign).is(matchedBy(beanDiffer(expectedCampaign)
                .useCompareStrategy(CPM_PRICE_CAMPAIGN_COMPARE_STRATEGY)));
    }

    @Test
    public void autoProlongationVideo() {
        var pricePackage = steps.pricePackageSteps().createPricePackage(defaultPricePackage()
                .withAvailableAdGroupTypes(Set.of(AdGroupType.CPM_VIDEO)));

        var campaign = defaultCampaign(pricePackage.getPricePackageId())
                .withFlightOrderVolume(2345L)
                .withAutoProlongation(true);

        var result = addCampaign(campaign);
        assertThat(result.getValidationResult().flattenErrors()).isEmpty();

        var actualCampaign = getCampaignFromResult(result);
        Assertions.assertThat(actualCampaign.getStrategy().getStrategyData().getAutoProlongation()).isEqualTo(1L);
    }

    @Test
    public void autoProlongationYndxFrontpage() {
        var pricePackage = steps.pricePackageSteps().createPricePackage(defaultPricePackage()
                .withAvailableAdGroupTypes(Set.of(AdGroupType.CPM_YNDX_FRONTPAGE)));

        var campaign = defaultCampaign(pricePackage.getPricePackageId())
                .withFlightOrderVolume(2345L)
                .withAutoProlongation(true);

        var result = addCampaign(campaign);
        assertThat(result.getValidationResult().flattenErrors()).isEmpty();

        var actualCampaign = getCampaignFromResult(result);
        Assertions.assertThat(actualCampaign.getStrategy().getStrategyData().getAutoProlongation()).isEqualTo(0L);
    }

    @Test
    public void product() {
        var pricePackage = steps.pricePackageSteps().createPricePackage(defaultPricePackage());
        var campaign = defaultCampaign(pricePackage.getPricePackageId())
                .withCurrency(CurrencyCode.RUB);

        var result = addCampaign(campaign);
        assertThat(result.getValidationResult().flattenErrors()).isEmpty();


        var actualCampaign = getCampaignFromResult(result);
        var expectedProduct =
                productService.calculateProductForCampaign(CampaignType.CPM_PRICE, CurrencyCode.RUB, false);
        Assertions.assertThat(actualCampaign.getProductId()).isEqualTo(expectedProduct.getId());
    }

    @Test
    public void productCpmVideo() {
        //При создании кампании в пакете которой есть только видео баннеры записывать продукт от аукционного видео.
        var pricePackage = steps.pricePackageSteps().createPricePackage(videoPricePackage());
        var campaign = defaultCampaign(pricePackage.getPricePackageId())
                .withCurrency(CurrencyCode.RUB);

        var result = addCampaign(campaign);
        assertThat(result.getValidationResult().flattenErrors()).isEmpty();


        var actualCampaign = getCampaignFromResult(result);
        Assertions.assertThat(actualCampaign.getProductId()).isEqualTo(CPM_VIDEO_RUB_PRODUCTID);
    }

    @Test
    public void forbiddenFieldsNotNull_Fail() {
        var pricePackage = steps.pricePackageSteps().createPricePackage(defaultPricePackage());
        var campaign = defaultCampaign(pricePackage.getPricePackageId())
                .withEnableCompanyInfo(false)
                .withEnableCpcHold(false)
                .withHasExtendedGeoTargeting(false)
                .withHasTitleSubstitution(false)
                .withFlightStatusApprove(PriceFlightStatusApprove.YES)
                .withFlightStatusCorrect(PriceFlightStatusCorrect.YES)
                .withFlightReasonIncorrect(PriceFlightReasonIncorrect.NO_DEFAULT_GROUP)
                .withStrategy(new DbStrategy())
                .withTimeTarget(TimeTarget.parseRawString("1ABCDEFGHIJKLMNOPQRSTUVWX"));

        var result = addCampaign(campaign);

        assertValidationErrorOnProperty(result, CpmPriceCampaign.ENABLE_COMPANY_INFO, isNull());
        assertValidationErrorOnProperty(result, CpmPriceCampaign.ENABLE_CPC_HOLD, isNull());
        assertValidationErrorOnProperty(result, CpmPriceCampaign.HAS_EXTENDED_GEO_TARGETING, isNull());
        assertValidationErrorOnProperty(result, CpmPriceCampaign.HAS_TITLE_SUBSTITUTION, isNull());
        assertValidationErrorOnProperty(result, CpmPriceCampaign.FLIGHT_STATUS_APPROVE, isNull());
        assertValidationErrorOnProperty(result, CpmPriceCampaign.FLIGHT_STATUS_CORRECT, isNull());
        assertValidationErrorOnProperty(result, CpmPriceCampaign.FLIGHT_REASON_INCORRECT, isNull());
        assertValidationErrorOnProperty(result, CpmPriceCampaign.STRATEGY, isNull());
        assertValidationErrorOnProperty(result, CpmPriceCampaign.TIME_TARGET, isNull());
    }

    @Test
    public void unexistingPricePackageId_Fail() {
        var campaign = defaultCampaign(Long.MAX_VALUE);

        var result = addCampaign(campaign);

        assertValidationErrorOnProperty(result, CpmPriceCampaign.PRICE_PACKAGE_ID, objectNotFound());
    }

    // TODO: удалить в DIRECT-109162
    @Test
    public void saveFrontpageData_AllViewTypes() {
        var pricePackage = defaultPricePackage();
        pricePackage.getTargetingsFixed()
                .withViewTypes(List.of(ViewType.DESKTOP, ViewType.MOBILE, ViewType.NEW_TAB));
        var pricePackageId = steps.pricePackageSteps().createPricePackage(pricePackage).getPricePackageId();
        var campaign = defaultCampaign(pricePackageId);
        campaign.getFlightTargetingsSnapshot()
                .withViewTypes(List.of(ViewType.DESKTOP, ViewType.MOBILE, ViewType.NEW_TAB));

        var result = addCampaign(campaign);
        var campaignId = result.get(0).getResult();
        var frontpageTypes = campaignRepository.getFrontpageTypesForCampaigns(defaultClient.getShard(),
                List.of(campaignId)).get(campaignId);

        assertThat(frontpageTypes).isEqualTo(
                Set.of(FrontpageCampaignShowType.FRONTPAGE,
                        FrontpageCampaignShowType.FRONTPAGE_MOBILE,
                        FrontpageCampaignShowType.BROWSER_NEW_TAB));
    }

    @Test
    public void saveFrontpageData_emptyViewTypesForVideo() {
        var pricePackage = defaultPricePackage();
        pricePackage.setAvailableAdGroupTypes(Set.of(AdGroupType.CPM_VIDEO));
        pricePackage.setIsFrontpage(false);
        pricePackage.getTargetingsFixed()
                .withViewTypes(List.of(ViewType.DESKTOP, ViewType.MOBILE, ViewType.NEW_TAB));
        var pricePackageId = steps.pricePackageSteps().createPricePackage(pricePackage).getPricePackageId();
        var campaign = defaultCampaign(pricePackageId);
        campaign.getFlightTargetingsSnapshot()
                .withViewTypes(List.of(ViewType.DESKTOP, ViewType.MOBILE, ViewType.NEW_TAB));

        var result = addCampaign(campaign);
        var campaignId = result.get(0).getResult();
        var frontpageTypes = campaignRepository.getFrontpageTypesForCampaigns(defaultClient.getShard(),
                List.of(campaignId)).get(campaignId);

        assertThat(frontpageTypes).isNull();
    }

    @Test
    @Description("Прайсовый пакет должен быть привязан к клиенту")
    public void packageNotAvailableForClient_Error() {
        var pricePackageId = steps.pricePackageSteps().createPricePackage(defaultPricePackage()
                .withClients(emptyList()))
                .getPricePackageId();
        var campaign = defaultCampaign(pricePackageId);

        MassResult<Long> result = addCampaign(campaign);
        MatcherAssert.assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(
                        path(index(0), field(CampaignWithPricePackage.PRICE_PACKAGE_ID)),
                        objectNotFound())));
    }

    @Test
    @Description("Публичный прайсовый пакет не должен быть забанен у клиента")
    public void publicPackageNotAvailableForClient_Error() {
        var pricePackageId = steps.pricePackageSteps().createPricePackage(defaultPricePackage()
                .withIsPublic(true)
                .withClients(List.of(disallowedPricePackageClient(defaultClient))))
                .getPricePackageId();
        var campaign = defaultCampaign(pricePackageId);

        MassResult<Long> result = addCampaign(campaign);
        MatcherAssert.assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(
                        path(index(0), field(CampaignWithPricePackage.PRICE_PACKAGE_ID)),
                        objectNotFound())));
    }

    @Test
    @Description("Публичный прайсовый пакет должен иметь ту же валюту, что и клиент")
    public void publicPackageHasAnotherCurrency_Error() {
        assumeThat(defaultClient.getClient().getWorkCurrency(), is(CurrencyCode.RUB));
        var pricePackageId = steps.pricePackageSteps().createPricePackage(defaultPricePackage()
                .withIsPublic(true)
                .withCurrency(CurrencyCode.USD)
                .withClients(List.of(disallowedPricePackageClient(defaultClient))))
                .getPricePackageId();
        var campaign = defaultCampaign(pricePackageId);

        MassResult<Long> result = addCampaign(campaign);
        MatcherAssert.assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(
                        path(index(0), field(CampaignWithPricePackage.PRICE_PACKAGE_ID)),
                        objectNotFound())));
    }

    @Test
    @Description("Публичный прайсовый пакет не может быть использован для создания кампании, если он не заапрувлен.")
    public void publicPackageNotApproved_Error() {
        for (StatusApprove statusApprove : StatusApprove.values()) {
            if (statusApprove == StatusApprove.YES) {
                continue;
            }
            var pricePackageId = steps.pricePackageSteps().createPricePackage(defaultPricePackage()
                    .withIsPublic(true)
                    .withClients(emptyList())
                    .withStatusApprove(statusApprove))
                    .getPricePackageId();
            var campaign = defaultCampaign(pricePackageId);

            MassResult<Long> result = addCampaign(campaign);
            MatcherAssert.assertThat(result.getValidationResult(),
                    hasDefectDefinitionWith(validationError(
                            path(index(0), field(CampaignWithPricePackage.PRICE_PACKAGE_ID)),
                            objectNotFound())));

        }
    }

    @Test
    @Description("Публичный прайсовый пакет - ok")
    public void publicPackageAvailableForClient_Ok() {
        var pricePackageId = steps.pricePackageSteps().createPricePackage(defaultPricePackage()
                .withIsPublic(true)
                .withClients(emptyList()))
                .getPricePackageId();
        var campaign = defaultCampaign(pricePackageId);

        MassResult<Long> result = addCampaign(campaign);
        assertThat(result.getValidationResult().flattenErrors()).isEmpty();
    }

    @Test
    @Description("Архивный прайсовый пакет не может быть использован для создания кампании")
    public void archivedPackage_Error() {
        var pricePackageId = steps.pricePackageSteps().createPricePackage(defaultPricePackage()
                .withIsArchived(true))
                .getPricePackageId();
        var campaign = defaultCampaign(pricePackageId);

        MassResult<Long> result = addCampaign(campaign);
        MatcherAssert.assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(
                        path(index(0), field(CampaignWithPricePackage.PRICE_PACKAGE_ID)),
                        objectNotFound())));
    }

    @Test
    public void saveAllowedPageIds_normal() {
        var pricePackageId = steps.pricePackageSteps()
                .createPricePackage(
                        defaultPricePackage()
                                .withAllowedPageIds(List.of(4L, 3L, 2L))
                ).getPricePackageId();
        var campaign = defaultCampaign(pricePackageId);

        var result = addCampaign(campaign);
        assertThat(result.getValidationResult().flattenErrors()).isEmpty();
        CpmPriceCampaign actualCampaign = getCampaignFromResult(result);

        assertThat(actualCampaign.getAllowedPageIds()).isEqualTo(List.of(4L, 3L, 2L));
    }

    @Test
    public void saveAllowedPageIds_emptyList() {
        var pricePackageId = steps.pricePackageSteps()
                .createPricePackage(
                        defaultPricePackage()
                                .withAllowedPageIds(Collections.emptyList())
                ).getPricePackageId();
        var campaign = defaultCampaign(pricePackageId);

        var result = addCampaign(campaign);
        assertThat(result.getValidationResult().flattenErrors()).isEmpty();
        CpmPriceCampaign actualCampaign = getCampaignFromResult(result);

        assertThat(actualCampaign.getAllowedPageIds()).isNull();
    }

    @Test
    public void saveAllowedPageIds_null() {
        var pricePackageId = steps.pricePackageSteps()
                .createPricePackage(
                        defaultPricePackage()
                                .withAllowedPageIds(null)
                ).getPricePackageId();
        var campaign = defaultCampaign(pricePackageId);

        var result = addCampaign(campaign);
        assertThat(result.getValidationResult().flattenErrors()).isEmpty();
        CpmPriceCampaign actualCampaign = getCampaignFromResult(result);

        assertThat(actualCampaign.getAllowedPageIds()).isNull();
    }

    @Test
    @Description("Если на пакете автопринятие, то РК автопринята")
    public void autoApprove_enable() {
        var pricePackageId = steps.pricePackageSteps()
                .createPricePackage(
                        defaultPricePackage()
                                .withCampaignAutoApprove(true)
                ).getPricePackageId();
        var campaign = defaultCampaign(pricePackageId);

        var result = addCampaign(campaign);

        assertThat(result.getValidationResult().flattenErrors()).isEmpty();
        CpmPriceCampaign actualCampaign = getCampaignFromResult(result);
        assertThat(actualCampaign.getFlightStatusApprove()).isEqualTo(PriceFlightStatusApprove.YES);
    }

    @Test
    @Description("Если на пакете нет автопринятия, то новая РК не принята")
    public void autoApprove_disable() {
        var pricePackageId = steps.pricePackageSteps()
                .createPricePackage(
                        defaultPricePackage()
                                .withCampaignAutoApprove(false)
                ).getPricePackageId();
        var campaign = defaultCampaign(pricePackageId);

        var result = addCampaign(campaign);

        assertThat(result.getValidationResult().flattenErrors()).isEmpty();
        CpmPriceCampaign actualCampaign = getCampaignFromResult(result);
        assertThat(actualCampaign.getFlightStatusApprove()).isEqualTo(PriceFlightStatusApprove.NEW);
    }

    @Test
    @Description("Если на пакете разрешены brandsafety, то сохраняется")
    public void brandsafety_enable() {
        List<Long> categories = asList(4294967299L, 4294967297L, 4294967298L);
        var pricePackageId = steps.pricePackageSteps()
                .createPricePackage(
                        defaultPricePackage()
                                .withCampaignOptions(new PricePackageCampaignOptions().withAllowBrandSafety(true))
                ).getPricePackageId();
        var campaign = defaultCampaign(pricePackageId)
                .withBrandSafetyCategories(categories);

        var result = addCampaign(campaign);

        assertThat(result.getValidationResult().flattenErrors()).isEmpty();
        CpmPriceCampaign actualCampaign = getCampaignFromResult(result);
        assertThat(actualCampaign.getBrandSafetyCategories()).isEqualTo(categories);
    }

    @Test
    @Description("Если на пакете запрещены brandsafety, то ошибка валидации")
    public void brandsafety_disable() {
        List<Long> categories = asList(4294967299L, 4294967297L, 4294967298L);
        var pricePackageId = steps.pricePackageSteps()
                .createPricePackage(
                        defaultPricePackage()
                                .withCampaignOptions(new PricePackageCampaignOptions().withAllowBrandSafety(false))
                ).getPricePackageId();
        var campaign = defaultCampaign(pricePackageId)
                .withBrandSafetyCategories(categories);

        var result = addCampaign(campaign);

        MatcherAssert.assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(
                        path(index(0), field(CampaignWithBrandSafety.BRAND_SAFETY_CATEGORIES)),
                        mustBeEmpty())));
    }

    @Test
    @Description("Если на пакете флаг brandsafety = null, то считает что разрешены")
    public void brandsafety_null() {
        List<Long> categories = asList(4294967299L, 4294967297L, 4294967298L);
        var pricePackageId = steps.pricePackageSteps()
                .createPricePackage(
                        defaultPricePackage()
                                .withCampaignOptions(null)
                ).getPricePackageId();
        var campaign = defaultCampaign(pricePackageId)
                .withBrandSafetyCategories(categories);

        var result = addCampaign(campaign);

        assertThat(result.getValidationResult().flattenErrors()).isEmpty();
        CpmPriceCampaign actualCampaign = getCampaignFromResult(result);
        assertThat(actualCampaign.getBrandSafetyCategories()).isEqualTo(categories);
    }

    @Test
    @Description("если в пакете явно указана частота, она копируется на РК")
    public void saveFrequencyLimit_inherited() {
        var pricePackage = defaultPricePackage();
        pricePackage.getCampaignOptions().setShowsFrequencyLimit(new ShowsFrequencyLimit()
                .withFrequencyLimitIsForCampaignTime(false)
                .withFrequencyLimitDays(7)
                .withFrequencyLimit(3)
        );
        var pricePackageId = steps.pricePackageSteps().createPricePackage(pricePackage).getPricePackageId();
        var campaign = defaultCampaign(pricePackageId);

        var result = addCampaign(campaign);

        assertThat(result.getValidationResult().flattenErrors()).isEmpty();
        CpmPriceCampaign actualCampaign = getCampaignFromResult(result);
        assertThat(actualCampaign.getImpressionRateCount())
                .isEqualTo(pricePackage.getCampaignOptions().getShowsFrequencyLimit().getFrequencyLimit());
    }

    @Test
    @Description("На рк добавляется частота, заданная в кампании при разрешённой частоте в пакете")
    public void saveFrequencyLimit_explicit() {
        var pricePackage = defaultPricePackage();
        pricePackage.getCampaignOptions().setShowsFrequencyLimit(new ShowsFrequencyLimit());
        var pricePackageId = steps.pricePackageSteps().createPricePackage(pricePackage).getPricePackageId();
        var campaign = defaultCampaign(pricePackageId)
                .withImpressionRateCount(11)
                .withImpressionRateIntervalDays(5);

        var result = addCampaign(campaign);

        assertThat(result.getValidationResult().flattenErrors()).isEmpty();
        CpmPriceCampaign actualCampaign = getCampaignFromResult(result);
        assertThat(actualCampaign.getImpressionRateCount()).isEqualTo(campaign.getImpressionRateCount());
    }

    @Test
    @Description("пакет запрещает частоту, добавить кампанию с проставленной частотой - ошибка валидации")
    public void saveFrequencyLimit_packageNotAllows() {
        var pricePackage = defaultPricePackage();
        pricePackage.getCampaignOptions().setShowsFrequencyLimit(null);
        var pricePackageId = steps.pricePackageSteps().createPricePackage(pricePackage).getPricePackageId();
        var campaign = defaultCampaign(pricePackageId)
                .withImpressionRateCount(11);

        var result = addCampaign(campaign);

        MatcherAssert.assertThat(result.getValidationResult(),
                hasDefectWithDefinition(validationError(path(index(0),
                        field(CpmPriceCampaign.IMPRESSION_RATE_COUNT)),
                        DefectIds.MUST_BE_NULL)));
    }

    @Test
    @Description("в пакете одни параметры частоты, а в кампании другие - ошибка валидации")
    public void saveFrequencyLimit_packageDifferent() {
        var pricePackage = defaultPricePackage();
        pricePackage.getCampaignOptions().setShowsFrequencyLimit(new ShowsFrequencyLimit()
                .withFrequencyLimit(3)
                .withFrequencyLimitIsForCampaignTime(false));
        var pricePackageId = steps.pricePackageSteps().createPricePackage(pricePackage).getPricePackageId();
        var campaign = defaultCampaign(pricePackageId)
                .withImpressionRateCount(11);

        var result = addCampaign(campaign);

        MatcherAssert.assertThat(result.getValidationResult(),
                hasDefectWithDefinition(validationError(path(index(0),
                        field(CpmPriceCampaign.IMPRESSION_RATE_COUNT)),
                        DefectIds.INVALID_VALUE)));
    }

    @Test
    @Description("Режим минимальной частоты. Можно как в пакете")
    public void minFrequencyLimit_sameAsPackage() {
        var pricePackage = minFrequencyPricePackage();
        var pricePackageId = steps.pricePackageSteps().createPricePackage(pricePackage).getPricePackageId();
        var campaign = defaultCampaign(pricePackageId)
                .withImpressionRateCount(3)
                .withImpressionRateIntervalDays(7);

        var result = addCampaign(campaign);

        assertThat(result.getValidationResult().flattenErrors()).isEmpty();
        CpmPriceCampaign actualCampaign = getCampaignFromResult(result);
        assertThat(actualCampaign.getImpressionRateCount()).isEqualTo(campaign.getImpressionRateCount());
    }

    @Test
    @Description("Режим минимальной частоты. Можно числа болше чем в пакете")
    public void minFrequencyLimit_greaterAsPackage() {
        var pricePackage = minFrequencyPricePackage();
        var pricePackageId = steps.pricePackageSteps().createPricePackage(pricePackage).getPricePackageId();
        var campaign = defaultCampaign(pricePackageId)
                .withImpressionRateCount(4)
                .withImpressionRateIntervalDays(8);

        var result = addCampaign(campaign);

        assertThat(result.getValidationResult().flattenErrors()).isEmpty();
        CpmPriceCampaign actualCampaign = getCampaignFromResult(result);
        assertThat(actualCampaign.getImpressionRateCount()).isEqualTo(campaign.getImpressionRateCount());
    }

    @Test
    @Description("Режим минимальной частоты. Не может быть пусто")
    public void minFrequencyLimit_empty() {
        var pricePackage = minFrequencyPricePackage();
        var pricePackageId = steps.pricePackageSteps().createPricePackage(pricePackage).getPricePackageId();
        var campaign = defaultCampaign(pricePackageId);

        var result = addCampaign(campaign);

        MatcherAssert.assertThat(result.getValidationResult(),
                hasDefectWithDefinition(validationError(path(index(0),
                        field(CpmPriceCampaign.IMPRESSION_RATE_COUNT)),
                        DefectIds.CANNOT_BE_NULL)));
    }

    @Test
    @Description("Режим минимальной частоты. Нельзя задать частоту меньше чем в пакете")
    public void minFrequencyLimit_tooLow() {
        var pricePackage = minFrequencyPricePackage();
        var pricePackageId = steps.pricePackageSteps().createPricePackage(pricePackage).getPricePackageId();
        var campaign = defaultCampaign(pricePackageId)
                .withImpressionRateCount(2)
                .withImpressionRateIntervalDays(7);

        var result = addCampaign(campaign);

        MatcherAssert.assertThat(result.getValidationResult(),
                hasDefectWithDefinition(validationError(path(index(0),
                        field(CpmPriceCampaign.IMPRESSION_RATE_COUNT)),
                        DefectIds.INVALID_VALUE)));
    }

    @Test
    @Description("Для прайсового видео сохраняем кампанию с пустым viewTypes")
    public void cpmVideo_emptyViewTypes() {
        var pricePackage = defaultPricePackage();
        pricePackage.setAvailableAdGroupTypes(Set.of(AdGroupType.CPM_VIDEO));
        pricePackage.getTargetingsFixed().setViewTypes(List.of(ViewType.DESKTOP));
        var pricePackageId = steps.pricePackageSteps().createPricePackage(pricePackage).getPricePackageId();
        var campaign = defaultCampaign(pricePackageId);
        campaign.getFlightTargetingsSnapshot().setViewTypes(emptyList());

        var result = addCampaign(campaign);

        assertThat(result.getValidationResult().flattenErrors()).isEmpty();
        CpmPriceCampaign actualCampaign = getCampaignFromResult(result);
        assertThat(actualCampaign.getFlightTargetingsSnapshot().getViewTypes()).isEmpty();
    }

    @Test
    @Description("Для прайсового видео на морде сохраняем кампанию с непустым viewTypes")
    public void cpmFrontpageVideo_ViewTypes() {
        var pricePackage = defaultPricePackage();
        pricePackage.setAvailableAdGroupTypes(Set.of(AdGroupType.CPM_VIDEO));
        pricePackage.getTargetingsFixed().setViewTypes(List.of(ViewType.DESKTOP));
        pricePackage.setIsFrontpage(true);
        var pricePackageId = steps.pricePackageSteps().createPricePackage(pricePackage).getPricePackageId();
        var campaign = defaultCampaign(pricePackageId);
        campaign.getFlightTargetingsSnapshot().setViewTypes(List.of(ViewType.DESKTOP));

        var result = addCampaign(campaign);

        assertThat(result.getValidationResult().flattenErrors()).isEmpty();
        CpmPriceCampaign actualCampaign = getCampaignFromResult(result);
        assertThat(actualCampaign.getFlightTargetingsSnapshot().getViewTypes()).containsExactly(ViewType.DESKTOP);
    }

    @Test
    @Description("На пакете разрешено ограничение видео инвентаря - сохраняется")
    public void disabledVideoPlacements_enable() {
        var pricePackage = defaultPricePackage();
        pricePackage.getCampaignOptions().withAllowDisabledVideoPlaces(true);
        var pricePackageId = steps.pricePackageSteps().createPricePackage(pricePackage).getPricePackageId();
        var campaign = defaultCampaign(pricePackageId)
                .withDisabledVideoPlacements(DISABLED_VIDEO_PLACEMENTS);

        var result = addCampaign(campaign);

        assertThat(result.getValidationResult().flattenErrors()).isEmpty();
        CpmPriceCampaign actualCampaign = getCampaignFromResult(result);
        assertThat(actualCampaign.getDisabledVideoPlacements()).containsOnlyElementsOf(DISABLED_VIDEO_PLACEMENTS);
    }

    @Test
    @Description("На пакете запрещено ограничение видео инвентаря - ошибка валидации")
    public void disabledVideoPlacements_forbidden() {
        var pricePackage = defaultPricePackage();
        pricePackage.getCampaignOptions().withAllowDisabledVideoPlaces(false);
        var pricePackageId = steps.pricePackageSteps().createPricePackage(pricePackage).getPricePackageId();
        var campaign = defaultCampaign(pricePackageId)
                .withDisabledVideoPlacements(DISABLED_VIDEO_PLACEMENTS);

        var result = addCampaign(campaign);

        MatcherAssert.assertThat(result.getValidationResult(),
                hasDefectWithDefinition(validationError(path(index(0),
                        field(CpmPriceCampaign.DISABLED_VIDEO_PLACEMENTS)),
                        mustBeEmpty())));
    }

    @Test
    @Description("На пакете разрешено ограничение площадок - сохраняется")
    public void disabledPlaces_enable() {
        var pricePackage = defaultPricePackage();
        pricePackage.getCampaignOptions().withAllowDisabledPlaces(true);
        var pricePackageId = steps.pricePackageSteps().createPricePackage(pricePackage).getPricePackageId();
        var campaign = defaultCampaign(pricePackageId)
                .withDisabledDomains(List.of(DOMAIN, WWW + ANOTHER_DOMAIN));

        var result = addCampaign(campaign);

        assertThat(result.getValidationResult().flattenErrors()).isEmpty();
        CpmPriceCampaign actualCampaign = getCampaignFromResult(result);
        assertThat(actualCampaign.getDisabledDomains()).containsExactlyInAnyOrder(DOMAIN, ANOTHER_DOMAIN);
    }

    @Test
    @Description("На пакете запрещено ограничение площадок - ошибка валидации")
    public void disabledPlaces_forbidden() {
        var pricePackage = defaultPricePackage();
        pricePackage.getCampaignOptions().withAllowDisabledPlaces(false);
        var pricePackageId = steps.pricePackageSteps().createPricePackage(pricePackage).getPricePackageId();
        var campaign = defaultCampaign(pricePackageId)
                .withDisabledDomains(List.of(DOMAIN, WWW + ANOTHER_DOMAIN));

        var result = addCampaign(campaign);

        MatcherAssert.assertThat(result.getValidationResult(),
                hasDefectWithDefinition(validationError(path(index(0),
                        field(CpmPriceCampaign.DISABLED_DOMAINS)),
                        mustBeEmpty())));
    }

    @Test
    @Description("На пакете заданы корректировки по типу инвентаря - проросли на кампанию")
    public void bidModifiers() {
        var pricePackage = defaultPricePackage()
                .withBidModifiers(List.of(
                        new BidModifierInventory()
                                .withType(BidModifierType.INVENTORY_MULTIPLIER)
                                .withInventoryAdjustments(List.of(
                                        new BidModifierInventoryAdjustment().withInventoryType(InventoryType.INPAGE),
                                        new BidModifierInventoryAdjustment().withInventoryType(InventoryType.INSTREAM_WEB)
                                ))));
        var pricePackageId = steps.pricePackageSteps().createPricePackage(pricePackage).getPricePackageId();
        var campaign = defaultCampaign(pricePackageId);

        var result = addCampaign(campaign);

          (result.getValidationResult().flattenErrors()).isEmpty();
        CpmPriceCampaign actualCampaign = getCampaignFromResult(result);
        var bidModifiers = actualCampaign.getBidModifiers();
        assertThat(bidModifiers)
                .as("Ожидаются только определённые типы корректировок")
                .filteredOn(it -> it.getType() == BidModifierType.INVENTORY_MULTIPLIER)
                .first()
                .extracting( t -> ((BidModifierInventory)t).getInventoryAdjustments())
                .asInstanceOf(InstanceOfAssertFactories.list(BidModifierInventoryAdjustment.class))
                .extracting(BidModifierInventoryAdjustment::getInventoryType)
                .isSubsetOf(InventoryType.INAPP, InventoryType.INBANNER, InventoryType.REWARDED)
                .doesNotContain(InventoryType.INPAGE, InventoryType.INSTREAM_WEB);
    }

    @Test
    @Description("На пакете не заданы корректировки по типу инвентаря - на кампании нет корректировок")
    public void bidModifiers_empty() {
        var pricePackage = defaultPricePackage()
                .withBidModifiers(emptyList());
        var pricePackageId = steps.pricePackageSteps().createPricePackage(pricePackage).getPricePackageId();
        var campaign = defaultCampaign(pricePackageId);

        var result = addCampaign(campaign);

        assertThat(result.getValidationResult().flattenErrors()).isEmpty();
        CpmPriceCampaign actualCampaign = getCampaignFromResult(result);
        var bidModifiers = actualCampaign.getBidModifiers();
        assertThat(bidModifiers).isNull();
    }

    @Test
    @Description("На пакете разрешен весь инвентарь - на кампании нет корректировок")
    public void bidModifiers_full() {
        var pricePackage = defaultPricePackage()
                .withBidModifiers(List.of(
                        new BidModifierInventory()
                                .withType(BidModifierType.INVENTORY_MULTIPLIER)
                                .withInventoryAdjustments(List.of(
                                        new BidModifierInventoryAdjustment().withInventoryType(InventoryType.INSTREAM_WEB),
                                        new BidModifierInventoryAdjustment().withInventoryType(InventoryType.INPAGE),
                                        new BidModifierInventoryAdjustment().withInventoryType(InventoryType.INAPP),
                                        new BidModifierInventoryAdjustment().withInventoryType(InventoryType.INTERSTITIAL),
                                        new BidModifierInventoryAdjustment().withInventoryType(InventoryType.INBANNER),
                                        new BidModifierInventoryAdjustment().withInventoryType(InventoryType.REWARDED),
                                        new BidModifierInventoryAdjustment().withInventoryType(InventoryType.PREROLL),
                                        new BidModifierInventoryAdjustment().withInventoryType(InventoryType.MIDROLL),
                                        new BidModifierInventoryAdjustment().withInventoryType(InventoryType.POSTROLL),
                                        new BidModifierInventoryAdjustment().withInventoryType(InventoryType.PAUSEROLL),
                                        new BidModifierInventoryAdjustment().withInventoryType(InventoryType.OVERLAY),
                                        new BidModifierInventoryAdjustment().withInventoryType(InventoryType.POSTROLL_OVERLAY),
                                        new BidModifierInventoryAdjustment().withInventoryType(InventoryType.POSTROLL_WRAPPER),
                                        new BidModifierInventoryAdjustment().withInventoryType(InventoryType.INROLL_OVERLAY),
                                        new BidModifierInventoryAdjustment().withInventoryType(InventoryType.INROLL),
                                        new BidModifierInventoryAdjustment().withInventoryType(InventoryType.FULLSCREEN)
                                ))));
        var pricePackageId = steps.pricePackageSteps().createPricePackage(pricePackage).getPricePackageId();
        var campaign = defaultCampaign(pricePackageId);

        var result = addCampaign(campaign);

        assertThat(result.getValidationResult().flattenErrors()).isEmpty();
        CpmPriceCampaign actualCampaign = getCampaignFromResult(result);
        var bidModifiers = actualCampaign.getBidModifiers();
        assertThat(bidModifiers).isNull();
    }

    @Test
    @Description("применять валидацию allowExpandedDesktopCreative на кампании только для пакета на главной")
    public void allowExpandedDesktopCreative_cpmVideo_validationDisabled() {
        var pricePackage = defaultPricePackage()
                .withAvailableAdGroupTypes(Set.of(AdGroupType.CPM_VIDEO));
        pricePackage.getTargetingsFixed().setAllowExpandedDesktopCreative(true);
        var pricePackageId = steps.pricePackageSteps().createPricePackage(pricePackage).getPricePackageId();
        var campaign = defaultCampaign(pricePackageId);
        campaign.getFlightTargetingsSnapshot().setAllowExpandedDesktopCreative(false);

        var result = addCampaign(campaign);

        assertThat(result.getValidationResult().flattenErrors()).isEmpty();
    }

    @Test
    @Description("Сохраняется верификатор moat на прайсовой кампании с фичей")
    public void save_moat_valid() {
        steps.featureSteps().addClientFeature(defaultClient.getClientId(), MOAT_MEASURER_CAMP, true);
        steps.featureSteps().addClientFeature(defaultClient.getClientId(), MOAT_USE_UNSTABLE_SCRIPT, true);
        var pricePackageId = steps.pricePackageSteps().createPricePackage(defaultPricePackage()).getPricePackageId();
        var campaign = defaultCampaign(pricePackageId);
        campaign.setMeasurers(List.of(new CampaignMeasurer()
                .withMeasurerSystem(CampaignMeasurerSystem.MOAT)
                .withParams("{}")
        ));

        var result = addCampaign(campaign);

        assertThat(result.getValidationResult().flattenErrors()).isEmpty();
        CpmPriceCampaign actualCampaign = getCampaignFromResult(result);
        assertThat(actualCampaign.getMeasurers()).isNotEmpty();
        var params = actualCampaign.getMeasurers().get(0).getParams();
        //MOAT_USE_UNSTABLE_SCRIPT учитывается
        assertThat(params).containsIgnoringCase("use_unstable_script");
    }

    @Test
    @Description("Сохраняется верификатор moat на прайсовой кампании без фичи не пройдём валидацию")
    public void save_moat_invalid() {
        steps.featureSteps().addClientFeature(defaultClient.getClientId(), MOAT_MEASURER_CAMP, false);
        var pricePackageId = steps.pricePackageSteps().createPricePackage(defaultPricePackage()).getPricePackageId();
        var campaign = defaultCampaign(pricePackageId);
        campaign.setMeasurers(List.of(new CampaignMeasurer()
                .withMeasurerSystem(CampaignMeasurerSystem.MOAT)
                .withParams("{\"use_unstable_script\": true}")
        ));

        var result = addCampaign(campaign);

        MatcherAssert.assertThat(result.getValidationResult(),
                hasDefectWithDefinition(validationError(path(index(0),
                                field(CpmPriceCampaign.MEASURERS), index(0)),
                        DefectIds.INVALID_VALUE)));
    }
    @Test
    @Description("Сохраняется верификатор ias на прайсовой кампании с фичей")
    public void save_ias_valid() {
        steps.featureSteps().addClientFeature(defaultClient.getClientId(), IAS_MEASURER, true);
        var pricePackageId = steps.pricePackageSteps().createPricePackage(defaultPricePackage()).getPricePackageId();
        var campaign = defaultCampaign(pricePackageId);
        campaign.setMeasurers(List.of(new CampaignMeasurer()
                .withMeasurerSystem(CampaignMeasurerSystem.IAS)
                .withParams("{\"advid\":123,\"pubid\":123}")
        ));

        var result = addCampaign(campaign);

        assertThat(result.getValidationResult().flattenErrors()).isEmpty();
        CpmPriceCampaign actualCampaign = getCampaignFromResult(result);
        assertThat(actualCampaign.getMeasurers()).isNotEmpty();
        var params = actualCampaign.getMeasurers().get(0).getParams();
    }

    @Test
    @Description("Сохраняется верификатор ias на прайсовой кампании без фичи не пройдём валидацию")
    public void save_ias_invalid() {
        steps.featureSteps().addClientFeature(defaultClient.getClientId(), IAS_MEASURER, false);
        var pricePackageId = steps.pricePackageSteps().createPricePackage(defaultPricePackage()).getPricePackageId();
        var campaign = defaultCampaign(pricePackageId);
        campaign.setMeasurers(List.of(new CampaignMeasurer()
                .withMeasurerSystem(CampaignMeasurerSystem.IAS)
                .withParams("{\"advid\":123,\"pubid\":123}")
        ));

        var result = addCampaign(campaign);

        MatcherAssert.assertThat(result.getValidationResult(),
                hasDefectWithDefinition(validationError(path(index(0),
                                field(CpmPriceCampaign.MEASURERS), index(0)),
                        DefectIds.INVALID_VALUE)));
    }

    @Test
    public void strategyCPD() {
        var pricePackage = steps.pricePackageSteps().createPricePackage(defaultPricePackage()
                        .withIsCpd(true)
                        .withCurrency(CurrencyCode.RUB)
                        .withOrderVolumeMin(25_000_000L)
                        .withOrderVolumeMax(25_000_000L)
                        .withPrice(BigDecimal.valueOf(50_000_000L)));

        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        var startDate = LocalDate.of(currentYear + 2, 1, 1);
        var endDate = LocalDate.of(currentYear + 2, 1, 1);
        var campaign = defaultCampaign(pricePackage.getPricePackageId())
                .withStartDate(startDate)
                .withEndDate(endDate)
                .withFlightOrderVolume(25_000_000L);

        BigDecimal expectedBudget = BigDecimal.valueOf(50_000_000L);

        var result = addCampaign(campaign);
        assertThat(result.getValidationResult().flattenErrors()).isEmpty();

        var actualCampaign = getCampaignFromResult(result);
        var expectedCampaign = new CpmPriceCampaign()
                .withStrategy((DbStrategy) new DbStrategy()
                        .withStrategy(CampOptionsStrategy.DIFFERENT_PLACES)
                        .withStrategyName(StrategyName.PERIOD_FIX_BID)
                        .withStrategyData(new StrategyData()
                                .withName("period_fix_bid")
                                .withVersion(1L)
                                .withAutoProlongation(0L)
                                .withStart(startDate)
                                .withFinish(endDate)
                                .withBudget(expectedBudget))
                        .withAutobudget(CampaignsAutobudget.NO)
                        .withPlatform(CampaignsPlatform.CONTEXT));
        Assertions.assertThat(actualCampaign).is(matchedBy(beanDiffer(expectedCampaign)
                .useCompareStrategy(CPM_PRICE_CAMPAIGN_COMPARE_STRATEGY)));
    }

    private CpmPriceCampaign defaultCampaign(Long pricePackageId) {
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        return new CpmPriceCampaign()
                .withType(CampaignType.CPM_PRICE)
                .withCurrency(defaultClient.getClient().getWorkCurrency())
                .withName("Campaign name")
                .withStartDate(LocalDate.of(currentYear + 1, 1, 1))
                .withEndDate(LocalDate.of(currentYear + 1, 12, 1))
                .withTimeZoneId(DEFAULT_TIMEZONE)
                .withMetrikaCounters(null)
                .withHasAddMetrikaTagToUrl(DEFAULT_ADD_METRIKA_TAG_TO_URL)
                .withHasAddOpenstatTagToUrl(DEFAULT_ADD_OPENSTAT_TAG_TO_URL)
                .withContentLanguage(ContentLanguage.RU)
                .withAttributionModel(campaignConstantsService.getDefaultAttributionModel())
                .withSmsTime(new TimeInterval().withEndHour(12).withEndMinute(30).withStartHour(1).withStartMinute(15))
                .withSmsFlags(EnumSet.of(SmsFlag.MODERATE_RESULT_SMS, SmsFlag.CAMP_FINISHED_SMS))
                .withEmail("1@1.ru")
                .withWarningBalance(20)
                .withEnableSendAccountNews(false)
                .withEnablePausedByDayBudgetEvent(false)
                .withCheckPositionIntervalEvent(CampaignWarnPlaceInterval._30)
                .withEnableOfflineStatNotice(false)
                .withEnableCheckPositionEvent(true)
                .withPricePackageId(pricePackageId)
                .withFlightTargetingsSnapshot(new PriceFlightTargetingsSnapshot()
                        .withGeoType(REGION_TYPE_DISTRICT)
                        .withGeoExpanded(List.of(VOLGA_DISTRICT, SIBERIAN_DISTRICT))
                        .withViewTypes(List.of(ViewType.DESKTOP, ViewType.MOBILE, ViewType.NEW_TAB))
                        .withAllowExpandedDesktopCreative(true))
                .withFlightOrderVolume(500000L)
                .withStrategyId(null);
    }

    private PricePackage videoPricePackage() {
        return defaultPricePackage()
                .withAvailableAdGroupTypes(Set.of(AdGroupType.CPM_VIDEO));
    }

    private PricePackage defaultPricePackage() {
        return approvedPricePackage()
                .withPrice(BigDecimal.valueOf(200L))
                .withCurrency(defaultClient.getClient().getWorkCurrency())
                .withOrderVolumeMin(100L)
                .withOrderVolumeMax(1000000L)
                .withTargetingsFixed(new TargetingsFixed()
                        .withGeo(List.of(VOLGA_DISTRICT, SIBERIAN_DISTRICT))
                        .withGeoType(REGION_TYPE_DISTRICT)
                        .withGeoExpanded(List.of(VOLGA_DISTRICT, SIBERIAN_DISTRICT))
                        .withViewTypes(List.of(ViewType.DESKTOP, ViewType.MOBILE, ViewType.NEW_TAB))
                        .withAllowExpandedDesktopCreative(true))
                .withTargetingsCustom(new TargetingsCustom())
                .withDateStart(LocalDate.of(2017, 1, 1))
                .withDateEnd(LocalDate.of(2222, 1, 1))
                .withClients(List.of(allowedPricePackageClient(defaultClient)))
                .withIsPublic(false)
                .withIsArchived(false);
    }

    private PricePackage minFrequencyPricePackage() {
        var pricePackage = defaultPricePackage();
        pricePackage.getCampaignOptions().setShowsFrequencyLimit(new ShowsFrequencyLimit()
                .withFrequencyLimit(3)
                .withFrequencyLimitDays(7)
                .withMinLimit(true)
                .withFrequencyLimitIsForCampaignTime(false));
        return pricePackage;
    }

    private MassResult<Long> addCampaign(CpmPriceCampaign campaign) {
        var options = new CampaignOptions();
        RestrictedCampaignsAddOperation addOperation = new RestrictedCampaignsAddOperation(
                List.of(campaign),
                defaultClient.getShard(),
                UidAndClientId.of(defaultClient.getUid(), defaultClient.getClientId()),
                defaultClient.getUid(),
                campaignModifyRepository,
                strategyTypedRepository,
                addRestrictedCampaignValidationService,
                campaignAddOperationSupportFacade,
                dslContextProvider,
                rbacService, options, metrikaClientFactory, goalUtilsService);
        return addOperation.prepareAndApply();
    }

    private CpmPriceCampaign getCampaignFromResult(MassResult<Long> result) {
        return (CpmPriceCampaign) campaignTypedRepository.getTypedCampaigns(defaultClient.getShard(),
                List.of(result.get(0).getResult())).get(0);
    }

    private void assertValidationErrorOnProperty(MassResult<Long> result, ModelProperty<?, ?> property,
                                                 Defect<?> defect) {
        Assert.assertThat(result.getValidationResult(), hasDefectWithDefinition(
                validationError(path(index(0), field(property)), defect)));
    }

}
