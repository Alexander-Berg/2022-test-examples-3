package ru.yandex.direct.web.entity.adgroup.controller.cpm;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;
import one.util.streamex.StreamEx;
import org.junit.After;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerType;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerWithCreative;
import ru.yandex.direct.core.entity.banner.model.old.OldCpmBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldCpmIndoorBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldCpmOutdoorBanner;
import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.currency.model.cpmyndxfrontpage.FrontpageCampaignShowType;
import ru.yandex.direct.core.entity.deal.model.Deal;
import ru.yandex.direct.core.entity.deal.model.DealPlacement;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.placements.model.Placement;
import ru.yandex.direct.core.entity.placements.repository.PlacementsRepository;
import ru.yandex.direct.core.entity.retargeting.model.ConditionType;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.GoalType;
import ru.yandex.direct.core.entity.retargeting.model.Retargeting;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.entity.retargeting.model.Rule;
import ru.yandex.direct.core.entity.retargeting.service.RetargetingConditionService;
import ru.yandex.direct.core.entity.tag.model.Tag;
import ru.yandex.direct.core.entity.tag.repository.TagRepository;
import ru.yandex.direct.core.entity.turbolanding.repository.TurboLandingRepository;
import ru.yandex.direct.core.testing.data.TestDeals;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.core.testing.info.DealInfo;
import ru.yandex.direct.core.testing.info.RetConditionInfo;
import ru.yandex.direct.core.testing.info.RetargetingInfo;
import ru.yandex.direct.core.testing.repository.TestClientRepository;
import ru.yandex.direct.core.testing.repository.TestCpmYndxFrontpageRepository;
import ru.yandex.direct.core.testing.repository.TestCryptaSegmentRepository;
import ru.yandex.direct.core.testing.repository.TestTagRepository;
import ru.yandex.direct.core.testing.stub.MetrikaHelperStub;
import ru.yandex.direct.dbutil.model.UidAndClientId;
import ru.yandex.direct.multitype.entity.LimitOffset;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.web.core.model.WebResponse;
import ru.yandex.direct.web.core.model.retargeting.CryptaInterestTypeWeb;
import ru.yandex.direct.web.entity.adgroup.controller.AdGroupControllerTestBase;
import ru.yandex.direct.web.entity.adgroup.controller.CpmAdGroupController;
import ru.yandex.direct.web.entity.adgroup.converter.RetargetingConverter;
import ru.yandex.direct.web.entity.adgroup.model.WebCpmAdGroup;
import ru.yandex.direct.web.entity.adgroup.model.WebCpmAdGroupRetargeting;
import ru.yandex.direct.web.entity.adgroup.model.WebRetargetingGoal;
import ru.yandex.direct.web.entity.adgroup.model.WebRetargetingRule;
import ru.yandex.direct.web.entity.banner.model.WebCpmBanner;
import ru.yandex.direct.web.entity.banner.model.WebPixel;
import ru.yandex.direct.web.entity.keyword.model.WebKeyword;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.thymeleaf.util.SetUtils.singletonSet;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.core.entity.banner.turbolanding.model.OldBannerTurboLandingStatusModerate.READY;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.BIG_PLACEMENT_PAGE_ID;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.PRIVATE_GOAL_ID;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.PUBLIC_GOAL_ID;
import static ru.yandex.direct.core.testing.data.TestFullGoals.filterCryptaGoals;
import static ru.yandex.direct.core.testing.data.TestFullGoals.filterMetrikaGoals;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.defaultRetCondition;
import static ru.yandex.direct.core.testing.data.TestRetargetings.defaultRetargeting;
import static ru.yandex.direct.test.utils.matcher.LocalDateTimeMatcher.approximatelyNow;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.web.testing.data.TestAdGroups.defaultCpmAdGroupRetargeting;

public class CpmAdGroupControllerTestBase extends AdGroupControllerTestBase {
    private static final CompareStrategy RETARGETING_CONDITION_MATCHER =
            DefaultCompareStrategies.onlyExpectedFields()
                    .forFields(newPath(RetargetingCondition.LAST_CHANGE_TIME.name())).useMatcher(approximatelyNow());

    protected ClientInfo agencyClientInfo;
    protected CampaignInfo campaignInfo;
    protected CampaignInfo frontpageCampaignInfo;

    @Autowired
    protected CpmAdGroupController controller;
    @Autowired
    private TagRepository tagRepository;
    @Autowired
    protected TestTagRepository testTagRepository;

