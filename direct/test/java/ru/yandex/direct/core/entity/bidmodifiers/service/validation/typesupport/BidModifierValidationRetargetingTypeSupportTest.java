package ru.yandex.direct.core.entity.bidmodifiers.service.validation.typesupport;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.model.CpmBannerAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmVideoAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CriterionType;
import ru.yandex.direct.core.entity.bidmodifier.AbstractBidModifierRetargeting;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierRetargeting;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierRetargetingAdjustment;
import ru.yandex.direct.core.entity.bidmodifiers.validation.typesupport.BidModifierValidationRetargetingTypeSupport;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.retargeting.model.ConditionType;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingConditionRepository;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.bidmodifier.BidModifierRetargeting.RETARGETING_ADJUSTMENTS;
import static ru.yandex.direct.core.entity.bidmodifiers.validation.BidModifiersDefects.notSupportedMultiplier;
import static ru.yandex.direct.core.entity.bidmodifiers.validation.BidModifiersDefects.unsupportedRetargetingType;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@ParametersAreNonnullByDefault
public class BidModifierValidationRetargetingTypeSupportTest {
    private static final Path errorPath = path(field(RETARGETING_ADJUSTMENTS.name()), index(0));
    private ClientId clientId;
    private CpmBannerAdGroup adGroupCpmBanner;
    private CpmVideoAdGroup adGroupCpmVideo;
    private BidModifierRetargeting modifier;
    private BidModifierValidationRetargetingTypeSupport service;
    private RetargetingCondition retargetingCondition;

    @Before
    public void setUp() {
        clientId = ClientId.fromLong(1L);
        adGroupCpmBanner = new CpmBannerAdGroup().withType(AdGroupType.CPM_BANNER);
        adGroupCpmVideo = new CpmVideoAdGroup().withType(AdGroupType.CPM_VIDEO);
        modifier = new BidModifierRetargeting().withRetargetingAdjustments(singletonList(
                new BidModifierRetargetingAdjustment().withPercent(120).withRetargetingConditionId(1L)));

        ShardHelper mockHelper = mock(ShardHelper.class);
        when(mockHelper.getShardByClientIdStrictly(clientId)).thenReturn(1);

        retargetingCondition = new RetargetingCondition();
        retargetingCondition.withId(1L).withDeleted(false).withType(ConditionType.metrika_goals);

        RetargetingConditionRepository mockRepository = mock(RetargetingConditionRepository.class);
        when(mockRepository.getFromRetargetingConditionsTable(1, clientId, singleton(1L))).thenReturn(
                singletonList(retargetingCondition));

        service = new BidModifierValidationRetargetingTypeSupport(mockHelper, mockRepository);
    }

    @Test
    public void validateAddStep1_cpmAdGroupWithKeywords_passes() {
        ValidationResult<AbstractBidModifierRetargeting, Defect> vr = service.validateAddStep1(modifier.withAdGroupId(1L),
                CampaignType.CPM_BANNER, adGroupCpmBanner.withCriterionType(CriterionType.KEYWORD), clientId, null);
        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validateAddStep1_cpmAdGroupWithoutKeywords_errorIsGenerated() {
        ValidationResult<AbstractBidModifierRetargeting, Defect> vr = service.validateAddStep1(modifier.withAdGroupId(1L),
                CampaignType.CPM_BANNER, adGroupCpmBanner.withCriterionType(CriterionType.USER_PROFILE), clientId,
                null);
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(
                validationError(errorPath, notSupportedMultiplier()))));
    }

    @Test
    public void validateAddStep1_cpmAdGroupVideo_errorIsGenerated() {
        ValidationResult<AbstractBidModifierRetargeting, Defect> vr = service.validateAddStep1(modifier.withAdGroupId(1L),
                CampaignType.CPM_BANNER, adGroupCpmVideo, clientId, null);
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(
                validationError(errorPath, notSupportedMultiplier()))));
    }

    @Test
    public void validateAddStep1_cpmBannerCampaign_errorIsGenerated() {
        ValidationResult<AbstractBidModifierRetargeting, Defect> vr = service.validateAddStep1(modifier.withCampaignId(1L),
                CampaignType.CPM_BANNER, null, clientId, null);
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(
                validationError(errorPath, notSupportedMultiplier()))));
    }

    @Test
    public void validateAddStep1_cpmDealsCampaign_errorIsGenerated() {
        ValidationResult<AbstractBidModifierRetargeting, Defect> vr = service.validateAddStep1(modifier.withCampaignId(1L),
                CampaignType.CPM_DEALS, null, clientId, null);
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(
                validationError(errorPath, notSupportedMultiplier()))));
    }

    @Test
    public void validateAddStep1_interestsRetargetingCondition_errorIsGenerated() {
        retargetingCondition.withType(ConditionType.interests);
        ValidationResult<AbstractBidModifierRetargeting, Defect> vr = service.validateAddStep1(modifier.withCampaignId(1L),
                CampaignType.TEXT, null, clientId, null);
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(
                validationError(errorPath, unsupportedRetargetingType()))));
    }

    @Test
    public void validateAddStep1_shortcutsRetargetingCondition_passes() {
        retargetingCondition.withType(ConditionType.shortcuts);
        ValidationResult<AbstractBidModifierRetargeting, Defect> vr = service.validateAddStep1(modifier.withCampaignId(1L),
                CampaignType.TEXT, null, clientId, null);
        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validateAddStep1_cpmYndxFrontpageCampaign_errorIsGenerated() {
        ValidationResult<AbstractBidModifierRetargeting, Defect> vr = service.validateAddStep1(modifier.withCampaignId(1L),
                CampaignType.CPM_YNDX_FRONTPAGE, null, clientId, null);
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(
                validationError(errorPath, notSupportedMultiplier()))));
    }

    @Test
    public void validateAddStep1_contentPromotionCampaign_NoErrorIsGenerated() {
        ValidationResult<AbstractBidModifierRetargeting, Defect> vr = service.validateAddStep1(modifier.withCampaignId(1L),
                CampaignType.CONTENT_PROMOTION, null, clientId, null);
        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }
}
