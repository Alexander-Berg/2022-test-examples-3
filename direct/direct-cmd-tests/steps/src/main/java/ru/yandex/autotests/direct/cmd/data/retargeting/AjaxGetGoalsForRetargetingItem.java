package ru.yandex.autotests.direct.cmd.data.retargeting;

import com.google.gson.annotations.SerializedName;

public class AjaxGetGoalsForRetargetingItem {

    @SerializedName("goal_id")
    private Long goalId;

    @SerializedName("goal_name")
    private String goalName;

    @SerializedName("goal_domain")
    private String goalDomain;

    @SerializedName("counter_id")
    private Long counterId;

    @SerializedName("allow_to_use")
    private Integer allowToUse;

    public Long getGoalId() {
        return goalId;
    }

    public AjaxGetGoalsForRetargetingItem withGoalId(Long goalId) {
        this.goalId = goalId;
        return this;
    }

    public String getGoalName() {
        return goalName;
    }

    public AjaxGetGoalsForRetargetingItem withGoalName(String goalName) {
        this.goalName = goalName;
        return this;
    }

    public String getGoalDomain() {
        return goalDomain;
    }

    public AjaxGetGoalsForRetargetingItem withGoalDomain(String goalDomain) {
        this.goalDomain = goalDomain;
        return this;
    }

    public Long getCounterId() {
        return counterId;
    }

    public AjaxGetGoalsForRetargetingItem withCounterId(Long counterId) {
        this.counterId = counterId;
        return this;
    }

    public Integer getAllowToUse() {
        return allowToUse;
    }

    public AjaxGetGoalsForRetargetingItem withAllowToUse(Integer allowToUse) {
        this.allowToUse = allowToUse;
        return this;
    }
}
