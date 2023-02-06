package ru.yandex.autotests.direct.cmd.data.commons.adjustment;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class AbSegmentMultiplier {

    @SerializedName("is_enabled")
    private Boolean enabled;

    @SerializedName("hierarchical_multiplier_id")
    private Long hierarchicalMultiplierId;

    @SerializedName("last_change")
    private String lastChange;

    @SerializedName("ab_segments")
    private List<AbSegmentMultiplierData> abSegments;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Long getHierarchicalMultiplierId() {
        return hierarchicalMultiplierId;
    }

    public void setHierarchicalMultiplierId(Long hierarchicalMultiplierId) {
        this.hierarchicalMultiplierId = hierarchicalMultiplierId;
    }

    public String getLastChange() {
        return lastChange;
    }

    public void setLastChange(String lastChange) {
        this.lastChange = lastChange;
    }

    public List<AbSegmentMultiplierData> getAbSegments() {
        return abSegments;
    }

    public void setAbSegments(List<AbSegmentMultiplierData> abSegments) {
        this.abSegments = abSegments;
    }

    public AbSegmentMultiplier withEnabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public AbSegmentMultiplier withHierarchicalMultiplierId(Long hierarchicalMultiplierId) {
        this.hierarchicalMultiplierId = hierarchicalMultiplierId;
        return this;
    }

    public AbSegmentMultiplier withLastChange(String lastChange) {
        this.lastChange = lastChange;
        return this;
    }

    public AbSegmentMultiplier withAbSegments(List<AbSegmentMultiplierData> abSegments) {
        this.abSegments = abSegments;
        return this;
    }
}
