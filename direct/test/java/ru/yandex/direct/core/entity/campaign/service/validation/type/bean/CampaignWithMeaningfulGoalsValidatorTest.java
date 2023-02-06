package ru.yandex.direct.core.entity.campaign.service.validation.type.bean;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import one.util.streamex.LongStreamEx;
import one.util.streamex.StreamEx;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.core.entity.campaign.model.CampaignWithMeaningfulGoalsWithRequiredFields;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.MeaningfulGoal;
import ru.yandex.direct.core.entity.campaign.model.StrategyName;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.service.RequestBasedMetrikaClientAdapter;
import ru.yandex.direct.core.entity.campaign.service.validation.type.container.CampaignValidationContainer;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.metrika.client.model.response.CounterInfoDirect;
import ru.yandex.direct.metrika.client.model.response.UserCountersExtended;
import ru.yandex.direct.test.utils.RandomNumberUtils;
import ru.yandex.direct.validation.defect.CollectionDefects;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.MEANINGFUL_GOALS_MAX_COUNT;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.invalidValue;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@ParametersAreNonnullByDefault
public class CampaignWithMeaningfulGoalsValidatorTest {

    private CampaignValidationContainer container;
    private RequestBasedMetrikaClientAdapter metrikaClientAdapter;

    @Before
    public void before() {
        metrikaClientAdapter = mock(RequestBasedMetrikaClientAdapter.class);
        when(metrikaClientAdapter.getUsersCountersNumExtendedByCampaignCounterIds()).thenReturn(List.of());
        container = mock(CampaignValidationContainer.class);
        when(container.isCopy()).thenReturn(false);
        when(container.getMetrikaClient()).thenReturn(metrikaClientAdapter);
    }

    @Test
    public void maxMeaningfulGoalCount_HasError() {
        Set<Goal> goals = LongStreamEx.range(201)
                .boxed()
                .map(goalId -> new Goal().withId(goalId))
                .select(Goal.class)
                .toSet();
        TextCampaign campaign = createCampaign(goals, null, null);

        CampaignWithMeaningfulGoalsValidator validator =
                new CampaignWithMeaningfulGoalsValidator(goals, container, false, Set.of());

        ValidationResult<CampaignWithMeaningfulGoalsWithRequiredFields, Defect> vr = validator.apply(campaign);

        assertThat(vr).is(matchedBy(hasDefectWithDefinition(validationError(path(field(TextCampaign.MEANINGFUL_GOALS)),
                CollectionDefects.maxCollectionSize(MEANINGFUL_GOALS_MAX_COUNT)))));
    }

