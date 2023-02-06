package ru.yandex.autotests.direct.cmd.data.commons.adjustment;

import com.google.gson.annotations.SerializedName;

public class HierarchicalMultipliers {

    @SerializedName(value = "retargeting_multiplier")
    private RetargetingMultiplier retargetingMultiplier;

    @SerializedName(value = "demography_multiplier")
    private DemographyMultiplier demographyMultiplier;

    @SerializedName(value = "mobile_multiplier")
    private MobileMultiplier mobileMultiplier;

    @SerializedName("geo_multiplier")
    private GeoMultiplier geoMultiplier;

    @SerializedName("ab_segment_multiplier")
    private AbSegmentMultiplier abSegmentMultiplier;

    @SerializedName("performance_tgo_multiplier")
    private PerformanceTgoMultiplier performanceTgoMultiplier;

    public RetargetingMultiplier getRetargetingMultiplier() {
        return retargetingMultiplier;
    }

    public void setRetargetingMultiplier(RetargetingMultiplier retargetingMultiplier) {
        this.retargetingMultiplier = retargetingMultiplier;
    }

    public MobileMultiplier getMobileMultiplier() {
        return mobileMultiplier;
    }

    public void setMobileMultiplier(MobileMultiplier mobileMultiplier) {
        this.mobileMultiplier = mobileMultiplier;
    }

    public DemographyMultiplier getDemographyMultiplier() {
        return demographyMultiplier;
    }

    public GeoMultiplier getGeoMultiplier() {
        return geoMultiplier;
    }

    public void setDemographyMultiplier(DemographyMultiplier demographyMultiplier) {
        this.demographyMultiplier = demographyMultiplier;
    }

    public HierarchicalMultipliers withRetargetingMultiplier(RetargetingMultiplier retargetingMultiplier) {
        this.retargetingMultiplier = retargetingMultiplier;
        return this;
    }

    public HierarchicalMultipliers withMobileMultiplier(MobileMultiplier mobileMultiplier) {
        this.mobileMultiplier = mobileMultiplier;
        return this;
    }

    public HierarchicalMultipliers withDemographyMultiplier(DemographyMultiplier demographyMultiplier) {
        this.demographyMultiplier = demographyMultiplier;
        return this;
    }

    public HierarchicalMultipliers withGeoMultiplier(GeoMultiplier geoMultiplier) {
        this.geoMultiplier = geoMultiplier;
        return this;
    }

    public AbSegmentMultiplier getAbSegmentMultiplier() {
        return abSegmentMultiplier;
    }

    public HierarchicalMultipliers withAbSegmentMultiplier(AbSegmentMultiplier abSegmentMultiplier) {
        this.abSegmentMultiplier = abSegmentMultiplier;
        return this;
    }

    public PerformanceTgoMultiplier getPerformanceTgoMultiplier() {
        return performanceTgoMultiplier;
    }

    public void setPerformanceTgoMultiplier(
            PerformanceTgoMultiplier performanceTgoMultiplier)
    {
        this.performanceTgoMultiplier = performanceTgoMultiplier;
    }

    public HierarchicalMultipliers withPerformanceTgoMultiplier(PerformanceTgoMultiplier performanceTgoMultiplier) {
        this.performanceTgoMultiplier = performanceTgoMultiplier;
        return this;
    }
}
