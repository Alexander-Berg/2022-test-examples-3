package ru.yandex.autotests.direct.cmd.data.commons;

import com.google.gson.annotations.SerializedName;

public class EmailNotifications {

    @SerializedName("paused_by_day_budget")
    private Integer pausedByDayBudget;

    public Integer getPausedByDayBudget() {
        return pausedByDayBudget;
    }

    public EmailNotifications withPausedByDayBudget(Integer pausedByDayBudget) {
        this.pausedByDayBudget = pausedByDayBudget;
        return this;
    }
}
