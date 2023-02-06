package ru.yandex.direct.grid.processing.util;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import one.util.streamex.StreamEx;
import org.apache.commons.lang3.RandomStringUtils;

import ru.yandex.direct.core.entity.adgroup.model.AdGroupStates;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.bidmodifier.container.MultipliersBounds;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.grid.core.entity.group.container.GdiAdGroupRegionsInfo;
import ru.yandex.direct.grid.core.entity.group.model.GdiBaseGroup;
import ru.yandex.direct.grid.core.entity.group.model.GdiDynamicGroup;
import ru.yandex.direct.grid.core.entity.group.model.GdiGroupModerationStatus;
import ru.yandex.direct.grid.core.entity.group.model.GdiGroupPrimaryStatus;
import ru.yandex.direct.grid.core.entity.group.model.GdiGroupStatus;
import ru.yandex.direct.grid.core.entity.group.model.GdiInternalGroup;
import ru.yandex.direct.grid.core.entity.group.model.GdiMinusKeywordsPackInfo;
import ru.yandex.direct.grid.core.entity.group.model.GdiMobileContentAdGroupOsType;
import ru.yandex.direct.grid.core.entity.group.model.GdiMobileContentGroup;
import ru.yandex.direct.grid.core.entity.model.GdiEntityStats;
import ru.yandex.direct.grid.core.util.stats.GridStatNew;
import ru.yandex.direct.grid.model.campaign.GdCampaignType;
import ru.yandex.direct.grid.model.entity.adgroup.GdAdGroupType;
import ru.yandex.direct.grid.processing.model.group.GdAdGroup;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupAccess;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupFilter;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupModerationStatus;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupMultipliersBounds;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupPrimaryStatus;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupRegionsInfo;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupStatus;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupsContainer;
import ru.yandex.direct.grid.processing.model.group.GdBannerAction;
import ru.yandex.direct.grid.processing.model.group.GdDynamicAdGroup;
import ru.yandex.direct.grid.processing.model.group.GdMinusKeywordsPackInfo;
import ru.yandex.direct.grid.processing.model.group.GdMobileContentAdGroup;
import ru.yandex.direct.grid.processing.model.group.GdTextAdGroup;
import ru.yandex.direct.grid.processing.service.group.AvailableAdGroupTypesCalculator;
import ru.yandex.direct.regions.Region;
import ru.yandex.direct.test.utils.RandomNumberUtils;
import ru.yandex.direct.utils.Counter;

import static com.google.common.primitives.Longs.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static ru.yandex.direct.grid.processing.util.InputTestDataUtils.getDefaultLimitOffset;
import static ru.yandex.direct.grid.processing.util.InputTestDataUtils.getDefaultStatRequirements;
import static ru.yandex.direct.utils.CommonUtils.nvl;

/**
 * Вспомогательные методы для создания внутренних и внешних моделей AdGroups
 */
public class AdGroupTestDataUtils {

    //Тестовые идентификаторы не предназначены для работы с БД, т.к. при параллельном запуске тестов возможны коллизии
    //Про работе с БД нужно самостоятельно заботиться об уникальности id.
    public static final long TEST_CID = 10001L;
    public static final long[] TEST_GROUP_IDS = {153, 193, 293, 393, 493, 593, 693, 793, 893, 993, 1093};

    public static final String CAMPAIGN_NOT_FOUND_DEFECT = "CampaignDefectIds.Gen.CAMPAIGN_NOT_FOUND";
    public static final String INCONSISTENT_AD_GROUP_TYPE_TO_CAMPAIGN_TYPE_DEFECT =
            "AdGroupDefectIds.Gen.INCONSISTENT_AD_GROUP_TYPE_TO_CAMPAIGN_TYPE";
    public static final String FEED_NOT_EXIST_DEFECT = "AdGroupDefectIds.ModelId.FEED_NOT_EXIST";
    public static final String CONTENT_PROMOTION_SEVERAL_TYPES_NOT_ALLOWED =
            "AdGroupDefectIds.Gen.CONTENT_PROMOTION_SEVERAL_TYPES_NOT_ALLOWED";
    public static final String CONTENT_PROMOTION_DISTINCT_TYPE_FROM_EXISTING =
            "AdGroupDefectIds.Gen.CONTENT_PROMOTION_DISTINCT_TYPE_FROM_EXISTING";
    public static final String CONTENT_TYPE_NOT_MATCHES_ADGROUP_CONTENT_TYPE =
            "BannerDefectIds.Gen.CONTENT_TYPE_NOT_MATCHES_ADGROUP_CONTENT_TYPE";
    public static final String MUST_BE_VALID_ID = "DefectIds.MUST_BE_VALID_ID";

