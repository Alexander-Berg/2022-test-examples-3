package ru.yandex.direct.core.testing.data;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.LongStream;

import javax.annotation.Nullable;

import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.GoalType;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.stream.Collectors.toList;
import static ru.yandex.direct.core.entity.retargeting.model.Goal.AB_SEGMENT_UPPER_BOUND;
import static ru.yandex.direct.core.entity.retargeting.model.Goal.BRANDSAFETY_LOWER_BOUND;
import static ru.yandex.direct.core.entity.retargeting.model.Goal.BRANDSAFETY_UPPER_BOUND;
import static ru.yandex.direct.core.entity.retargeting.model.Goal.CDP_SEGMENT_UPPER_BOUND;
import static ru.yandex.direct.core.entity.retargeting.model.Goal.CONTENT_CATEGORY_UPPER_BOUND;
import static ru.yandex.direct.core.entity.retargeting.model.Goal.CONTENT_GENRE_UPPER_BOUND;
import static ru.yandex.direct.core.entity.retargeting.model.Goal.CRYPTA_BEHAVIORS_UPPER_BOUND;
import static ru.yandex.direct.core.entity.retargeting.model.Goal.CRYPTA_FAMILY_UPPER_BOUND;
import static ru.yandex.direct.core.entity.retargeting.model.Goal.CRYPTA_INTERESTS_CA_LOWER_BOUND;
import static ru.yandex.direct.core.entity.retargeting.model.Goal.CRYPTA_INTERESTS_UPPER_BOUND;
import static ru.yandex.direct.core.entity.retargeting.model.Goal.CRYPTA_INTERNAL_UPPER_BOUND;
import static ru.yandex.direct.core.entity.retargeting.model.Goal.CRYPTA_SOCIAL_DEMO_UPPER_BOUND;
import static ru.yandex.direct.core.entity.retargeting.model.Goal.HOST_LOWER_BOUND;
import static ru.yandex.direct.core.entity.retargeting.model.Goal.HOST_UPPER_BOUND;
import static ru.yandex.direct.core.entity.retargeting.model.Goal.LAL_SEGMENT_UPPER_BOUND;
import static ru.yandex.direct.core.entity.retargeting.model.Goal.METRIKA_AUDIENCE_UPPER_BOUND;
import static ru.yandex.direct.core.entity.retargeting.model.Goal.METRIKA_ECOMMERCE_UPPER_BOUND;
import static ru.yandex.direct.core.entity.retargeting.model.Goal.METRIKA_GOAL_UPPER_BOUND;
import static ru.yandex.direct.core.entity.retargeting.model.Goal.METRIKA_SEGMENT_UPPER_BOUND;
import static ru.yandex.direct.core.entity.retargeting.model.Goal.MOBILE_GOAL_UPPER_BOUND;
import static ru.yandex.direct.core.entity.retargeting.model.Goal.MUSIC_AUDIO_GENRES_UPPER_BOUND;
import static ru.yandex.direct.core.entity.retargeting.model.GoalType.AB_SEGMENT;
import static ru.yandex.direct.core.entity.retargeting.model.GoalType.AUDIENCE;
import static ru.yandex.direct.core.entity.retargeting.model.GoalType.AUDIO_GENRES;
import static ru.yandex.direct.core.entity.retargeting.model.GoalType.BEHAVIORS;
import static ru.yandex.direct.core.entity.retargeting.model.GoalType.BRANDSAFETY;
import static ru.yandex.direct.core.entity.retargeting.model.GoalType.CDP_SEGMENT;
import static ru.yandex.direct.core.entity.retargeting.model.GoalType.CONTENT_CATEGORY;
import static ru.yandex.direct.core.entity.retargeting.model.GoalType.CONTENT_GENRE;
import static ru.yandex.direct.core.entity.retargeting.model.GoalType.ECOMMERCE;
import static ru.yandex.direct.core.entity.retargeting.model.GoalType.FAMILY;
import static ru.yandex.direct.core.entity.retargeting.model.GoalType.GOAL;
import static ru.yandex.direct.core.entity.retargeting.model.GoalType.HOST;
import static ru.yandex.direct.core.entity.retargeting.model.GoalType.INTERESTS;
import static ru.yandex.direct.core.entity.retargeting.model.GoalType.INTERNAL;
import static ru.yandex.direct.core.entity.retargeting.model.GoalType.LAL_SEGMENT;
import static ru.yandex.direct.core.entity.retargeting.model.GoalType.MOBILE;
import static ru.yandex.direct.core.entity.retargeting.model.GoalType.SEGMENT;
import static ru.yandex.direct.core.entity.retargeting.model.GoalType.SOCIAL_DEMO;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

