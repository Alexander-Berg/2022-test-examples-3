package ru.yandex.direct.web.entity.inventori.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import one.util.streamex.StreamEx;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.model.CpmIndoorAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmOutdoorAdGroup;
import ru.yandex.direct.core.entity.banner.model.ModerateBannerPage;
import ru.yandex.direct.core.entity.banner.model.StatusModerateBannerPage;
import ru.yandex.direct.core.entity.banner.model.StatusModerateOperator;
import ru.yandex.direct.core.entity.bidmodifier.BannerType;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierBannerTypeAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierInventoryAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.InventoryType;
import ru.yandex.direct.core.entity.crypta.repository.CryptaSegmentRepository;
import ru.yandex.direct.core.entity.currency.model.cpmyndxfrontpage.FrontpageCampaignShowType;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.security.DirectAuthentication;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CpmIndoorBannerInfo;
import ru.yandex.direct.core.testing.info.CpmOutdoorBannerInfo;
import ru.yandex.direct.core.testing.info.RetConditionInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.repository.TestCpmYndxFrontpageRepository;
import ru.yandex.direct.core.testing.repository.TestCryptaSegmentRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.campaign.model0.strategy.AutobudgetMaxImpressionsCustomPeriodStrategy;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.configuration.mock.auth.DirectWebAuthenticationSourceMock;
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmAudioBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmBannerWithTurbolanding;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmGeoPinBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmIndoorBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmOutdoorBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmVideoBanner;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultBannerTypeAdjustment;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultInventoryAdjustment;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyBannerTypeModifier;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyInventoryModifier;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeCpmBannerCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeCpmYndxFrontpageCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.autobudgetMaxImpressionsCustomPeriodStrategy;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmGeoproductAdGroup;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.defaultCpmRetCondition;
import static ru.yandex.direct.core.testing.data.TestRetargetings.defaultTargetInterest;

@DirectWebTest
@RunWith(SpringRunner.class)
public abstract class CampaignForecastControllerTestBase {

    protected static final long REACH_LESS_THAN = 500;

    protected static final int SUCCESS_CODE = HttpStatus.OK.value();
    protected static final int INTERNAL_ERROR_CODE = HttpStatus.INTERNAL_SERVER_ERROR.value();
    protected static final int VALIDATION_ERROR_CODE = HttpStatus.BAD_REQUEST.value();

    protected static final String CAMPAIGN_ID_PATH = "campaign_id";
    protected static final String EXAMPLE_TYPE_PATH = "new_campaign_example_type";
    protected static final String STRATEGY_PATH = "strategy";
    protected static final String STRATEGY_TYPE_PATH = STRATEGY_PATH + ".type";
    protected static final String BUDGET_PATH = STRATEGY_PATH + ".budget";
    protected static final String START_DATE_PATH = STRATEGY_PATH + ".start_date";
    protected static final String END_DATE_PATH = STRATEGY_PATH + ".end_date";
    protected static final String IMPRESSION_LIMIT_PATH = STRATEGY_PATH + ".impression_limit";
    protected static final String DAYS_PATH = IMPRESSION_LIMIT_PATH + ".days";
    protected static final String IMPRESSIONS_PATH = IMPRESSION_LIMIT_PATH + ".impressions";
    protected static final String CPM_PATH = STRATEGY_PATH + ".cpm";

    @Autowired
    protected Steps steps;

    @Autowired
    protected DirectWebAuthenticationSource authenticationSource;

    @Autowired
    protected TestCryptaSegmentRepository testCryptaSegmentRepository;

    @Autowired
    protected CryptaSegmentRepository cryptaSegmentRepository;

    @Autowired
    private TestCpmYndxFrontpageRepository testCpmYndxFrontpageRepository;

    protected ClientInfo clientInfo;
    protected CampaignInfo campaignInfo;
    protected AdGroupInfo adGroupInfo;

    @Before
    public void before() {
        AutobudgetMaxImpressionsCustomPeriodStrategy strategy = autobudgetMaxImpressionsCustomPeriodStrategy();
        campaignInfo = steps.campaignSteps().createCampaign(activeCpmBannerCampaign(null, null)
                .withStrategy(strategy)
                .withDisabledDomains(singleton("ya.ru"))
                .withDisabledSsp(singletonList("Smaato"))
                .withDisabledVideoPlacements(singletonList("video.ru"))
        );
        clientInfo = campaignInfo.getClientInfo();
        setAuthData();
    }

