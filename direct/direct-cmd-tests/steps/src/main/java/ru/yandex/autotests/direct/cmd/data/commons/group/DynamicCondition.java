package ru.yandex.autotests.direct.cmd.data.commons.group;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class DynamicCondition {

    @SerializedName("dyn_cond_id")
    private String dynamicConditionId;

    @SerializedName("dyn_id")
    private String dynId;

    @SerializedName("condition_name")
    private String dynamicConditionName;

    @SerializedName("type")
    private String type;

    @SerializedName("price")
    private Float price;

    @SerializedName("price_context")
    private String priceContext;

    @SerializedName("autobudgetPriority")
    private String autobudgetPriority;

    @SerializedName("adgroup_id")
    private Long adGroupId;

    @SerializedName("condition")
    private List<Condition> conditions;

    @SerializedName("is_suspended")
    private String isSuspended;

    @SerializedName("status_bs_synced")
    private String statusBsSynced;

    public void setPrice(Float price) {
        this.price = price;
    }

    public String getDynamicConditionId() {
        return dynamicConditionId;
    }

    public String getDynId() {
        return dynId;
    }

    public Long getAdGroupId() {
        return adGroupId;
    }

    public void setAdGroupId(Long adGroupId) {
        this.adGroupId = adGroupId;
    }

    public void setDynamicConditionId(String dynamicConditionId) {
        this.dynamicConditionId = dynamicConditionId;
    }

    public void setDynId(String dynId) {
        this.dynId = dynId;
    }

    public String getDynamicConditionName() {
        return dynamicConditionName;
    }

    public void setDynamicConditionName(String dynamicConditionName) {
        this.dynamicConditionName = dynamicConditionName;
    }

    public Float getPrice() {
        return price;
    }

    public String getPriceContext() {
        return priceContext;
    }

    public void setPriceContext(String priceContext) {
        this.priceContext = priceContext;
    }

    public List<Condition> getConditions() {
        return conditions;
    }

    public void setConditions(List<Condition> conditions) {
        this.conditions = conditions;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAutobudgetPriority() {
        return autobudgetPriority;
    }

    public void setAutobudgetPriority(String autobudgetPriority) {
        this.autobudgetPriority = autobudgetPriority;
    }

    public String getIsSuspended() {
        return isSuspended;
    }

    public void setIsSuspended(String isSuspended) {
        this.isSuspended = isSuspended;
    }

    public DynamicCondition withDynamicConditionId(String dynamicConditionId) {
        this.dynamicConditionId = dynamicConditionId;
        return this;
    }

    public DynamicCondition withDynId(String dynId) {
        this.dynId = dynId;
        return this;
    }

    public DynamicCondition withDynamicConditionName(String dynamicConditionName) {
        this.dynamicConditionName = dynamicConditionName;
        return this;
    }

    public DynamicCondition withType(String type) {
        this.type = type;
        return this;
    }

    public DynamicCondition withPrice(Float price) {
        this.price = price;
        return this;
    }

    public DynamicCondition withPriceContext(String priceContext) {
        this.priceContext = priceContext;
        return this;
    }

    public DynamicCondition withAutobudgetPriority(String autobudgetPriority) {
        this.autobudgetPriority = autobudgetPriority;
        return this;
    }

    public DynamicCondition withAdGroupId(Long adGroupId) {
        this.adGroupId = adGroupId;
        return this;
    }

    public DynamicCondition withConditions(List<Condition> condition) {
        this.conditions = condition;
        return this;
    }

    public DynamicCondition withIsSuspended(String isSuspended) {
        this.isSuspended = isSuspended;
        return this;
    }

    public String getStatusBsSynced() {
        return statusBsSynced;
    }

    public DynamicCondition withStatusBsSynced(String statusBsSynced) {
        this.statusBsSynced = statusBsSynced;
        return this;
    }
}

