package ru.yandex.direct.core.testing.data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.RandomStringUtils;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType;
import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionVideoAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmAudioAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmBannerAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmGeoPinAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmGeoproductAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmIndoorAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmOutdoorAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmVideoAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmYndxFrontpageAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CriterionType;
import ru.yandex.direct.core.entity.adgroup.model.DynSmartAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.DynamicFeedAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.DynamicTextAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.InternalAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.McBannerAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.MobileContentAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.MobileContentAdGroupDeviceTypeTargeting;
import ru.yandex.direct.core.entity.adgroup.model.MobileContentAdGroupNetworkTargeting;
import ru.yandex.direct.core.entity.adgroup.model.PerformanceAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.StatusBLGenerated;
import ru.yandex.direct.core.entity.adgroup.model.StatusModerate;
import ru.yandex.direct.core.entity.adgroup.model.StatusPostModerate;
import ru.yandex.direct.core.entity.adgroup.model.StatusShowsForecast;
import ru.yandex.direct.core.entity.adgroup.model.TextAdGroup;
import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign;
import ru.yandex.direct.core.entity.feedfilter.model.FeedFilter;
import ru.yandex.direct.core.entity.mobilecontent.model.MobileContent;
import ru.yandex.direct.core.entity.mobilecontent.model.OsType;
import ru.yandex.direct.core.entity.placements.model1.IndoorPlacement;
import ru.yandex.direct.core.entity.placements.model1.OutdoorPlacement;
import ru.yandex.direct.regions.Region;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
import static ru.yandex.direct.core.entity.adgroup.model.MobileContentAdGroupDeviceTypeTargeting.PHONE;
import static ru.yandex.direct.core.entity.adgroup.model.MobileContentAdGroupNetworkTargeting.CELLULAR;
import static ru.yandex.direct.core.entity.adgroup.service.AdGroupCpmPriceUtils.PRIORITY_DEFAULT;
import static ru.yandex.direct.core.entity.adgroup.service.AdGroupCpmPriceUtils.PRIORITY_SPECIFIC;
import static ru.yandex.direct.core.entity.adgroup.service.validation.types.CpmIndoorAdGroupValidation.INDOOR_GEO_DEFAULT;
import static ru.yandex.direct.core.entity.adgroup.service.validation.types.CpmOutdoorAdGroupValidation.OUTDOOR_GEO_DEFAULT;
import static ru.yandex.direct.core.entity.adgroup.service.validation.types.InternalAdGroupValidation.MAX_LEVEL_VALUE;
import static ru.yandex.direct.core.testing.data.TestMobileContents.defaultMobileContent;
import static ru.yandex.direct.core.testing.data.TestPlacements.placementToPageBlocks;
import static ru.yandex.direct.utils.CommonUtils.nvl;

public final class TestGroups {
    public static final EnumSet<MobileContentAdGroupDeviceTypeTargeting> DEFAULT_DEVICE_TYPE_TARGETING =
            EnumSet.of(PHONE);
    public static final EnumSet<MobileContentAdGroupNetworkTargeting> DEFAULT_NETWORK_TARGETING = EnumSet.of(CELLULAR);
    public static final List<Long> DEFAULT_GEO = List.of(Region.MOSCOW_REGION_ID);

    private static final String DEFAULT_ANDROID_STORE_URL =
            "https://play.google.com/store/apps/details?id=";
    private static final String DEFAULT_IOS_APP_STORE_URL =
            "https://itunes.apple.com/app/garageband/";
    private static final String DEFAULT_ANDROID_MIN_OSE_VERSION = "8.0";

    private static final LocalDateTime YESTERDAY = LocalDate.now().minusDays(1).atTime(0, 0);

    private TestGroups() {
    }

    public static TextAdGroup clientTextAdGroup(Long campaignId) {
        return clientTextAdGroup(campaignId, null);
    }

