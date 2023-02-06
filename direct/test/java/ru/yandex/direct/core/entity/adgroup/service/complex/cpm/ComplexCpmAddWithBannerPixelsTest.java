package ru.yandex.direct.core.entity.adgroup.service.complex.cpm;

import java.util.Arrays;
import java.util.List;

import com.google.common.collect.ImmutableList;
import one.util.streamex.StreamEx;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.container.ComplexCpmAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CriterionType;
import ru.yandex.direct.core.entity.adgroup.model.StatusShowsForecast;
import ru.yandex.direct.core.entity.adgroup.service.complex.ComplexAdGroupAddOperationFactory;
import ru.yandex.direct.core.entity.banner.model.BannerMeasurer;
import ru.yandex.direct.core.entity.banner.model.BannerMeasurerSystem;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldCpmBanner;
import ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects;
import ru.yandex.direct.core.entity.banner.type.pixels.InventoryType;
import ru.yandex.direct.core.entity.banner.type.pixels.PixelPermissionInfo;
import ru.yandex.direct.core.entity.banner.type.pixels.PixelProvider;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.placements.repository.PlacementsRepository;
import ru.yandex.direct.core.entity.retargeting.model.ConditionType;
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
import ru.yandex.direct.core.testing.info.DealInfo;
import ru.yandex.direct.core.testing.repository.TestCryptaSegmentRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.regions.GeoTree;
import ru.yandex.direct.regions.GeoTreeFactory;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.validation.result.Path;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.entity.adgroup.service.complex.cpm.ComplexCpmAdGroupTestData.cpmBannerAdGroupWithRetargetings;
import static ru.yandex.direct.core.entity.retargeting.service.validation2.CpmAdGroupCreator.createDealWithNonYandexPlacements;
import static ru.yandex.direct.core.entity.retargeting.service.validation2.CpmAdGroupCreator.createDealWithYandexPlacements;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.BIG_PLACEMENT_PAGE_ID;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.PRIVATE_GOAL_ID;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.PUBLIC_GOAL_ID;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.dcmPixelUrl;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.tnsPixelUrl;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.yaAudiencePixelUrl;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultGoalWithId;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmBannerAdGroup;
import static ru.yandex.direct.core.testing.data.TestKeywords.keywordForCpmBanner;
import static ru.yandex.direct.core.testing.data.TestNewCpmBanners.fullCpmBanner;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.defaultRetCondition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoErrors;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

