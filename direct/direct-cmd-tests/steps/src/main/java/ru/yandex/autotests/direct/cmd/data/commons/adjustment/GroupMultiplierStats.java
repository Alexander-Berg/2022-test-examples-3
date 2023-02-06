package ru.yandex.autotests.direct.cmd.data.commons.adjustment;

import com.google.gson.annotations.SerializedName;

public class GroupMultiplierStats {

    @SerializedName("adjustments_upper_bound")
    private String adjustmentsUpperBound;

    @SerializedName("adjustments_lower_bound")
    private String adjustmentsLowerBound;

    public String getAdjustmentsUpperBound() {
        return adjustmentsUpperBound;
    }

    public void setAdjustmentsUpperBound(String adjustmentsUpperBound) {
        this.adjustmentsUpperBound = adjustmentsUpperBound;
    }

    public String getAdjustmentsLowerBound() {
        return adjustmentsLowerBound;
    }

    public void setAdjustmentsLowerBound(String adjustmentsLowerBound) {
        this.adjustmentsLowerBound = adjustmentsLowerBound;
    }

    public GroupMultiplierStats withAdjustmentsLowerBound(String adjustmentsLowerBound) {
        this.adjustmentsLowerBound = adjustmentsLowerBound;
        return this;
    }

    public GroupMultiplierStats withAdjustmentsUpperBound(String adjustmentsUpperBound) {
        this.adjustmentsUpperBound = adjustmentsUpperBound;
        return this;
    }
}
