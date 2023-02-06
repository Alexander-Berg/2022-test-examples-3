package ru.yandex.autotests.direct.httpclient.data.adjustment.rates;

import com.google.gson.annotations.SerializedName;
import org.codehaus.jackson.annotate.JsonProperty;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

/**
 * Created by aleran on 06.08.2015.
 */
public class RetargetingCondition {

    @SerializedName("multiplier_pct")
    private String multiplierPct;


    public String getMultiplierPct() {
        return multiplierPct;
    }

    public void setMultiplierPct(String multiplierPct) {
        this.multiplierPct = multiplierPct;
    }


    public RetargetingCondition withMultiplierPct(String multiplierPct){
        this.multiplierPct = multiplierPct;
        return this;
    }
}