    public static TextAdGroup clientTextAdGroup(Long campaignId, @Nullable Long filteredFeedId) {
        TextAdGroup adGroup = new TextAdGroup()
                .withType(AdGroupType.BASE)
                .withCampaignId(campaignId)
                .withFilteredFeedId(filteredFeedId);
        if (filteredFeedId != null) {
            fillDefaultDynSmartClientExtraFields(adGroup);
        }
        fillDefaultUserFields(adGroup);
        return adGroup;
    }

    public static TextAdGroup clientTextAdGroup(
            Long campaignId, @Nullable Long feedId, @Nullable FeedFilter feedFilter) {
        TextAdGroup adGroup = new TextAdGroup()
                .withType(AdGroupType.BASE)
                .withCampaignId(campaignId)
                .withFeedId(feedId)
                .withFeedFilter(feedFilter)
                .withUsersSegments(emptyList());
        if (feedId != null) {
            fillDefaultDynSmartClientExtraFields(adGroup);
        }
        fillDefaultUserFields(adGroup);
        return adGroup;
    }

    public static CpmOutdoorAdGroup clientCpmOutdoorAdGroup(Long cid, OutdoorPlacement placement) {
        return new CpmOutdoorAdGroup()
                .withName("default outdoor ad group name")
                .withType(AdGroupType.CPM_OUTDOOR)
                .withCampaignId(cid)
                .withPageBlocks(placementToPageBlocks(placement));
    }

    public static CpmIndoorAdGroup clientCpmIndoorAdGroup(Long cid, IndoorPlacement placement) {
        return new CpmIndoorAdGroup()
                .withName("default indoor ad group name")
                .withType(AdGroupType.CPM_INDOOR)
                .withCampaignId(cid)
                .withPageBlocks(placementToPageBlocks(placement));
    }

    public static CpmYndxFrontpageAdGroup clientYndxFrontpageAdGroupForPriceSales(Long cid) {
        CpmYndxFrontpageAdGroup adGroup = new CpmYndxFrontpageAdGroup()
                .withName("default frontpage ad group for price sales name")
                .withType(AdGroupType.CPM_YNDX_FRONTPAGE)
                .withCampaignId(cid)
                .withPriority(PRIORITY_DEFAULT);
        fillDefaultUserFields(adGroup);
        return adGroup;
    }

    public static PerformanceAdGroup clientPerformanceAdGroup(Long campaignId, Long feedId) {
        var adGroup = new PerformanceAdGroup()
                .withName("Test performance group")
                .withType(AdGroupType.PERFORMANCE)
                .withCampaignId(campaignId)
                .withFeedId(feedId);
        fillDefaultDynSmartClientExtraFields(adGroup);
        fillDefaultUserFields(adGroup);
        return adGroup;
    }

    public static DynamicFeedAdGroup clientDynamicFeedAdGroup(Long campaignId, Long feedId) {
        var adGroup = new DynamicFeedAdGroup()
                .withName("Test performance group")
                .withType(AdGroupType.DYNAMIC)
                .withCampaignId(campaignId)
                .withFeedId(feedId);
        fillDefaultDynSmartClientExtraFields(adGroup);
        fillDefaultUserFields(adGroup);
        return adGroup;
    }

    public static DynamicTextAdGroup clientDynamicTextAdGroup(Long campaignId,
                                                              @Nullable Long mainDomainId,
                                                              String domainUrl) {
        var adGroup = new DynamicTextAdGroup()
                .withName("Test dynamic text group")
                .withType(AdGroupType.DYNAMIC)
                .withCampaignId(campaignId)
                .withMainDomainId(mainDomainId)
                .withDomainUrl(domainUrl);
        fillDefaultDynSmartClientExtraFields(adGroup);
        fillDefaultUserFields(adGroup);
        return adGroup;
    }

