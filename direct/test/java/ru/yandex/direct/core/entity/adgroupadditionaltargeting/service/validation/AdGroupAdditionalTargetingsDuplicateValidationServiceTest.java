package ru.yandex.direct.core.entity.adgroupadditionaltargeting.service.validation;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import one.util.streamex.StreamEx;
import org.hamcrest.Matcher;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargetingJoinType;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargetingMode;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AuditoriumGeoSegmentsAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.CallerReferrersAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.ClidTypesAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.ClidsAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.ContentCategoriesAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.DesktopInstalledAppsAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.DeviceIdsAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.FeaturesInPPAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.InterfaceLang;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.InterfaceLangsAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.MobileInstalledApp;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.MobileInstalledAppsAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.PlusUserSegmentsAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.QueryOptionsAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.QueryReferersAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.SearchTextAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.ShowDatesAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.SidsAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.TestIdsAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.UserAgentsAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.UuidsAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.VisitGoalsAdGroupAdditionalTargeting;
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
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.uatraits.model.OsFamiliesAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.uatraits.model.OsFamily;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.uatraits.model.OsName;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.uatraits.model.OsNamesAdGroupAdditionalTargeting;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.duplicatedObject;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(JUnitParamsRunner.class)
public class AdGroupAdditionalTargetingsDuplicateValidationServiceTest {

    private static AdGroupAdditionalTargetingsDuplicateValidationService duplicateValidationService;

    @BeforeClass
    public static void setUp() {
        duplicateValidationService = new AdGroupAdditionalTargetingsDuplicateValidationService();
    }

    private static final Long AD_GROUP_ID = 1111L;

    private static final Set<Long> SET_OF_LONGS = Set.of(1L, 2L, 3L);
    private static final Set<String> SET_OF_STRINGS = Set.of("first", "second", "third");
    private static final Set<InterfaceLang> SET_OF_INTERFACE_LANGS = Set.of(InterfaceLang.RU, InterfaceLang.UK,
            InterfaceLang.EN);
    private static final Set<MobileInstalledApp> SET_OF_MOBILE_INSTALLED_APPS = Set.of(
            new MobileInstalledApp().withMobileContentId(1L).withStoreUrl("url1"),
            new MobileInstalledApp().withMobileContentId(2L).withStoreUrl("url2"),
            new MobileInstalledApp().withMobileContentId(3L).withStoreUrl("url3")
    );
    private static final Set<LocalDate> SET_OF_LOCAL_DATE = Set.of(LocalDate.MIN, LocalDate.MAX, LocalDate.EPOCH);
    private static final List<String> LIST_OF_UNIQUE_STRINGS = List.of("first", "second", "third");
    private static final List<BrowserEngine> LIST_OF_UNIQUE_BROWSER_ENGINES = List.of(
            new BrowserEngine().withMaxVersion(null).withMinVersion(null).withTargetingValueEntryId(1L),
            new BrowserEngine().withMaxVersion(null).withMinVersion(null).withTargetingValueEntryId(2L),
            new BrowserEngine().withMaxVersion(null).withMinVersion(null).withTargetingValueEntryId(3L)
    );
    private static final List<BrowserName> LIST_OF_UNIQUE_BROWSER_NAMES = List.of(
            new BrowserName().withMaxVersion(null).withMinVersion(null).withTargetingValueEntryId(1L),
            new BrowserName().withMaxVersion(null).withMinVersion(null).withTargetingValueEntryId(2L),
            new BrowserName().withMaxVersion(null).withMinVersion(null).withTargetingValueEntryId(3L)
    );
    private static final List<DeviceVendor> LIST_OF_UNIQUE_DEVICE_VENDORS = List.of(
            new DeviceVendor().withTargetingValueEntryId(1L),
            new DeviceVendor().withTargetingValueEntryId(2L),
            new DeviceVendor().withTargetingValueEntryId(3L)
    );
    private static final List<OsFamily> LIST_OF_UNIQUE_OS_FAMILIES = List.of(
            new OsFamily().withMaxVersion(null).withMinVersion(null).withTargetingValueEntryId(1L),
            new OsFamily().withMaxVersion(null).withMinVersion(null).withTargetingValueEntryId(2L),
            new OsFamily().withMaxVersion(null).withMinVersion(null).withTargetingValueEntryId(3L)
    );
    private static final List<OsName> LIST_OF_UNIQUE_OS_NAMES = List.of(
            new OsName().withTargetingValueEntryId(1L),
            new OsName().withTargetingValueEntryId(2L),
            new OsName().withTargetingValueEntryId(3L)

    );

