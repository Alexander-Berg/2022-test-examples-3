package ru.yandex.direct.core.entity.adgroup.service.validation.types;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.core.entity.adgroup.model.MobileContentAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.MobileContentAdGroupDeviceTypeTargeting;
import ru.yandex.direct.core.entity.adgroup.model.MobileContentAdGroupNetworkTargeting;
import ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefectIds;
import ru.yandex.direct.core.entity.campaign.service.CampaignService;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.mobileapp.service.MobileAppService;
import ru.yandex.direct.core.entity.mobilecontent.container.MobileAppStoreUrl;
import ru.yandex.direct.core.entity.mobilecontent.util.MobileAppStoreUrlParser;
import ru.yandex.direct.validation.builder.Constraint;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.validator.internal.util.CollectionHelper.asSet;
import static org.mockito.Mockito.mock;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.advertisedAppLinkIsNotSet;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.deviceTargetingIsNotSet;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.invalidAppLinkFormat;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.invalidAppStoreUrl;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.invalidMinOsVersion;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.minOsVersionIsNotSet;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.networkTargetingIsNotSet;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.storeUrlMustBeTheSameAsUrlInMobileContentCampaign;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class MobileContentAdGroupValidationTest {
    private static final String VALID_ITUNES_STORE_URL = "https://itunes.apple.com/ru/app/angry-birds/id343200656?mt=8";
    private static final String VALID_APP_STORE_URL = "https://apps.apple.com/ru/app/angry-birds/id343200656?mt=8";
    private static final String VALID_PLAY_MARKET_STORE_URL = "https://play.google.com/store/apps/details?id=ru" +
            ".aviasales";
    private static final String VALID_PLAY_MARKET_STORE_URL_HL_EN = "https://play.google.com/store/apps/details?id=ru" +
            ".aviasales&hl=en";
    private static final String VALID_PLAY_MARKET_STORE_URL_GL_US = "https://play.google.com/store/apps/details?id=ru" +
            ".aviasales&gl=us";
    private static final long CAMPAIGN_ID = 133123L;
    private static final long CAMPAIGN_ID_GOOGLE_PLAY = 133132L;
    private static final Map<Long, MobileAppStoreUrl> CAMPAIGNS_STORE_URL =
            ImmutableMap.of(CAMPAIGN_ID, MobileAppStoreUrlParser.parseStrict(VALID_ITUNES_STORE_URL));
    private static final Map<Long, MobileAppStoreUrl> CAMPAIGNS_MARKET_URL =
            ImmutableMap.of(CAMPAIGN_ID_GOOGLE_PLAY, MobileAppStoreUrlParser.parseStrict(VALID_PLAY_MARKET_STORE_URL));

    private MobileContentAdGroup adGroup;
    private MobileContentAdGroup googlePlayAdGroup;
    private MobileContentAdGroupValidation validation;

    @Before
    public void setUp() {
        adGroup = new MobileContentAdGroup()
                .withCampaignId(CAMPAIGN_ID)
                .withStoreUrl(VALID_ITUNES_STORE_URL)
                .withMinimalOperatingSystemVersion("1.0")
                .withDeviceTypeTargeting(asSet(MobileContentAdGroupDeviceTypeTargeting.PHONE))
                .withNetworkTargeting(asSet(MobileContentAdGroupNetworkTargeting.WI_FI));

        googlePlayAdGroup = new MobileContentAdGroup()
                .withCampaignId(CAMPAIGN_ID_GOOGLE_PLAY)
                .withStoreUrl(VALID_PLAY_MARKET_STORE_URL)
                .withMinimalOperatingSystemVersion("1.0")
                .withDeviceTypeTargeting(asSet(MobileContentAdGroupDeviceTypeTargeting.PHONE))
                .withNetworkTargeting(asSet(MobileContentAdGroupNetworkTargeting.WI_FI));

        validation = new MobileContentAdGroupValidation(
                mock(FeatureService.class),
                mock(CampaignService.class),
                mock(MobileAppService.class));
    }

    @Test
    public void validateAdGroup_storeUrlIsNull() {
        ValidationResult<MobileContentAdGroup, Defect> result = validation.validateAdGroup(
                adGroup.withStoreUrl(null));

        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(path(field(MobileContentAdGroup.STORE_URL.name())), advertisedAppLinkIsNotSet()))));
    }

    @Test
    public void validateAdGroup_storeUrlIsEmpty_errorLinkIsMissing() {
        ValidationResult<MobileContentAdGroup, Defect> result = validation.validateAdGroup(
                adGroup.withStoreUrl(""));

        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(path(field(MobileContentAdGroup.STORE_URL.name())), advertisedAppLinkIsNotSet()))));
    }

    @Test
    public void validateAdGroup_storeUrlIsInvalid_errorInvalidValue() {
        ValidationResult<MobileContentAdGroup, Defect> result = validation.validateAdGroup(
                adGroup.withStoreUrl("wwwgooglecom"));

        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(path(field(MobileContentAdGroup.STORE_URL.name())), invalidAppLinkFormat()))));
    }

    @Test
    public void validateAdGroup_googleApStoreIsValid() {
        ValidationResult<MobileContentAdGroup, Defect> result = validation.validateAdGroup(
                adGroup.withStoreUrl("https://play.google.com/store/apps/details?id=com.rovio.angrybirds"));

        assertThat(result).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validateAdGroup_iTunesStoreIsValid() {
        ValidationResult<MobileContentAdGroup, Defect> result = validation.validateAdGroup(
                adGroup.withStoreUrl("https://itunes.apple.com/ru/app/angry-birds/id343200656?mt=8"));

        assertThat(result).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validateAdGroup_AppleAppStoreIsValid() {
        ValidationResult<MobileContentAdGroup, Defect> result = validation.validateAdGroup(
                adGroup.withStoreUrl("https://apps.apple.com/ru/app/angry-birds-classic/id343200656"));

        assertThat(result).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validateAdGroup_invalidDomain() {
        ValidationResult<MobileContentAdGroup, Defect> result = validation.validateAdGroup(
                adGroup.withStoreUrl("https://www.google.ru/search?q=itunes+store+link+angry+birds"));

        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(path(field(MobileContentAdGroup.STORE_URL.name())),
                        AdGroupDefectIds.AppStoreDomain.APP_STORE_IS_NOT_SUPPORTED))));
    }

    @Test
    public void validateAdGroup_googlePlay_invalidContentType() {
        ValidationResult<MobileContentAdGroup, Defect> result = validation.validateAdGroup(
                adGroup.withStoreUrl("https://play.google.com/store/books/details?id=com.rovio.angrybirds"));

        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(path(field(MobileContentAdGroup.STORE_URL.name())), invalidAppStoreUrl()))));
    }

    @Test
    public void validateAdGroup_googlePlay_invalidStoreId() {
        ValidationResult<MobileContentAdGroup, Defect> result = validation.validateAdGroup(
                adGroup.withStoreUrl("https://play.google.com/store/apps/details?id=42"));

        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(path(field(MobileContentAdGroup.STORE_URL.name())), invalidAppStoreUrl()))));
    }


    @Test
    public void validateAdGroup_itunes_invalidContentType() {
        ValidationResult<MobileContentAdGroup, Defect> result = validation.validateAdGroup(
                adGroup.withStoreUrl("https://itunes.apple.com/ru/book/angry-birds/id343200656?mt=8"));

        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(path(field(MobileContentAdGroup.STORE_URL.name())), invalidAppStoreUrl()))));
    }

    @Test
    public void validateAdGroup_itunes_invalidStoreId() {
        ValidationResult<MobileContentAdGroup, Defect> result = validation.validateAdGroup(
                adGroup.withStoreUrl("https://itunes.apple.com/ru/app/angry-birds/ix343200656?mt=8"));

        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(path(field(MobileContentAdGroup.STORE_URL.name())), invalidAppStoreUrl()))));
    }

    @Test
    public void validateAdGroup_minOsVersion_isNull() {
        ValidationResult<MobileContentAdGroup, Defect> result = validation.validateAdGroup(adGroup
                .withStoreUrl("https://itunes.apple.com/ru/app/angry-birds/id343200656?mt=8")
                .withMinimalOperatingSystemVersion(null));

        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(path(field(MobileContentAdGroup.MINIMAL_OPERATING_SYSTEM_VERSION.name())),
                        minOsVersionIsNotSet()))));
    }

    @Test
    public void validateAdGroup_minOsVersion_isEmpty() {
        ValidationResult<MobileContentAdGroup, Defect> result = validation.validateAdGroup(adGroup
                .withStoreUrl("https://itunes.apple.com/ru/app/angry-birds/id343200656?mt=8")
                .withMinimalOperatingSystemVersion(""));

        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(path(field(MobileContentAdGroup.MINIMAL_OPERATING_SYSTEM_VERSION.name())),
                        minOsVersionIsNotSet()))));
    }

    @Test
    public void validateAdGroup_minOsVersion_isSpaces() {
        ValidationResult<MobileContentAdGroup, Defect> result = validation.validateAdGroup(adGroup
                .withStoreUrl("https://itunes.apple.com/ru/app/angry-birds/id343200656?mt=8")
                .withMinimalOperatingSystemVersion("    "));

        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(path(field(MobileContentAdGroup.MINIMAL_OPERATING_SYSTEM_VERSION.name())),
                        minOsVersionIsNotSet()))));
    }

    @Test
    public void validateAdGroup_minOsVersion_iOsPositiveTest() {
        ValidationResult<MobileContentAdGroup, Defect> result = validation.validateAdGroup(adGroup
                .withStoreUrl("https://itunes.apple.com/ru/app/angry-birds/id343200656?mt=8")
                .withMinimalOperatingSystemVersion("11.0"));

        assertThat(result).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validateAdGroup_minOsVersion_iOsInvalidVersion() {
        ValidationResult<MobileContentAdGroup, Defect> result = validation.validateAdGroup(adGroup
                .withStoreUrl("https://itunes.apple.com/ru/app/angry-birds/id343200656?mt=8")
                .withMinimalOperatingSystemVersion("111.3"));

        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(path(field(MobileContentAdGroup.MINIMAL_OPERATING_SYSTEM_VERSION.name())),
                        invalidMinOsVersion()))));
    }

    @Test
    public void validateAdGroup_minOsVersion_iOsAndValidAndroidVersion() {
        ValidationResult<MobileContentAdGroup, Defect> result = validation.validateAdGroup(adGroup
                .withStoreUrl("https://itunes.apple.com/ru/app/angry-birds/id343200656?mt=8")
                .withMinimalOperatingSystemVersion("4.3"));

        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(path(field(MobileContentAdGroup.MINIMAL_OPERATING_SYSTEM_VERSION.name())),
                        invalidMinOsVersion()))));
    }

    @Test
    public void validateAdGroup_minOsVersion_androidPositiveTest() {
        ValidationResult<MobileContentAdGroup, Defect> result = validation.validateAdGroup(adGroup
                .withStoreUrl("https://play.google.com/store/apps/details?id=com.rovio.angrybirds")
                .withMinimalOperatingSystemVersion("4.3"));

        assertThat(result).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validateAdGroup_minOsVersion_androidInvalidVersion() {
        ValidationResult<MobileContentAdGroup, Defect> result = validation.validateAdGroup(adGroup
                .withStoreUrl("https://play.google.com/store/apps/details?id=com.rovio.angrybirds")
                .withMinimalOperatingSystemVersion("4.9"));

        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(path(field(MobileContentAdGroup.MINIMAL_OPERATING_SYSTEM_VERSION.name())),
                        invalidMinOsVersion()))));
    }

    @Test
    public void validateAdGroup_minOsVersion_androidAndValidiOsVersion() {
        ValidationResult<MobileContentAdGroup, Defect> result = validation.validateAdGroup(adGroup
                .withStoreUrl("https://play.google.com/store/apps/details?id=com.rovio.angrybirds")
                .withMinimalOperatingSystemVersion("121.0"));

        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(path(field(MobileContentAdGroup.MINIMAL_OPERATING_SYSTEM_VERSION.name())),
                        invalidMinOsVersion()))));
    }

    @Test
    public void validateAdGroup_deviceTargeting_positiveTest() {
        ValidationResult<MobileContentAdGroup, Defect> result = validation.validateAdGroup(adGroup
                .withDeviceTypeTargeting(asSet(MobileContentAdGroupDeviceTypeTargeting.TABLET)));

        assertThat(result).is(matchedBy(hasNoDefectsDefinitions()));
    }


    @Test
    public void validateAdGroup_deviceTargetingIsNull() {
        ValidationResult<MobileContentAdGroup, Defect> result = validation.validateAdGroup(adGroup
                .withDeviceTypeTargeting(null));

        assertThat(result).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(field(MobileContentAdGroup.DEVICE_TYPE_TARGETING.name())),
                        deviceTargetingIsNotSet()))));
    }

    @Test
    public void validateAdGroup_deviceTargetingIsEmpty_validationError() {
        ValidationResult<MobileContentAdGroup, Defect> result = validation.validateAdGroup(adGroup
                .withDeviceTypeTargeting(emptySet()));

        assertThat(result).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(field(MobileContentAdGroup.DEVICE_TYPE_TARGETING.name())),
                        deviceTargetingIsNotSet()))));
    }

    @Test
    public void validateAdGroup_networkTargeting_positiveTest() {
        ValidationResult<MobileContentAdGroup, Defect> result = validation.validateAdGroup(adGroup
                .withNetworkTargeting(asSet(MobileContentAdGroupNetworkTargeting.WI_FI)));

        assertThat(result).is(matchedBy(hasNoDefectsDefinitions()));
    }


    @Test
    public void validateAdGroup_networkTargetingIsNull() {
        ValidationResult<MobileContentAdGroup, Defect> result = validation.validateAdGroup(adGroup
                .withNetworkTargeting(null));

        assertThat(result).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(field(MobileContentAdGroup.NETWORK_TARGETING.name())),
                        networkTargetingIsNotSet()))));
    }

    @Test
    public void validateAdGroup_networkargetingIsEmpty_validationError() {
        ValidationResult<MobileContentAdGroup, Defect> result = validation.validateAdGroup(adGroup
                .withNetworkTargeting(emptySet()));

        assertThat(result).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(field(MobileContentAdGroup.NETWORK_TARGETING.name())),
                        networkTargetingIsNotSet()))));
    }

    @Test
    public void storeUrlMustBeEqualToUrlAttachedToCampaign_WithoutCampaignId_NoError() {
        storeUrlEqualToUrlAttachedToCampaign_BaseTest(null, adGroup.withCampaignId(null), CAMPAIGNS_STORE_URL);
    }

    @Test
    public void storeUrlMustBeEqualToUrlAttachedToCampaign_CampaignWithoutMobileAppOrUnknownCampaign_NoError() {
        storeUrlEqualToUrlAttachedToCampaign_BaseTest(null, adGroup.withCampaignId(CAMPAIGN_ID + 1L),
                CAMPAIGNS_STORE_URL);
    }

    @Test
    public void storeUrlMustBeEqualToUrlAttachedToCampaign_StoreUrlForAdGroupCorrespondToCampaign_NoError() {
        storeUrlEqualToUrlAttachedToCampaign_BaseTest(null, adGroup, CAMPAIGNS_STORE_URL);
    }

    @Test
    public void storeUrlMustBeEqualToUrlAttachedToCampaign_StoreUrlForAdGroupNotCorrespondToCampaign_ReturnsError() {
        storeUrlEqualToUrlAttachedToCampaign_BaseTest(storeUrlMustBeTheSameAsUrlInMobileContentCampaign(),
                adGroup.withStoreUrl("http://ya.ru"), CAMPAIGNS_STORE_URL);
    }

    @Test
    public void storeUrlMustBeEqualToUrlAttachedToCampaign_ItunesStoreUrlChangedToAppStoreUrl() {
        storeUrlEqualToUrlAttachedToCampaign_BaseTest(null,
                adGroup.withStoreUrl(VALID_APP_STORE_URL), CAMPAIGNS_STORE_URL);
    }

    @Test
    public void storeUrlMustBeEqualToUrlAttachedToCampaign_HlParamChanged() {
        storeUrlEqualToUrlAttachedToCampaign_BaseTest(null,
                googlePlayAdGroup.withStoreUrl(VALID_PLAY_MARKET_STORE_URL_HL_EN), CAMPAIGNS_MARKET_URL);
    }

    @Test
    public void storeUrlMustBeEqualToUrlAttachedToCampaign_GlParamChanged() {
        storeUrlEqualToUrlAttachedToCampaign_BaseTest(storeUrlMustBeTheSameAsUrlInMobileContentCampaign(),
                googlePlayAdGroup.withStoreUrl(VALID_PLAY_MARKET_STORE_URL_GL_US), CAMPAIGNS_MARKET_URL);
    }

    @Test
    public void storeUrlMustBeEqualToUrlAttachedToCampaign_GoogleStoreUrlUnchanged() {
        storeUrlEqualToUrlAttachedToCampaign_BaseTest(null, googlePlayAdGroup, CAMPAIGNS_MARKET_URL);
    }

    private void storeUrlEqualToUrlAttachedToCampaign_BaseTest(Defect expectedDefect,
                                                                     MobileContentAdGroup adGroup,
                                                                     Map<Long, MobileAppStoreUrl> campaignsStoreUrlMap) {
        Constraint<MobileContentAdGroup, Defect> constraint =
                validation.storeUrlMustBeEqualToUrlAttachedToCampaign(campaignsStoreUrlMap, new HashMap<>());
        Defect defect = constraint.apply(adGroup);
        assertThat(defect).isEqualTo(expectedDefect);
    }
}
