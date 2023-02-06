package ru.yandex.autotests.direct.cmd.data.commons.campaign;

import com.google.gson.annotations.SerializedName;

public class StrategyMinPrice {

    @SerializedName("premium")
    private String premium;

    @SerializedName("guarantee")
    private String guarantee;

    public String getPremium() {
        return premium;
    }

    public StrategyMinPrice withPremium(String premium) {
        this.premium = premium;
        return this;
    }

    public String getGuarantee() {
        return guarantee;
    }

    public StrategyMinPrice withGuarantee(String guarantee) {
        this.guarantee = guarantee;
        return this;
    }
}