    public static Collection<Object[]> getTestTargetingsWithSetValues() {
        return List.of(
                testValues("AuditoriumGeoSegmentsAdGroupAdditionalTargeting", SET_OF_LONGS,
                        c -> new AuditoriumGeoSegmentsAdGroupAdditionalTargeting().withValue(c)),
                testValues("ClidTypesAdGroupAdditionalTargeting", SET_OF_LONGS,
                        c -> new ClidTypesAdGroupAdditionalTargeting().withValue(c)),
                testValues("ClidsAdGroupAdditionalTargeting", SET_OF_LONGS,
                        c -> new ClidsAdGroupAdditionalTargeting().withValue(c)),
                testValues("ContentCategoriesAdGroupAdditionalTargeting", SET_OF_LONGS,
                        c -> new ContentCategoriesAdGroupAdditionalTargeting().withValue(c)),
                testValues("DesktopInstalledAppsAdGroupAdditionalTargeting", SET_OF_LONGS,
                        c -> new DesktopInstalledAppsAdGroupAdditionalTargeting().withValue(c)),
                testValues("DeviceIdsAdGroupAdditionalTargeting", SET_OF_STRINGS,
                        c -> new DeviceIdsAdGroupAdditionalTargeting().withValue(c)),
                testValues("FeaturesInPPAdGroupAdditionalTargeting", SET_OF_STRINGS,
                        c -> new FeaturesInPPAdGroupAdditionalTargeting().withValue(c)),
                testValues("InterfaceLangsAdGroupAdditionalTargeting", SET_OF_INTERFACE_LANGS,
                        c -> new InterfaceLangsAdGroupAdditionalTargeting().withValue(c)),
                testValues("MobileInstalledAppsAdGroupAdditionalTargeting", SET_OF_MOBILE_INSTALLED_APPS,
                        c -> new MobileInstalledAppsAdGroupAdditionalTargeting().withValue(c)),
                testValues("PlusUserSegmentsAdGroupAdditionalTargeting", SET_OF_LONGS,
                        c -> new PlusUserSegmentsAdGroupAdditionalTargeting().withValue(c)),
                testValues("QueryOptionsAdGroupAdditionalTargeting", SET_OF_STRINGS,
                        c -> new QueryOptionsAdGroupAdditionalTargeting().withValue(c)),
                testValues("SearchTextAdGroupAdditionalTargeting", SET_OF_STRINGS,
                        c -> new SearchTextAdGroupAdditionalTargeting().withValue(c)),
                testValues("ShowDatesAdGroupAdditionalTargeting", SET_OF_LOCAL_DATE,
                        c -> new ShowDatesAdGroupAdditionalTargeting().withValue(c)),
                testValues("SidsAdGroupAdditionalTargeting", SET_OF_LONGS,
                        c -> new SidsAdGroupAdditionalTargeting().withValue(c)),
                testValues("TestIdsAdGroupAdditionalTargeting", SET_OF_LONGS,
                        c -> new TestIdsAdGroupAdditionalTargeting().withValue(c)),
                testValues("UuidsAdGroupAdditionalTargeting", SET_OF_STRINGS,
                        c -> new UuidsAdGroupAdditionalTargeting().withValue(c)),
                testValues("VisitGoalsAdGroupAdditionalTargeting", SET_OF_LONGS,
                        c -> new VisitGoalsAdGroupAdditionalTargeting().withValue(c)),
                testValues("YpCookiesAdGroupAdditionalTargeting", SET_OF_STRINGS,
                        c -> new YpCookiesAdGroupAdditionalTargeting().withValue(c)),
                testValues("YsCookiesAdGroupAdditionalTargeting", SET_OF_STRINGS,
                        c -> new YsCookiesAdGroupAdditionalTargeting().withValue(c))
        );
    }

