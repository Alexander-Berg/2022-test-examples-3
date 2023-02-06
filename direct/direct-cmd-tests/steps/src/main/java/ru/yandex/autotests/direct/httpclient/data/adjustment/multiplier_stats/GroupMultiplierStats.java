package ru.yandex.autotests.direct.httpclient.data.adjustment.multiplier_stats;

import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

/**
 * Created by aleran on 01.09.2015.
 */
public class GroupMultiplierStats {
    @JsonPath(responsePath = "adjustments_upper_bound")
    private String adjustmentsUpperBound;

    @JsonPath(responsePath = "adjustments_lower_bound")
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

    public GroupMultiplierStats withAdjustmentsLowerBound(String adjustmentsLowerBound){
        this.adjustmentsLowerBound = adjustmentsLowerBound;
        return this;
    }

    public GroupMultiplierStats withAdjustmentsUpperBound(String adjustmentsUpperBound){
        this.adjustmentsUpperBound = adjustmentsUpperBound;
        return this;
    }
}
