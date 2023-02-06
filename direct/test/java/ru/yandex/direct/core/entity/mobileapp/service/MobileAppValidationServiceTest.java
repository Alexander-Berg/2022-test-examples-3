package ru.yandex.direct.core.entity.mobileapp.service;

import java.util.List;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableList;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import ru.yandex.direct.core.entity.mobileapp.MobileAppDefects;
import ru.yandex.direct.core.entity.mobileapp.model.MobileApp;
import ru.yandex.direct.core.entity.mobileapp.model.MobileAppTracker;
import ru.yandex.direct.core.entity.mobileapp.model.MobileAppTrackerTrackingSystem;
import ru.yandex.direct.core.entity.mobileapp.repository.MobileAppRepository;
import ru.yandex.direct.core.entity.mobilecontent.model.MobileContent;
import ru.yandex.direct.core.entity.mobilecontent.model.OsType;
import ru.yandex.direct.core.entity.mobilecontent.service.MobileContentService;
import ru.yandex.direct.core.entity.trustedredirects.service.TrustedRedirectsService;
import ru.yandex.direct.core.entity.uac.service.trackingurl.TrackingUrlParseService;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.validation.defect.ids.StringDefectIds;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.DefectIds;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoErrorsAndWarnings;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(JUnitParamsRunner.class)
public class MobileAppValidationServiceTest {
    private static final String ANDROID_URL =
            "https://play.google.com/store/apps/details?id=com.google.android.apps.docs";
    private static final String IOS_URL = "https://itunes.apple.com/ru/app/minecraft/id479516143?mt=8&";