    public static AdGroup activeAdGroupByType(AdGroupType adGroupType, Long campaignId) {
        switch (adGroupType) {
            case BASE:
                return activeTextAdGroup(campaignId);
            case DYNAMIC:
                return activeDynamicTextAdGroup(campaignId);
            case PERFORMANCE:
                return activePerformanceAdGroup(campaignId);
            case MOBILE_CONTENT:
                return activeMobileAppAdGroup(campaignId);
            case MCBANNER:
                return activeMcBannerAdGroup(campaignId);
            default:
                throw new IllegalArgumentException("Неизвестный тип группы: " + adGroupType);
        }
    }

    public static TextAdGroup defaultTextAdGroup(Long cid) {
        return activeTextAdGroup(cid);
    }

    public static TextAdGroup activeTextAdGroup() {
        return activeTextAdGroup(null);
    }

    public static TextAdGroup activeTextAdGroup(Long cid) {
        return activeTextAdGroup(cid, null, null);
    }

    public static TextAdGroup activeTextAdGroup(Long cid, @Nullable Long feedId, @Nullable FeedFilter feedFilter) {
        TextAdGroup adGroup = clientTextAdGroup(cid, feedId, feedFilter);
        fillSystemFieldsForActiveAdGroup(adGroup);
        return adGroup;
    }

    public static McBannerAdGroup activeMcBannerAdGroup(Long cid) {
        McBannerAdGroup adGroup = new McBannerAdGroup();
        adGroup
                .withType(AdGroupType.MCBANNER)
                .withCampaignId(cid);
        fillDefaultUserFields(adGroup);
        fillSystemFieldsForActiveAdGroup(adGroup);
        return adGroup;
    }

    public static MobileContentAdGroup activeMobileAppAdGroup(Long cid) {
        return createMobileAppAdGroup(cid, defaultMobileContent());
    }

    /**
     * При создании адгруппы mobile content имеет значение store url, по нему определяется ОС.
     * Поэтому отдельный метод создания мобильной группы под iOS.
     */
    public static MobileContentAdGroup createIosMobAppAdGroup(Long cid, MobileContent iosMobileContent) {
        checkArgument(iosMobileContent.getOsType() == OsType.IOS);
        return createMobileAppAdGroup(cid, iosMobileContent)
                .withStoreUrl(DEFAULT_IOS_APP_STORE_URL + "id408709785");
    }

    public static MobileContentAdGroup createMobileAppAdGroup(Long cid, MobileContent mobileContent) {
        MobileContentAdGroup adGroup = new MobileContentAdGroup()
                .withType(AdGroupType.MOBILE_CONTENT)
                .withDeviceTypeTargeting(DEFAULT_DEVICE_TYPE_TARGETING)
                .withNetworkTargeting(DEFAULT_NETWORK_TARGETING)
                .withMinimalOperatingSystemVersion(DEFAULT_ANDROID_MIN_OSE_VERSION)
                .withMobileContent(mobileContent)
                .withStoreUrl(getDefaultStoreHref(mobileContent))
                .withCampaignId(cid);
        fillDefaultUserFields(adGroup);
        fillSystemFieldsForActiveAdGroup(adGroup);
        return adGroup.withMobileContentId(mobileContent.getId());
    }

    public static DynamicTextAdGroup activeDynamicTextAdGroup(Long cid) {
        var adGroup = clientDynamicTextAdGroup(cid, null, "yandex.ru");
        fillSystemFieldsForActiveAdGroup(adGroup);
        return adGroup;
    }

    public static DynamicTextAdGroup draftDynamicTextAdGroup(Long cid) {
        var adGroup = clientDynamicTextAdGroup(cid, null, "yandex.ru");
        fillSystemFieldsForDraftAdGroup(adGroup);
        return adGroup;
    }

    public static DynamicFeedAdGroup activeDynamicFeedAdGroup(Long cid, Long feedId) {
        var adGroup = clientDynamicFeedAdGroup(cid, feedId);
        fillSystemFieldsForActiveAdGroup(adGroup);
        return adGroup;
    }

    public static DynamicFeedAdGroup draftDynamicFeedAdGroup(Long cid, Long feedId) {
        var adGroup = clientDynamicFeedAdGroup(cid, feedId);
        fillSystemFieldsForDraftAdGroup(adGroup);
        return adGroup;
    }

