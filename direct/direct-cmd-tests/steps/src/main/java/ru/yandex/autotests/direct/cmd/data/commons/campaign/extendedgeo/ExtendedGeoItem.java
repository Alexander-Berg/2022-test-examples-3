package ru.yandex.autotests.direct.cmd.data.commons.campaign.extendedgeo;

import com.google.gson.annotations.SerializedName;

public class ExtendedGeoItem extends NegativeExtendedGeoItem {
    @SerializedName("multiplier_pct")
    private String multiplierPct;

    @SerializedName("negative")
    private NegativeExtendedGeoItem negative;

    public String getMultiplierPct() {
        return multiplierPct;
    }

    public ExtendedGeoItem withMultiplierPct(String multiplierPct) {
        this.multiplierPct = multiplierPct;
        return this;
    }

    public NegativeExtendedGeoItem getNegative() {
        return negative;
    }

    public ExtendedGeoItem withNegative(NegativeExtendedGeoItem negative) {
        this.negative = negative;
        return this;
    }
}