    @Test
    public void valueFromMetrikaForCrrStrategy_Valid() {
        Set<Goal> goals = Set.of((Goal) new Goal().withId(1L));
        TextCampaign campaign = createCampaign(goals, StrategyName.AUTOBUDGET_CRR, true);

        Set<String> enabledFeatures = Set.of(FeatureName.ALLOW_MEANINGFUL_GOAL_VALUE_FROM_METRIKA.getName());
        CampaignWithMeaningfulGoalsValidator validator =
                new CampaignWithMeaningfulGoalsValidator(goals, container, false, enabledFeatures);

        ValidationResult<CampaignWithMeaningfulGoalsWithRequiredFields, Defect> vr = validator.apply(campaign);

        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void valueFromMetrikaForNotCrrStrategy_HasError() {
        Set<Goal> goals = Set.of((Goal) new Goal().withId(2L));
        TextCampaign campaign = createCampaign(goals, StrategyName.AUTOBUDGET_AVG_CPA, true);

        Set<String> enabledFeatures = Set.of(FeatureName.ALLOW_MEANINGFUL_GOAL_VALUE_FROM_METRIKA.getName());
        CampaignWithMeaningfulGoalsValidator validator =
                new CampaignWithMeaningfulGoalsValidator(goals, container, false, enabledFeatures);

        ValidationResult<CampaignWithMeaningfulGoalsWithRequiredFields, Defect> vr = validator.apply(campaign);

        assertThat(vr).is(matchedBy(hasDefectWithDefinition(validationError(path(field(TextCampaign.MEANINGFUL_GOALS),
                        index(0), field(MeaningfulGoal.IS_METRIKA_SOURCE_OF_VALUE)),
                invalidValue()))));

    }

    @Test
    public void valueFromMetrikaForCrrStrategyWithoutFeature_HasError() {
        Set<Goal> goals = Set.of((Goal) new Goal().withId(2L));
        TextCampaign campaign = createCampaign(goals, StrategyName.AUTOBUDGET_CRR, true);

        CampaignWithMeaningfulGoalsValidator validator =
                new CampaignWithMeaningfulGoalsValidator(goals, container, false, Set.of());

        ValidationResult<CampaignWithMeaningfulGoalsWithRequiredFields, Defect> vr = validator.apply(campaign);

        assertThat(vr).is(matchedBy(hasDefectWithDefinition(validationError(path(field(TextCampaign.MEANINGFUL_GOALS),
                        index(0), field(MeaningfulGoal.IS_METRIKA_SOURCE_OF_VALUE)),
                invalidValue()))));
    }

    @Test
    public void falseValueFromMetrikaForNotCrrStrategyWithFeature_Valid() {
        Set<Goal> goals = Set.of((Goal) new Goal().withId(2L));
        TextCampaign campaign = createCampaign(goals, StrategyName.AUTOBUDGET_AVG_CPA, false);

        CampaignWithMeaningfulGoalsValidator validator =
                new CampaignWithMeaningfulGoalsValidator(goals, container, false, Set.of());

        ValidationResult<CampaignWithMeaningfulGoalsWithRequiredFields, Defect> vr = validator.apply(campaign);

        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void crrStrategy_WithUnavailableGoal_Valid() {
        int userCounterId = 11;
        int goalCounterId = 22;
        when(metrikaClientAdapter.getUsersCountersNumExtendedByCampaignCounterIds())
                .thenReturn(List.of(new UserCountersExtended()
                        .withCounters(List.of(new CounterInfoDirect().withId(userCounterId)))));

        Set<Goal> goals = Set.of((Goal) new Goal().withId(2L).withCounterId(goalCounterId));
        TextCampaign campaign = createCampaign(goals, StrategyName.AUTOBUDGET_CRR, false);

        Set<String> enabledFeatures = Set.of(FeatureName.ALLOW_MEANINGFUL_GOAL_VALUE_FROM_METRIKA.getName(),
                FeatureName.DIRECT_UNAVAILABLE_GOALS_ALLOWED.getName());
        CampaignWithMeaningfulGoalsValidator validator =
                new CampaignWithMeaningfulGoalsValidator(goals, container, false, enabledFeatures);

        ValidationResult<CampaignWithMeaningfulGoalsWithRequiredFields, Defect> vr = validator.apply(campaign);

        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void valueFromMetrikaForCrrStrategy_WithUnavailableGoalWithoutGoalFeature_Valid() {
        int userCounterId = 11;
        int goalCounterId = 22;
        when(metrikaClientAdapter.getUsersCountersNumExtendedByCampaignCounterIds())
                .thenReturn(List.of(new UserCountersExtended()
                        .withCounters(List.of(new CounterInfoDirect().withId(userCounterId)))));

        Set<Goal> goals = Set.of((Goal) new Goal().withId(2L).withCounterId(goalCounterId));
        TextCampaign campaign = createCampaign(goals, StrategyName.AUTOBUDGET_CRR, true);

        Set<String> enabledFeatures = Set.of(FeatureName.ALLOW_MEANINGFUL_GOAL_VALUE_FROM_METRIKA.getName());
        CampaignWithMeaningfulGoalsValidator validator =
                new CampaignWithMeaningfulGoalsValidator(goals, container, false, enabledFeatures);

        ValidationResult<CampaignWithMeaningfulGoalsWithRequiredFields, Defect> vr = validator.apply(campaign);

        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void valueFromMetrikaForCrrStrategyWithUnavailableGoal_HasError() {
        int userCounterId = 11;
        int goalCounterId = 22;
        when(metrikaClientAdapter.getUsersCountersNumExtendedByCampaignCounterIds())
                .thenReturn(List.of(new UserCountersExtended()
                        .withCounters(List.of(new CounterInfoDirect().withId(userCounterId)))));

        Set<Goal> goals = Set.of((Goal) new Goal().withId(2L).withCounterId(goalCounterId));
        TextCampaign campaign = createCampaign(goals, StrategyName.AUTOBUDGET_CRR, true);

        Set<String> enabledFeatures = Set.of(FeatureName.ALLOW_MEANINGFUL_GOAL_VALUE_FROM_METRIKA.getName(),
                FeatureName.DIRECT_UNAVAILABLE_GOALS_ALLOWED.getName());
        CampaignWithMeaningfulGoalsValidator validator =
                new CampaignWithMeaningfulGoalsValidator(goals, container, false, enabledFeatures);

        ValidationResult<CampaignWithMeaningfulGoalsWithRequiredFields, Defect> vr = validator.apply(campaign);

        assertThat(vr).is(matchedBy(hasDefectWithDefinition(validationError(
                path(field(TextCampaign.MEANINGFUL_GOALS), index(0), field(MeaningfulGoal.IS_METRIKA_SOURCE_OF_VALUE)),
                invalidValue()))));
    }

    private TextCampaign createCampaign(Set<Goal> goals,
                                        @Nullable StrategyName strategyName,
                                        @Nullable Boolean isMetrikaSourceOfValue) {
        var campaign = new TextCampaign()
                .withId(RandomNumberUtils.nextPositiveLong())
                .withCurrency(CurrencyCode.RUB)
                .withMeaningfulGoals(createMeaningfulGoals(goals, isMetrikaSourceOfValue));
        if (strategyName != null) {
            campaign.withStrategy((DbStrategy) new DbStrategy().withStrategyName(strategyName));
        }
        return campaign;
    }

    private List<MeaningfulGoal> createMeaningfulGoals(Set<Goal> goals, @Nullable Boolean isMetrikaSourceOfValue) {
        return StreamEx.of(goals)
                .map(goal -> new MeaningfulGoal()
                        .withConversionValue(BigDecimal.TEN)
                        .withIsMetrikaSourceOfValue(isMetrikaSourceOfValue)
                        .withGoalId(goal.getId())
                ).toList();
    }
}