    public static PerformanceAdGroup activePerformanceAdGroup(Long cid) {
        return activePerformanceAdGroup(cid, null);
    }

    public static PerformanceAdGroup activePerformanceAdGroup(Long cid, Long feedId) {
        var adGroup = clientPerformanceAdGroup(cid, feedId);
        fillSystemFieldsForActiveAdGroup(adGroup);
        return adGroup;
    }

    public static PerformanceAdGroup draftPerformanceAdGroup(Long cid, Long feedId) {
        var adGroup = clientPerformanceAdGroup(cid, feedId);
        fillSystemFieldsForDraftAdGroup(adGroup);
        return adGroup;
    }

    private static void fillDefaultDynSmartClientExtraFields(DynSmartAdGroup adGroup) {
        adGroup.setFieldToUseAsName("some name");
        adGroup.setFieldToUseAsBody("some body");
    }

    public static PerformanceAdGroup defaultPerformanceAdGroup(Long cid, Long feedId) {
        PerformanceAdGroup adGroup = new PerformanceAdGroup()
                .withType(AdGroupType.PERFORMANCE)
                .withCampaignId(cid)
                .withFeedId(feedId)
                .withStatusBLGenerated(StatusBLGenerated.YES)
                .withLastChange(YESTERDAY);
        fillDefaultUserFieldsWithConstants(adGroup);
        fillDefaultDynSmartClientExtraFields(adGroup);
        fillSystemFieldsForActiveAdGroupWithConstants(adGroup);
        return adGroup;
    }

    public static CpmBannerAdGroup activeCpmBannerAdGroup(Long cid) {
        CpmBannerAdGroup adGroup = new CpmBannerAdGroup();
        adGroup
                .withType(AdGroupType.CPM_BANNER)
                .withCampaignId(cid)
                .withCriterionType(CriterionType.USER_PROFILE);
        fillDefaultUserFields(adGroup);
        fillSystemFieldsForActiveAdGroup(adGroup);
        return adGroup;
    }

    public static CpmGeoproductAdGroup activeCpmGeoproductAdGroup(Long cid) {
        CpmGeoproductAdGroup adGroup = new CpmGeoproductAdGroup();
        adGroup
                .withType(AdGroupType.CPM_GEOPRODUCT)
                .withCampaignId(cid)
                .withPageGroupTags(singletonList("app-metro"))
                .withTargetTags(singletonList("app-metro"));
        fillDefaultUserFields(adGroup);
        fillSystemFieldsForActiveAdGroup(adGroup);
        return adGroup;
    }

    public static CpmGeoPinAdGroup activeCpmGeoPinAdGroup(Long cid) {
        CpmGeoPinAdGroup adGroup = new CpmGeoPinAdGroup();
        adGroup
                .withType(AdGroupType.CPM_GEO_PIN)
                .withCampaignId(cid)
                .withPageGroupTags(singletonList("geo-pin"))
                .withTargetTags(singletonList("geo-pin"));
        fillDefaultUserFields(adGroup);
        fillSystemFieldsForActiveAdGroup(adGroup);
        return adGroup;
    }

    public static CpmAudioAdGroup activeCpmAudioAdGroup(Long cid) {
        CpmAudioAdGroup adGroup = new CpmAudioAdGroup();
        adGroup
                .withType(AdGroupType.CPM_AUDIO)
                .withCampaignId(cid);
        fillDefaultUserFields(adGroup);
        fillSystemFieldsForActiveAdGroup(adGroup);
        return adGroup;
    }

    public static CpmVideoAdGroup activeCpmVideoAdGroup(Long cid) {
        CpmVideoAdGroup adGroup = new CpmVideoAdGroup();
        adGroup
                .withType(AdGroupType.CPM_VIDEO)
                .withCampaignId(cid)
                .withCriterionType(CriterionType.USER_PROFILE);
        fillDefaultUserFields(adGroup);
        fillSystemFieldsForActiveAdGroup(adGroup);
        return adGroup;
    }

