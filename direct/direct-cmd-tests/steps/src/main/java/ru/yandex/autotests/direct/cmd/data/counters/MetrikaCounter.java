package ru.yandex.autotests.direct.cmd.data.counters;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class MetrikaCounter {

    @SerializedName("allow")
    private Boolean allow;

    @SerializedName("goals")
    private List<MetrikaGoal> metrikaGoals;

    public Boolean getAllow() {
        return allow;
    }

    public void setAllow(Boolean allow) {
        this.allow = allow;
    }

    public List<MetrikaGoal> getGoals() {
        return metrikaGoals;
    }

    public void setGoals(List<MetrikaGoal> metrikaGoals) {
        this.metrikaGoals = metrikaGoals;
    }
}
