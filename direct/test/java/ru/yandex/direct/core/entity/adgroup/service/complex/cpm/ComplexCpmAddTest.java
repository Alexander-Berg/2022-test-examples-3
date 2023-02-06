package ru.yandex.direct.core.entity.adgroup.service.complex.cpm;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import jdk.jfr.Description;
import one.util.streamex.StreamEx;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.container.ComplexCpmAdGroup;
import ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.model.AdShowType;
import ru.yandex.direct.core.entity.adgroup.model.StatusShowsForecast;
import ru.yandex.direct.core.entity.adgroup.model.UsersSegment;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.adgroup.service.complex.ComplexAdGroupAddOperationFactory;
import ru.yandex.direct.core.entity.adgroup.service.complex.ComplexAdGroupTestCommons;
import ru.yandex.direct.core.entity.banner.model.Banner;
import ru.yandex.direct.core.entity.banner.model.BannerWithAdditionalHrefs;
import ru.yandex.direct.core.entity.banner.model.BannerWithHref;
import ru.yandex.direct.core.entity.banner.model.BannerWithMeasurers;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerWithTurboLanding;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.banner.type.pixels.InventoryType;
import ru.yandex.direct.core.entity.bidmodifier.ComplexBidModifier;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign;
import ru.yandex.direct.core.entity.crypta.repository.CryptaSegmentRepository;
import ru.yandex.direct.core.entity.currency.model.cpmyndxfrontpage.FrontpageCampaignShowType;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.placements.model1.IndoorPlacement;
import ru.yandex.direct.core.entity.placements.model1.OutdoorPlacement;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackage;
import ru.yandex.direct.core.entity.pricepackage.model.ViewType;
import ru.yandex.direct.core.entity.retargeting.model.ConditionType;
import ru.yandex.direct.core.entity.retargeting.model.CryptaGoalScope;
import ru.yandex.direct.core.entity.retargeting.model.CryptaInterestType;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.GoalType;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingConditionBase;
import ru.yandex.direct.core.entity.retargeting.model.Rule;
import ru.yandex.direct.core.entity.retargeting.model.RuleType;
import ru.yandex.direct.core.entity.retargeting.model.TargetInterest;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.core.testing.repository.TestCpmYndxFrontpageRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.MetrikaHelperStub;
import ru.yandex.direct.core.validation.defects.params.CurrencyAmountDefectParams;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.regions.GeoTree;
import ru.yandex.direct.regions.GeoTreeFactory;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.allFields;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.entity.adgroup.container.ComplexCpmAdGroup.TARGET_INTERESTS;
import static ru.yandex.direct.core.entity.adgroup.service.AdGroupCpmPriceUtils.PRIORITY_DEFAULT;
import static ru.yandex.direct.core.entity.adgroup.service.AdGroupCpmPriceUtils.PRIORITY_SPECIFIC;
import static ru.yandex.direct.core.entity.adgroup.service.complex.ComplexTextAdGroupTestData.randomComplexBidModifierMobile;
import static ru.yandex.direct.core.entity.adgroup.service.complex.ComplexTextAdGroupTestData.randomPriceRetargeting;
import static ru.yandex.direct.core.entity.adgroup.service.complex.cpm.ComplexCpmAdGroupTestData.cpmBannerAdGroupWithKeywords;
import static ru.yandex.direct.core.entity.adgroup.service.complex.cpm.ComplexCpmAdGroupTestData.cpmBannerAdGroupWithRetargetings;
import static ru.yandex.direct.core.entity.adgroup.service.complex.cpm.ComplexCpmAdGroupTestData.cpmGeoproductAdGroup;
import static ru.yandex.direct.core.entity.adgroup.service.complex.cpm.ComplexCpmAdGroupTestData.cpmIndoorAdGroup;
import static ru.yandex.direct.core.entity.adgroup.service.complex.cpm.ComplexCpmAdGroupTestData.cpmOutdoorAdGroup;
import static ru.yandex.direct.core.entity.adgroup.service.complex.cpm.ComplexCpmAdGroupTestData.cpmVideoAdGroup;
import static ru.yandex.direct.core.entity.adgroup.service.complex.cpm.ComplexCpmAdGroupTestData.cpmYndxFrontpageAdGroup;
import static ru.yandex.direct.core.entity.adgroup.service.complex.cpm.ComplexCpmAdGroupTestData.cpmYndxFrontpageAdGroupForPriceSales;
import static ru.yandex.direct.core.entity.adgroup.service.complex.cpm.ComplexCpmAdGroupTestData.emptyAdGroup;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.cpmPriceAdGroupUseNotAllowedBidModifiers;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.keywordsNotAllowed;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.minusKeywordsNotAllowed;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.inconsistentCreativeFormat;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.inconsistentStateBannerTypeAndAdgroupType;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.noRightsToPixel;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.requiredCreativesWithHtml5TypeOnly;
import static ru.yandex.direct.core.entity.bidmodifier.BidModifierAdjustment.PERCENT;
import static ru.yandex.direct.core.entity.bidmodifier.BidModifierMobile.MOBILE_ADJUSTMENT;
import static ru.yandex.direct.core.entity.bids.validation.BidsDefects.CurrencyAmountDefects.CPM_PRICE_IS_NOT_GREATER_THAN_MIN;
import static ru.yandex.direct.core.entity.pricepackage.service.validation.PricePackageValidator.REGION_TYPE_REGION;
import static ru.yandex.direct.core.entity.retargeting.model.TargetInterest.RETARGETING_CONDITION_ID;
import static ru.yandex.direct.core.entity.retargeting.service.validation2.RetargetingDefects.retargetingConditionIsInvalidByDefaultAdGroup;
import static ru.yandex.direct.core.entity.retargeting.service.validation2.RetargetingDefects.retargetingConditionIsInvalidForPricePackage;
import static ru.yandex.direct.core.entity.retargeting.service.validation2.RetargetingDefects.retargetingConditionIsInvalidForRetargeting;
import static ru.yandex.direct.core.entity.userssegments.service.validation.UsersSegmentDefects.goalTypeNotSupportedInAdGroup;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.adriverPixelUrl;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.yaAudiencePixelUrl;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultCpmVideoAddition;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultGoalByType;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultGoalByTypeAndId;
import static ru.yandex.direct.core.testing.data.TestGroups.activeDefaultVideoAdGroupForPriceSales;
import static ru.yandex.direct.core.testing.data.TestKeywords.defaultClientKeyword;
import static ru.yandex.direct.core.testing.data.TestKeywords.keywordForCpmBanner;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.fullTextBanner;
import static ru.yandex.direct.core.testing.data.TestPricePackages.AGE_18_24;
import static ru.yandex.direct.core.testing.data.TestPricePackages.AGE_25_34;
import static ru.yandex.direct.core.testing.data.TestPricePackages.AGE_35_44;
import static ru.yandex.direct.core.testing.data.TestPricePackages.C1_INCOME_GOAL_ID;
import static ru.yandex.direct.core.testing.data.TestPricePackages.DEFAULT_RETARGETING_CONDITION;
import static ru.yandex.direct.core.testing.data.TestPricePackages.FEMALE_CRYPTA_GOAL_ID;
import static ru.yandex.direct.core.testing.data.TestPricePackages.LTV;
import static ru.yandex.direct.core.testing.data.TestPricePackages.MALE_CRYPTA_GOAL_ID;
import static ru.yandex.direct.core.testing.data.TestPricePackages.MID_INCOME_GOAL_ID;
import static ru.yandex.direct.core.testing.data.TestPricePackages.allowedPricePackageClient;
import static ru.yandex.direct.core.testing.data.TestPricePackages.approvedPricePackage;
import static ru.yandex.direct.core.testing.data.TestPricePackages.emptyTargetingsCustom;
import static ru.yandex.direct.core.testing.data.TestRegions.CENTRAL_DISTRICT;
import static ru.yandex.direct.core.testing.data.TestRegions.KRASNODAR_KRAI;
import static ru.yandex.direct.core.testing.data.TestRegions.RUSSIA;
import static ru.yandex.direct.core.testing.data.TestRegions.URYUPINSK_REGION_ID;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.defaultCpmRetCondition;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.defaultRetCondition;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.defaultRules;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.ltvGoal;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.ltvRule;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.ruleOrSocialDemo;
import static ru.yandex.direct.currency.Money.valueOf;
import static ru.yandex.direct.feature.FeatureName.CPM_PRICE_BANNER_ADDITIONAL_HREFS_ALLOWED;
import static ru.yandex.direct.regions.Region.KYIV_REGION_ID;
import static ru.yandex.direct.regions.Region.MOSCOW_REGION_ID;
import static ru.yandex.direct.regions.Region.REGION_TYPE_COUNTRY;
import static ru.yandex.direct.regions.Region.SAINT_PETERSBURG_REGION_ID;
import static ru.yandex.direct.regions.Region.SOUTH_FEDERAL_DISTRICT_REGION_ID;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.utils.CommonUtils.nvl;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.validation.defect.CollectionDefects.isEmptyCollection;
import static ru.yandex.direct.validation.defect.CommonDefects.invalidValue;
import static ru.yandex.direct.validation.defect.CommonDefects.isNull;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.defect.NumberDefects.lessThanOrEqualTo;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ComplexCpmAddTest {
    private static final CompareStrategy AD_GROUP_COMPARE_STRATEGY_WITH_STATUSES = onlyExpectedFields()
            .forFields(newPath("statusBsSynced")).useMatcher(is(StatusBsSynced.NO))
            .forFields(newPath("statusShowsForecast")).useMatcher(is(StatusShowsForecast.NEW));

    @Autowired
    private ComplexAdGroupAddOperationFactory addOperationFactory;
    @Autowired
    private GeoTreeFactory geoTreeFactory;
    private GeoTree geoTree;

    @Autowired
    private CryptaSegmentRepository cryptaSegmentRepository;

    @Autowired
    private AdGroupRepository adGroupRepository;
    @Autowired
    private BannerTypedRepository bannerTypedRepository;
    @Autowired
    private ComplexAdGroupTestCommons commonChecks;
    @Autowired
    private MetrikaHelperStub metrikaHelperStub;

    @Autowired
    private TestCpmYndxFrontpageRepository testCpmYndxFrontpageRepository;

    @Autowired
    private Steps steps;

    private ClientInfo clientInfo;
    private ClientId clientId;
    private CampaignInfo campaign;
    private CampaignInfo cpmYndxCampaign;
    private CpmPriceCampaign cpmPriceCampaign;
    private Goal behaviorGoalForPriceSales;
    private List<Rule> rules;
    private Long canvasCreativeId;
    private Long videoCreativeId;
    private Long html5CreativeIdForFrontpage;
    private Long html5CreativeIdForGeoproduct;
    private Long html5CreativeIdForPriceSales;
    private PricePackage pricePackage;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createClient(defaultClient().withWorkCurrency(CurrencyCode.RUB));
        clientId = clientInfo.getClientId();

        geoTree = geoTreeFactory.getGlobalGeoTree();
        testCpmYndxFrontpageRepository.fillMinBidsTestValues();

        campaign = steps.campaignSteps().createActiveCpmBannerCampaign(clientInfo);

        behaviorGoalForPriceSales = (Goal) ltvGoal()
                .withCryptaScope(Set.of(CryptaGoalScope.COMMON, CryptaGoalScope.INTERNAL_AD));
        pricePackage = approvedPricePackage()
                .withTargetingsCustom(emptyTargetingsCustom()
                        .withRetargetingCondition(
                                DEFAULT_RETARGETING_CONDITION.withCryptaSegments(
                                        List.of(AGE_18_24, AGE_25_34, AGE_35_44,
                                                MALE_CRYPTA_GOAL_ID, MID_INCOME_GOAL_ID, LTV)
                                )))
                .withClients(List.of(allowedPricePackageClient(clientInfo)));
        pricePackage.getTargetingsFixed()
                .withGeo(List.of(RUSSIA))
                .withGeoExpanded(List.of(RUSSIA))
                .withGeoType(REGION_TYPE_COUNTRY);
        steps.pricePackageSteps().createPricePackage(pricePackage);
        cpmPriceCampaign = steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo, pricePackage);
        setCpmPriceAdditionalHrefsFeature(true);

        cpmYndxCampaign = steps.campaignSteps().createActiveCpmYndxFrontpageCampaign();
        testCpmYndxFrontpageRepository.setCpmYndxFrontpageCampaignsAllowedFrontpageTypes(
                cpmYndxCampaign.getShard(),
                cpmYndxCampaign.getCampaignId(),
                singletonList(FrontpageCampaignShowType.FRONTPAGE));

        rules = defaultRules();
        metrikaHelperStub.addGoalsFromRules(campaign.getUid(), rules);
        cryptaSegmentRepository.add(singletonList(behaviorGoalForPriceSales));

        CreativeInfo creativeInfo = steps.creativeSteps().addDefaultCanvasCreative(campaign.getClientInfo());
        canvasCreativeId = creativeInfo.getCreativeId();

        CreativeInfo videoCreativeInfo = steps.creativeSteps()
                .addDefaultCpmVideoAdditionCreative(campaign.getClientInfo(),
                        steps.creativeSteps().getNextCreativeId());
        videoCreativeId = videoCreativeInfo.getCreativeId();

        CreativeInfo creativeInfoForFrontpage =
                steps.creativeSteps().addDefaultHtml5CreativeForFrontpage(cpmYndxCampaign.getClientInfo(),
                        steps.creativeSteps().getNextCreativeId());
        html5CreativeIdForFrontpage = creativeInfoForFrontpage.getCreativeId();

        CreativeInfo creativeInfoForGeoproduct =
                steps.creativeSteps().addDefaultHtml5CreativeForGeoproduct(campaign.getClientInfo(),
                        steps.creativeSteps().getNextCreativeId());
        html5CreativeIdForGeoproduct = creativeInfoForGeoproduct.getCreativeId();

        CreativeInfo creativeInfoForPriceSales =
                steps.creativeSteps().addDefaultHtml5CreativeForPriceSales(clientInfo, cpmPriceCampaign);
        html5CreativeIdForPriceSales = creativeInfoForPriceSales.getCreativeId();
    }

    @Test
    public void oneEmptyCpmBannerAdGroup() {
        ComplexCpmAdGroup complexAdGroup = emptyAdGroup(campaign.getCampaignId());
        ComplexCpmAdGroupAddOperation operation =
                createOperation(singletonList(complexAdGroup), campaign.getClientInfo());
        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result, isFullySuccessful());
        checkComplexAdGroup(complexAdGroup);
    }

    @Test
    public void oneFullCpmBannerAdGroupWithRetargetings() {
        RetargetingCondition retCondition = retargetingCondition(clientId, rules);
        ComplexCpmAdGroup complexAdGroup =
                cpmBannerAdGroupWithRetargetings(campaign.getCampaignId(), retCondition, canvasCreativeId);
        ComplexCpmAdGroupAddOperation operation =
                createOperation(singletonList(complexAdGroup), campaign.getClientInfo());
        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result, isFullySuccessful());
        checkComplexAdGroup(complexAdGroup);
    }

    @Test
    public void oneFullCpmBannerAdGroupWithKeywords() {
        ComplexCpmAdGroup complexAdGroup = cpmBannerAdGroupWithKeywords(campaign.getCampaignId(), canvasCreativeId);
        ComplexCpmAdGroupAddOperation operation =
                createOperation(singletonList(complexAdGroup), campaign.getClientInfo());
        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result, isFullySuccessful());
        checkComplexAdGroup(complexAdGroup);
    }

    @Test
    public void addOneFullCpmGeoproductAdGroup_noError() {
        RetargetingCondition retCondition = retargetingCondition(clientId, rules);
        Long turbolandingId = steps.turboLandingSteps().createDefaultBannerTurboLanding(clientId).getId();
        ComplexCpmAdGroup complexAdGroup = cpmGeoproductAdGroup(campaign.getCampaignId(), turbolandingId,
                retCondition, html5CreativeIdForGeoproduct);
        ComplexCpmAdGroupAddOperation operation =
                createOperation(singletonList(complexAdGroup), campaign.getClientInfo());
        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result, isFullySuccessful());
        checkComplexAdGroup(complexAdGroup);
    }

    @Test
    public void addOneFullCpmGeoproductAdGroupWithHref_ValidationError() {
        RetargetingCondition retCondition = retargetingCondition(clientId, rules);
        Long turbolandingId = steps.turboLandingSteps().createDefaultBannerTurboLanding(clientId).getId();
        ComplexCpmAdGroup complexAdGroup = cpmGeoproductAdGroup(campaign.getCampaignId(), turbolandingId,
                retCondition, html5CreativeIdForGeoproduct);
        ((BannerWithHref) complexAdGroup.getBanners().get(0)).withHref("http://www.yandex.ru");
        ComplexCpmAdGroupAddOperation operation =
                createOperation(singletonList(complexAdGroup), campaign.getClientInfo());
        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result.getValidationResult(), hasDefectDefinitionWith(validationError(
                path(index(0), field(AdGroup.BANNERS), index(0), field(CpmBanner.HREF)),
                isNull())));
    }

    @Test
    public void addOneFullCpmGeoproductAdGroupWithoutTurbolanding_ValidationError() {
        RetargetingCondition retCondition = retargetingCondition(clientId, rules);
        ComplexCpmAdGroup complexAdGroup = cpmGeoproductAdGroup(campaign.getCampaignId(), null,
                retCondition, html5CreativeIdForGeoproduct);
        ComplexCpmAdGroupAddOperation operation =
                createOperation(singletonList(complexAdGroup), campaign.getClientInfo());
        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result.getValidationResult(), hasDefectDefinitionWith(validationError(
                path(index(0), field(AdGroup.BANNERS), index(0), field(OldBannerWithTurboLanding.TURBO_LANDING_ID)),
                notNull())));
    }

    @Test
    public void cpmGeoproductAdGroupWithInvalidCreative_ValidationError() {
        RetargetingCondition retCondition = retargetingCondition(clientId, rules);
        Long turbolandingId = steps.turboLandingSteps().createDefaultBannerTurboLanding(clientId).getId();
        ComplexCpmAdGroup complexAdGroup = cpmGeoproductAdGroup(campaign.getCampaignId(), turbolandingId, retCondition,
                canvasCreativeId);

        ComplexCpmAdGroupAddOperation operation =
                createOperation(singletonList(complexAdGroup), campaign.getClientInfo());
        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(
                        path(index(0), field(AdGroup.BANNERS), index(0), field(CpmBanner.CREATIVE_ID)),
                        inconsistentCreativeFormat())));
    }

    @Test
    public void oneFullCpmVideoAdGroup() {
        RetargetingCondition retCondition = retargetingCondition(clientId, rules);
        ComplexCpmAdGroup complexAdGroup =
                cpmVideoAdGroup(campaign.getCampaignId(), retCondition, videoCreativeId);
        ComplexCpmAdGroupAddOperation operation =
                createOperation(singletonList(complexAdGroup), campaign.getClientInfo());
        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result, isFullySuccessful());
        checkComplexAdGroup(complexAdGroup);
    }

    @Test
    public void adGroupWithBannerWithValidationError() {
        ComplexCpmAdGroup complexAdGroup = emptyAdGroup(campaign.getCampaignId())
                .withBanners(singletonList(fullTextBanner(campaign.getCampaignId(), null)));
        ComplexCpmAdGroupAddOperation operation =
                createOperation(singletonList(complexAdGroup), campaign.getClientInfo());
        MassResult<Long> result = operation.prepareAndApply();
        Path errPath = path(index(0), field(ComplexCpmAdGroup.BANNERS), index(0));
        assertThat(result.getValidationResult(), hasDefectDefinitionWith(
                validationError(errPath, inconsistentStateBannerTypeAndAdgroupType())));
    }

    @Test
    public void adGroupWithKeywordsWithValidationError() {
        ComplexCpmAdGroup complexCpmAdGroup = emptyAdGroup(campaign.getCampaignId())
                .withKeywords(singletonList(defaultClientKeyword().withPrice(BigDecimal.ONE)));
        ComplexCpmAdGroupAddOperation operation =
                createOperation(singletonList(complexCpmAdGroup), campaign.getClientInfo());
        MassResult<Long> result = operation.prepareAndApply();
        Path errPath = path(index(0), field(ComplexTextAdGroup.KEYWORDS.name()), index(0), field(Keyword.PRICE.name()));
        Defect error = new Defect<>(CPM_PRICE_IS_NOT_GREATER_THAN_MIN,
                new CurrencyAmountDefectParams(valueOf(BigDecimal.valueOf(5), CurrencyCode.RUB)));
        assertThat(result.getValidationResult(), hasDefectDefinitionWith(validationError(errPath, error)));
    }

    @Test
    public void adGroupWithRetargetingWithValidationError() {
        RetargetingCondition retCondition = retargetingCondition(clientId, null);
        ComplexCpmAdGroup complexCpmAdGroup = emptyAdGroup(campaign.getCampaignId())
                .withTargetInterests(
                        singletonList(randomPriceRetargeting(retCondition.getId())))
                .withRetargetingConditions(singletonList(retCondition));
        ComplexCpmAdGroupAddOperation operation =
                createOperation(singletonList(complexCpmAdGroup), campaign.getClientInfo());
        MassResult<Long> result = operation.prepareAndApply();
        Path errPath = path(index(0), field(ComplexTextAdGroup.TARGET_INTERESTS.name()), index(0),
                field(RETARGETING_CONDITION_ID.name()));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(errPath, retargetingConditionIsInvalidForRetargeting())));
    }

    @Test
    public void adGroupWithBidModifierWithValidationError() {
        ComplexCpmAdGroup complexAdGroup = cpmBannerAdGroupWithKeywords(campaign.getCampaignId(), canvasCreativeId);
        complexAdGroup.getComplexBidModifier().getMobileModifier().getMobileAdjustment().withPercent(400_000);

        ComplexCpmAdGroupAddOperation operation =
                createOperation(singletonList(complexAdGroup), campaign.getClientInfo());
        MassResult<Long> result = operation.prepareAndApply();
        Path errPath = path(index(0), field(ComplexTextAdGroup.COMPLEX_BID_MODIFIER.name()),
                field(ComplexBidModifier.MOBILE_MODIFIER), field(MOBILE_ADJUSTMENT), field(PERCENT));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(errPath, lessThanOrEqualTo(1300))));
    }

    @Test
    public void fullCpmOutdoorAdGroup() {
        Long videoCreativeId = steps.creativeSteps().getNextCreativeId();
        RetargetingCondition retCondition = retargetingCondition(clientId, rules);
        steps.creativeSteps().addDefaultCpmOutdoorVideoCreative(campaign.getClientInfo(), videoCreativeId);
        OutdoorPlacement placement = steps.placementSteps().addDefaultOutdoorPlacementWithOneBlock();
        ComplexCpmAdGroup complexAdGroup =
                cpmOutdoorAdGroup(campaign.getCampaignId(), placement, retCondition, videoCreativeId);
        ComplexCpmAdGroupAddOperation operation =
                createOperation(singletonList(complexAdGroup), campaign.getClientInfo());
        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result, isFullySuccessful());
        checkComplexAdGroup(complexAdGroup);
    }

    @Test
    public void cpmOutdoorAdGroupWithKeywords_ValidationError() {
        RetargetingCondition retCondition = retargetingCondition(clientId, rules);
        OutdoorPlacement placement = steps.placementSteps().addDefaultOutdoorPlacementWithOneBlock();
        ComplexCpmAdGroup complexAdGroup = cpmOutdoorAdGroup(campaign.getCampaignId(), placement, retCondition, null)
                .withKeywords(singletonList(keywordForCpmBanner()));

        ComplexCpmAdGroupAddOperation operation =
                createOperation(singletonList(complexAdGroup), campaign.getClientInfo());
        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(0)), keywordsNotAllowed())));
    }

    @Test
    public void cpmOutdoorAdGroupWithVideoGoalsWithWrongType_ValidationError() {
        RetargetingCondition retCondition = retargetingCondition(clientId, rules);
        OutdoorPlacement placement = steps.placementSteps().addDefaultOutdoorPlacementWithOneBlock();
        ComplexCpmAdGroup complexAdGroup =
                cpmOutdoorAdGroup(campaign.getCampaignId(), placement, retCondition, null)
                        .withUsersSegments(singletonList(new UsersSegment().withType(AdShowType.THIRD_QUARTILE)));

        ComplexCpmAdGroupAddOperation operation =
                createOperation(singletonList(complexAdGroup), campaign.getClientInfo());
        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result.getValidationResult(), hasDefectDefinitionWith(
                validationError(path(index(0), field("usersSegments"), index(0), field(UsersSegment.TYPE)),
                        goalTypeNotSupportedInAdGroup())));
    }

    @Test
    public void fullCpmIndoorAdGroup() {
        Long videoCreativeId = steps.creativeSteps().getNextCreativeId();
        RetargetingCondition retCondition =
                steps.retConditionSteps().createIndoorRetCondition(campaign.getClientInfo()).getRetCondition();
        steps.creativeSteps().addDefaultCpmIndoorVideoCreative(campaign.getClientInfo(), videoCreativeId);
        IndoorPlacement placement = steps.placementSteps().addDefaultIndoorPlacementWithOneBlock();
        ComplexCpmAdGroup complexAdGroup =
                cpmIndoorAdGroup(campaign.getCampaignId(), placement, retCondition, videoCreativeId);
        ComplexCpmAdGroupAddOperation operation =
                createOperation(singletonList(complexAdGroup), campaign.getClientInfo());
        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result, isFullySuccessful());
        checkComplexAdGroup(complexAdGroup);
    }

    @Test
    public void cpmIndoorAdGroupWithKeywords_ValidationError() {
        RetargetingCondition retCondition =
                steps.retConditionSteps().createIndoorRetCondition(campaign.getClientInfo()).getRetCondition();
        IndoorPlacement placement = steps.placementSteps().addDefaultIndoorPlacementWithOneBlock();
        ComplexCpmAdGroup complexAdGroup = cpmIndoorAdGroup(campaign.getCampaignId(), placement, retCondition, null)
                .withKeywords(singletonList(keywordForCpmBanner()));

        ComplexCpmAdGroupAddOperation operation =
                createOperation(singletonList(complexAdGroup), campaign.getClientInfo());
        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(0)), keywordsNotAllowed())));
    }

    @Test
    public void cpmIndoorAdGroupWithWrongRetargetings_ValidationError() {
        Long videoCreativeId = steps.creativeSteps().getNextCreativeId();
        RetargetingCondition retCondition = retargetingCondition(clientId, rules);
        steps.creativeSteps().addDefaultCpmIndoorVideoCreative(campaign.getClientInfo(), videoCreativeId);
        IndoorPlacement placement = steps.placementSteps().addDefaultIndoorPlacementWithOneBlock();
        ComplexCpmAdGroup complexAdGroup =
                cpmIndoorAdGroup(campaign.getCampaignId(), placement, retCondition, videoCreativeId);
        ComplexCpmAdGroupAddOperation operation =
                createOperation(singletonList(complexAdGroup), campaign.getClientInfo());
        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result.getValidationResult(), hasDefectDefinitionWith(
                validationError(path(index(0), field("targetInterests"),
                        index(0), field("retargetingConditionId")),
                        retargetingConditionIsInvalidForRetargeting())));
    }

    @Test
    public void cpmIndoorAdGroupWithVideoGoalsWithWrongType_ValidationError() {
        RetargetingCondition retCondition = retargetingCondition(clientId, rules);
        IndoorPlacement placement = steps.placementSteps().addDefaultIndoorPlacementWithOneBlock();
        ComplexCpmAdGroup complexAdGroup =
                cpmIndoorAdGroup(campaign.getCampaignId(), placement, retCondition, null)
                        .withUsersSegments(singletonList(new UsersSegment().withType(AdShowType.THIRD_QUARTILE)));

        ComplexCpmAdGroupAddOperation operation =
                createOperation(singletonList(complexAdGroup), campaign.getClientInfo());
        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result.getValidationResult(), hasDefectDefinitionWith(
                validationError(path(index(0), field("usersSegments"), index(0), field(UsersSegment.TYPE)),
                        goalTypeNotSupportedInAdGroup())));
    }

    @Test
    public void fullCpmYndxFrontpageAdGroup() {
        ComplexCpmAdGroup complexAdGroup =
                cpmYndxFrontpageAdGroup(cpmYndxCampaign.getCampaignId(), html5CreativeIdForFrontpage);
        ComplexCpmAdGroupAddOperation operation =
                createOperation(singletonList(complexAdGroup), cpmYndxCampaign.getClientInfo());
        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result, isFullySuccessful());
    }

    @Test
    public void cpmYndxFrontpageAdGroupWithKeywords_ValidationError() {
        ComplexCpmAdGroup complexAdGroup =
                cpmYndxFrontpageAdGroup(cpmYndxCampaign.getCampaignId(), html5CreativeIdForFrontpage)
                        .withKeywords(singletonList(keywordForCpmBanner()));

        ComplexCpmAdGroupAddOperation operation =
                createOperation(singletonList(complexAdGroup), cpmYndxCampaign.getClientInfo());
        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(0)), keywordsNotAllowed())));
    }

    @Test
    public void cpmYndxFrontpageAdGroupWithMinusKeywords_ValidationError() {
        ComplexCpmAdGroup complexAdGroup =
                cpmYndxFrontpageAdGroup(cpmYndxCampaign.getCampaignId(), html5CreativeIdForFrontpage);

        complexAdGroup.withAdGroup(complexAdGroup.getAdGroup().withMinusKeywords(singletonList("abc")));
        ComplexCpmAdGroupAddOperation operation =
                createOperation(singletonList(complexAdGroup), cpmYndxCampaign.getClientInfo());
        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(
                        path(index(0), field(AdGroup.MINUS_KEYWORDS)), minusKeywordsNotAllowed())));
    }

    @Test
    public void cpmYndxFrontpageAdGroup_AdriverPixel_ValidationError() {
        ComplexCpmAdGroup complexAdGroup =
                cpmYndxFrontpageAdGroup(cpmYndxCampaign.getCampaignId(), html5CreativeIdForFrontpage);

        complexAdGroup.withBanners(singletonList(
                ((CpmBanner) complexAdGroup.getBanners().get(0)).withPixels(singletonList(adriverPixelUrl()))));
        ComplexCpmAdGroupAddOperation operation =
                createOperation(singletonList(complexAdGroup), cpmYndxCampaign.getClientInfo());
        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(
                        path(index(0), field(ComplexCpmAdGroup.BANNERS), index(0), field(CpmBanner.PIXELS),
                                index(0)),
                        noRightsToPixel(adriverPixelUrl(), emptyList(), CampaignType.CPM_YNDX_FRONTPAGE,
                                InventoryType.NOT_DEAL))));
    }

    @Test
    public void cpmYndxFrontpageAdGroup_YaAudiencePixel_NoError() {
        ComplexCpmAdGroup complexAdGroup =
                cpmYndxFrontpageAdGroup(cpmYndxCampaign.getCampaignId(), html5CreativeIdForFrontpage);

        complexAdGroup.withBanners(singletonList(
                ((CpmBanner) complexAdGroup.getBanners().get(0)).withPixels(singletonList(yaAudiencePixelUrl()))));
        ComplexCpmAdGroupAddOperation operation =
                createOperation(singletonList(complexAdGroup), cpmYndxCampaign.getClientInfo());
        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result, isFullySuccessful());
    }

    @Test
    public void cpmYndxFrontpageAdGroup_VideoCreative_Error() {
        CreativeInfo creativeInfo = steps.creativeSteps().createCreative(defaultCpmVideoAddition(null, null),
                cpmYndxCampaign.getClientInfo());
        ComplexCpmAdGroup complexAdGroup =
                cpmYndxFrontpageAdGroup(cpmYndxCampaign.getCampaignId(), creativeInfo.getCreativeId());
        ComplexCpmAdGroupAddOperation operation =
                createOperation(singletonList(complexAdGroup), cpmYndxCampaign.getClientInfo());
        MassResult<Long> result = operation.prepareAndApply();

        assertThat(result.getValidationResult(), hasDefectDefinitionWith(validationError(
                path(index(0), field(ComplexCpmAdGroup.BANNERS), index(0), field(CpmBanner.CREATIVE_ID)),
                requiredCreativesWithHtml5TypeOnly())));
    }

    @Test
    public void fullCpmYndxFrontpageAdGroupForPriceSales() {
        ComplexCpmAdGroup complexAdGroup = validComplexAdGroupForPriceSales(PRIORITY_DEFAULT);
        ComplexCpmAdGroupAddOperation operation =
                createOperation(singletonList(complexAdGroup), clientInfo);

        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result, isFullySuccessful());
    }

    @Test
    public void fullCpmYndxFrontpageAdGroupForPriceSales_WrongGeo_ValidationError() {
        ComplexCpmAdGroup complexAdGroup = validComplexAdGroupForPriceSales(PRIORITY_DEFAULT);
        complexAdGroup.getAdGroup().withGeo(List.of(CENTRAL_DISTRICT));
        ComplexCpmAdGroupAddOperation operation =
                createOperation(singletonList(complexAdGroup), clientInfo);

        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result.getValidationResult(), hasDefectDefinitionWith(
                validationError(path(index(0), field(AdGroup.GEO)), invalidValue())));
    }

    @Test
    @Description("в снэпшоте RUSSIA, KYIV не в России, поэтому ошибка")
    public void fullCpmYndxFrontpageAdGroupForPriceSales_WrongGeoSpecificGroup_ValidationError() {
        ComplexCpmAdGroup complexAdGroup = validComplexAdGroupForPriceSales(PRIORITY_SPECIFIC);
        complexAdGroup.getAdGroup().withGeo(List.of(KYIV_REGION_ID));
        ComplexCpmAdGroupAddOperation operation =
                createOperation(singletonList(complexAdGroup), clientInfo);

        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result.getValidationResult(), hasDefectDefinitionWith(
                validationError(path(index(0), field(AdGroup.GEO)), invalidValue())));
    }

    @Test
    @Description("в снэпшоте RUSSIA, в специфической группе разрешается сужать аудиторию даже до Урюпинска")
    public void fullCpmYndxFrontpageAdGroupForPriceSales_TownInGeoSpecificGroup_Ok() {
        ComplexCpmAdGroup complexAdGroup = validComplexAdGroupForPriceSales(PRIORITY_SPECIFIC);
        complexAdGroup.getAdGroup().withGeo(List.of(URYUPINSK_REGION_ID, CENTRAL_DISTRICT));
        ComplexCpmAdGroupAddOperation operation =
                createOperation(singletonList(complexAdGroup), clientInfo);

        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result, isFullySuccessful());
    }

    @Test
    @Description("geo10 гео группы совпадает с пакетом")
    public void geoType10_geoAsPackage_Successful() {
        ComplexCpmAdGroup complexAdGroup = validDefaultAdGroupForPriceSales();
        complexAdGroup.getAdGroup().withGeo(List.of(
                SOUTH_FEDERAL_DISTRICT_REGION_ID, SAINT_PETERSBURG_REGION_ID, KRASNODAR_KRAI));
        ComplexCpmAdGroupAddOperation operation =
                createOperation(singletonList(complexAdGroup), clientInfo);

        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result, isFullySuccessful());
    }

    @Test
    @Description("geo10 гео группы - часть гео пакета")
    public void geoType10_geoSubPackage_Successful() {
        ComplexCpmAdGroup complexAdGroup = validDefaultAdGroupForPriceSales();
        complexAdGroup.getAdGroup().withGeo(List.of(SAINT_PETERSBURG_REGION_ID));
        ComplexCpmAdGroupAddOperation operation =
                createOperation(singletonList(complexAdGroup), clientInfo);

        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result, isFullySuccessful());
    }

    @Test
    @Description("geo10 ошибка валидации,когда не выбрано обязательное гео")
    public void geo10_WrongFixedGeo_ValidationError() {
        ComplexCpmAdGroup complexAdGroup = validDefaultAdGroupForPriceSales();
        complexAdGroup.getAdGroup().withGeo(List.of(SOUTH_FEDERAL_DISTRICT_REGION_ID));
        ComplexCpmAdGroupAddOperation operation =
                createOperation(singletonList(complexAdGroup), clientInfo);

        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result.getValidationResult(), hasDefectDefinitionWith(
                validationError(path(index(0), field(AdGroup.GEO)), invalidValue())));
    }

    @Test
    @Description("geo10 ошибка валидации выбрано лишнее гео")
    public void geo10_WrongCustomGeo_ValidationError() {
        ComplexCpmAdGroup complexAdGroup = validDefaultAdGroupForPriceSales();
        complexAdGroup.getAdGroup().withGeo(List.of(
                URYUPINSK_REGION_ID, SOUTH_FEDERAL_DISTRICT_REGION_ID, SAINT_PETERSBURG_REGION_ID, KRASNODAR_KRAI));
        ComplexCpmAdGroupAddOperation operation =
                createOperation(singletonList(complexAdGroup), clientInfo);

        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result.getValidationResult(), hasDefectDefinitionWith(
                validationError(path(index(0), field(AdGroup.GEO)), invalidValue())));
    }

    private CpmPriceCampaign createVideoYndxFrontpageCampaign() {
        PricePackage pricePackage = defaultPricePackageWithGeoType10()
                .withIsFrontpage(true)
                .withAvailableAdGroupTypes(Set.of(AdGroupType.CPM_VIDEO))
                .withClients(List.of(allowedPricePackageClient(clientInfo)));
        pricePackage.getTargetingsFixed()
                .withViewTypes(List.of(ViewType.DESKTOP));
        steps.pricePackageSteps().createPricePackage(pricePackage);
        return steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo, pricePackage);
    }

    private MassResult<Long> createVideoYndxFrontpageAdGroup(
            Long priority, CpmPriceCampaign cpmPriceCampaign, List<Long> goalIds, List<Long> geo) {
        ComplexCpmAdGroup adGroup = new ComplexCpmAdGroup()
                .withAdGroup(activeDefaultVideoAdGroupForPriceSales(cpmPriceCampaign)
                        .withGeo(nvl(geo, List.of(SAINT_PETERSBURG_REGION_ID)))
                        .withPriority(priority)
                )
                .withTargetInterests(List.of(new TargetInterest()));
        List<Rule> rules = goalIds == null ? List.of() : List.of(ruleOrSocialDemo(goalIds));
        List<RetargetingConditionBase> retargetingConditions = List.of(defaultCpmRetCondition().withRules(rules));
        adGroup.withRetargetingConditions(retargetingConditions);
        MassResult<Long> result = createOperation(singletonList(adGroup), clientInfo).prepareAndApply();
        return result;
    }

    @Test
    public void cpmVideoYndxFrontpageAdGroupForPriceSales_defaultAndSpecific_Ok() {
        //Смок тест что создаётся пакет, кампания, основная группа и специфичная группа прайсового видео на морде
        CpmPriceCampaign cpmPriceCampaign = createVideoYndxFrontpageCampaign();
        MassResult<Long> result = createVideoYndxFrontpageAdGroup(PRIORITY_DEFAULT, cpmPriceCampaign,
                List.of(AGE_18_24, AGE_25_34, AGE_35_44), null);

        assertThat(result, isFullySuccessful());

        result = createVideoYndxFrontpageAdGroup(PRIORITY_SPECIFIC, cpmPriceCampaign, List.of(AGE_25_34), null);
        assertThat(result, isFullySuccessful());
    }

    @Test
    public void cpmVideoYndxFrontpageAdGroupForPriceSales_default_ValidationError() {
        //Видео на главной. В основной группе таргетинг должен соответствовать настройкам пакета
        CpmPriceCampaign cpmPriceCampaign = createVideoYndxFrontpageCampaign();
        MassResult<Long> result = createVideoYndxFrontpageAdGroup(PRIORITY_DEFAULT, cpmPriceCampaign,
                List.of(FEMALE_CRYPTA_GOAL_ID), List.of(MOSCOW_REGION_ID));
        assertThat(result.getValidationResult(), hasDefectDefinitionWith(
                validationError(path(index(0), field(AdGroup.GEO)),
                        invalidValue())));
    }

    @Test
    public void cpmVideoYndxFrontpageAdGroupForPriceSales_specific_ValidationErrorGeo() {
        // Видео на главной. В специфичной группе таргетинг должен быть подмножеством таргетинга основной.
        // И не обязательно соотвтетствовать настройкам пакета
        CpmPriceCampaign cpmPriceCampaign = createVideoYndxFrontpageCampaign();
        MassResult<Long> result = createVideoYndxFrontpageAdGroup(PRIORITY_DEFAULT, cpmPriceCampaign,
                List.of(AGE_25_34), List.of(SAINT_PETERSBURG_REGION_ID));

        assertThat(result, isFullySuccessful());

        result = createVideoYndxFrontpageAdGroup(PRIORITY_SPECIFIC, cpmPriceCampaign, List.of(AGE_25_34),
                List.of(SAINT_PETERSBURG_REGION_ID, KRASNODAR_KRAI));
        assertThat(result.getValidationResult(), hasDefectDefinitionWith(
                validationError(path(index(0), field(AdGroup.GEO)),
                        invalidValue())));
    }

    @Test
    public void cpmVideoYndxFrontpageAdGroupForPriceSales_specific_ValidationErrorGoals() {
        // Видео на главной. В специфичной группе таргетинг должен быть подмножеством таргетинга основной.
        // добавление основной группы на мужчин. Специфичная на LTV. Должна валидация ругнуться
        CpmPriceCampaign cpmPriceCampaign = createVideoYndxFrontpageCampaign();
        metrikaHelperStub.addGoalsFromRules(cpmPriceCampaign.getUid(), List.of(ruleOrSocialDemo(MALE_CRYPTA_GOAL_ID)));
        MassResult<Long> resultDefault = createVideoYndxFrontpageAdGroup(PRIORITY_DEFAULT, cpmPriceCampaign,
                List.of(MALE_CRYPTA_GOAL_ID), List.of(SAINT_PETERSBURG_REGION_ID));
        assertThat(resultDefault, isFullySuccessful());

        var retargetingCondition = defaultCpmRetCondition()
                .withRules(List.of(ltvRule()));
        metrikaHelperStub.addGoalsFromRules(cpmPriceCampaign.getUid(), retargetingCondition.getRules());
        ComplexCpmAdGroup adGroup = new ComplexCpmAdGroup()
                .withAdGroup(activeDefaultVideoAdGroupForPriceSales(cpmPriceCampaign)
                        .withGeo(List.of(SAINT_PETERSBURG_REGION_ID))
                        .withPriority(PRIORITY_SPECIFIC))
                .withTargetInterests(List.of(new TargetInterest()))
                .withRetargetingConditions(List.of(retargetingCondition));
        MassResult<Long> result = createOperation(singletonList(adGroup), clientInfo).prepareAndApply();
        assertThat(result.getValidationResult(), hasDefectDefinitionWith(
                validationError(path(index(0), field(TARGET_INTERESTS), index(0), field(RETARGETING_CONDITION_ID)),
                        retargetingConditionIsInvalidByDefaultAdGroup())));
    }

    @Test
    public void cpmVideoYndxFrontpageAdGroupForPriceSales_specificSocdem_Ok() {
        //на пакете можно только LTV. Основная без аудитории создалась. Специфичная на мужчин
        PricePackage pricePackage = defaultPricePackageWithGeoType10()
                .withIsFrontpage(true)
                .withAvailableAdGroupTypes(Set.of(AdGroupType.CPM_VIDEO))
                .withClients(List.of(allowedPricePackageClient(clientInfo)));
        pricePackage.getTargetingsFixed().withViewTypes(List.of(ViewType.DESKTOP));
        pricePackage.getTargetingsCustom().getRetargetingCondition().setCryptaSegments(List.of(ltvGoal().getId()));
        steps.pricePackageSteps().createPricePackage(pricePackage);
        CpmPriceCampaign cpmPriceCampaign = steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo, pricePackage);
        MassResult<Long> result = createVideoYndxFrontpageAdGroup(PRIORITY_DEFAULT, cpmPriceCampaign,
                null, null);
        assertThat(result, isFullySuccessful());

        result = createVideoYndxFrontpageAdGroup(PRIORITY_SPECIFIC, cpmPriceCampaign, List.of(MALE_CRYPTA_GOAL_ID), null);
        assertThat(result, isFullySuccessful());
    }

    @Test
    public void cpmYndxFrontpageAdGroupForPriceSales_socialDemo_Ok() {
        //На добавление дефолтной группы можно задавать таргетинги. На обновление уже нельзя
        List<Goal> socialGoals = List.of(defaultGoalByTypeAndId(AGE_18_24, GoalType.SOCIAL_DEMO));
        List<Goal> behaviorGoals = List.of(behaviorGoalForPriceSales);
        List<Rule> rules = List.of(
                new Rule()
                        .withType(RuleType.OR)
                        .withGoals(socialGoals),
                new Rule()
                        .withType(RuleType.OR)
                        .withGoals(behaviorGoals));
        metrikaHelperStub.addGoalsFromRules(cpmPriceCampaign.getUid(), rules);
        RetargetingCondition retargetingCondition = retargetingCondition(clientId, rules);

        ComplexCpmAdGroup complexAdGroup = validComplexAdGroupForPriceSales(PRIORITY_DEFAULT)
                .withRetargetingConditions(List.of(retargetingCondition));
        ComplexCpmAdGroupAddOperation operation =
                createOperation(singletonList(complexAdGroup), clientInfo);

        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result, isFullySuccessful());
    }

    @Test
    public void cpmYndxFrontpageAdGroupForPriceSales_defaultAndSpecific_Ok() {
        //добавить в кампанию дефолтную группу и специфичную.
        //дефолтная (18-24 или 25-34 или 35-44) И (средний доход)
        //в специфической 25-34 и средний доход и интересы животные
        var demo1 = ruleOrSocialDemo(List.of(AGE_18_24, AGE_25_34, AGE_35_44));
        var demo2 = ruleOrSocialDemo(MID_INCOME_GOAL_ID);
        var demo3 = ruleOrSocialDemo(AGE_25_34);
        var demo4 = defaultGoalByTypeAndId(2499001255L, GoalType.INTERESTS);
        var goals = StreamEx.of(demo1.getGoals())
                .append(demo2.getGoals())
                .append(demo3.getGoals())
                .append(demo4)
                .map(g -> {
                    g.setCryptaScope(Set.of(CryptaGoalScope.COMMON, CryptaGoalScope.INTERNAL_AD));
                    return g;
                })
                .toSet();
        cryptaSegmentRepository.add(goals);
        var retargetingConditionDefault = defaultCpmRetCondition();
        retargetingConditionDefault
                .withRules(List.of(demo1, demo2, ltvRule()));
        metrikaHelperStub.addGoalsFromRules(cpmPriceCampaign.getUid(), retargetingConditionDefault.getRules());
        ComplexCpmAdGroup defaultAdGroup = validComplexAdGroupForPriceSales(PRIORITY_DEFAULT)
                .withRetargetingConditions(List.of(retargetingConditionDefault));
        assertThat(createOperation(singletonList(defaultAdGroup), clientInfo).prepareAndApply(), isFullySuccessful());

        var specificConditionDefault = defaultCpmRetCondition();
        specificConditionDefault
                .withRules(List.of(demo3, demo2,
                        new Rule().withType(RuleType.OR).withGoals(List.of(demo4))
                                .withInterestType(CryptaInterestType.all),
                        ltvRule()));
        metrikaHelperStub.addGoalsFromRules(cpmPriceCampaign.getUid(), specificConditionDefault.getRules());
        ComplexCpmAdGroup specificAdGroup = validComplexAdGroupForPriceSales(PRIORITY_SPECIFIC)
                .withRetargetingConditions(List.of(specificConditionDefault));
        MassResult<Long> result = createOperation(singletonList(specificAdGroup), clientInfo).prepareAndApply();
        assertThat(result, isFullySuccessful());
    }

    @Test
    public void cpmYndxFrontpageAdGroupForPriceSales_defaultAndSpecific_ValidationError() {
        //добавить в кампанию дефолтную группу и специфичную.
        //дефолтная (18-24 или 25-34 или 35-44) И (средний доход)
        //в специфической 25-34 и (средний или высокий доход)
        var demo1 = ruleOrSocialDemo(List.of(AGE_18_24, AGE_25_34, AGE_35_44));
        var demo2 = ruleOrSocialDemo(MID_INCOME_GOAL_ID);
        var demo3 = ruleOrSocialDemo(AGE_25_34);
        var demo4 = ruleOrSocialDemo(List.of(MID_INCOME_GOAL_ID, C1_INCOME_GOAL_ID));
        var goals = StreamEx.of(demo1.getGoals())
                .append(demo2.getGoals())
                .append(demo3.getGoals())
                .append(demo4.getGoals())
                .map(g -> {
                    g.setCryptaScope(Set.of(CryptaGoalScope.COMMON, CryptaGoalScope.INTERNAL_AD));
                    return g;
                })
                .toSet();
        cryptaSegmentRepository.add(goals);
        var retargetingConditionDefault = defaultCpmRetCondition();
        retargetingConditionDefault
                .withRules(List.of(demo1, demo2, ltvRule()));
        metrikaHelperStub.addGoalsFromRules(cpmPriceCampaign.getUid(), retargetingConditionDefault.getRules());
        ComplexCpmAdGroup defaultAdGroup = validComplexAdGroupForPriceSales(PRIORITY_DEFAULT)
                .withRetargetingConditions(List.of(retargetingConditionDefault));
        assertThat(createOperation(singletonList(defaultAdGroup), clientInfo).prepareAndApply(), isFullySuccessful());

        var specificConditionDefault = defaultCpmRetCondition();
        specificConditionDefault
                .withRules(List.of(demo3, demo4, ltvRule()));
        metrikaHelperStub.addGoalsFromRules(cpmPriceCampaign.getUid(), specificConditionDefault.getRules());
        ComplexCpmAdGroup specificAdGroup = validComplexAdGroupForPriceSales(PRIORITY_SPECIFIC)
                .withRetargetingConditions(List.of(specificConditionDefault));
        MassResult<Long> result = createOperation(singletonList(specificAdGroup), clientInfo).prepareAndApply();
        assertThat(result.getValidationResult(), hasDefectDefinitionWith(
                validationError(path(index(0), field(TARGET_INTERESTS), index(0), field(RETARGETING_CONDITION_ID)),
                        retargetingConditionIsInvalidByDefaultAdGroup())));
    }

    @Test
    public void cpmYndxFrontpageAdGroupForPriceSales_defaultAndSpecific_munusRule_ValidationError() {
        //добавить в кампанию дефолтную группу и специфичную.
        //дефолтная (18-24 или 25-34 или 35-44) И (средний доход)
        //в специфической 25-34
        var demo1 = ruleOrSocialDemo(List.of(AGE_18_24, AGE_25_34, AGE_35_44));
        var demo2 = ruleOrSocialDemo(MID_INCOME_GOAL_ID);
        var demo3 = ruleOrSocialDemo(AGE_25_34);
        var goals = StreamEx.of(demo1.getGoals())
                .append(demo2.getGoals())
                .append(demo3.getGoals())
                .map(g -> {
                    g.setCryptaScope(Set.of(CryptaGoalScope.COMMON, CryptaGoalScope.INTERNAL_AD));
                    return g;
                })
                .toSet();
        cryptaSegmentRepository.add(goals);
        var retargetingConditionDefault = defaultCpmRetCondition();
        retargetingConditionDefault
                .withRules(List.of(demo1, demo2, ltvRule()));
        metrikaHelperStub.addGoalsFromRules(cpmPriceCampaign.getUid(), retargetingConditionDefault.getRules());
        ComplexCpmAdGroup defaultAdGroup = validComplexAdGroupForPriceSales(PRIORITY_DEFAULT)
                .withRetargetingConditions(List.of(retargetingConditionDefault));
        assertThat(createOperation(singletonList(defaultAdGroup), clientInfo).prepareAndApply(), isFullySuccessful());

        var specificConditionDefault = defaultCpmRetCondition();
        specificConditionDefault
                .withRules(List.of(demo3,
                        ltvRule()));
        metrikaHelperStub.addGoalsFromRules(cpmPriceCampaign.getUid(), specificConditionDefault.getRules());
        ComplexCpmAdGroup specificAdGroup = validComplexAdGroupForPriceSales(PRIORITY_SPECIFIC)
                .withRetargetingConditions(List.of(specificConditionDefault));
        MassResult<Long> result = createOperation(singletonList(specificAdGroup), clientInfo).prepareAndApply();
        assertThat(result.getValidationResult(), hasDefectDefinitionWith(
                validationError(path(index(0), field(TARGET_INTERESTS), index(0), field(RETARGETING_CONDITION_ID)),
                        retargetingConditionIsInvalidByDefaultAdGroup())));
    }

    @Test
    public void cpmYndxFrontpageAdGroupForPriceSales_IncorrectGoalType_ValidationError() {
        List<Goal> validGoals = List.of(behaviorGoalForPriceSales);
        List<Goal> invalidGoals = List.of(defaultGoalByType(GoalType.FAMILY));
        List<Rule> rules = List.of(
                new Rule()
                        .withType(RuleType.OR)
                        .withGoals(validGoals),
                new Rule()
                        .withType(RuleType.OR)
                        .withGoals(invalidGoals));
        metrikaHelperStub.addGoalsFromRules(cpmPriceCampaign.getUid(), rules);
        RetargetingCondition retargetingCondition = retargetingCondition(clientId, rules);

        ComplexCpmAdGroup complexAdGroup = validComplexAdGroupForPriceSales(PRIORITY_DEFAULT)
                .withRetargetingConditions(List.of(retargetingCondition));
        ComplexCpmAdGroupAddOperation operation =
                createOperation(singletonList(complexAdGroup), clientInfo);

        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result.getValidationResult(), hasDefectDefinitionWith(
                validationError(path(index(0), field(TARGET_INTERESTS), index(0), field(RETARGETING_CONDITION_ID)),
                        retargetingConditionIsInvalidForPricePackage())));
    }

    @Test
    public void cpmYndxFrontpageAdGroupForPriceSales_BidModifier_ValidationError() {
        ComplexCpmAdGroup complexAdGroup = validComplexAdGroupForPriceSales(PRIORITY_SPECIFIC)
                .withComplexBidModifier(randomComplexBidModifierMobile());
        ComplexCpmAdGroupAddOperation operation =
                createOperation(singletonList(complexAdGroup), clientInfo);

        MassResult<Long> result = operation.prepareAndApply();

        assertThat(result.getValidationResult(), hasDefectDefinitionWith(validationError(path(index(0)),
                cpmPriceAdGroupUseNotAllowedBidModifiers())));
    }

    @Test
    public void cpmYndxFrontpageAdGroupForPriceSales_AdditionalHrefsWithoutFeature_ValidationError() {
        setCpmPriceAdditionalHrefsFeature(false);
        ComplexCpmAdGroup complexAdGroup = validComplexAdGroupForPriceSales(PRIORITY_DEFAULT);
        ComplexCpmAdGroupAddOperation operation =
                createOperation(singletonList(complexAdGroup), clientInfo);

        MassResult<Long> result = operation.prepareAndApply();

        assertThat(result.getValidationResult(), hasDefectDefinitionWith(validationError(
                path(index(0), field(ComplexCpmAdGroup.BANNERS), index(0), field(CpmBanner.ADDITIONAL_HREFS)),
                isEmptyCollection())));
    }

    private void checkComplexAdGroup(ComplexCpmAdGroup complexAdGroup) {
        AdGroup expectedAdGroup = complexAdGroup.getAdGroup();
        List<AdGroup> adGroups =
                adGroupRepository.getAdGroups(campaign.getShard(), singletonList(expectedAdGroup.getId()));
        CompareStrategy compareStrategy = isEmpty(complexAdGroup.getKeywords()) ?
                onlyExpectedFields() : AD_GROUP_COMPARE_STRATEGY_WITH_STATUSES;
        assertThat("группа успешно добавлена", adGroups,
                contains(beanDiffer(expectedAdGroup).useCompareStrategy(compareStrategy)));

        var expectedBanners = complexAdGroup.getBanners();
        var banners =
                bannerTypedRepository.getBannersByGroupIds(campaign.getShard(), singletonList(expectedAdGroup.getId()));
        if (expectedBanners == null) {
            assertThat("в группе не должно быть баннеров", banners, empty());
        } else {
            expectedBanners.forEach(b -> {
                if (b instanceof BannerWithMeasurers) {
                    var banner = (BannerWithMeasurers) b;
                    if (banner.getMeasurers() == null) {
                        banner.setMeasurers(emptyList());
                    }
                }
                if (b instanceof BannerWithAdditionalHrefs) {
                    var banner = (BannerWithAdditionalHrefs) b;
                    if (banner.getAdditionalHrefs() == null) {
                        banner.setAdditionalHrefs(emptyList());
                    }
                }
            });
            CompareStrategy bannerCompareStrategy = allFields()
                    .forFields(newPath("lastChange")).useMatcher(notNullValue())
                    .forFields(newPath("creativeRelationId")).useMatcher(notNullValue())
                    .forFields(newPath("measurers/0/bannerId")).useMatcher(notNullValue())
                    .forFields(newPath("measurers/0/params")).useMatcher(notNullValue())
                    .forFields(newPath("showTitleAndBody")).useMatcher(is(false));
            assertThat("добавлены правильные баннеры", banners,
                    contains(mapList(expectedBanners,
                            banner -> BeanDifferMatcher.<Banner>beanDiffer(banner).useCompareStrategy(bannerCompareStrategy))));
        }

        commonChecks
                .checkKeywords(complexAdGroup.getKeywords(), expectedAdGroup.getId(), campaign.getShard());

        commonChecks.checkRetargetings(complexAdGroup.getTargetInterests(), expectedAdGroup.getId(),
                campaign.getClientId(), campaign.getShard());

        commonChecks.checkBidModifiers(complexAdGroup.getComplexBidModifier(), expectedAdGroup.getId(),
                campaign.getCampaignId(), campaign.getShard());

    }

    private static RetargetingCondition retargetingCondition(ClientId clientId, List<Rule> rules) {
        return (RetargetingCondition) defaultRetCondition(clientId)
                .withType(ConditionType.interests)
                .withRules(rules);
    }

    private PricePackage defaultPricePackageWithGeoType10() {
        var pricePackage = approvedPricePackage()
                .withClients(List.of(allowedPricePackageClient(clientInfo)));
        pricePackage.getTargetingsFixed()
                .withGeo(List.of(SAINT_PETERSBURG_REGION_ID))
                .withGeoType(REGION_TYPE_REGION)
                .withGeoExpanded(null);
        pricePackage.getTargetingsCustom()
                .withGeo(List.of(SOUTH_FEDERAL_DISTRICT_REGION_ID, SAINT_PETERSBURG_REGION_ID, KRASNODAR_KRAI))
                .withGeoType(REGION_TYPE_REGION)
                .withGeoExpanded(null);
        pricePackage.getTargetingsCustom().getRetargetingCondition().setCryptaSegments(
                List.of(AGE_18_24, AGE_25_34, AGE_35_44, MALE_CRYPTA_GOAL_ID, MID_INCOME_GOAL_ID, ltvGoal().getId()));
        return pricePackage;
    }

    private ComplexCpmAdGroup validDefaultAdGroupForPriceSales() {
        List<Goal> goals = List.of(behaviorGoalForPriceSales);
        List<Rule> rules = List.of(new Rule()
                .withType(RuleType.OR)
                .withGoals(goals));
        metrikaHelperStub.addGoalsFromRules(clientInfo.getUid(), rules);
        RetargetingCondition retargetingCondition = retargetingCondition(clientId, rules);

        PricePackage pricePackageGeoType10 = defaultPricePackageWithGeoType10();
        var pricePackageInfo = steps.pricePackageSteps().createPricePackage(pricePackageGeoType10);
        var cpmPriceCampaign = steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo,
                pricePackageInfo.getPricePackage());

        return cpmYndxFrontpageAdGroupForPriceSales(cpmPriceCampaign.getId(), html5CreativeIdForPriceSales,
                PRIORITY_DEFAULT)
                .withTargetInterests(List.of(new TargetInterest()))
                .withRetargetingConditions(List.of(retargetingCondition));
    }

    private ComplexCpmAdGroup validComplexAdGroupForPriceSales(Long priority) {
        List<Goal> goals = List.of(behaviorGoalForPriceSales);
        List<Rule> rules = List.of(new Rule()
                .withType(RuleType.OR)
                .withGoals(goals));
        metrikaHelperStub.addGoalsFromRules(clientInfo.getUid(), rules);
        RetargetingCondition retargetingCondition = retargetingCondition(clientId, rules);

        return cpmYndxFrontpageAdGroupForPriceSales(cpmPriceCampaign.getId(), html5CreativeIdForPriceSales, priority)
                .withTargetInterests(List.of(new TargetInterest()))
                .withRetargetingConditions(List.of(retargetingCondition));
    }

    private ComplexCpmAdGroupAddOperation createOperation(List<ComplexCpmAdGroup> complexAdGroups,
                                                          ClientInfo clientInfo) {
        return addOperationFactory.createCpmAdGroupAddOperation(true, complexAdGroups,
                geoTree, false, null, clientInfo.getUid(), clientInfo.getClientId(), clientInfo.getUid(),
                true);
    }

    private void setCpmPriceAdditionalHrefsFeature(boolean enabled) {
        steps.featureSteps()
                .addClientFeature(clientInfo.getClientId(), CPM_PRICE_BANNER_ADDITIONAL_HREFS_ALLOWED, enabled);
    }

}
