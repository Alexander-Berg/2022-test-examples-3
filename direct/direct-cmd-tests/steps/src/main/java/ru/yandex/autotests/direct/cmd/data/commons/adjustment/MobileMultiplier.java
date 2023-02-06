package ru.yandex.autotests.direct.cmd.data.commons.adjustment;

import com.google.gson.annotations.SerializedName;

public class MobileMultiplier {

    @SerializedName(value = "multiplier_pct")
    private String multiplierPct;

    public String getMultiplierPct() {
        return multiplierPct;
    }

    public void setMultiplierPct(String multiplierPct) {
        this.multiplierPct = multiplierPct;
    }

    public MobileMultiplier withMultiplierPct(String multiplierPct){
        this.multiplierPct = multiplierPct;
        return this;
    }

    public static MobileMultiplier getDefaultMobileMultiplier(String multiplierPct) {
        return new MobileMultiplier().
                withMultiplierPct(multiplierPct);
    }
}
