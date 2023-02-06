package ru.yandex.direct.core.entity.bidmodifiers.add.performance;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.bidmodifier.BidModifierPerformanceTgoAdjustment;
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.CampaignSteps;
import ru.yandex.direct.result.MassResult;
import ru.yandex.qatools.allure.annotations.Description;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static ru.yandex.direct.core.entity.bidmodifiers.validation.BidModifiersDefects.notSupportedMultiplier;
import static ru.yandex.direct.core.entity.bidmodifiers.validation.BidModifiersDefects.singleValueModifierAlreadyExists;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultPerformanceTgoAdjustment;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyClientPerformanceTgoModifier;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
@Description("Негативные сценарии добавления корректировок ставок на Смарт-ТГО")
public class AddBidModifiersPerformanceTgoNegativeTest {
    @Autowired
    private BidModifierService bidModifierService;

    @Autowired
    private CampaignSteps campaignSteps;

    @Test
    public void addPerformanceTgoModifierToWrongCampaign() {
        CampaignInfo campaign = campaignSteps.createActiveTextCampaign();

        BidModifierPerformanceTgoAdjustment adjustment = createDefaultPerformanceTgoAdjustment();
        MassResult<List<Long>> result = addModifier(campaign, adjustment);

        Assert.assertThat(result.getValidationResult(), hasDefectWithDefinition(
                validationError(path(index(0), field("performanceTgoAdjustment")),
                        notSupportedMultiplier())));
    }

    @Test
    public void addTwoPerformanceTgoModifiersTest() {
        CampaignInfo campaign = campaignSteps.createActivePerformanceCampaign();

        BidModifierPerformanceTgoAdjustment adjustment = createDefaultPerformanceTgoAdjustment();
        MassResult<List<Long>> firstResult = addModifier(campaign, adjustment);
        MassResult<List<Long>> secondResult = addModifier(campaign, adjustment);

        assertSoftly(softly -> {
            softly.assertThat(firstResult.getValidationResult()).is(matchedBy(hasNoDefectsDefinitions()));
            softly.assertThat(secondResult.getValidationResult()).is(matchedBy(hasDefectWithDefinition(
                    validationError(path(index(0), field("performanceTgoAdjustment")),
                            singleValueModifierAlreadyExists()))));
        });
    }

    private MassResult<List<Long>> addModifier(CampaignInfo campaign, BidModifierPerformanceTgoAdjustment adjustment) {
        return bidModifierService.add(
                singletonList(
                        createEmptyClientPerformanceTgoModifier()
                                .withEnabled(true)
                                .withCampaignId(campaign.getCampaignId())
                                .withPerformanceTgoAdjustment(adjustment)
                ), campaign.getClientId(), campaign.getUid());
    }
}
