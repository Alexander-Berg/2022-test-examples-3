package ru.yandex.autotests.direct.cmd.data.commons.campaign.extendedgeo;

import com.google.gson.annotations.SerializedName;

public class NegativeExtendedGeoItem {
    @SerializedName("all")
    private String all;
    @SerializedName("partly")
    private Partly partly;

    public Partly getPartly() {
        return partly;
    }

    public <T extends NegativeExtendedGeoItem> T withPartly(Partly partly) {
        this.partly = partly;
        return (T) this;
    }

    public String getAll() {
        return all;
    }

    public <T extends NegativeExtendedGeoItem> T withAll(String all) {
        this.all = all;
        return (T) this;
    }
}
