package ru.yandex.autotests.direct.cmd.data.commons.group;

import com.google.gson.annotations.SerializedName;
import ru.yandex.autotests.directapi.common.api45.RetargetingConditionGoalItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Группа целей Метрики
 * Передается на сервер в retargeting_conditions[] -> condition[],
 * а возвращается в all_retargeting_conditions[] -> condition[].
 *
 * В БД хранится в формате json в таблице retargeting_conditions -> condition
 */
public class RetargetingConditionItem {

    public static final int MAX_GOALS = 250;

    public static RetargetingConditionItem fromApiRetargetingConditionItem(
            ru.yandex.autotests.directapi.common.api45.RetargetingConditionItem apiCondition) {
        RetargetingConditionItem condition = new RetargetingConditionItem().withType(apiCondition.getType());
        List<RetargetingGoal> retargetingGoals = new ArrayList<>();
        for (RetargetingConditionGoalItem goalItem : apiCondition.getGoals()) {
            retargetingGoals.add(new RetargetingGoal()
                    .withGoalId(String.valueOf(goalItem.getGoalID()))
                    .withTime(String.valueOf(goalItem.getTime())));
        }
        condition.withGoals(retargetingGoals);
        return condition;
    }

    @SerializedName("type")
    private String type;

    @SerializedName("goals")
    private List<RetargetingGoal> goals;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public RetargetingConditionItem withType(String type) {
        this.type = type;
        return this;
    }

    public List<RetargetingGoal> getGoals() {
        return goals;
    }

    public void setGoals(List<RetargetingGoal> goals) {
        this.goals = goals;
    }

    public RetargetingConditionItem withGoals(List<RetargetingGoal> goals) {
        this.goals = goals;
        return this;
    }
}
