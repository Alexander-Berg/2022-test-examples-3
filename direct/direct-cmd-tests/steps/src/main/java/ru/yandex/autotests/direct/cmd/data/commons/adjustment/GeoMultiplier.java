package ru.yandex.autotests.direct.cmd.data.commons.adjustment;

import java.util.List;
import java.util.Map;

import com.google.gson.annotations.SerializedName;

import static java.util.stream.Collectors.toList;

public class GeoMultiplier {

    @SerializedName("is_enabled")
    private Integer isEnabled;
    @SerializedName("hierarchical_multiplier_id")
    private Long hierarchicalMultiplierId;
    @SerializedName("last_change")
    private String lastChange;
    @SerializedName("regions")
    private List<GeoMultiplierRegion> regions;

    public Integer getIsEnabled() {
        return isEnabled;
    }

    public GeoMultiplier withIsEnabled(Integer isEnabled) {
        this.isEnabled = isEnabled;
        return this;
    }

    public Long getHierarchicalMultiplierId() {
        return hierarchicalMultiplierId;
    }

    public GeoMultiplier withHierarchicalMultiplierId(Long hierarchicalMultiplierId) {
        this.hierarchicalMultiplierId = hierarchicalMultiplierId;
        return this;
    }

    public String getLastChange() {
        return lastChange;
    }

    public GeoMultiplier withLastChange(String lastChange) {
        this.lastChange = lastChange;
        return this;
    }

    public List<GeoMultiplierRegion> getRegions() {
        return regions;
    }

    public GeoMultiplier withRegions(
            List<GeoMultiplierRegion> regions)
    {
        this.regions = regions;
        return this;
    }

    public GeoMultiplier withRegions(Map<String, String> regionsMap) {
        regions = regionsMap.entrySet().stream()
                .map(x -> new GeoMultiplierRegion()
                        .withRegionId(Long.valueOf(x.getKey()))
                        .withPct(Integer.valueOf(x.getValue())))
                .collect(toList());
        return this;
    }
}