    @Autowired
    private RetargetingConditionService retargetingConditionService;
    @Autowired
    protected MetrikaHelperStub metrikaHelperStub;
    @Autowired
    protected TestCryptaSegmentRepository testCryptaSegmentRepository;
    @Autowired
    protected TurboLandingRepository turboLandingRepository;
    @Autowired
    protected PlacementsRepository placementsRepository;
    @Autowired
    private TestCpmYndxFrontpageRepository testCpmYndxFrontpageRepository;
    @Autowired
    private TestClientRepository testClientRepository;

    protected Long creativeId;

    protected WebCpmAdGroupRetargeting retargetingForAdd;

    protected Long interestsRetCondId;

    private List<Deal> addedDeals;
    protected Goal publicGoal;
    protected Goal privateGoal;
    private Placement yandexPlacement;
    private Placement nonYandexPlacement;
    private Deal yandexOnlyPlacementDeal;
    protected Deal nonYandexOnlyPlacementDeal;
    private Deal emptyPlacementDeal;

    @Before
    public void before() {
        super.before();
        agencyClientInfo = steps.clientSteps()
                .createDefaultClientWithRole(RbacRole.AGENCY);
        testClientRepository.setAgencyToClient(agencyClientInfo.getShard(),
                UidAndClientId.of(agencyClientInfo.getUid(), agencyClientInfo.getClientId()),
                singletonSet(clientId));

        campaignInfo = steps.campaignSteps().createActiveCpmBannerCampaign(clientInfo);
        testCpmYndxFrontpageRepository.fillMinBidsTestValues();
        frontpageCampaignInfo = steps.campaignSteps().createActiveCpmYndxFrontpageCampaign(clientInfo);
        testCpmYndxFrontpageRepository.setCpmYndxFrontpageCampaignsAllowedFrontpageTypes(
                frontpageCampaignInfo.getShard(),
                frontpageCampaignInfo.getCampaignId(),
                singletonList(FrontpageCampaignShowType.FRONTPAGE));
        CreativeInfo creativeInfo = steps.creativeSteps().addDefaultCanvasCreative(clientInfo);
        creativeId = creativeInfo.getCreativeId();
        RetConditionInfo retConditionInfo = steps.retConditionSteps().createRetCondition(
                (RetargetingCondition) defaultRetCondition(null).withType(ConditionType.interests),
                clientInfo
        );
        interestsRetCondId = retConditionInfo.getRetConditionId();

        addedDeals = new ArrayList<>();
        publicGoal = new Goal();
        publicGoal.withId(PUBLIC_GOAL_ID).withType(GoalType.SOCIAL_DEMO);
        privateGoal = new Goal();
        privateGoal.withId(PRIVATE_GOAL_ID).withType(GoalType.SOCIAL_DEMO);
        yandexPlacement =
                new Placement().withIsYandexPage(1L).withPageId(BIG_PLACEMENT_PAGE_ID).withDomain("");
        nonYandexPlacement =
                new Placement().withIsYandexPage(0L).withPageId(BIG_PLACEMENT_PAGE_ID + 1).withDomain("");
        placementsRepository.insertPlacements(ImmutableList.of(yandexPlacement, nonYandexPlacement));
        yandexOnlyPlacementDeal = dealWithPlacements(singletonList(yandexPlacement));
        nonYandexOnlyPlacementDeal = dealWithPlacements(singletonList(nonYandexPlacement));
        emptyPlacementDeal = dealWithPlacements(emptyList());

        retargetingForAdd = defaultCpmAdGroupRetargeting();
        List<WebRetargetingGoal> goals = StreamEx.of(retargetingForAdd.getGroups())
                .toFlatList(WebRetargetingRule::getGoals);
        List<Goal> coreGoals = mapList(goals, RetargetingConverter::webRetargetingGoalToCore);
        coreGoals.addAll(ImmutableList.of(publicGoal, privateGoal));
        List<Goal> metrikaGoals = filterMetrikaGoals(coreGoals);
        metrikaHelperStub.addGoals(clientInfo.getUid(), metrikaGoals);
        testCryptaSegmentRepository.addAll(filterCryptaGoals(coreGoals));
    }

    protected CampaignInfo createCpmDealCampaignWithInventory(List<Deal> deals) {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveCpmDealsCampaign(clientInfo);
        List<DealInfo> dealsInfo = steps.dealSteps().addDeals(deals, agencyClientInfo);
        addedDeals.addAll(mapList(dealsInfo, DealInfo::getDeal));
        mapList(dealsInfo, DealInfo::getDealId).forEach(
                dealId -> steps.dealSteps().linkDealWithCampaign(dealId, campaignInfo.getCampaignId()));
        return campaignInfo;
    }

