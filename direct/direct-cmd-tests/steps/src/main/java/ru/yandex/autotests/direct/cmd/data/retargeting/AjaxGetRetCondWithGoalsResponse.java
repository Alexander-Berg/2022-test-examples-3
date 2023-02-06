package ru.yandex.autotests.direct.cmd.data.retargeting;

import java.util.List;

import com.google.gson.annotations.SerializedName;

import ru.yandex.autotests.direct.cmd.data.commons.group.RetargetingConditionItem;

public class AjaxGetRetCondWithGoalsResponse {
    @SerializedName("is_accessible")
    private Integer isAccessible;
    @SerializedName("properties")
    private String properties;
    @SerializedName("condition_name")
    private String conditionName;
    @SerializedName("condition_desc")
    private String conditionDesc;
    @SerializedName("ClientID")
    private Long clentId;
    @SerializedName("condition")
    private List<RetargetingConditionItem> condition;
    @SerializedName("is_used")
    private Integer isUsed;
    @SerializedName("ret_cond_id")
    private Long retCondId;

    public Integer getIsAccessible() {
        return isAccessible;
    }

    public AjaxGetRetCondWithGoalsResponse withIsAccessible(Integer isAccessible) {
        this.isAccessible = isAccessible;
        return this;
    }

    public String getProperties() {
        return properties;
    }

    public AjaxGetRetCondWithGoalsResponse withProperties(String properties) {
        this.properties = properties;
        return this;
    }

    public String getConditionName() {
        return conditionName;
    }

    public AjaxGetRetCondWithGoalsResponse withConditionName(String conditionName) {
        this.conditionName = conditionName;
        return this;
    }

    public String getConditionDesc() {
        return conditionDesc;
    }

    public AjaxGetRetCondWithGoalsResponse withConditionDesc(String conditionDesc) {
        this.conditionDesc = conditionDesc;
        return this;
    }

    public Long getClentId() {
        return clentId;
    }

    public AjaxGetRetCondWithGoalsResponse withClentId(Long clentId) {
        this.clentId = clentId;
        return this;
    }

    public List<RetargetingConditionItem> getCondition() {
        return condition;
    }

    public AjaxGetRetCondWithGoalsResponse withCondition(
            List<RetargetingConditionItem> condition)
    {
        this.condition = condition;
        return this;
    }

    public Integer getIsUsed() {
        return isUsed;
    }

    public AjaxGetRetCondWithGoalsResponse withIsUsed(Integer isUsed) {
        this.isUsed = isUsed;
        return this;
    }

    public Long getRetCondId() {
        return retCondId;
    }

    public AjaxGetRetCondWithGoalsResponse withRetCondId(Long retCondId) {
        this.retCondId = retCondId;
        return this;
    }
}