    private static final String DYNAMIC_MAIN_DOMAIN = RandomStringUtils.randomAlphabetic(7);
    private static final String MOBILE_STORE_CONTENT_HREF = RandomStringUtils.randomAlphabetic(7);
    private static final GdiMobileContentAdGroupOsType MOBILE_OS_TYPE = GdiMobileContentAdGroupOsType.ANDROID;
    private static final String MOBILE_ICON_HASH = "BFPewVw";

    private static final int MULTIPLIER_UPPER_BOUND = 110;
    private static final int MULTIPLIER_LOWER_BOUND = 10;

    //внутренние представление гео-регионов
    private static final GdiAdGroupRegionsInfo[] INTERNAL_GROUP_GEO_REGIONS = {
            new GdiAdGroupRegionsInfo()
                    .withRegionIds(emptyList()),
            new GdiAdGroupRegionsInfo()
                    .withRegionIds(singletonList(Region.RUSSIA_REGION_ID)),
            new GdiAdGroupRegionsInfo()
                    .withRegionIds(asList(Region.RUSSIA_REGION_ID, Region.KAZAKHSTAN_REGION_ID))
                    .withEffectiveRegionIds(singletonList(Region.RUSSIA_REGION_ID))
                    .withRestrictedRegionIds(singletonList(Region.KAZAKHSTAN_REGION_ID))
    };

    //внешнее представление гео-регионов, соответсвущие внутренним (INTERNAL_GROUP_GEO_REGIONS)
    private static final GdAdGroupRegionsInfo[] GROUP_GEO_REGIONS = {
            new GdAdGroupRegionsInfo()
                    .withRegionIds(emptyList()),
            new GdAdGroupRegionsInfo()
                    .withRegionIds(singletonList((int) Region.RUSSIA_REGION_ID)),
            new GdAdGroupRegionsInfo()
                    .withRegionIds(
                            Arrays.asList((int) Region.RUSSIA_REGION_ID, (int) Region.KAZAKHSTAN_REGION_ID))
                    .withEffectiveRegionIds(singletonList((int) Region.RUSSIA_REGION_ID))
                    .withRestrictedRegionIds(singletonList((int) Region.KAZAKHSTAN_REGION_ID))
    };

    public static GdiBaseGroup defaultGdiBaseGroup() {
        return defaultGdiBaseGroup(TEST_CID, TEST_GROUP_IDS[0], INTERNAL_GROUP_GEO_REGIONS[0]);
    }

    public static GdiBaseGroup defaultGdiBaseGroup(long cid, long pid, GdiAdGroupRegionsInfo geo) {
        return new GdiBaseGroup()
                .withId(pid)
                .withMultipliersBounds(new MultipliersBounds().withLower(MULTIPLIER_LOWER_BOUND)
                        .withUpper(MULTIPLIER_UPPER_BOUND))
                .withCampaignId(cid)
                .withType(AdGroupType.BASE)
                .withRegionsInfo(geo)
                .withStatus(defaultGdiGroupStatus())
                .withStat(getEmptyStats())
                .withLibraryMinusKeywordsPacks(singletonList(new GdiMinusKeywordsPackInfo()
                        .withId(123L)
                        .withName("some pack name")));
    }

    public static GdiMobileContentGroup defaultGdiMobileContentGroup() {
        return defaultGdiMobileContentGroup(TEST_CID, TEST_GROUP_IDS[1], INTERNAL_GROUP_GEO_REGIONS[1]);
    }

    public static GdiMobileContentGroup defaultGdiMobileContentGroup(long cid, long pid, GdiAdGroupRegionsInfo geo) {
        return new GdiMobileContentGroup()
                .withId(pid)
                .withMultipliersBounds(new MultipliersBounds().withLower(MULTIPLIER_LOWER_BOUND)
                        .withUpper(MULTIPLIER_UPPER_BOUND))
                .withCampaignId(cid)
                .withType(AdGroupType.MOBILE_CONTENT)
                .withRegionsInfo(geo)
                .withStatus(defaultGdiGroupStatus())
                .withStoreHref(MOBILE_STORE_CONTENT_HREF)
                .withIconHash(MOBILE_ICON_HASH)
                .withOsType(MOBILE_OS_TYPE)
                .withStat(getEmptyStats());
    }

