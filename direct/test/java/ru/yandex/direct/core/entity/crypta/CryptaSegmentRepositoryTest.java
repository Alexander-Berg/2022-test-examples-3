package ru.yandex.direct.core.entity.crypta;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.crypta.repository.CryptaSegmentRepository;
import ru.yandex.direct.core.entity.retargeting.model.CryptaGoalScope;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.GoalType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.repository.TestCryptaSegmentRepository;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static java.util.Collections.singleton;
import static junit.framework.TestCase.assertEquals;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultGoalByTypeAndId;
import static ru.yandex.direct.dbschema.ppcdict.tables.CryptaGoals.CRYPTA_GOALS;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CryptaSegmentRepositoryTest {
    // идентификаторы целей указаны чтобы обойти ошибки с одинаковыми рандомными id в CI
    private static final Goal GOAL_SOCIAL_DEMO_TYPE =
            (Goal) defaultGoalByTypeAndId(2499000097L, GoalType.SOCIAL_DEMO).withParentId(0L)
                    .withCryptaScope(Set.of(CryptaGoalScope.COMMON, CryptaGoalScope.INTERNAL_AD));
    private static final Goal GOAL_SOCIAL_DEMO =
            (Goal) defaultGoalByTypeAndId(2499000098L, GoalType.SOCIAL_DEMO).withParentId(123L)
                    .withCryptaScope(Set.of(CryptaGoalScope.COMMON, CryptaGoalScope.INTERNAL_AD));
    private static final Goal GOAL_INTERESTS =
            (Goal) defaultGoalByTypeAndId(2499051397L, GoalType.INTERESTS).withParentId(0L)
                    .withCryptaScope(Set.of(CryptaGoalScope.COMMON));
    private static final Goal GOAL_PARENT_INTERESTS =
            (Goal) defaultGoalByTypeAndId(2499027805L, GoalType.INTERESTS).withParentId(456L)
                    .withCryptaScope(Set.of(CryptaGoalScope.COMMON));
    private static final Goal GOAL_BEHAVIORS_TYPE =
            (Goal) defaultGoalByTypeAndId(2499000591L, GoalType.BEHAVIORS).withParentId(0L)
                    .withCryptaScope(Set.of(CryptaGoalScope.COMMON));
    private static final Goal GOAL_BEHAVIORS =
            (Goal) defaultGoalByTypeAndId(2499000952L, GoalType.BEHAVIORS).withParentId(42L)
                    .withCryptaScope(Set.of(CryptaGoalScope.COMMON));
    private static final Goal GOAL_AUDIO_GENRES =
            (Goal) defaultGoalByTypeAndId(2499990001L, GoalType.AUDIO_GENRES).withParentId(0L)
                    .withCryptaScope(Set.of(CryptaGoalScope.COMMON));
    private static final Goal GOAL_BRAND_SAFETY =
            (Goal) defaultGoalByTypeAndId(4_294_967_296L, GoalType.BRANDSAFETY).withParentId(0L)
                    .withCryptaScope(Set.of(CryptaGoalScope.COMMON));
    private static final Goal GOAL_CONTENT_CATEGORY =
            (Goal) defaultGoalByTypeAndId(4_294_968_296L, GoalType.CONTENT_CATEGORY).withParentId(0L)
                    .withCryptaScope(Set.of(CryptaGoalScope.COMMON));
    private static final Goal GOAL_CONTENT_GENRE =
            (Goal) defaultGoalByTypeAndId(4_294_970_296L, GoalType.CONTENT_GENRE).withParentId(14L)

                    .withCryptaScope(Set.of(CryptaGoalScope.COMMON));
    private static final Goal CUSTOM_AUDIENCE_GOAL_FOR_MEDIA =
            (Goal) defaultGoalByTypeAndId(Goal.CRYPTA_INTERESTS_CA_LOWER_BOUND, GoalType.INTERESTS)
                    .withCryptaScope(Set.of(CryptaGoalScope.MEDIA));
    private static final Goal CUSTOM_AUDIENCE_GOAL_FOR_PERFORMANCE =
            (Goal) defaultGoalByTypeAndId(Goal.CRYPTA_INTERESTS_CA_LOWER_BOUND + 1, GoalType.INTERESTS)
                    .withCryptaScope(Set.of(CryptaGoalScope.PERFORMANCE));
    private static final Goal CUSTOM_AUDIENCE_GOAL_FOR_ALL_CAMPAIGNS =
            (Goal) defaultGoalByTypeAndId(Goal.CRYPTA_INTERESTS_CA_LOWER_BOUND + 2, GoalType.INTERESTS)
                    .withCryptaScope(Set.of(CryptaGoalScope.PERFORMANCE, CryptaGoalScope.MEDIA));


    private static final Goal GOAL_SOCIAL_DEMO_INTERNAL_AD =
            (Goal) defaultGoalByTypeAndId(2499000096L, GoalType.SOCIAL_DEMO).withParentId(123L)
                    .withCryptaScope(Set.of(CryptaGoalScope.INTERNAL_AD));

    private static final Set<Goal> INTERNAL_AD_ONLY_GOALS = Set.of(GOAL_SOCIAL_DEMO_INTERNAL_AD);
    private static final Set<Goal> COMMON_AND_INTERNAL_AD_GOALS = Set.of(GOAL_SOCIAL_DEMO_TYPE, GOAL_SOCIAL_DEMO);
    private static final Set<Goal> COMMON_ONLY_GOALS = Set.of(GOAL_INTERESTS, GOAL_PARENT_INTERESTS,
            GOAL_BEHAVIORS_TYPE, GOAL_BEHAVIORS, GOAL_AUDIO_GENRES, GOAL_BRAND_SAFETY, GOAL_CONTENT_CATEGORY,
            GOAL_CONTENT_GENRE);

    private static final Set<Goal> COMMON_GOALS = Sets.union(COMMON_ONLY_GOALS, COMMON_AND_INTERNAL_AD_GOALS);
    private static final Set<Goal> INTERNAL_AD_GOALS = Sets.union(INTERNAL_AD_ONLY_GOALS, COMMON_AND_INTERNAL_AD_GOALS);
    private static final Set<Goal> CUSTOM_AUDIENCE_GOALS = Set.of(
            CUSTOM_AUDIENCE_GOAL_FOR_PERFORMANCE, CUSTOM_AUDIENCE_GOAL_FOR_MEDIA, CUSTOM_AUDIENCE_GOAL_FOR_ALL_CAMPAIGNS
    );

    @Autowired
    private CryptaSegmentRepository cryptaSegmentRepository;

    @Autowired
    private TestCryptaSegmentRepository testCryptaSegmentRepository;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Before
    public void before() {
        dslContextProvider.ppcdict().truncate(CRYPTA_GOALS).execute();

        testCryptaSegmentRepository.addAll(COMMON_ONLY_GOALS, Set.of(CryptaGoalScope.COMMON));
        testCryptaSegmentRepository.addAll(INTERNAL_AD_ONLY_GOALS, Set.of(CryptaGoalScope.INTERNAL_AD));
        testCryptaSegmentRepository.addAll(
                COMMON_AND_INTERNAL_AD_GOALS, Set.of(CryptaGoalScope.COMMON, CryptaGoalScope.INTERNAL_AD));
        CUSTOM_AUDIENCE_GOALS.forEach(goal -> testCryptaSegmentRepository.addAll(Set.of(goal), goal.getCryptaScope()));
    }

    @Test
    public void getAll_WithoutDefinedScope() {
        assertEquals(COMMON_GOALS, Set.copyOf(cryptaSegmentRepository.getAll().values()));
    }

    @Test
    public void getAll_Common() {
        assertEquals(COMMON_GOALS, Set.copyOf(cryptaSegmentRepository.getAll(CryptaGoalScope.COMMON).values()));
    }

    @Test
    public void getAll_InternalAd() {
        assertEquals(INTERNAL_AD_GOALS,
                Set.copyOf(cryptaSegmentRepository.getAll(CryptaGoalScope.INTERNAL_AD).values()));
    }

    @Test
    public void getSocialDemoTypes() {
        assertEquals(Set.of(GOAL_SOCIAL_DEMO_TYPE, GOAL_BEHAVIORS_TYPE),
                new HashSet<>(cryptaSegmentRepository.getSocialDemoTypes().values()));
    }

    @Test
    public void getWithoutBrandSafety() {
        assertEquals(Set.of(GOAL_SOCIAL_DEMO_TYPE, GOAL_SOCIAL_DEMO, GOAL_INTERESTS, GOAL_PARENT_INTERESTS,
                GOAL_BEHAVIORS_TYPE, GOAL_BEHAVIORS, GOAL_AUDIO_GENRES, GOAL_CONTENT_CATEGORY, GOAL_CONTENT_GENRE),
                new HashSet<>(cryptaSegmentRepository.getWithoutBrandSafety(CryptaGoalScope.COMMON).values()));
    }

    @Test
    public void getSocialDemo() {
        assertEquals(Set.of(GOAL_SOCIAL_DEMO, GOAL_BEHAVIORS),
                new HashSet<>(cryptaSegmentRepository.getSocialDemo().values()));
    }

    @Test
    public void getInterests() {
        assertEquals(Set.of(GOAL_INTERESTS, GOAL_PARENT_INTERESTS),
                new HashSet<>(cryptaSegmentRepository.getInterests().values()));
    }

    @Test
    public void getAudioGenres() {
        assertEquals(singleton(GOAL_AUDIO_GENRES),
                new HashSet<>(cryptaSegmentRepository.getAudioGenres().values()));
    }

    @Test
    public void getBrandSafety() {
        assertEquals(singleton(GOAL_BRAND_SAFETY),
                new HashSet<>(cryptaSegmentRepository.getBrandSafety().values()));
    }

    @Test
    public void getBehaviors() {
        assertEquals(Set.of(GOAL_BEHAVIORS),
                new HashSet<>(cryptaSegmentRepository.getBehaviorsByIds(
                        List.of(GOAL_BEHAVIORS.getId(),
                                GOAL_AUDIO_GENRES.getId(),
                                GOAL_SOCIAL_DEMO.getId(),
                                GOAL_INTERESTS.getId())
                ).values()));
    }

    @Test
    public void getByTypes() {
        assertEquals(Set.of(GOAL_BRAND_SAFETY, GOAL_CONTENT_CATEGORY, GOAL_CONTENT_GENRE),
                new HashSet<>(cryptaSegmentRepository.getBrandSafetyAndContentSegments().values()));
    }

    @Test
    public void checkAddGoal() {
        Goal goal = (Goal) defaultGoalByTypeAndId(Goal.CRYPTA_INTERESTS_UPPER_BOUND - 10, GoalType.INTERESTS)
                .withParentId(1234L)
                .withCryptaScope(Set.of(CryptaGoalScope.COMMON, CryptaGoalScope.INTERNAL_AD));

        int addedCount = cryptaSegmentRepository.add(List.of(goal));
        assertEquals(addedCount, 1);

        Goal actual = cryptaSegmentRepository.getById(goal.getId());
        assertEquals(actual, goal);
    }


    @Test
    public void shouldReturnAllGoalsForCustomAudience() {
        assertEquals(
                CUSTOM_AUDIENCE_GOALS,
                Set.copyOf(
                        cryptaSegmentRepository.getAll(List.of(CryptaGoalScope.MEDIA, CryptaGoalScope.PERFORMANCE))
                                .values()
                )
        );
    }

    @Test
    public void shouldReturnGoalsForCustomAudience_campaignIsMedia() {
        assertEquals(
                Set.of(CUSTOM_AUDIENCE_GOAL_FOR_MEDIA, CUSTOM_AUDIENCE_GOAL_FOR_ALL_CAMPAIGNS),
                Set.copyOf(cryptaSegmentRepository.getAll(CryptaGoalScope.MEDIA).values())
        );
    }

    @Test
    public void shouldReturnGoalsForCustomAudience_campaignIsPerformance() {
        assertEquals(
                Set.of(CUSTOM_AUDIENCE_GOAL_FOR_PERFORMANCE, CUSTOM_AUDIENCE_GOAL_FOR_ALL_CAMPAIGNS),
                Set.copyOf(cryptaSegmentRepository.getAll(CryptaGoalScope.PERFORMANCE).values())
        );
    }
}