    public static CpmVideoAdGroup activeNonSkippableCpmVideoAdGroup(Long cid) {
        CpmVideoAdGroup adGroup = new CpmVideoAdGroup();
        adGroup
                .withType(AdGroupType.CPM_VIDEO)
                .withIsNonSkippable(true)
                .withCampaignId(cid)
                .withCriterionType(CriterionType.USER_PROFILE);
        fillDefaultUserFields(adGroup);
        fillSystemFieldsForActiveAdGroup(adGroup);
        return adGroup;
    }

    public static CpmOutdoorAdGroup activeCpmOutdoorAdGroup(Long cid, OutdoorPlacement placement) {
        CpmOutdoorAdGroup adGroup = new CpmOutdoorAdGroup();
        adGroup
                .withType(AdGroupType.CPM_OUTDOOR)
                .withCampaignId(cid)
                .withPageBlocks(placementToPageBlocks(placement))
                .withUsersSegments(emptyList());

        fillDefaultUserFields(adGroup);
        fillSystemFieldsForActiveAdGroup(adGroup);
        adGroup.withGeo(OUTDOOR_GEO_DEFAULT);
        return adGroup;
    }

    public static CpmOutdoorAdGroup draftCpmOutdoorAdGroup(Long cid, OutdoorPlacement placement) {
        CpmOutdoorAdGroup adGroup = new CpmOutdoorAdGroup();
        adGroup
                .withType(AdGroupType.CPM_OUTDOOR)
                .withCampaignId(cid)
                .withPageBlocks(placementToPageBlocks(placement));

        fillDefaultUserFields(adGroup);
        fillSystemFieldsForDraftAdGroup(adGroup);
        adGroup.withGeo(OUTDOOR_GEO_DEFAULT);
        return adGroup;
    }

    public static CpmIndoorAdGroup activeCpmIndoorAdGroup(Long cid, IndoorPlacement placement) {
        CpmIndoorAdGroup adGroup = new CpmIndoorAdGroup();
        adGroup
                .withType(AdGroupType.CPM_INDOOR)
                .withCampaignId(cid)
                .withPageBlocks(placementToPageBlocks(placement))
                .withUsersSegments(emptyList());

        fillDefaultUserFields(adGroup);
        fillSystemFieldsForActiveAdGroup(adGroup);
        adGroup.withGeo(INDOOR_GEO_DEFAULT);
        return adGroup;
    }

    public static CpmIndoorAdGroup draftCpmIndoorAdGroup(Long cid, IndoorPlacement placement) {
        CpmIndoorAdGroup adGroup = new CpmIndoorAdGroup();
        adGroup
                .withType(AdGroupType.CPM_INDOOR)
                .withCampaignId(cid)
                .withPageBlocks(placementToPageBlocks(placement))
                .withUsersSegments(emptyList());

        fillDefaultUserFields(adGroup);
        fillSystemFieldsForDraftAdGroup(adGroup);
        adGroup.withGeo(INDOOR_GEO_DEFAULT);
        return adGroup;
    }

    public static CpmYndxFrontpageAdGroup activeCpmYndxFrontpageAdGroup(Long cid) {
        return activeCpmYndxFrontpageAdGroupWithPriority(cid, null);
    }

    public static CpmYndxFrontpageAdGroup activeCpmYndxFrontpageAdGroupWithPriority(Long cid, Long priority) {
        CpmYndxFrontpageAdGroup adGroup = new CpmYndxFrontpageAdGroup();
        adGroup.withType(AdGroupType.CPM_YNDX_FRONTPAGE)
                .withCampaignId(cid)
                .withPriority(priority)
                .withPageGroupTags(singletonList("portal-trusted"))
                .withTargetTags(singletonList("portal-trusted"));
        fillDefaultUserFields(adGroup);
        fillSystemFieldsForActiveAdGroup(adGroup);
        return adGroup;
    }

