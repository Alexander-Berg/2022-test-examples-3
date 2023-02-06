package ru.yandex.direct.core.entity.adgroup.repository;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import one.util.streamex.StreamEx;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType;
import ru.yandex.direct.core.entity.adgroup.model.CpmIndoorAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmOutdoorAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.DynSmartAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.DynamicAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.DynamicFeedAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.DynamicTextAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.InternalAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.MobileContentAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.MobileContentAdGroupDeviceTypeTargeting;
import ru.yandex.direct.core.entity.adgroup.model.MobileContentAdGroupNetworkTargeting;
import ru.yandex.direct.core.entity.adgroup.model.PageBlock;
import ru.yandex.direct.core.entity.adgroup.model.PerformanceAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.StatusBLGenerated;
import ru.yandex.direct.core.entity.adgroup.model.StatusModerate;
import ru.yandex.direct.core.entity.adgroup.model.StatusPostModerate;
import ru.yandex.direct.core.entity.adgroup.model.StatusShowsForecast;
import ru.yandex.direct.core.entity.adgroup.model.TextAdGroup;
import ru.yandex.direct.core.entity.domain.service.DomainService;
import ru.yandex.direct.core.entity.minuskeywordspack.model.MinusKeywordsPack;
import ru.yandex.direct.core.entity.placements.model1.IndoorPlacement;
import ru.yandex.direct.core.entity.placements.model1.OutdoorPlacement;
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatchCategory;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestGroups;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.FeedInfo;
import ru.yandex.direct.core.testing.info.PerformanceAdGroupInfo;
import ru.yandex.direct.core.testing.info.adgroup.ContentPromotionAdGroupInfo;
import ru.yandex.direct.core.testing.steps.FeedSteps;
import ru.yandex.direct.core.testing.steps.PlacementSteps;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.regions.Region;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.entity.adgroup.repository.typesupport.InternalAdGroupSupport.DEFAULT_LEVEL;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmIndoorAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmOutdoorAdGroup;
import static ru.yandex.direct.core.testing.data.TestMinusKeywordsPacks.privateMinusKeywordsPack;
import static ru.yandex.direct.core.testing.data.adgroup.TestContentPromotionAdGroups.fullContentPromotionAdGroup;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@CoreTest
@RunWith(SpringRunner.class)
public class AdGroupRepositoryUpdateTest {
    private static final String[] COMMON_COMPARE_PROPERTIES = new String[]{
            "Name", "Geo", "MinusKeywords", "StatusBsSynced", "StatusModerate",
            "StatusPostModerate", "StatusShowsForecast", "TrackingParams", "ForecastDate",
            "LastChange", "PageGroupTags", "TargetTags"
    };
    public static final String[] TEXT_COMPARE_PROPERTIES = COMMON_COMPARE_PROPERTIES;
    private static final String[] MOBILE_CONTENT_COMPARE_PROPERTIES =
            StreamEx.of(COMMON_COMPARE_PROPERTIES)
                    .append("DeviceTypeTargeting", "NetworkTargeting", "MinimalOperatingSystemVersion")
                    .append()
                    .toArray(String[]::new);
    private static final String[] DYN_SMART_COMPARE_PROPERTIES =
            StreamEx.of(COMMON_COMPARE_PROPERTIES)
                    .append("StatusBLGenerated")
                    .append("FieldToUseAsName")
                    .append("FieldToUseAsBody")
                    .toArray(String[]::new);
    private static final String[] TEXT_WITH_FEED_COMPARE_PROPERTIES = DYN_SMART_COMPARE_PROPERTIES;
    private static final String[] DYNAMIC_COMPARE_PROPERTIES =
            StreamEx.of(DYN_SMART_COMPARE_PROPERTIES)
                    .append("RelevanceMatchCategories")
                    .append()
                    .toArray(String[]::new);
    private static final String[] DYNAMIC_TEXT_COMPARE_PROPERTIES =
            StreamEx.of(DYNAMIC_COMPARE_PROPERTIES)
                    .append("MainDomainId")
                    .toArray(String[]::new);
    private static final String[] CPM_OUTDOOR_COMPARE_PROPERTIES =
            StreamEx.of(COMMON_COMPARE_PROPERTIES)
                    .append(CpmOutdoorAdGroup.PAGE_BLOCKS.name())
                    .toArray(String[]::new);
    private static final String[] CPM_INDOOR_COMPARE_PROPERTIES =
            StreamEx.of(COMMON_COMPARE_PROPERTIES)
                    .append(CpmIndoorAdGroup.PAGE_BLOCKS.name())
                    .toArray(String[]::new);
    private static final String[] INTERNAL_COMPARE_PROPERTIES =
            StreamEx.of(COMMON_COMPARE_PROPERTIES)
                    .append(InternalAdGroup.LEVEL.name())
                    .toArray(String[]::new);

    private static final String OLD_NAME = "old_name";
    private static final String NEW_NAME = "new_name";

    private static final List<Long> OLD_GEO = singletonList(Region.MOSCOW_REGION_ID);
    private static final List<Long> NEW_GEO = singletonList(Region.CRIMEA_REGION_ID);

    private static final List<String> OLD_MINUS_KEYWORDS = singletonList("abc");
    private static final List<String> NEW_MINUS_KEYWORDS = singletonList("efh");

    private static final StatusBsSynced OLD_BS_SYNC = StatusBsSynced.NO;
    private static final StatusBsSynced NEW_BS_SYNC = StatusBsSynced.YES;

    private static final StatusModerate OLD_STATUS_MODERATE = StatusModerate.NO;
    private static final StatusModerate NEW_STATUS_MODERATE = StatusModerate.YES;

    private static final StatusPostModerate OLD_STATUS_POST_MODERATE = StatusPostModerate.NO;
    private static final StatusPostModerate NEW_STATUS_POST_MODERATE = StatusPostModerate.YES;

    private static final String OLD_TRACKING_PARAMS = "a";
    private static final String NEW_TRACKING_PARAMS = "b";

