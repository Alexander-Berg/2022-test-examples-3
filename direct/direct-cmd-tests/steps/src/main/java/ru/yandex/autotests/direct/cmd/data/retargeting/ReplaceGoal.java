package ru.yandex.autotests.direct.cmd.data.retargeting;

import com.google.gson.annotations.SerializedName;

public class ReplaceGoal {

    @SerializedName("old_goal_id")
    private String oldGoalId;

    @SerializedName("new_goal_id")
    private String newGoalId;

    @SerializedName("goal_type")
    private String goalType;

    public String getOldGoalId() {
        return oldGoalId;
    }

    public ReplaceGoal withOldGoalId(String oldGoalId) {
        this.oldGoalId = oldGoalId;
        return this;
    }

    public String getNewGoalId() {
        return newGoalId;
    }

    public ReplaceGoal withNewGoalId(String newGoalId) {
        this.newGoalId = newGoalId;
        return this;
    }

    public String getGoalType() {
        return goalType;
    }

    public ReplaceGoal withGoalType(String goalType) {
        this.goalType = goalType;
        return this;
    }
}