    public static CpmYndxFrontpageAdGroup activeDefaultAdGroupForPriceSales(@Nullable CpmPriceCampaign campaign) {
        return activeAdGroupForPriceSales(campaign)
                .withPriority(PRIORITY_DEFAULT);
    }

    public static CpmYndxFrontpageAdGroup activeSpecificAdGroupForPriceSales(@Nullable CpmPriceCampaign campaign) {
        return activeAdGroupForPriceSales(campaign)
                .withPriority(PRIORITY_SPECIFIC);
    }

    private static CpmYndxFrontpageAdGroup activeAdGroupForPriceSales(@Nullable CpmPriceCampaign campaign) {
        CpmYndxFrontpageAdGroup adGroup = new CpmYndxFrontpageAdGroup()
                .withType(AdGroupType.CPM_YNDX_FRONTPAGE)
                .withPageGroupTags(singletonList("portal-trusted"))
                .withTargetTags(singletonList("portal-trusted"));
        if (campaign != null) {
            adGroup
                    .withCampaignId(campaign.getId())
                    // Группа будет иметь не совсем готовый формат geo для сохранения.
                    // Пока что годилось для тестов, но фронт разваливается, в идеале здесь требуется делать все доп
                    // действия с гео, что делаются в операциях, как минимум такие на момент написания комента:
                    // adGroup.setGeo(clientGeoService.convertForSave(adGroup.getGeo(), geoTree));
                    // adGroup.setGeo(geoTree.refineGeoIds(adGroup.getGeo()));
                    .withGeo(campaign.getFlightTargetingsSnapshot().getGeoExpanded());
        }
        fillDefaultUserFields(adGroup);
        fillSystemFieldsForActiveAdGroup(adGroup);
        return adGroup;
    }

    public static CpmVideoAdGroup activeDefaultVideoAdGroupForPriceSales(@Nullable CpmPriceCampaign campaign) {
        CpmVideoAdGroup adGroup = new CpmVideoAdGroup()
                .withType(AdGroupType.CPM_VIDEO)
                .withPriority(PRIORITY_DEFAULT);
        if (campaign != null) {
            adGroup.withCampaignId(campaign.getId())
                    .withGeo(campaign.getFlightTargetingsSnapshot().getGeoExpanded());
        }
        fillDefaultUserFields(adGroup);
        fillSystemFieldsForActiveAdGroup(adGroup);
        return adGroup;
    }

    public static CpmBannerAdGroup activeCpmBannerAdGroupForPriceSales(@Nullable CpmPriceCampaign campaign) {
        CpmBannerAdGroup adGroup = new CpmBannerAdGroup()
                .withCriterionType(CriterionType.USER_PROFILE)
                .withType(AdGroupType.CPM_BANNER);
        if (campaign != null) {
            adGroup.withCampaignId(campaign.getId())
                    .withGeo(campaign.getFlightTargetingsSnapshot().getGeoExpanded());
        }
        fillDefaultUserFields(adGroup);
        fillSystemFieldsForActiveAdGroup(adGroup);
        return adGroup;
    }

    public static ContentPromotionVideoAdGroup activeContentPromotionVideoAdGroup(Long cid) {
        ContentPromotionVideoAdGroup adGroup = new ContentPromotionVideoAdGroup();
        adGroup.withType(AdGroupType.CONTENT_PROMOTION_VIDEO)
                .withCampaignId(cid)
                .withPageGroupTags(singletonList("content-promotion-video"))
                .withTargetTags(singletonList("content-promotion-video"));
        fillDefaultUserFields(adGroup);
        fillSystemFieldsForActiveAdGroup(adGroup);
        return adGroup;
    }

    public static ContentPromotionAdGroup activeContentPromotionAdGroup(
            Long cid,
            ContentPromotionAdgroupType contentPromotionAdgroupType) {
        ContentPromotionAdGroup adGroup = draftContentPromotionAdGroup(cid, contentPromotionAdgroupType);
        fillSystemFieldsForActiveAdGroup(adGroup);
        return adGroup;
    }

