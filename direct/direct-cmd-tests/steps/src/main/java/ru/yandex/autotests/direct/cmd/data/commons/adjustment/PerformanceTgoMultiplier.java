package ru.yandex.autotests.direct.cmd.data.commons.adjustment;

import com.google.gson.annotations.SerializedName;

public class PerformanceTgoMultiplier {

    @SerializedName(value = "multiplier_pct")
    private String multiplierPct;

    public String getMultiplierPct() {
        return multiplierPct;
    }

    public void setMultiplierPct(String multiplierPct) {
        this.multiplierPct = multiplierPct;
    }

    public PerformanceTgoMultiplier withMultiplierPct(String multiplierPct){
        this.multiplierPct = multiplierPct;
        return this;
    }
}
