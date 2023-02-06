package ru.yandex.direct.core.testing.data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import one.util.streamex.StreamEx;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;

import ru.yandex.direct.core.entity.retargeting.model.ConditionType;
import ru.yandex.direct.core.entity.retargeting.model.CryptaInterestType;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.GoalType;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingConditionGoal;
import ru.yandex.direct.core.entity.retargeting.model.Rule;
import ru.yandex.direct.core.entity.retargeting.model.RuleType;
import ru.yandex.direct.dbutil.model.ClientId;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static ru.yandex.direct.core.entity.retargeting.model.ConditionType.dmp;
import static ru.yandex.direct.core.entity.retargeting.model.CryptaInterestType.long_term;
import static ru.yandex.direct.core.entity.retargeting.model.Goal.METRIKA_AUDIENCE_UPPER_BOUND;
import static ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition.DEFAULT_TYPE;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultABSegmentGoal;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultBrandSafetyGoal;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultGoalByType;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultGoalByTypeAndId;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultLalSegmentGoal;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultMetrikaGoals;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultParentAndLalSegmentPair;

public final class TestRetargetingConditions {

    public static final ConditionType OPPOSITE_TO_DEFAULT_TYPE = dmp;
    private static final Long LTV_GOAL_ID = 2499000201L;

    private TestRetargetingConditions() {
    }

    public static RetargetingCondition defaultRetCondition(@Nullable ClientId clientId) {
        RetargetingCondition retargetingCondition = new RetargetingCondition();
        retargetingCondition.withClientId(clientId != null ? clientId.asLong() : null)
                .withName("default test retargeting condition " + RandomStringUtils.randomNumeric(5))
                .withDescription("default test retargeting condition description " + RandomStringUtils.randomNumeric(5))
                .withDeleted(false)
                .withInterest(false)
                .withType(DEFAULT_TYPE)
                .withLastChangeTime(LocalDateTime.now())
                .withRules(defaultRules())
                .withAutoRetargeting(false);
        return retargetingCondition;
    }

    public static RetargetingCondition defaultCpmRetCondition() {
        return (RetargetingCondition) new RetargetingCondition()
                .withName("retargeting condition " + RandomStringUtils.randomNumeric(5))
                .withDescription("retargeting condition description " + RandomStringUtils.randomNumeric(5))
                .withType(ConditionType.interests)
                .withRules(defaultCpmRetConditionRules())
                .withDeleted(false)
                .withInterest(false)
                .withLastChangeTime(LocalDateTime.now());
    }

    public static RetargetingCondition defaultIndoorRetCondition() {
        return (RetargetingCondition) new RetargetingCondition()
                .withName("retargeting condition " + RandomStringUtils.randomNumeric(5))
                .withDescription("retargeting condition description " + RandomStringUtils.randomNumeric(5))
                .withType(ConditionType.interests)
                .withRules(getCpmIndoorRetargetingRules())
                .withDeleted(false)
                .withInterest(false)
                .withLastChangeTime(LocalDateTime.now());
    }

    private static List<Rule> defaultCpmRetConditionRules() {
        return asList(
                defaultRule(singletonList(defaultGoalByType(GoalType.SOCIAL_DEMO)), RuleType.OR),
                defaultRule(singletonList(defaultGoalByType(GoalType.AUDIENCE)), RuleType.NOT),
                defaultRule(singletonList(defaultGoalByType(GoalType.FAMILY)), RuleType.OR),
                defaultRule(singletonList(defaultGoalByType(GoalType.INTERESTS)), RuleType.OR, long_term)
        );
    }

    public static List<Rule> getCpmIndoorRetargetingRules() {
        List<Rule> rules = new ArrayList<>();
        //crypta social demo
        rules.add(new Rule()
                .withType(RuleType.OR)
                .withGoals(singletonList((Goal) new Goal()
                        .withType(GoalType.SOCIAL_DEMO)
                        .withKeyword("111")
                        .withKeywordValue("111")
                        .withParentId(1L)
                        .withId(RandomUtils.nextLong(METRIKA_AUDIENCE_UPPER_BOUND + 1, METRIKA_AUDIENCE_UPPER_BOUND + 3)))));

        rules.add(new Rule()
                .withType(RuleType.OR)
                .withGoals(singletonList((Goal) new Goal()
                        .withType(GoalType.SOCIAL_DEMO)
                        .withKeyword("222")
                        .withKeywordValue("222")
                        .withParentId(1L)
                        .withId(RandomUtils.nextLong(METRIKA_AUDIENCE_UPPER_BOUND + 3, METRIKA_AUDIENCE_UPPER_BOUND + 9)))));
        return rules;
    }