    public static ContentPromotionAdGroup draftContentPromotionAdGroup(
            Long cid, ContentPromotionAdgroupType contentPromotionAdgroupType
    ) {
        List<String> tags = null;
        switch (contentPromotionAdgroupType) {
            case VIDEO:
                tags = singletonList("content-promotion-video");
                break;
            case COLLECTION:
                tags = singletonList("content-promotion-collection");
                break;
            case SERVICE:
                tags = singletonList("yndx-services");
                break;
            case EDA:
                tags = singletonList("yndx-eda");
                break;
        }

        ContentPromotionAdGroup adGroup = new ContentPromotionAdGroup();
        adGroup
                .withType(AdGroupType.CONTENT_PROMOTION)
                .withCampaignId(cid)
                .withContentPromotionType(contentPromotionAdgroupType)
                .withPageGroupTags(tags)
                .withTargetTags(tags);
        fillDefaultUserFields(adGroup);
        return adGroup;
    }

    public static InternalAdGroup internalAdGroup(Long cid, Long level) {
        return internalAdGroup(cid, level, null, null);
    }

    public static InternalAdGroup internalAdGroup(Long cid, Long level, Integer rf, Integer rfReset) {
        InternalAdGroup adGroup = new InternalAdGroup();
        adGroup
                .withType(AdGroupType.INTERNAL)
                .withCampaignId(cid)
                .withLevel(level)
                .withRf(rf)
                .withRfReset(rfReset);
        fillDefaultUserFieldsWithConstants(adGroup);
        return adGroup;
    }

    public static InternalAdGroup activeInternalAdGroup(Long cid) {
        return activeInternalAdGroup(cid, (long) RandomNumberUtils.nextPositiveInteger((int) MAX_LEVEL_VALUE));
    }

    public static InternalAdGroup activeInternalAdGroup(Long cid, Long level) {
        return activeInternalAdGroup(cid, level, null, null);
    }

    public static InternalAdGroup activeInternalAdGroup(Long cid, Long level, Integer rf, Integer rfReset) {
        return activeInternalAdGroup(cid, level, rf, rfReset, null, null);
    }

    public static InternalAdGroup activeInternalAdGroup(Long cid, Long level, Integer rf, Integer rfReset,
                                                        LocalDateTime startTime, LocalDateTime finishTime) {
        return activeInternalAdGroup(cid, level, rf, rfReset, startTime, finishTime, null, null, null, null);
    }

    public static InternalAdGroup activeInternalAdGroup(Long cid, Long level, Integer rf, Integer rfReset,
                                                        LocalDateTime startTime, LocalDateTime finishTime,
                                                        Integer maxClicksCount, Integer maxClicksPeriod,
                                                        Integer maxStopsCount, Integer maxStopsPeriod) {
        InternalAdGroup adGroup = new InternalAdGroup();
        fillDefaultUserFieldsWithConstants(adGroup);
        fillSystemFieldsForActiveAdGroup(adGroup);
        return adGroup
                .withType(AdGroupType.INTERNAL)
                .withName("test internalGroup name " + RandomStringUtils.randomAlphabetic(7))
                .withCampaignId(cid)
                .withLevel(level)
                .withRf(rf)
                .withRfReset(rfReset)
                .withStartTime(startTime)
                .withFinishTime(finishTime)
                .withMaxClicksCount(maxClicksCount)
                .withMaxClicksPeriod(maxClicksPeriod)
                .withMaxStopsCount(maxStopsCount)
                .withMaxStopsPeriod(maxStopsPeriod);
    }

    public static AdGroup draftTextAdgroup(Long cid) {
        TextAdGroup adGroup = new TextAdGroup();
        adGroup
                .withType(AdGroupType.BASE)
                .withCampaignId(cid);
        fillDefaultUserFields(adGroup);
        fillSystemFieldsForDraftAdGroup(adGroup);
        return adGroup;
    }

