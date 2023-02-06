package ru.yandex.autotests.direct.cmd.data.commons.adjustment;

import com.google.gson.annotations.SerializedName;

import java.util.HashMap;

public class RetargetingMultiplier {

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

    public static RetargetingMultiplier getDefaultRetargetingMultiplier(String retargetingId, String multiplierPct) {
        HashMap<String, RetargetingCondition> retargetingConditionMap = new HashMap<>();
        retargetingConditionMap.put(retargetingId, new RetargetingCondition()
                .withMultiplierPct(multiplierPct));
        return new RetargetingMultiplier().
                withEnabled(1).
                withConditions(retargetingConditionMap);
    }
}