    public static RetargetingCondition bigRetCondition(@Nullable ClientId clientId) {
        RetargetingCondition retargetingCondition = new RetargetingCondition();
        retargetingCondition.withClientId(clientId != null ? clientId.asLong() : null)
                .withName("big test retargeting condition " + RandomStringUtils.randomNumeric(5))
                .withDescription("big test retargeting condition description " + RandomStringUtils.randomNumeric(5))
                .withDeleted(false)
                .withInterest(false)
                .withAutoRetargeting(false)
                .withType(DEFAULT_TYPE)
                .withLastChangeTime(LocalDateTime.now())
                .withRules(bigRules());
        return retargetingCondition;
    }

    public static RetargetingCondition bigRetConditionWithLalSegments(@Nullable ClientId clientId) {
        RetargetingCondition retargetingCondition = new RetargetingCondition();
        retargetingCondition.withClientId(clientId != null ? clientId.asLong() : null)
                .withName("big test retargeting condition with lal-segments " + RandomStringUtils.randomNumeric(5))
                .withDescription("big test retargeting condition description with lal-segments  " +
                        RandomStringUtils.randomNumeric(5))
                .withDeleted(false)
                .withInterest(false)
                .withType(DEFAULT_TYPE)
                .withLastChangeTime(LocalDateTime.now())
                .withRules(bigRulesWithLalSegments());
        return retargetingCondition;
    }

    public static RetargetingCondition defaultABSegmentRetCondition(@Nullable ClientId clientId) {
        RetargetingCondition retargetingCondition = new RetargetingCondition();
        retargetingCondition.withClientId(clientId != null ? clientId.asLong() : null)
                .withName("default test AB-segment retargeting condition " + RandomStringUtils.randomNumeric(5))
                .withDescription("default test AB-segment retargeting condition description " + RandomStringUtils.randomNumeric(5))
                .withDeleted(false)
                .withInterest(false)
                .withType(ConditionType.ab_segments)
                .withLastChangeTime(LocalDateTime.now())
                .withRules(defaultABSegmentRules());
        return retargetingCondition;

    }

    public static RetargetingCondition defaultBrandSafetyRetCondition(@Nullable ClientId clientId) {
        RetargetingCondition retargetingCondition = new RetargetingCondition();
        retargetingCondition.withClientId(clientId != null ? clientId.asLong() : null)
                .withName("default test brand safety retargeting condition " + RandomStringUtils.randomNumeric(5))
                .withDescription("default test brand safety retargeting condition description " + RandomStringUtils.randomNumeric(5))
                .withDeleted(false)
                .withInterest(false)
                .withType(ConditionType.brandsafety)
                .withLastChangeTime(LocalDateTime.now())
                .withRules(defaultBrandSafetyRules());
        return retargetingCondition;
    }

    public static RetargetingCondition interestsRetCondition(@Nullable ClientId clientId, List<Goal> goals) {
        return (RetargetingCondition) defaultRetCondition(clientId)
                .withType(ConditionType.interests)
                .withRules(defaultRules(goals));
    }

    public static RetargetingConditionGoal defaultGoal(@Nullable Long id) {
        return defaultGoalByTypeAndId(id, GoalType.GOAL);
    }

    public static RetargetingConditionGoal defaultGoal() {
        return defaultGoal(null);
    }

    public static List<Rule> defaultRules() {
        List<Goal> goals = defaultMetrikaGoals();
        goals.forEach(goal -> goal.withAllowToUse(null).withName(null));

        return singletonList(defaultRule(goals, RuleType.ALL, null));
    }

    public static List<Rule> defaultRules(List<Goal>... listsOfGoals) {
        return Stream.of(listsOfGoals)
                .map(TestRetargetingConditions::defaultRule)
                .collect(toList());
    }

