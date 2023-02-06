package ru.yandex.direct.web.data;

import ru.yandex.direct.web.core.model.retargeting.Condition;
import ru.yandex.direct.web.core.model.retargeting.RetargetingConditionRuleType;

import static ru.yandex.direct.web.data.TestGoal.defaultWebMetrikaGoals;

class TestCondition {

    static Condition defaultCondition() {
        return new Condition()
                .withConditionType(RetargetingConditionRuleType.all)
                .withConditionGoalWebs(defaultWebMetrikaGoals());
    }
}