    public static GdiInternalGroup defaultGdiInternalGroup() {
        return new GdiInternalGroup()
                .withId(TEST_GROUP_IDS[10])
                .withCampaignId(TEST_CID)
                .withMultipliersBounds(new MultipliersBounds().withLower(MULTIPLIER_LOWER_BOUND)
                        .withUpper(MULTIPLIER_UPPER_BOUND))
                .withType(AdGroupType.MOBILE_CONTENT)
                .withRegionsInfo(INTERNAL_GROUP_GEO_REGIONS[0])
                .withStatus(defaultGdiGroupStatus())
                .withStat(getEmptyStats());
    }

    public static GdiDynamicGroup defaultGdiDynamicGroup() {
        return defaultGdiDynamicGroup(TEST_CID, TEST_GROUP_IDS[2], INTERNAL_GROUP_GEO_REGIONS[2]);
    }

    public static GdiDynamicGroup defaultGdiDynamicGroup(Long cid, Long pid, GdiAdGroupRegionsInfo geo) {
        return new GdiDynamicGroup()
                .withId(pid)
                .withMultipliersBounds(new MultipliersBounds().withLower(MULTIPLIER_LOWER_BOUND)
                        .withUpper(MULTIPLIER_UPPER_BOUND))
                .withCampaignId(cid)
                .withType(AdGroupType.DYNAMIC)
                .withRegionsInfo(geo)
                .withStatus(defaultGdiGroupStatus())
                .withMainDomain(DYNAMIC_MAIN_DOMAIN)
                .withStat(getEmptyStats());
    }

    /**
     * Возвращает группу с незаполненными полями index, campaign
     */
    public static GdTextAdGroup defaultGdBaseGroup() {
        return defaultGdBaseGroup(TEST_CID, TEST_GROUP_IDS[0], GROUP_GEO_REGIONS[0]);
    }

    public static GdTextAdGroup defaultGdBaseGroup(Long pid) {
        return defaultGdBaseGroup(TEST_CID, pid, GROUP_GEO_REGIONS[0]);
    }

    public static GdTextAdGroup defaultGdBaseGroup(Long cid, Long pid, GdAdGroupRegionsInfo geo) {
        GdAdGroupStatus gdAdGroupStatus = defaultGdAdGroupStatus();

        return new GdTextAdGroup()
                .withId(pid)
                .withMultipliersBounds(new GdAdGroupMultipliersBounds().withLower(MULTIPLIER_LOWER_BOUND)
                        .withUpper(MULTIPLIER_UPPER_BOUND))
                .withType(GdAdGroupType.TEXT)
                .withCampaignId(cid)
                .withMinusKeywords(emptyList())
                .withLibraryMinusKeywordsPacks(singletonList(new GdMinusKeywordsPackInfo()
                        .withId(123L)
                        .withName("some pack name")))
                .withRegionsInfo(nvl(geo, GROUP_GEO_REGIONS[0]))
                .withAccess(defaultGdAdGroupAccess(pid, AdGroupType.BASE, gdAdGroupStatus))
                .withStatus(gdAdGroupStatus)
                .withStats(StatHelper.internalStatsToOuter(getEmptyStats(), GdCampaignType.TEXT));
    }

    public static List<GdAdGroup> generateAdGroupsList(int size) {
        Counter counter = new Counter();
        return StreamEx.generate(() -> defaultGdBaseGroup((long) counter.next()))
                .limit(size)
                .map(GdAdGroup.class::cast)
                .toList();
    }

    public static GdMobileContentAdGroup defaultGdMobileContentGroup() {
        return defaultGdMobileContentGroup(TEST_CID, TEST_GROUP_IDS[1], GROUP_GEO_REGIONS[1]);
    }

    public static GdMobileContentAdGroup defaultGdMobileContentGroup(Long cid, Long pid, GdAdGroupRegionsInfo geo) {
        GdAdGroupStatus gdAdGroupStatus = defaultGdAdGroupStatus();

        return new GdMobileContentAdGroup()
                .withId(pid)
                .withMultipliersBounds(new GdAdGroupMultipliersBounds().withLower(MULTIPLIER_LOWER_BOUND)
                        .withUpper(MULTIPLIER_UPPER_BOUND))
                .withType(GdAdGroupType.MOBILE_CONTENT)
                .withCampaignId(cid)
                .withMinusKeywords(emptyList())
                .withRegionsInfo(nvl(geo, GROUP_GEO_REGIONS[1]))
                .withAccess(defaultGdAdGroupAccess(pid, AdGroupType.MOBILE_CONTENT, gdAdGroupStatus))
                .withStatus(gdAdGroupStatus)
                .withStoreHref(MOBILE_STORE_CONTENT_HREF)
                .withStats(StatHelper.internalStatsToOuter(getEmptyStats(), GdCampaignType.MOBILE_CONTENT));
    }

