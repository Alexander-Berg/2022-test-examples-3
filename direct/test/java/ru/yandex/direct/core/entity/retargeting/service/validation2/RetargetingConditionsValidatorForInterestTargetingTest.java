package ru.yandex.direct.core.entity.retargeting.service.validation2;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.retargeting.model.ConditionType;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.entity.retargeting.model.Rule;
import ru.yandex.direct.core.entity.retargeting.model.RuleType;
import ru.yandex.direct.core.entity.retargeting.model.TargetingCategory;
import ru.yandex.direct.validation.result.Path;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.retargeting.Constants.GOALS_PER_RULE_FOR_INTEREST_TARGETING;
import static ru.yandex.direct.core.entity.retargeting.Constants.INTEREST_LINK_TIME_VALUE;
import static ru.yandex.direct.core.entity.retargeting.Constants.RULES_PER_CONDITION_FOR_INTEREST_TARGETING;
import static ru.yandex.direct.core.entity.retargeting.service.validation2.RetargetingDefects.inconsistentStateTargetingCategoryUnavailable;
import static ru.yandex.direct.core.entity.retargeting.service.validation2.RetargetingDefects.retargetingConditionAlreadyExists;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CollectionDefects.collectionSizeIsValid;
import static ru.yandex.direct.validation.defect.CommonDefects.inconsistentState;
import static ru.yandex.direct.validation.defect.CommonDefects.invalidValue;
import static ru.yandex.direct.validation.defect.CommonDefects.isNull;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.defect.CommonDefects.objectNotFound;
import static ru.yandex.direct.validation.defect.CommonDefects.validId;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(JUnitParamsRunner.class)
public class RetargetingConditionsValidatorForInterestTargetingTest {

    private static final long CLIENT_ID = 10L;
    private static final String NAME_1 = "name";

    private static final long TARGETING_CATEGORY_IMPORT_ID_1 = 21672155L;
    private static final long TARGETING_CATEGORY_IMPORT_ID_2 = 21672160L;
    private static final long TARGETING_CATEGORY_IMPORT_ID_NOT_AVAILABLE = 21672170L;
    private static final long TARGETING_CATEGORY_IMPORT_ID_THAT_CLIENT_EXIST = 21672180L;
    private static final long CLIENT_GOAL_ID = 21672190L;

    private Goal goalInterest1 =
            (Goal) new Goal().withId(TARGETING_CATEGORY_IMPORT_ID_1).withTime(INTEREST_LINK_TIME_VALUE);
    private Goal goalInterest2 =
            (Goal) new Goal().withId(TARGETING_CATEGORY_IMPORT_ID_2).withTime(INTEREST_LINK_TIME_VALUE);

    private RetargetingConditionsValidator validator;
    private RetargetingCondition retargetingCondition;

    @Before
    public void init() {

        Set<Long> clientGoalIds = Set.of(CLIENT_GOAL_ID);
        Set<Long> clientInterestTargetingIds = Set.of(TARGETING_CATEGORY_IMPORT_ID_THAT_CLIENT_EXIST);

        List<TargetingCategory> targetingCategories = new ArrayList<>();
        targetingCategories.add(new TargetingCategory(51L, 5L, "Новости",
                "NEWS_AND_MAGAZINES", BigInteger.valueOf(TARGETING_CATEGORY_IMPORT_ID_1), true));
        targetingCategories.add(new TargetingCategory(52L, null, "Покупки",
                "SHOPPING", BigInteger.valueOf(TARGETING_CATEGORY_IMPORT_ID_2), true));
        targetingCategories.add(new TargetingCategory(53L, null, "Персонализация",
                "PERSONALIZATION", BigInteger.valueOf(TARGETING_CATEGORY_IMPORT_ID_NOT_AVAILABLE), false));

        validator = RetargetingConditionsValidator.retConditionsIsValid(
                clientGoalIds,
                emptyList(),
                emptyMap(),
                emptySet(),
                emptySet(),
                clientInterestTargetingIds,
                targetingCategories,
                emptyMap(),
                false,
                false,
                false, false);

        retargetingCondition = new RetargetingCondition();
        retargetingCondition.withClientId(CLIENT_ID)
                .withType(ConditionType.metrika_goals)
                .withName(NAME_1)
                .withInterest(true)
                .withRules(singletonList(new Rule()
                        .withType(RuleType.ALL)
                        .withGoals(singletonList(goalInterest1))));
    }

