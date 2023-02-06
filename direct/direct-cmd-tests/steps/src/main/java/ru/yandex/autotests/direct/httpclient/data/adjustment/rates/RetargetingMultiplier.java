package ru.yandex.autotests.direct.httpclient.data.adjustment.rates;

import com.google.gson.annotations.SerializedName;
import ru.yandex.autotests.direct.httpclient.data.ResponseMapBean;

import java.util.HashMap;

/**
 * Created by aleran on 06.08.2015.
 */
public class RetargetingMultiplier extends ResponseMapBean {

    @SerializedName("is_enabled")
    private Integer enabled;

    private HashMap<String, RetargetingCondition> conditions;

    public Integer getEnabled() {
        return enabled;
    }

    public void setEnabled(Integer enabled) {
        this.enabled = enabled;
    }

    public HashMap<String, RetargetingCondition> getConditions() {
        return conditions;
    }

    public void setConditions(HashMap<String, RetargetingCondition> conditions) {
        this.conditions = conditions;
    }

    public RetargetingMultiplier withConditions(HashMap<String, RetargetingCondition> conditions){
        this.conditions = conditions;
        return this;
    }

    public RetargetingMultiplier withEnabled(Integer enabled){
        this.enabled = enabled;
        return this;
    }
}
