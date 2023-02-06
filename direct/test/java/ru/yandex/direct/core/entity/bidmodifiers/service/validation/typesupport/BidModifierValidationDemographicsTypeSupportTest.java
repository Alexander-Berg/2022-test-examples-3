package ru.yandex.direct.core.entity.bidmodifiers.service.validation.typesupport;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.model.CpmBannerAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmVideoAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CriterionType;
import ru.yandex.direct.core.entity.bidmodifier.AgeType;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDemographics;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDemographicsAdjustment;
import ru.yandex.direct.core.entity.bidmodifiers.service.CachingFeaturesProvider;
import ru.yandex.direct.core.entity.bidmodifiers.validation.typesupport.BidModifierValidationDemographicsTypeSupport;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.bidmodifier.BidModifierDemographics.DEMOGRAPHICS_ADJUSTMENTS;
import static ru.yandex.direct.core.entity.bidmodifiers.validation.BidModifiersDefects.notSupportedMultiplier;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class BidModifierValidationDemographicsTypeSupportTest {
    private static final Path errorPath = path(field(DEMOGRAPHICS_ADJUSTMENTS.name()), index(0));
    private ClientId clientId;
    private CpmBannerAdGroup adGroupCpmBanner;
    private CpmVideoAdGroup adGroupCpmVideo;
    private BidModifierDemographics modifier;
    private BidModifierValidationDemographicsTypeSupport service;
    private FeatureService featureService;

    @Before
    public void setUp() {
        clientId = ClientId.fromLong(1L);
        adGroupCpmBanner = new CpmBannerAdGroup().withType(AdGroupType.CPM_BANNER);
        adGroupCpmVideo = new CpmVideoAdGroup().withType(AdGroupType.CPM_VIDEO);
        modifier = new BidModifierDemographics().withDemographicsAdjustments(singletonList(
                new BidModifierDemographicsAdjustment().withPercent(120).withAge(AgeType._25_34)));
        service = new BidModifierValidationDemographicsTypeSupport();
        featureService = mock(FeatureService.class);
        when(featureService.isEnabledForClientId(clientId, FeatureName.DEMOGRAPHY_BID_MODIFIER_UNKNOWN_AGE_ALLOWED)).thenReturn(false);

    }

    @Test
    public void validateAddStep1_cpmAdGroupWithKeywords_passes() {
        ValidationResult<BidModifierDemographics, Defect> vr = service.validateAddStep1(modifier.withAdGroupId(1L),
                CampaignType.CPM_BANNER, adGroupCpmBanner.withCriterionType(CriterionType.KEYWORD), clientId, new CachingFeaturesProvider(featureService));
        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validateAddStep1_cpmAdGroupWithoutKeywords_errorIsGenerated() {
        ValidationResult<BidModifierDemographics, Defect> vr = service.validateAddStep1(modifier.withAdGroupId(1L),
                CampaignType.CPM_BANNER, adGroupCpmBanner.withCriterionType(CriterionType.USER_PROFILE), clientId,
                new CachingFeaturesProvider(featureService));
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(
                validationError(errorPath, notSupportedMultiplier()))));
    }

    @Test
    public void validateAddStep1_cpmAdGroupVideo_errorIsGenerated() {
        ValidationResult<BidModifierDemographics, Defect> vr = service.validateAddStep1(modifier.withAdGroupId(1L),
                CampaignType.CPM_BANNER, adGroupCpmVideo, clientId, new CachingFeaturesProvider(featureService));
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(
                validationError(errorPath, notSupportedMultiplier()))));
    }

    @Test
    public void validateAddStep1_cpmBannerCampaign_errorIsGenerated() {
        ValidationResult<BidModifierDemographics, Defect> vr = service.validateAddStep1(modifier.withCampaignId(1L),
                CampaignType.CPM_BANNER, null, clientId, new CachingFeaturesProvider(featureService));
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(
                validationError(errorPath, notSupportedMultiplier()))));
    }

    @Test
    public void validateAddStep1_cpmDealsCampaign_errorIsGenerated() {
        ValidationResult<BidModifierDemographics, Defect> vr = service.validateAddStep1(modifier.withCampaignId(1L),
                CampaignType.CPM_DEALS, null, clientId, new CachingFeaturesProvider(featureService));
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(
                validationError(errorPath, notSupportedMultiplier()))));
    }

    @Test
    public void validateAddStep1_cpmYndxFrontpageCampaign_errorIsGenerated() {
        ValidationResult<BidModifierDemographics, Defect> vr = service.validateAddStep1(modifier.withCampaignId(1L),
                CampaignType.CPM_YNDX_FRONTPAGE, null, clientId, new CachingFeaturesProvider(featureService));
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(
                validationError(errorPath, notSupportedMultiplier()))));
    }

    @Test
    public void validateAddStep1_contentPromotionCampaign_NoErrorIsGenerated() {
        ValidationResult<BidModifierDemographics, Defect> vr = service.validateAddStep1(modifier.withCampaignId(1L),
                CampaignType.CONTENT_PROMOTION, null, clientId, new CachingFeaturesProvider(featureService));
        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }
}
