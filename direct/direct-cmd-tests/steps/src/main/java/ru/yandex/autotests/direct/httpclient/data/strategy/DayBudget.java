package ru.yandex.autotests.direct.httpclient.data.strategy;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;
import ru.yandex.autotests.httpclient.lite.core.exceptions.BackEndClientParametersException;

import java.io.IOException;

/**
 * Created by alexey-n on 17.08.14.
 */
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@Deprecated
public class DayBudget {

    private Boolean set;
    @JsonPath(responsePath = "sum")
    private String sum;

    @JsonPath(responsePath = "show_mode")
    @JsonProperty("show_mode")
    private String showMode;

    public DayBudget() {
        set = false;
        showMode = "default";
        sum = "";
    }

    public Boolean getSet() {
        return set;
    }

    public void setSet(Boolean set) {
        this.set = set;
    }

    public String getSum() {
        return sum;
    }

    public void setSum(String sum) {
        this.sum = sum;
    }

    @JsonProperty("show_mode")
    public String getShowMode() {
        return showMode;
    }

    @JsonProperty("show_mode")
    public void setShowMode(String showMode) {
        this.showMode = showMode;
    }

    public String toJson() {
        String json;
        try {
            json = (new ObjectMapper()).writeValueAsString(this);
        } catch (IOException e) {
            throw new BackEndClientParametersException("Object parsing exception", e);
        }
        return json;
    }
}
