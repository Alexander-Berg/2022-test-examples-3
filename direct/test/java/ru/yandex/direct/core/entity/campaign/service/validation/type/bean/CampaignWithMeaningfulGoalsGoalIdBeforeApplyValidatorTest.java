package ru.yandex.direct.core.entity.campaign.service.validation.type.bean;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import one.util.streamex.LongStreamEx;
import org.junit.Test;

import ru.yandex.direct.core.entity.campaign.model.CampaignWithMeaningfulGoalsWithRequiredFields;
import ru.yandex.direct.core.entity.campaign.model.MeaningfulGoal;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.service.validation.type.bean.meaningfulgoals.CampaignWithMeaningfulGoalsGoalIdBeforeApplyValidator;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.test.utils.RandomNumberUtils;
import ru.yandex.direct.validation.defect.CollectionDefects;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_CPI_GOAL_ID;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.ENGAGED_SESSION_GOAL_ID;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.MEANINGFUL_GOALS_MAX_COUNT;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.ids.CollectionDefectIds.Gen.MUST_BE_IN_COLLECTION;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class CampaignWithMeaningfulGoalsGoalIdBeforeApplyValidatorTest {
    @Test
    public void maxMeaningfulGoalCount_HasError() {
        ModelChanges<CampaignWithMeaningfulGoalsWithRequiredFields> mc =
                new ModelChanges<>(RandomNumberUtils.nextPositiveLong(),
                        CampaignWithMeaningfulGoalsWithRequiredFields.class);

        mc.process(LongStreamEx.range(201)
                        .boxed()
                        .map(goalId -> new MeaningfulGoal().withGoalId(goalId))
                        .toList(),
                TextCampaign.MEANINGFUL_GOALS);

        CampaignWithMeaningfulGoalsGoalIdBeforeApplyValidator validator =
                CampaignWithMeaningfulGoalsGoalIdBeforeApplyValidator.build(Set.of(),
                        false,
                        new TextCampaign().withCurrency(CurrencyCode.RUB));

        ValidationResult<ModelChanges<CampaignWithMeaningfulGoalsWithRequiredFields>, Defect> vr = validator.apply(mc);

        assertThat(vr).is(matchedBy(hasDefectWithDefinition(validationError(path(field(TextCampaign.MEANINGFUL_GOALS)),
                CollectionDefects.maxCollectionSize(MEANINGFUL_GOALS_MAX_COUNT)))));
    }

    @Test
    public void engagedSessionGoalId_Valid() {
        ModelChanges<CampaignWithMeaningfulGoalsWithRequiredFields> mc =
                new ModelChanges<>(RandomNumberUtils.nextPositiveLong(),
                        CampaignWithMeaningfulGoalsWithRequiredFields.class);

        mc.process(List.of(new MeaningfulGoal()
                        .withGoalId(ENGAGED_SESSION_GOAL_ID)
                        .withConversionValue(BigDecimal.TEN)),
                TextCampaign.MEANINGFUL_GOALS);

        CampaignWithMeaningfulGoalsGoalIdBeforeApplyValidator validator =
                CampaignWithMeaningfulGoalsGoalIdBeforeApplyValidator.build(null,
                        false,
                        new TextCampaign().withCurrency(CurrencyCode.RUB));

        ValidationResult<ModelChanges<CampaignWithMeaningfulGoalsWithRequiredFields>, Defect> vr = validator.apply(mc);

        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void defaultCPIGoalId_hasError() {
        ModelChanges<CampaignWithMeaningfulGoalsWithRequiredFields> mc =
                new ModelChanges<>(RandomNumberUtils.nextPositiveLong(),
                        CampaignWithMeaningfulGoalsWithRequiredFields.class);

        mc.process(List.of(new MeaningfulGoal()
                        .withGoalId(DEFAULT_CPI_GOAL_ID)
                        .withConversionValue(BigDecimal.TEN)),
                TextCampaign.MEANINGFUL_GOALS);

        CampaignWithMeaningfulGoalsGoalIdBeforeApplyValidator validator =
                CampaignWithMeaningfulGoalsGoalIdBeforeApplyValidator.build(Set.of(),
                        false,
                        new TextCampaign().withCurrency(CurrencyCode.RUB));

        ValidationResult<ModelChanges<CampaignWithMeaningfulGoalsWithRequiredFields>, Defect> vr = validator.apply(mc);

        assertThat(vr).is(matchedBy(hasDefectWithDefinition(validationError(path(field(TextCampaign.MEANINGFUL_GOALS),
                        index(0), field(MeaningfulGoal.GOAL_ID)),
                MUST_BE_IN_COLLECTION))));
    }
}
