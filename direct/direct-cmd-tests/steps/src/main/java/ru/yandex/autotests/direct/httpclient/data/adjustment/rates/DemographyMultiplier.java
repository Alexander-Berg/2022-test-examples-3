package ru.yandex.autotests.direct.httpclient.data.adjustment.rates;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.gson.annotations.SerializedName;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import ru.yandex.autotests.direct.httpclient.data.adjustment.rates.DemographyCondition;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

import java.util.List;

/**
 * Created by aleran on 06.08.2015.
 */
public class DemographyMultiplier {

    @SerializedName("is_enabled")
    @JsonPath(requestPath = "is_enabled", responsePath = "is_enabled")
    private Integer enabled;

    @JsonPath(requestPath = "conditions", responsePath = "conditions")
    List<DemographyCondition> conditions;

    public Integer getEnabled() {
        return enabled;
    }

    public void setEnabled(Integer enabled) {
        this.enabled = enabled;
    }

    public List<DemographyCondition> getConditions() {
        return conditions;
    }

    public void setConditions(List<DemographyCondition> conditions) {
        this.conditions = conditions;
    }

    public DemographyMultiplier withConditions(List<DemographyCondition> conditions) {
        this.conditions = conditions;
        return this;
    }

    public DemographyMultiplier withEnabled(Integer enabled) {
        this.enabled = enabled;
        return this;
    }
}