    private static final Boolean OLD_STATUS_AUTOBUDGET_SHOW = false;
    private static final Boolean NEW_STATUS_AUTOBUDGET_SHOW = true;

    private static final StatusShowsForecast OLD_STATUS_SHOWS_FORECAST = StatusShowsForecast.ARCHIVED;
    private static final StatusShowsForecast NEW_STATUS_SHOWS_FORECAST = StatusShowsForecast.NEW;

    private static final LocalDateTime OLD_FORECAST_DATE = LocalDateTime.of(2018, 1, 1, 1, 1);
    private static final LocalDateTime NEW_FORECAST_DATE = LocalDateTime.of(2018, 1, 2, 1, 1);

    private static final LocalDateTime OLD_LAST_CHANGE = LocalDateTime.of(2018, 1, 3, 1, 1);
    private static final LocalDateTime NEW_LAST_CHANGE = LocalDateTime.of(2018, 1, 4, 1, 1);

    private static final List<String> OLD_PAGE_GROUP_TAGS = singletonList("old_page_group_tag");
    private static final List<String> NEW_PAGE_GROUP_TAGS = singletonList("new_page_group_tag");

    private static final List<String> OLD_TARGET_TAGS = singletonList("old_target_tag");
    private static final List<String> NEW_TARGET_TAGS = singletonList("new_target_tag");

    private static final Set<MobileContentAdGroupDeviceTypeTargeting> OLD_DEVICE_TYPE_TARGETING =
            EnumSet.of(MobileContentAdGroupDeviceTypeTargeting.PHONE);
    private static final Set<MobileContentAdGroupDeviceTypeTargeting> NEW_DEVICE_TYPE_TARGETING =
            EnumSet.of(MobileContentAdGroupDeviceTypeTargeting.TABLET);

    private static final Set<MobileContentAdGroupNetworkTargeting> OLD_NETWORK_TARGETING =
            EnumSet.of(MobileContentAdGroupNetworkTargeting.CELLULAR);
    private static final Set<MobileContentAdGroupNetworkTargeting> NEW_NETWORK_TARGETING =
            EnumSet.of(MobileContentAdGroupNetworkTargeting.WI_FI);

    private static final String OLD_MIN_OS_VERSION = "1.0";
    private static final String NEW_MIN_OS_VERSION = "2.0";

    private static final String OLD_DOMAIN_URL = "ya1.ru";
    private static final String NEW_DOMAIN_URL = "ya2.ru";

    private static final StatusBLGenerated OLD_STATUS_BL_GENERATED = StatusBLGenerated.NO;
    private static final StatusBLGenerated NEW_STATUS_BL_GENERATED = StatusBLGenerated.YES;

    public static final String OLD_FIELD_TO_USE_AS_NAME = "name1";
    public static final String NEW_FIELD_TO_USE_AS_NAME = "name2";

    public static final String OLD_FIELD_TO_USE_AS_BODY = "body1";
    public static final String NEW_FIELD_TO_USE_AS_BODY = "body2";

    private static final Set<RelevanceMatchCategory> OLD_RELEVANCE_MATCH_CATEGORIES =
            EnumSet.of(RelevanceMatchCategory.exact_mark, RelevanceMatchCategory.competitor_mark);
    private static final Set<RelevanceMatchCategory> NEW_RELEVANCE_MATCH_CATEGORIES =
            EnumSet.of(RelevanceMatchCategory.exact_mark);

    @Autowired
    private Steps steps;
    @Autowired
    public PlacementSteps placementSteps;
    @Autowired
    public FeedSteps feedSteps;
    @Autowired
    public DslContextProvider dslContextProvider;
    @Autowired
    public DomainService domainService;
    @Autowired
    public AdGroupRepository adGroupRepository;

