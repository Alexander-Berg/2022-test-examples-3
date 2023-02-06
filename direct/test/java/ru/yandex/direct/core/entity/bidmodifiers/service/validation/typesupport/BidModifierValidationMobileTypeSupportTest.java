package ru.yandex.direct.core.entity.bidmodifiers.service.validation.typesupport;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.model.CpmBannerAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmVideoAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CriterionType;
import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierMobile;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierMobileAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.OsType;
import ru.yandex.direct.core.entity.bidmodifiers.container.BidModifierKey;
import ru.yandex.direct.core.entity.bidmodifiers.repository.FakeBidModifierRepository;
import ru.yandex.direct.core.entity.bidmodifiers.service.CachingFeaturesProvider;
import ru.yandex.direct.core.entity.bidmodifiers.validation.typesupport.BidModifierValidationMobileTypeSupport;
import ru.yandex.direct.core.entity.bidmodifiers.validation.typesupport.DeviceModifiersConflictChecker;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.testing.FakeShardByClient;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.bidmodifier.BidModifierMobile.MOBILE_ADJUSTMENT;
import static ru.yandex.direct.core.entity.bidmodifiers.validation.BidModifiersDefects.deviceBidModifiersAllZeros;
import static ru.yandex.direct.core.entity.bidmodifiers.validation.BidModifiersDefects.notSupportedMultiplier;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoErrors;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@ParametersAreNonnullByDefault
public class BidModifierValidationMobileTypeSupportTest {
    private static final Path errorPath = path(field(MOBILE_ADJUSTMENT.name()));
    private ClientId clientId;
    private CpmBannerAdGroup adGroupCpmBanner;
    private CpmVideoAdGroup adGroupCpmVideo;
    private BidModifierMobile modifier;
    private BidModifierValidationMobileTypeSupport service;
    private FeatureService featureService;

    private FakeBidModifierRepository bidModifierRepository;
    private DeviceModifiersConflictChecker conflictChecker;

    @Before
    public void setUp() {
        clientId = ClientId.fromLong(1L);

        bidModifierRepository = new FakeBidModifierRepository(List.of());
        conflictChecker = new DeviceModifiersConflictChecker(new FakeShardByClient(), bidModifierRepository);
        service = new BidModifierValidationMobileTypeSupport(conflictChecker);
        clientId = ClientId.fromLong(1L);
        adGroupCpmBanner = new CpmBannerAdGroup().withType(AdGroupType.CPM_BANNER);
        adGroupCpmVideo = new CpmVideoAdGroup().withType(AdGroupType.CPM_VIDEO);
        modifier = new BidModifierMobile().withMobileAdjustment(
                new BidModifierMobileAdjustment().withPercent(120));
        featureService = mock(FeatureService.class);
    }

    @Test
    public void validateAddStep1_cpmAdGroupWithKeywords_passes() {
        ValidationResult<BidModifierMobile, Defect> vr = service.validateAddStep1(modifier.withAdGroupId(1L),
                CampaignType.CPM_BANNER, adGroupCpmBanner.withCriterionType(CriterionType.KEYWORD), clientId,
                new CachingFeaturesProvider(featureService));
        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validateAddStep1_cpmAdGroupWithoutKeywords_errorIsGenerated() {
        ValidationResult<BidModifierMobile, Defect> vr = service.validateAddStep1(modifier.withAdGroupId(1L),
                CampaignType.CPM_BANNER, adGroupCpmBanner.withCriterionType(CriterionType.USER_PROFILE), clientId,
                new CachingFeaturesProvider(featureService));
        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validateAddStep1_cpmAdGroupVideo_success() {
        ValidationResult<BidModifierMobile, Defect> vr = service.validateAddStep1(modifier.withAdGroupId(1L),
                CampaignType.CPM_BANNER, adGroupCpmVideo, clientId,
                new CachingFeaturesProvider(featureService));
        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));

    }

