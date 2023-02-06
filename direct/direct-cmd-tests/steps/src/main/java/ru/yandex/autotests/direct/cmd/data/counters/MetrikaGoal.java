package ru.yandex.autotests.direct.cmd.data.counters;

import com.google.gson.annotations.SerializedName;

public class MetrikaGoal {

    @SerializedName("goal_id")
    private Long goalId;

    @SerializedName("counter_status")
    private String counterStatus;

    @SerializedName("goal_status")
    private String goalStatus;

    @SerializedName("goal_name")
    private String goalName;

    @SerializedName("context_goals_count")
    private Long contextGoalsCount;

    @SerializedName("goals_count")
    private Long goalsCount;

    public Long getGoalId() {
        return goalId;
    }

    public void setGoalId(Long goalId) {
        this.goalId = goalId;
    }

    public String getCounterStatus() {
        return counterStatus;
    }

    public void setCounterStatus(String counterStatus) {
        this.counterStatus = counterStatus;
    }

    public String getGoalStatus() {
        return goalStatus;
    }

    public void setGoalStatus(String goalStatus) {
        this.goalStatus = goalStatus;
    }

    public String getGoalName() {
        return goalName;
    }

    public void setGoalName(String goalName) {
        this.goalName = goalName;
    }

    public Long getContextGoalsCount() {
        return contextGoalsCount;
    }

    public void setContextGoalsCount(Long contextGoalsCount) {
        this.contextGoalsCount = contextGoalsCount;
    }

    public Long getGoalsCount() {
        return goalsCount;
    }

    public void setGoalsCount(Long goalsCount) {
        this.goalsCount = goalsCount;
    }
}