    protected void createCpmBannerCampaign() {
        AutobudgetMaxImpressionsCustomPeriodStrategy campaignStrategy = autobudgetMaxImpressionsCustomPeriodStrategy();
        campaignInfo = steps.campaignSteps().createCampaign(
                activeCpmBannerCampaign(clientInfo.getClientId(), clientInfo.getUid())
                        .withStrategy(campaignStrategy)
                        .withDisabledDomains(singleton("ya.ru"))
                        .withDisabledSsp(singletonList("Smaato"))
                        .withDisabledVideoPlacements(singletonList("video.ru")),
                clientInfo);
    }

    protected void createFullCpmBannerAdGroup(Long creativeId) {
        createFullCpmBannerAdGroup(creativeId, defaultCpmRetCondition());
    }

    protected void createFullCpmBannerAdGroup(Long creativeId, RetargetingCondition retargetingCondition) {
        adGroupInfo = steps.adGroupSteps().createActiveCpmBannerAdGroup(campaignInfo);
        steps.bannerSteps().createActiveCpmBanner(
                activeCpmBanner(campaignInfo.getCampaignId(), adGroupInfo.getAdGroupId(), creativeId), adGroupInfo);

        if (retargetingCondition != null) {
            createRetargetingCondition(retargetingCondition);
        }

        steps.bidModifierSteps().createDefaultAdGroupIosBidModifierMobile(adGroupInfo);
        steps.bidModifierSteps().createDefaultAdGroupBidModifierDesktop(adGroupInfo);
    }

    protected void createFullCpmVideoBannerAdGroup(Long creativeId, RetargetingCondition retargetingCondition) {
        adGroupInfo = steps.adGroupSteps().createActiveCpmVideoAdGroup(campaignInfo);
        steps.bannerSteps().createActiveCpmVideoBanner(activeCpmVideoBanner(campaignInfo.getCampaignId(),
                adGroupInfo.getAdGroupId(), creativeId), adGroupInfo);
        createRetargetingCondition(retargetingCondition);

        steps.bidModifierSteps().createDefaultAdGroupIosBidModifierMobile(adGroupInfo);
        steps.bidModifierSteps().createDefaultAdGroupBidModifierDesktop(adGroupInfo);
    }

    protected void createFullCpmAudioBannerAdGroup(Long creativeId, RetargetingCondition retargetingCondition) {
        adGroupInfo = steps.adGroupSteps().createActiveCpmAudioAdGroup(campaignInfo);
        steps.bannerSteps().createActiveCpmAudioBanner(activeCpmAudioBanner(campaignInfo.getCampaignId(),
                adGroupInfo.getAdGroupId(), creativeId), adGroupInfo);
        createRetargetingCondition(retargetingCondition);

        steps.bidModifierSteps().createDefaultAdGroupIosBidModifierMobile(adGroupInfo);
        steps.bidModifierSteps().createDefaultAdGroupBidModifierDesktop(adGroupInfo);
    }

    protected void createFullCpmGeoproductAdGroup(Long creativeId, RetargetingCondition retargetingCondition) {
        adGroupInfo = steps.adGroupSteps().createActiveCpmGeoproductAdGroup(campaignInfo);
        steps.bannerSteps().createActiveCpmBanner(
                activeCpmBanner(campaignInfo.getCampaignId(), adGroupInfo.getAdGroupId(), creativeId), adGroupInfo);
        createRetargetingCondition(retargetingCondition);

        steps.bidModifierSteps().createDefaultAdGroupIosBidModifierMobile(adGroupInfo);
        steps.bidModifierSteps().createDefaultAdGroupBidModifierDesktop(adGroupInfo);
    }

    protected void  createFullCpmGeoPinAdGroup(Long creativeId, Long permalinkId, RetargetingCondition retargetingCondition) {
        adGroupInfo = steps.adGroupSteps().createActiveCpmGeoPinAdGroup(campaignInfo);
        steps.bannerSteps().createActiveCpmGeoPinBanner(
                activeCpmGeoPinBanner(campaignInfo.getCampaignId(), adGroupInfo.getAdGroupId(), creativeId, permalinkId ), adGroupInfo);
        createRetargetingCondition(retargetingCondition);

        steps.bidModifierSteps().createDefaultAdGroupIosBidModifierMobile(adGroupInfo);
        steps.bidModifierSteps().createDefaultAdGroupBidModifierDesktop(adGroupInfo);
    }

