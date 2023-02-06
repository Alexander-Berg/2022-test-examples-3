package ru.yandex.direct.core.testing.data;

import java.time.LocalDate;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import one.util.streamex.StreamEx;

import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargetingJoinType;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargetingMode;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.CallerReferrersAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.ClidTypesAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.ClidsAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.ContentCategoriesAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.DesktopInstalledAppsAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.DeviceIdsAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.FeaturesInPPAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.HasLCookieAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.HasPassportIdAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.InterfaceLang;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.InterfaceLangsAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.InternalNetworkAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.IsDefaultYandexSearchAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.IsPPLoggedInAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.IsVirusedAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.IsYandexPlusAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.MobileInstalledApp;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.MobileInstalledAppsAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.PlusUserSegmentsAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.QueryOptionsAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.QueryReferersAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.SearchTextAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.ShowDatesAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.SidsAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.TestIdsAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.TimeAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.UserAgentsAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.UuidsAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.YandexUidsAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.YpCookiesAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.YsCookiesAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.uatraits.model.BrowserEngine;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.uatraits.model.BrowserEnginesAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.uatraits.model.BrowserName;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.uatraits.model.BrowserNamesAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.uatraits.model.DeviceNamesAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.uatraits.model.DeviceVendor;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.uatraits.model.DeviceVendorsAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.uatraits.model.IsMobileAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.uatraits.model.IsTabletAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.uatraits.model.IsTouchAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.uatraits.model.OsFamiliesAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.uatraits.model.OsFamily;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.uatraits.model.OsName;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.uatraits.model.OsNamesAdGroupAdditionalTargeting;
import ru.yandex.direct.libs.timetarget.TimeTarget;
import ru.yandex.direct.libs.timetarget.TimeTargetUtils;

import static java.util.Arrays.asList;
import static ru.yandex.direct.core.entity.adgroupadditionaltargeting.DistribSoftConstants.DISTRIB_SOFT;
import static ru.yandex.direct.core.entity.adgroupadditionaltargeting.UaTraitsConstants.BROWSER_ENGINE_VALID_VALUES;
import static ru.yandex.direct.core.entity.adgroupadditionaltargeting.UaTraitsConstants.DEVICE_VENDOR_VALID_VALUES;
import static ru.yandex.direct.core.entity.adgroupadditionaltargeting.UaTraitsConstants.OS_FAMILY_VALID_VALUES;
import static ru.yandex.direct.core.entity.adgroupadditionaltargeting.UaTraitsConstants.OS_NAME_VALID_VALUES;
import static ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.InterfaceLang.RU;
import static ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.InterfaceLang.UK;
import static ru.yandex.direct.core.entity.retargeting.model.Goal.CONTENT_CATEGORY_UPPER_BOUND;
import static ru.yandex.direct.utils.FunctionalUtils.concat;

