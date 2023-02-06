package ru.yandex.direct.core.entity.adgroup.repository;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType;
import ru.yandex.direct.core.entity.adgroup.model.CpmBannerAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmGeoproductAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmIndoorAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmOutdoorAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmVideoAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmYndxFrontpageAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CriterionType;
import ru.yandex.direct.core.entity.adgroup.model.DynamicFeedAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.DynamicTextAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.InternalAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.McBannerAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.MobileContentAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.PerformanceAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.TextAdGroup;
import ru.yandex.direct.core.entity.mobilecontent.model.MobileContent;
import ru.yandex.direct.core.entity.placements.model1.IndoorPlacement;
import ru.yandex.direct.core.entity.placements.model1.OutdoorPlacement;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.FeedInfo;
import ru.yandex.direct.core.testing.steps.AdGroupSteps;
import ru.yandex.direct.core.testing.steps.CampaignSteps;
import ru.yandex.direct.core.testing.steps.Steps;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyFields;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmBannerAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmGeoproductAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmIndoorAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmOutdoorAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmVideoAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmYndxFrontpageAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeDynamicFeedAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeDynamicTextAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeInternalAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeMcBannerAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activePerformanceAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeTextAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.createMobileAppAdGroup;
import static ru.yandex.direct.core.testing.data.TestMobileContents.defaultMobileContent;
import static ru.yandex.direct.core.testing.data.adgroup.TestContentPromotionAdGroups.fullContentPromotionAdGroup;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class GetAdGroupsTest {

    private static final CompareStrategy COMMON_GROUP_COMPARE_STRATEGY = onlyFields(
            newPath("id"), newPath("campaignId"), newPath("type"));

    private static final CompareStrategy MOBILE_APP_GROUP_COMPARE_STRATEGY = onlyFields(
            newPath("id"), newPath("campaignId"), newPath("type"), newPath("mobileContentId"));

    private static final CompareStrategy DYNAMIC_TEXT_GROUP_COMPARE_STRATEGY = onlyFields(
            newPath("id"), newPath("campaignId"), newPath("type"), newPath("mainDomainId"));

    private static final CompareStrategy DYNAMIC_FEED_GROUP_COMPARE_STRATEGY = onlyFields(
            newPath("id"), newPath("campaignId"), newPath("type"), newPath("feedId"));

    private static final CompareStrategy PERFORMANCE_AD_GROUP_COMPARE_STRATEGY = onlyFields(
            newPath("id"), newPath("campaignId"), newPath("type"), newPath("feedId"),
            newPath("fieldToUseAsName"), newPath("fieldToUseAsBody"), newPath("statusBLGenerated"));

    private static final CompareStrategy CPM_BANNER_GROUP_COMPARE_STRATEGY = onlyFields(
            newPath("id"), newPath("campaignId"), newPath("type"), newPath("criterionType"));

    private static final CompareStrategy CPM_GEOPRODUCT_GROUP_COMPARE_STRATEGY = onlyFields(
            newPath("id"), newPath("campaignId"), newPath("type"), newPath("criterionType"));

    private static final CompareStrategy CPM_OUTDOOR_GROUP_COMPARE_STRATEGY = onlyFields(
            newPath("id"), newPath("campaignId"), newPath("type"), newPath("pageBlocks"));

    private static final CompareStrategy CPM_INDOOR_GROUP_COMPARE_STRATEGY = onlyFields(
            newPath("id"), newPath("campaignId"), newPath("type"), newPath("pageBlocks"));

    private static final CompareStrategy INTERNAL_GROUP_COMPARE_STRATEGY = onlyFields(
            newPath("id"), newPath("campaignId"), newPath("type"), newPath("level"));

    private static final CompareStrategy CONTENT_PROMOTION_COMPARE_STRATEGY = onlyFields(
            newPath("id"), newPath("campaignId"), newPath("type"), newPath("contentPromotionType"));

    private int shard;
    private ClientInfo clientInfo;

    private TextAdGroup expectedTextAdGroup;
    private Long textAdGroupId;

    private MobileContentAdGroup expectedMobileAppAdGroup;
    private Long mobileAppAdGroupId;

    private DynamicTextAdGroup expectedDynamicTextAdGroup;
    private Long dynamicTextAdGroupId;

    private DynamicFeedAdGroup expectedDynamicFeedAdGroup;
    private Long dynamicFeedAdGroupId;

    private PerformanceAdGroup expectedPerformanceAdGroup;
    private Long performanceAdGroupId;

    private CpmBannerAdGroup expectedCpmBannerWithUserProfileAdGroup;
    private Long cpmBannerWithUserProfileAdGroupId;

    private CpmBannerAdGroup expectedCpmBannerWithKeywordsAdGroup;
    private Long cpmBannerWithKeywordsAdGroupId;

    private CpmGeoproductAdGroup expectedCpmGeoproductAdGroup;
    private Long cpmGeoproductAdGroupId;

    private CpmBannerAdGroup expectedCpmBannerInDealsAdGroup;
    private Long cpmBannerInDealsAdGroupId;

    private CpmVideoAdGroup expectedCpmVideoAdGroup;
    private Long cpmVideoAdGroupId;

    private CpmOutdoorAdGroup expectedCpmOutdoorAdGroup;
    private Long cpmOutdoorAdGroupId;

    private CpmIndoorAdGroup expectedCpmIndoorAdGroup;
    private Long cpmIndoorAdGroupId;

    private CpmYndxFrontpageAdGroup expectedCpmYndxFrontpageAdGroup;
    private Long cpmYndxFrontpageAdGroupId;

    private ContentPromotionAdGroup expectedContentPromotionVideoAdGroup;
    private Long contentPromotionVideoAdGroupId;

    private ContentPromotionAdGroup expectedContentPromotionAdGroup;
    private Long contentPromotionAdGroupId;

    private InternalAdGroup expectedInternalFreeAdGroup;
    private Long internalFreeAdGroupId;

    private InternalAdGroup expectedInternalDistribAdGroup;
    private Long internalDistribAdGroupId;

    private InternalAdGroup expectedInternalAutobudgetAdGroup;
    private Long internalAutobudgetAdGroupId;

    private McBannerAdGroup expectedMcBannerAdGroup;
    private Long mcBannerAdGroupId;

    @Autowired
    private Steps steps;

    @Autowired
    private AdGroupRepository repository;

    @Before
    public void setUp() {
        clientInfo = steps.clientSteps().createClient(new ClientInfo());

        shard = clientInfo.getShard();

        CampaignSteps campaignSteps = steps.campaignSteps();
        AdGroupSteps adGroupSteps = steps.adGroupSteps();

        // text
        CampaignInfo textCampaign = campaignSteps.createActiveTextCampaign(clientInfo);
        expectedTextAdGroup = activeTextAdGroup(textCampaign.getCampaignId());
        textAdGroupId = adGroupSteps.createAdGroup(expectedTextAdGroup, textCampaign).getAdGroupId();

        // mobile app
        CampaignInfo mobileAppCampaign = campaignSteps.createActiveMobileAppCampaign(clientInfo);
        MobileContent mobileContent = defaultMobileContent();
        expectedMobileAppAdGroup = createMobileAppAdGroup(mobileAppCampaign.getCampaignId(), mobileContent);
        mobileAppAdGroupId = adGroupSteps.createAdGroup(expectedMobileAppAdGroup, mobileAppCampaign).getAdGroupId();

        // dynamic text
        CampaignInfo dynamicCampaign = campaignSteps.createActiveDynamicCampaign(clientInfo);
        expectedDynamicTextAdGroup = activeDynamicTextAdGroup(dynamicCampaign.getCampaignId());
        dynamicTextAdGroupId = adGroupSteps.createAdGroup(expectedDynamicTextAdGroup, dynamicCampaign).getAdGroupId();

        // dynamic feed
        CampaignInfo dynamicCampaign1 = campaignSteps.createActiveDynamicCampaign(clientInfo);
        FeedInfo feedInfo = steps.feedSteps().createDefaultFeed(clientInfo);
        expectedDynamicFeedAdGroup = activeDynamicFeedAdGroup(dynamicCampaign1.getCampaignId(), feedInfo.getFeedId());
        dynamicFeedAdGroupId = adGroupSteps.createAdGroup(expectedDynamicFeedAdGroup, dynamicCampaign1).getAdGroupId();

        // performance
        CampaignInfo performanceCampaign = campaignSteps.createActivePerformanceCampaign(clientInfo);
        expectedPerformanceAdGroup =
                activePerformanceAdGroup(performanceCampaign.getCampaignId(), feedInfo.getFeedId());
        performanceAdGroupId =
                adGroupSteps.createAdGroup(expectedPerformanceAdGroup, performanceCampaign).getAdGroupId();

        // cpm_banner
        CampaignInfo cpmBannerCampaign = campaignSteps.createActiveCpmBannerCampaign(clientInfo);
        expectedCpmBannerWithUserProfileAdGroup = activeCpmBannerAdGroup(cpmBannerCampaign.getCampaignId())
                .withCriterionType(CriterionType.USER_PROFILE);
        expectedCpmBannerWithKeywordsAdGroup = activeCpmBannerAdGroup(cpmBannerCampaign.getCampaignId())
                .withCriterionType(CriterionType.KEYWORD);
        expectedCpmGeoproductAdGroup = activeCpmGeoproductAdGroup(cpmBannerCampaign.getCampaignId());
        expectedCpmVideoAdGroup = activeCpmVideoAdGroup(cpmBannerCampaign.getCampaignId());
        cpmBannerWithUserProfileAdGroupId =
                adGroupSteps.createAdGroup(expectedCpmBannerWithUserProfileAdGroup, cpmBannerCampaign).getAdGroupId();
        cpmBannerWithKeywordsAdGroupId =
                adGroupSteps.createAdGroup(expectedCpmBannerWithKeywordsAdGroup, cpmBannerCampaign).getAdGroupId();
        cpmGeoproductAdGroupId =
                adGroupSteps.createAdGroup(expectedCpmGeoproductAdGroup, cpmBannerCampaign).getAdGroupId();
        cpmVideoAdGroupId = adGroupSteps.createAdGroup(expectedCpmVideoAdGroup, cpmBannerCampaign).getAdGroupId();

        OutdoorPlacement outdoorPlacement = steps.placementSteps().addDefaultOutdoorPlacementWithOneBlock();
        expectedCpmOutdoorAdGroup = activeCpmOutdoorAdGroup(cpmBannerCampaign.getCampaignId(), outdoorPlacement);
        cpmOutdoorAdGroupId = adGroupSteps.createAdGroup(expectedCpmOutdoorAdGroup, cpmBannerCampaign).getAdGroupId();

        IndoorPlacement indoorPlacement = steps.placementSteps().addDefaultIndoorPlacementWithOneBlock();
        expectedCpmIndoorAdGroup = activeCpmIndoorAdGroup(cpmBannerCampaign.getCampaignId(), indoorPlacement);
        cpmIndoorAdGroupId = adGroupSteps.createAdGroup(expectedCpmIndoorAdGroup, cpmBannerCampaign).getAdGroupId();

        // cpm_deals
        CampaignInfo cpmDealsCampaign = campaignSteps.createActiveCpmDealsCampaign(clientInfo);
        expectedCpmBannerInDealsAdGroup = activeCpmBannerAdGroup(cpmBannerCampaign.getCampaignId());
        cpmBannerInDealsAdGroupId =
                adGroupSteps.createAdGroup(expectedCpmBannerInDealsAdGroup, cpmDealsCampaign).getAdGroupId();

        // cpm_yndx_frontpage
        CampaignInfo frontpageCampaign = campaignSteps.createActiveCpmYndxFrontpageCampaign(clientInfo);
        expectedCpmYndxFrontpageAdGroup = activeCpmYndxFrontpageAdGroup(frontpageCampaign.getCampaignId());
        cpmYndxFrontpageAdGroupId =
                adGroupSteps.createAdGroup(expectedCpmYndxFrontpageAdGroup, frontpageCampaign).getAdGroupId();

        // content_promotion_video
        var contentPromotionCampaign = steps.contentPromotionCampaignSteps().createDefaultCampaign(clientInfo);
        expectedContentPromotionVideoAdGroup =
                fullContentPromotionAdGroup(ContentPromotionAdgroupType.VIDEO)
                        .withCampaignId(contentPromotionCampaign.getCampaignId());
        contentPromotionVideoAdGroupId = steps.contentPromotionAdGroupSteps()
                .createAdGroup(contentPromotionCampaign, expectedContentPromotionVideoAdGroup)
                .getAdGroupId();

        // content_promotion
        expectedContentPromotionAdGroup = fullContentPromotionAdGroup(ContentPromotionAdgroupType.COLLECTION)
                .withCampaignId(contentPromotionCampaign.getCampaignId());
        contentPromotionAdGroupId = steps.contentPromotionAdGroupSteps()
                .createAdGroup(contentPromotionCampaign, expectedContentPromotionAdGroup)
                .getAdGroupId();

        // internal for internal_free
        CampaignInfo internalFreeCampaign = campaignSteps.createActiveInternalFreeCampaign(clientInfo);
        expectedInternalFreeAdGroup = activeInternalAdGroup(internalFreeCampaign.getCampaignId());
        internalFreeAdGroupId =
                adGroupSteps.createAdGroup(expectedInternalFreeAdGroup, internalFreeCampaign).getAdGroupId();

        // internal for internal_distrib
        CampaignInfo internalDistribCampaign = campaignSteps.createActiveInternalFreeCampaign(clientInfo);
        expectedInternalDistribAdGroup = activeInternalAdGroup(internalDistribCampaign.getCampaignId());
        internalDistribAdGroupId =
                adGroupSteps.createAdGroup(expectedInternalDistribAdGroup, internalDistribCampaign).getAdGroupId();

        // internal for internal_autobudget
        CampaignInfo internalAutobudgetCampaign = campaignSteps.createActiveInternalAutobudgetCampaign(clientInfo);
        expectedInternalAutobudgetAdGroup = activeInternalAdGroup(internalAutobudgetCampaign.getCampaignId());
        internalAutobudgetAdGroupId =
                adGroupSteps.createAdGroup(expectedInternalAutobudgetAdGroup, internalAutobudgetCampaign)
                        .getAdGroupId();

        // mcbanner
        CampaignInfo mcBannerCampaign = campaignSteps.createActiveMcBannerCampaign(clientInfo);
        expectedMcBannerAdGroup = activeMcBannerAdGroup(mcBannerCampaign.getCampaignId());
        mcBannerAdGroupId =
                adGroupSteps.createAdGroup(expectedMcBannerAdGroup, mcBannerCampaign).getAdGroupId();

    }

    @Test
    public void getTextAdGroup() {
        List<AdGroup> adGroups = repository.getAdGroups(shard, singletonList(textAdGroupId));
        assumeThat("вернулась одна группа", adGroups.size(), is(1));
        AdGroup adGroup = adGroups.get(0);
        assumeThat("вернулась группа правильного типа", adGroup, instanceOf(TextAdGroup.class));
        TextAdGroup textAdGroup = (TextAdGroup) adGroup;
        assertThat("извлеченные данные группы соответствуют ожидаемым", textAdGroup,
                beanDiffer(expectedTextAdGroup).useCompareStrategy(COMMON_GROUP_COMPARE_STRATEGY));
    }

    @Test
    public void getMobileAppAdGroup() {
        List<AdGroup> adGroups = repository.getAdGroups(shard, singletonList(mobileAppAdGroupId));
        assumeThat("вернулась одна группа", adGroups.size(), is(1));
        AdGroup adGroup = adGroups.get(0);
        assumeThat("вернулась группа правильного типа", adGroup, instanceOf(MobileContentAdGroup.class));
        MobileContentAdGroup mobileAppAdGroup = (MobileContentAdGroup) adGroup;
        assertThat("извлеченные данные группы соответствуют ожидаемым", mobileAppAdGroup,
                beanDiffer(expectedMobileAppAdGroup).useCompareStrategy(MOBILE_APP_GROUP_COMPARE_STRATEGY));
    }

    @Test
    public void getDynamicTextAdGroup() {
        List<AdGroup> adGroups = repository.getAdGroups(shard, singletonList(dynamicTextAdGroupId));
        assumeThat("вернулась одна группа", adGroups.size(), is(1));
        AdGroup adGroup = adGroups.get(0);
        assumeThat("вернулась группа правильного типа", adGroup, instanceOf(DynamicTextAdGroup.class));
        DynamicTextAdGroup dynamicTextAdGroup = (DynamicTextAdGroup) adGroup;
        assertThat("извлеченные данные группы соответствуют ожидаемым", dynamicTextAdGroup,
                beanDiffer(expectedDynamicTextAdGroup).useCompareStrategy(DYNAMIC_TEXT_GROUP_COMPARE_STRATEGY));
    }

    @Test
    public void getDynamicFeedAdGroup() {
        List<AdGroup> adGroups = repository.getAdGroups(shard, singletonList(dynamicFeedAdGroupId));
        assumeThat("вернулась одна группа", adGroups.size(), is(1));
        AdGroup adGroup = adGroups.get(0);
        assumeThat("вернулась группа правильного типа", adGroup, instanceOf(DynamicFeedAdGroup.class));
        DynamicFeedAdGroup dynamicFeedAdGroup = (DynamicFeedAdGroup) adGroup;
        assertThat("извлеченные данные группы соответствуют ожидаемым", dynamicFeedAdGroup,
                beanDiffer(expectedDynamicFeedAdGroup).useCompareStrategy(DYNAMIC_FEED_GROUP_COMPARE_STRATEGY));
    }

    @Test
    public void getPerformanceAdGroup() {
        List<AdGroup> adGroups = repository.getAdGroups(shard, singletonList(performanceAdGroupId));
        assumeThat("вернулась одна группа", adGroups.size(), is(1));
        AdGroup adGroup = adGroups.get(0);
        assumeThat("вернулась группа правильного типа", adGroup, instanceOf(PerformanceAdGroup.class));
        PerformanceAdGroup performanceAdGroup = (PerformanceAdGroup) adGroup;
        assertThat("извлеченные данные группы соответствуют ожидаемым", performanceAdGroup,
                beanDiffer(expectedPerformanceAdGroup).useCompareStrategy(PERFORMANCE_AD_GROUP_COMPARE_STRATEGY));
    }

    @Test
    public void getCpmBannerWithUserProfileAdGroup() {
        List<AdGroup> adGroups = repository.getAdGroups(shard, singletonList(cpmBannerWithUserProfileAdGroupId));
        assumeThat("вернулась одна группа", adGroups.size(), is(1));
        AdGroup adGroup = adGroups.get(0);
        assumeThat("вернулась группа правильного типа", adGroup, instanceOf(CpmBannerAdGroup.class));
        CpmBannerAdGroup cpmBannerAdGroup = (CpmBannerAdGroup) adGroup;
        assertThat("извлеченные данные группы соответствуют ожидаемым", cpmBannerAdGroup,
                beanDiffer(expectedCpmBannerWithUserProfileAdGroup)
                        .useCompareStrategy(CPM_BANNER_GROUP_COMPARE_STRATEGY));
    }

    @Test
    public void getCpmBannerWithKeywordsAdGroup() {
        List<AdGroup> adGroups = repository.getAdGroups(shard, singletonList(cpmBannerWithKeywordsAdGroupId));
        assumeThat("вернулась одна группа", adGroups.size(), is(1));
        AdGroup adGroup = adGroups.get(0);
        assumeThat("вернулась группа правильного типа", adGroup, instanceOf(CpmBannerAdGroup.class));
        CpmBannerAdGroup cpmBannerAdGroup = (CpmBannerAdGroup) adGroup;
        assertThat("извлеченные данные группы соответствуют ожидаемым", cpmBannerAdGroup,
                beanDiffer(expectedCpmBannerWithKeywordsAdGroup).useCompareStrategy(CPM_BANNER_GROUP_COMPARE_STRATEGY));
    }

    @Test
    public void getCpmGeoproductAdGroup() {
        List<AdGroup> adGroups = repository.getAdGroups(shard, singletonList(cpmGeoproductAdGroupId));
        assumeThat("вернулась одна группа", adGroups.size(), is(1));
        AdGroup adGroup = adGroups.get(0);
        assumeThat("вернулась группа правильного типа", adGroup, instanceOf(CpmGeoproductAdGroup.class));
        CpmGeoproductAdGroup cpmGeoproductAdGroup = (CpmGeoproductAdGroup) adGroup;
        assertThat("извлеченные данные группы соответствуют ожидаемым", cpmGeoproductAdGroup,
                beanDiffer(expectedCpmGeoproductAdGroup)
                        .useCompareStrategy(CPM_GEOPRODUCT_GROUP_COMPARE_STRATEGY));
    }

    @Test
    public void getCpmBannerInDealsAdGroup() {
        List<AdGroup> adGroups = repository.getAdGroups(shard, singletonList(cpmBannerInDealsAdGroupId));
        assumeThat("вернулась одна группа", adGroups.size(), is(1));
        AdGroup adGroup = adGroups.get(0);
        assumeThat("вернулась группа правильного типа", adGroup, instanceOf(CpmBannerAdGroup.class));
        CpmBannerAdGroup cpmBannerInDealsAdGroup = (CpmBannerAdGroup) adGroup;
        assertThat("извлеченные данные группы соответствуют ожидаемым", cpmBannerInDealsAdGroup,
                beanDiffer(expectedCpmBannerInDealsAdGroup).useCompareStrategy(COMMON_GROUP_COMPARE_STRATEGY));
    }

    @Test
    public void getCpmVideoAdGroup() {
        List<AdGroup> adGroups = repository.getAdGroups(shard, singletonList(cpmVideoAdGroupId));
        assumeThat("вернулась одна группа", adGroups.size(), is(1));
        AdGroup adGroup = adGroups.get(0);
        assumeThat("вернулась группа правильного типа", adGroup, instanceOf(CpmVideoAdGroup.class));
        CpmVideoAdGroup cpmVideoAdGroup = (CpmVideoAdGroup) adGroup;
        assertThat("извлеченные данные группы соответствуют ожидаемым", cpmVideoAdGroup,
                beanDiffer(expectedCpmVideoAdGroup).useCompareStrategy(COMMON_GROUP_COMPARE_STRATEGY));
    }

    @Test
    public void getCpmYndxFrontpageAdGroup() {
        List<AdGroup> adGroups = repository.getAdGroups(shard, singletonList(cpmYndxFrontpageAdGroupId));
        assumeThat("вернулась одна группа", adGroups.size(), is(1));
        AdGroup adGroup = adGroups.get(0);
        assumeThat("вернулась группа правильного типа", adGroup, instanceOf(CpmYndxFrontpageAdGroup.class));
        CpmYndxFrontpageAdGroup cpmYndxFrontpageAdGroup = (CpmYndxFrontpageAdGroup) adGroup;
        assertThat("извлеченные данные группы соответствуют ожидаемым", cpmYndxFrontpageAdGroup,
                beanDiffer(expectedCpmYndxFrontpageAdGroup).useCompareStrategy(COMMON_GROUP_COMPARE_STRATEGY));
    }

    @Test
    public void getContentPromotionVideoAdGroup() {
        List<AdGroup> adGroups = repository.getAdGroups(shard, singletonList(contentPromotionVideoAdGroupId));
        assumeThat("вернулась одна группа", adGroups.size(), is(1));
        AdGroup adGroup = adGroups.get(0);
        assumeThat("вернулась группа правильного типа", adGroup, instanceOf(ContentPromotionAdGroup.class));
        ContentPromotionAdGroup contentVideoAdGroup = (ContentPromotionAdGroup) adGroup;
        assertThat("извлеченные данные группы соответствуют ожидаемым", contentVideoAdGroup,
                beanDiffer(expectedContentPromotionVideoAdGroup).useCompareStrategy(COMMON_GROUP_COMPARE_STRATEGY));
    }

    @Test
    public void getContentPromotionAdGroup() {
        List<AdGroup> adGroups = repository.getAdGroups(shard, singletonList(contentPromotionAdGroupId));
        assumeThat("вернулась одна группа", adGroups.size(), is(1));
        AdGroup adGroup = adGroups.get(0);
        assumeThat("вернулась группа правильного типа", adGroup, instanceOf(ContentPromotionAdGroup.class));
        ContentPromotionAdGroup contentPromotionAdGroup = (ContentPromotionAdGroup) adGroup;
        assertThat("извлеченные данные группы соответствуют ожидаемым", contentPromotionAdGroup,
                beanDiffer(expectedContentPromotionAdGroup).useCompareStrategy(CONTENT_PROMOTION_COMPARE_STRATEGY));
    }

    @Test
    public void getCpmOutdoorAdGroup() {
        List<AdGroup> adGroups = repository.getAdGroups(shard, singletonList(cpmOutdoorAdGroupId));
        assumeThat("вернулась одна группа", adGroups.size(), is(1));
        AdGroup adGroup = adGroups.get(0);
        assumeThat("вернулась группа правильного типа", adGroup, instanceOf(CpmOutdoorAdGroup.class));
        CpmOutdoorAdGroup cpmOutdoorAdGroup = (CpmOutdoorAdGroup) adGroup;
        assertThat("извлеченные данные группы соответствуют ожидаемым", cpmOutdoorAdGroup,
                beanDiffer(expectedCpmOutdoorAdGroup).useCompareStrategy(CPM_OUTDOOR_GROUP_COMPARE_STRATEGY));
    }

    @Test
    public void getCpmOutdoorAdGroupWithNullPageBlocks() {
        OutdoorPlacement placement = steps.placementSteps().addDefaultOutdoorPlacementWithOneBlock();
        CampaignInfo cpmBannerCampaign = steps.campaignSteps().createActiveCpmBannerCampaign(clientInfo);
        CpmOutdoorAdGroup expectedAdGroup = activeCpmOutdoorAdGroup(cpmBannerCampaign.getCampaignId(), placement)
                .withPageBlocks(null);
        Long adGroupId = steps.adGroupSteps().createAdGroup(expectedAdGroup, cpmBannerCampaign).getAdGroupId();

        List<AdGroup> adGroups = repository.getAdGroups(shard, singletonList(adGroupId));
        assumeThat("вернулась одна группа", adGroups.size(), is(1));
        AdGroup adGroup = adGroups.get(0);
        assumeThat("вернулась группа правильного типа", adGroup, instanceOf(CpmOutdoorAdGroup.class));
        CpmOutdoorAdGroup cpmOutdoorAdGroup = (CpmOutdoorAdGroup) adGroup;
        assertThat("извлеченные данные группы соответствуют ожидаемым", cpmOutdoorAdGroup,
                beanDiffer(expectedAdGroup).useCompareStrategy(CPM_OUTDOOR_GROUP_COMPARE_STRATEGY));
    }

    @Test
    public void getCpmIndoorAdGroup() {
        List<AdGroup> adGroups = repository.getAdGroups(shard, singletonList(cpmIndoorAdGroupId));
        assumeThat("вернулась одна группа", adGroups.size(), is(1));
        AdGroup adGroup = adGroups.get(0);
        assumeThat("вернулась группа правильного типа", adGroup, instanceOf(CpmIndoorAdGroup.class));
        CpmIndoorAdGroup cpmIndoorAdGroup = (CpmIndoorAdGroup) adGroup;
        assertThat("извлеченные данные группы соответствуют ожидаемым", cpmIndoorAdGroup,
                beanDiffer(expectedCpmIndoorAdGroup).useCompareStrategy(CPM_INDOOR_GROUP_COMPARE_STRATEGY));
    }

    @Test
    public void getCpmIndoorAdGroupWithNullPageBlocks() {
        IndoorPlacement placement = steps.placementSteps().addDefaultIndoorPlacementWithOneBlock();
        CampaignInfo cpmBannerCampaign = steps.campaignSteps().createActiveCpmBannerCampaign(clientInfo);
        CpmIndoorAdGroup expectedAdGroup = activeCpmIndoorAdGroup(cpmBannerCampaign.getCampaignId(), placement)
                .withPageBlocks(null);
        Long adGroupId = steps.adGroupSteps().createAdGroup(expectedAdGroup, cpmBannerCampaign).getAdGroupId();

        List<AdGroup> adGroups = repository.getAdGroups(shard, singletonList(adGroupId));
        assumeThat("вернулась одна группа", adGroups.size(), is(1));
        AdGroup adGroup = adGroups.get(0);
        assumeThat("вернулась группа правильного типа", adGroup, instanceOf(CpmIndoorAdGroup.class));
        CpmIndoorAdGroup cpmIndoorAdGroup = (CpmIndoorAdGroup) adGroup;
        assertThat("извлеченные данные группы соответствуют ожидаемым", cpmIndoorAdGroup,
                beanDiffer(expectedAdGroup).useCompareStrategy(CPM_INDOOR_GROUP_COMPARE_STRATEGY));
    }

    @Test
    public void getInternalFreeAdGroup() {
        List<AdGroup> adGroups = repository.getAdGroups(shard, singletonList(internalFreeAdGroupId));
        assumeThat("вернулась одна группа", adGroups.size(), is(1));
        AdGroup adGroup = adGroups.get(0);
        assumeThat("вернулась группа правильного типа", adGroup, instanceOf(InternalAdGroup.class));
        InternalAdGroup internalAdGroup = (InternalAdGroup) adGroup;
        assertThat("извлеченные данные группы соответствуют ожидаемым", internalAdGroup,
                beanDiffer(expectedInternalFreeAdGroup).useCompareStrategy(INTERNAL_GROUP_COMPARE_STRATEGY));
    }

    @Test
    public void getInternalDistribAdGroup() {
        List<AdGroup> adGroups = repository.getAdGroups(shard, singletonList(internalDistribAdGroupId));
        assumeThat("вернулась одна группа", adGroups.size(), is(1));
        AdGroup adGroup = adGroups.get(0);
        assumeThat("вернулась группа правильного типа", adGroup, instanceOf(InternalAdGroup.class));
        InternalAdGroup internalAdGroup = (InternalAdGroup) adGroup;
        assertThat("извлеченные данные группы соответствуют ожидаемым", internalAdGroup,
                beanDiffer(expectedInternalDistribAdGroup).useCompareStrategy(INTERNAL_GROUP_COMPARE_STRATEGY));
    }

    @Test
    public void getInternalAutobudgetAdGroup() {
        List<AdGroup> adGroups = repository.getAdGroups(shard, singletonList(internalAutobudgetAdGroupId));
        assumeThat("вернулась одна группа", adGroups.size(), is(1));
        AdGroup adGroup = adGroups.get(0);
        assumeThat("вернулась группа правильного типа", adGroup, instanceOf(InternalAdGroup.class));
        InternalAdGroup internalAdGroup = (InternalAdGroup) adGroup;
        assertThat("извлеченные данные группы соответствуют ожидаемым", internalAdGroup,
                beanDiffer(expectedInternalAutobudgetAdGroup).useCompareStrategy(INTERNAL_GROUP_COMPARE_STRATEGY));
    }

    @Test
    public void getOnlyInternalAdGroups() {
        List<Long> adGroupIds =
                Arrays.asList(internalFreeAdGroupId, internalDistribAdGroupId, internalAutobudgetAdGroupId);
        List<AdGroup> adGroups = repository.getAdGroups(shard, adGroupIds);
        assumeThat(String.format("вернулось %d групп", adGroupIds.size()), adGroups.size(), is(adGroupIds.size()));
        assertThat("извлеченные данные групп соответствуют ожидаемым", adGroups,
                beanDiffer(Arrays.asList(expectedInternalFreeAdGroup, expectedInternalDistribAdGroup,
                        expectedInternalAutobudgetAdGroup).stream().map(e -> (AdGroup) e).collect(Collectors.toList()))
                        .useCompareStrategy(COMMON_GROUP_COMPARE_STRATEGY));
    }

    @Test
    public void getMcBannerAdGroup() {
        List<AdGroup> adGroups = repository.getAdGroups(shard, singletonList(mcBannerAdGroupId));
        assumeThat("вернулась одна группа", adGroups.size(), is(1));
        AdGroup adGroup = adGroups.get(0);
        assumeThat("вернулась группа правильного типа", adGroup, instanceOf(McBannerAdGroup.class));
        McBannerAdGroup mcBannerAdGroup = (McBannerAdGroup) adGroup;
        assertThat("извлеченные данные группы соответствуют ожидаемым", mcBannerAdGroup,
                beanDiffer(expectedMcBannerAdGroup).useCompareStrategy(COMMON_GROUP_COMPARE_STRATEGY));
    }

    @Test
    public void getAdGroupsOfAllTypes() {
        List<Long> adGroupIds = Arrays.asList(textAdGroupId, mobileAppAdGroupId, dynamicTextAdGroupId,
                dynamicFeedAdGroupId, performanceAdGroupId, cpmBannerWithUserProfileAdGroupId,
                cpmBannerWithKeywordsAdGroupId, cpmBannerInDealsAdGroupId, cpmVideoAdGroupId, cpmOutdoorAdGroupId,
                cpmYndxFrontpageAdGroupId, contentPromotionVideoAdGroupId, contentPromotionAdGroupId,
                internalFreeAdGroupId, internalDistribAdGroupId, internalAutobudgetAdGroupId,
                mcBannerAdGroupId);
        List<AdGroup> adGroups = repository.getAdGroups(shard, adGroupIds);
        assumeThat(String.format("вернулось %d групп", adGroupIds.size()), adGroups.size(), is(adGroupIds.size()));
        assertThat("извлеченные данные групп соответствуют ожидаемым", adGroups,
                beanDiffer(Arrays.asList(expectedTextAdGroup, expectedMobileAppAdGroup, expectedDynamicTextAdGroup,
                        expectedDynamicFeedAdGroup, expectedPerformanceAdGroup, expectedCpmBannerWithUserProfileAdGroup,
                        expectedCpmBannerWithKeywordsAdGroup, expectedCpmBannerInDealsAdGroup, expectedCpmVideoAdGroup,
                        expectedCpmOutdoorAdGroup, expectedCpmYndxFrontpageAdGroup,
                        expectedContentPromotionVideoAdGroup, expectedContentPromotionAdGroup,
                        expectedInternalFreeAdGroup, expectedInternalDistribAdGroup, expectedInternalAutobudgetAdGroup,
                        expectedMcBannerAdGroup))
                        .useCompareStrategy(COMMON_GROUP_COMPARE_STRATEGY));
    }
}
