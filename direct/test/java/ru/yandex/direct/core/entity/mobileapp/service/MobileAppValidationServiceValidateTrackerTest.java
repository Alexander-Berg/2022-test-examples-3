package ru.yandex.direct.core.entity.mobileapp.service;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefectIds;
import ru.yandex.direct.core.entity.mobileapp.MobileAppDefects;
import ru.yandex.direct.core.entity.mobileapp.model.MobileAppTracker;
import ru.yandex.direct.core.entity.mobileapp.model.MobileAppTrackerTrackingSystem;
import ru.yandex.direct.core.entity.mobileapp.repository.MobileAppRepository;
import ru.yandex.direct.core.entity.mobilecontent.model.OsType;
import ru.yandex.direct.core.entity.mobilecontent.service.MobileContentService;
import ru.yandex.direct.core.entity.trustedredirects.service.TrustedRedirectsService;
import ru.yandex.direct.core.entity.uac.service.trackingurl.TrackingUrlParseService;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.validation.defect.ids.CollectionDefectIds;
import ru.yandex.direct.validation.defect.ids.StringDefectIds;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.DefectIds;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static ru.yandex.direct.core.entity.mobileapp.service.MobileAppValidationService.MAX_TRACKER_URL_LENGTH;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoErrorsAndWarnings;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(JUnitParamsRunner.class)
public class MobileAppValidationServiceValidateTrackerTest {
    private static final String INVALID_TRACKER_URL = "it's not URL :(";
    static final String VALID_TRACKER_URL =
            "https://some-tracker-url.ru/hdvKP92aPib?%24$3p=a_yandex_direct&~click_id={LOGID}&~android_id={googleaid}&~os_ifa={iosifa}";
    private static final String TRACKER_URL_WITHOUT_LOGID = "http://some-tracker-url.ru/?{ios_ifa}-{google_aid}";
    private static final String TRACKER_URL_WITHOUT_LOGID_FOR_ADJUST =
            "http://app.adjust.com/dk93w7?{ios_ifa}-{google_aid}";
    private static final String TRACKER_IMPRESSION_URL_WITHOUT_LOGID_FOR_ADJUST =
            "https://view.adjust.com/impression/dk93w7?{ios_ifa}-{google_aid}";
    private static final String VALID_APPMETRICA_TRACKING_URL =
            "https://redirect.appmetrica.yandex.com/serve/1178225855864813589?click_id={logid}&ios_ifa={ios_ifa}&google_aid={google_aid}";

    private static final String VALID_MAIL_RU_TRACKING_URL =
            "https://trk.mail.ru/c/uh0jz9?mt_gaid={google_aid}&logid={LOGID}";

    private static final String VALID_MAIL_RU_IMPRESSION_URL =
            "https://trk.mail.ru/i/uh0jz9?mt_gaid={google_aid}&logid={LOGID}";

    private static final String VALID_MAIL_RU_IMPRESSION_URL_WITH_ANOTHER_TRACKER_ID =
            "https://trk.mail.ru/i/aaaaaa?mt_gaid={google_aid}&logid={LOGID}";

    private static final Map<OsType, String> URL_WITHOUT_OS_SPECIFIC_MACROS = ImmutableMap.of(
            OsType.ANDROID, "http://ya.ru/{logid}-{ios_ifa}",
            OsType.IOS, "http://ya.ru/{logid}-{google_aid}"
    );

    private static final Set<OsType> ALL_OS_TYPES = EnumSet.allOf(OsType.class);
    private static final Set<MobileAppTrackerTrackingSystem> ALL_TRACKING_SYSTEMS =
            EnumSet.allOf(MobileAppTrackerTrackingSystem.class);

    private MobileAppValidationService validationService;

    public static List<OsType> for_AnyOsType() {
        return ImmutableList.copyOf(ALL_OS_TYPES);
    }

    public static Collection<List<Object>> for_AnyOsType_AnyTrackersExceptOther() {
        return Sets.cartesianProduct(
                ALL_OS_TYPES,
                Sets.difference(ALL_TRACKING_SYSTEMS, singleton(MobileAppTrackerTrackingSystem.OTHER))
        );
    }

