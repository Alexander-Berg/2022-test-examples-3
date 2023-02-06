package ru.yandex.direct.core.entity.retargeting.model;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

@RunWith(Parameterized.class)
public class RetargetingConditionGoalComputeTypeTest {

    @Parameterized.Parameters(name = "goalId = {0}, expected goalType = {1}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {0L, GoalType.GOAL},
                {1L, GoalType.GOAL},
                {999_999_999L, GoalType.GOAL},
                {1_000_000_000L, GoalType.SEGMENT},
                {1_499_999_999L, GoalType.SEGMENT},
                {1_500_000_000L, GoalType.LAL_SEGMENT},
                {1_899_999_999L, GoalType.LAL_SEGMENT},
                {1_900_000_000L, GoalType.MOBILE},
                {1_999_999_999L, GoalType.MOBILE},
                {2_000_000_000L, GoalType.AUDIENCE},
                {2_500_000_000L, GoalType.AB_SEGMENT},
                {2_599_999_999L, GoalType.AB_SEGMENT},
                {2_600_000_000L, GoalType.CDP_SEGMENT},
                {2_999_999_999L, GoalType.CDP_SEGMENT},
                {3_000_000_000L, GoalType.ECOMMERCE},
                {3_899_999_999L, GoalType.ECOMMERCE},
                {3_900_000_000L, GoalType.GOAL},

                // strange default from perl
                {4_000_000_000L, GoalType.GOAL},
                {5_000_000_000L, GoalType.GOAL},

                {4_294_967_295L, GoalType.GOAL},
                {4_294_967_296L, GoalType.BRANDSAFETY},
                {4_294_968_295L, GoalType.BRANDSAFETY},
                {4_294_968_296L, GoalType.CONTENT_CATEGORY},
                {4_294_970_295L, GoalType.CONTENT_CATEGORY},
                {4_294_970_296L, GoalType.CONTENT_GENRE},
                {4_294_972_295L, GoalType.CONTENT_GENRE},
                {4_294_972_296L, GoalType.GOAL},
        });
    }

    @Parameterized.Parameter(0)
    public Long goalId;

    @Parameterized.Parameter(1)
    public GoalType expectedGoalType;

    private Goal goal;

    @Before
    public void before() {
        goal = new Goal();
        goal.withId(goalId);
    }

    @Test
    public void getType_IdConformsToTypeGoal_TypeGoal() {
        assertThat(goal.getType(), equalTo(expectedGoalType));
    }
}