/**
 * Тест проверяет валидацию пикселей на cpm-баннерах в комплексной операции добавления cpm-групп
 */
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ComplexCpmAddWithBannerPixelsTest {
    private static final CompareStrategy AD_GROUP_COMPARE_STRATEGY_WITH_STATUSES = onlyExpectedFields()
            .forFields(newPath("statusBsSynced")).useMatcher(is(StatusBsSynced.NO))
            .forFields(newPath("statusShowsForecast")).useMatcher(is(StatusShowsForecast.NEW));

    @Autowired
    private ComplexAdGroupAddOperationFactory addOperationFactory;

    @Autowired
    private GeoTreeFactory geoTreeFactory;

    @Autowired
    private PlacementsRepository placementsRepository;

    @Autowired
    private Steps steps;

    @Autowired
    private TestCryptaSegmentRepository testCryptaSegmentRepository;

    private GeoTree geoTree;

    private CampaignInfo campaign;
    private ClientInfo clientInfo;
    private Long canvasCreativeId;
    private List<DealInfo> dealInfosNonYandex;
    private List<DealInfo> dealInfosYandex;

    @Before
    public void before() {
        List<Goal> goalsToAdd = Arrays.asList(
                defaultGoalWithId(PRIVATE_GOAL_ID, GoalType.SOCIAL_DEMO),
                defaultGoalWithId(PUBLIC_GOAL_ID, GoalType.SOCIAL_DEMO)
        );
        testCryptaSegmentRepository.addAll(goalsToAdd);
        geoTree = geoTreeFactory.getGlobalGeoTree();

        ClientInfo agencyClientInfo = steps.clientSteps().createDefaultClientWithRole(RbacRole.AGENCY);
        clientInfo = steps.clientSteps().createDefaultClientUnderAgency(agencyClientInfo);

        campaign = steps.campaignSteps().createActiveCpmBannerCampaign(clientInfo);

        CreativeInfo creativeInfo = steps.creativeSteps().addDefaultCanvasCreative(clientInfo);
        canvasCreativeId = creativeInfo.getCreativeId();
        dealInfosNonYandex = createDealWithNonYandexPlacements(steps, placementsRepository, agencyClientInfo);
        dealInfosYandex = createDealWithYandexPlacements(steps, placementsRepository, agencyClientInfo);
    }

    @After
    public void after() {
        steps.dealSteps()
                .unlinkDeals(clientInfo.getShard(), mapList(dealInfosNonYandex, DealInfo::getDealId));
        steps.dealSteps().deleteDeals(mapList(dealInfosNonYandex, DealInfo::getDeal), clientInfo);

        steps.dealSteps()
                .unlinkDeals(clientInfo.getShard(), mapList(dealInfosYandex, DealInfo::getDealId));
        steps.dealSteps().deleteDeals(mapList(dealInfosYandex, DealInfo::getDeal), clientInfo);

        placementsRepository.deletePlacementsBy(ImmutableList.of(BIG_PLACEMENT_PAGE_ID, BIG_PLACEMENT_PAGE_ID + 1));
    }

    @Test
    public void oneCpmAdGroupCpmBannerCampaign_WithPrivateRetargetings_Error() {
        RetargetingCondition retCondition = steps.retConditionSteps()
                .createRetCondition(
                        (RetargetingCondition) defaultRetCondition(null)
                                .withType(ConditionType.interests)
                                .withRules(
                                        singletonList(createRuleFromSocialDemoGoalIds(singletonList(PRIVATE_GOAL_ID)))),
                        clientInfo
                ).getRetCondition();
        ComplexCpmAdGroup complexAdGroup =
                cpmBannerAdGroupWithRetargetings(campaign.getCampaignId(), retCondition, canvasCreativeId)
                        .withBanners(singletonList(
                                fullCpmBanner(campaign.getCampaignId(), null, canvasCreativeId)
                                        .withPixels(singletonList(dcmPixelUrl()))));
        ComplexCpmAdGroupAddOperation operation = createOperation(singletonList(complexAdGroup));
        MassResult<Long> result = operation.prepareAndApply();
        Path errPath = path(index(0), field(ComplexCpmAdGroup.BANNERS), index(0), field(OldCpmBanner.PIXELS), index(0));
        assertThat(result.getValidationResult(), hasDefectDefinitionWith(
                validationError(errPath, BannerDefects.noRightsToPixel(dcmPixelUrl(),
                        Arrays.asList(new PixelPermissionInfo().withProvider(PixelProvider.YANDEXAUDIENCE)),
                        CampaignType.CPM_BANNER,
                        InventoryType.NOT_DEAL))));
    }

    @Test
    public void oneCpmAdGroupNonYandexInventoryCampaign_WithPrivateRetargetings_Error() {
        RetargetingCondition retCondition = steps.retConditionSteps()
                .createRetCondition(
                        (RetargetingCondition) defaultRetCondition(null)
                                .withType(ConditionType.interests)
                                .withRules(
                                        singletonList(createRuleFromSocialDemoGoalIds(singletonList(PRIVATE_GOAL_ID)))),
                        clientInfo
                ).getRetCondition();
        ComplexCpmAdGroup complexAdGroup = createComplexCpmAdGroupWithSomeInventory(dealInfosNonYandex, clientInfo,
                singletonList(dcmPixelUrl()), singletonList(retCondition), emptyList());

        ComplexCpmAdGroupAddOperation operation = createOperation(singletonList(complexAdGroup));
        MassResult<Long> result = operation.prepareAndApply();
        Path errPath = path(index(0), field(ComplexCpmAdGroup.BANNERS), index(0), field(OldCpmBanner.PIXELS), index(0));
        assertThat(result.getValidationResult(), hasDefectDefinitionWith(
                validationError(errPath, BannerDefects.noRightsToPixel(dcmPixelUrl(), emptyList(),
                        CampaignType.CPM_DEALS,
                        InventoryType.PRIVATE_CONDITIONS_FOREIGN_INVENTORY))));
    }

    @Test
    public void oneCpmAdGroupNonYandexInventoryCampaign_WithPublicRetargetings_NoError() {
        RetargetingCondition retCondition = steps.retConditionSteps()
                .createRetCondition(
                        (RetargetingCondition) defaultRetCondition(null)
                                .withType(ConditionType.interests)
                                .withRules(
                                        singletonList(createRuleFromSocialDemoGoalIds(singletonList(PUBLIC_GOAL_ID)))),
                        clientInfo
                ).getRetCondition();
        ComplexCpmAdGroup complexAdGroup = createComplexCpmAdGroupWithSomeInventory(dealInfosNonYandex, clientInfo,
                singletonList(dcmPixelUrl()), singletonList(retCondition), emptyList());

        ComplexCpmAdGroupAddOperation operation = createOperation(singletonList(complexAdGroup));
        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result.getValidationResult(), hasNoErrors());
    }

    @Test
    public void oneCpmAdGroupNonYandexInventoryCampaign_WithMeasurer_Error() {
        RetargetingCondition retCondition = steps.retConditionSteps()
                .createRetCondition(
                        (RetargetingCondition) defaultRetCondition(null)
                                .withType(ConditionType.interests)
                                .withRules(
                                        singletonList(createRuleFromSocialDemoGoalIds(singletonList(PUBLIC_GOAL_ID)))),
                        clientInfo
                ).getRetCondition();
        List<BannerMeasurer> measurers =
                List.of(new BannerMeasurer().withBannerMeasurerSystem(BannerMeasurerSystem.MEDIASCOPE));
        ComplexCpmAdGroup complexAdGroup = createComplexCpmAdGroupWithSomeInventory(dealInfosNonYandex, clientInfo,
                singletonList(tnsPixelUrl()), singletonList(retCondition), measurers);

        ComplexCpmAdGroupAddOperation operation = createOperation(singletonList(complexAdGroup));
        MassResult<Long> result = operation.prepareAndApply();

        Path errPath = path(index(0), field(ComplexCpmAdGroup.BANNERS), index(0), field(CpmBanner.PIXELS), index(0));
        assertThat(result.getValidationResult(), hasDefectDefinitionWith(
                validationError(errPath,
                        BannerDefects.invalidPixelWithMeasurer(tnsPixelUrl(),
                                BannerMeasurerSystem.MEDIASCOPE.name()))));
    }

    @Test
    public void oneCpmAdGroupNonYandexInventoryCampaign_WithKeywords_Error() {
        ComplexCpmAdGroup complexAdGroup = createComplexCpmAdGroupWithSomeInventory(dealInfosNonYandex, clientInfo,
                singletonList(dcmPixelUrl()), null, emptyList());

        ComplexCpmAdGroupAddOperation operation = createOperation(singletonList(complexAdGroup));
        MassResult<Long> result = operation.prepareAndApply();
        Path errPath = path(index(0), field(ComplexCpmAdGroup.BANNERS), index(0), field(CpmBanner.PIXELS), index(0));
        assertThat(result.getValidationResult(), hasDefectDefinitionWith(
                validationError(errPath, BannerDefects.noRightsToPixel(dcmPixelUrl(), emptyList(),
                        CampaignType.CPM_DEALS,
                        InventoryType.PRIVATE_CONDITIONS_FOREIGN_INVENTORY))));
    }

    @Test
    public void oneCpmAdGroupYandexInventoryCampaign_WithRetargeting_NoError() {
        RetargetingCondition retCondition = steps.retConditionSteps()
                .createRetCondition(
                        (RetargetingCondition) defaultRetCondition(null)
                                .withType(ConditionType.interests)
                                .withRules(
                                        singletonList(createRuleFromSocialDemoGoalIds(singletonList(PUBLIC_GOAL_ID)))),
                        clientInfo
                ).getRetCondition();
        ComplexCpmAdGroup complexAdGroup = createComplexCpmAdGroupWithSomeInventory(dealInfosYandex, clientInfo,
                singletonList(yaAudiencePixelUrl()), singletonList(retCondition), emptyList());

        ComplexCpmAdGroupAddOperation operation = createOperation(singletonList(complexAdGroup));
        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result.getValidationResult(), hasNoErrors());
    }

    @Test
    public void oneCpmAdGroupYandexInventoryCampaign_WithKeyword_NoError() {
        ComplexCpmAdGroup complexAdGroup = createComplexCpmAdGroupWithSomeInventory(dealInfosYandex, clientInfo,
                singletonList(yaAudiencePixelUrl()), null, emptyList());

        ComplexCpmAdGroupAddOperation operation = createOperation(singletonList(complexAdGroup));
        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result.getValidationResult(), hasNoErrors());
    }

    private ComplexCpmAdGroupAddOperation createOperation(List<ComplexCpmAdGroup> complexAdGroups) {
        return addOperationFactory.createCpmAdGroupAddOperation(true, complexAdGroups,
                geoTree, false, null, clientInfo.getClient().getAgencyUserId(), clientInfo.getClientId(),
                clientInfo.getUid(),
                true);
    }

    private Rule createRuleFromSocialDemoGoalIds(List<Long> socialDemoGoalIds) {
        List<Goal> goals = createGoalsFromGoalIds(socialDemoGoalIds);
        Rule rule = new Rule();
        rule.withGoals(goals).withType(RuleType.OR);
        return rule;
    }

    private List<Goal> createGoalsFromGoalIds(List<Long> socialDemoGoalIds) {
        return StreamEx.of(socialDemoGoalIds).map(id -> {
            Goal someGoal = new Goal();
            someGoal.withId(id).withType(GoalType.SOCIAL_DEMO);
            return someGoal;
        }).toList();
    }

    private ComplexCpmAdGroup createComplexCpmAdGroupWithSomeInventory(List<DealInfo> dealInfos,
                                                                       ClientInfo clientInfo,
                                                                       List<String> bannerPixelUrls,
                                                                       List<RetargetingConditionBase> retConditions,
                                                                       List<BannerMeasurer> measurers) {
        CampaignInfo cpmDealCampaignInfo =
                steps.campaignSteps().createActiveCpmDealsCampaign(clientInfo);
        mapList(dealInfos, DealInfo::getDealId).forEach(
                dealId -> steps.dealSteps().linkDealWithCampaign(dealId, cpmDealCampaignInfo.getCampaignId()));

        var cpmBanner = fullCpmBanner(cpmDealCampaignInfo.getCampaignId(), null, canvasCreativeId)
                .withPixels(bannerPixelUrls)
                .withMeasurers(measurers);

        AdGroup adGroup = activeCpmBannerAdGroup(cpmDealCampaignInfo.getCampaignId())
                .withCriterionType(retConditions != null ? CriterionType.USER_PROFILE : CriterionType.KEYWORD);
        ComplexCpmAdGroup complexCpmAdGroup = new ComplexCpmAdGroup().withAdGroup(adGroup)
                .withBanners(singletonList(cpmBanner));
        if (retConditions != null) {
            retConditions.forEach(retCondition -> retCondition.setId(null));
            List<TargetInterest> targetInterests = mapList(retConditions, t -> new TargetInterest());
            complexCpmAdGroup = complexCpmAdGroup
                    .withTargetInterests(targetInterests)
                    .withRetargetingConditions(retConditions);
        } else {
            complexCpmAdGroup = complexCpmAdGroup
                    .withKeywords(singletonList(keywordForCpmBanner()));
        }
        return complexCpmAdGroup;
    }
}