    public static Collection<List<Object>> for_AnyOsType_AnyTrackersExceptAdjustAndOther() {
        return Sets.cartesianProduct(
                ALL_OS_TYPES,
                Sets.difference(ALL_TRACKING_SYSTEMS,
                        ImmutableSet.of(MobileAppTrackerTrackingSystem.ADJUST, MobileAppTrackerTrackingSystem.OTHER))
        );
    }

    public static Collection<List<Object>> for_AnyOsType_LogIdInDifferentCases() {
        return Sets.cartesianProduct(
                ALL_OS_TYPES,
                ImmutableSet.of("{logid}", "{LOGID}", "{LoGiD}")
        );
    }

    public static List<List<Object>> for_AnyOsType_AnyValidUserParams() {
        return Lists.cartesianProduct(
                ImmutableList.copyOf(ALL_OS_TYPES),
                ImmutableList.of(
                        emptyList(),
                        singletonList("0-9a_zA_Z$%~"),
                        singletonList(StringUtils.repeat("x", MobileAppValidationService.MAX_USER_PARAM_LENGTH)),
                        Collections.nCopies(MobileAppValidationService.MAX_USER_PARAM_SIZE, "x")
                )
        );
    }

    @Mock
    private TrustedRedirectsService trustedRedirectsService;

    @Mock
    private ShardHelper shardHelper;

    @Mock
    private MobileAppRepository mobileAppRepository;

    @Mock
    private MobileContentService mobileContentService;

    @Mock
    private TrackingUrlParseService trackingUrlParseService;

    @Before
    public void before() {
        initMocks(this);
        when(trustedRedirectsService.checkTrackingHref(anyString())).thenReturn(TrustedRedirectsService.Result.TRUSTED);
        when(trustedRedirectsService.checkImpressionUrl(anyString())).thenReturn(TrustedRedirectsService.Result.TRUSTED);
        validationService = new MobileAppValidationService(trustedRedirectsService, shardHelper, mobileAppRepository,
                mobileContentService, trackingUrlParseService);
    }

    @Test
    @Parameters(method = "for_AnyOsType")
    public void validateMobileAppTracker_AllFilled_NoErrors(OsType osType) {
        MobileAppTracker tracker = createValidTracker();
        ValidationResult<MobileAppTracker, Defect> result =
                validationService.validateMobileAppTracker(tracker, osType);
        assertThat(result).is(matchedBy(hasNoErrorsAndWarnings()));
    }