    @Test
    public void validate() {
        assertFalse(validator.apply(singletonList(retargetingCondition)).hasAnyErrors());
    }

    @Test
    public void validate_WithWrongClientId() {
        retargetingCondition.setClientId(0L);

        var result = validator.apply(singletonList(retargetingCondition));
        assertThat(result.flattenErrors(),
                contains(validationError(path(index(0), field("clientId")), validId())));
    }

    @Test
    public void validate_WithoutClientId() {
        retargetingCondition.setClientId(null);

        var result = validator.apply(singletonList(retargetingCondition));
        assertThat(result.flattenErrors(),
                contains(validationError(path(index(0), field("clientId")), notNull())));
    }

    @Test
    public void validate_WithoutType() {
        retargetingCondition.setType(null);

        var result = validator.apply(singletonList(retargetingCondition));
        assertThat(result.flattenErrors(),
                contains(validationError(path(index(0), field("type")), notNull())));
    }

    @SuppressWarnings("unused")
    @Parameterized.Parameters(name = "{0}")
    private static Object[] wrongTypeParameters() {
        return Arrays.stream(ConditionType.values())
                .filter(type -> type != ConditionType.metrika_goals)
                .toArray();
    }

    @Test
    @Parameters(method = "wrongTypeParameters")
    public void validate_WithWrongType(ConditionType wrongType) {
        retargetingCondition.setType(wrongType);

        var result = validator.apply(singletonList(retargetingCondition));
        assertThat(result.flattenErrors(),
                contains(validationError(path(index(0), field("type")), inconsistentState())));
    }

    @Test
    public void validate_WithDescription() {
        retargetingCondition.setDescription("Must be without description");

        var result = validator.apply(singletonList(retargetingCondition));
        assertThat(result.flattenErrors(),
                contains(validationError(path(index(0), field("description")), isNull())));
    }

    @Test
    public void validate_WithoutRules() {
        retargetingCondition.setRules(null);

        var result = validator.apply(singletonList(retargetingCondition));
        assertThat(result.flattenErrors(),
                contains(validationError(path(index(0), field("rules")), notNull())));
    }

    @Test
    public void validate_WithTwoRules() {
        Rule rule1 = new Rule().withType(RuleType.ALL).withGoals(singletonList(goalInterest1));
        Rule rule2 = new Rule().withType(RuleType.ALL).withGoals(singletonList(goalInterest2));
        retargetingCondition.setRules(List.of(rule1, rule2));

        var result = validator.apply(singletonList(retargetingCondition));
        assertThat(result.flattenErrors(), contains(validationError(path(index(0), field("rules")),
                collectionSizeIsValid(RULES_PER_CONDITION_FOR_INTEREST_TARGETING,
                        RULES_PER_CONDITION_FOR_INTEREST_TARGETING))));
    }

    @Test
    public void validate_WithOneNullRule() {
        retargetingCondition.setRules(singletonList(null));

        var result = validator.apply(singletonList(retargetingCondition));
        assertThat(result.flattenErrors(),
                contains(validationError(path(index(0), field("rules"), index(0)), notNull())));
    }

    @Test
    public void validate_WithNullRuleType() {
        retargetingCondition.getRules().get(0).setType(null);

        var result = validator.apply(singletonList(retargetingCondition));
        assertThat(result.flattenErrors(),
                contains(validationError(getPathToRuleField("type"), notNull())));
    }

    @SuppressWarnings("unused")
    @Parameterized.Parameters(name = "{0}")
    private static Object[] wrongRuleTypeParameters() {
        return Arrays.stream(RuleType.values())
                .filter(type -> type != RuleType.ALL)
                .toArray();
    }

    @Test
    @Parameters(method = "wrongRuleTypeParameters")
    public void validate_WithWrongRuleType(RuleType ruleType) {
        retargetingCondition.getRules().get(0).setType(ruleType);

        var result = validator.apply(singletonList(retargetingCondition));
        assertThat(result.flattenErrors(),
                contains(validationError(getPathToRuleField("type"), inconsistentState())));
    }

