package ru.yandex.autotests.direct.cmd.data.commons.adjustment;

import java.util.Map;

import com.google.gson.annotations.SerializedName;

public class GeoMultiplierRegion {
    @SerializedName("geo_multiplier_value_id")
    private Long id;
    @SerializedName("region_id")
    private Long regionId;
    @SerializedName("multiplier_pct")
    private Integer pct;

    public Long getId() {
        return id;
    }

    public GeoMultiplierRegion withId(Long id) {
        this.id = id;
        return this;
    }

    public Long getRegionId() {
        return regionId;
    }

    public GeoMultiplierRegion withRegionId(Long regionId) {
        this.regionId = regionId;
        return this;
    }

    public Integer getPct() {
        return pct;
    }

    public GeoMultiplierRegion withPct(Integer pct) {
        this.pct = pct;
        return this;
    }


}