    private Deal dealWithPlacements(List<Placement> placements) {
        List<DealPlacement> dealPlacements = mapList(placements, t -> new DealPlacement().withPageId(t.getPageId()));
        Deal deal = TestDeals.defaultPrivateDeal(agencyClientInfo.getClientId());
        deal.withPlacements(dealPlacements);
        return deal;
    }

    @After
    public void after() {
        List<Long> dealIds = mapList(addedDeals, Deal::getId);
        steps.dealSteps().unlinkDeals(shard, dealIds);
        steps.dealSteps().deleteDeals(addedDeals, clientInfo);
        placementsRepository
                .deletePlacementsBy(ImmutableList.of(yandexPlacement.getPageId(), nonYandexPlacement.getPageId()));
    }

    protected void addAndExpectError(WebCpmAdGroup requestAdGroup, String path, String code) {
        WebResponse response = controller.saveCpmAdGroup(singletonList(requestAdGroup), campaignInfo.getCampaignId(),
                true, false, false, null);
        checkErrorResponse(response, path, code);
    }

    protected void updateAndExpectError(WebCpmAdGroup requestAdGroup, String path, String code) {
        WebResponse response = controller.saveCpmAdGroup(singletonList(requestAdGroup), campaignInfo.getCampaignId(),
                false, false, false, null);
        checkErrorResponse(response, path, code);
    }

    protected void updateAndCheckResult(WebCpmAdGroup requestAdGroup) {
        updateAndCheckResult(singletonList(requestAdGroup));
    }

    protected void updateAndCheckResult(List<WebCpmAdGroup> requestAdGroups) {
        WebResponse response = controller.saveCpmAdGroup(requestAdGroups, campaignInfo.getCampaignId(),
                false, false, null, null);
        checkResponse(response);
    }

    protected List<AdGroup> findAdGroups() {
        return findAdGroups(campaignInfo.getCampaignId());
    }

    protected List<Keyword> findKeywords() {
        return findKeywords(campaignInfo.getCampaignId());
    }

    protected List<BidModifier> findBidModifiers() {
        return findBidModifiers(campaignInfo.getCampaignId());
    }

    protected List<Long> findTags(long adGroupId) {
        List<Tag> tags =
                tagRepository.getAdGroupsTags(campaignInfo.getShard(), singletonList(adGroupId)).get(adGroupId);
        return mapList(tags, Tag::getId);
    }

    protected void checkBanner(long adGroupId, AdGroupType adGroupType, WebCpmBanner webBanner) {
        checkBanner(adGroupId, adGroupType, webBanner, true);
    }

    protected void checkBannerWithoutTurbo(long adGroupId, AdGroupType adGroupType, WebCpmBanner webBanner) {
        checkBanner(adGroupId, adGroupType, webBanner, false);
    }

