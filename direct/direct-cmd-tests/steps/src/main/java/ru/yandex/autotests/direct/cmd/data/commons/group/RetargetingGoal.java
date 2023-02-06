package ru.yandex.autotests.direct.cmd.data.commons.group;

import com.google.gson.annotations.SerializedName;

public class RetargetingGoal {

    public static RetargetingGoal forAudience(String audienceId) {
        return new RetargetingGoal().withGoalId(audienceId).withGoalType(GoalType.AUDIENCE);
    }

    public static RetargetingGoal forGoal(String goalId) {
        return new RetargetingGoal().withGoalId(goalId).withGoalType(GoalType.GOAL).withTime("30");
    }

    @SerializedName("goal_id")
    private String goalId;

    @SerializedName("goal_type")
    private GoalType goalType;

    @SerializedName("goal_subtype")
    private String goalSubtype;

    @SerializedName("time")
    private String time;

    @SerializedName("goal_domain")
    private String goalDomain;

    @SerializedName("allow_to_use")
    private Integer allowToUse;

    @SerializedName("goal_name")
    private String goalName;

    public String getGoalSubtype() {
        return goalSubtype;
    }

    public RetargetingGoal withGoalSubtype(String goalSubtype) {
        this.goalSubtype = goalSubtype;
        return this;
    }

    public String getGoalDomain() {
        return goalDomain;
    }

    public RetargetingGoal withGoalDomain(String goalDomain) {
        this.goalDomain = goalDomain;
        return this;
    }

    public Integer getAllowToUse() {
        return allowToUse;
    }

    public RetargetingGoal withAllowToUse(Integer allowToUse) {
        this.allowToUse = allowToUse;
        return this;
    }

    public String getGoalName() {
        return goalName;
    }

    public RetargetingGoal withGoalName(String goalName) {
        this.goalName = goalName;
        return this;
    }

    public String getGoalId() {
        return goalId;
    }

    public RetargetingGoal withGoalId(String goalId) {
        this.goalId = goalId;
        return this;
    }

    public GoalType getGoalType() {
        return goalType;
    }

    public RetargetingGoal withGoalType(GoalType goalType) {
        this.goalType = goalType;
        return this;
    }

    public String getTime() {
        return time;
    }

    public RetargetingGoal withTime(String time) {
        this.time = time;
        return this;
    }

    public void setGoalId(String goalId) {
        this.goalId = goalId;
    }

    public void setGoalType(GoalType goalType) {
        this.goalType = goalType;
    }

    public void setTime(String time) {
        this.time = time;
    }
}
