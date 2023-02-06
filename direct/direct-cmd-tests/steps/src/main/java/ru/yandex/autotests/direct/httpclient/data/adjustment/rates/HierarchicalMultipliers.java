package ru.yandex.autotests.direct.httpclient.data.adjustment.rates;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import org.codehaus.jackson.annotate.JsonProperty;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

/**
 * Created by aleran on 06.08.2015.
 */

public class HierarchicalMultipliers {

    @Expose
    @SerializedName(value = "retargeting_multiplier")
    @JsonPath(responsePath = "retargeting_multiplier")
    private RetargetingMultiplier retargetingMultiplier;

    @Expose
    @SerializedName(value = "demography_multiplier")
    @JsonPath(requestPath = "demography_multiplier", responsePath = "demography_multiplier")
    private DemographyMultiplier demographyMultiplier;

    @Expose
    @SerializedName(value = "mobile_multiplier")
    @JsonPath(responsePath = "mobile_multiplier")
    private MobileMultiplier mobileMultiplier;

    public RetargetingMultiplier getRetargetingMultiplier() {
        return retargetingMultiplier;
    }

    public void setRetargetingMultiplier(RetargetingMultiplier retargetingMultiplier) {
        this.retargetingMultiplier = retargetingMultiplier;
    }

    @JsonProperty(value = "mobile_multiplier")
    public MobileMultiplier getMobileMultiplier() {
        return mobileMultiplier;
    }

    @JsonProperty(value = "mobile_multiplier")
    public void setMobileMultiplier(MobileMultiplier mobileMultiplier) {
        this.mobileMultiplier = mobileMultiplier;
    }

    @JsonProperty(value = "demography_multiplier")
    public DemographyMultiplier getDemographyMultiplier() {
        return demographyMultiplier;
    }

    @JsonProperty(value = "demography_multiplier")
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

    public String toJson() {
        return new Gson().toJson(this, HierarchicalMultipliers.class);
    }

    public String toString() {
        return toJson();
    }
}