    private void checkBanner(long adGroupId, AdGroupType adGroupType, WebCpmBanner webBanner, boolean hasTurbo) {
        List<OldBanner> banners = findOldBanners(adGroupId);
        assertThat("в группе должен быть один баннер", banners, hasSize(1));

        final OldBannerType bannerType;
        if (adGroupType == AdGroupType.CPM_OUTDOOR) {
            bannerType = OldBannerType.CPM_OUTDOOR;
        } else if (adGroupType == AdGroupType.CPM_INDOOR) {
            bannerType = OldBannerType.CPM_INDOOR;
        } else {
            bannerType = OldBannerType.CPM_BANNER;
        }

        final OldBannerWithCreative expectedBanner;
        if (bannerType == OldBannerType.CPM_BANNER) {
            List<WebPixel> webPixels = webBanner.getPixels();
            List<String> expectedPixels = mapList(webPixels, WebPixel::getUrl);

            expectedBanner = new OldCpmBanner()
                    .withBannerType(OldBannerType.CPM_BANNER)
                    .withPixels(expectedPixels);

            if (hasTurbo) {
                ((OldCpmBanner) expectedBanner).withTurboLandingId(bannerTurboLandings.get(0).getId())
                        .withTurboLandingStatusModerate(READY);
            }

            if (webBanner.getMeasurers() == null || webBanner.getMeasurers().isEmpty()) {
                ((OldCpmBanner) expectedBanner).withMeasurers(emptyList());
            }
        } else if (bannerType == OldBannerType.CPM_INDOOR) {
            expectedBanner = new OldCpmIndoorBanner()
                    .withBannerType(OldBannerType.CPM_INDOOR);
        } else {
            expectedBanner = new OldCpmOutdoorBanner()
                    .withBannerType(OldBannerType.CPM_OUTDOOR);
        }

        expectedBanner
                .withCreativeId(Long.parseLong(webBanner.getCreative().getCreativeId()))
                .withIsMobile(false)
                .withHref(webBanner.getHref() == null ? null : webBanner.getUrlProtocol() + webBanner.getHref());

        assertThat("баннер обновился корректно", banners.get(0),
                beanDiffer((OldBanner) expectedBanner).useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields()));
    }

    protected void checkKeywords(List<WebKeyword> requestKeywords) {
        List<Keyword> keywords = findKeywords();
        assertThat("неверное количество фраз", keywords, hasSize(requestKeywords.size()));

        assertThat("фраза не совпадает с ожидаемой",
                keywords.get(0).getPhrase(), equalTo(requestKeywords.get(0).getPhrase()));
    }

    protected void checkRetargetings(List<WebCpmAdGroupRetargeting> requestRetargetings, long adGroupId) {
        List<Retargeting> retargetings = findRetargetings(adGroupId);
        assertThat("неверное количество ретаргетингов", retargetings, hasSize(requestRetargetings.size()));

        List<Retargeting> expectedRetargetings = mapList(requestRetargetings, r -> new Retargeting()
                .withPriceContext(BigDecimal.valueOf(r.getPriceContext()).setScale(2, RoundingMode.DOWN)));
        assertThat("ретаргетинг корректный", retargetings, containsInAnyOrder(mapList(expectedRetargetings, r ->
                beanDiffer(r).useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields()))));
    }

    protected void checkRetargetingConditions(List<WebCpmAdGroupRetargeting> retargetings, long adGroupId) {
        if (retargetings == null) {
            return;
        }
        List<RetargetingCondition> retargetingConditions = getExpectedRetargetingConditions(retargetings);

        List<RetargetingCondition> actual = retargetingConditionService
                .getRetargetingConditions(clientId, null, singletonList(adGroupId), null, null,
                        LimitOffset.maxLimited());

        assertThat("в группе правильные условия ретаргетинга", actual, containsInAnyOrder(
                mapList(retargetingConditions,
                        rc -> beanDiffer(rc).useCompareStrategy(RETARGETING_CONDITION_MATCHER))));
    }

    private static List<RetargetingCondition> getExpectedRetargetingConditions(
            List<WebCpmAdGroupRetargeting> retargetings) {
        List<RetargetingCondition> retargetingConditions =
                RetargetingConverter.webCpmRetargetingsToCoreRetargetingConditions(retargetings);
        for (int i = 0; i < retargetingConditions.size(); i++) {
            Long retargetingConditionId = retargetings.get(i).getRetargetingConditionId();
            retargetingConditions.get(i).withId(retargetingConditionId);
        }
        return retargetingConditions;
    }

    protected WebCpmAdGroupRetargeting createRetargeting(AdGroupInfo adGroupInfo) {
        RetConditionInfo retargetingCondition = steps.retConditionSteps().createCpmRetCondition(clientInfo);
        return createRetargeting(adGroupInfo, retargetingCondition);
    }

    protected WebCpmAdGroupRetargeting createIndoorRetargeting(AdGroupInfo adGroupInfo) {
        RetConditionInfo retargetingCondition = steps.retConditionSteps().createIndoorRetCondition(clientInfo);
        return createRetargeting(adGroupInfo, retargetingCondition);
    }

    private WebCpmAdGroupRetargeting createRetargeting(AdGroupInfo adGroupInfo, RetConditionInfo retargetingCondition) {
        RetargetingInfo retargeting = steps.retargetingSteps().createRetargeting(defaultRetargeting()
                .withRetargetingConditionId(retargetingCondition.getRetConditionId()), adGroupInfo);

        return new WebCpmAdGroupRetargeting()
                .withId(retargeting.getRetargetingId())
                .withRetargetingConditionId(retargetingCondition.getRetConditionId())
                .withPriceContext(retargeting.getRetargeting().getPriceContext().doubleValue())
                .withName(retargetingCondition.getRetCondition().getName())
                .withDescription(retargetingCondition.getRetCondition().getDescription())
                .withConditionType(retargetingCondition.getRetCondition().getType())
                .withGroups(mapList(retargetingCondition.getRetCondition().getRules(), this::ruleFromCore));
    }

    private WebRetargetingRule ruleFromCore(Rule rule) {
        return new WebRetargetingRule()
                .withInterestType(CryptaInterestTypeWeb.fromCoreType(rule.getInterestType()))
                .withRuleType(rule.getType())
                .withGoals(mapList(rule.getGoals(), this::goalFromCore));
    }

    private WebRetargetingGoal goalFromCore(Goal goal) {
        return new WebRetargetingGoal()
                .withId(goal.getId())
                .withGoalType(goal.getType())
                .withTime(goal.getTime());
    }
}
