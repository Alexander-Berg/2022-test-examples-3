package ru.yandex.autotests.direct.cmd.data.commons.group;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class CpmRetargetingGroup {
    @SerializedName("type")
    private String type;

    @SerializedName("goals")
    private List<CpmGoal> goals;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<CpmGoal> getGoals() {
        return goals;
    }

    public void setGoals(List<CpmGoal> goals) {
        this.goals = goals;
    }

    public CpmRetargetingGroup withType(String type) {
        this.type = type;
        return this;
    }

    public CpmRetargetingGroup withGoals(List<CpmGoal> goals) {
        this.goals = goals;
        return this;
    }
}