public final class TestFullGoals {
    private static final Map<GoalType, Pair<Long, Long>> TYPE_INTERVALS = new HashMap<>();

    static {
        TYPE_INTERVALS.put(GOAL, new ImmutablePair<>(1L, METRIKA_GOAL_UPPER_BOUND - 1));
        TYPE_INTERVALS.put(SEGMENT, new ImmutablePair<>(METRIKA_GOAL_UPPER_BOUND, METRIKA_SEGMENT_UPPER_BOUND - 1));
        TYPE_INTERVALS.put(LAL_SEGMENT, new ImmutablePair<>(METRIKA_SEGMENT_UPPER_BOUND, LAL_SEGMENT_UPPER_BOUND - 1));
        TYPE_INTERVALS.put(MOBILE, new ImmutablePair<>(LAL_SEGMENT_UPPER_BOUND, MOBILE_GOAL_UPPER_BOUND - 1));
        TYPE_INTERVALS.put(AUDIENCE, new ImmutablePair<>(MOBILE_GOAL_UPPER_BOUND, METRIKA_AUDIENCE_UPPER_BOUND - 1));
        TYPE_INTERVALS.put(SOCIAL_DEMO,
                new ImmutablePair<>(METRIKA_AUDIENCE_UPPER_BOUND, CRYPTA_SOCIAL_DEMO_UPPER_BOUND - 1));
        TYPE_INTERVALS.put(FAMILY, new ImmutablePair<>(CRYPTA_SOCIAL_DEMO_UPPER_BOUND, CRYPTA_FAMILY_UPPER_BOUND - 1));
        TYPE_INTERVALS.put(BEHAVIORS, new ImmutablePair<>(CRYPTA_FAMILY_UPPER_BOUND, CRYPTA_BEHAVIORS_UPPER_BOUND - 1));
        TYPE_INTERVALS
                .put(INTERESTS, new ImmutablePair<>(CRYPTA_BEHAVIORS_UPPER_BOUND, CRYPTA_INTERESTS_CA_LOWER_BOUND - 1));
        TYPE_INTERVALS
                .put(INTERNAL, new ImmutablePair<>(CRYPTA_INTERESTS_UPPER_BOUND, CRYPTA_INTERNAL_UPPER_BOUND - 1));
        TYPE_INTERVALS
                .put(AUDIO_GENRES, new ImmutablePair<>(CRYPTA_INTERNAL_UPPER_BOUND,
                        MUSIC_AUDIO_GENRES_UPPER_BOUND - 1));
        TYPE_INTERVALS.put(AB_SEGMENT, new ImmutablePair<>(MUSIC_AUDIO_GENRES_UPPER_BOUND, AB_SEGMENT_UPPER_BOUND - 1));
        TYPE_INTERVALS.put(CDP_SEGMENT, new ImmutablePair<>(AB_SEGMENT_UPPER_BOUND, CDP_SEGMENT_UPPER_BOUND - 1));
        TYPE_INTERVALS.put(ECOMMERCE, new ImmutablePair<>(CDP_SEGMENT_UPPER_BOUND, METRIKA_ECOMMERCE_UPPER_BOUND - 1));
        TYPE_INTERVALS.put(BRANDSAFETY, Pair.of(BRANDSAFETY_LOWER_BOUND, BRANDSAFETY_UPPER_BOUND));
        TYPE_INTERVALS.put(CONTENT_CATEGORY,
                new ImmutablePair<>(BRANDSAFETY_UPPER_BOUND, CONTENT_CATEGORY_UPPER_BOUND));
        TYPE_INTERVALS.put(CONTENT_GENRE, new ImmutablePair<>(CONTENT_CATEGORY_UPPER_BOUND, CONTENT_GENRE_UPPER_BOUND));
        TYPE_INTERVALS.put(HOST, new ImmutablePair<>(HOST_LOWER_BOUND, HOST_UPPER_BOUND));
    }

    public static Goal defaultGoal(Long id) {
        return defaultGoalByTypeAndId(id, GOAL);
    }

    public static Goal defaultAudience(Long id) {
        return (Goal) defaultGoalByTypeAndId(id, AUDIENCE).withSubtype("uploading_email");
    }

    public static Goal defaultGoalByTypeAndId(Long id, GoalType type) {
        Goal goal = defaultGoalWithId(id, type);
        if (type.isCrypta()) {
            goal.withName(type.name().toLowerCase() + id);
        }
        return goal;
    }

    public static Goal defaultGoalByType(GoalType type) {
        return defaultGoalByTypeAndId(null, type);
    }