@SuppressWarnings("unused")
public final class TestAdGroupAdditionalTargetings {
    public static final Set<Long> SIDS = Set.of(1L, 3L);
    public static final Set<String> UUIDS = Set.of(
            "bb91c6e3d1bcb785bc8ffab48bed03e5", "c4e6538d6081e38922f2b971b6a69f29");
    public static final Set<String> DEVICE_IDS = Set.of("1A987627-2F60-4AC2-9061-06DCFA0E42AC",
            "c85406d70b8553116de4a8ade48b04fd");
    public static final Set<String> DEVICE_IDS_2 = Set.of("1A987627-2F60-4AC2-9061-06DCFA0E42AD",
            "c85406d70b8553116de4a8ade48b04ff");
    public static final Set<Long> PLUS_USER_SEGMENTS = Set.of(1L, 3L);
    public static final Set<String> SEARCH_TEXT = Set.of("коронавирус", "\\320\\272\\320\\276\\321" +
            "\\200\\320\\276\\320\\275\\320\\260\\320\\262\\320\\270\\321\\200\\321\\203\\321\\201");
    public static final List<String> YANDEX_UIDS = List.of("1021110101545123184", "2021110101545123184", "%42", "%1");
    public static final List<String> YANDEX_UIDS_2 = List.of("1021110101545123185", "2021110101545123185", "%43", "%2");
    public static final List<String> REFERERS = List.of("%yandex.com.tr%", "%harita%");
    public static final EnumSet<InterfaceLang> INTERFACE_LANGS = EnumSet.of(RU, UK);
    public static final List<String> USER_AGENTS = List.of("%Yandex%", "%YNDX-SB001%");
    public static final List<String> DEVICE_NAMES = List.of("YNDX-SB001", "YNDX-SB002");
    public static final Set<LocalDate> SHOW_DATES = Set.of(LocalDate.now(), LocalDate.now().plusYears(10));
    public static final Set<Long> DESKTOP_INSTALLED_APPS = DISTRIB_SOFT.keySet();
    public static final Set<Long> CLID_TYPES = Set.of(1L, 10L);
    public static final Set<Long> CLIDS = Set.of(100500L, 100501L);
    public static final Set<String> QUERY_OPTIONS = Set.of("test", "browser");
    public static final Set<Long> TEST_IDS = Set.of(4444L, 5555L);
    public static final Set<String> YS_COOKIES = Set.of("test", "ext");
    public static final Set<String> FEATURES_IN_PP = Set.of("opapapa", "tyctyc");
    public static final Set<String> YP_COOKIES = Set.of("test", "ext");
    public static final List<BrowserEngine> BROWSER_ENGINES = List.of(
            new BrowserEngine()
                    .withTargetingValueEntryId(Collections.min(BROWSER_ENGINE_VALID_VALUES))
                    .withMaxVersion("33.4")
                    .withMinVersion("1.0"),
            new BrowserEngine()
                    .withTargetingValueEntryId(Collections.max(BROWSER_ENGINE_VALID_VALUES)));
    public static final List<BrowserName> BROWSER_NAMES = List.of(
            new BrowserName().withTargetingValueEntryId(10L),
            new BrowserName().withTargetingValueEntryId(11L),
            new BrowserName()
                    .withTargetingValueEntryId(12L)
                    .withMaxVersion("12.4")
                    .withMinVersion("7.0"));
    public static final List<OsFamily> OS_FAMILIES = List.of(
            new OsFamily()
                    .withTargetingValueEntryId(Collections.min(OS_FAMILY_VALID_VALUES))
                    .withMaxVersion("33.4")
                    .withMinVersion("1.0"),
            new OsFamily()
                    .withTargetingValueEntryId(Collections.max(OS_FAMILY_VALID_VALUES)));
    public static final List<OsName> OS_NAMES = asList(
            new OsName().withTargetingValueEntryId(Collections.min(OS_NAME_VALID_VALUES)),
            new OsName().withTargetingValueEntryId(Collections.max(OS_NAME_VALID_VALUES)));
    public static final List<DeviceVendor> DEVICE_VENDORS = asList(
            new DeviceVendor().withTargetingValueEntryId(Collections.min(DEVICE_VENDOR_VALID_VALUES)),
            new DeviceVendor().withTargetingValueEntryId(Collections.max(DEVICE_VENDOR_VALID_VALUES)));
    public static final Set<MobileInstalledApp> MOBILE_INSTALLED_APPS = Set.of(
            new MobileInstalledApp().withStoreUrl("http://play.google.com/store/apps/details?id=ru.yandex.searchplugin")
    );
    private static final Set<Class<? extends AdGroupAdditionalTargeting>> EXCLUDED_ADDITIONAL_TARGETINGS_IN_EXCEL =
            Set.of(TimeAdGroupAdditionalTargeting.class);

    // originalTimeTarget нужно выставить null, чтобы в тестах это поле не сравнивалось
    public static final List<TimeTarget> TIME_TARGETS =
            List.of(TimeTarget.parseRawString("1A2A3A4A5A6A7A").withOriginalTimeTarget(null));

    public static final List<TimeTarget> IRRELEVANT_TIME_TARGETS =
            List.of(TimeTargetUtils.timeTarget24x7UseWorkingWeekend());