    private ClientInfo clientInfo;
    private MinusKeywordsPack oldPack;
    private MinusKeywordsPack newPack;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        oldPack = steps.minusKeywordsPackSteps().createMinusKeywordsPack(
                privateMinusKeywordsPack().withMinusKeywords(OLD_MINUS_KEYWORDS), clientInfo).getMinusKeywordsPack();
        newPack = steps.minusKeywordsPackSteps().createMinusKeywordsPack(
                privateMinusKeywordsPack().withMinusKeywords(NEW_MINUS_KEYWORDS), clientInfo).getMinusKeywordsPack();
    }

    @SuppressWarnings("unchecked")
    private <A extends AdGroup> A applyOldValues(A adGroup) {
        return (A) adGroup.withName(OLD_NAME)
                .withGeo(OLD_GEO)
                .withMinusKeywordsId(oldPack.getId())
                .withMinusKeywords(OLD_MINUS_KEYWORDS)
                .withStatusBsSynced(OLD_BS_SYNC)
                .withStatusModerate(OLD_STATUS_MODERATE)
                .withStatusPostModerate(OLD_STATUS_POST_MODERATE)
                .withStatusAutobudgetShow(OLD_STATUS_AUTOBUDGET_SHOW)
                .withTrackingParams(OLD_TRACKING_PARAMS)
                .withStatusShowsForecast(OLD_STATUS_SHOWS_FORECAST)
                .withForecastDate(OLD_FORECAST_DATE)
                .withLastChange(OLD_LAST_CHANGE)
                .withPageGroupTags(OLD_PAGE_GROUP_TAGS)
                .withTargetTags(OLD_TARGET_TAGS);
    }

    private <T extends AdGroup> ModelChanges<T> applyNewValues(T adGroup, Class<T> clazz) {
        return new ModelChanges<>(adGroup.getId(), clazz)
                .process(NEW_NAME, AdGroup.NAME)
                .process(NEW_GEO, AdGroup.GEO)
                .process(newPack.getId(), AdGroup.MINUS_KEYWORDS_ID)
                .process(NEW_BS_SYNC, AdGroup.STATUS_BS_SYNCED)
                .process(NEW_STATUS_MODERATE, AdGroup.STATUS_MODERATE)
                .process(NEW_STATUS_POST_MODERATE, AdGroup.STATUS_POST_MODERATE)
                .process(NEW_STATUS_AUTOBUDGET_SHOW, AdGroup.STATUS_AUTOBUDGET_SHOW)
                .process(NEW_TRACKING_PARAMS, AdGroup.TRACKING_PARAMS)
                .process(NEW_STATUS_SHOWS_FORECAST, AdGroup.STATUS_SHOWS_FORECAST)
                .process(NEW_FORECAST_DATE, AdGroup.FORECAST_DATE)
                .process(NEW_LAST_CHANGE, AdGroup.LAST_CHANGE);
    }

    private TextAdGroup applyTextWithFeedOldValues(TextAdGroup adGroup) {
        applyDynSmartOldValues(adGroup);
        return adGroup;
    }

    private ModelChanges<TextAdGroup> applyTextWithFeedNewValues(TextAdGroup adGroup) {
        return applyDynSmartNewValues(adGroup, TextAdGroup.class);
    }

    private MobileContentAdGroup applyMobileContentOldValues(MobileContentAdGroup adGroup) {
        applyOldValues(adGroup);
        return adGroup
                .withDeviceTypeTargeting(OLD_DEVICE_TYPE_TARGETING)
                .withNetworkTargeting(OLD_NETWORK_TARGETING)
                .withMinimalOperatingSystemVersion(OLD_MIN_OS_VERSION);
    }

    private ModelChanges<MobileContentAdGroup> applyMobileContentNewValues(
            MobileContentAdGroup adGroup, Class<MobileContentAdGroup> clazz) {
        return applyNewValues(adGroup, clazz)
                .process(NEW_DEVICE_TYPE_TARGETING, MobileContentAdGroup.DEVICE_TYPE_TARGETING)
                .process(NEW_NETWORK_TARGETING, MobileContentAdGroup.NETWORK_TARGETING)
                .process(NEW_MIN_OS_VERSION, MobileContentAdGroup.MINIMAL_OPERATING_SYSTEM_VERSION);
    }

    private ModelChanges<InternalAdGroup> applyInternalNewValues(InternalAdGroup adGroup,
                                                                 Class<InternalAdGroup> clazz, Long level) {
        return applyNewValues(adGroup, clazz)
                .process(level, InternalAdGroup.LEVEL);
    }

    private DynamicAdGroup applyDynamicOldValues(DynamicAdGroup adGroup) {
        applyDynSmartOldValues(adGroup);
        return adGroup
                .withRelevanceMatchCategories(OLD_RELEVANCE_MATCH_CATEGORIES);
    }

    private <T extends DynamicAdGroup> ModelChanges<T> applyDynamicNewValues(T adGroup, Class<T> clazz) {
        return applyDynSmartNewValues(adGroup, clazz)
                .process(NEW_RELEVANCE_MATCH_CATEGORIES, DynamicTextAdGroup.RELEVANCE_MATCH_CATEGORIES);
    }

    private DynSmartAdGroup applyDynSmartOldValues(DynSmartAdGroup adGroup) {
        applyOldValues(adGroup);
        return adGroup
                .withStatusBLGenerated(OLD_STATUS_BL_GENERATED)
                .withFieldToUseAsName(OLD_FIELD_TO_USE_AS_NAME)
                .withFieldToUseAsBody(OLD_FIELD_TO_USE_AS_BODY);
    }

    private <T extends DynSmartAdGroup> ModelChanges<T> applyDynSmartNewValues(T adGroup, Class<T> clazz) {
        return applyNewValues(adGroup, clazz)
                .process(NEW_STATUS_BL_GENERATED, DynSmartAdGroup.STATUS_B_L_GENERATED)
                .process(NEW_FIELD_TO_USE_AS_NAME, DynSmartAdGroup.FIELD_TO_USE_AS_NAME)
                .process(NEW_FIELD_TO_USE_AS_BODY, DynSmartAdGroup.FIELD_TO_USE_AS_BODY);
    }

    private Long getOrCreateDomain(int shard, String domain) {
        return domainService.getOrCreate(dslContextProvider.ppc(shard), singletonList(domain))
                .get(0);
    }

    private DynamicTextAdGroup applyDynamicTextOldValues(int shard, DynamicTextAdGroup adGroup) {
        applyDynamicOldValues(adGroup);
        return adGroup
                .withMainDomainId(getOrCreateDomain(shard, OLD_DOMAIN_URL));
    }

    private ModelChanges<DynamicTextAdGroup> applyDynamicTextNewValues(
            int shard, DynamicTextAdGroup adGroup, Class<DynamicTextAdGroup> clazz) {
        return applyDynamicNewValues(adGroup, clazz)
                .process(getOrCreateDomain(shard, NEW_DOMAIN_URL), DynamicTextAdGroup.MAIN_DOMAIN_ID);
    }

    private ModelChanges<AdGroup> applyPageGroupAndTargetTagsNewValues(AdGroup adGroup, Class<AdGroup> clazz) {
        return applyNewValues(adGroup, clazz)
                .process(NEW_PAGE_GROUP_TAGS, AdGroup.PAGE_GROUP_TAGS)
                .process(NEW_TARGET_TAGS, AdGroup.TARGET_TAGS);
    }


    private AdGroup fillExpectedMinusWords(AdGroup adGroup) {
        return adGroup.withMinusKeywords(NEW_MINUS_KEYWORDS);
    }

    @Test
    public void testText() {
        AdGroupInfo adGroup = steps.adGroupSteps().createAdGroup(applyOldValues(TestGroups.activeTextAdGroup()),
                clientInfo);

        AdGroup addedAdGroup = adGroupRepository.getAdGroups(
                adGroup.getShard(), singletonList(adGroup.getAdGroupId())).get(0);

        AppliedChanges<AdGroup> expectedAdGroup = applyNewValues(addedAdGroup, AdGroup.class)
                .applyTo(addedAdGroup);

        adGroupRepository.updateAdGroups(
                adGroup.getShard(), adGroup.getClientId(), singletonList(expectedAdGroup));

        AdGroup actualAdGroup = adGroupRepository.getAdGroups(
                        adGroup.getShard(), singletonList(adGroup.getAdGroupId()))
                .get(0);

        assertThat(actualAdGroup)
                .isEqualToComparingOnlyGivenFields(
                        fillExpectedMinusWords(expectedAdGroup.getModel()), TEXT_COMPARE_PROPERTIES);
    }

    @Test
    public void testTextWithFeed() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveTextCampaign(clientInfo);
        FeedInfo feedInfo = feedSteps.createDefaultFeed(campaignInfo.getClientInfo());
        AdGroupInfo adGroup = steps.adGroupSteps().createAdGroup(applyTextWithFeedOldValues(
                TestGroups.activeTextAdGroup(campaignInfo.getCampaignId(), feedInfo.getFeedId(), null)));

        TextAdGroup addedAdGroup = (TextAdGroup) adGroupRepository.getAdGroups(
                adGroup.getShard(), singletonList(adGroup.getAdGroupId())).get(0);

        AppliedChanges<AdGroup> expectedAdGroup = applyTextWithFeedNewValues(addedAdGroup)
                .applyTo(addedAdGroup)
                .castModelUp(AdGroup.class);

        adGroupRepository.updateAdGroups(
                adGroup.getShard(), adGroup.getClientId(), singletonList(expectedAdGroup));

        AdGroup actualAdGroup = adGroupRepository.getAdGroups(
                        adGroup.getShard(), singletonList(adGroup.getAdGroupId()))
                .get(0);

        assertThat(actualAdGroup)
                .isEqualToComparingOnlyGivenFields(
                        fillExpectedMinusWords(expectedAdGroup.getModel()), TEXT_WITH_FEED_COMPARE_PROPERTIES);
    }

    @Test
    public void testMobileContent() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveDynamicCampaign(clientInfo);
        AdGroupInfo adGroup = steps.adGroupSteps().createAdGroup(
                applyMobileContentOldValues(
                        TestGroups.activeMobileAppAdGroup(campaignInfo.getCampaignId())), campaignInfo);

        MobileContentAdGroup addedAdGroup = (MobileContentAdGroup) adGroupRepository.getAdGroups(
                adGroup.getShard(), singletonList(adGroup.getAdGroupId())).get(0);

        AppliedChanges<AdGroup> expectedAdGroup = applyMobileContentNewValues(
                addedAdGroup, MobileContentAdGroup.class)
                .applyTo(addedAdGroup)
                .castModelUp(AdGroup.class);

        adGroupRepository.updateAdGroups(
                adGroup.getShard(), adGroup.getClientId(), singletonList(expectedAdGroup));

        AdGroup actualAdGroup = adGroupRepository.getAdGroups(
                        adGroup.getShard(), singletonList(adGroup.getAdGroupId()))
                .get(0);

        assertThat(actualAdGroup)
                .isEqualToComparingOnlyGivenFields(
                        fillExpectedMinusWords(expectedAdGroup.getModel()), MOBILE_CONTENT_COMPARE_PROPERTIES);
    }

    @Test
    public void testDynamicText() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveDynamicCampaign(clientInfo);
        AdGroupInfo adGroup = steps.adGroupSteps().createAdGroup(
                applyDynamicTextOldValues(campaignInfo.getShard(),
                        TestGroups.activeDynamicTextAdGroup(campaignInfo.getCampaignId())), campaignInfo);

        DynamicTextAdGroup addedAdGroup = (DynamicTextAdGroup) adGroupRepository.getAdGroups(
                adGroup.getShard(), singletonList(adGroup.getAdGroupId())).get(0);

        AppliedChanges<AdGroup> expectedAdGroup = applyDynamicTextNewValues(
                campaignInfo.getShard(),
                addedAdGroup, DynamicTextAdGroup.class)
                .applyTo(addedAdGroup)
                .castModelUp(AdGroup.class);

        adGroupRepository.updateAdGroups(
                adGroup.getShard(), adGroup.getClientId(), singletonList(expectedAdGroup));

        AdGroup actualAdGroup = adGroupRepository.getAdGroups(
                        adGroup.getShard(), singletonList(adGroup.getAdGroupId()))
                .get(0);

        assertThat(actualAdGroup)
                .isEqualToComparingOnlyGivenFields(
                        fillExpectedMinusWords(expectedAdGroup.getModel()), DYNAMIC_TEXT_COMPARE_PROPERTIES);
    }

    @Test
    public void testDynamicFeedDynamicFields() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveDynamicCampaign(clientInfo);
        FeedInfo feedInfo = feedSteps.createDefaultFeed(campaignInfo.getClientInfo());
        AdGroupInfo adGroup = steps.adGroupSteps().createAdGroup(
                applyDynamicOldValues(
                        TestGroups.activeDynamicFeedAdGroup(campaignInfo.getCampaignId(), feedInfo.getFeedId())),
                campaignInfo);

        DynamicFeedAdGroup addedAdGroup = (DynamicFeedAdGroup) adGroupRepository.getAdGroups(
                adGroup.getShard(), singletonList(adGroup.getAdGroupId())).get(0);

        AppliedChanges<AdGroup> expectedAdGroup = applyDynamicNewValues(addedAdGroup, DynamicFeedAdGroup.class)
                .applyTo(addedAdGroup)
                .castModelUp(AdGroup.class);

        adGroupRepository.updateAdGroups(
                adGroup.getShard(), adGroup.getClientId(), singletonList(expectedAdGroup));

        AdGroup actualAdGroup = adGroupRepository.getAdGroups(
                        adGroup.getShard(), singletonList(adGroup.getAdGroupId()))
                .get(0);

        assertThat(actualAdGroup)
                .isEqualToComparingOnlyGivenFields(
                        fillExpectedMinusWords(expectedAdGroup.getModel()), DYNAMIC_COMPARE_PROPERTIES);
    }

    // Все, кроме самой строчки с операцией - копипаста из
    // PerformanceAdGroupSupportTest::updateAdGroups_success
    // https://a.yandex-team.ru/arc_vcs/direct/core/src/test/java/ru/yandex/direct/core/entity/adgroup/repository/typesupport/PerformanceAdGroupSupportTest.java?rev=r6137075#L42
    @Test
    public void testPerformanceFields() {
        //Подготавливаем данные в базе (группу и новый фид)
        PerformanceAdGroupInfo groupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup();
        PerformanceAdGroup performanceAdGroup = groupInfo.getPerformanceAdGroup();
        Integer shard = groupInfo.getShard();
        Long adGroupId = groupInfo.getAdGroupId();
        ClientId clientId = groupInfo.getClientId();
        ClientInfo clientInfo = groupInfo.getClientInfo();
        FeedInfo newFeedInfo = feedSteps.createDefaultFeed(clientInfo);
        Long newFeedId = newFeedInfo.getFeedId();

        //Группа с новыми полями
        PerformanceAdGroup groupWithChangedFields = new PerformanceAdGroup()
                .withFeedId(newFeedId)
                .withStatusBLGenerated(StatusBLGenerated.PROCESSING)
                .withFieldToUseAsName("The changed name")
                .withFieldToUseAsBody("The changed body");

        //Сохраняем новые значения полей
        AppliedChanges<PerformanceAdGroup> changes =
                new ModelChanges<>(adGroupId, PerformanceAdGroup.class)
                        .process(groupWithChangedFields.getFeedId(), PerformanceAdGroup.FEED_ID)
                        .process(groupWithChangedFields.getStatusBLGenerated(), PerformanceAdGroup.STATUS_B_L_GENERATED)
                        .process(groupWithChangedFields.getFieldToUseAsName(), PerformanceAdGroup.FIELD_TO_USE_AS_NAME)
                        .process(groupWithChangedFields.getFieldToUseAsBody(), PerformanceAdGroup.FIELD_TO_USE_AS_BODY)
                        .applyTo(performanceAdGroup);
        AppliedChanges<AdGroup> adGroupAppliedChanges = changes.castModelUp(AdGroup.class);
        adGroupRepository.updateAdGroups(shard, clientId, singletonList(adGroupAppliedChanges));

        //Читаем актаульное состояние из базы
        AdGroup actual = adGroupRepository.getAdGroups(shard, singletonList(adGroupId)).get(0);

        //Сверяем прочитанное из базы с тем что сохраняли.
        AssertionsForClassTypes.assertThat(actual)
                .is(matchedBy(beanDiffer(groupWithChangedFields)
                        .useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void testCpmBanner() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveCpmBannerCampaign(clientInfo);
        AdGroupInfo adGroup = steps.adGroupSteps().createAdGroup(
                applyOldValues(TestGroups.activeCpmBannerAdGroup(campaignInfo.getCampaignId())), campaignInfo);

        AdGroup addedAdGroup = adGroupRepository.getAdGroups(
                adGroup.getShard(), singletonList(adGroup.getAdGroupId())).get(0);

        AppliedChanges<AdGroup> expectedAdGroup = applyNewValues(addedAdGroup, AdGroup.class)
                .applyTo(addedAdGroup);

        adGroupRepository.updateAdGroups(
                adGroup.getShard(), adGroup.getClientId(), singletonList(expectedAdGroup));

        AdGroup actualAdGroup = adGroupRepository.getAdGroups(
                        adGroup.getShard(), singletonList(adGroup.getAdGroupId()))
                .get(0);

        assertThat(actualAdGroup)
                .isEqualToComparingOnlyGivenFields(
                        fillExpectedMinusWords(expectedAdGroup.getModel()), COMMON_COMPARE_PROPERTIES);
    }

    @Test
    public void testCpmGeoproductBanner() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveCpmBannerCampaign(clientInfo);
        AdGroupInfo adGroup = steps.adGroupSteps().createAdGroup(
                applyOldValues(TestGroups.activeCpmGeoproductAdGroup(campaignInfo.getCampaignId())), campaignInfo);

        AdGroup addedAdGroup = adGroupRepository.getAdGroups(
                adGroup.getShard(), singletonList(adGroup.getAdGroupId())).get(0);

        AppliedChanges<AdGroup> expectedAdGroup = applyNewValues(addedAdGroup, AdGroup.class)
                .applyTo(addedAdGroup);

        adGroupRepository.updateAdGroups(
                adGroup.getShard(), adGroup.getClientId(), singletonList(expectedAdGroup));

        AdGroup actualAdGroup = adGroupRepository.getAdGroups(
                        adGroup.getShard(), singletonList(adGroup.getAdGroupId()))
                .get(0);

        assertThat(actualAdGroup)
                .isEqualToComparingOnlyGivenFields(
                        fillExpectedMinusWords(expectedAdGroup.getModel()), COMMON_COMPARE_PROPERTIES);
    }

    @Test
    public void testCpmGeoproduct_UpdateBsTagsToAppNavi() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveCpmBannerCampaign(clientInfo);
        AdGroupInfo adGroup = steps.adGroupSteps().createAdGroup(
                applyOldValues(TestGroups.activeCpmGeoproductAdGroup(campaignInfo.getCampaignId()))
                        .withTargetTags(singletonList("app-metro"))
                        .withPageGroupTags(singletonList("app-metro")), campaignInfo);

        AdGroup addedAdGroup = adGroupRepository.getAdGroups(adGroup.getShard(), singletonList(adGroup.getAdGroupId()))
                .get(0);

        AppliedChanges<AdGroup> expectedAdGroup = applyNewValues(addedAdGroup, AdGroup.class)
                .process(singletonList("app-navi"), AdGroup.TARGET_TAGS)
                .process(singletonList("app-navi"), AdGroup.PAGE_GROUP_TAGS)
                .applyTo(addedAdGroup);

        adGroupRepository.updateAdGroups(
                adGroup.getShard(), adGroup.getClientId(), singletonList(expectedAdGroup));

        AdGroup actualAdGroup = adGroupRepository.getAdGroups(
                        adGroup.getShard(), singletonList(adGroup.getAdGroupId()))
                .get(0);

        assertThat(actualAdGroup)
                .isEqualToComparingOnlyGivenFields(
                        fillExpectedMinusWords(expectedAdGroup.getModel()), COMMON_COMPARE_PROPERTIES);
    }

    @Test
    public void testCpmVideo() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveCpmBannerCampaign(clientInfo);
        AdGroupInfo adGroup = steps.adGroupSteps().createAdGroup(
                applyOldValues(TestGroups.activeCpmVideoAdGroup(campaignInfo.getCampaignId())), campaignInfo);

        AdGroup addedAdGroup = adGroupRepository.getAdGroups(
                adGroup.getShard(), singletonList(adGroup.getAdGroupId())).get(0);

        AppliedChanges<AdGroup> expectedAdGroup = applyNewValues(addedAdGroup, AdGroup.class)
                .applyTo(addedAdGroup);

        adGroupRepository.updateAdGroups(
                adGroup.getShard(), adGroup.getClientId(), singletonList(expectedAdGroup));

        AdGroup actualAdGroup = adGroupRepository.getAdGroups(
                        adGroup.getShard(), singletonList(adGroup.getAdGroupId()))
                .get(0);

        assertThat(actualAdGroup)
                .isEqualToComparingOnlyGivenFields(
                        fillExpectedMinusWords(expectedAdGroup.getModel()), COMMON_COMPARE_PROPERTIES);
    }

    @Test
    public void testCpmAudio() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveCpmBannerCampaign(clientInfo);
        AdGroupInfo adGroup = steps.adGroupSteps().createAdGroup(
                applyOldValues(TestGroups.activeCpmAudioAdGroup(campaignInfo.getCampaignId())), campaignInfo);

        AdGroup addedAdGroup = adGroupRepository.getAdGroups(
                adGroup.getShard(), singletonList(adGroup.getAdGroupId())).get(0);

        AppliedChanges<AdGroup> expectedAdGroup = applyNewValues(addedAdGroup, AdGroup.class)
                .applyTo(addedAdGroup);

        adGroupRepository.updateAdGroups(
                adGroup.getShard(), adGroup.getClientId(), singletonList(expectedAdGroup));

        AdGroup actualAdGroup = adGroupRepository.getAdGroups(
                        adGroup.getShard(), singletonList(adGroup.getAdGroupId()))
                .get(0);

        assertThat(actualAdGroup)
                .isEqualToComparingOnlyGivenFields(
                        fillExpectedMinusWords(expectedAdGroup.getModel()), COMMON_COMPARE_PROPERTIES);
    }

    @Test
    public void testCpmOutdoor() {
        OutdoorPlacement placement = placementSteps.addDefaultOutdoorPlacementWithOneBlock();

        CampaignInfo campaignInfo = steps.campaignSteps().createActiveCpmBannerCampaign(clientInfo);
        AdGroupInfo adGroup = steps.adGroupSteps().createAdGroup(
                applyOldValues(activeCpmOutdoorAdGroup(campaignInfo.getCampaignId(), placement)), campaignInfo);

        CpmOutdoorAdGroup addedAdGroup = (CpmOutdoorAdGroup) adGroupRepository
                .getAdGroups(adGroup.getShard(), singletonList(adGroup.getAdGroupId()))
                .get(0);

        AppliedChanges<AdGroup> expectedAdGroup = applyNewValues(addedAdGroup, CpmOutdoorAdGroup.class)
                .process(asList(new PageBlock().withPageId(1L).withImpId(1L),
                        new PageBlock().withPageId(3L).withImpId(2L)), CpmOutdoorAdGroup.PAGE_BLOCKS)
                .applyTo(addedAdGroup)
                .castModelUp(AdGroup.class);

        adGroupRepository.updateAdGroups(
                adGroup.getShard(), adGroup.getClientId(), singletonList(expectedAdGroup));

        AdGroup actualAdGroup = adGroupRepository.getAdGroups(
                        adGroup.getShard(), singletonList(adGroup.getAdGroupId()))
                .get(0);

        assertThat(actualAdGroup)
                .isEqualToComparingOnlyGivenFields(
                        fillExpectedMinusWords(expectedAdGroup.getModel()), CPM_OUTDOOR_COMPARE_PROPERTIES);
    }

    @Test
    public void testCpmIndoor() {
        IndoorPlacement placement = placementSteps.addDefaultIndoorPlacementWithOneBlock();

        CampaignInfo campaignInfo = steps.campaignSteps().createActiveCpmBannerCampaign(clientInfo);
        AdGroupInfo adGroup = steps.adGroupSteps().createAdGroup(
                applyOldValues(activeCpmIndoorAdGroup(campaignInfo.getCampaignId(), placement)), campaignInfo);

        CpmIndoorAdGroup addedAdGroup = (CpmIndoorAdGroup) adGroupRepository
                .getAdGroups(adGroup.getShard(), singletonList(adGroup.getAdGroupId()))
                .get(0);

        AppliedChanges<AdGroup> expectedAdGroup = applyNewValues(addedAdGroup, CpmIndoorAdGroup.class)
                .process(asList(new PageBlock().withPageId(1L).withImpId(1L),
                        new PageBlock().withPageId(3L).withImpId(2L)), CpmIndoorAdGroup.PAGE_BLOCKS)
                .applyTo(addedAdGroup)
                .castModelUp(AdGroup.class);

        adGroupRepository.updateAdGroups(
                adGroup.getShard(), adGroup.getClientId(), singletonList(expectedAdGroup));

        AdGroup actualAdGroup = adGroupRepository.getAdGroups(
                        adGroup.getShard(), singletonList(adGroup.getAdGroupId()))
                .get(0);

        assertThat(actualAdGroup)
                .isEqualToComparingOnlyGivenFields(
                        fillExpectedMinusWords(expectedAdGroup.getModel()), CPM_INDOOR_COMPARE_PROPERTIES);
    }

    @Test
    public void testCpmYndxFrontpage() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveCpmYndxFrontpageCampaign(clientInfo);
        AdGroupInfo adGroup = steps.adGroupSteps().createAdGroup(
                applyOldValues(TestGroups.activeCpmYndxFrontpageAdGroup(campaignInfo.getCampaignId())), campaignInfo);

        AdGroup addedAdGroup = adGroupRepository.getAdGroups(
                adGroup.getShard(), singletonList(adGroup.getAdGroupId())).get(0);

        AppliedChanges<AdGroup> expectedAdGroup = applyNewValues(addedAdGroup, AdGroup.class)
                .applyTo(addedAdGroup);

        adGroupRepository.updateAdGroups(
                adGroup.getShard(), adGroup.getClientId(), singletonList(expectedAdGroup));

        AdGroup actualAdGroup = adGroupRepository.getAdGroups(
                        adGroup.getShard(), singletonList(adGroup.getAdGroupId()))
                .get(0);

        assertThat(actualAdGroup)
                .isEqualToComparingOnlyGivenFields(
                        fillExpectedMinusWords(expectedAdGroup.getModel()), COMMON_COMPARE_PROPERTIES);
    }

    @Test
    public void updateContentPromotionVideoAdGroup_SaveAllFields_SuccessTest() {
        var campaignInfo = steps.contentPromotionCampaignSteps().createDefaultCampaign();
        var adGroup = steps.contentPromotionAdGroupSteps().createAdGroup(
                new ContentPromotionAdGroupInfo()
                        .withCampaignInfo(campaignInfo)
                        .withAdGroup(applyOldValues(fullContentPromotionAdGroup(ContentPromotionAdgroupType.VIDEO))));

        AdGroup addedAdGroup = adGroupRepository.getAdGroups(
                adGroup.getShard(), singletonList(adGroup.getAdGroupId())).get(0);

        AppliedChanges<AdGroup> expectedAdGroup = applyNewValues(addedAdGroup, AdGroup.class)
                .applyTo(addedAdGroup);

        adGroupRepository.updateAdGroups(
                adGroup.getShard(), adGroup.getClientId(), singletonList(expectedAdGroup));

        AdGroup actualAdGroup = adGroupRepository.getAdGroups(
                        adGroup.getShard(), singletonList(adGroup.getAdGroupId()))
                .get(0);

        assertThat(actualAdGroup)
                .isEqualToComparingOnlyGivenFields(
                        fillExpectedMinusWords(expectedAdGroup.getModel()), COMMON_COMPARE_PROPERTIES);
    }

    @Test
    public void updateContentPromotionAdGroup_SaveAllFields_SuccessTest() {
        var campaignInfo = steps.contentPromotionCampaignSteps().createDefaultCampaign();
        var adGroupInfo =
                steps.contentPromotionAdGroupSteps().createAdGroup(campaignInfo,
                        applyOldValues(fullContentPromotionAdGroup(ContentPromotionAdgroupType.COLLECTION)));

        AdGroup addedAdGroup = adGroupRepository.getAdGroups(
                adGroupInfo.getShard(), singletonList(adGroupInfo.getAdGroupId())).get(0);

        AppliedChanges<AdGroup> expectedAdGroup = applyNewValues(addedAdGroup, AdGroup.class)
                .applyTo(addedAdGroup);

        adGroupRepository.updateAdGroups(
                adGroupInfo.getShard(), adGroupInfo.getClientId(), singletonList(expectedAdGroup));

        AdGroup actualAdGroup = adGroupRepository.getAdGroups(
                        adGroupInfo.getShard(), singletonList(adGroupInfo.getAdGroupId()))
                .get(0);

        assertThat(actualAdGroup)
                .isEqualToComparingOnlyGivenFields(
                        fillExpectedMinusWords(expectedAdGroup.getModel()), COMMON_COMPARE_PROPERTIES);
    }

    @Test
    public void testInternalFreeBanner() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveInternalFreeCampaign();
        AdGroupInfo adGroup = steps.adGroupSteps().createAdGroup(
                applyOldValues(TestGroups.activeInternalAdGroup(campaignInfo.getCampaignId())));

        AdGroup addedAdGroup = adGroupRepository.getAdGroups(
                adGroup.getShard(), singletonList(adGroup.getAdGroupId())).get(0);

        AppliedChanges<AdGroup> expectedAdGroup = applyNewValues(addedAdGroup, AdGroup.class)
                .applyTo(addedAdGroup);

        adGroupRepository.updateAdGroups(
                adGroup.getShard(), adGroup.getClientId(), singletonList(expectedAdGroup));

        AdGroup actualAdGroup = adGroupRepository.getAdGroups(
                        adGroup.getShard(), singletonList(adGroup.getAdGroupId()))
                .get(0);

        assertThat(actualAdGroup)
                .isEqualToComparingOnlyGivenFields(
                        fillExpectedMinusWords(expectedAdGroup.getModel()), INTERNAL_COMPARE_PROPERTIES);
    }

    @Test
    public void testInternalFreeBannerChangeLevel() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveInternalFreeCampaign();
        AdGroupInfo adGroup = steps.adGroupSteps().createAdGroup(
                applyOldValues(TestGroups.activeInternalAdGroup(campaignInfo.getCampaignId())));

        InternalAdGroup addedAdGroup = (InternalAdGroup) adGroupRepository.getAdGroups(
                adGroup.getShard(), singletonList(adGroup.getAdGroupId())).get(0);

        AppliedChanges<AdGroup> expectedAdGroup = applyNewValues(addedAdGroup, AdGroup.class)
                .applyTo(addedAdGroup);

        adGroupRepository.updateAdGroups(
                adGroup.getShard(), adGroup.getClientId(), singletonList(expectedAdGroup));

        AdGroup actualAdGroup = adGroupRepository.getAdGroups(
                        adGroup.getShard(), singletonList(adGroup.getAdGroupId()))
                .get(0);

        assertThat(actualAdGroup)
                .isEqualToComparingOnlyGivenFields(
                        fillExpectedMinusWords(expectedAdGroup.getModel()), INTERNAL_COMPARE_PROPERTIES);

        AppliedChanges<InternalAdGroup> changes =
                new ModelChanges<>(addedAdGroup.getId(), InternalAdGroup.class)
                        .process(2L, InternalAdGroup.LEVEL)
                        .applyTo(addedAdGroup);

        expectedAdGroup = changes.castModelUp(AdGroup.class);

        adGroupRepository.updateAdGroups(adGroup.getShard(), adGroup.getClientId(), singletonList(expectedAdGroup));

        actualAdGroup = adGroupRepository.getAdGroups(adGroup.getShard(), singletonList(adGroup.getAdGroupId())).get(0);

        assertThat(actualAdGroup)
                .isEqualToComparingOnlyGivenFields(
                        fillExpectedMinusWords(expectedAdGroup.getModel()), INTERNAL_COMPARE_PROPERTIES);

        changes = new ModelChanges<>(addedAdGroup.getId(), InternalAdGroup.class)
                .process(null, InternalAdGroup.LEVEL)
                .applyTo(addedAdGroup);

        expectedAdGroup = changes.castModelUp(AdGroup.class);

        adGroupRepository.updateAdGroups(adGroup.getShard(), adGroup.getClientId(), singletonList(expectedAdGroup));

        actualAdGroup = adGroupRepository.getAdGroups(adGroup.getShard(), singletonList(adGroup.getAdGroupId())).get(0);

        var expectedModel = (InternalAdGroup) expectedAdGroup.getModel();
        expectedModel.setLevel(DEFAULT_LEVEL);
        assertThat(actualAdGroup)
                .isEqualToComparingOnlyGivenFields(
                        fillExpectedMinusWords(expectedModel), INTERNAL_COMPARE_PROPERTIES);
    }

    @Test
    public void testInternalDistribBanner() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveInternalDistribCampaign();
        AdGroupInfo adGroup = steps.adGroupSteps().createAdGroup(
                applyOldValues(TestGroups.activeInternalAdGroup(campaignInfo.getCampaignId())));

        AdGroup addedAdGroup = adGroupRepository.getAdGroups(
                adGroup.getShard(), singletonList(adGroup.getAdGroupId())).get(0);

        AppliedChanges<AdGroup> expectedAdGroup = applyNewValues(addedAdGroup, AdGroup.class)
                .applyTo(addedAdGroup);

        adGroupRepository.updateAdGroups(
                adGroup.getShard(), adGroup.getClientId(), singletonList(expectedAdGroup));

        AdGroup actualAdGroup = adGroupRepository.getAdGroups(
                        adGroup.getShard(), singletonList(adGroup.getAdGroupId()))
                .get(0);

        assertThat(actualAdGroup)
                .isEqualToComparingOnlyGivenFields(
                        fillExpectedMinusWords(expectedAdGroup.getModel()), INTERNAL_COMPARE_PROPERTIES);
    }

    @Test
    public void testInternalAutobudgetBanner() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveInternalAutobudgetCampaign();
        AdGroupInfo adGroup = steps.adGroupSteps().createAdGroup(
                applyOldValues(TestGroups.activeInternalAdGroup(campaignInfo.getCampaignId())));

        AdGroup addedAdGroup = adGroupRepository.getAdGroups(
                adGroup.getShard(), singletonList(adGroup.getAdGroupId())).get(0);

        AppliedChanges<AdGroup> expectedAdGroup = applyNewValues(addedAdGroup, AdGroup.class)
                .applyTo(addedAdGroup);

        adGroupRepository.updateAdGroups(
                adGroup.getShard(), adGroup.getClientId(), singletonList(expectedAdGroup));

        AdGroup actualAdGroup = adGroupRepository.getAdGroups(
                        adGroup.getShard(), singletonList(adGroup.getAdGroupId()))
                .get(0);

        assertThat(actualAdGroup)
                .isEqualToComparingOnlyGivenFields(
                        fillExpectedMinusWords(expectedAdGroup.getModel()), INTERNAL_COMPARE_PROPERTIES);
    }

    @Test
    public void testMcBanner() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveMcBannerCampaign(clientInfo);
        AdGroupInfo adGroup = steps.adGroupSteps().createAdGroup(
                applyOldValues(TestGroups.activeMcBannerAdGroup(campaignInfo.getCampaignId())), campaignInfo);

        AdGroup addedAdGroup = adGroupRepository.getAdGroups(
                adGroup.getShard(), singletonList(adGroup.getAdGroupId())).get(0);

        AppliedChanges<AdGroup> expectedAdGroup = applyNewValues(addedAdGroup, AdGroup.class)
                .applyTo(addedAdGroup);

        adGroupRepository.updateAdGroups(
                adGroup.getShard(), adGroup.getClientId(), singletonList(expectedAdGroup));

        AdGroup actualAdGroup = adGroupRepository.getAdGroups(
                        adGroup.getShard(), singletonList(adGroup.getAdGroupId()))
                .get(0);

        assertThat(actualAdGroup)
                .isEqualToComparingOnlyGivenFields(
                        fillExpectedMinusWords(expectedAdGroup.getModel()), COMMON_COMPARE_PROPERTIES);
    }

    @Test
    public void testPageGroupAndTargetTags() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveCpmBannerCampaign(clientInfo);
        AdGroupInfo adGroup = steps.adGroupSteps().createAdGroup(
                applyOldValues(TestGroups.activeTextAdGroup(campaignInfo.getCampaignId())), campaignInfo);

        AdGroup addedAdGroup = adGroupRepository.getAdGroups(
                adGroup.getShard(), singletonList(adGroup.getAdGroupId())).get(0);

        AppliedChanges<AdGroup> expectedAdGroup = applyPageGroupAndTargetTagsNewValues(addedAdGroup, AdGroup.class)
                .applyTo(addedAdGroup);

        adGroupRepository.updateAdGroups(
                adGroup.getShard(), adGroup.getClientId(), singletonList(expectedAdGroup));

        AdGroup actualAdGroup = adGroupRepository.getAdGroups(
                        adGroup.getShard(), singletonList(adGroup.getAdGroupId()))
                .get(0);

        assertThat(actualAdGroup)
                .isEqualToComparingOnlyGivenFields(
                        fillExpectedMinusWords(expectedAdGroup.getModel()), COMMON_COMPARE_PROPERTIES);
    }
}