    @Test
    public void validateAddStep1_cpmBannerCampaign_errorIsGenerated() {
        ValidationResult<BidModifierMobile, Defect> vr = service.validateAddStep1(modifier.withCampaignId(1L),
                CampaignType.CPM_BANNER,
                null, clientId, new CachingFeaturesProvider(featureService));
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(
                validationError(errorPath, notSupportedMultiplier()))));
    }

    @Test
    public void validateAddStep1_cpmDealsCampaign_errorIsGenerated() {
        ValidationResult<BidModifierMobile, Defect> vr = service.validateAddStep1(modifier.withCampaignId(1L),
                CampaignType.CPM_DEALS, null,
                clientId, new CachingFeaturesProvider(featureService));
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(
                validationError(errorPath, notSupportedMultiplier()))));
    }

    @Test
    public void validateAddStep1_cpmYndxFrontpazgeCampaign_errorIsGenerated() {
        ValidationResult<BidModifierMobile, Defect> vr = service.validateAddStep1(modifier.withCampaignId(1L),
                CampaignType.CPM_YNDX_FRONTPAGE, null,
                clientId, new CachingFeaturesProvider(featureService));
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(
                validationError(errorPath, notSupportedMultiplier()))));
    }

    @Test
    public void validateAddStep1_contentPromotionCampaign_noErrorIsGenerated() {
        ValidationResult<BidModifierMobile, Defect> vr = service.validateAddStep1(modifier.withCampaignId(1L),
                CampaignType.CONTENT_PROMOTION, null, clientId,
                new CachingFeaturesProvider(featureService));
        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validateAddStep1_contentPromotionCampaignLowPercent_success() {
        ValidationResult<BidModifierMobile, Defect> vr = service.validateAddStep1(
                modifier.withCampaignId(1L).withMobileAdjustment(new BidModifierMobileAdjustment().withPercent(40)),
                CampaignType.CONTENT_PROMOTION, null, clientId,
                new CachingFeaturesProvider(featureService));
        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validateAddStep1_settingOs_success() {
        ValidationResult<BidModifierMobile, Defect> vr = service.validateAddStep1(
                modifier.withCampaignId(1L)
                        .withMobileAdjustment(new BidModifierMobileAdjustment()
                                .withPercent(110)
                                .withOsType(OsType.ANDROID)),
                CampaignType.TEXT, null, clientId,
                new CachingFeaturesProvider(featureService));
        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validateAddStep1_settingOs_noErrorIsGenerated() {
        ValidationResult<BidModifierMobile, Defect> vr = service.validateAddStep1(
                modifier.withCampaignId(1L)
                        .withMobileAdjustment(new BidModifierMobileAdjustment()
                                .withPercent(110)
                                .withOsType(OsType.ANDROID)),
                CampaignType.TEXT, new AdGroup().withType(AdGroupType.BASE), clientId,
                new CachingFeaturesProvider(featureService));
        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validateAddStep1_TurnOffOs_noErrorIsGenerated() {
        ValidationResult<BidModifierMobile, Defect> vr = service.validateAddStep1(
                modifier.withCampaignId(1L)
                        .withMobileAdjustment(new BidModifierMobileAdjustment()
                                .withPercent(0)
                                .withOsType(OsType.ANDROID)),
                CampaignType.TEXT, new AdGroup().withType(AdGroupType.BASE), clientId,
                new CachingFeaturesProvider(featureService));
        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validateAddStep1_TurnOffMobile_success() {
        ValidationResult<BidModifierMobile, Defect> vr = service.validateAddStep1(
                modifier.withCampaignId(1L)
                        .withMobileAdjustment(new BidModifierMobileAdjustment()
                                .withPercent(0)),
                CampaignType.TEXT, new AdGroup().withType(AdGroupType.BASE), clientId,
                new CachingFeaturesProvider(featureService));
        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    private ValidationResult<List<BidModifierMobile>, Defect> testValidateAddStep2(
            CampaignType campaignType,
            List<BidModifier> modifiersInOperation,
            List<BidModifier> modifiersInDB
    ) {
        bidModifierRepository = new FakeBidModifierRepository(modifiersInDB);
        conflictChecker = new DeviceModifiersConflictChecker(new FakeShardByClient(), bidModifierRepository);
        service = new BidModifierValidationMobileTypeSupport(conflictChecker);

        Map<Long, CampaignType> campaignTypes = Map.of(1L, campaignType);
        Map<Long, AdGroup> adGroupsWithType = Map.of(1L, new AdGroup().withType(AdGroupType.BASE));

        var modifiersToValidate = modifiersInOperation.stream()
                .filter(m -> m instanceof BidModifierMobile)
                .map(m -> (BidModifierMobile) m)
                .collect(Collectors.toList());
        Map<BidModifierKey, BidModifier> allValidBidModifiersInOperation = modifiersInOperation
                .stream().collect(Collectors.toMap(BidModifierKey::new, m -> m));

        Map<BidModifierKey, BidModifierMobile> existingModifiers = modifiersInDB.stream()
                .filter(m -> m instanceof BidModifierMobile)
                .map(m -> (BidModifierMobile) m)
                .collect(Collectors.toMap(BidModifierKey::new, m -> m));

        return service.validateAddStep2(clientId,
                modifiersToValidate,
                existingModifiers,
                campaignTypes,
                adGroupsWithType,
                new CachingFeaturesProvider(featureService),
                allValidBidModifiersInOperation);
    }

    @Test
    public void validateAddStep2_happyPath() {
        var result = testValidateAddStep2(
                CampaignType.TEXT,
                List.of(BidModifierTestHelper.getMobile()),
                Collections.emptyList()
        );

        assertThat(result).is(matchedBy(hasNoErrors()));
    }

    @Test
    public void validateAddStep2_noZeroValues_forTextCampaign() {
        var result = testValidateAddStep2(
                CampaignType.TEXT,
                List.of(BidModifierTestHelper.getZeroMobile(), BidModifierTestHelper.getZeroDesktop()),
                Collections.emptyList()
        );

        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0)), deviceBidModifiersAllZeros()))));
    }

    @Test
    public void validateAddStep2_noZeroValues_forMediaCampaign_noFeature() {
        var result = testValidateAddStep2(
                CampaignType.CPM_BANNER,
                List.of(BidModifierTestHelper.getZeroMobile(), BidModifierTestHelper.getZeroDesktop()),
                Collections.emptyList()
        );

        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0)), deviceBidModifiersAllZeros()))));
    }

    @Test
    public void validateAddStep2_allowsSameZeroValues_forMediaCampaign_withFeature() {
        when(featureService.isEnabledForClientId(clientId, FeatureName.SMARTTV_BID_MODIFIER_ENABLED)).thenReturn(true);

        var result = testValidateAddStep2(
                CampaignType.CPM_BANNER,
                List.of(BidModifierTestHelper.getZeroDesktop(), BidModifierTestHelper.getZeroMobile()),
                Collections.emptyList()
        );

        assertThat(result).is(matchedBy(hasNoErrors()));
    }

    @Test
    public void validateAddStep2_noZeroValues_forMediaCampaign_withFeature() {
        when(featureService.isEnabledForClientId(clientId, FeatureName.SMARTTV_BID_MODIFIER_ENABLED)).thenReturn(true);

        var result = testValidateAddStep2(
                CampaignType.CPM_BANNER,
                List.of(BidModifierTestHelper.getZeroMobile(),
                        BidModifierTestHelper.getZeroDesktop()
                ),
                List.of(BidModifierTestHelper.getZeroSmartTv())
        );

        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0)), deviceBidModifiersAllZeros()))));
    }

    @Test
    public void validateAddStep2_noZeroValues_forTextCampaign_withDesktopOnly() {
        var result = testValidateAddStep2(
                CampaignType.TEXT,
                List.of(BidModifierTestHelper.getZeroMobile(),
                        BidModifierTestHelper.getZeroDesktopOnly(),
                        BidModifierTestHelper.getZeroTablet()),
                Collections.emptyList()
        );

        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0)), deviceBidModifiersAllZeros()))));
    }

    @Test
    public void validateAddStep2_noZeroValues_forMediaCampaign_withDesktopOnly_noFeature() {
        var result = testValidateAddStep2(
                CampaignType.CPM_BANNER,
                List.of(BidModifierTestHelper.getZeroMobile(),
                        BidModifierTestHelper.getZeroDesktopOnly(),
                        BidModifierTestHelper.getZeroTablet()),
                Collections.emptyList()
        );

        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0)), deviceBidModifiersAllZeros()))));
    }

    @Test
    public void validateAddStep2_allowsSameZeroValues_forMediaCampaign_withDesktopOnly_withFeature() {
        when(featureService.isEnabledForClientId(clientId, FeatureName.SMARTTV_BID_MODIFIER_ENABLED)).thenReturn(true);

        var result = testValidateAddStep2(
                CampaignType.CPM_BANNER,
                List.of(BidModifierTestHelper.getZeroMobile(),
                        BidModifierTestHelper.getZeroDesktopOnly(),
                        BidModifierTestHelper.getZeroTablet()),
                Collections.emptyList()
        );

        assertThat(result).is(matchedBy(hasNoErrors()));
    }

    @Test
    public void validateAddStep2_noZeroValues_forMediaCampaign_withDesktopOnly_withFeature() {
        when(featureService.isEnabledForClientId(clientId, FeatureName.SMARTTV_BID_MODIFIER_ENABLED)).thenReturn(true);

        var result = testValidateAddStep2(
                CampaignType.CPM_BANNER,
                List.of(BidModifierTestHelper.getZeroMobile(),
                        BidModifierTestHelper.getZeroDesktopOnly()
                ),
                List.of(BidModifierTestHelper.getZeroSmartTv(),
                        BidModifierTestHelper.getZeroTablet())
        );

        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0)), deviceBidModifiersAllZeros()))));
    }

}
