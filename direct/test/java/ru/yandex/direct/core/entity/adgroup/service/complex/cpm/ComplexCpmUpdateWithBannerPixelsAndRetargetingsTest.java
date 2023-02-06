package ru.yandex.direct.core.entity.adgroup.service.complex.cpm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.common.collect.ImmutableList;
import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.container.ComplexCpmAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CriterionType;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.adgroup.service.complex.ComplexAdGroupAddOperationFactory;
import ru.yandex.direct.core.entity.adgroup.service.complex.ComplexAdGroupUpdateOperationFactory;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldCpmBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects;
import ru.yandex.direct.core.entity.banner.type.pixels.InventoryType;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.placements.repository.PlacementsRepository;
import ru.yandex.direct.core.entity.retargeting.model.ConditionType;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.GoalType;
import ru.yandex.direct.core.entity.retargeting.model.Retargeting;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingConditionBase;
import ru.yandex.direct.core.entity.retargeting.model.Rule;
import ru.yandex.direct.core.entity.retargeting.model.RuleType;
import ru.yandex.direct.core.entity.retargeting.model.TargetInterest;
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingRepository;
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
import ru.yandex.direct.result.Result;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.retargeting.service.RetargetingUtils.convertRetargetingsToTargetInterests;
import static ru.yandex.direct.core.entity.retargeting.service.validation2.CpmAdGroupCreator.createDealWithNonYandexPlacements;
import static ru.yandex.direct.core.entity.retargeting.service.validation2.CpmAdGroupCreator.createDealWithYandexPlacements;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.BIG_PLACEMENT_PAGE_ID;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.PRIVATE_GOAL_ID;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.PUBLIC_GOAL_ID;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.adfoxPixelUrl;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.dcmPixelUrl;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.yaAudiencePixelUrl;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultGoalWithId;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmBannerAdGroup;
import static ru.yandex.direct.core.testing.data.TestNewCpmBanners.fullCpmBanner;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.defaultRetCondition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