    public static List<String> for_AnyValidStoreHref() {
        return ImmutableList.of(ANDROID_URL, IOS_URL);
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
    private ClientId clientId;

    @Mock
    private TrackingUrlParseService trackingUrlParseService;

    private MobileAppValidationService validationService;

    @Before
    public void before() {
        initMocks(this);
        when(trustedRedirectsService.checkTrackingHref(anyString())).thenReturn(TrustedRedirectsService.Result.TRUSTED);
        when(trustedRedirectsService.checkImpressionUrl(anyString())).thenReturn(TrustedRedirectsService.Result.TRUSTED);
        validationService = new MobileAppValidationService(trustedRedirectsService, shardHelper, mobileAppRepository,
                mobileContentService, trackingUrlParseService);
    }

    @Test
    @Parameters(method = "for_AnyValidStoreHref")
    public void validateMobileApp_Valid_NoErrors(String storeHref) {
        MobileApp app = createValidMobileApp().withStoreHref(storeHref);
        ValidationResult<MobileApp, Defect> result = validationService.validateMobileApp(app, clientId);
        assertThat(result).is(matchedBy(hasNoErrorsAndWarnings()));

    }

    @Test
    public void validateMobileApp_NullInStoreHref_ErrorOnStoreHref() {
        MobileApp app = createValidMobileApp().withStoreHref(null);
        ValidationResult<MobileApp, Defect> result = validationService.validateMobileApp(app, clientId);
        assertThat(result).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(field("storeHref")), DefectIds.CANNOT_BE_NULL))));
    }

    @Test
    public void validateMobileApp_EmptyStoreHref_ErrorOnStoreHref() {
        MobileApp app = createValidMobileApp().withStoreHref("");
        ValidationResult<MobileApp, Defect> result = validationService.validateMobileApp(app, clientId);
        assertThat(result).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(field("storeHref")), StringDefectIds.CANNOT_BE_EMPTY))));
    }

    @Test
    public void validateMobileApp_LongStoreHref_NoErrors() {
        MobileApp app = createValidMobileApp()
                .withStoreHref(expandUpToLength(IOS_URL, MobileAppValidationService.MAX_STORE_HREF_LENGTH));
        ValidationResult<MobileApp, Defect> result = validationService.validateMobileApp(app, clientId);
        assertThat(result).is(matchedBy(hasNoErrorsAndWarnings()));
    }

    @Test
    public void validateMobileApp_TooLongStoreHref_ErrorOnStoreHref() {
        MobileApp app = createValidMobileApp()
                .withStoreHref(expandUpToLength(IOS_URL, MobileAppValidationService.MAX_STORE_HREF_LENGTH + 1));
        ValidationResult<MobileApp, Defect> result = validationService.validateMobileApp(app, clientId);
        assertThat(result).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(field("storeHref")), StringDefectIds.LENGTH_CANNOT_BE_MORE_THAN_MAX))));
    }

    @Test
    public void validateMobileApp_NullInName_ErrorOnName() {
        MobileApp app = createValidMobileApp().withName(null);
        ValidationResult<MobileApp, Defect> result = validationService.validateMobileApp(app, clientId);
        assertThat(result).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(field("name")), DefectIds.CANNOT_BE_NULL))));
    }

    @Test
    public void validateMobileApp_EmptyName_ErrorOnName() {
        MobileApp app = createValidMobileApp().withName("");
        ValidationResult<MobileApp, Defect> result = validationService.validateMobileApp(app, clientId);
        assertThat(result).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(field("name")), StringDefectIds.CANNOT_BE_EMPTY))));
    }

    @Test
    public void validateMobileApp_NullInDisplayedAttributes_ErrorOnDisplayedAttributes() {
        MobileApp app = createValidMobileApp().withDisplayedAttributes(null);
        ValidationResult<MobileApp, Defect> result = validationService.validateMobileApp(app, clientId);
        assertThat(result).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(field("displayedAttributes")), DefectIds.CANNOT_BE_NULL))));
    }

    @Test
    public void validateMobileApp_NullInTrackers_ErrorOnTrackers() {
        MobileApp app = createValidMobileApp().withTrackers(null);
        ValidationResult<MobileApp, Defect> result = validationService.validateMobileApp(app, clientId);
        assertThat(result).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(field("trackers")), DefectIds.CANNOT_BE_NULL))));
    }

    @Test
    public void validateMobileApp_NullInDomain_NoErrors() {
        MobileApp app = createValidMobileApp().withDomain(null);
        ValidationResult<MobileApp, Defect> result = validationService.validateMobileApp(app, clientId);
        assertThat(result).is(matchedBy(hasNoErrorsAndWarnings()));
    }

    @Test
    public void validateMobileApp_OneBundleIdNoAppmetrika_NoErrors() {
        MobileApp app1 = createValidMobileApp()
                .withId(1L)
                .withMobileContent(new MobileContent()
                        .withOsType(OsType.IOS)
                        .withBundleId("bundle"));
        MobileApp app2 = createValidMobileApp()
                .withId(2L)
                .withMobileContent(new MobileContent()
                        .withOsType(OsType.IOS)
                        .withBundleId("bundle"));
        when(mobileAppRepository.getMobileApps(anyInt(), isNull(), isNull())).thenReturn(List.of(app1, app2));
        ValidationResult<MobileApp, Defect> result = validationService.validateMobileApp(app2, clientId);
        assertThat(result).is(matchedBy(hasNoErrorsAndWarnings()));
    }

    @Test
    public void validateMobileApp_TwoBundleIdsOneAppmetrika_NoErrors() {
        MobileApp app1 = createValidMobileApp()
                .withId(1L)
                .withAppMetrikaApplicationId(12345L)
                .withMobileContent(new MobileContent()
                        .withOsType(OsType.IOS)
                        .withBundleId("bundle 1"));
        MobileApp app2 = createValidMobileApp()
                .withId(2L)
                .withAppMetrikaApplicationId(12345L)
                .withMobileContent(new MobileContent()
                        .withOsType(OsType.IOS)
                        .withBundleId("bundle 2"));
        when(mobileAppRepository.getMobileApps(anyInt(), isNull(), isNull())).thenReturn(List.of(app1, app2));
        ValidationResult<MobileApp, Defect> result = validationService.validateMobileApp(app2, clientId);
        assertThat(result).is(matchedBy(hasNoErrorsAndWarnings()));
    }

    @Test
    public void validateMobileApp_OneBundleIdOneAppmetrika_ErrorOnAppmetrika() {
        MobileApp app1 = createValidMobileApp()
                .withId(1L)
                .withAppMetrikaApplicationId(12345L)
                .withMobileContent(new MobileContent()
                        .withOsType(OsType.IOS)
                        .withBundleId("bundle"));
        MobileApp app2 = createValidMobileApp()
                .withId(2L)
                .withAppMetrikaApplicationId(12345L)
                .withMobileContent(new MobileContent()
                        .withOsType(OsType.IOS)
                        .withBundleId("bundle"));
        when(mobileAppRepository.getMobileApps(anyInt(), eq(clientId), isNull())).thenReturn(List.of(app1, app2));
        ValidationResult<MobileApp, Defect> result = validationService.validateMobileApp(app2, clientId);
        assertThat(result).is(matchedBy(hasDefectDefinitionWith(validationError(
                MobileAppDefects.Gen.APPMETRIKA_APPLICATION_ALREADY_USED))));
    }


    private MobileApp createValidMobileApp() {
        return new MobileApp()
                .withName("My app")
                .withStoreHref(ANDROID_URL)
                .withDisplayedAttributes(emptySet())
                .withTrackers(singletonList(createValidTracker()))
                .withDomain("ya.ru");
    }

    private MobileAppTracker createValidTracker() {
        return new MobileAppTracker()
                .withTrackingSystem(MobileAppTrackerTrackingSystem.OTHER)
                .withTrackerId("121231-aaa-bbb-23123")
                .withUrl(MobileAppValidationServiceValidateTrackerTest.VALID_TRACKER_URL)
                .withImpressionUrl(MobileAppValidationServiceValidateTrackerTest.VALID_TRACKER_URL)
                .withUserParams(emptyList());
    }

    @SuppressWarnings("SameParameterValue")
    @Nonnull
    private String expandUpToLength(String initialString, int length) {
        return initialString + randomAlphanumeric(length - initialString.length());
    }
}