    protected void createCpmYndxFrontpageCampaign(Set<FrontpageCampaignShowType> allowedTypes) {
        AutobudgetMaxImpressionsCustomPeriodStrategy campaignStrategy = autobudgetMaxImpressionsCustomPeriodStrategy();
        campaignInfo = steps.campaignSteps().createCampaign(
                activeCpmYndxFrontpageCampaign(clientInfo.getClientId(), clientInfo.getUid())
                        .withStrategy(campaignStrategy),
                clientInfo);

        testCpmYndxFrontpageRepository.setCpmYndxFrontpageCampaignsAllowedFrontpageTypes(
                campaignInfo.getShard(), campaignInfo.getCampaignId(), allowedTypes);
    }

    protected void createFullCpmYndxFrontpageAdGroup() {
        createFullCpmYndxFrontpageAdGroup(null, defaultCpmRetCondition());
    }

    protected void createFullCpmYndxFrontpageAdGroup(Long creativeId) {
        createFullCpmYndxFrontpageAdGroup(creativeId, defaultCpmRetCondition());
    }

    protected void createFullCpmYndxFrontpageAdGroup(Long creativeId, RetargetingCondition retargetingCondition) {
        adGroupInfo = steps.adGroupSteps().createActiveCpmYndxFrontpageAdGroup(campaignInfo);
        steps.bannerSteps().createActiveCpmBanner(
                activeCpmBanner(campaignInfo.getCampaignId(), adGroupInfo.getAdGroupId(), creativeId), adGroupInfo);
        createRetargetingCondition(retargetingCondition);

        steps.bidModifierSteps().createDefaultAdGroupIosBidModifierMobile(adGroupInfo);
        steps.bidModifierSteps().createDefaultAdGroupBidModifierDesktop(adGroupInfo);
    }

    protected void createFullCpmGeoproductAdGroup(Long creativeId, Long turbolandingId) {
        createFullCpmGeoproductAdGroup(creativeId, turbolandingId, singletonList("app-metro"), singletonList("app-metro"));
    }

    protected void createFullCpmGeoproductAdGroup(Long creativeId, Long turbolandingId, List<String> targetTags,
                                                  List<String> pageGroupTags) {
        adGroupInfo = steps.adGroupSteps().createAdGroup(new AdGroupInfo()
                .withAdGroup(activeCpmGeoproductAdGroup(campaignInfo.getCampaignId())
                        .withPageGroupTags(pageGroupTags)
                        .withTargetTags(targetTags))
                .withCampaignInfo(campaignInfo));
        steps.bannerSteps().createActiveCpmBanner(
                activeCpmBannerWithTurbolanding(campaignInfo.getCampaignId(), adGroupInfo.getAdGroupId(), creativeId,
                        turbolandingId), adGroupInfo);
        createRetargetingCondition();
    }

    protected void createFullCpmOutdoorAdGroup(Long creativeId) {
        adGroupInfo = steps.adGroupSteps().createActiveCpmOutdoorAdGroup(campaignInfo);
        CpmOutdoorBannerInfo banner = steps.bannerSteps().createActiveCpmOutdoorBanner(activeCpmOutdoorBanner(
                campaignInfo.getCampaignId(), adGroupInfo.getAdGroupId(), creativeId), adGroupInfo);
        ModerateBannerPage moderateBannerPage = new ModerateBannerPage()
                .withBannerId(banner.getBannerId())
                .withPageId(((CpmOutdoorAdGroup) adGroupInfo.getAdGroup()).getPageBlocks().get(0).getPageId())
                .withStatusModerate(StatusModerateBannerPage.YES)
                .withStatusModerateOperator(StatusModerateOperator.NONE)
                .withIsRemoved(false)
                .withCreateTime(LocalDateTime.now());
        steps.moderateBannerPageSteps().createModerateBannerPage(banner, moderateBannerPage);
    }

    protected void createFullCpmOutdoorAdGroup() {
        createFullCpmOutdoorAdGroup(null);
    }