/**
 * Тест проверяет валидацию пикселей на cpm-баннерах в комплексной операции обновления cpm-групп
 * с criterion_type=USER_PROFILE
 */
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ComplexCpmUpdateWithBannerPixelsAndRetargetingsTest {

    @Autowired
    private ComplexAdGroupAddOperationFactory addOperationFactory;

    @Autowired
    private ComplexAdGroupUpdateOperationFactory updateOperationFactory;

    @Autowired
    private GeoTreeFactory geoTreeFactory;

    @Autowired
    private PlacementsRepository placementsRepository;

    @Autowired
    private BannerTypedRepository bannerTypedRepository;

    @Autowired
    private AdGroupRepository adGroupRepository;

    @Autowired
    private RetargetingRepository retargetingRepository;

    @Autowired
    private TestCryptaSegmentRepository testCryptaSegmentRepository;

    @Autowired
    private Steps steps;

    private GeoTree geoTree;

    protected static ClientInfo clientInfo;
    protected static Long canvasCreativeId;
    protected static Long canvasCreativeIdOtherSize;
    protected static CampaignInfo dealCampaignNonYandexPlacements;
    protected static CampaignInfo dealCampaignYandexPlacements;
    protected static Boolean isTnsDisabled;
    protected List<DealInfo> dealInfosNonYandex;
    protected List<DealInfo> dealInfosYandex;
    private Long operatorUid;

    @Before
    public void before() throws Exception {
        List<Goal> goalsToAdd = Arrays.asList(
                defaultGoalWithId(PRIVATE_GOAL_ID, GoalType.SOCIAL_DEMO),
                defaultGoalWithId(PUBLIC_GOAL_ID, GoalType.SOCIAL_DEMO)
        );
        testCryptaSegmentRepository.addAll(goalsToAdd);
        geoTree = geoTreeFactory.getGlobalGeoTree();

        ClientInfo agencyClientInfo = steps.clientSteps().createDefaultClientWithRole(RbacRole.AGENCY);
        clientInfo = steps.clientSteps().createDefaultClientUnderAgency(agencyClientInfo);
        operatorUid = agencyClientInfo.getUid();

        CreativeInfo creativeInfo = steps.creativeSteps().addDefaultCanvasCreative(clientInfo);
        canvasCreativeId = creativeInfo.getCreativeId();
        canvasCreativeIdOtherSize = steps.creativeSteps()
                .addDefaultCanvasCreativeWithSize(clientInfo, 300L, 200L)
                .getCreativeId();
        dealInfosNonYandex = createDealWithNonYandexPlacements(steps, placementsRepository, agencyClientInfo);
        dealInfosYandex = createDealWithYandexPlacements(steps, placementsRepository, agencyClientInfo);
        dealCampaignNonYandexPlacements = createCpmDealCampaignWithDeals(dealInfosNonYandex, clientInfo);
        dealCampaignYandexPlacements = createCpmDealCampaignWithDeals(dealInfosYandex, clientInfo);
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

    //Обновляем баннер добавлением пикселя dcm и обновляем имеющийся на группе публичный ретаргетинг на приватный
    @Test
    public void addDcmPixel_PublicRetargetingToPrivate_NegativeTest() {
        Matcher errorMatcher = Matchers.contains(
                validationError(path(index(0), field(ComplexCpmAdGroup.BANNERS), index(0),
                        field(OldCpmBanner.PIXELS), index(0)),
                        BannerDefects.noRightsToPixel(dcmPixelUrl(), emptyList(),
                                CampaignType.CPM_DEALS,
                                InventoryType.PRIVATE_CONDITIONS_FOREIGN_INVENTORY)));
        testSingleGroupWithSingleBanner(null, dcmPixelUrl(), PUBLIC_GOAL_ID, PRIVATE_GOAL_ID,
                dealCampaignNonYandexPlacements.getCampaignId(), errorMatcher);
    }

    //Добавляем на группу баннер с dcm пикселем и публичным условием ретаргетинга, а потом меняем на приватное и
    //меняем пиксель на пиксель adfox
    @Test
    public void updateDcmPixelToAdfox_PublicRetargetingToPrivate_PositiveTest() {
        testSingleGroupWithSingleBanner(dcmPixelUrl(), adfoxPixelUrl(), PUBLIC_GOAL_ID, PRIVATE_GOAL_ID,
                dealCampaignNonYandexPlacements.getCampaignId(), null);
    }

    //добавляем в группу баннер без пикселей и приватное условие ретаргетинга,
    // а потом пытаемся обновить на запрещённый пиксель
    @Test
    public void addDcmPixel_LeavePrivateRetargeting_NegativeTest() {
        Matcher errorMatcher = Matchers.contains(validationError(
                path(index(0), field(ComplexCpmAdGroup.BANNERS), index(0),
                        field(OldCpmBanner.PIXELS), index(0)),
                BannerDefects.noRightsToPixel(dcmPixelUrl(), emptyList(),
                        CampaignType.CPM_DEALS,
                        InventoryType.PRIVATE_CONDITIONS_FOREIGN_INVENTORY)));
        testSingleGroupWithSingleBanner(null, dcmPixelUrl(), PRIVATE_GOAL_ID, PRIVATE_GOAL_ID,
                dealCampaignNonYandexPlacements.getCampaignId(), errorMatcher);
    }

    /**
     * Добавляем баннер с пикселем dcm, затем обновляем имеющийся на группе публичный ретаргетинг
     * на приватный, а баннер не передаём в операцию обновления. Сейчас такой кейс не поддержан,
     * так как нам с веба приходят все баннеры
     */
    @Test
    @Ignore("См комментарий выше")
    public void publicRetargetingToPrivate_BannerUnchanged_NegativeTest() {
        Matcher errorMatcher = Matchers.contains(validationError(
                path(index(0), field(ComplexCpmAdGroup.TARGET_INTERESTS), index(0)),
                BannerDefects.noRightsToPixel(dcmPixelUrl(), emptyList(),
                        CampaignType.CPM_DEALS,
                        InventoryType.PRIVATE_CONDITIONS_FOREIGN_INVENTORY)));
        RetargetingConditionBase retargetingConditionForAdd = buildRetargetingConditionByGoalId(PUBLIC_GOAL_ID);
        RetargetingConditionBase retargetingConditionForUpdate = buildRetargetingConditionByGoalId(PRIVATE_GOAL_ID);
        var bannersForAdd = singletonList(fullCpmBanner(null, null, canvasCreativeId)
                .withPixels(singletonList(dcmPixelUrl())));
        List<CpmBanner> bannersForUpdate = emptyList();
        testSingleAdGroup(retargetingConditionForAdd, retargetingConditionForUpdate, bannersForAdd,
                bannersForUpdate, dealCampaignNonYandexPlacements.getCampaignId(), 1, errorMatcher);
    }

    //Обновляем баннер добавлением пикселя dcm и обновляем имеющийся на группе приватный ретаргетинг на публичный
    @Test
    public void addDcmPixel_PrivateRetargetingToPublic_PositiveTest() {
        testSingleGroupWithSingleBanner(null, dcmPixelUrl(), PRIVATE_GOAL_ID, PUBLIC_GOAL_ID,
                dealCampaignNonYandexPlacements.getCampaignId(), null);
    }

    //Добавляем группу, соответствующую сделке на яндексовской площадке, добавляем пиксель аудиторий.
    //Обновляем публичные ретаргетинги на приватные, баннер передаём
    @Test
    public void leaveYaAudiencePixel_PublicToPrivateRetargeting_PositiveTest() {
        testSingleGroupWithSingleBanner(yaAudiencePixelUrl(), yaAudiencePixelUrl(),
                PUBLIC_GOAL_ID, PRIVATE_GOAL_ID, dealCampaignYandexPlacements.getCampaignId(), null);
    }

    //Добавляем группу, соответствующую сделке на неяндексовской площадке, баннер без пикселей
    //Оставляем публичные ретаргетинги, добавляем пиксель аудиторий.
    @Test
    public void leaveYaAudiencePixel_NonYandexPlacement_NegativeTest() {
        Matcher errorMatcher = Matchers.contains(
                validationError(path(index(0), field(ComplexCpmAdGroup.BANNERS), index(0),
                        field(OldCpmBanner.PIXELS), index(0)),
                        BannerDefects.noRightsToAudiencePixel(yaAudiencePixelUrl())));
        testSingleGroupWithSingleBanner(null, yaAudiencePixelUrl(),
                PUBLIC_GOAL_ID, PUBLIC_GOAL_ID, dealCampaignNonYandexPlacements.getCampaignId(), errorMatcher);
    }

    //Добавляем группу, баннер с dcm пикселем, публичный ретаргетинг
    //Затем меняем на приватный, пиксель оставляем
    @Test
    public void leaveDcmPixel_PublicToPrivateRetargeting_NegativeTest() {
        Matcher errorMatcher = Matchers.contains(
                validationError(path(index(0), field(ComplexCpmAdGroup.BANNERS), index(0),
                        field(OldCpmBanner.PIXELS), index(0)),
                        BannerDefects.noRightsToPixel(dcmPixelUrl(), emptyList(),
                                CampaignType.CPM_DEALS,
                                InventoryType.PRIVATE_CONDITIONS_FOREIGN_INVENTORY)));
        testSingleGroupWithSingleBanner(dcmPixelUrl(), dcmPixelUrl(),
                PUBLIC_GOAL_ID, PRIVATE_GOAL_ID, dealCampaignNonYandexPlacements.getCampaignId(), errorMatcher);
    }

    //Добавляем группу, баннер с adfox пикселем, публичный ретаргетинг
    //Затем меняем на приватный, пиксель обновляем на dcm
    @Test
    public void addDcmPixel_YandexPlacements_NegativeTest() {
        Matcher errorMatcher = Matchers.contains(
                validationError(path(index(0), field(ComplexCpmAdGroup.BANNERS), index(0),
                        field(OldCpmBanner.PIXELS), index(0)),
                        BannerDefects.noRightsToPixel(dcmPixelUrl(), emptyList(),
                                CampaignType.CPM_DEALS,
                                InventoryType.PRIVATE_CONDITIONS_FOREIGN_INVENTORY)));
        testSingleGroupWithSingleBanner(adfoxPixelUrl(), dcmPixelUrl(),
                PUBLIC_GOAL_ID, PRIVATE_GOAL_ID, dealCampaignYandexPlacements.getCampaignId(), errorMatcher);
    }

    public void testSingleGroupWithSingleBanner(String pixelForAdd, String pixelForUpdate,
                                                Long goalIdForAdd, Long goalIdForUpdate,
                                                Long campaignId, Matcher expectedErrorMatcher) {
        Integer expectedErrorNum = (expectedErrorMatcher == null ? 0 : 1);
        RetargetingConditionBase retargetingConditionForAdd = buildRetargetingConditionByGoalId(goalIdForAdd);
        RetargetingConditionBase retargetingConditionForUpdate = buildRetargetingConditionByGoalId(goalIdForUpdate);
        var bannersForAdd = singletonList(fullCpmBanner(null, null, canvasCreativeId)
                .withPixels(pixelForAdd == null ? emptyList() : singletonList(pixelForAdd)));
        var bannersForUpdate = singletonList(fullCpmBanner(null, null, canvasCreativeId)
                .withPixels(pixelForUpdate == null ? emptyList() : singletonList(pixelForUpdate)));
        testSingleAdGroup(retargetingConditionForAdd, retargetingConditionForUpdate, bannersForAdd,
                bannersForUpdate, campaignId, expectedErrorNum, expectedErrorMatcher);
    }

    public void testSingleAdGroup(RetargetingConditionBase retargetingConditionForAdd,
                                  RetargetingConditionBase retargetingConditionForUpdate,
                                  List<CpmBanner> bannersForAdd,
                                  List<CpmBanner> bannersForUpdate,
                                  Long campaignId,
                                  Integer numErrorsExpected,
                                  Matcher errorsMatcher) {
        List<TargetInterest> targetInterestsForAdd = singletonList(new TargetInterest());
        ComplexCpmAdGroup complexCpmAdGroupForAdd = new ComplexCpmAdGroup()
                .withAdGroup(activeCpmBannerAdGroup(campaignId)
                        .withCriterionType(CriterionType.USER_PROFILE))
                .withTargetInterests(targetInterestsForAdd)
                .withRetargetingConditions(singletonList(retargetingConditionForAdd))
                .withBanners(new ArrayList<>(bannersForAdd));
        ComplexCpmAdGroupAddOperation addOperation = createAddOperation(singletonList(complexCpmAdGroupForAdd));
        MassResult<Long> addOperationResult = addOperation.prepareAndApply();
        List<Long> adGroupIds = mapList(addOperationResult.getResult(), Result::getResult);


        List<ComplexCpmAdGroup> complexCpmAdGroupsForUpdate = constructComplexCpmAdGroupsForUpdate(
                singletonList(retargetingConditionForUpdate), singletonList(bannersForUpdate), adGroupIds);
        ComplexCpmAdGroupUpdateOperation updateOperation = createUpdateOperation(complexCpmAdGroupsForUpdate);
        MassResult<Long> updateOperationResult = updateOperation.prepareAndApply();

        Integer numErrorsActual = updateOperationResult.getValidationResult().flattenErrors().size();
        assertThat(numErrorsActual, comparesEqualTo(numErrorsExpected));
        if (errorsMatcher != null) {
            assertThat(updateOperationResult.getValidationResult().flattenErrors(), errorsMatcher);
        }
    }

    private RetargetingConditionBase buildRetargetingConditionByGoalId(Long goalId) {
        return steps.retConditionSteps()
                .createRetCondition(
                        (RetargetingCondition) defaultRetCondition(null)
                                .withType(ConditionType.interests)
                                .withRules(singletonList(createRuleFromSocialDemoGoalIds(singletonList(goalId)))),
                        clientInfo
                ).getRetCondition().withId(null);
    }

    private static Rule createRuleFromSocialDemoGoalIds(List<Long> socialDemoGoalIds) {
        List<Goal> goals = createGoalsFromGoalIds(socialDemoGoalIds);
        Rule rule = new Rule();
        rule.withGoals(goals).withType(RuleType.OR);
        return rule;
    }

    private static List<Goal> createGoalsFromGoalIds(List<Long> socialDemoGoalIds) {
        return StreamEx.of(socialDemoGoalIds).map(id -> {
            Goal someGoal = new Goal();
            someGoal.withId(id).withType(GoalType.SOCIAL_DEMO);
            return someGoal;
        }).toList();
    }

    private CampaignInfo createCpmDealCampaignWithDeals(List<DealInfo> dealInfos,
                                                        ClientInfo clientInfo) {
        CampaignInfo cpmDealCampaignInfo = steps.campaignSteps().createActiveCpmDealsCampaign(clientInfo);
        mapList(dealInfos, DealInfo::getDealId).forEach(
                dealId -> steps.dealSteps().linkDealWithCampaign(dealId, cpmDealCampaignInfo.getCampaignId()));
        return cpmDealCampaignInfo;
    }

    private ComplexCpmAdGroupAddOperation createAddOperation(List<ComplexCpmAdGroup> complexAdGroups) {
        return addOperationFactory.createCpmAdGroupAddOperation(true, complexAdGroups,
                geoTree, false, null, operatorUid,
                clientInfo.getClientId(), clientInfo.getUid(), true);
    }

    private List<ComplexCpmAdGroup> constructComplexCpmAdGroupsForUpdate(
            List<RetargetingConditionBase> retargetingConditionsForUpdate,
            List<List<CpmBanner>> bannersForUpdate,
            List<Long> adGroupIds) {
        Map<Long, List<Long>> bannerIdsByAdGroupIds = StreamEx.of(bannerTypedRepository.getBannersByGroupIds(
                clientInfo.getShard(), adGroupIds))
                .map(b -> (CpmBanner) b)
                .mapToEntry(CpmBanner::getAdGroupId, CpmBanner::getId)
                .collapseKeys()
                .toMap();
        Map<Long, List<CpmBanner>> bannersForUpdateByAdGroupIds = EntryStream.of(adGroupIds)
                .mapValues(bannerIdsByAdGroupIds::get)
                .mapToValue((index, idList) -> StreamEx.zip(bannersForUpdate.get(index), idList,
                        (cpmBanner, id) -> cpmBanner.withId(id)).toList())
                .mapKeys(adGroupIds::get)
                .toMap();

        Map<Long, List<Retargeting>> retargetingsAddedByAdGroupId =
                StreamEx.of(retargetingRepository.getRetargetingsByAdGroups(clientInfo.getShard(), adGroupIds))
                        .groupingBy(Retargeting::getAdGroupId);
        Map<Integer, Long> retConditionIdByAdGroupIndex = EntryStream.of(adGroupIds)
                .mapValues(retargetingsAddedByAdGroupId::get)
                .mapValues(retargetings -> retargetings.get(0))
                .mapValues(Retargeting::getRetargetingConditionId)
                .toMap();
        Map<Long, List<RetargetingConditionBase>> retConditionsForUpdateByAdGroupIds =
                EntryStream.of(retConditionIdByAdGroupIndex)
                        .mapToValue((index, id) -> retargetingConditionsForUpdate.get(index).withId(id))
                        .mapValues(retCondition -> singletonList(retCondition))
                        .mapKeys(adGroupIds::get)
                        .toMap();
        Map<Long, List<TargetInterest>> targetInterestsForUpdateByAdGroupIds = StreamEx.of(adGroupIds)
                .mapToEntry(retargetingsAddedByAdGroupId::get)
                .filterValues(Objects::nonNull)
                .mapValues(t -> convertRetargetingsToTargetInterests(t, emptyList()))
                .toMap();

        List<AdGroup> adGroups = adGroupRepository.getAdGroups(clientInfo.getShard(), adGroupIds);
        return StreamEx.of(adGroups)
                .map(singleAdGroup -> new ComplexCpmAdGroup()
                        .withAdGroup(singleAdGroup)
                        .withTargetInterests(targetInterestsForUpdateByAdGroupIds.get(singleAdGroup.getId()))
                        .withRetargetingConditions(retConditionsForUpdateByAdGroupIds.get(singleAdGroup.getId()))
                        .withBanners(new ArrayList<>(bannersForUpdateByAdGroupIds.get(singleAdGroup.getId()))))
                .toList();
    }

    private ComplexCpmAdGroupUpdateOperation createUpdateOperation(List<ComplexCpmAdGroup> adGroups) {
        return updateOperationFactory.createCpmAdGroupUpdateOperation(adGroups, geoTree, false, null,
                operatorUid, clientInfo.getClientId(), clientInfo.getUid(), true);
    }
}
