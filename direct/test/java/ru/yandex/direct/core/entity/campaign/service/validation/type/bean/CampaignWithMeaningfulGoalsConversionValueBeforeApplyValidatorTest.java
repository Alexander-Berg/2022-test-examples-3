package ru.yandex.direct.core.entity.campaign.service.validation.type.bean;

import java.math.BigDecimal;
import java.util.List;

import org.junit.Test;

import ru.yandex.direct.core.entity.campaign.model.CampaignWithMeaningfulGoalsWithRequiredFields;
import ru.yandex.direct.core.entity.campaign.model.MeaningfulGoal;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.service.validation.type.bean.meaningfulgoals.CampaignWithMeaningfulGoalsConversionValueBeforeApplyValidator;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.test.utils.RandomNumberUtils;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.ENGAGED_SESSION_GOAL_ID;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.ids.NumberDefectIds.MUST_BE_IN_THE_INTERVAL_INCLUSIVE;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class CampaignWithMeaningfulGoalsConversionValueBeforeApplyValidatorTest {

    @Test
    public void engagedSessionWithValue_Valid() {
        ModelChanges<CampaignWithMeaningfulGoalsWithRequiredFields> mc =
                new ModelChanges<>(RandomNumberUtils.nextPositiveLong(),
                        CampaignWithMeaningfulGoalsWithRequiredFields.class);
        mc.process(List.of(new MeaningfulGoal()
                        .withGoalId(ENGAGED_SESSION_GOAL_ID)
                        .withConversionValue(BigDecimal.TEN)),
                TextCampaign.MEANINGFUL_GOALS);

        CampaignWithMeaningfulGoalsConversionValueBeforeApplyValidator validator =
                CampaignWithMeaningfulGoalsConversionValueBeforeApplyValidator.build(
                        new TextCampaign().withCurrency(CurrencyCode.RUB));

        ValidationResult<ModelChanges<CampaignWithMeaningfulGoalsWithRequiredFields>, Defect> vr = validator.apply(mc);

        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void engagedSessionWithoutValue_Valid() {

        ModelChanges<CampaignWithMeaningfulGoalsWithRequiredFields> mc =
                new ModelChanges<>(RandomNumberUtils.nextPositiveLong(),
                        CampaignWithMeaningfulGoalsWithRequiredFields.class);
        mc.process(List.of(new MeaningfulGoal()
                        .withGoalId(ENGAGED_SESSION_GOAL_ID)),
                TextCampaign.MEANINGFUL_GOALS);
        CampaignWithMeaningfulGoalsConversionValueBeforeApplyValidator validator =
                CampaignWithMeaningfulGoalsConversionValueBeforeApplyValidator.build(
                        new TextCampaign().withCurrency(CurrencyCode.RUB));

        ValidationResult<ModelChanges<CampaignWithMeaningfulGoalsWithRequiredFields>, Defect> vr = validator.apply(mc);

        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void engagedSessionWithValue_hasError() {
        ModelChanges<CampaignWithMeaningfulGoalsWithRequiredFields> mc =
                new ModelChanges<>(RandomNumberUtils.nextPositiveLong(),
                        CampaignWithMeaningfulGoalsWithRequiredFields.class);
        mc.process(List.of(new MeaningfulGoal()
                        .withGoalId(ENGAGED_SESSION_GOAL_ID)
                        .withConversionValue(CurrencyCode.RUB.getCurrency().getMaxAutobudget().add(BigDecimal.valueOf(1)))),
                TextCampaign.MEANINGFUL_GOALS);

        CampaignWithMeaningfulGoalsConversionValueBeforeApplyValidator validator =
                CampaignWithMeaningfulGoalsConversionValueBeforeApplyValidator.build(
                        new TextCampaign().withCurrency(CurrencyCode.RUB));

        ValidationResult<ModelChanges<CampaignWithMeaningfulGoalsWithRequiredFields>, Defect> vr = validator.apply(mc);

        assertThat(vr).is(matchedBy(hasDefectWithDefinition(validationError(path(field(TextCampaign.MEANINGFUL_GOALS),
                        index(0), field(MeaningfulGoal.CONVERSION_VALUE)),
                MUST_BE_IN_THE_INTERVAL_INCLUSIVE))));
    }
}
