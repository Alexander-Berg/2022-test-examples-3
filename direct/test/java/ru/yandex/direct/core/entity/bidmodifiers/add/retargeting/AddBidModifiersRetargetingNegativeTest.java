package ru.yandex.direct.core.entity.bidmodifiers.add.retargeting;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierRetargetingAdjustment;
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService;
import ru.yandex.direct.core.entity.bidmodifiers.validation.BidModifiersDefects;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.CampaignSteps;
import ru.yandex.direct.core.testing.steps.RetConditionSteps;
import ru.yandex.direct.result.MassResult;
import ru.yandex.qatools.allure.annotations.Description;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.bidmodifiers.validation.BidModifiersDefects.duplicateRetargetingCondition;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyClientRetargetingModifier;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
@Description("Негативные сценарии добавления корректировок ставок ретаргетинга")
public class AddBidModifiersRetargetingNegativeTest {

    @Autowired
    private BidModifierService bidModifierService;

    @Autowired
    private RetConditionSteps retConditionSteps;

    @Autowired
    private CampaignSteps campaignSteps;

    private CampaignInfo campaign;
    private Long retCondId;

    @Before
    public void before() {
        campaign = campaignSteps.createActiveDynamicCampaign();
        retCondId = retConditionSteps.createDefaultRetCondition(campaign.getClientInfo()).getRetConditionId();
    }

    @Test
    @Description("Использование одного условия ретаргетинга в нескольких объектах RetargetingAdjustment в одном запросе")
    public void sameRetargetingConditionInAdjustmentsTest() {
        MassResult<List<Long>> result = addBidModifiers(singletonList(
                createEmptyClientRetargetingModifier().withCampaignId(campaign.getCampaignId())
                        .withRetargetingAdjustments(newArrayList(
                                new BidModifierRetargetingAdjustment()
                                        .withRetargetingConditionId(retCondId).withPercent(110),
                                new BidModifierRetargetingAdjustment()
                                        .withRetargetingConditionId(retCondId).withPercent(120)))));
        assertThat(result.getValidationResult(), hasDefectWithDefinition(
                validationError(path(index(0)), duplicateRetargetingCondition())));
    }

    @Test
    @Description("Добавляем ретаргетинговую корректировку ставок, указывая ранее использованное условие ретаргетинга")
    public void adjustmentIntersectionTest() {
        addBidModifiers(singletonList(
                createEmptyClientRetargetingModifier().withCampaignId(campaign.getCampaignId())
                        .withRetargetingAdjustments(newArrayList(
                                new BidModifierRetargetingAdjustment()
                                        .withRetargetingConditionId(retCondId).withPercent(110)))));
        MassResult<List<Long>> result = addBidModifiers(singletonList(
                createEmptyClientRetargetingModifier().withCampaignId(campaign.getCampaignId())
                        .withRetargetingAdjustments(newArrayList(
                                new BidModifierRetargetingAdjustment()
                                        .withRetargetingConditionId(retCondId).withPercent(120)))));
        assertThat(result.getValidationResult(), hasDefectWithDefinition(
                validationError(path(index(0), field("retargetingAdjustments")),
                        BidModifiersDefects.retargetingConditionAlreadyExists())));
    }

    private MassResult<List<Long>> addBidModifiers(List<BidModifier> bidModifiers) {
        bidModifiers.forEach(bidModifier -> bidModifier.setEnabled(true));
        return bidModifierService.add(bidModifiers, campaign.getClientId(), campaign.getUid());
    }
}
