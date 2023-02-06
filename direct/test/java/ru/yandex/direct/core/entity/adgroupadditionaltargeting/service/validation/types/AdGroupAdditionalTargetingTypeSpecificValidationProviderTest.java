package ru.yandex.direct.core.entity.adgroupadditionaltargeting.service.validation.types;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.hamcrest.Matcher;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargetingJoinType;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargetingMode;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdditionalTargetingValue;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.CallerReferrersAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.ClidTypesAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.ClidsAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.ContentCategoriesAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.DesktopInstalledAppsAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.DeviceIdsAdGroupAdditionalTargeting;
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
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.TimeAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.UserAgentsAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.UuidsAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.YandexUidsAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.YandexuidAgeAdGroupAdditionalTargeting;
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
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.libs.timetarget.TimeTarget;
import ru.yandex.direct.libs.timetarget.TimeTargetUtils;
import ru.yandex.direct.libs.timetarget.WeekdayType;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.adgroupadditionaltargeting.DistribSoftConstants.DISTRIB_PRODUCT_VALID_VALUES;
import static ru.yandex.direct.core.entity.adgroupadditionaltargeting.UaTraitsConstants.BROWSER_ENGINE_VALID_VALUES;
import static ru.yandex.direct.core.entity.adgroupadditionaltargeting.UaTraitsConstants.BROWSER_NAME_VALID_VALUES;
import static ru.yandex.direct.core.entity.adgroupadditionaltargeting.UaTraitsConstants.DEVICE_VENDOR_VALID_VALUES;
import static ru.yandex.direct.core.entity.adgroupadditionaltargeting.UaTraitsConstants.OS_FAMILY_VALID_VALUES;
import static ru.yandex.direct.core.entity.adgroupadditionaltargeting.UaTraitsConstants.OS_NAME_VALID_VALUES;
import static ru.yandex.direct.core.entity.adgroupadditionaltargeting.service.validation.AdGroupAdditionalTargetingsDefects.incorrectVersion;
import static ru.yandex.direct.core.entity.retargeting.Constants.MAX_GOALS_PER_RULE;
import static ru.yandex.direct.core.entity.retargeting.Constants.MIN_GOALS_PER_RULE;
import static ru.yandex.direct.core.entity.retargeting.model.Goal.CONTENT_CATEGORY_UPPER_BOUND;
import static ru.yandex.direct.core.entity.retargeting.model.Goal.CONTENT_GENRE_UPPER_BOUND;
import static ru.yandex.direct.core.entity.retargeting.model.Goal.CRYPTA_BEHAVIORS_UPPER_BOUND;
import static ru.yandex.direct.core.entity.retargeting.model.Goal.CRYPTA_SOCIAL_DEMO_UPPER_BOUND;
import static ru.yandex.direct.core.entity.retargeting.service.validation2.RetargetingDefects.unsupportedGoalId;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CollectionDefects.collectionSizeInInterval;
import static ru.yandex.direct.validation.defect.CollectionDefects.collectionSizeIsValid;
import static ru.yandex.direct.validation.defect.CollectionDefects.notEmptyCollection;
import static ru.yandex.direct.validation.defect.CommonDefects.invalidFormat;
import static ru.yandex.direct.validation.defect.CommonDefects.invalidValue;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.defect.NumberDefects.greaterThan;
import static ru.yandex.direct.validation.defect.NumberDefects.greaterThanOrEqualTo;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(Parameterized.class)
public class AdGroupAdditionalTargetingTypeSpecificValidationProviderTest {
    private static final Long AD_GROUP_ID = 1111L;

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();

    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private AdGroupAdditionalTargetingTypeSpecificValidationProvider adGroupAdditionalTargetingTypeSpecificValidationProvider;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> getParameters() {
        return Arrays.asList(new Object[][]{
                {
                        "незаполненный yandexuid",
                        setDefaultParams(new YandexUidsAdGroupAdditionalTargeting()),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), notNull()))
                },
                {
                        "некорректный yandexuid",
                        setDefaultParams(new YandexUidsAdGroupAdditionalTargeting())
                                .withValue(asList("8021110101545123184", "3456742345678865456789")),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), invalidValue()))
                },
                {
                        "некорректный yandexuid-маска",
                        setDefaultParams(new YandexUidsAdGroupAdditionalTargeting())
                                .withValue(singletonList("42")),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), invalidValue()))
                },
                {
                        "некорректный yandexuid-маска 3 цифры",
                        setDefaultParams(new YandexUidsAdGroupAdditionalTargeting())
                                .withValue(singletonList("%420")),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), invalidValue()))
                },
                {
                        "корректный yandexuid",
                        setDefaultParams(new YandexUidsAdGroupAdditionalTargeting())
                                .withValue(singletonList("8021110101545123184")),
                        hasNoDefectsDefinitions()
                },
                {
                        "корректный yandexuid-маска",
                        setDefaultParams(new YandexUidsAdGroupAdditionalTargeting())
                                .withValue(asList("%0", "%2", "%45")),
                        hasNoDefectsDefinitions()
                },
                {
                        "смешанный yandexuid",
                        setDefaultParams(new YandexUidsAdGroupAdditionalTargeting())
                                .withValue(asList("%0", "%2", "%45", "8021110101545123184")),
                        hasNoDefectsDefinitions()
                },
                {
                        "корректный yandexuidAge",
                        setDefaultParams(new YandexuidAgeAdGroupAdditionalTargeting())
                                .withValue(AdditionalTargetingValue.of(12)),
                        hasNoDefectsDefinitions()
                },
                {
                        "некорректный yandexuidAge - без значения",
                        setDefaultParams(new YandexuidAgeAdGroupAdditionalTargeting())
                                .withValue(null),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), notNull()))
                },
                {
                        "некорректный yandexuidAge - значение меньше минимума",
                        setDefaultParams(new YandexuidAgeAdGroupAdditionalTargeting())
                                .withValue(AdditionalTargetingValue
                                .of(YandexuidAgeTargetingValidation.MIN_AGE_VALUE - 1)),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")),
                                greaterThanOrEqualTo(YandexuidAgeTargetingValidation.MIN_AGE_VALUE)))
                },
                {
                        "незаполненная маска сайта",
                        setDefaultParams(new QueryReferersAdGroupAdditionalTargeting()),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), notNull()))
                },
                {
                        "одна пустая маска сайта",
                        setDefaultParams(new QueryReferersAdGroupAdditionalTargeting())
                                .withValue(asList("%yandex.com.tr%", "", "%harita%")),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), invalidValue()))
                },
                {
                        "корректная маска сайта",
                        setDefaultParams(new QueryReferersAdGroupAdditionalTargeting())
                                .withValue(asList("%yandex.com.tr%", "%harita%", "% %")),
                        hasNoDefectsDefinitions()
                },
                {
                        "одна маска сайта из одного символа '%'",
                        setDefaultParams(new QueryReferersAdGroupAdditionalTargeting())
                                .withValue(asList("%yandex.com.tr%", "  % ")),
                        hasDefectWithDefinition(validationError(path(index(0), field("value"), index(1)),
                                invalidFormat()))
                },
                {
                        "маска сайта без символа '%'",
                        setDefaultParams(new QueryReferersAdGroupAdditionalTargeting())
                                .withValue(asList("yandex.com.tr", "123")),
                        hasNoDefectsDefinitions()
                },
                {
                        "маска сайта из двух сиволов '%",
                        setDefaultParams(new QueryReferersAdGroupAdditionalTargeting())
                                .withValue(asList("yandex.com.tr", "%%")),
                        hasDefectWithDefinition(validationError(path(index(0), field("value"), index(1)),
                                invalidFormat()))
                },
                {
                        "маска сайта из нескольких сиволов '%",
                        setDefaultParams(new QueryReferersAdGroupAdditionalTargeting())
                                .withValue(asList("yandex.com.tr", "%%%%%")),
                        hasDefectWithDefinition(validationError(path(index(0), field("value"), index(1)),
                                invalidFormat()))
                },

                {
                        "незаполненный Referer пользователя",
                        setDefaultParams(new CallerReferrersAdGroupAdditionalTargeting()),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), notNull()))
                },
                {
                        "один пустой Referer пользователя",
                        setDefaultParams(new CallerReferrersAdGroupAdditionalTargeting())
                                .withValue(asList("%yandex.com.tr%", "", "%harita%")),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), invalidValue()))
                },
                {
                        "корректный Referer пользователя",
                        setDefaultParams(new CallerReferrersAdGroupAdditionalTargeting())
                                .withValue(asList("%yandex.com.tr%", "%harita%", "% %")),
                        hasNoDefectsDefinitions()
                },
                {
                        "один Referer пользователя из одного символа '%'",
                        setDefaultParams(new CallerReferrersAdGroupAdditionalTargeting())
                                .withValue(asList("%yandex.com.tr%", "  % ")),
                        hasDefectWithDefinition(validationError(path(index(0), field("value"), index(1)),
                                invalidFormat()))
                },
                {
                        "Referer пользователя без символа '%'",
                        setDefaultParams(new CallerReferrersAdGroupAdditionalTargeting())
                                .withValue(asList("yandex.com.tr", "123")),
                        hasNoDefectsDefinitions()
                },
                {
                        "Referer пользователя из двух сиволов '%",
                        setDefaultParams(new CallerReferrersAdGroupAdditionalTargeting())
                                .withValue(asList("yandex.com.tr", "%%")),
                        hasDefectWithDefinition(validationError(path(index(0), field("value"), index(1)),
                                invalidFormat()))
                },
                {
                        "Referer пользователя из нескольких сиволов '%",
                        setDefaultParams(new CallerReferrersAdGroupAdditionalTargeting())
                                .withValue(asList("yandex.com.tr", "%%%%%")),
                        hasDefectWithDefinition(validationError(path(index(0), field("value"), index(1)),
                                invalidFormat()))
                },

                {
                        "незаполненный язык интерфейса площадки",
                        setDefaultParams(new InterfaceLangsAdGroupAdditionalTargeting()),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), notNull()))
                },
                {
                        "пустой язык интерфейса площадки",
                        setDefaultParams(new InterfaceLangsAdGroupAdditionalTargeting())
                                .withValue(EnumSet.noneOf(InterfaceLang.class)),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), notEmptyCollection()))
                },
                {
                        "корректный язык интерфейса площадки",
                        setDefaultParams(new InterfaceLangsAdGroupAdditionalTargeting())
                                .withValue(EnumSet.allOf(InterfaceLang.class)),
                        hasNoDefectsDefinitions()
                },
                {
                        "незаполненный юзер-агент",
                        setDefaultParams(new UserAgentsAdGroupAdditionalTargeting()),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), notNull()))
                },
                {
                        "один пустой юзер-агент",
                        setDefaultParams(new UserAgentsAdGroupAdditionalTargeting())
                                .withValue(asList("%Yandex%", "", "%YNDX-SB001%")),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), invalidValue()))
                },
                {
                        "корректный юзер-агент",
                        setDefaultParams(new UserAgentsAdGroupAdditionalTargeting())
                                .withValue(asList("%Yandex%", "%YNDX-SB001%")),
                        hasNoDefectsDefinitions()
                },
                {
                        "незаполненный движок браузера",
                        setDefaultParams(new BrowserEnginesAdGroupAdditionalTargeting()),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), notNull()))
                },
                {
                        "корректный движок браузера (одно значение без версий)",
                        setDefaultParams(new BrowserEnginesAdGroupAdditionalTargeting())
                                .withValue(singletonList(
                                new BrowserEngine()
                                        .withTargetingValueEntryId(Collections.min(BROWSER_ENGINE_VALID_VALUES)))),
                        hasNoDefectsDefinitions()
                },
                {
                        "некорректный движок браузера (одно значение без версий)",
                        setDefaultParams(new BrowserEnginesAdGroupAdditionalTargeting())
                                .withValue(singletonList(
                                new BrowserEngine()
                                        .withTargetingValueEntryId(Collections.min(BROWSER_ENGINE_VALID_VALUES) - 1))),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), invalidValue()))
                },
                {
                        "корректный движок браузера (два значения без версий)",
                        setDefaultParams(new BrowserEnginesAdGroupAdditionalTargeting())
                                .withValue(asList(
                                new BrowserEngine()
                                        .withTargetingValueEntryId(Collections.min(BROWSER_ENGINE_VALID_VALUES)),
                                new BrowserEngine()
                                        .withTargetingValueEntryId(Collections.max(BROWSER_ENGINE_VALID_VALUES)))),
                        hasNoDefectsDefinitions()
                },
                {
                        "корректный движок браузера (два значения с версиями)",
                        setDefaultParams(new BrowserEnginesAdGroupAdditionalTargeting())
                                .withValue(asList(
                                new BrowserEngine()
                                        .withTargetingValueEntryId(Collections.min(BROWSER_ENGINE_VALID_VALUES))
                                        .withMinVersion("5.55").withMaxVersion("6.666"),
                                new BrowserEngine()
                                        .withTargetingValueEntryId(Collections.max(BROWSER_ENGINE_VALID_VALUES))
                                        .withMinVersion("1.1").withMaxVersion("777.777"))),
                        hasNoDefectsDefinitions()
                },
                {
                        "корректный движок браузера (один, с конкретной версией)",
                        setDefaultParams(new BrowserEnginesAdGroupAdditionalTargeting())
                                .withValue(singletonList(
                                new BrowserEngine()
                                        .withTargetingValueEntryId(Collections.max(BROWSER_ENGINE_VALID_VALUES))
                                        .withMinVersion("4.5").withMaxVersion("4.5"))),
                        hasNoDefectsDefinitions()
                },
                {
                        "корректный движок браузера (один, с версиями)",
                        setDefaultParams(new BrowserEnginesAdGroupAdditionalTargeting())
                                .withValue(singletonList(
                                new BrowserEngine()
                                        .withTargetingValueEntryId(Collections.max(BROWSER_ENGINE_VALID_VALUES))
                                        .withMinVersion("5.55").withMaxVersion("6.666"))),
                        hasNoDefectsDefinitions()
                },
                {
                        "некорректный движок браузера (неверное значение минимальной версии #1)",
                        setDefaultParams(new BrowserEnginesAdGroupAdditionalTargeting())
                                .withValue(singletonList(
                                new BrowserEngine()
                                        .withTargetingValueEntryId(Collections.max(BROWSER_ENGINE_VALID_VALUES))
                                        .withMinVersion("7777.777"))),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), incorrectVersion()))
                },
                {
                        "некорректный движок браузера (неверное значение минимальной версии #2)",
                        setDefaultParams(new BrowserEnginesAdGroupAdditionalTargeting())
                                .withValue(singletonList(
                                new BrowserEngine()
                                        .withTargetingValueEntryId(Collections.max(BROWSER_ENGINE_VALID_VALUES))
                                        .withMinVersion("777.7777"))),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), incorrectVersion()))
                },
                {
                        "некорректный движок браузера (неверное значение максимальной версии #1)",
                        setDefaultParams(new BrowserEnginesAdGroupAdditionalTargeting())
                                .withValue(singletonList(
                                new BrowserEngine()
                                        .withTargetingValueEntryId(Collections.max(BROWSER_ENGINE_VALID_VALUES))
                                        .withMaxVersion("7777.777"))),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), incorrectVersion()))
                },
                {
                        "некорректный движок браузера (неверное значение максимальной версии #2)",
                        setDefaultParams(new BrowserEnginesAdGroupAdditionalTargeting())
                                .withValue(singletonList(
                                new BrowserEngine()
                                        .withTargetingValueEntryId(Collections.max(BROWSER_ENGINE_VALID_VALUES))
                                        .withMaxVersion("777.7777"))),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), incorrectVersion()))
                },
                {
                        "некорректный движок браузера (максимальная версия меньше минимальной)",
                        setDefaultParams(new BrowserEnginesAdGroupAdditionalTargeting())
                                .withValue(singletonList(
                                new BrowserEngine()
                                        .withTargetingValueEntryId(Collections.max(BROWSER_ENGINE_VALID_VALUES))
                                        .withMinVersion("777.777").withMaxVersion("1.1"))),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), incorrectVersion()))
                },
                {
                        "незаполненный таргетинг на браузер",
                        setDefaultParams(new BrowserNamesAdGroupAdditionalTargeting()),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), notNull()))
                },
                {
                        "корректный таргетинг на браузер (одно значение без версий)",
                        setDefaultParams(new BrowserNamesAdGroupAdditionalTargeting())
                                .withValue(singletonList(
                                new BrowserName()
                                        .withTargetingValueEntryId(Collections.min(BROWSER_NAME_VALID_VALUES)))),
                        hasNoDefectsDefinitions()
                },
                {
                        "некорректный таргетинг на браузер (одно значение без версий)",
                        setDefaultParams(new BrowserNamesAdGroupAdditionalTargeting())
                                .withValue(singletonList(
                                new BrowserName()
                                        .withTargetingValueEntryId(Collections.min(BROWSER_NAME_VALID_VALUES) - 1))),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), invalidValue()))
                },
                {
                        "корректный таргетинг на браузер (два значения без версий)",
                        setDefaultParams(new BrowserNamesAdGroupAdditionalTargeting())
                                .withValue(asList(
                                new BrowserName()
                                        .withTargetingValueEntryId(Collections.min(BROWSER_NAME_VALID_VALUES)),
                                new BrowserName()
                                        .withTargetingValueEntryId(Collections.max(BROWSER_NAME_VALID_VALUES)))),
                        hasNoDefectsDefinitions()
                },
                {
                        "корректный таргетинг на браузер (два значения с версиями)",
                        setDefaultParams(new BrowserNamesAdGroupAdditionalTargeting())
                                .withValue(asList(
                                new BrowserName()
                                        .withTargetingValueEntryId(Collections.min(BROWSER_ENGINE_VALID_VALUES))
                                        .withMinVersion("5.55").withMaxVersion("6.666"),
                                new BrowserName()
                                        .withTargetingValueEntryId(Collections.max(BROWSER_ENGINE_VALID_VALUES))
                                        .withMinVersion("1.1").withMaxVersion("777.777"))),
                        hasNoDefectsDefinitions()
                },
                {
                        "корректный таргетинг на браузер (один, с конкретной версией)",
                        setDefaultParams(new BrowserNamesAdGroupAdditionalTargeting())
                                .withValue(singletonList(
                                new BrowserName()
                                        .withTargetingValueEntryId(Collections.max(BROWSER_NAME_VALID_VALUES))
                                        .withMinVersion("4.5").withMaxVersion("4.5"))),
                        hasNoDefectsDefinitions()
                },
                {
                        "корректный таргетинг на браузер (один, с версиями)",
                        setDefaultParams(new BrowserNamesAdGroupAdditionalTargeting())
                                .withValue(singletonList(
                                new BrowserName()
                                        .withTargetingValueEntryId(Collections.max(BROWSER_NAME_VALID_VALUES))
                                        .withMinVersion("5.55").withMaxVersion("6.666"))),
                        hasNoDefectsDefinitions()
                },
                {
                        "некорректный таргетинг на браузер (неверное значение минимальной версии #1)",
                        setDefaultParams(new BrowserNamesAdGroupAdditionalTargeting())
                                .withValue(singletonList(
                                new BrowserName()
                                        .withTargetingValueEntryId(Collections.max(BROWSER_NAME_VALID_VALUES))
                                        .withMinVersion("7777.777"))),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), incorrectVersion()))
                },
                {
                        "некорректный таргетинг на браузер (неверное значение минимальной версии #2)",
                        setDefaultParams(new BrowserNamesAdGroupAdditionalTargeting())
                                .withValue(singletonList(
                                new BrowserName()
                                        .withTargetingValueEntryId(Collections.max(BROWSER_NAME_VALID_VALUES))
                                        .withMinVersion("777.7777"))),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), incorrectVersion()))
                },
                {
                        "некорректный таргетинг на браузер (неверное значение максимальной версии #1)",
                        setDefaultParams(new BrowserNamesAdGroupAdditionalTargeting())
                                .withValue(singletonList(
                                new BrowserName()
                                        .withTargetingValueEntryId(Collections.max(BROWSER_NAME_VALID_VALUES))
                                        .withMaxVersion("7777.777"))),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), incorrectVersion()))
                },
                {
                        "некорректный таргетинг на браузер (неверное значение максимальной версии #2)",
                        setDefaultParams(new BrowserNamesAdGroupAdditionalTargeting())
                                .withValue(singletonList(
                                new BrowserName()
                                        .withTargetingValueEntryId(Collections.max(BROWSER_NAME_VALID_VALUES))
                                        .withMaxVersion("777.7777"))),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), incorrectVersion()))
                },
                {
                        "некорректный таргетинг на браузер (максимальная версия меньше минимальной)",
                        setDefaultParams(new BrowserNamesAdGroupAdditionalTargeting())
                                .withValue(singletonList(
                                new BrowserName()
                                        .withTargetingValueEntryId(Collections.max(BROWSER_NAME_VALID_VALUES))
                                        .withMinVersion("777.777").withMaxVersion("1.1"))),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), incorrectVersion()))
                },
                {
                        "незаполненный таргетинг на семейство ОС",
                        setDefaultParams(new OsFamiliesAdGroupAdditionalTargeting()),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), notNull()))
                },
                {
                        "корректный таргетинг на семейство ОС (одно значение без версий)",
                        setDefaultParams(new OsFamiliesAdGroupAdditionalTargeting())
                                .withValue(singletonList(
                                new OsFamily()
                                        .withTargetingValueEntryId(Collections.min(OS_FAMILY_VALID_VALUES)))),
                        hasNoDefectsDefinitions()
                },
                {
                        "некорректный таргетинг на семейство ОС (одно значение без версий)",
                        setDefaultParams(new OsFamiliesAdGroupAdditionalTargeting())
                                .withValue(singletonList(
                                new OsFamily()
                                        .withTargetingValueEntryId(Collections.min(OS_FAMILY_VALID_VALUES) - 1))),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), invalidValue()))
                },
                {
                        "корректный таргетинг на семейство ОС (два значения без версий)",
                        setDefaultParams(new OsFamiliesAdGroupAdditionalTargeting())
                                .withValue(asList(
                                new OsFamily()
                                        .withTargetingValueEntryId(Collections.min(OS_FAMILY_VALID_VALUES)),
                                new OsFamily()
                                        .withTargetingValueEntryId(Collections.max(OS_FAMILY_VALID_VALUES)))),
                        hasNoDefectsDefinitions()
                },
                {
                        "корректный таргетинг на семейство ОС (два значения с версиями)",
                        setDefaultParams(new OsFamiliesAdGroupAdditionalTargeting())
                                .withValue(asList(
                                new OsFamily()
                                        .withTargetingValueEntryId(Collections.min(OS_FAMILY_VALID_VALUES))
                                        .withMinVersion("5.55").withMaxVersion("6.666"),
                                new OsFamily()
                                        .withTargetingValueEntryId(Collections.max(OS_FAMILY_VALID_VALUES))
                                        .withMinVersion("1.1").withMaxVersion("777.777"))),
                        hasNoDefectsDefinitions()
                },
                {
                        "корректный таргетинг на семейство ОС (один, с конкретной версией)",
                        setDefaultParams(new OsFamiliesAdGroupAdditionalTargeting())
                                .withValue(singletonList(
                                new OsFamily()
                                        .withTargetingValueEntryId(Collections.max(OS_FAMILY_VALID_VALUES))
                                        .withMinVersion("4.5").withMaxVersion("4.5"))),
                        hasNoDefectsDefinitions()
                },
                {
                        "корректный таргетинг на семейство ОС (один, с версиями)",
                        setDefaultParams(new OsFamiliesAdGroupAdditionalTargeting())
                                .withValue(singletonList(
                                new OsFamily()
                                        .withTargetingValueEntryId(Collections.max(OS_FAMILY_VALID_VALUES))
                                        .withMinVersion("5.55").withMaxVersion("6.666"))),
                        hasNoDefectsDefinitions()
                },
                {
                        "некорректный таргетинг на семейство ОС (неверное значение минимальной версии #1)",
                        setDefaultParams(new OsFamiliesAdGroupAdditionalTargeting())
                                .withValue(singletonList(
                                new OsFamily()
                                        .withTargetingValueEntryId(Collections.max(OS_FAMILY_VALID_VALUES))
                                        .withMinVersion("7777.777"))),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), incorrectVersion()))
                },
                {
                        "некорректный таргетинг на семейство ОС (неверное значение минимальной версии #2)",
                        setDefaultParams(new OsFamiliesAdGroupAdditionalTargeting())
                                .withValue(singletonList(
                                new OsFamily()
                                        .withTargetingValueEntryId(Collections.max(OS_FAMILY_VALID_VALUES))
                                        .withMinVersion("777.7777"))),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), incorrectVersion()))
                },
                {
                        "некорректный таргетинг на семейство ОС (неверное значение максимальной версии #1)",
                        setDefaultParams(new OsFamiliesAdGroupAdditionalTargeting())
                                .withValue(singletonList(
                                new OsFamily()
                                        .withTargetingValueEntryId(Collections.max(OS_FAMILY_VALID_VALUES))
                                        .withMaxVersion("7777.777"))),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), incorrectVersion()))
                },
                {
                        "некорректный таргетинг на семейство ОС (неверное значение максимальной версии #2)",
                        setDefaultParams(new OsFamiliesAdGroupAdditionalTargeting())
                                .withValue(singletonList(
                                new OsFamily()
                                        .withTargetingValueEntryId(Collections.max(OS_FAMILY_VALID_VALUES))
                                        .withMaxVersion("777.7777"))),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), incorrectVersion()))
                },
                {
                        "некорректный таргетинг на семейство ОС (максимальная версия меньше минимальной)",
                        setDefaultParams(new OsFamiliesAdGroupAdditionalTargeting())
                                .withValue(singletonList(
                                new OsFamily()
                                        .withTargetingValueEntryId(Collections.max(OS_FAMILY_VALID_VALUES))
                                        .withMinVersion("777.777").withMaxVersion("1.1"))),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), incorrectVersion()))
                },
                {
                        "незаполненный таргетинг на ОС",
                        setDefaultParams(new OsNamesAdGroupAdditionalTargeting()),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), notNull()))
                },
                {
                        "корректный таргетинг на ОС",
                        setDefaultParams(new OsNamesAdGroupAdditionalTargeting())
                                .withValue(singletonList(
                                new OsName().withTargetingValueEntryId(Collections.min(OS_NAME_VALID_VALUES)))),
                        hasNoDefectsDefinitions()
                },
                {
                        "некорректный таргетинг на ОC",
                        setDefaultParams(new OsNamesAdGroupAdditionalTargeting())
                                .withValue(singletonList(
                                new OsName().withTargetingValueEntryId(Collections.min(OS_NAME_VALID_VALUES) - 1))),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), invalidValue()))
                },
                {
                        "корректный таргетинг на ОC (два значения)",
                        setDefaultParams(new OsNamesAdGroupAdditionalTargeting())
                                .withValue(asList(
                                new OsName().withTargetingValueEntryId(Collections.min(OS_NAME_VALID_VALUES)),
                                new OsName().withTargetingValueEntryId(Collections.max(OS_NAME_VALID_VALUES)))),
                        hasNoDefectsDefinitions()
                },
                {
                        "незаполненный таргетинг на производителя устройства",
                        setDefaultParams(new DeviceVendorsAdGroupAdditionalTargeting()),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), notNull()))
                },
                {
                        "корректный таргетинг на производителя устройства",
                        setDefaultParams(new DeviceVendorsAdGroupAdditionalTargeting())
                                .withValue(singletonList(
                                new DeviceVendor().withTargetingValueEntryId(Collections.min(DEVICE_VENDOR_VALID_VALUES)))),
                        hasNoDefectsDefinitions()
                },
                {
                        "некорректный таргетинг на производителя устройства",
                        setDefaultParams(new DeviceVendorsAdGroupAdditionalTargeting())
                                .withValue(singletonList(
                                new DeviceVendor().withTargetingValueEntryId(Collections.min(DEVICE_VENDOR_VALID_VALUES) - 1))),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), invalidValue()))
                },
                {
                        "корректный таргетинг на производителя устройства (два значения)",
                        setDefaultParams(new DeviceVendorsAdGroupAdditionalTargeting())
                                .withValue(asList(
                                new DeviceVendor().withTargetingValueEntryId(Collections.min(DEVICE_VENDOR_VALID_VALUES)),
                                new DeviceVendor().withTargetingValueEntryId(Collections.max(DEVICE_VENDOR_VALID_VALUES)))),
                        hasNoDefectsDefinitions()
                },
                {
                        "незаполненный таргетинг на название устройства",
                        setDefaultParams(new DeviceNamesAdGroupAdditionalTargeting()),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), notNull()))
                },
                {
                        "корректный таргетинг на название устройства",
                        setDefaultParams(new DeviceNamesAdGroupAdditionalTargeting())
                                .withValue(singletonList("yndx-sb001")),
                        hasNoDefectsDefinitions()
                },
                {
                        "незаполненный таргетинг на установленность приложений",
                        setDefaultParams(new DesktopInstalledAppsAdGroupAdditionalTargeting()),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), notNull()))
                },
                {
                        "некорректный таргетинг на установленность приложений",
                        setDefaultParams(new DesktopInstalledAppsAdGroupAdditionalTargeting()
                                .withValue(Set.of(Collections.max(DISTRIB_PRODUCT_VALID_VALUES) + 1))),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), invalidValue()))
                },
                {
                        "корректный таргетинг на установленность приложений",
                        setDefaultParams(new DesktopInstalledAppsAdGroupAdditionalTargeting()
                                .withValue(Set.of(Collections.max(DISTRIB_PRODUCT_VALID_VALUES)))),
                        hasNoDefectsDefinitions()
                },
                {
                        "незаполненный таргетинг на типы clid'ов",
                        setDefaultParams(new ClidTypesAdGroupAdditionalTargeting()),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), notNull()))
                },
                {
                        "некорректный таргетинг на типы clid'ы",
                        setDefaultParams(new ClidsAdGroupAdditionalTargeting()
                                .withValue(Set.of(-10L))),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), greaterThan(0L)))
                },
                {
                        "корректный таргетинг на типы clid'ов",
                        setDefaultParams(new ClidTypesAdGroupAdditionalTargeting()
                                .withValue(Set.of(1L, 10L))),
                        hasNoDefectsDefinitions()
                },
                {
                        "незаполненный таргетинг на clid'ы",
                        setDefaultParams(new ClidsAdGroupAdditionalTargeting()),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), notNull()))
                },
                {
                        "некорректный таргетинг на clid'ы",
                        setDefaultParams(new ClidsAdGroupAdditionalTargeting()
                                .withValue(Set.of(-10L))),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), greaterThan(0L)))
                },
                {
                        "корректный таргетинг на clid'ы",
                        setDefaultParams(new ClidsAdGroupAdditionalTargeting()
                                .withValue(Set.of(123L, 73526L))),
                        hasNoDefectsDefinitions()
                },
                {
                        "незаполненный таргетинг на опции",
                        setDefaultParams(new QueryOptionsAdGroupAdditionalTargeting()),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), notNull()))
                },
                {
                        "некорректный таргетинг на опции",
                        setDefaultParams(new QueryOptionsAdGroupAdditionalTargeting()
                                .withValue(Set.of(""))),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), invalidValue()))
                },
                {
                        "корректный таргетинг на опции",
                        setDefaultParams(new QueryOptionsAdGroupAdditionalTargeting()
                                .withValue(Set.of("browser"))),
                        hasNoDefectsDefinitions()
                },
                {
                        "незаполненный таргетинг на id экспериментов",
                        setDefaultParams(new TestIdsAdGroupAdditionalTargeting()),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), notNull()))
                },
                {
                        "некорректный таргетинг на id экспериментов",
                        setDefaultParams(new TestIdsAdGroupAdditionalTargeting()
                                .withValue(Set.of(-100500L))),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), greaterThan(0L)))
                },
                {
                        "корректный таргетинг на id экспериментов",
                        setDefaultParams(new TestIdsAdGroupAdditionalTargeting()
                                .withValue(Set.of(100500L, 100501L))),
                        hasNoDefectsDefinitions()
                },
                {
                        "незаполненный таргетинг SID - null",
                        setDefaultParams(new SidsAdGroupAdditionalTargeting()),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), notNull()))
                },
                {
                        "незаполненный таргетинг SID - пустой набор значений",
                        setDefaultParams(new SidsAdGroupAdditionalTargeting().withValue(Set.of())),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), notEmptyCollection()))
                },
                {
                        "некорректный таргетинг SID",
                        setDefaultParams(new SidsAdGroupAdditionalTargeting()
                                .withValue(Set.of(-1L))),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), greaterThan(0L)))
                },
                {
                        "корректный таргетинг SID",
                        setDefaultParams(new SidsAdGroupAdditionalTargeting()
                                .withValue(Set.of(1L, 2L))),
                        hasNoDefectsDefinitions()
                },
                {
                        "незаполненный таргетинг UUID - null",
                        setDefaultParams(new UuidsAdGroupAdditionalTargeting()),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), notNull()))
                },
                {
                        "незаполненный таргетинг UUID - пустой набор значений",
                        setDefaultParams(new UuidsAdGroupAdditionalTargeting().withValue(Set.of())),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), notEmptyCollection()))
                },
                {
                        "некорректный таргетинг UUID",
                        setDefaultParams(new UuidsAdGroupAdditionalTargeting()
                                .withValue(Set.of(""))),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), invalidValue()))
                },
                {
                        "некорректный таргетинг UUID - пробелы",
                        setDefaultParams(new UuidsAdGroupAdditionalTargeting()
                                .withValue(Set.of("22 22"))),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), invalidValue()))
                },
                {
                        "некорректный таргетинг UUID - запятые",
                        setDefaultParams(new UuidsAdGroupAdditionalTargeting()
                                .withValue(Set.of("22,22"))),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), invalidValue()))
                },
                {
                        "корректный таргетинг UUID",
                        setDefaultParams(new UuidsAdGroupAdditionalTargeting()
                                .withValue(Set.of(
                                        "bb91c6e3d1bcb785bc8ffab48bed03e5", "c4e6538d6081e38922f2b971b6a69f29"))),
                        hasNoDefectsDefinitions()
                },
                {
                        "незаполненный таргетинг DeviceId - null",
                        setDefaultParams(new DeviceIdsAdGroupAdditionalTargeting()),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), notNull()))
                },
                {
                        "незаполненный таргетинг DeviceId - пустой набор значений",
                        setDefaultParams(new DeviceIdsAdGroupAdditionalTargeting().withValue(Set.of())),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), notEmptyCollection()))
                },
                {
                        "некорректный таргетинг DeviceId",
                        setDefaultParams(new DeviceIdsAdGroupAdditionalTargeting()
                                .withValue(Set.of(""))),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), invalidValue()))
                },
                {
                        "некорректный таргетинг DeviceId - пробелы",
                        setDefaultParams(new DeviceIdsAdGroupAdditionalTargeting()
                                .withValue(Set.of("22 22"))),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), invalidValue()))
                },
                {
                        "некорректный таргетинг DeviceId - запятые",
                        setDefaultParams(new DeviceIdsAdGroupAdditionalTargeting()
                                .withValue(Set.of("22,22"))),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), invalidValue()))
                },
                {
                        "корректный таргетинг DeviceId",
                        setDefaultParams(new DeviceIdsAdGroupAdditionalTargeting()
                                .withValue(Set.of(
                                        "1A987627-2F60-4AC2-9061-06DCFA0E42AC",
                                        "c85406d70b8553116de4a8ade48b04fd",
                                        "69ecf662-a00e-4fe8-9a93-424de74f1b24"))),
                        hasNoDefectsDefinitions()
                },
                {
                        "незаполненный таргетинг Сегменты Плюса - null",
                        setDefaultParams(new PlusUserSegmentsAdGroupAdditionalTargeting()),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), notNull()))
                },
                {
                        "незаполненный таргетинг Сегменты Плюса - пустой набор значений",
                        setDefaultParams(new PlusUserSegmentsAdGroupAdditionalTargeting().withValue(Set.of())),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), notEmptyCollection()))
                },
                {
                        "некорректный таргетинг Сегменты Плюса",
                        setDefaultParams(new PlusUserSegmentsAdGroupAdditionalTargeting()
                                .withValue(Set.of(-2L))),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), greaterThan(0L)))
                },
                {
                        "корректный таргетинг Сегменты Плюса",
                        setDefaultParams(new PlusUserSegmentsAdGroupAdditionalTargeting()
                                .withValue(Set.of(1L, 2L))),
                        hasNoDefectsDefinitions()
                },
                {
                        "незаполненный таргетинг Текст поискового запроса - null",
                        setDefaultParams(new SearchTextAdGroupAdditionalTargeting()),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), notNull()))
                },
                {
                        "незаполненный таргетинг Текст поискового запроса - пустой набор значений",
                        setDefaultParams(new SearchTextAdGroupAdditionalTargeting().withValue(Set.of())),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), notEmptyCollection()))
                },
                {
                        "некорректный таргетинг Текст поискового запроса",
                        setDefaultParams(new SearchTextAdGroupAdditionalTargeting()
                                .withValue(Set.of(" "))),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), invalidValue()))
                },
                {
                        "корректный таргетинг Текст поискового запроса",
                        setDefaultParams(new SearchTextAdGroupAdditionalTargeting()
                                .withValue(Set.of("коронавирус", "\\320\\272\\320\\276\\321\\200\\320\\276\\320\\275" +
                                        "\\320\\260\\320\\262\\320\\270\\321\\200\\321\\203\\321\\201"))),
                        hasNoDefectsDefinitions()
                },
                {
                        "незаполненный таргетинг на содержимое YS куки",
                        setDefaultParams(new YsCookiesAdGroupAdditionalTargeting()),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), notNull()))
                },
                {
                        "некорректный таргетинг на содержимое YS куки",
                        setDefaultParams(new YsCookiesAdGroupAdditionalTargeting()
                                .withValue(Set.of(""))),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), invalidValue()))
                },
                {
                        "корректный таргетинг на содержимое YS куки",
                        setDefaultParams(new YsCookiesAdGroupAdditionalTargeting()
                                .withValue(Set.of("desktop", "yabrowser"))),
                        hasNoDefectsDefinitions()
                },
                {
                        "незаполненная дата показа",
                        setDefaultParams(new ShowDatesAdGroupAdditionalTargeting()),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), notNull()))
                },
                {
                        "корректная дата показа",
                        setDefaultParams(new ShowDatesAdGroupAdditionalTargeting())
                                .withValue(Set.of(LocalDate.now())),
                        hasNoDefectsDefinitions()
                },
                {
                        "незаполненный таргетинг на установленность мобильных приложений",
                        setDefaultParams(new MobileInstalledAppsAdGroupAdditionalTargeting()),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), notNull()))
                },
                {
                        "пустой таргетинг на установленность мобильных приложений",
                        setDefaultParams(new MobileInstalledAppsAdGroupAdditionalTargeting())
                                .withValue(emptySet()),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), notEmptyCollection()))
                },
                {
                        "незаполененное значение в таргетинге на установленность мобильных приложений",
                        setDefaultParams(new MobileInstalledAppsAdGroupAdditionalTargeting())
                                .withValue(Set.of(new MobileInstalledApp().withStoreUrl(null))),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), invalidValue()))
                },
                {
                        "пустая строка в таргетинге на установленность мобильных приложений",
                        setDefaultParams(new MobileInstalledAppsAdGroupAdditionalTargeting())
                                .withValue(Set.of(new MobileInstalledApp().withStoreUrl(""))),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), invalidValue()))
                },
                {
                        "некорректная ссылка в таргетинге на установленность мобильных приложений",
                        setDefaultParams(new MobileInstalledAppsAdGroupAdditionalTargeting())
                                .withValue(Set.of(new MobileInstalledApp().withStoreUrl("test"))),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), invalidValue()))
                },
                {
                        "ссылка на некорректный магазин в таргетинге на установленность мобильных приложений",
                        setDefaultParams(new MobileInstalledAppsAdGroupAdditionalTargeting())
                                .withValue(Set.of(new MobileInstalledApp()
                                .withStoreUrl("https://www.amazon.com/Yandex-LLC-Yandex-Maps/dp/B00R7JHHJY/ref=sr_1_1" +
                                        "?keywords=yandex&qid=1562941064&s=gateway&sr=8-1"))),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), invalidValue()))
                },
                {
                        "некорректная ссылка на приложение в таргетинге на установленность мобильных приложений",
                        setDefaultParams(new MobileInstalledAppsAdGroupAdditionalTargeting())
                                .withValue(Set.of(new MobileInstalledApp()
                                .withStoreUrl("https://play.google.com/store/apps/details ?id=ru.yandex" +
                                        ".yandexmaps&hl=en"))),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), invalidValue()))
                },
                {
                        "корретный таргетинг на установленность мобильных приложений",
                        setDefaultParams(new MobileInstalledAppsAdGroupAdditionalTargeting())
                                .withValue(Set.of(new MobileInstalledApp()
                                .withStoreUrl("https://play.google.com/store/apps/details?id=ru.yandex" +
                                        ".yandexmaps&hl=en"))),
                        hasNoDefectsDefinitions()
                },
                {
                        "некорретный таргетинг на установленность мобильных приложений с одной валидной и одной " +
                                "невалидной ссылками",
                        setDefaultParams(new MobileInstalledAppsAdGroupAdditionalTargeting())
                                .withValue(Set.of(
                                new MobileInstalledApp().withStoreUrl("https://play.google.com" +
                                        "/store/apps/details?id=ru.yandex.yandexmaps&hl=en"),
                                new MobileInstalledApp().withStoreUrl("https://apps.apple.com/ru/app/яндекс-карты"))),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), invalidValue()))
                },
                {
                        "корретный таргетинг на установленность мобильных приложений с двумя ссылками",
                        setDefaultParams(new MobileInstalledAppsAdGroupAdditionalTargeting())
                                .withValue(Set.of(
                                new MobileInstalledApp().withStoreUrl("https://play.google.com" +
                                        "/store/apps/details?id=ru.yandex.yandexmaps&hl=en"),
                                new MobileInstalledApp().withStoreUrl("https://apps.apple.com" +
                                        "/ru/app/яндекс-карты/id313877526"))),
                        hasNoDefectsDefinitions()
                },
                {
                        "корретный таргетинг на жанры",
                        setDefaultParams(new ContentCategoriesAdGroupAdditionalTargeting())
                                .withValue(Set.of(CONTENT_CATEGORY_UPPER_BOUND - 1, CONTENT_GENRE_UPPER_BOUND - 1)),
                        hasNoDefectsDefinitions()
                },
                {
                        "некорретный таргетинг на жанры с goal_id из недопустимого диапазона",
                        setDefaultParams(new ContentCategoriesAdGroupAdditionalTargeting())
                                .withValue(Set.of(CRYPTA_SOCIAL_DEMO_UPPER_BOUND - 5,
                                CRYPTA_BEHAVIORS_UPPER_BOUND - 1)),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")), unsupportedGoalId()))
                },
                {
                        "некорретный таргетинг на жанры с пустым списком goal_id",
                        setDefaultParams(new ContentCategoriesAdGroupAdditionalTargeting())
                                .withValue(emptySet()),
                        hasDefectWithDefinition(validationError(path(index(0), field("value")),
                                collectionSizeInInterval(MIN_GOALS_PER_RULE, MAX_GOALS_PER_RULE)))
                },
                {
                        "некорретный таргетинг на жанры с JOIN_TYPE = ALL",
                        setDefaultParams(new ContentCategoriesAdGroupAdditionalTargeting())
                                .withJoinType(AdGroupAdditionalTargetingJoinType.ALL),
                        hasDefectWithDefinition(validationError(path(index(0),
                                field(AdGroupAdditionalTargeting.JOIN_TYPE)), invalidValue()))
                },
                {
                        "некорретный таргетинг на жанры с TARGETING_MODE = FILTERING",
                        setDefaultParams(new ContentCategoriesAdGroupAdditionalTargeting())
                                .withTargetingMode(AdGroupAdditionalTargetingMode.FILTERING),
                        hasDefectWithDefinition(validationError(path(index(0),
                                field(AdGroupAdditionalTargeting.TARGETING_MODE)), invalidValue()))
                },
                {
                        "корректный TimeTarget",
                        setDefaultParams(new TimeAdGroupAdditionalTargeting())
                                .withValue(List.of(TimeTargetUtils.timeTarget24x7()))
                                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY),
                        hasNoDefectsDefinitions()
                },
                {
                        "некорректный TimeTarget пустой",
                        setDefaultParams(new TimeAdGroupAdditionalTargeting())
                                .withValue(List.of())
                                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY),
                        hasDefectWithDefinition(validationError(path(index(0),
                                field("value")), collectionSizeIsValid(1, 1)))
                },
                {
                        "некорректный TimeTarget больше одного",
                        setDefaultParams(new TimeAdGroupAdditionalTargeting())
                                .withValue(List.of(TimeTargetUtils.timeTarget24x7(), TimeTargetUtils.timeTarget24x7()))
                                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY),
                        hasDefectWithDefinition(validationError(path(index(0),
                                field("value")), collectionSizeIsValid(1, 1)))
                },
                {
                        "некорректный TimeTarget без коэффициентов",
                        setDefaultParams(new TimeAdGroupAdditionalTargeting())
                                .withValue(List.of(new TimeTarget()))
                                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY),
                        hasDefectWithDefinition(validationError(path(index(0),
                                field("value")), invalidValue()))
                },
                {
                        "некорректный TimeTarget c мультипликаторами",
                        setDefaultParams(new TimeAdGroupAdditionalTargeting())
                                .withValue(List.of(
                                        TimeTargetUtils.timeTarget24x7().copy().withHourCoef(WeekdayType.FRIDAY, 4, 20)
                                ))
                                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY),
                        hasDefectWithDefinition(validationError(path(index(0),
                                field("value")), invalidValue()))
                },
                {
                        "некорректный TimeTarget с JOIN_TYPE = ALL",
                        setDefaultParams(new TimeAdGroupAdditionalTargeting())
                                .withValue(List.of(TimeTargetUtils.timeTarget24x7()))
                                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                                .withJoinType(AdGroupAdditionalTargetingJoinType.ALL),
                        hasDefectWithDefinition(validationError(path(index(0),
                                field(AdGroupAdditionalTargeting.JOIN_TYPE)), invalidValue()))
                },
                {
                        "некорректный TimeTarget с TARGETING_MODE = FILTERING",
                        setDefaultParams(new TimeAdGroupAdditionalTargeting())
                                .withValue(List.of(TimeTargetUtils.timeTarget24x7()))
                                .withTargetingMode(AdGroupAdditionalTargetingMode.FILTERING)
                                .withJoinType(AdGroupAdditionalTargetingJoinType.ALL),
                        hasDefectWithDefinition(validationError(path(index(0),
                                field(AdGroupAdditionalTargeting.TARGETING_MODE)), invalidValue()))
                },
        });
    }

    @Parameterized.Parameter
    public String testDescription;

    @Parameterized.Parameter(1)
    public AdGroupAdditionalTargeting targeting;

    @Parameterized.Parameter(2)
    public Matcher matcher;

    private static <T extends AdGroupAdditionalTargeting> T setDefaultParams(T targeting) {
        targeting
                .withAdGroupId(AD_GROUP_ID)
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY);
        return targeting;
    }

    @Test
    public void validateTargeting_test() {
        ValidationResult<List<AdGroupAdditionalTargeting>, Defect> actual =
                adGroupAdditionalTargetingTypeSpecificValidationProvider.validateTargetings(singletonList(targeting));

        assertThat(actual).is(matchedBy(matcher));
    }
}
