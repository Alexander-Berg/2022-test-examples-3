package ru.yandex.direct.core.entity.bidmodifiers.service.validation.typesupport;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.model.CpmBannerAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmVideoAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CriterionType;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierPerformanceTgo;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierPerformanceTgoAdjustment;
import ru.yandex.direct.core.entity.bidmodifiers.validation.typesupport.BidModifierValidationPerformanceTgoTypeSupport;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.bidmodifier.BidModifierPerformanceTgo.PERFORMANCE_TGO_ADJUSTMENT;
import static ru.yandex.direct.core.entity.bidmodifiers.validation.BidModifiersDefects.notSupportedMultiplier;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class BidModifierPerformanceTgoTypeSupportTest {
    private static final Path errorPath = path(field(PERFORMANCE_TGO_ADJUSTMENT.name()));
    private ClientId clientId;
    private CpmBannerAdGroup adGroupCpmBanner;
    private CpmVideoAdGroup adGroupCpmVideo;
    private BidModifierPerformanceTgo modifier;
    private BidModifierValidationPerformanceTgoTypeSupport service;

    @Before
    public void setUp() {
        clientId = ClientId.fromLong(1L);
        adGroupCpmBanner = new CpmBannerAdGroup().withType(AdGroupType.CPM_BANNER);
        adGroupCpmVideo = new CpmVideoAdGroup().withType(AdGroupType.CPM_VIDEO);
        modifier = new BidModifierPerformanceTgo().withPerformanceTgoAdjustment(
                new BidModifierPerformanceTgoAdjustment().withPercent(120));

        service = new BidModifierValidationPerformanceTgoTypeSupport();
    }

    @Test
    public void validateAddStep1_cpmAdGroupWithKeywords_errorIsGenerated() {
        ValidationResult<BidModifierPerformanceTgo, Defect> vr = service.validateAddStep1(modifier.withAdGroupId(1L),
                CampaignType.CPM_BANNER, adGroupCpmBanner.withCriterionType(CriterionType.KEYWORD), clientId, null);
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(
                validationError(errorPath, notSupportedMultiplier()))));
    }

    @Test
    public void validateAddStep1_cpmAdGroupWithoutKeywords_errorIsGenerated() {
        ValidationResult<BidModifierPerformanceTgo, Defect> vr = service.validateAddStep1(modifier.withAdGroupId(1L),
                CampaignType.CPM_BANNER, adGroupCpmBanner.withCriterionType(CriterionType.USER_PROFILE), clientId,
                null);
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(
                validationError(errorPath, notSupportedMultiplier()))));
    }

    @Test
    public void validateAddStep1_cpmAdGroupVideo_errorIsGenerated() {
        ValidationResult<BidModifierPerformanceTgo, Defect> vr = service.validateAddStep1(modifier.withAdGroupId(1L),
                CampaignType.CPM_BANNER, adGroupCpmVideo, clientId, null);
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(
                validationError(errorPath, notSupportedMultiplier()))));
    }

    @Test
    public void validateAddStep1_cpmBannerCampaign_errorIsGenerated() {
        ValidationResult<BidModifierPerformanceTgo, Defect> vr = service.validateAddStep1(modifier.withCampaignId(1L),
                CampaignType.CPM_BANNER, null, clientId, null);
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(
                validationError(errorPath, notSupportedMultiplier()))));
    }

    @Test
    public void validateAddStep1_cpmDealsCampaign_errorIsGenerated() {
        ValidationResult<BidModifierPerformanceTgo, Defect> vr = service.validateAddStep1(modifier.withCampaignId(1L),
                CampaignType.CPM_DEALS, null, clientId, null);
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(
                validationError(errorPath, notSupportedMultiplier()))));
    }

    @Test
    public void validateAddStep1_cpmYndxFrontpageCampaign_errorIsGenerated() {
        ValidationResult<BidModifierPerformanceTgo, Defect> vr = service.validateAddStep1(modifier.withCampaignId(1L),
                CampaignType.CPM_YNDX_FRONTPAGE, null, clientId, null);
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(
                validationError(errorPath, notSupportedMultiplier()))));
    }

    @Test
    public void validateAddStep1_contentPromotionCampaign_errorIsGenerated() {
        ValidationResult<BidModifierPerformanceTgo, Defect> vr = service.validateAddStep1(modifier.withCampaignId(1L),
                CampaignType.CONTENT_PROMOTION, null, clientId, null);
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(
                validationError(errorPath, notSupportedMultiplier()))));
    }
}