    private static AdGroup fillDefaultUserFields(AdGroup adGroup) {
        return adGroup.withName("test group " + randomNumeric(5))
                .withMinusKeywords(emptyList())
                .withLibraryMinusKeywordsIds(emptyList())
                .withGeo(nvl(adGroup.getGeo(), singletonList(Region.RUSSIA_REGION_ID)))
                .withPageGroupTags(nvl(adGroup.getPageGroupTags(), emptyList()))
                .withTargetTags(nvl(adGroup.getTargetTags(), emptyList()));
    }

    private static void setStatusBLGeneratedIfApplicable(AdGroup adGroup, StatusBLGenerated statusBLGenerated) {
        if (!(adGroup instanceof DynSmartAdGroup)) {
            return;
        }
        var dynSmartAdGroup = (DynSmartAdGroup) adGroup;

        if (dynSmartAdGroup instanceof TextAdGroup) {
            var textAdGroup = (TextAdGroup) dynSmartAdGroup;
            if (textAdGroup.getFilteredFeedId() == null && textAdGroup.getFeedId() == null) {
                return;
            }
        }

        dynSmartAdGroup.withStatusBLGenerated(nvl(dynSmartAdGroup.getStatusBLGenerated(), statusBLGenerated));
    }

    private static void fillSystemFieldsForActiveAdGroup(AdGroup adGroup) {
        adGroup.withStatusBsSynced(StatusBsSynced.YES)
                .withStatusModerate(StatusModerate.YES)
                .withStatusPostModerate(StatusPostModerate.YES)
                .withStatusAutobudgetShow(true)
                .withStatusShowsForecast(StatusShowsForecast.SENDING)
                .withBsRarelyLoaded(false)
                .withPriorityId(1L)
                .withForecastDate(LocalDateTime.now().minusDays(1).withNano(0));
        setStatusBLGeneratedIfApplicable(adGroup, StatusBLGenerated.YES);
    }

    private static AdGroup fillDefaultUserFieldsWithConstants(AdGroup adGroup) {
        return adGroup.withName("test group name")
                .withMinusKeywords(emptyList())
                .withLibraryMinusKeywordsIds(emptyList())
                .withGeo(DEFAULT_GEO)
                .withPageGroupTags(emptyList())
                .withTargetTags(emptyList());
    }

    private static void fillSystemFieldsForActiveAdGroupWithConstants(AdGroup adGroup) {
        adGroup.withStatusBsSynced(StatusBsSynced.YES)
                .withStatusModerate(StatusModerate.YES)
                .withStatusPostModerate(StatusPostModerate.YES)
                .withStatusAutobudgetShow(true)
                .withStatusShowsForecast(StatusShowsForecast.SENDING)
                .withBsRarelyLoaded(false)
                .withPriorityId(0L)
                .withForecastDate(YESTERDAY)
                .withTrackingParams("tracking_params");
        setStatusBLGeneratedIfApplicable(adGroup, StatusBLGenerated.YES);
    }

    private static void fillSystemFieldsForDraftAdGroup(AdGroup adGroup) {
        adGroup.withStatusBsSynced(StatusBsSynced.NO)
                .withStatusModerate(StatusModerate.NEW)
                .withStatusPostModerate(StatusPostModerate.READY)
                .withStatusAutobudgetShow(true)
                .withStatusShowsForecast(StatusShowsForecast.SENDING)
                .withForecastDate(YESTERDAY);
        setStatusBLGeneratedIfApplicable(adGroup, StatusBLGenerated.NO);
    }

    @Nonnull
    public static String getDefaultStoreHref(MobileContent mobileContent) {
        return getDefaultStoreHref(mobileContent.getStoreContentId());
    }

    @Nonnull
    public static String getDefaultStoreHref(String storeContentId) {
        return DEFAULT_ANDROID_STORE_URL + storeContentId;
    }

    @Nonnull
    public static String getIosStoreHref(String storeContentId) {
        return DEFAULT_IOS_APP_STORE_URL + storeContentId;
    }
}
