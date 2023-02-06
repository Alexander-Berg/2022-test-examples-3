package ru.yandex.autotests.direct.cmd.data.commons.adjustment;

import com.google.gson.annotations.SerializedName;

public class AbSegmentMultiplierData {

    @SerializedName("ab_segment_multiplier_value_id")
    private Long id;

    @SerializedName("segment_id")
    private Long segmentId;

    @SerializedName("section_id")
    private Long sectionId;

    @SerializedName("multiplier_pct")
    private String multiplierPct;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSegmentId() {
        return segmentId;
    }

    public void setSegmentId(Long segmentId) {
        this.segmentId = segmentId;
    }

    public Long getSectionId() {
        return sectionId;
    }

    public void setSectionId(Long sectionId) {
        this.sectionId = sectionId;
    }

    public String getMultiplierPct() {
        return multiplierPct;
    }

    public void setMultiplierPct(String multiplierPct) {
        this.multiplierPct = multiplierPct;
    }

    public AbSegmentMultiplierData withId(Long id) {
        this.id = id;
        return this;
    }

    public AbSegmentMultiplierData withSegmentId(Long segmentId) {
        this.segmentId = segmentId;
        return this;
    }

    public AbSegmentMultiplierData withSectionId(Long sectionId) {
        this.sectionId = sectionId;
        return this;
    }

    public AbSegmentMultiplierData withMultiplierPct(String multiplierPct) {
        this.multiplierPct = multiplierPct;
        return this;
    }
}