    protected void createFullCpmIndoorAdGroup(Long creativeId) {
        adGroupInfo = steps.adGroupSteps().createActiveCpmIndoorAdGroup(campaignInfo);
        steps.bidModifierSteps().createDefaultAdGroupBidModifierDemographics(adGroupInfo);
        CpmIndoorBannerInfo banner = steps.bannerSteps().createActiveCpmIndoorBanner(activeCpmIndoorBanner(
                campaignInfo.getCampaignId(), adGroupInfo.getAdGroupId(), creativeId), adGroupInfo);
        ModerateBannerPage moderateBannerPage = new ModerateBannerPage()
                .withBannerId(banner.getBannerId())
                .withPageId(((CpmIndoorAdGroup) adGroupInfo.getAdGroup()).getPageBlocks().get(0).getPageId())
                .withStatusModerate(StatusModerateBannerPage.YES)
                .withStatusModerateOperator(StatusModerateOperator.NONE)
                .withIsRemoved(false)
                .withCreateTime(LocalDateTime.now());
        steps.moderateBannerPageSteps().createModerateBannerPage(banner, moderateBannerPage);

        steps.bidModifierSteps().createDefaultAdGroupBidModifierDemographics(adGroupInfo);
    }

    protected void createFullCpmIndoorAdGroup() {
        createFullCpmIndoorAdGroup(null);
    }

    protected void createUserProfileCpmAdGroup() {
        adGroupInfo = steps.adGroupSteps().createActiveCpmBannerAdGroup(campaignInfo);
    }

    protected void createKeywordCpmAdGroup() {
        adGroupInfo = steps.adGroupSteps().createActiveCpmBannerAdGroupWithKeywordsCriterionType(campaignInfo);
        steps.bannerSteps().createDefaultBanner(adGroupInfo);
    }

    protected void createRetargetingCondition() {
        createRetargetingCondition(defaultCpmRetCondition());
    }

    protected void createRetargetingCondition(RetargetingCondition retargetingCondition) {
        if (adGroupInfo == null) {
            createUserProfileCpmAdGroup();
        }

        steps.retargetingSteps().createRetargeting(defaultTargetInterest(), adGroupInfo,
                new RetConditionInfo()
                        .withClientInfo(clientInfo)
                        .withRetCondition(retargetingCondition));

        testCryptaSegmentRepository.addAll(
                StreamEx.of(retargetingCondition.collectGoals())
                        .filter(goal -> !goal.getType().isMetrika())
                        .toList());
    }

    protected void createAdjustments(CampaignInfo campaignInfo) {
        //Корректировка типа "Баннер"
        List<BidModifierBannerTypeAdjustment> bannerTypeAdjustments = singletonList(
                createDefaultBannerTypeAdjustment()
                        .withBannerType(BannerType.CPM_BANNER)
                        .withPercent(110)
        );

        steps.bidModifierSteps().createCampaignBidModifier(
                createEmptyBannerTypeModifier()
                        .withCampaignId(campaignInfo.getCampaignId())
                        .withBannerTypeAdjustments(bannerTypeAdjustments),
                campaignInfo);

        //Корректировка типа "Инвентори"
        List<BidModifierInventoryAdjustment> inventoryAdjustments = asList(
                createDefaultInventoryAdjustment()
                        .withInventoryType(InventoryType.INPAGE)
                        .withPercent(120),
                createDefaultInventoryAdjustment()
                        .withInventoryType(InventoryType.INSTREAM_WEB)
                        .withPercent(230),
                createDefaultInventoryAdjustment()
                        .withInventoryType(InventoryType.INAPP)
                        .withPercent(340),
                createDefaultInventoryAdjustment()
                        .withInventoryType(InventoryType.INBANNER)
                        .withPercent(450),
                createDefaultInventoryAdjustment()
                        .withInventoryType(InventoryType.REWARDED)
                        .withPercent(560)
        );

        steps.bidModifierSteps().createCampaignBidModifier(
                createEmptyInventoryModifier()
                        .withCampaignId(campaignInfo.getCampaignId())
                        .withInventoryAdjustments(inventoryAdjustments),
                campaignInfo);
    }

    protected String getSegment(Goal goal) {
        return goal.getKeyword() + ":" + goal.getKeywordValue();
    }

    protected String getSegmentShort(Goal goal) {
        return goal.getKeywordShort() + ":" + goal.getKeywordValueShort();
    }

    @SuppressWarnings("Duplicates")
    protected void setAuthData() {
        DirectWebAuthenticationSourceMock authSource =
                (DirectWebAuthenticationSourceMock) authenticationSource;
        authSource.withOperator(new User()
                .withUid(clientInfo.getUid()));
        authSource.withSubjectUser(new User()
                .withClientId(clientInfo.getClientId())
                .withUid(clientInfo.getUid()));

        UserInfo userInfo = clientInfo.getChiefUserInfo();
        User user = userInfo.getUser();
        SecurityContextHolder.getContext()
                .setAuthentication(new DirectAuthentication(user, user));
    }
}