    public static GdDynamicAdGroup defaultGdDynamicGroup() {
        return defaultGdDynamicGroup(TEST_CID, TEST_GROUP_IDS[2], GROUP_GEO_REGIONS[2]);
    }

    public static GdDynamicAdGroup defaultGdDynamicGroup(Long cid, Long pid, GdAdGroupRegionsInfo geo) {
        GdAdGroupStatus gdAdGroupStatus = defaultGdAdGroupStatus();

        return new GdDynamicAdGroup()
                .withId(pid)
                .withMultipliersBounds(new GdAdGroupMultipliersBounds().withLower(MULTIPLIER_LOWER_BOUND)
                        .withUpper(MULTIPLIER_UPPER_BOUND))
                .withType(GdAdGroupType.DYNAMIC)
                .withCampaignId(cid)
                .withMinusKeywords(emptyList())
                .withRegionsInfo(nvl(geo, GROUP_GEO_REGIONS[2]))
                .withAccess(defaultGdAdGroupAccess(pid, AdGroupType.DYNAMIC, gdAdGroupStatus))
                .withStatus(gdAdGroupStatus)
                .withMainDomain(DYNAMIC_MAIN_DOMAIN)


                .withStats(StatHelper.internalStatsToOuter(getEmptyStats(), GdCampaignType.DYNAMIC));
    }

    public static GdAdGroupAccess defaultGdAdGroupAccess() {
        return defaultGdAdGroupAccess(RandomNumberUtils.nextPositiveLong());
    }

    public static GdAdGroupAccess defaultGdAdGroupAccess(Long adGroupId) {
        return defaultGdAdGroupAccess(adGroupId, AdGroupType.BASE, defaultGdAdGroupStatus());
    }

    private static GdAdGroupAccess defaultGdAdGroupAccess(Long adGroupId, AdGroupType type,
                                                          GdAdGroupStatus gdAdGroupStatus) {
        return new GdAdGroupAccess()
                .withAdGroupId(adGroupId)
                .withType(type)
                .withStatus(gdAdGroupStatus)
                .withCanEdit(true)
                .withCanCopy(AvailableAdGroupTypesCalculator.canCopyAdGroup(type, CampaignType.TEXT, Set.of()))
                .withCanEditAds(AvailableAdGroupTypesCalculator.canEditAdGroupAds(type, Set.of()))
                .withCanEditKeywords(AvailableAdGroupTypesCalculator.canEditAdGroupKeywords(type))
                .withCanEditRegions(AvailableAdGroupTypesCalculator.canEditAdGroupRegions(type))
                .withCanViewRegions(AvailableAdGroupTypesCalculator.canEditAdGroupRegions(type))
                .withCanBeSentToModerationByClient(false)
                .withShowGeneralPriceOnEdit(true)
                .withBannerActions(singleton(GdBannerAction.SAVE_AND_MODERATE));
    }

    public static GdiGroupStatus defaultGdiGroupStatus() {
        return new GdiGroupStatus()
                .withModerationStatus(GdiGroupModerationStatus.ACCEPTED)
                .withStateFlags(defaultStateFlags())
                .withPrimaryStatus(GdiGroupPrimaryStatus.ACTIVE);
    }

    public static GdAdGroupStatus defaultGdAdGroupStatus() {
        return new GdAdGroupStatus()
                .withActive(false)
                .withShowing(true)
                .withArchived(false)
                .withBsEverSynced(false)
                .withModerationStatus(GdAdGroupModerationStatus.ACCEPTED)
                .withPrimaryStatus(GdAdGroupPrimaryStatus.ACTIVE);
    }

    public static AdGroupStates defaultStateFlags() {
        return new AdGroupStates()
                .withActive(false)
                .withArchived(false)
                .withShowing(true)
                .withBsEverSynced(false)
                .withHasDraftAds(false);
    }

    public static GdiEntityStats getEmptyStats() {
        return GridStatNew.addZeros(new GdiEntityStats());
    }


    public static GdAdGroupsContainer getDefaultGdAdGroupsContainer() {
        return new GdAdGroupsContainer()
                .withFilter(new GdAdGroupFilter())
                .withOrderBy(emptyList())
                .withStatRequirements(getDefaultStatRequirements())
                .withLimitOffset(getDefaultLimitOffset());
    }

}
