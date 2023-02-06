package ru.yandex.autotests.direct.httpclient.data.adjustment.rates;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.gson.annotations.SerializedName;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.ObjectMapper;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;
import ru.yandex.autotests.httpclient.lite.core.exceptions.BackEndClientParametersException;

import java.io.IOException;

/**
 * Created by aleran on 05.08.2015.
 */
public class DemographyCondition {

    @JsonPath(requestPath = "age", responsePath = "age")
    private String age;
    @JsonPath(requestPath = "gender", responsePath = "gender")
    private String gender;

    @JsonPath(requestPath = "multiplier_pct", responsePath = "multiplier_pct")
    @SerializedName("multiplier_pct")
    private String multiplierPct;

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getMultiplierPct() {
        return multiplierPct;
    }

    public void setMultiplierPct(String multiplierPct) {
        this.multiplierPct = multiplierPct;
    }

    public DemographyCondition withAge(String age) {
        this.age = age;
        return this;
    }

    public DemographyCondition withGender(String gender) {
        this.gender = gender;
        return this;
    }

    public DemographyCondition withMultiplierPct(String multiplierPct) {
        this.multiplierPct = multiplierPct;
        return this;
    }
}
