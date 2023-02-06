package ru.yandex.autotests.direct.cmd.data.commons.campaign;

import com.google.gson.annotations.SerializedName;

import ru.yandex.autotests.httpclientlite.core.support.gson.NullValueWrapper;

public class DayBudget {
    @SerializedName("stop_time")
    private String stopTime;

    @SerializedName("show_mode")
    private ShowMode showMode;

    @SerializedName("daily_change_count")
    private String dailyChangeCount;

    @SerializedName("recommended_sum")
    private String recomendedSum;

    @SerializedName("sum")
    private String sum;

    @SerializedName("set")
    private Boolean set;

    public String getSum() {
        return sum;
    }

    public void setSum(String sum) {
        this.sum = sum;
    }

    public String getRecomendedSum() {
        return recomendedSum;
    }

    public void setRecomendedSum(String recomendedSum) {
        this.recomendedSum = recomendedSum;
    }

    public String getDailyChangeCount() {
        return dailyChangeCount;
    }

    public void setDailyChangeCount(String dailyChangeCount) {
        this.dailyChangeCount = dailyChangeCount;
    }

    public ShowMode getShowMode() {
        return showMode;
    }

    public void setShowMode(ShowMode showMode) {
        this.showMode = showMode;
    }

    public String getStopTime() {
        return stopTime;
    }

    public void setStopTime(String stopTime) {
        this.stopTime = stopTime;
    }

    public DayBudget withStopTime(String stopTime) {
        this.stopTime = stopTime;
        return this;
    }

    public DayBudget withShowMode(ShowMode showMode) {
        setShowMode(showMode);
        return this;
    }

    public DayBudget withDailyChangeCount(String dailyChangeCount) {
        this.dailyChangeCount = dailyChangeCount;
        return this;
    }

    public DayBudget withRecomendedSum(String recomendedSum) {
        this.recomendedSum = recomendedSum;
        return this;
    }

    public DayBudget withSum(String sum) {
        this.sum = sum;
        return this;
    }

    public Boolean getSet() {
        return set;
    }

    public DayBudget withSet(Boolean set) {
        this.set = set;
        return this;
    }

    public enum ShowMode {
        @SerializedName("default")
        DEFAULT,
        @SerializedName("stretched")
        STRETCHED
    }
}