    public static HasPassportIdAdGroupAdditionalTargeting validHasPassportIdTargeting() {
        return new HasPassportIdAdGroupAdditionalTargeting()
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY);
    }

    public static IsVirusedAdGroupAdditionalTargeting validIsVirusedTargeting() {
        return new IsVirusedAdGroupAdditionalTargeting()
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY);
    }

    public static HasLCookieAdGroupAdditionalTargeting validHasLCookieTargeting() {
        return new HasLCookieAdGroupAdditionalTargeting()
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY);
    }

    public static InternalNetworkAdGroupAdditionalTargeting validInternalNetworkTargeting() {
        return new InternalNetworkAdGroupAdditionalTargeting()
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY);
    }

    public static IsMobileAdGroupAdditionalTargeting validIsMobileTargeting() {
        return new IsMobileAdGroupAdditionalTargeting()
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY);
    }

    public static IsTabletAdGroupAdditionalTargeting validIsTabletTargeting() {
        return new IsTabletAdGroupAdditionalTargeting()
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY);
    }

    public static IsTouchAdGroupAdditionalTargeting validIsTouchTargeting() {
        return new IsTouchAdGroupAdditionalTargeting()
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY);
    }

    public static YandexUidsAdGroupAdditionalTargeting validYandexUidTargeting() {
        return new YandexUidsAdGroupAdditionalTargeting()
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                .withValue(YANDEX_UIDS);
    }

    public static YandexUidsAdGroupAdditionalTargeting targetingYandexUidTargetingWithValue(List<String> yandexUids) {
        return new YandexUidsAdGroupAdditionalTargeting()
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                .withValue(yandexUids);
    }

    public static YandexUidsAdGroupAdditionalTargeting filteringYandexUidTargetingWithValue(List<String> yandexUids) {
        return new YandexUidsAdGroupAdditionalTargeting()
                .withTargetingMode(AdGroupAdditionalTargetingMode.FILTERING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                .withValue(yandexUids);
    }

    public static QueryReferersAdGroupAdditionalTargeting validQueryReferersTargeting() {
        return new QueryReferersAdGroupAdditionalTargeting()
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                .withValue(REFERERS);
    }

    public static CallerReferrersAdGroupAdditionalTargeting validCallerReferrersTargeting() {
        return new CallerReferrersAdGroupAdditionalTargeting()
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                .withValue(REFERERS);
    }

    public static InterfaceLangsAdGroupAdditionalTargeting validInterfaceLangsTargeting() {
        return new InterfaceLangsAdGroupAdditionalTargeting()
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                .withValue(INTERFACE_LANGS);
    }

    public static UserAgentsAdGroupAdditionalTargeting validUserAgentsTargeting() {
        return new UserAgentsAdGroupAdditionalTargeting()
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                .withValue(USER_AGENTS);
    }

    public static BrowserEnginesAdGroupAdditionalTargeting validBrowserEnginesTargeting() {
        return new BrowserEnginesAdGroupAdditionalTargeting()
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                .withValue(BROWSER_ENGINES);
    }

    public static BrowserNamesAdGroupAdditionalTargeting validBrowserNamesTargeting() {
        return new BrowserNamesAdGroupAdditionalTargeting()
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                .withValue(BROWSER_NAMES);
    }

    public static OsFamiliesAdGroupAdditionalTargeting validOsFamiliesTargeting() {
        return new OsFamiliesAdGroupAdditionalTargeting()
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                .withValue(OS_FAMILIES);
    }

    public static OsNamesAdGroupAdditionalTargeting validOsNamesTargeting() {
        return new OsNamesAdGroupAdditionalTargeting()
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                .withValue(OS_NAMES);
    }

    public static DeviceVendorsAdGroupAdditionalTargeting validDeviceVendorsTargeting() {
        return new DeviceVendorsAdGroupAdditionalTargeting()
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                .withValue(DEVICE_VENDORS);
    }

    public static DeviceNamesAdGroupAdditionalTargeting validDeviceNamesTargeting() {
        return new DeviceNamesAdGroupAdditionalTargeting()
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                .withValue(DEVICE_NAMES);
    }

    public static ShowDatesAdGroupAdditionalTargeting validShowDatesTargeting() {
        return new ShowDatesAdGroupAdditionalTargeting()
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                .withValue(SHOW_DATES);
    }

    public static DesktopInstalledAppsAdGroupAdditionalTargeting validDesktopInstalledAppsTargeting() {
        return new DesktopInstalledAppsAdGroupAdditionalTargeting()
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                .withValue(DESKTOP_INSTALLED_APPS);
    }

    public static ClidTypesAdGroupAdditionalTargeting validClidTypesTargeting() {
        return new ClidTypesAdGroupAdditionalTargeting()
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                .withValue(CLID_TYPES);
    }

    public static ClidsAdGroupAdditionalTargeting validClidsTargeting() {
        return new ClidsAdGroupAdditionalTargeting()
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                .withValue(CLIDS);
    }

    public static QueryOptionsAdGroupAdditionalTargeting validQueryOptionsTargeting() {
        return new QueryOptionsAdGroupAdditionalTargeting()
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                .withValue(QUERY_OPTIONS);
    }

    public static TestIdsAdGroupAdditionalTargeting validTestIdsTargeting() {
        return new TestIdsAdGroupAdditionalTargeting()
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                .withValue(TEST_IDS);
    }

    public static YsCookiesAdGroupAdditionalTargeting validYsCookiesTargeting() {
        return new YsCookiesAdGroupAdditionalTargeting()
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                .withValue(YS_COOKIES);
    }

    public static FeaturesInPPAdGroupAdditionalTargeting validFeaturesInPPTargeting() {
        return new FeaturesInPPAdGroupAdditionalTargeting()
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                .withValue(FEATURES_IN_PP);
    }

    public static YpCookiesAdGroupAdditionalTargeting validYpCookiesTargeting() {
        return new YpCookiesAdGroupAdditionalTargeting()
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                .withValue(YP_COOKIES);
    }

    public static IsYandexPlusAdGroupAdditionalTargeting validIsYandexPlusTargeting() {
        return new IsYandexPlusAdGroupAdditionalTargeting()
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY);
    }

    public static IsPPLoggedInAdGroupAdditionalTargeting validIsPPLoggedTargeting() {
        return new IsPPLoggedInAdGroupAdditionalTargeting()
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY);
    }

    public static MobileInstalledAppsAdGroupAdditionalTargeting validMobileInstalledAppsTargeting() {
        return new MobileInstalledAppsAdGroupAdditionalTargeting()
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                .withValue(MOBILE_INSTALLED_APPS);
    }

    public static IsDefaultYandexSearchAdGroupAdditionalTargeting validIsDefaultYandexSearchTargeting() {
        return new IsDefaultYandexSearchAdGroupAdditionalTargeting()
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY);
    }

    public static SidsAdGroupAdditionalTargeting validSidsTargeting() {
        return new SidsAdGroupAdditionalTargeting()
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                .withValue(SIDS);
    }

    public static UuidsAdGroupAdditionalTargeting validUuidsTargeting() {
        return new UuidsAdGroupAdditionalTargeting()
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                .withValue(UUIDS);
    }

    public static DeviceIdsAdGroupAdditionalTargeting validDeviceIdsTargeting() {
        return new DeviceIdsAdGroupAdditionalTargeting()
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                .withValue(DEVICE_IDS);
    }

    public static DeviceIdsAdGroupAdditionalTargeting targetingDeviceIdsTargetingWithValue(Set<String> deviceIds) {
        return new DeviceIdsAdGroupAdditionalTargeting()
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                .withValue(deviceIds);
    }

    public static DeviceIdsAdGroupAdditionalTargeting filteringDeviceIdsTargetingWithValue(Set<String> deviceIds) {
        return new DeviceIdsAdGroupAdditionalTargeting()
                .withTargetingMode(AdGroupAdditionalTargetingMode.FILTERING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                .withValue(deviceIds);
    }

    public static PlusUserSegmentsAdGroupAdditionalTargeting validPlusUserSegmentsTargeting() {
        return new PlusUserSegmentsAdGroupAdditionalTargeting()
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                .withValue(PLUS_USER_SEGMENTS);
    }

    public static SearchTextAdGroupAdditionalTargeting validSearchTextTargeting() {
        return new SearchTextAdGroupAdditionalTargeting()
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                .withValue(SEARCH_TEXT);
    }

    public static TimeAdGroupAdditionalTargeting validTimeTargeting() {
        return new TimeAdGroupAdditionalTargeting()
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                .withValue(TIME_TARGETS);
    }

    public static TimeAdGroupAdditionalTargeting irrelevantValidTimeTargeting() {
        return new TimeAdGroupAdditionalTargeting()
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                .withValue(IRRELEVANT_TIME_TARGETS);
    }

    public static YandexUidsAdGroupAdditionalTargeting validYandexUidFiltering() {
        return new YandexUidsAdGroupAdditionalTargeting()
                .withTargetingMode(AdGroupAdditionalTargetingMode.FILTERING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ALL)
                .withValue(List.of("5021110101545123184", "6021110101545123184"));
    }

    public static YandexUidsAdGroupAdditionalTargeting invalidYandexUidTargeting() {
        return new YandexUidsAdGroupAdditionalTargeting()
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                .withValue(List.of("8021110101545123184", "упячка"));
    }

    public static ContentCategoriesAdGroupAdditionalTargeting validContentCategoriesAdGroupAdditionalTargeting() {
        return new ContentCategoriesAdGroupAdditionalTargeting()
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                .withValue(Set.of(CONTENT_CATEGORY_UPPER_BOUND - 1));
    }

    public static List<AdGroupAdditionalTargeting> allValidInternalAdAdditionalTargetingsForExcel() {
        return StreamEx.of(allValidInternalAdAdditionalTargetings())
                .remove(t -> EXCLUDED_ADDITIONAL_TARGETINGS_IN_EXCEL.contains(t.getClass()))
                .toList();
    }

    public static List<AdGroupAdditionalTargeting> allValidInternalAdAdditionalTargetings() {
        return List.of(
                validHasPassportIdTargeting(),
                validIsVirusedTargeting(),
                validHasLCookieTargeting(),
                validInternalNetworkTargeting(),
                validIsMobileTargeting(),
                validIsTabletTargeting(),
                validIsTouchTargeting(),
                validYandexUidTargeting(),
                validQueryReferersTargeting(),
                validCallerReferrersTargeting(),
                validInterfaceLangsTargeting(),
                validUserAgentsTargeting(),
                validBrowserEnginesTargeting(),
                validBrowserNamesTargeting(),
                validOsFamiliesTargeting(),
                validOsNamesTargeting(),
                validDeviceVendorsTargeting(),
                validDeviceNamesTargeting(),
                validShowDatesTargeting(),
                validDesktopInstalledAppsTargeting(),
                validClidTypesTargeting(),
                validClidsTargeting(),
                validQueryOptionsTargeting(),
                validTestIdsTargeting(),
                validYsCookiesTargeting(),
                validFeaturesInPPTargeting(),
                validYpCookiesTargeting(),
                validIsYandexPlusTargeting(),
                validIsPPLoggedTargeting(),
                validMobileInstalledAppsTargeting(),
                validIsDefaultYandexSearchTargeting(),
                validSidsTargeting(),
                validUuidsTargeting(),
                validDeviceIdsTargeting(),
                validPlusUserSegmentsTargeting(),
                validSearchTextTargeting(),
                validTimeTargeting()
        );
    }

    public static List<AdGroupAdditionalTargeting> allIrrelevantValidInternalAdAdditionalTargetings() {
        return List.of(
                irrelevantValidTimeTargeting()
        );
    }

    public static List<AdGroupAdditionalTargeting> allValidInternalAdAdditionalTargetingsIncludingIrrelevant() {
        return concat(
                allValidInternalAdAdditionalTargetings(),
                allIrrelevantValidInternalAdAdditionalTargetings()
        );
    }
}