    public static Goal defaultGoalByTypeWithTankerName(GoalType type) {
        var goal = defaultGoalByTypeAndId(null, type);
        return (Goal) goal.withTankerNameKey("tanker_name_" + goal.getId());
    }

    public static Goal defaultGoalWithId(Long id, GoalType type) {
        return TestFullGoals.defaultGoalWithId(id, type, null);
    }

    public static Goal defaultGoalWithId(Long id, GoalType type, @Nullable Integer period) {
        if (id == null) {
            id = generateGoalId(type);
        } else {
            checkArgument(Goal.computeType(id) == type);
        }
        Goal goal = new Goal();
        goal.withId(id);

        if (type.isMetrika() || type.isDirect()) {
            goal.withTime(period == null ? RandomUtils.nextInt(1, 91) : period)
                    .withAllowToUse(true);
        } else {
            goal.withKeyword("" + RandomUtils.nextInt(1, 10000))
                    .withKeywordValue("" + RandomUtils.nextInt(1, 10000));

            if (type == INTERESTS) {
                goal.withKeywordShort("" + RandomUtils.nextInt(1, 10000))
                        .withKeywordValueShort("" + RandomUtils.nextInt(1, 10000));
            }
        }
        return goal;
    }

    public static Goal defaultAudience() {
        return defaultAudience(null);
    }

    public static List<Goal> defaultGoals(List<Long> ids) {
        return mapList(ids, TestFullGoals::defaultGoal);
    }

    public static List<Goal> defaultGoals(Integer numberOfGoals) {
        return defaultGoals(LongStream.rangeClosed(1, numberOfGoals).boxed().collect(toList()));
    }

    public static List<Goal> defaultGoals() {
        return mapList(Arrays.asList(GoalType.values()), TestFullGoals::defaultGoalByType);
    }

    public static List<Goal> defaultGoalsWithTankerName() {
        return mapList(Arrays.asList(GoalType.values()), TestFullGoals::defaultGoalByTypeWithTankerName);
    }

    public static List<Goal> defaultMetrikaGoals() {
        return filterMetrikaGoals(defaultGoals());
    }

    public static List<Goal> defaultMetrikaGoalsForLals() {
        return mapList(Arrays.asList(GOAL, SEGMENT, AUDIENCE), TestFullGoals::defaultGoalByType);
    }

    public static List<Goal> defaultCryptaGoals() {
        return filterCryptaGoals(defaultGoals());
    }

    public static List<Goal> defaultCryptaGoalsWithTankerName() {
        return filterCryptaGoals(defaultGoalsWithTankerName());
    }

    public static List<Goal> filterCryptaGoals(List<Goal> goals) {
        return goals.stream().filter(g -> g.getType().isCrypta() && g.getType() != HOST).collect(toList());
    }

    public static List<Goal> filterMetrikaGoals(List<Goal> goals) {
        return goals.stream().filter(g -> g.getType().isMetrika()).collect(toList());
    }

    public static List<Goal> filterLalSegmentGoals(List<Goal> goals) {
        return goals.stream().filter(g -> g.getType() == GoalType.LAL_SEGMENT).collect(toList());
    }

    public static Goal defaultABSegmentGoal() {
        return (Goal) defaultGoalByType(GoalType.AB_SEGMENT).withSectionId(RandomUtils.nextLong(1L, 91L));
    }

    public static Goal defaultBrandSafetyGoal() {
        return defaultGoalByType(GoalType.BRANDSAFETY);
    }

    public static Goal defaultLalSegmentGoal() {
        Goal parentGoal = defaultGoalByType(GoalType.GOAL);
        return defaultLalSegmentGoal(parentGoal.getId());
    }

    public static Goal defaultLalSegmentGoal(Long parentId) {
        return defaultLalSegmentGoal(parentId, null);
    }

    public static Goal defaultLalSegmentGoal(Long parentId, Long unionWithId) {
        return (Goal) defaultGoalByType(GoalType.LAL_SEGMENT)
                .withParentId(parentId)
                .withUnionWithId(unionWithId);
    }

    public static List<Goal> defaultParentAndLalSegmentPair() {
        Goal parentGoal = defaultGoalByType(GoalType.GOAL);
        return List.of(parentGoal, defaultLalSegmentGoal(parentGoal.getId(), parentGoal.getId()));
    }

    public static long generateGoalId(GoalType type) {
        Pair<Long, Long> interval = TYPE_INTERVALS.get(type);
        return RandomUtils.nextLong(interval.getLeft(), interval.getRight());
    }
}