    public static Collection<Object[]> getTestTargetingsWithListValues() {
        return List.of(
                testValues("QueryReferersAdGroupAdditionalTargeting", LIST_OF_UNIQUE_STRINGS,
                        c -> new QueryReferersAdGroupAdditionalTargeting().withValue(c)),
                testValues("CallerReferrersAdGroupAdditionalTargeting", LIST_OF_UNIQUE_STRINGS,
                        c -> new CallerReferrersAdGroupAdditionalTargeting().withValue(c)),
                testValues("UserAgentsAdGroupAdditionalTargeting", LIST_OF_UNIQUE_STRINGS,
                        c -> new UserAgentsAdGroupAdditionalTargeting().withValue(c)),
                testValues("YandexUidsAdGroupAdditionalTargeting", LIST_OF_UNIQUE_STRINGS,
                        c -> new YandexUidsAdGroupAdditionalTargeting().withValue(c)),
                testValues("BrowserEnginesAdGroupAdditionalTargeting", LIST_OF_UNIQUE_BROWSER_ENGINES,
                        c -> new BrowserEnginesAdGroupAdditionalTargeting().withValue(c)),
                testValues("BrowserNamesAdGroupAdditionalTargeting", LIST_OF_UNIQUE_BROWSER_NAMES,
                        c -> new BrowserNamesAdGroupAdditionalTargeting().withValue(c)),
                testValues("DeviceNamesAdGroupAdditionalTargeting", LIST_OF_UNIQUE_STRINGS,
                        c -> new DeviceNamesAdGroupAdditionalTargeting().withValue(c)),
                testValues("DeviceVendorsAdGroupAdditionalTargeting", LIST_OF_UNIQUE_DEVICE_VENDORS,
                        c -> new DeviceVendorsAdGroupAdditionalTargeting().withValue(c)),
                testValues("OsFamiliesAdGroupAdditionalTargeting", LIST_OF_UNIQUE_OS_FAMILIES,
                        c -> new OsFamiliesAdGroupAdditionalTargeting().withValue(c)),
                testValues("OsNamesAdGroupAdditionalTargeting", LIST_OF_UNIQUE_OS_NAMES,
                        c -> new OsNamesAdGroupAdditionalTargeting().withValue(c))
        );
    }

    public static Collection<Object[]> getTestTargetings() {
        return StreamEx.of(getTestTargetingsWithSetValues())
                .append(getTestTargetingsWithListValues())
                .toList();
    }

    private static <R, C extends Collection<R>> Object[] testValues(
            String targetingDescription, C values,
            Function<C, ? extends AdGroupAdditionalTargeting> targetingConstructor) {
        return new Object[]{targetingDescription, targetingConstructor, values};
    }

