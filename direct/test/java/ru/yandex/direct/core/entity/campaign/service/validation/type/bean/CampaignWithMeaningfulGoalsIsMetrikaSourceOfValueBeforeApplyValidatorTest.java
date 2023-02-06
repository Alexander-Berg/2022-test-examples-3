package ru.yandex.direct.core.entity.campaign.service.validation.type.bean;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import ru.yandex.direct.core.entity.campaign.model.CampaignWithMeaningfulGoalsWithRequiredFields;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.MeaningfulGoal;
import ru.yandex.direct.core.entity.campaign.model.StrategyName;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.service.validation.type.bean.meaningfulgoals.CampaignWithMeaningfulGoalsIsMetrikaSourceOfValueBeforeApplyValidator;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.test.utils.RandomNumberUtils;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.campaign.model.CampaignSource.DIRECT;
import static ru.yandex.direct.feature.FeatureName.ALLOW_MEANINGFUL_GOAL_VALUE_FROM_METRIKA;
import static ru.yandex.direct.feature.FeatureName.DIRECT_UNAVAILABLE_GOALS_ALLOWED;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.invalidValue;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class CampaignWithMeaningfulGoalsIsMetrikaSourceOfValueBeforeApplyValidatorTest {
    private static final long GOAL_ID = 1L;
    private static final int COUNTER_ID = 1;
    private final CampaignWithMeaningfulGoalsWithRequiredFields crrStrategyCampaign =
            new TextCampaign()
                    .withStrategy((DbStrategy) new DbStrategy().withStrategyName(StrategyName.AUTOBUDGET_CRR))
                    .withCurrency(CurrencyCode.RUB)
                    .withSource(DIRECT);
    private final CampaignWithMeaningfulGoalsWithRequiredFields notCrrStrategyCampaign =
            new TextCampaign()
                    .withStrategy((DbStrategy) new DbStrategy().withStrategyName(StrategyName.AUTOBUDGET_AVG_CPA))
                    .withCurrency(CurrencyCode.RUB)
                    .withSource(DIRECT);

    @Test
    public void metrikaIsNotSourceOfValue_Valid() {
        ModelChanges<CampaignWithMeaningfulGoalsWithRequiredFields> mc =
                new ModelChanges<>(RandomNumberUtils.nextPositiveLong(),
                        CampaignWithMeaningfulGoalsWithRequiredFields.class);

        mc.process(buildMeaningfulGoals(false), TextCampaign.MEANINGFUL_GOALS);

        CampaignWithMeaningfulGoalsIsMetrikaSourceOfValueBeforeApplyValidator validator =
                CampaignWithMeaningfulGoalsIsMetrikaSourceOfValueBeforeApplyValidator
                        .build(
                                Set.of(),
                                Set.of(),
                                Set.of(),
                                notCrrStrategyCampaign
                        );

        ValidationResult<ModelChanges<CampaignWithMeaningfulGoalsWithRequiredFields>, Defect> vr = validator.apply(mc);

        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void metrikaIsSourceOfValue_Valid() {
        ModelChanges<CampaignWithMeaningfulGoalsWithRequiredFields> mc =
                new ModelChanges<>(RandomNumberUtils.nextPositiveLong(),
                        CampaignWithMeaningfulGoalsWithRequiredFields.class);
        mc.process(buildMeaningfulGoals(true), TextCampaign.MEANINGFUL_GOALS);

        CampaignWithMeaningfulGoalsIsMetrikaSourceOfValueBeforeApplyValidator validator =
                CampaignWithMeaningfulGoalsIsMetrikaSourceOfValueBeforeApplyValidator
                        .build(
                                Set.of((Goal) new Goal().withId(GOAL_ID).withCounterId(COUNTER_ID)),
                                Set.of(COUNTER_ID),
                                Set.of(ALLOW_MEANINGFUL_GOAL_VALUE_FROM_METRIKA.getName()),
                                crrStrategyCampaign
                        );

        ValidationResult<ModelChanges<CampaignWithMeaningfulGoalsWithRequiredFields>, Defect> vr = validator.apply(mc);

        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void metrikaIsSourceOfValueGoalValueFromMetrikaNotAllowed_HasError() {
        ModelChanges<CampaignWithMeaningfulGoalsWithRequiredFields> mc =
                new ModelChanges<>(RandomNumberUtils.nextPositiveLong(),
                        CampaignWithMeaningfulGoalsWithRequiredFields.class);
        mc.process(buildMeaningfulGoals(true), TextCampaign.MEANINGFUL_GOALS);

        CampaignWithMeaningfulGoalsIsMetrikaSourceOfValueBeforeApplyValidator validator =
                CampaignWithMeaningfulGoalsIsMetrikaSourceOfValueBeforeApplyValidator
                        .build(
                                Set.of((Goal) new Goal().withId(GOAL_ID).withCounterId(COUNTER_ID)),
                                Set.of(COUNTER_ID),
                                Set.of(),
                                crrStrategyCampaign
                        );

        ValidationResult<ModelChanges<CampaignWithMeaningfulGoalsWithRequiredFields>, Defect> vr = validator.apply(mc);

        assertThat(vr).is(matchedBy(hasDefectWithDefinition(validationError(path(field(TextCampaign.MEANINGFUL_GOALS),
                        index(0), field(MeaningfulGoal.IS_METRIKA_SOURCE_OF_VALUE)),
                invalidValue()))));
    }

    @Test
    public void metrikaIsSourceOfValueUnavailableGoal_HasError() {
        ModelChanges<CampaignWithMeaningfulGoalsWithRequiredFields> mc =
                new ModelChanges<>(RandomNumberUtils.nextPositiveLong(),
                        CampaignWithMeaningfulGoalsWithRequiredFields.class);
        mc.process(buildMeaningfulGoals(true), TextCampaign.MEANINGFUL_GOALS);

        CampaignWithMeaningfulGoalsIsMetrikaSourceOfValueBeforeApplyValidator validator =
                CampaignWithMeaningfulGoalsIsMetrikaSourceOfValueBeforeApplyValidator
                        .build(
                                Set.of((Goal) new Goal().withId(GOAL_ID).withCounterId(COUNTER_ID)),
                                Set.of(),
                                Set.of(ALLOW_MEANINGFUL_GOAL_VALUE_FROM_METRIKA.getName(),
                                        DIRECT_UNAVAILABLE_GOALS_ALLOWED.getName()),
                                crrStrategyCampaign
                        );

        ValidationResult<ModelChanges<CampaignWithMeaningfulGoalsWithRequiredFields>, Defect> vr = validator.apply(mc);

        assertThat(vr).is(matchedBy(hasDefectWithDefinition(validationError(path(field(TextCampaign.MEANINGFUL_GOALS),
                        index(0), field(MeaningfulGoal.IS_METRIKA_SOURCE_OF_VALUE)),
                invalidValue()))));
    }

    @Test
    public void metrikaIsSourceOfValueNotCrrStrategy_hasError() {
        ModelChanges<CampaignWithMeaningfulGoalsWithRequiredFields> mc =
                new ModelChanges<>(RandomNumberUtils.nextPositiveLong(),
                        CampaignWithMeaningfulGoalsWithRequiredFields.class);
        mc.process(buildMeaningfulGoals(true), TextCampaign.MEANINGFUL_GOALS);

        CampaignWithMeaningfulGoalsIsMetrikaSourceOfValueBeforeApplyValidator validator =
                CampaignWithMeaningfulGoalsIsMetrikaSourceOfValueBeforeApplyValidator
                        .build(
                                Set.of((Goal) new Goal().withId(GOAL_ID).withCounterId(COUNTER_ID)),
                                Set.of(COUNTER_ID),
                                Set.of(ALLOW_MEANINGFUL_GOAL_VALUE_FROM_METRIKA.getName()),
                                notCrrStrategyCampaign
                        );

        ValidationResult<ModelChanges<CampaignWithMeaningfulGoalsWithRequiredFields>, Defect> vr = validator.apply(mc);

        assertThat(vr).is(matchedBy(hasDefectWithDefinition(validationError(path(field(TextCampaign.MEANINGFUL_GOALS),
                        index(0), field(MeaningfulGoal.IS_METRIKA_SOURCE_OF_VALUE)),
                invalidValue()))));
    }

    private List<MeaningfulGoal> buildMeaningfulGoals(boolean isMetrikaSourceOfValue) {
        return List.of(new MeaningfulGoal()
                .withGoalId(GOAL_ID)
                .withIsMetrikaSourceOfValue(isMetrikaSourceOfValue)
                .withConversionValue(BigDecimal.TEN));
    }
}
