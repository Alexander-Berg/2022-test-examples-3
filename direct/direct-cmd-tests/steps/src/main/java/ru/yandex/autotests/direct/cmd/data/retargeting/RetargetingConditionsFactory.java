package ru.yandex.autotests.direct.cmd.data.retargeting;

import ru.yandex.autotests.direct.cmd.data.commons.group.RetConditionItemType;
import ru.yandex.autotests.direct.cmd.data.commons.group.RetargetingCondition;
import ru.yandex.autotests.direct.cmd.data.commons.group.RetargetingConditionItem;
import ru.yandex.autotests.direct.cmd.data.commons.group.RetargetingGoal;
import ru.yandex.autotests.direct.cmd.data.counters.MetrikaCountersData;

import java.util.Arrays;

import static java.util.Collections.singletonList;

public class RetargetingConditionsFactory {
    public static final String[] AUDIENCES = {"2000110532", "2000110555", "2000110556"};

    public static RetargetingCondition getEmptyRetargetingCondition() {
        return new RetargetingCondition().withConditionName("Condition").withConditionDesc("desc");
    }

    public static final RetargetingCondition RET_COND_AUDIENCE_ALL_1 = getEmptyRetargetingCondition()
            .withCondition(singletonList(new RetargetingConditionItem()
                            .withType(RetConditionItemType.ALL.getValue())
                            .withGoals(singletonList(RetargetingGoal.forAudience(AUDIENCES[0])))
                    )
            );

    public static final RetargetingCondition RET_COND_AUDIENCE_ALL_2 = getEmptyRetargetingCondition()
            .withCondition(singletonList(new RetargetingConditionItem()
                            .withType(RetConditionItemType.ALL.getValue())
                            .withGoals(Arrays.asList(RetargetingGoal.forAudience(AUDIENCES[0]),
                                    RetargetingGoal.forAudience(AUDIENCES[1])))
                    )
            );

    public static final RetargetingCondition RET_COND_AUDIENCE_OR_1 = getEmptyRetargetingCondition()
            .withCondition(singletonList(new RetargetingConditionItem()
                            .withType(RetConditionItemType.OR.getValue())
                            .withGoals(singletonList(RetargetingGoal.forAudience(AUDIENCES[0])))
                    )
            );

    public static final RetargetingCondition RET_COND_AUDIENCE_OR_2 = getEmptyRetargetingCondition()
            .withCondition(singletonList(new RetargetingConditionItem()
                            .withType(RetConditionItemType.OR.getValue())
                            .withGoals(Arrays.asList(RetargetingGoal.forAudience(AUDIENCES[0]),
                                    RetargetingGoal.forAudience(AUDIENCES[1])))
                    )
            );

    public static final RetargetingCondition RET_COND_AUDIENCE_NOT_1_ALL_1 = getEmptyRetargetingCondition()
            .withCondition(Arrays.asList(new RetargetingConditionItem()
                            .withType(RetConditionItemType.NOT.getValue())
                            .withGoals(singletonList(RetargetingGoal.forAudience(AUDIENCES[0]))),
                    new RetargetingConditionItem()
                            .withType(RetConditionItemType.ALL.getValue())
                            .withGoals(singletonList(RetargetingGoal.forAudience(AUDIENCES[1])))
                    )
            );

    public static final RetargetingCondition RET_COND_AUDIENCE_ALL_1_ALL_1 = getEmptyRetargetingCondition()
            .withCondition(Arrays.asList(new RetargetingConditionItem()
                            .withType(RetConditionItemType.ALL.getValue())
                            .withGoals(singletonList(RetargetingGoal.forAudience(AUDIENCES[0]))),
                    new RetargetingConditionItem()
                            .withType(RetConditionItemType.ALL.getValue())
                            .withGoals(singletonList(RetargetingGoal.forAudience(AUDIENCES[1])))
                    )
            );

    public static final RetargetingCondition RET_COND_AUDIENCE_NOT_2_ALL_2 = getEmptyRetargetingCondition()
            .withCondition(Arrays.asList(new RetargetingConditionItem()
                            .withType(RetConditionItemType.NOT.getValue())
                            .withGoals(Arrays.asList(RetargetingGoal.forAudience(AUDIENCES[0]),
                                    RetargetingGoal.forAudience(AUDIENCES[1]))),
                    new RetargetingConditionItem()
                            .withType(RetConditionItemType.ALL.getValue())
                            .withGoals(Arrays.asList(RetargetingGoal.forAudience(AUDIENCES[0]),
                                    RetargetingGoal.forAudience(AUDIENCES[2])))
                    )
            );

    public static final RetargetingCondition RET_COND_AUDIENCE_NOT_2_OR_2 = getEmptyRetargetingCondition()
            .withCondition(Arrays.asList(new RetargetingConditionItem()
                            .withType(RetConditionItemType.NOT.getValue())
                            .withGoals(Arrays.asList(RetargetingGoal.forAudience(AUDIENCES[0]),
                                    RetargetingGoal.forAudience(AUDIENCES[1]))),
                    new RetargetingConditionItem()
                            .withType(RetConditionItemType.OR.getValue())
                            .withGoals(Arrays.asList(RetargetingGoal.forAudience(AUDIENCES[0]),
                                    RetargetingGoal.forAudience(AUDIENCES[2])))
                    )
            );

    public static final RetargetingCondition RET_COND_GOAL_AND_AUDIENCE_ALL_1_ALL_1 = getEmptyRetargetingCondition()
            .withCondition(Arrays.asList(new RetargetingConditionItem()
                            .withType(RetConditionItemType.ALL.getValue())
                            .withGoals(singletonList(RetargetingGoal.forAudience(AUDIENCES[0]))),
                    new RetargetingConditionItem()
                            .withType(RetConditionItemType.ALL.getValue())
                            .withGoals(singletonList(RetargetingGoal
                                    .forGoal(String.valueOf(MetrikaCountersData.DEFAULT_COUNTER.getFirstGoalId()))))
                    )
            );

}