    private static <T extends AdGroupAdditionalTargeting> T setDefaultTargetingParams(T targeting) {
        targeting
                .withAdGroupId(AD_GROUP_ID)
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY);
        return targeting;
    }

    private static <T extends AdGroupAdditionalTargeting> T setDefaultFilteringParams(T targeting) {
        targeting
                .withAdGroupId(AD_GROUP_ID)
                .withTargetingMode(AdGroupAdditionalTargetingMode.FILTERING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ALL);
        return targeting;
    }

    private void checkByMatchers(ValidationResult<List<AdGroupAdditionalTargeting>, Defect> result,
                                 List<Matcher> matchers) {
        for (Matcher matcher : matchers) {
            assertThat(result).is(matchedBy(matcher));
        }
    }

    @Test
    @Parameters(method = "getTestTargetings")
    @TestCaseName("validate non-duplicated values for {0}")
    public void validateTargetings_forValuesWithoutDuplicates(
            @SuppressWarnings("unused") String targetingDescription,
            Function<Collection<Object>, AdGroupAdditionalTargeting> targetingConstructor,
            Collection<Object> values) {
        AdGroupAdditionalTargeting targeting = setDefaultTargetingParams(targetingConstructor.apply(values));

        ValidationResult<List<AdGroupAdditionalTargeting>, Defect> result =
                duplicateValidationService.validateTargetings(List.of(targeting));

        checkByMatchers(result, List.of(hasNoDefectsDefinitions()));
    }

    @Test
    @Parameters(method = "getTestTargetingsWithSetValues")
    @TestCaseName("validate duplicated value between different modes for {0}")
    public void validateTargetings_forSetValuesWithDuplicates(
            @SuppressWarnings("unused") String targetingDescription,
            Function<Set<Object>, AdGroupAdditionalTargeting> targetingConstructor,
            Set<Object> values) {
        Object firstElement = values.iterator().next();

        AdGroupAdditionalTargeting targeting1 = setDefaultTargetingParams(targetingConstructor.apply(values));
        AdGroupAdditionalTargeting targeting2 =
                setDefaultFilteringParams(targetingConstructor.apply(Set.of(firstElement)));

        ValidationResult<List<AdGroupAdditionalTargeting>, Defect> result =
                duplicateValidationService.validateTargetings(List.of(targeting1, targeting2));

        checkByMatchers(result, List.of(
                hasDefectWithDefinition(validationError(path(index(0), field("value")), duplicatedObject())),
                hasDefectWithDefinition(validationError(path(index(1), field("value")), duplicatedObject()))
        ));
    }

    @Test
    @Parameters(method = "getTestTargetingsWithListValues")
    @TestCaseName("validate duplicated value between different modes for {0}")
    public void validateTargetings_forListValuesWithDuplicatesInDifferentModes(
            @SuppressWarnings("unused") String targetingDescription,
            Function<List<Object>, AdGroupAdditionalTargeting> targetingConstructor,
            List<Object> values) {
        Object firstElement = values.get(0);

        AdGroupAdditionalTargeting targeting1 = setDefaultTargetingParams(targetingConstructor.apply(values));
        AdGroupAdditionalTargeting targeting2 =
                setDefaultFilteringParams(targetingConstructor.apply(List.of(firstElement)));

        ValidationResult<List<AdGroupAdditionalTargeting>, Defect> result =
                duplicateValidationService.validateTargetings(List.of(targeting1, targeting2));

        checkByMatchers(result, List.of(
                hasDefectWithDefinition(validationError(path(index(0), field("value"), index(0)),
                        duplicatedObject())),
                hasDefectWithDefinition(validationError(path(index(1), field("value"), index(0)),
                        duplicatedObject()))
        ));
    }

    @Test
    @Parameters(method = "getTestTargetingsWithListValues")
    @TestCaseName("validate duplicated value in single mode for {0}")
    public void validateTargetings_forListValuesWithDuplicatesInSingleMode(
            @SuppressWarnings("unused") String targetingDescription,
            Function<List<Object>, AdGroupAdditionalTargeting> targetingConstructor,
            List<Object> values) {
        Object firstElement = values.get(0);
        List<Object> listWithDuplicate = StreamEx.of(values)
                .prepend(firstElement)
                .toList();

        AdGroupAdditionalTargeting targeting1 =
                setDefaultTargetingParams(targetingConstructor.apply(listWithDuplicate));

        ValidationResult<List<AdGroupAdditionalTargeting>, Defect> result =
                duplicateValidationService.validateTargetings(List.of(targeting1));

        checkByMatchers(result, List.of(
                hasDefectWithDefinition(validationError(path(index(0), field("value"), index(0)),
                        duplicatedObject())),
                hasDefectWithDefinition(validationError(path(index(0), field("value"), index(1)),
                        duplicatedObject()))
        ));
    }
}