    @Test
    public void validate_WithoutRuleGoals() {
        retargetingCondition.getRules().get(0).setGoals(null);

        var result = validator.apply(singletonList(retargetingCondition));
        assertThat(result.flattenErrors(),
                contains(validationError(getPathToRuleField("goals"), notNull())));
    }

    @Test
    public void validate_RuleWithTwoGoals() {
        retargetingCondition.getRules().get(0).setGoals(List.of(goalInterest1, goalInterest2));

        var result = validator.apply(singletonList(retargetingCondition));
        assertThat(result.flattenErrors(), contains(validationError(getPathToRuleField("goals"),
                collectionSizeIsValid(GOALS_PER_RULE_FOR_INTEREST_TARGETING,
                        GOALS_PER_RULE_FOR_INTEREST_TARGETING))));
    }

    @Test
    public void validate_RuleWithNullGoal() {
        retargetingCondition.getRules().get(0).setGoals(singletonList(null));

        var result = validator.apply(singletonList(retargetingCondition));
        assertThat(result.flattenErrors(), contains(validationError(
                path(index(0), field("rules"), index(0), field("goals"), index(0)), notNull())));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void validate_WithoutGoalIdAndType() {
        retargetingCondition.getRules().get(0).getGoals().get(0).withId(null);

        // При сбросе Id цели происходит сброс и ее типа
        var result = validator.apply(singletonList(retargetingCondition));
        assertThat(result.flattenErrors(), contains(
                validationError(getPathToGoalField("id"), notNull()),
                validationError(getPathToGoalField("type"), notNull())));
    }

    @Test
    public void validate_WithWrongGoalId() {
        retargetingCondition.getRules().get(0).getGoals().get(0).withId(0L);

        var result = validator.apply(singletonList(retargetingCondition));
        assertThat(result.flattenErrors(),
                contains(validationError(getPathToGoalField("id"), validId())));
    }

    @Test
    public void validate_WhenClientAlreadyHasThisInterest() {
        retargetingCondition.getRules().get(0).getGoals().get(0).withId(TARGETING_CATEGORY_IMPORT_ID_THAT_CLIENT_EXIST);

        var result = validator.apply(singletonList(retargetingCondition));
        assertThat(result.flattenErrors(),
                contains(validationError(getPathToGoalField("id"), retargetingConditionAlreadyExists())));
    }

    @Test
    public void validate_WhenGoalIsNotFromTargetingCategories() {
        retargetingCondition.getRules().get(0).getGoals().get(0).withId(CLIENT_GOAL_ID);

        var result = validator.apply(singletonList(retargetingCondition));
        assertThat(result.flattenErrors(),
                contains(validationError(getPathToGoalField("id"), objectNotFound())));
    }

    @Test
    public void validate_WhenGoalIsNotAvailable() {
        retargetingCondition.getRules().get(0).getGoals().get(0).withId(TARGETING_CATEGORY_IMPORT_ID_NOT_AVAILABLE);

        var result = validator.apply(singletonList(retargetingCondition));
        assertThat(result.flattenErrors(), contains(validationError(getPathToGoalField("id"),
                inconsistentStateTargetingCategoryUnavailable())));
    }

    @Test
    public void validate_WithoutGoalTime() {
        retargetingCondition.getRules().get(0).getGoals().get(0).setTime(null);

        var result = validator.apply(singletonList(retargetingCondition));
        assertThat(result.flattenErrors(), contains(validationError(getPathToGoalField("time"), notNull())));
    }

    @Test
    public void validate_WithGoalWrongTime() {
        retargetingCondition.getRules().get(0).getGoals().get(0).setTime(1);

        var result = validator.apply(singletonList(retargetingCondition));
        assertThat(result.flattenErrors(), contains(validationError(getPathToGoalField("time"), invalidValue())));
    }

    private Path getPathToRuleField(String fieldName) {
        return path(index(0), field("rules"), index(0), field(fieldName));
    }

    private Path getPathToGoalField(String fieldName) {
        return path(index(0), field("rules"), index(0), field("goals"), index(0), field(fieldName));
    }
}
