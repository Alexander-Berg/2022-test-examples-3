package ru.yandex.autotests.direct.cmd.data.commons;

import com.google.gson.annotations.SerializedName;

public class Goal {
    @SerializedName("goal_id")
    private String goalId;

    @SerializedName("name")
    private String name;

    @SerializedName("parent_goal_id")
    private String parentGoalId;

    @SerializedName("goal_type")
    private String goalType;

    @SerializedName("status")
    private String status;

    @SerializedName("subgoal_index")
    private String subgoalIndex;

    public String getGoalId() {
        return goalId;
    }

    public String getName() {
        return name;
    }

    public String getParentGoalId() {
        return parentGoalId;
    }

    public String getGoalType() {
        return goalType;
    }

    public String getStatus() {
        return status;
    }

    public String getSubgoalIndex() {
        return subgoalIndex;
    }
}
