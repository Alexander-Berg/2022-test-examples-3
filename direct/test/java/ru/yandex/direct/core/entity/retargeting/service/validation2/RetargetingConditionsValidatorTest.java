package ru.yandex.direct.core.entity.retargeting.service.validation2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import ru.yandex.direct.core.entity.retargeting.model.ConditionType;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.GoalType;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.entity.retargeting.model.Rule;
import ru.yandex.direct.core.entity.retargeting.model.RuleType;
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingConditionMappings;
import ru.yandex.direct.validation.defect.CollectionDefects;
import ru.yandex.direct.validation.defect.CommonDefects;
import ru.yandex.direct.validation.defect.NumberDefects;
import ru.yandex.direct.validation.defect.StringDefects;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.either;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static ru.yandex.direct.core.entity.retargeting.Constants.CRYPTA_PARENT_IDS_FOR_VALIDATION;
import static ru.yandex.direct.core.entity.retargeting.Constants.MAX_DESCRIPTION_LENGTH;
import static ru.yandex.direct.core.entity.retargeting.Constants.MAX_GOALS_PER_INTEREST_RULE;
import static ru.yandex.direct.core.entity.retargeting.Constants.MAX_GOALS_PER_RULE;
import static ru.yandex.direct.core.entity.retargeting.Constants.MAX_GOAL_TIME;
import static ru.yandex.direct.core.entity.retargeting.Constants.MAX_INTEREST_RULES_PER_CONDITION;
import static ru.yandex.direct.core.entity.retargeting.Constants.MAX_NAME_LENGTH;
import static ru.yandex.direct.core.entity.retargeting.Constants.MAX_RULES_PER_CONDITION;
import static ru.yandex.direct.core.entity.retargeting.Constants.MIN_GOALS_PER_RULE;
import static ru.yandex.direct.core.entity.retargeting.Constants.MIN_GOAL_TIME;
import static ru.yandex.direct.core.entity.retargeting.Constants.MIN_RULES_PER_CONDITION;
import static ru.yandex.direct.core.entity.retargeting.model.ConditionType.ab_segments;
import static ru.yandex.direct.core.entity.retargeting.model.ConditionType.interests;
import static ru.yandex.direct.core.entity.retargeting.model.CryptaInterestType.all;
import static ru.yandex.direct.core.entity.retargeting.model.CryptaInterestType.short_term;
import static ru.yandex.direct.core.entity.retargeting.model.Goal.AB_SEGMENT_UPPER_BOUND;
import static ru.yandex.direct.core.entity.retargeting.model.Goal.CDP_SEGMENT_UPPER_BOUND;
import static ru.yandex.direct.core.entity.retargeting.model.Goal.CRYPTA_BEHAVIORS_UPPER_BOUND;
import static ru.yandex.direct.core.entity.retargeting.model.Goal.CRYPTA_FAMILY_UPPER_BOUND;
import static ru.yandex.direct.core.entity.retargeting.model.Goal.CRYPTA_INTERESTS_UPPER_BOUND;
import static ru.yandex.direct.core.entity.retargeting.model.Goal.CRYPTA_SOCIAL_DEMO_UPPER_BOUND;
import static ru.yandex.direct.core.entity.retargeting.model.Goal.LAL_SEGMENT_UPPER_BOUND;
import static ru.yandex.direct.core.entity.retargeting.model.Goal.METRIKA_AUDIENCE_UPPER_BOUND;
import static ru.yandex.direct.core.entity.retargeting.model.Goal.METRIKA_ECOMMERCE_UPPER_BOUND;
import static ru.yandex.direct.core.entity.retargeting.model.Goal.METRIKA_SEGMENT_UPPER_BOUND;
import static ru.yandex.direct.core.entity.retargeting.model.Goal.MOBILE_GOAL_UPPER_BOUND;
import static ru.yandex.direct.core.entity.retargeting.model.RuleType.ALL;
import static ru.yandex.direct.core.entity.retargeting.model.RuleType.NOT;
import static ru.yandex.direct.core.entity.retargeting.model.RuleType.OR;
import static ru.yandex.direct.core.entity.retargeting.service.validation2.RetargetingConditionsValidator.ORPHAN_SEGMENT_PARENT_ID;
import static ru.yandex.direct.core.entity.retargeting.service.validation2.RetargetingDefects.allCryptaGoalsMustHaveSameType;
import static ru.yandex.direct.core.entity.retargeting.service.validation2.RetargetingDefects.allElementsAreNegative;
import static ru.yandex.direct.core.entity.retargeting.service.validation2.RetargetingDefects.allGoalsMustBeEitherFromMetrikaOrCrypta;
import static ru.yandex.direct.core.entity.retargeting.service.validation2.RetargetingDefects.cryptaGoalsAllowedOnlyForInterestsType;
import static ru.yandex.direct.core.entity.retargeting.service.validation2.RetargetingDefects.cryptaGoalsAllowedOnlyForOrCondition;
import static ru.yandex.direct.core.entity.retargeting.service.validation2.RetargetingDefects.duplicatedObjectWithName;
import static ru.yandex.direct.core.entity.retargeting.service.validation2.RetargetingDefects.duplicatedObjectWithRules;
import static ru.yandex.direct.core.entity.retargeting.service.validation2.RetargetingDefects.interestLimitExceeded;
import static ru.yandex.direct.core.entity.retargeting.service.validation2.RetargetingDefects.interestsTypeIsNotSpecified;
import static ru.yandex.direct.core.entity.retargeting.service.validation2.RetargetingDefects.mustHaveSameParentId;
import static ru.yandex.direct.core.entity.retargeting.service.validation2.RetargetingDefects.mustNotContainAllElements;
import static ru.yandex.direct.core.entity.retargeting.service.validation2.RetargetingDefects.mutuallyExclusiveParameters;
import static ru.yandex.direct.core.entity.retargeting.service.validation2.RetargetingDefects.requiredTimeForGoalOrSegment;
import static ru.yandex.direct.core.entity.retargeting.service.validation2.RetargetingDefects.unsupportedGoalId;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultGoalByTypeAndId;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class RetargetingConditionsValidatorTest {

    private static final String NAME_1 = "xxx";
    private static final String NAME_2 = "aaa";
    private static final String NAME_EXISTING = "zzz";
    private static final long CLIENT_ID = 10L;
    private static final long GOAL_ID_GOAL_1 = 1L;
    private static final long GOAL_ID_GOAL_2 = 2L;
    private static final long GOAL_ID_GOAL_WITH_LAL = 3L;
    private static final long GOAL_ID_AUDIENCE = METRIKA_AUDIENCE_UPPER_BOUND - 1;

    private static final long LAL_ID = LAL_SEGMENT_UPPER_BOUND - 10;

    private static final long GOAL_ID_SOCIAL_DEMO_1 = CRYPTA_SOCIAL_DEMO_UPPER_BOUND - 1;
    private static final long GOAL_ID_SOCIAL_DEMO_2 = CRYPTA_SOCIAL_DEMO_UPPER_BOUND - 2;
    private static final long GOAL_ID_SOCIAL_DEMO_3 = CRYPTA_SOCIAL_DEMO_UPPER_BOUND - 3;
    private static final long GOAL_ID_SOCIAL_DEMO_4 = CRYPTA_SOCIAL_DEMO_UPPER_BOUND - 4;
    private static final long GOAL_ID_INTERESTS_1 = CRYPTA_INTERESTS_UPPER_BOUND - 1;
    private static final long GOAL_ID_INTERESTS_2 = CRYPTA_INTERESTS_UPPER_BOUND - 2;
    private static final long GOAL_ID_BEHAVIOR = CRYPTA_BEHAVIORS_UPPER_BOUND - 10;
    private static final long GOAL_ID_FAMILY_ORPHAN = CRYPTA_FAMILY_UPPER_BOUND - 1;
    private static final long GOAL_ID_FAMILY_PARENTED = CRYPTA_FAMILY_UPPER_BOUND - 2;
    private static final long GOAL_ID_TV_PARENT = 2_499_000_200L;
    private static final long GOAL_ID_TOO_MUCH_TV = 2_499_000_201L;
    private static final long GOAL_ID_NOT_MUCH_TV = 2_499_000_202L;

    private static final long ECOMMERCE_GOAL_ID = METRIKA_ECOMMERCE_UPPER_BOUND - 1;
    private static final long MOBILE_GOAL_ID = MOBILE_GOAL_UPPER_BOUND - 1;
    private static final long AB_SEGMENT_GOAL_ID = AB_SEGMENT_UPPER_BOUND - 1;
    private static final long GOAL_ID_UNKNOWN = 222L;
    private static final long PARENT_WITH_VALIDATION_1 = (long) CRYPTA_PARENT_IDS_FOR_VALIDATION.toArray()[0];
    private static final long PARENT_WITH_VALIDATION_2 = (long) CRYPTA_PARENT_IDS_FOR_VALIDATION.toArray()[1];
    private static final long PARENT_WITHOUT_VALIDATION = 1L;
    private static final long SEGMENT_GOAL_ID = METRIKA_SEGMENT_UPPER_BOUND - 1;
    private static final long CDP_SEGMENT_GOAL_ID = CDP_SEGMENT_UPPER_BOUND - 1;

    private static final boolean SKIP_GOAL_EXISTENCE_CHECK = false;

    private static Set<String> existingRules;
    private RetargetingConditionsValidator validatorUnderTest;
    private RetargetingCondition valid;

    private static Goal validGoal2() {
        Goal goal = new Goal();
        goal.withId(GOAL_ID_GOAL_2)
                .withTime(1);
        return goal;
    }

    private static Rule validRule2() {
        Rule rule = new Rule();
        rule.withType(ALL)
                .withGoals(ImmutableList.of(validGoal2()));
        return rule;
    }

    private static RetargetingCondition validRetCond2() {
        RetargetingCondition retargetingCondition = new RetargetingCondition();
        retargetingCondition.withClientId(CLIENT_ID)
                .withType(ConditionType.metrika_goals)
                .withName(NAME_2)
                .withRules(ImmutableList.of(validRule2()));
        return retargetingCondition;
    }

    @BeforeClass
    public static void beforeClass() {
        String rule1s = "[{\"goals\":[{\"goal_id\":" + GOAL_ID_GOAL_2
                + ",\"time\":1},{\"goal_id\":" + GOAL_ID_GOAL_1
                + ",\"time\":1}],\"type\":\"all\"}]";
        existingRules = ImmutableSet.of(rule1s);
    }

    @Before
    public void before() {
        valid = validRetargetingCondition();
        validatorUnderTest = initRetargetingConditionsValidator(existingRules, false);
    }

    private RetargetingConditionsValidator initRetargetingConditionsValidator(Set<String> existingRules,
                                                                              boolean forInternalAd) {
        Map<Long, Goal> cryptaGoals = new HashMap<>();
        cryptaGoals.put(GOAL_ID_SOCIAL_DEMO_1, (Goal) new Goal().withId(GOAL_ID_SOCIAL_DEMO_1)
                .withParentId(PARENT_WITH_VALIDATION_1));
        cryptaGoals.put(GOAL_ID_SOCIAL_DEMO_2, (Goal) new Goal().withId(GOAL_ID_SOCIAL_DEMO_2)
                .withParentId(PARENT_WITH_VALIDATION_1));
        cryptaGoals.put(GOAL_ID_SOCIAL_DEMO_3, (Goal) new Goal().withId(GOAL_ID_SOCIAL_DEMO_3)
                .withParentId(PARENT_WITH_VALIDATION_1));
        cryptaGoals.put(GOAL_ID_SOCIAL_DEMO_4, (Goal) new Goal().withId(GOAL_ID_SOCIAL_DEMO_4)
                .withParentId(PARENT_WITH_VALIDATION_2));

        cryptaGoals.put(GOAL_ID_INTERESTS_1, (Goal) new Goal().withId(GOAL_ID_INTERESTS_1)
                .withParentId(PARENT_WITHOUT_VALIDATION));
        cryptaGoals.put(GOAL_ID_INTERESTS_2, (Goal) new Goal().withId(GOAL_ID_INTERESTS_2)
                .withParentId(PARENT_WITHOUT_VALIDATION));

        cryptaGoals.put(GOAL_ID_FAMILY_ORPHAN, (Goal) new Goal().withId(GOAL_ID_FAMILY_ORPHAN)
                .withParentId(ORPHAN_SEGMENT_PARENT_ID));
        cryptaGoals.put(GOAL_ID_FAMILY_PARENTED, (Goal) new Goal().withId(GOAL_ID_FAMILY_PARENTED)
                .withParentId(GOAL_ID_FAMILY_ORPHAN));

        cryptaGoals.put(GOAL_ID_NOT_MUCH_TV, (Goal) new Goal().withId(GOAL_ID_NOT_MUCH_TV)
                .withParentId(GOAL_ID_TV_PARENT));
        cryptaGoals.put(GOAL_ID_TOO_MUCH_TV, (Goal) new Goal().withId(GOAL_ID_TOO_MUCH_TV)
                .withParentId(GOAL_ID_TV_PARENT));
        cryptaGoals.put(GOAL_ID_BEHAVIOR, defaultGoalByTypeAndId(GOAL_ID_BEHAVIOR, GoalType.BEHAVIORS));

        Set<Long> clientGoalIds = Set.of(
                GOAL_ID_GOAL_1, GOAL_ID_GOAL_2, GOAL_ID_GOAL_WITH_LAL,
                GOAL_ID_AUDIENCE, GOAL_ID_INTERESTS_1, GOAL_ID_INTERESTS_2,
                GOAL_ID_SOCIAL_DEMO_1, GOAL_ID_SOCIAL_DEMO_2, GOAL_ID_SOCIAL_DEMO_3,
                GOAL_ID_SOCIAL_DEMO_4,
                GOAL_ID_NOT_MUCH_TV, GOAL_ID_TOO_MUCH_TV, SEGMENT_GOAL_ID, CDP_SEGMENT_GOAL_ID, ECOMMERCE_GOAL_ID,
                MOBILE_GOAL_ID, AB_SEGMENT_GOAL_ID
        );

        Goal lalGoal = new Goal();
        lalGoal
                .withId(LAL_ID)
                .withParentId(GOAL_ID_GOAL_WITH_LAL);

        return RetargetingConditionsValidator.retConditionsIsValid(
                clientGoalIds,
                List.of(lalGoal),
                cryptaGoals,
                Set.of(NAME_EXISTING),
                existingRules,
                emptySet(),
                emptyList(),
                singletonMap(GOAL_ID_NOT_MUCH_TV, singleton(GOAL_ID_TOO_MUCH_TV)),
                SKIP_GOAL_EXISTENCE_CHECK, forInternalAd, false, false);
    }


    @Test
    public void valid() {
        assertFalse(validatorUnderTest.apply(singletonList(valid)).hasAnyErrors());
    }

    @Test
    public void validate_RuleAlreadyExist_HasError() {
        Goal goal = new Goal();
        goal.withId(GOAL_ID_GOAL_1)
                .withTime(1);

        Rule existingRule = new Rule();
        existingRule.withType(ALL)
                .withGoals(Arrays.asList(validGoal2(), goal));

        RetargetingCondition invalid = new RetargetingCondition();
        invalid.withClientId(CLIENT_ID)
                .withType(ConditionType.metrika_goals)
                .withName(NAME_1 + 1)
                .withRules(singletonList(existingRule));

        ValidationResult<List<RetargetingCondition>, Defect> result = validatorUnderTest.apply(singletonList(invalid));
        assertThat(result.flattenErrors(),
                contains(validationError(path(index(0), field("rules")), CommonDefects.inconsistentStateAlreadyExists())));
    }

    @Test
    public void validate_AutoRetargetingRuleAlreadyExist_HasNoError() {
        Goal goal = new Goal();
        goal.withId(GOAL_ID_GOAL_1)
                .withTime(1);

        Rule existingRule = new Rule();
        existingRule.withType(ALL)
                .withGoals(Arrays.asList(validGoal2(), goal));

        RetargetingCondition invalid = new RetargetingCondition();
        invalid.withClientId(CLIENT_ID)
                .withType(ConditionType.metrika_goals)
                .withName(NAME_1 + 1)
                .withRules(singletonList(existingRule))
                .withAutoRetargeting(true);

        ValidationResult<List<RetargetingCondition>, Defect> result = validatorUnderTest.apply(singletonList(invalid));
        assertThat(result, hasNoDefectsDefinitions());
    }

    @Test
    public void validate_NegativeSegment_NoError() {
        Goal goal = new Goal();
        goal.withId(SEGMENT_GOAL_ID);
        Rule rule = new Rule();
        rule.withType(NOT)
                .withGoals(singletonList(goal));

        RetargetingCondition valid2 = new RetargetingCondition();
        valid2.withClientId(CLIENT_ID)
                .withType(ConditionType.metrika_goals)
                .withName(NAME_1 + 2)
                .withRules(singletonList(rule));

        assertFalse(validatorUnderTest.apply(singletonList(valid2)).hasAnyErrors());
    }

    @Test
    public void validate_NegativeABSegmentNotAllowed_HasError() {
        Goal goal = new Goal();
        goal.withId(AB_SEGMENT_GOAL_ID).withTime(1);
        Rule rule = new Rule();
        rule.withType(NOT)
                .withGoals(singletonList(goal));

        RetargetingCondition invalid = new RetargetingCondition();
        invalid.withClientId(CLIENT_ID)
                .withType(ConditionType.metrika_goals)
                .withName(NAME_1 + 3)
                .withRules(singletonList(rule));

        ValidationResult<List<RetargetingCondition>, Defect> result = validatorUnderTest.apply(singletonList(invalid));
        assertThat(result.flattenErrors(),
                contains(validationError(path(index(0)), RetargetingDefects.invalidGoalsForType())));
    }

    @Test
    public void validate_NegativeEcommerce_NoError() {
        Goal goal = new Goal();
        goal.withId(ECOMMERCE_GOAL_ID).withTime(1);
        Rule rule = new Rule();
        rule.withType(NOT)
                .withGoals(singletonList(goal));

        RetargetingCondition valid = new RetargetingCondition();
        valid.withClientId(CLIENT_ID)
                .withType(ConditionType.metrika_goals)
                .withName(NAME_1 + 4)
                .withRules(singletonList(rule));

        assertFalse(validatorUnderTest.apply(singletonList(valid)).hasAnyErrors());
    }

    @Test
    public void validate_NegativeMobileGoal_NoError() {
        Goal goal = new Goal();
        goal.withId(MOBILE_GOAL_ID).withTime(1);
        Rule rule = new Rule();
        rule.withType(NOT)
                .withGoals(singletonList(goal));

        RetargetingCondition valid = new RetargetingCondition();
        valid.withClientId(CLIENT_ID)
                .withType(ConditionType.metrika_goals)
                .withName(NAME_1 + 5)
                .withRules(singletonList(rule));

        assertFalse(validatorUnderTest.apply(singletonList(valid)).hasAnyErrors());
    }

    @Test
    public void invalidValueWhenRetCondIsNull() {
        ValidationResult<List<RetargetingCondition>, Defect> result =
                validatorUnderTest.apply(singletonList(null));
        assertThat(result.flattenErrors(),
                contains(validationError(path(index(0)), CommonDefects.notNull())));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void duplicatedNameDefectDefinition() {
        final Defect duplicatedNameDefect = duplicatedObjectWithName();
        valid.setName(NAME_2);
        ValidationResult<List<RetargetingCondition>, Defect> result = validatorUnderTest.apply(
                asList(valid, validRetCond2()));
        assertThat(result.flattenErrors(),
                contains(
                        validationError(path(index(0)), duplicatedNameDefect),
                        validationError(path(index(1)), duplicatedNameDefect)
                ));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void donCheckDuplicatedRetargetingConditionNamesForSomeTypes_SuccessResult() {
        RetargetingCondition rc1 = (RetargetingCondition) valid.withType(ConditionType.ab_segments)
                .withName(RetargetingCondition.DEFAULT_NAME_FOR_TYPES_WITHOUT_NAME);

        RetargetingCondition rc2 = (RetargetingCondition) validRetCond2().withType(ConditionType.ab_segments)
                .withName(RetargetingCondition.DEFAULT_NAME_FOR_TYPES_WITHOUT_NAME);

        ValidationResult<List<RetargetingCondition>, Defect> result = validatorUnderTest.apply(asList(rc1, rc2));
        assertThat(result, hasNoDefectsDefinitions());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void duplicatedRulesDefectDefinition() {
        final Defect duplicatedRulesDefect = duplicatedObjectWithRules();
        valid.setRules(validRetCond2().getRules());
        ValidationResult<List<RetargetingCondition>, Defect> result = validatorUnderTest.apply(
                asList(valid, validRetCond2()));
        assertThat(result.flattenErrors(),
                contains(
                        validationError(path(index(0)), duplicatedRulesDefect),
                        validationError(path(index(1)), duplicatedRulesDefect)
                ));
    }

    @Test
    public void invalidValueWhenClientIdIsNull() {
        valid.setClientId(null);
        ValidationResult<List<RetargetingCondition>, Defect> result =
                validatorUnderTest.apply(singletonList(valid));
        assertThat(result.flattenErrors(),
                contains(validationError(path(index(0), field("clientId")), CommonDefects.notNull())));
    }

    @Test
    public void invalidValueWhenClientIdIncorrect() {
        valid.setClientId(0L);
        ValidationResult<List<RetargetingCondition>, Defect> result =
                validatorUnderTest.apply(singletonList(valid));
        assertThat(result.flattenErrors(),
                contains(validationError(path(index(0), field("clientId")), CommonDefects.validId())));
    }

    @Test
    public void invalidValueWhenNameIsNull() {
        valid.setName(null);
        ValidationResult<List<RetargetingCondition>, Defect> result =
                validatorUnderTest.apply(singletonList(valid));
        assertThat(result.flattenErrors(),
                contains(validationError(path(index(0), field("name")), CommonDefects.notNull())));
    }

    @Test
    public void emptyValueWhenNameIsEmpty() {
        valid.setName("");
        ValidationResult<List<RetargetingCondition>, Defect> result =
                validatorUnderTest.apply(singletonList(valid));
        assertThat(result.flattenErrors(),
                contains(validationError(path(index(0), field("name")), StringDefects.notEmptyString())));
    }

    @Test
    public void maxStringSizeWhenNameIsTooLong() {
        valid.setName(String.join("", Collections.nCopies(MAX_NAME_LENGTH + 1, "x")));
        ValidationResult<List<RetargetingCondition>, Defect> result = validatorUnderTest.apply(
                singletonList(valid));
        assertThat(result.flattenErrors(),
                contains(validationError(path(index(0), field("name")),
                        CollectionDefects.maxStringLength(MAX_NAME_LENGTH))));
    }

    @Test
    public void invalidCharsInName() {
        valid.setName("¬");
        ValidationResult<List<RetargetingCondition>, Defect> result = validatorUnderTest.apply(
                singletonList(valid));
        assertThat(result.flattenErrors(),
                contains(validationError(path(index(0), field("name")), StringDefects.admissibleChars())));
    }

    @Test
    public void nameAlreadyExists() {
        valid.setName(NAME_EXISTING);
        ValidationResult<List<RetargetingCondition>, Defect> result = validatorUnderTest.apply(
                singletonList(valid));
        assertThat(result.flattenErrors(),
                contains(validationError(path(index(0), field("name")),
                        CommonDefects.inconsistentStateAlreadyExists())));
    }

    @Test
    public void nameCanExistWhenTypeIsInterests() {
        valid.setName(NAME_EXISTING);
        valid.setType(interests);
        assertFalse(validatorUnderTest.apply(singletonList(valid)).hasAnyErrors());
    }

    @Test
    public void nameCanExistWhenTypeIsAbSegments() {
        valid.setName(NAME_EXISTING);
        valid.setType(ab_segments);
        assertFalse(validatorUnderTest.apply(singletonList(valid)).hasAnyErrors());
    }

    @Test
    public void maxStringSizeWhenDescriptionIsTooLong() {
        valid.setDescription(String.join("", Collections.nCopies(MAX_DESCRIPTION_LENGTH + 1, "x")));
        ValidationResult<List<RetargetingCondition>, Defect> result = validatorUnderTest.apply(
                singletonList(
                        valid));
        assertThat(result.flattenErrors(),
                contains(validationError(path(index(0), field("description")),
                        CollectionDefects.maxStringLength(MAX_DESCRIPTION_LENGTH))));
    }

    @Test
    public void invalidCharsInDescription() {
        valid.setDescription("¬");
        ValidationResult<List<RetargetingCondition>, Defect> result = validatorUnderTest.apply(
                singletonList(valid));
        assertThat(result.flattenErrors(),
                contains(validationError(path(index(0), field("description")), StringDefects.admissibleChars())));
    }

    @Test
    public void invalidValueWhenRulesIsNull() {
        valid.setRules(null);
        ValidationResult<List<RetargetingCondition>, Defect> result = validatorUnderTest.apply(
                singletonList(valid));
        assertThat(result.flattenErrors(),
                contains(validationError(path(index(0), field("rules")), CommonDefects.notNull())));
    }

    @Test
    public void invalidCollectionSizeWhenNoRules() {
        valid.setRules(emptyList());
        ValidationResult<List<RetargetingCondition>, Defect> result = validatorUnderTest.apply(
                singletonList(valid));
        assertThat(result.flattenErrors(),
                contains(validationError(path(index(0), field("rules")),
                        CollectionDefects.collectionSizeIsValid(MIN_RULES_PER_CONDITION, MAX_RULES_PER_CONDITION))));
    }

    @Test
    public void validInterestsConditionWithNoRules() {
        var condition = retargetingCondition(interests);
        var result = validatorUnderTest.apply(singletonList(condition));
        assertThat(result.flattenErrors(), empty());
    }

    @Test
    public void invalidCollectionSizeWhenTooManyRules() {
        valid.setRules(
                Collections.nCopies(MAX_RULES_PER_CONDITION + 1, validRule2()));
        ValidationResult<List<RetargetingCondition>, Defect> result = validatorUnderTest.apply(
                singletonList(valid));
        assertThat(result.flattenErrors(),
                contains(validationError(path(index(0), field("rules")),
                        CollectionDefects.collectionSizeIsValid(MIN_RULES_PER_CONDITION, MAX_RULES_PER_CONDITION))));
    }

    @Test
    public void invalidCollectionSizeWhenTooManyInterestRules() {
        Rule interestRule = new Rule().withType(ALL)
                .withGoals(ImmutableList.of((Goal) new Goal().withId(GOAL_ID_INTERESTS_1)));

        valid.setType(ConditionType.interests);
        valid.setRules(
                Collections.nCopies(MAX_INTEREST_RULES_PER_CONDITION + 1, interestRule));
        ValidationResult<List<RetargetingCondition>, Defect> result = validatorUnderTest.apply(
                singletonList(valid));

        assertThat(result,
                hasDefectDefinitionWith(validationError(
                        path(index(0), field("rules")),
                        interestLimitExceeded())));
    }

    @Test
    public void ruleAlreadyExists() {
        validatorUnderTest = initRetargetingConditionsValidator(
                ImmutableSet.of(RetargetingConditionMappings.rulesToJson(valid.getRules())), false);

        ValidationResult<List<RetargetingCondition>, Defect> result = validatorUnderTest.apply(
                singletonList(valid));
        assertThat(result.flattenErrors(),
                contains(
                        validationError(path(index(0), field("rules")),
                                CommonDefects.inconsistentStateAlreadyExists())));
    }

    @Test
    public void ruleCanExistWhenTypeIsInterests() {
        validatorUnderTest = initRetargetingConditionsValidator(
                ImmutableSet.of(RetargetingConditionMappings.rulesToJson(valid.getRules())), false);

        valid.setType(interests);
        assertFalse(validatorUnderTest.apply(singletonList(valid)).hasAnyErrors());
    }

    @Test
    public void invalidValueWhenRuleIsNull() {
        valid.setRules(singletonList(null));
        ValidationResult<List<RetargetingCondition>, Defect> result = validatorUnderTest.apply(
                singletonList(valid));
        assertThat(result.flattenErrors(),
                contains(validationError(path(index(0), field("rules"), index(0)), CommonDefects.notNull())));
    }

    @Test
    public void invalidValueWhenRuleTypeIsNull() {
        RetargetingCondition invalid = this.valid;
        invalid.getRules().get(0).setType(null);
        ValidationResult<List<RetargetingCondition>, Defect> result =
                validatorUnderTest.apply(singletonList(invalid));
        assertThat(result.flattenErrors(),
                contains(validationError(path(index(0), field("rules"), index(0), field("type")),
                        CommonDefects.notNull())));
    }

    @Test
    public void invalidValueWhenGoalsIsNull() {
        RetargetingCondition invalid = this.valid;
        invalid.getRules().get(0).setGoals(null);
        ValidationResult<List<RetargetingCondition>, Defect> result = validatorUnderTest.apply(
                singletonList(invalid));
        assertThat(result.flattenErrors(),
                contains(validationError(path(index(0), field("rules"), index(0), field("goals")),
                        CommonDefects.notNull())));
    }

    @Test
    public void invalidCollectionSizeWhenNoAnyGoals() {
        RetargetingCondition invalid = this.valid;
        invalid.getRules().get(0).setGoals(emptyList());
        ValidationResult<List<RetargetingCondition>, Defect> result =
                validatorUnderTest.apply(singletonList(invalid));
        assertThat(result.flattenErrors(),
                contains(validationError(path(index(0), field("rules"), index(0), field("goals")),
                        CollectionDefects.collectionSizeIsValid(MIN_GOALS_PER_RULE, MAX_GOALS_PER_RULE))));
    }

    @Test
    public void invalidCollectionSizeWhenTooManyGoals() {
        RetargetingCondition invalid = this.valid;
        invalid.getRules().get(0).setGoals(Collections.nCopies(MAX_GOALS_PER_RULE + 1, validGoal2()));
        ValidationResult<List<RetargetingCondition>, Defect> result =
                validatorUnderTest.apply(singletonList(invalid));
        assertThat(result.flattenErrors(),
                contains(validationError(path(index(0), field("rules"), index(0), field("goals")),
                        CollectionDefects.collectionSizeIsValid(MIN_GOALS_PER_RULE, MAX_GOALS_PER_RULE))));
    }

    @Test
    public void invalidCollectionSizeWhenTooManyInterestGoals() {
        RetargetingCondition invalid = this.valid;
        invalid.setType(ConditionType.interests);
        Rule rule = invalid.getRules().get(0);
        rule.setType(OR);
        rule.setGoals(Collections.nCopies(MAX_GOALS_PER_INTEREST_RULE + 1,
                (Goal) new Goal().withId(GOAL_ID_INTERESTS_1)));
        ValidationResult<List<RetargetingCondition>, Defect> result =
                validatorUnderTest.apply(singletonList(invalid));
        assertThat(result.flattenErrors(),
                contains(validationError(path(index(0), field("rules"), index(0), field("goals")),
                        CollectionDefects.collectionSizeIsValid(0, MAX_GOALS_PER_INTEREST_RULE))));
    }

    @Test
    public void invalidValueWhenGoalIsNull() {
        RetargetingCondition invalid = this.valid;
        invalid.getRules().get(0).setGoals(singletonList(null));
        ValidationResult<List<RetargetingCondition>, Defect> result = validatorUnderTest.apply(
                singletonList(invalid));
        assertThat(result.flattenErrors(),
                contains(validationError(path(index(0), field("rules"), index(0), field("goals"), index(0)),
                        CommonDefects.notNull())));
    }

    @Test
    public void requiredButEmptyWhenGoalTimeIsNullAndGoalTypeIsGoal() {
        RetargetingCondition invalid = this.valid;
        Goal goal = new Goal();
        goal.withId(GOAL_ID_GOAL_1);
        invalid.getRules().get(0).setGoals(singletonList(goal));
        ValidationResult<List<RetargetingCondition>, Defect> result = validatorUnderTest.apply(
                singletonList(invalid));
        assertThat(result.flattenErrors(),
                contains(validationError(path(index(0), field("rules"), index(0), field("goals"), index(0)),
                        requiredTimeForGoalOrSegment())));
    }

    @Test
    public void invalidValueWhenGoalIdIsNull() {
        RetargetingCondition invalid = this.valid;
        invalid.getRules().get(0).getGoals().get(0).setId(null);
        ValidationResult<List<RetargetingCondition>, Defect> result = validatorUnderTest.apply(
                singletonList(invalid));
        assertThat(result.flattenErrors(),
                contains(
                        validationError(path(index(0), field("rules"), index(0), field("goals"), index(0), field("id")),
                                CommonDefects.notNull())));
    }

    @Test
    public void invalidValueWhenGoalIdZero() {
        RetargetingCondition invalid = this.valid;
        invalid.getRules().get(0).getGoals().get(0).setId(0L);
        ValidationResult<List<RetargetingCondition>, Defect> result = validatorUnderTest.apply(
                singletonList(invalid));
        assertThat(result.flattenErrors(),
                contains(
                        validationError(path(index(0), field("rules"), index(0), field("goals"), index(0), field("id")),
                                CommonDefects.validId())));
    }

    @Test
    public void notFoundWhenGoalIdUnknown() {
        RetargetingCondition invalid = this.valid;
        invalid.getRules().get(0).getGoals().get(0).setId(GOAL_ID_UNKNOWN);
        ValidationResult<List<RetargetingCondition>, Defect> result = validatorUnderTest.apply(
                singletonList(invalid));
        assertThat(result.flattenErrors(),
                contains(
                        validationError(path(index(0), field("rules"), index(0), field("goals"), index(0), field("id")),
                                CommonDefects.objectNotFound())));
    }

    @Test
    public void invalidValueWhenGoalTimeZero() {
        RetargetingCondition invalid = this.valid;
        invalid.getRules().get(0).getGoals().get(0).setTime(0);
        ValidationResult<List<RetargetingCondition>, Defect> result = validatorUnderTest.apply(
                singletonList(invalid));
        assertThat(result.flattenErrors(),
                contains(validationError(
                        path(index(0), field("rules"), index(0), field("goals"), index(0), field("time")),
                        NumberDefects.inInterval(MIN_GOAL_TIME, MAX_GOAL_TIME))));
    }

    @Test
    public void validValueWhenCdpSegmentTimeAny() {
        var rule = valid.getRules().get(0);
        var newGoals = new ArrayList<>(rule.getGoals());

        // cdp segment time should always default to 540
        var cdpSegment = new Goal();
        cdpSegment
                .withId(CDP_SEGMENT_GOAL_ID);
        assertThat(cdpSegment.getTime(), equalTo(540));
        cdpSegment
                .withTime(1);
        assertThat(cdpSegment.getTime(), equalTo(540));

        newGoals.addAll(List.of(cdpSegment));
        rule.setGoals(newGoals);

        assertFalse(validatorUnderTest.apply(List.of(valid)).hasAnyErrors());
    }

    @Test
    public void validValueWhenCryptaGoalTimeZero() {
        valid.withType(interests);
        Rule rule = valid.getRules().get(0);
        rule.setType(OR);
        var newGoal = rule.getGoals().get(0);
        newGoal.withId(GOAL_ID_SOCIAL_DEMO_1).withTime(0);
        rule.setGoals(List.of(newGoal));

        assertFalse(validatorUnderTest.apply(singletonList(this.valid)).hasAnyErrors());
    }

    @Test
    public void invalidValueWhenGoalTimeTooBig() {
        RetargetingCondition invalid = this.valid;
        invalid.getRules().get(0).getGoals().get(0).setTime(MAX_GOAL_TIME + 1);
        ValidationResult<List<RetargetingCondition>, Defect> result = validatorUnderTest.apply(
                singletonList(invalid));
        assertThat(result.flattenErrors(),
                contains(validationError(
                        path(index(0), field("rules"), index(0), field("goals"), index(0), field("time")),
                        NumberDefects.inInterval(MIN_GOAL_TIME, MAX_GOAL_TIME))));
    }

    @Test
    public void invalidValueWhenGoalsFromMetrikaAndCrypta() {
        RetargetingCondition invalid = this.valid;
        invalid.withType(interests);
        Rule rule = invalid.getRules().get(0);
        rule.setType(OR);

        rule.setGoals(asList(validGoal2(), (Goal) new Goal().withId(GOAL_ID_SOCIAL_DEMO_1)));

        ValidationResult<List<RetargetingCondition>, Defect> result =
                validatorUnderTest.apply(singletonList(invalid));
        assertThat(result,
                hasDefectDefinitionWith(validationError(
                        path(index(0), field("rules"), index(0), field("goals")),
                        allGoalsMustBeEitherFromMetrikaOrCrypta())));
    }

    @Test
    public void validValueWhenGoalsFromMetrikaWithLals() {
        var rule = valid.getRules().get(0);
        var newGoals = new ArrayList<>(rule.getGoals());

        var lalGoal = new Goal();
        lalGoal
                .withId(LAL_ID)
                .withParentId(GOAL_ID_GOAL_WITH_LAL);
        newGoals.add(lalGoal);

        rule.setGoals(newGoals);

        assertFalse(validatorUnderTest.apply(List.of(valid)).hasAnyErrors());
    }

    @Test
    public void validWhenUnionWithIdIsEqualToParentId() {
        var rule = valid.getRules().get(0);
        var newGoals = new ArrayList<>(rule.getGoals());

        var lalGoal = new Goal();
        lalGoal
                .withId(LAL_ID)
                .withUnionWithId(GOAL_ID_GOAL_WITH_LAL);
        newGoals.add(lalGoal);

        rule.setGoals(newGoals);

        assertFalse(validatorUnderTest.apply(List.of(valid)).hasAnyErrors());
    }

    @Test
    public void notValidWhenUnionWithIdIsNotEqualToParentId() {
        var rule = valid.getRules().get(0);
        var newGoals = new ArrayList<>(rule.getGoals());

        var lalGoal = new Goal();
        lalGoal
                .withId(LAL_ID)
                .withUnionWithId(GOAL_ID_GOAL_1);
        newGoals.add(lalGoal);

        rule.setGoals(newGoals);

        assertTrue(validatorUnderTest.apply(List.of(valid)).hasAnyErrors());
    }

    @Test
    public void notValidWhenUnionWithIdIsNotPresent() {
        var rule = valid.getRules().get(0);
        List<Goal> newGoals = new ArrayList<>();

        var lalGoal = new Goal();
        lalGoal
                .withId(LAL_ID)
                .withUnionWithId(GOAL_ID_GOAL_WITH_LAL);
        newGoals.add(lalGoal);

        rule.setGoals(newGoals);

        assertTrue(validatorUnderTest.apply(List.of(valid)).hasAnyErrors());
    }

    @Test
    public void validWhenUnionWithIdIsPresent() {
        var rule = valid.getRules().get(0);

        List<Goal> newGoals = List.of(
                (Goal) new Goal()
                        .withId(LAL_ID)
                        .withUnionWithId(GOAL_ID_GOAL_WITH_LAL),
                (Goal) new Goal()
                        .withId(GOAL_ID_GOAL_WITH_LAL)
                        .withTime(1)
        );

        rule.setGoals(newGoals);

        assertFalse(validatorUnderTest.apply(List.of(valid)).hasAnyErrors());
    }

    @Test
    public void notValidWhenUnionWithIdNotInLalSegment() {
        var rule = valid.getRules().get(0);
        var newGoals = new ArrayList<>(rule.getGoals());

        var lalGoal = new Goal();
        lalGoal
                .withId(GOAL_ID_GOAL_2)
                .withUnionWithId(GOAL_ID_GOAL_1)
                .withTime(1);
        newGoals.add(lalGoal);

        rule.setGoals(newGoals);

        assertTrue(validatorUnderTest.apply(List.of(valid)).hasAnyErrors());
    }

    @Test
    public void validWhenSocialDemoGoalsHaveSameTypeAndParents() {
        Rule rule = valid.getRules().get(0);
        valid.withType(interests);
        rule.setType(OR);

        rule.setGoals(asList((Goal) new Goal().withId(GOAL_ID_SOCIAL_DEMO_1),
                (Goal) new Goal().withId(GOAL_ID_SOCIAL_DEMO_2)));

        assertFalse(validatorUnderTest.apply(singletonList(valid)).hasAnyErrors());
    }

    @Test
    public void invalidValueWhenCryptaGoalsHaveDifferentType() {
        RetargetingCondition invalid = this.valid;
        invalid.withType(interests);
        Rule rule = invalid.getRules().get(0);
        rule.setType(OR);

        rule.setGoals(asList((Goal) new Goal().withId(GOAL_ID_INTERESTS_1),
                (Goal) new Goal().withId(GOAL_ID_SOCIAL_DEMO_4)));

        ValidationResult<List<RetargetingCondition>, Defect> result = validatorUnderTest.apply(
                singletonList(invalid));
        assertThat(result,
                hasDefectDefinitionWith(validationError(
                        path(index(0), field("rules"), index(0), field("goals")),
                        allCryptaGoalsMustHaveSameType())));
    }

    @Test
    public void invalidValueWhenSocialDemoGoalsHaveDifferentKeyword() {
        RetargetingCondition invalid = this.valid;
        invalid.withType(interests);
        Rule rule = invalid.getRules().get(0);
        rule.setType(OR);

        rule.setGoals(asList((Goal) new Goal().withId(GOAL_ID_SOCIAL_DEMO_1),
                (Goal) new Goal().withId(GOAL_ID_SOCIAL_DEMO_4)));

        ValidationResult<List<RetargetingCondition>, Defect> result = validatorUnderTest.apply(
                singletonList(invalid));
        assertThat(result,
                hasDefectDefinitionWith(validationError(
                        path(index(0), field("rules"), index(0), field("goals")),
                        mustHaveSameParentId())));
    }

    @Test
    public void validValueWhenCryptaGoalsContainAllValuesWhenParentWithoutValidation() {
        RetargetingCondition invalid = this.valid;
        invalid.withType(interests);
        Rule rule = invalid.getRules().get(0);
        rule.setInterestType(short_term);
        rule.setType(OR);

        rule.setGoals(asList(
                (Goal) new Goal().withId(GOAL_ID_INTERESTS_1),
                (Goal) new Goal().withId(GOAL_ID_INTERESTS_2)));

        assertFalse(validatorUnderTest.apply(singletonList(valid)).hasAnyErrors());
    }

    @Test
    public void invalidValueWhenTypeIsNotInterestsForCryptaGoals() {
        RetargetingCondition invalid = this.valid;
        Rule rule = invalid.getRules().get(0);
        rule.setInterestType(short_term);
        rule.setType(OR);

        rule.setGoals(asList(
                (Goal) new Goal().withId(GOAL_ID_INTERESTS_1),
                (Goal) new Goal().withId(GOAL_ID_INTERESTS_2)));

        ValidationResult<List<RetargetingCondition>, Defect> result = validatorUnderTest.apply(
                singletonList(invalid));
        assertThat(result,
                hasDefectDefinitionWith(validationError(
                        path(index(0), field("rules"), index(0), field("goals")),
                        cryptaGoalsAllowedOnlyForInterestsType())));
    }

    @Test
    public void invalidValueWhenInterestsTypeIsNotSpecifiedForInterests() {
        RetargetingCondition invalid = this.valid;
        invalid.withType(interests);
        Rule rule = invalid.getRules().get(0);
        rule.setType(OR);

        rule.setGoals(asList(
                (Goal) new Goal().withId(GOAL_ID_INTERESTS_1),
                (Goal) new Goal().withId(GOAL_ID_INTERESTS_2)));

        ValidationResult<List<RetargetingCondition>, Defect> result = validatorUnderTest.apply(
                singletonList(invalid));
        assertThat(result,
                hasDefectDefinitionWith(validationError(
                        path(index(0), field("rules"), index(0), field("goals")),
                        interestsTypeIsNotSpecified())));
    }

    @Test
    public void invalidValueWhenCryptaGoalsContainAllValuesWhenParentWithValidation() {
        RetargetingCondition invalid = this.valid;
        invalid.withType(interests);
        Rule rule = invalid.getRules().get(0);
        rule.setType(OR);

        rule.setGoals(asList(
                (Goal) new Goal().withId(GOAL_ID_SOCIAL_DEMO_1),
                (Goal) new Goal().withId(GOAL_ID_SOCIAL_DEMO_2),
                (Goal) new Goal().withId(GOAL_ID_SOCIAL_DEMO_3)));

        ValidationResult<List<RetargetingCondition>, Defect> result = validatorUnderTest.apply(
                singletonList(invalid));
        assertThat(result,
                hasDefectDefinitionWith(validationError(
                        path(index(0), field("rules"), index(0), field("goals")),
                        mustNotContainAllElements())));
    }

    @Test
    public void unsupportedOrphanFamilySegmentId() {
        RetargetingCondition invalid = this.valid;
        Rule rule = invalid.getRules().get(0);
        rule.setType(OR);

        rule.setGoals(Collections.singletonList(
                (Goal) new Goal().withId(GOAL_ID_FAMILY_ORPHAN)));

        ValidationResult<List<RetargetingCondition>, Defect> result = validatorUnderTest.apply(
                singletonList(invalid));
        assertThat(result,
                hasDefectDefinitionWith(validationError(
                        path(index(0), field("rules"), index(0), field("goals"), index(0), field("id")),
                        unsupportedGoalId())));
    }

    @Test
    public void validParentedFamilySegmentId() {
        valid.withType(interests);
        Rule rule = valid.getRules().get(0);
        rule.setType(OR);

        rule.setGoals(Collections.singletonList(
                (Goal) new Goal().withId(GOAL_ID_FAMILY_PARENTED)));

        assertFalse(validatorUnderTest.apply(singletonList(valid)).hasAnyErrors());
    }

    @Test
    public void invalidValueWhenConditionWithInterestTypeContainsOnlyNegativeRules() {
        var condition = retargetingCondition(
                interests,
                rule(NOT, goal(GOAL_ID_GOAL_1, 1), goal(GOAL_ID_AUDIENCE, 1)),
                rule(NOT, goal(GOAL_ID_GOAL_2, 1))
        );

        var result = validatorUnderTest.apply(List.of(condition));

        assertThat(result, hasDefectDefinitionWith(
                validationError(path(index(0), field("rules")), allElementsAreNegative())
        ));
    }

    @Test
    public void invalidValueWhenConditionIsNotOrForCryptaGoals() {
        var condition = retargetingCondition(
                interests,
                rule(
                        ALL,
                        goal(GOAL_ID_INTERESTS_1, 0),
                        goal(GOAL_ID_INTERESTS_2, 0)
                )
        );

        var result = validatorUnderTest.apply(List.of(condition));

        assertThat(result,
                hasDefectDefinitionWith(validationError(
                        path(index(0), field("rules"), index(0), field("goals")),
                        cryptaGoalsAllowedOnlyForOrCondition())));
    }

    @Test
    public void invalidValueWhenConditionHasBehaviorIntoNegativeRules() {
        var condition = retargetingCondition(
                interests,
                rule(ALL, goal(GOAL_ID_GOAL_1, 1)),
                rule(NOT, goal(GOAL_ID_BEHAVIOR, 0))
        );

        var result = validatorUnderTest.apply(List.of(condition));

        assertThat(result, hasDefectDefinitionWith(
                validationError(
                        path(index(0), field("rules"), index(1), field("goals")),
                        cryptaGoalsAllowedOnlyForOrCondition()
                )
        ));
    }

    @Test
    public void validWhenConditionWithInterestTypeContainsOnlyNegativeRulesButForInternalAd() {
        var condition = retargetingCondition(
                interests,
                rule(NOT, goal(GOAL_ID_GOAL_1, 1), goal(GOAL_ID_GOAL_2, 1)),
                rule(NOT, goal(GOAL_ID_INTERESTS_1, 0)).withInterestType(all),
                rule(NOT, goal(GOAL_ID_BEHAVIOR, 0))
        );

        var validator = initRetargetingConditionsValidator(existingRules, true);
        var result = validator.apply(singletonList(condition));

        assertThat(result.flattenErrors(), empty());
    }

    @Test
    public void validWhenConditionWithInterestTypeContainsOnlyNegativeMetrikaRulesForInternalAd() {
        var condition = retargetingCondition(
                interests,
                rule(NOT, goal(GOAL_ID_GOAL_1, 1), goal(GOAL_ID_GOAL_2, 1))
        );

        var validator = initRetargetingConditionsValidator(existingRules, true);
        var result = validator.apply(singletonList(condition));

        assertThat(result.flattenErrors(), empty());
    }

    @Test
    public void invalidInterestsConditionWithNoRulesForInternalAd() {
        var condition = retargetingCondition(interests);
        var validator = initRetargetingConditionsValidator(existingRules, true);
        var result = validator.apply(singletonList(condition));
        assertThat(result.flattenErrors(),
                contains(validationError(path(index(0), field("rules")),
                        CollectionDefects.collectionSizeIsValid(MIN_RULES_PER_CONDITION, MAX_RULES_PER_CONDITION))));
    }

    @Test
    public void validWhenConditionWithDifferentCryptaTypesInRulesButForInternalAd() {
        var condition = retargetingCondition(
                interests,
                rule(OR, goal(GOAL_ID_INTERESTS_1, 0), goal(GOAL_ID_BEHAVIOR, 0)).withInterestType(all)
        );

        var validator = initRetargetingConditionsValidator(existingRules, true);
        var result = validator.apply(singletonList(condition));

        assertThat(result.flattenErrors(), empty());
    }

    @Test
    public void validWhenConditionHaveRuleTypeAllForInternalAd() {
        var condition = retargetingCondition(
                interests,
                rule(ALL, goal(GOAL_ID_INTERESTS_1, 0), goal(GOAL_ID_INTERESTS_2, 0)).withInterestType(all)
        );

        var validator = initRetargetingConditionsValidator(existingRules, true);
        var result = validator.apply(singletonList(condition));

        assertThat(result.flattenErrors(), empty());
    }

    @Test
    public void invalidWhenConditionWithDifferentTypesInRulesForInternalAd() {
        var condition = retargetingCondition(
                interests,
                rule(OR, goal(GOAL_ID_INTERESTS_1, 0), goal(GOAL_ID_GOAL_1, 1)).withInterestType(all)
        );

        var validator = initRetargetingConditionsValidator(existingRules, true);
        var result = validator.apply(singletonList(condition));

        assertThat(result, hasDefectDefinitionWith(
                validationError(
                        path(index(0), field("rules"), index(0), field("goals")),
                        allGoalsMustBeEitherFromMetrikaOrCrypta()
                )
        ));
    }

    @Test
    public void mutuallyExclusiveGoals() {
        Goal notMuchTvGoal = new Goal();
        notMuchTvGoal.withId(GOAL_ID_NOT_MUCH_TV).withTime(1);
        Goal tooMuchTvGoal = new Goal();
        tooMuchTvGoal.withId(GOAL_ID_TOO_MUCH_TV).withTime(1);
        Rule rule = new Rule();
        rule.withType(OR).withGoals(Arrays.asList(notMuchTvGoal, tooMuchTvGoal));
        RetargetingCondition invalid = this.valid;
        invalid.withRules(singletonList(rule)).withType(interests);

        ValidationResult<List<RetargetingCondition>, Defect> vr = validatorUnderTest.apply(singletonList(invalid));
        Path path = path(index(0), field("rules"), index(0), field("goals"));
        assertThat(vr, hasDefectDefinitionWith(
                either(validationError(path,
                        mutuallyExclusiveParameters(
                                path(field("id"), field(Long.toString(GOAL_ID_NOT_MUCH_TV))),
                                path(field("id"), field(Long.toString(GOAL_ID_TOO_MUCH_TV))))))
                        .or(validationError(path,
                                mustNotContainAllElements()))));
    }

    private Goal goal(Long goalId, Integer time) {
        Goal goal = new Goal();
        goal.withId(goalId).withTime(time);
        return goal;
    }

    private Rule rule(RuleType ruleType, Goal... goals) {
        return new Rule().withType(ruleType).withGoals(Arrays.asList(goals));
    }

    private RetargetingCondition validRetargetingCondition() {
        return retargetingCondition(ConditionType.metrika_goals,
                rule(ALL, goal(GOAL_ID_GOAL_1, 1), goal(GOAL_ID_GOAL_WITH_LAL, 1)));
    }

    private RetargetingCondition retargetingCondition(ConditionType type, Rule... rules) {
        var retargetingCondition = new RetargetingCondition();
        retargetingCondition
                .withClientId(CLIENT_ID)
                .withType(type)
                .withName(NAME_1)
                .withRules(Arrays.asList(rules));
        return retargetingCondition;
    }
}
