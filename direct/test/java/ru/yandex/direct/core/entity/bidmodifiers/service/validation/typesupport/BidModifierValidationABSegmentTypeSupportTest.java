package ru.yandex.direct.core.entity.bidmodifiers.service.validation.typesupport;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.model.CpmYndxFrontpageAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.TextAdGroup;
import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierABSegment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierABSegmentAdjustment;
import ru.yandex.direct.core.entity.bidmodifiers.validation.typesupport.BidModifierValidationABSegmentsTypeSupport;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.GoalType;
import ru.yandex.direct.core.entity.retargeting.service.RetargetingConditionService;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.bidmodifier.BidModifierABSegment.AB_SEGMENT_ADJUSTMENTS;
import static ru.yandex.direct.core.entity.bidmodifiers.validation.BidModifiersDefects.abSegmentBidModifiersNotSupportedOnAdGroups;
import static ru.yandex.direct.core.entity.bidmodifiers.validation.BidModifiersDefects.notSupportedMultiplier;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class BidModifierValidationABSegmentTypeSupportTest {
    private static final Path errorPath = path(field(AB_SEGMENT_ADJUSTMENTS.name()), index(0));

    public ClientId clientId;
    public BidModifierABSegment modifier;
    public BidModifierValidationABSegmentsTypeSupport service;
    private AdGroup adGroup;
    private CpmYndxFrontpageAdGroup adGroupYndxFrontpage;

    @Before
    public void setUp() {
        clientId = ClientId.fromLong(1L);

        List<BidModifierABSegmentAdjustment> abSegmentAdjustments2 = singletonList(
                new BidModifierABSegmentAdjustment().withSegmentId(1L).withSectionId(1L).withPercent(100));
        modifier = new BidModifierABSegment().withAbSegmentAdjustments(abSegmentAdjustments2);
        RetargetingConditionService retargetingConditionService = mock(RetargetingConditionService.class);
        when(retargetingConditionService.getAvailableMetrikaGoalsForRetargeting(any(), any(GoalType.class)))
                .thenReturn(singletonList(
                (Goal) new Goal().withSectionId(1L).withId(1L)
        ));
        adGroup = new TextAdGroup().withType(AdGroupType.BASE);
        adGroupYndxFrontpage = new CpmYndxFrontpageAdGroup().withType(AdGroupType.CPM_YNDX_FRONTPAGE);
        service = new BidModifierValidationABSegmentsTypeSupport(retargetingConditionService);

    }

    @Test
    public void validateAddStep1_abSegmentOnAdGroup_abSegmentBidModifiersNotSupportedOnAdGroupsError() {
        ValidationResult<BidModifierABSegment, Defect> vr = service.validateAddStep1(modifier.withAdGroupId(1L),
                CampaignType.TEXT, adGroup, clientId, null);
        assertThat(vr, hasDefectWithDefinition(
                validationError(path(field(BidModifier.AD_GROUP_ID)), abSegmentBidModifiersNotSupportedOnAdGroups())));

    }

    @Test
    public void validateAddStep1_cpmYndxFrontpageCampaign_errorIsGenerated() {
        ValidationResult<BidModifierABSegment, Defect> vr = service.validateAddStep1(modifier.withCampaignId(1L),
                CampaignType.CPM_YNDX_FRONTPAGE, adGroupYndxFrontpage, clientId, null);
        Assertions.assertThat(vr).is(matchedBy(hasDefectDefinitionWith(
                validationError(errorPath, notSupportedMultiplier()))));
    }

    @Test
    public void validateAddStep1_contentPromotionCampaign_errorIsGenerated() {
        ValidationResult<BidModifierABSegment, Defect> vr = service.validateAddStep1(modifier.withCampaignId(1L),
                CampaignType.CONTENT_PROMOTION, null, clientId, null);
        Assertions.assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }
}
