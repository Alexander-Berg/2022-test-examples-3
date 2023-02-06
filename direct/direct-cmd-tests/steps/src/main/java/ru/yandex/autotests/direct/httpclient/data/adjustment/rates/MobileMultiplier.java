package ru.yandex.autotests.direct.httpclient.data.adjustment.rates;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.gson.annotations.SerializedName;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;
import ru.yandex.autotests.httpclient.lite.core.exceptions.BackEndClientParametersException;

import java.io.IOException;

/**
 * Created by aleran on 06.08.2015.
 */
public class MobileMultiplier {

    @JsonPath(responsePath = "multiplier_pct")
    @JsonProperty(value = "multiplier_pct")
    @SerializedName(value = "multiplier_pct")
    private String multiplierPct;

    @JsonProperty(value = "multiplier_pct")
    public String getMultiplierPct() {
        return multiplierPct;
    }

    @JsonProperty(value = "multiplier_pct")
    public void setMultiplierPct(String multiplierPct) {
        this.multiplierPct = multiplierPct;
    }

    public MobileMultiplier withMultiplierPct(String multiplierPct){
        this.multiplierPct = multiplierPct;
        return this;
    }
}