    @Test
    @Parameters(method = "for_AnyOsType")
    public void validateMobileAppTracker_WithNullUrl_ErrorOnUrl(OsType osType) {
        MobileAppTracker tracker = createValidTracker().withUrl(null);
        ValidationResult<MobileAppTracker, Defect> result = validationService.validateMobileAppTracker(tracker, osType);
        assertThat(result).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(field("url")), DefectIds.CANNOT_BE_NULL))));
    }

    @Test
    @Parameters(method = "for_AnyOsType")
    public void validateMobileAppTracker_WithNullImpressionUrl_ErrorOnImpressionUrl(OsType osType) {
        MobileAppTracker tracker = createValidTracker().withImpressionUrl(null);
        ValidationResult<MobileAppTracker, Defect> result = validationService.validateMobileAppTracker(tracker, osType);
        assertThat(result).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(field("impressionUrl")), DefectIds.CANNOT_BE_NULL))));
    }

    @Test
    @Parameters(method = "for_AnyOsType")
    public void validateMobileAppTracker_EmptyUrl_ErrorOnUrl(OsType osType) {
        MobileAppTracker tracker = createValidTracker().withUrl("");
        ValidationResult<MobileAppTracker, Defect> result = validationService.validateMobileAppTracker(tracker, osType);
        assertThat(result).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(field("url")), StringDefectIds.CANNOT_BE_EMPTY))));
    }

    @Test
    @Parameters(method = "for_AnyOsType")
    public void validateMobileAppTracker_EmptyImpressionUrl_NoErrors(OsType osType) {
        MobileAppTracker tracker = createValidTracker().withImpressionUrl("");
        ValidationResult<MobileAppTracker, Defect> result = validationService.validateMobileAppTracker(tracker, osType);
        assertThat(result).is(matchedBy(hasNoErrorsAndWarnings()));
    }

    @Test
    @Parameters(method = "for_AnyOsType")
    public void validateMobileAppTracker_LongUrl_NoErrors(OsType osType) {
        MobileAppTracker tracker = createValidTracker()
                .withUrl(expandUpToLength(VALID_TRACKER_URL, MAX_TRACKER_URL_LENGTH));
        ValidationResult<MobileAppTracker, Defect> result = validationService.validateMobileAppTracker(tracker, osType);
        assertThat(result).is(matchedBy(hasNoErrorsAndWarnings()));
    }

    @Test
    @Parameters(method = "for_AnyOsType")
    public void validateMobileAppTracker_LongImpressionUrl_NoErrors(OsType osType) {
        MobileAppTracker tracker = createValidTracker()
                .withImpressionUrl(expandUpToLength(VALID_TRACKER_URL, MAX_TRACKER_URL_LENGTH));
        ValidationResult<MobileAppTracker, Defect> result = validationService.validateMobileAppTracker(tracker, osType);
        assertThat(result).is(matchedBy(hasNoErrorsAndWarnings()));
    }

    @Test
    @Parameters(method = "for_AnyOsType")
    public void validateMobileAppTracker_TooLongUrl_ErrorOnUrl(OsType osType) {
        MobileAppTracker tracker = createValidTracker()
                .withUrl(expandUpToLength(VALID_TRACKER_URL, MAX_TRACKER_URL_LENGTH + 1));
        ValidationResult<MobileAppTracker, Defect> result = validationService.validateMobileAppTracker(tracker, osType);
        assertThat(result).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(field("url")), StringDefectIds.LENGTH_CANNOT_BE_MORE_THAN_MAX))));
    }

    @Test
    @Parameters(method = "for_AnyOsType")
    public void validateMobileAppTracker_TooLongImpressionUrl_ErrorOnImpressionUrl(OsType osType) {
        MobileAppTracker tracker = createValidTracker()
                .withImpressionUrl(expandUpToLength(VALID_TRACKER_URL, MAX_TRACKER_URL_LENGTH + 1));
        ValidationResult<MobileAppTracker, Defect> result = validationService.validateMobileAppTracker(tracker, osType);
        assertThat(result).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(field("impressionUrl")), StringDefectIds.LENGTH_CANNOT_BE_MORE_THAN_MAX))));
    }

    @Test
    @Parameters(method = "for_AnyOsType")
    public void validateMobileAppTracker_NotUrl_ErrorOnUrl(OsType osType) {
        MobileAppTracker tracker = createValidTracker().withUrl(INVALID_TRACKER_URL);
        ValidationResult<MobileAppTracker, Defect> result = validationService.validateMobileAppTracker(tracker, osType);
        assertThat(result).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(field("url")), MobileAppDefects.Gen.INVALID_URL))));
    }

    @Test
    @Parameters(method = "for_AnyOsType")
    public void validateMobileAppTracker_NotImpressionUrl_ErrorOnImpressionUrl(OsType osType) {
        MobileAppTracker tracker = createValidTracker().withImpressionUrl(INVALID_TRACKER_URL);
        ValidationResult<MobileAppTracker, Defect> result = validationService.validateMobileAppTracker(tracker, osType);
        assertThat(result).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(field("impressionUrl")), MobileAppDefects.Gen.INVALID_URL))));
    }

    @Test
    @Parameters(method = "for_AnyOsType")
    public void validateMobileAppTracker_WithNullTrackerId_NoErrors(
            OsType osType) {
        MobileAppTracker tracker = createValidTracker()
                .withUrl(VALID_APPMETRICA_TRACKING_URL)
                .withImpressionUrl("")
                .withTrackingSystem(MobileAppTrackerTrackingSystem.APPMETRICA)
                .withTrackerId(null);
        ValidationResult<MobileAppTracker, Defect> result = validationService.validateMobileAppTracker(tracker, osType);
        assertThat(result).is(matchedBy(hasNoErrorsAndWarnings()));
    }

    @Test
    @Parameters(method = "for_AnyOsType")
    public void validateMobileAppTracker_WithNullTrackerIdAndOtherTrackingSystem_NoErrors(OsType osType) {
        MobileAppTracker tracker = createValidTracker()
                .withTrackingSystem(MobileAppTrackerTrackingSystem.OTHER)
                .withTrackerId(null);
        ValidationResult<MobileAppTracker, Defect> result = validationService.validateMobileAppTracker(tracker, osType);
        assertThat(result).is(matchedBy(hasNoErrorsAndWarnings()));
    }


    @Test
    @Parameters(method = "for_AnyOsType")
    public void validateMobileAppTracker_EmptyTrackerId_ErrorOnTrackerId(OsType osType) {
        MobileAppTracker tracker = createValidTracker()
                .withTrackingSystem(MobileAppTrackerTrackingSystem.APPMETRICA)
                .withTrackerId("");
        ValidationResult<MobileAppTracker, Defect> result = validationService.validateMobileAppTracker(tracker, osType);
        assertThat(result).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(field("trackerId")), StringDefectIds.CANNOT_BE_EMPTY))));
    }

    @Test
    @Parameters(method = "for_AnyOsType")
    public void validateMobileAppTracker_LongTrackerId_NoErrors(OsType osType) {
        MobileAppTracker tracker = createValidTracker()
                .withTrackerId(randomAlphanumeric(MobileAppValidationService.MAX_TRACKER_ID_LENGTH));
        ValidationResult<MobileAppTracker, Defect> result = validationService.validateMobileAppTracker(tracker, osType);
        assertThat(result).is(matchedBy(hasNoErrorsAndWarnings()));
    }

    @Test
    @Parameters(method = "for_AnyOsType")
    public void validateMobileAppTracker_TooLongTrackerId_ErrorOnTrackerId(OsType osType) {
        MobileAppTracker tracker = createValidTracker()
                .withTrackingSystem(MobileAppTrackerTrackingSystem.APPMETRICA)
                .withUrl(VALID_APPMETRICA_TRACKING_URL)
                .withTrackerId(randomAlphanumeric(MobileAppValidationService.MAX_TRACKER_URL_LENGTH + 1));
        ValidationResult<MobileAppTracker, Defect> result = validationService.validateMobileAppTracker(tracker, osType);
        assertThat(result).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(field("trackerId")), StringDefectIds.LENGTH_CANNOT_BE_MORE_THAN_MAX))));
    }

    @Test
    @Parameters(method = "for_AnyOsType")
    public void validateMobileAppTracker_WithoutTrackingSystem_ErrorOnTrackerSystem(OsType osType) {
        MobileAppTracker tracker = createValidTracker()
                .withTrackingSystem(null);
        ValidationResult<MobileAppTracker, Defect> result = validationService.validateMobileAppTracker(tracker, osType);
        assertThat(result).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(field("trackingSystem")), DefectIds.CANNOT_BE_NULL))));
    }

    @Test
    @Parameters(method = "for_AnyOsType_AnyTrackersExceptAdjustAndOther")
    public void validateMobileAppTracker_UrlWithoutLogId_ErrorOnUrl(
            OsType osType,
            MobileAppTrackerTrackingSystem trackingSystem) {
        MobileAppTracker tracker = createValidTracker()
                .withTrackingSystem(trackingSystem)
                .withUrl(TRACKER_URL_WITHOUT_LOGID);
        ValidationResult<MobileAppTracker, Defect> result = validationService.validateMobileAppTracker(tracker, osType);
        assertThat(result).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(field("url")), BannerDefectIds.Gen.TRACKING_URL_DOESNT_CONTAIN_MACROS))));
    }

    @Test
    @Parameters(method = "for_AnyOsType_AnyTrackersExceptAdjustAndOther")
    public void validateMobileAppTracker_ImpressionUrlWithoutLogId_ErrorOnUrl(
            OsType osType,
            MobileAppTrackerTrackingSystem trackingSystem) {
        MobileAppTracker tracker = createValidTracker()
                .withTrackingSystem(trackingSystem)
                .withImpressionUrl(TRACKER_URL_WITHOUT_LOGID);
        ValidationResult<MobileAppTracker, Defect> result = validationService.validateMobileAppTracker(tracker, osType);
        assertThat(result).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(field("impressionUrl")), BannerDefectIds.Gen.TRACKING_URL_DOESNT_CONTAIN_MACROS))));
    }

    @Test
    @Parameters(method = "for_AnyOsType_LogIdInDifferentCases")
    public void validateMobileAppTracker_UrlWithLogIdInAnyCase_NoErrors(
            OsType osType,
            String logidMacros) {
        MobileAppTracker tracker = createValidTracker()
                .withTrackingSystem(MobileAppTrackerTrackingSystem.OTHER)
                .withUrl(TRACKER_URL_WITHOUT_LOGID + logidMacros);
        ValidationResult<MobileAppTracker, Defect> result = validationService.validateMobileAppTracker(tracker, osType);
        assertThat(result).is(matchedBy(hasNoErrorsAndWarnings()));
    }

    @Test
    @Parameters(method = "for_AnyOsType_LogIdInDifferentCases")
    public void validateMobileAppTracker_ImpressionUrlWithLogIdInAnyCase_NoErrors(
            OsType osType,
            String logidMacros) {
        MobileAppTracker tracker = createValidTracker()
                .withTrackingSystem(MobileAppTrackerTrackingSystem.OTHER)
                .withImpressionUrl(TRACKER_URL_WITHOUT_LOGID + logidMacros);
        ValidationResult<MobileAppTracker, Defect> result = validationService.validateMobileAppTracker(tracker, osType);
        assertThat(result).is(matchedBy(hasNoErrorsAndWarnings()));
    }

    @Test
    @Parameters(method = "for_AnyOsType")
    public void validateMobileAppTracker_UrlWithoutLogIdForAdjust_NoErrors(OsType osType) {
        MobileAppTracker tracker = createValidTracker()
                .withTrackingSystem(MobileAppTrackerTrackingSystem.ADJUST)
                .withUrl(TRACKER_URL_WITHOUT_LOGID_FOR_ADJUST)
                .withImpressionUrl(TRACKER_IMPRESSION_URL_WITHOUT_LOGID_FOR_ADJUST);
        ValidationResult<MobileAppTracker, Defect> result = validationService.validateMobileAppTracker(tracker, osType);
        assertThat(result).is(matchedBy(hasNoErrorsAndWarnings()));
    }

    @Test
    @Parameters(method = "for_AnyOsType_AnyTrackersExceptOther")
    public void validateMobileAppTracker_UrlWithoutOsSpecificMacros_ErrorOnUrl(
            OsType osType,
            MobileAppTrackerTrackingSystem trackingSystem) {
        MobileAppTracker tracker = new MobileAppTracker()
                .withTrackingSystem(trackingSystem)
                .withUrl(URL_WITHOUT_OS_SPECIFIC_MACROS.get(osType));
        ValidationResult<MobileAppTracker, Defect> result = validationService.validateMobileAppTracker(tracker, osType);
        assertThat(result).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(field("url")), BannerDefectIds.Gen.TRACKING_URL_DOESNT_CONTAIN_MACROS))));
    }

    @Test
    @Parameters(method = "for_AnyOsType_AnyTrackersExceptOther")
    public void validateMobileAppTracker_ImpressionUrlWithoutOsSpecificMacros_ErrorOnUrl(
            OsType osType,
            MobileAppTrackerTrackingSystem trackingSystem) {
        MobileAppTracker tracker = new MobileAppTracker()
                .withTrackingSystem(trackingSystem)
                .withImpressionUrl(URL_WITHOUT_OS_SPECIFIC_MACROS.get(osType));
        ValidationResult<MobileAppTracker, Defect> result = validationService.validateMobileAppTracker(tracker, osType);
        assertThat(result).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(field("impressionUrl")), BannerDefectIds.Gen.TRACKING_URL_DOESNT_CONTAIN_MACROS))));
    }

    @Test
    @Parameters(method = "for_AnyOsType_AnyValidUserParams")
    public void validateMobileAppTracker_ValidUserParams_NoErrors(
            OsType osType,
            List<String> validUserParams
    ) {
        MobileAppTracker tracker = createValidTracker()
                .withUserParams(validUserParams);
        ValidationResult<MobileAppTracker, Defect> result = validationService.validateMobileAppTracker(tracker, osType);
        assertThat(result).is(matchedBy(hasNoErrorsAndWarnings()));
    }

    @Test
    public void validateMobileAppTracker_NullUserParams_ErrorOnUserParams() {
        MobileAppTracker tracker = createValidTracker()
                .withUserParams(null);
        ValidationResult<MobileAppTracker, Defect> result =
                validationService.validateMobileAppTracker(tracker, OsType.IOS);
        assertThat(result).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(field("userParams")), DefectIds.CANNOT_BE_NULL))));
    }

    @Test
    public void validateMobileAppTracker_TooManyUserParams_ErrorOnUserParams() {
        MobileAppTracker tracker = createValidTracker()
                .withUserParams(Collections.nCopies(MobileAppValidationService.MAX_USER_PARAM_SIZE + 1, "x"));
        ValidationResult<MobileAppTracker, Defect> result =
                validationService.validateMobileAppTracker(tracker, OsType.IOS);
        assertThat(result).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(field("userParams")), CollectionDefectIds.Size.SIZE_CANNOT_BE_MORE_THAN_MAX))));
    }

    @Test
    public void validateMobileAppTracker_NullInUserParams_ErrorOnUserParams() {
        MobileAppTracker tracker = createValidTracker()
                .withUserParams(singletonList(null));
        ValidationResult<MobileAppTracker, Defect> result =
                validationService.validateMobileAppTracker(tracker, OsType.IOS);
        assertThat(result).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(field("userParams")), CollectionDefectIds.Gen.CANNOT_CONTAIN_NULLS))));
    }

    @Test
    public void validateMobileAppTracker_EmptyStringInUserParams_ErrorOnUserParams() {
        MobileAppTracker tracker = createValidTracker()
                .withUserParams(singletonList(""));
        ValidationResult<MobileAppTracker, Defect> result =
                validationService.validateMobileAppTracker(tracker, OsType.IOS);
        assertThat(result).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(field("userParams"), index(0)), StringDefectIds.CANNOT_BE_EMPTY))));
    }

    @Test
    public void validateMobileAppTracker_NonAlphanumStringInUserParams_ErrorOnUserParams() {
        MobileAppTracker tracker = createValidTracker()
                .withUserParams(singletonList("?"));
        ValidationResult<MobileAppTracker, Defect> result =
                validationService.validateMobileAppTracker(tracker, OsType.IOS);
        assertThat(result).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(field("userParams"), index(0)), DefectIds.INVALID_VALUE))));
    }

    @Test
    public void validateMobileAppTracker_TooLongStringInUserParams_ErrorOnUserParams() {
        MobileAppTracker tracker = createValidTracker()
                .withUserParams(
                        singletonList(randomAlphanumeric(MobileAppValidationService.MAX_USER_PARAM_LENGTH + 1)));
        ValidationResult<MobileAppTracker, Defect> result =
                validationService.validateMobileAppTracker(tracker, OsType.IOS);
        assertThat(result).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(field("userParams"), index(0)), StringDefectIds.LENGTH_CANNOT_BE_MORE_THAN_MAX))));
    }

    @Test
    public void validateMobileAppTracker_DoesNotReturnNotSupportedErrorOnBlankUrl() {
        when(trustedRedirectsService.checkTrackingHref(anyString()))
                .thenReturn(TrustedRedirectsService.Result.NOT_TRUSTED);
        MobileAppTracker tracker = createValidTracker()
                .withUrl("");
        ValidationResult<MobileAppTracker, Defect> result =
                validationService.validateMobileAppTracker(tracker, OsType.IOS);
        assertThat(result).is(matchedBy(
                not(hasDefectDefinitionWith(validationError(
                        path(field("url")), BannerDefectIds.Gen.THIS_TRACKING_SYSTEM_DOMAIN_NOT_SUPPORTED))))
        );
    }

    @Test
    public void validateMobileAppTracker_TrustedRedirectsServiceReturnsNotTrusted_ErrorOnUrl() {
        String untrustedUrl = "http://ya.ru?{LOGID}-{ios_ifa}-{google_aid}";
        when(trustedRedirectsService.checkTrackingHref(anyString()))
                .thenReturn(TrustedRedirectsService.Result.NOT_TRUSTED);
        MobileAppTracker tracker = createValidTracker()
                .withUrl(untrustedUrl);
        ValidationResult<MobileAppTracker, Defect> result =
                validationService.validateMobileAppTracker(tracker, OsType.IOS);
        assertThat(result).is(matchedBy(hasDefectDefinitionWith(
                validationError(
                        path(field("url")), BannerDefectIds.Gen.THIS_TRACKING_SYSTEM_DOMAIN_NOT_SUPPORTED))));
    }

    @Test
    public void validateMobileAppTracker_TrustedRedirectsServiceReturnsHttpsRequired_ErrorOnUrl() {
        String notHttpsUrl = "http://ya.ru?{LOGID}-{ios_ifa}-{google_aid}";
        when(trustedRedirectsService.checkTrackingHref(anyString()))
                .thenReturn(TrustedRedirectsService.Result.HTTPS_REQUIRED);
        MobileAppTracker tracker = createValidTracker()
                .withUrl(notHttpsUrl);
        ValidationResult<MobileAppTracker, Defect> result =
                validationService.validateMobileAppTracker(tracker, OsType.IOS);
        assertThat(result).is(matchedBy(hasDefectDefinitionWith(
                validationError(
                        path(field("url")), BannerDefectIds.Gen.THIS_TRACKING_SYSTEM_DOMAIN_ONLY_SUPPORTS_HTTPS))));
    }

    @Test
    public void validateMobileAppTracker_DomainDoesNotMatchTrackerSystem_ErrorOnUrl() {
        MobileAppTracker tracker = createValidTracker()
                .withUrl(VALID_APPMETRICA_TRACKING_URL)
                .withTrackingSystem(MobileAppTrackerTrackingSystem.ADJUST);
        ValidationResult<MobileAppTracker, Defect> result =
                validationService.validateMobileAppTracker(tracker, OsType.IOS);
        assertThat(result).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(field("url")),
                        MobileAppDefects.Gen.TRACKER_URL_DOMAIN_DOES_NOT_MATCH_SELECTED_TRACKER_SYSTEM))));
    }

    @Test
    public void validateMobileAppTracker_DomainDoesNotMatchTrackerSystem_ErrorOnImpressionUrl() {
        MobileAppTracker tracker = createValidTracker()
                .withImpressionUrl(VALID_APPMETRICA_TRACKING_URL)
                .withTrackingSystem(MobileAppTrackerTrackingSystem.ADJUST);
        ValidationResult<MobileAppTracker, Defect> result =
                validationService.validateMobileAppTracker(tracker, OsType.IOS);
        assertThat(result).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(field("impressionUrl")),
                        MobileAppDefects.Gen.TRACKER_URL_DOMAIN_DOES_NOT_MATCH_SELECTED_TRACKER_SYSTEM))));
    }

    @Test
    public void validateMobileAppTracker_TrackerSystemWithoutDomain_NoErrors() {
        MobileAppTracker tracker = createValidTracker()
                .withUrl(VALID_MAIL_RU_TRACKING_URL)
                .withImpressionUrl(VALID_MAIL_RU_TRACKING_URL)
                .withTrackingSystem(MobileAppTrackerTrackingSystem.MAIL_RU);
        ValidationResult<MobileAppTracker, Defect> result =
                validationService.validateMobileAppTracker(tracker, OsType.ANDROID);
        assertThat(result).is(matchedBy(hasNoErrorsAndWarnings()));
    }

    private MobileAppTracker createValidTracker() {
        return new MobileAppTracker()
                .withTrackingSystem(MobileAppTrackerTrackingSystem.OTHER)
                .withTrackerId("121231-aaa-bbb-23123")
                .withUrl(VALID_TRACKER_URL)
                .withImpressionUrl(VALID_TRACKER_URL)
                .withUserParams(emptyList());
    }

    @SuppressWarnings("SameParameterValue")
    @Nonnull
    private String expandUpToLength(String initialString, int length) {
        return initialString + randomAlphanumeric(length - initialString.length());
    }

}