    public static List<Rule> defaultRules(List<List<Goal>> listsOfGoals) {
        return listsOfGoals.stream()
                .map(TestRetargetingConditions::defaultRule)
                .collect(toList());
    }

    public static Rule defaultRule(List<Goal> goals) {
        return defaultRule(goals, RuleType.OR);
    }

    public static Rule defaultRule(List<Goal> goals, RuleType type) {
        return defaultRule(goals, type, CryptaInterestType.all);
    }

    public static Rule defaultRule(List<Goal> goals, CryptaInterestType interestType) {
        return defaultRule(goals, RuleType.OR, interestType);
    }

    public static Rule defaultRule(List<Goal> goals, RuleType type, CryptaInterestType interestType) {
        return new Rule()
                .withType(type)
                .withGoals(goals)
                .withInterestType(goals.get(0).getType() == GoalType.INTERESTS ? interestType : null);
    }

    public static List<Rule> bigRules() {
        Goal goal1 = defaultGoalByType(GoalType.GOAL);
        Goal goal2 = defaultGoalByTypeAndId(458L, GoalType.GOAL);
        Goal goal3 = defaultGoalByType(GoalType.GOAL);
        Goal goal4 = defaultGoalByTypeAndId(439L, GoalType.GOAL);

        return asList(defaultRule(asList(goal1, goal2), RuleType.ALL), defaultRule(asList(goal3, goal4), RuleType.OR));
    }

    public static List<Rule> bigRulesWithLalSegments() {
        List<Goal> parentAndLalSegment = defaultParentAndLalSegmentPair();
        parentAndLalSegment.get(1).setUnionWithId(null);

        return asList(
                defaultRule(defaultParentAndLalSegmentPair(), RuleType.ALL),
                defaultRule(asList(defaultGoalByType(GoalType.GOAL), defaultLalSegmentGoal()), RuleType.OR),
                defaultRule(parentAndLalSegment, RuleType.NOT)
        );
    }

    public static List<Rule> defaultABSegmentRules() {
        Goal goal = defaultABSegmentGoal();
        return singletonList(defaultRule(singletonList(goal), RuleType.ALL).withSectionId(goal.getSectionId()));
    }

    public static List<Rule> defaultBrandSafetyRules() {
        Goal goal1 = defaultBrandSafetyGoal();
        Goal goal2 = defaultBrandSafetyGoal();
        return singletonList(defaultRule(asList(goal1, goal2), RuleType.NOT));
    }

    public static List<RetargetingCondition> bigRetConditions(ClientId clientId, int number) {
        return retConditions(clientId, number, TestRetargetingConditions::bigRetCondition);
    }

    public static List<RetargetingCondition> bigRetConditionsWithLalSegments(ClientId clientId, int number) {
        return retConditions(clientId, number, TestRetargetingConditions::bigRetConditionWithLalSegments);
    }

    public static List<RetargetingCondition> defaultRetConditions(ClientId clientId, int number) {
        return retConditions(clientId, number, TestRetargetingConditions::defaultRetCondition);
    }

    protected static List<RetargetingCondition> retConditions(ClientId clientId, int number,
                                                              Function<ClientId, RetargetingCondition> retConditionCreator) {
        List<RetargetingCondition> conditions = new ArrayList<>(number);
        for (int i = 0; i < number; i++) {
            RetargetingCondition condition = retConditionCreator.apply(clientId);
            condition.setDescription("condition number " + i);
            conditions.add(condition);
        }
        return conditions;
    }

    public static Goal ltvGoal() {
        return defaultGoalByTypeAndId(LTV_GOAL_ID, GoalType.BEHAVIORS);
    }

    public static Rule ltvRule() {
        return new Rule().withType(RuleType.OR).withGoals(List.of(ltvGoal()));
    }

    public static Rule ruleOrSocialDemo(Long goalId) {
        return ruleOrSocialDemo(List.of(goalId));
    }

    public static Rule ruleOrSocialDemo(List<Long> goalIds) {
        List<Goal> goals = StreamEx.of(goalIds)
                .map(id -> defaultGoalByTypeAndId(id, GoalType.SOCIAL_DEMO))
                .toList();
        return new Rule()
                .withType(RuleType.OR)
                .withGoals(goals);
    }
}
